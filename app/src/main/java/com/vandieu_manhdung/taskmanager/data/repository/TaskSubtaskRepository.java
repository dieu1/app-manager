package com.vandieu_manhdung.taskmanager.data.repository;

import android.content.Context;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.SyncStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TaskHistoryAction;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.notification.TaskNotificationManager;
import com.vandieu_manhdung.taskmanager.core.notification.TaskReminderScheduler;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.TaskSubtaskRules;
import com.vandieu_manhdung.taskmanager.core.util.TeamRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskSubtaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskHistoryDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamDao;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;
import com.vandieu_manhdung.taskmanager.model.TaskHistory;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public class TaskSubtaskRepository {

    private final Context context;
    private final TaskDao taskDao;
    private final TaskSubtaskDao subtaskDao;
    private final TaskHistoryDao historyDao;
    private final AppExecutors executors;
    private final CloudSyncManager cloudSync;
    private final TaskReminderScheduler reminderScheduler;
    private final TeamDao teamDao;

    public TaskSubtaskRepository(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext;
        taskDao = new TaskDao(applicationContext);
        subtaskDao = new TaskSubtaskDao(applicationContext);
        historyDao = new TaskHistoryDao(applicationContext);
        executors = AppExecutors.getInstance();
        cloudSync = CloudSyncManager.getInstance(applicationContext);
        reminderScheduler = new TaskReminderScheduler(applicationContext);
        teamDao = new TeamDao(applicationContext);
    }

    public void getSubtasks(
            String taskId,
            String userId,
            RepositoryCallback<List<TaskSubtask>> callback
    ) {
        executeInBackground(() -> {
            requireTask(taskId, userId);
            return subtaskDao.findAllByTask(taskId);
        }, callback);
    }

    public void createSubtask(
            String taskId,
            String userId,
            String title,
            int estimatedMinutes,
            RepositoryCallback<Boolean> callback
    ) {
        executeInBackground(() -> {
            Task task = requireTask(taskId, userId);
            String cleanTitle = requireTitle(title);
            if (estimatedMinutes < 0 || estimatedMinutes > 10080) {
                throw new IllegalArgumentException(
                        "Thời gian bước thực hiện phải từ 0 đến 10080 phút"
                );
            }

            long now = System.currentTimeMillis();
            TaskSubtask subtask = new TaskSubtask();
            subtask.setSubtaskId(UUID.randomUUID().toString());
            subtask.setTaskId(task.getTaskId());
            subtask.setWorkspaceId(task.getWorkspaceId());
            subtask.setCreatedBy(userId);
            if (task.getProjectId() != null && !task.getProjectId().isBlank()) {
                subtask.setAssigneeId(teamDao.findTaskAssigneeId(task.getTaskId()));
            }
            subtask.setTitle(cleanTitle);
            subtask.setEstimatedMinutes(estimatedMinutes);
            subtask.setCompleted(false);
            subtask.setCompletedAt(0);
            subtask.setDeletedAt(0);
            subtask.setVersion(1);
            subtask.setSyncStatus(SyncStatus.PENDING);
            subtask.setSortOrder(subtaskDao.nextSortOrder(taskId));
            subtask.setCreatedAt(now);
            subtask.setUpdatedAt(now);

            if (!subtaskDao.insert(subtask)) {
                throw new IllegalStateException("Không thể thêm bước thực hiện");
            }
            recordHistory(taskId, userId, TaskHistoryAction.UPDATED,
                    "Đã thêm bước: " + cleanTitle);
            cloudSync.upsertSubtask(subtask);
            recalculateParent(task);
            return true;
        }, callback);
    }

    public void toggleSubtask(
            String subtaskId,
            String userId,
            boolean completed,
            RepositoryCallback<Boolean> callback
    ) {
        executeInBackground(() -> {
            TaskSubtask subtask = subtaskDao.findById(subtaskId);
            if (subtask == null) {
                throw new IllegalStateException("Bước thực hiện không tồn tại");
            }
            Task task = requireTask(subtask.getTaskId(), userId);
            if (!canEditSubtask(task, subtask, userId)) {
                throw new SecurityException("Bạn không có quyền cập nhật bước này");
            }

            long now = System.currentTimeMillis();
            subtask.setCompleted(completed);
            subtask.setCompletedAt(completed ? now : 0);
            subtask.setUpdatedAt(now);
            subtask.setVersion(Math.max(1, subtask.getVersion() + 1));
            subtask.setSyncStatus(SyncStatus.PENDING);
            if (subtaskDao.update(subtask) <= 0) {
                throw new IllegalStateException("Không thể cập nhật bước thực hiện");
            }
            cloudSync.upsertSubtask(subtask);
            recalculateParent(task);
            recordHistory(task.getTaskId(), userId,
                    completed ? TaskHistoryAction.SUBTASK_COMPLETED : TaskHistoryAction.UPDATED,
                    (completed ? "Đã hoàn thành bước: " : "Đã mở lại bước: ") + subtask.getTitle());
            if (completed) {
                List<TaskSubtask> subtasks = subtaskDao.findAllByTask(task.getTaskId());
                TaskNotificationManager.showSubtaskCompleted(
                        context,
                        task,
                        subtask,
                        TaskSubtaskRules.completedCount(subtasks),
                        subtasks.size()
                );
            }
            return true;
        }, callback);
    }

    public void deleteSubtask(
            String subtaskId,
            String userId,
            RepositoryCallback<Boolean> callback
    ) {
        executeInBackground(() -> {
            TaskSubtask subtask = subtaskDao.findById(subtaskId);
            if (subtask == null) {
                throw new IllegalStateException("Bước thực hiện không tồn tại");
            }
            Task task = requireTask(subtask.getTaskId(), userId);
            if (!canEditSubtask(task, subtask, userId)) {
                throw new SecurityException("Bạn không có quyền xóa bước này");
            }
            long now = System.currentTimeMillis();
            subtask.setDeletedAt(now);
            subtask.setUpdatedAt(now);
            subtask.setVersion(Math.max(1, subtask.getVersion() + 1));
            subtask.setSyncStatus(SyncStatus.PENDING);
            if (subtaskDao.softDelete(subtaskId, now, subtask.getVersion()) <= 0) {
                throw new IllegalStateException("Không thể xóa bước thực hiện");
            }
            recordHistory(task.getTaskId(), userId, TaskHistoryAction.UPDATED,
                    "Đã xóa bước: " + subtask.getTitle());
            cloudSync.upsertSubtask(subtask);
            recalculateParentIfNeeded(task);
            return true;
        }, callback);
    }

    private void recalculateParent(Task task) {
        List<TaskSubtask> subtasks = subtaskDao.findAllByTask(task.getTaskId());
        TaskSubtaskRules.applyToTask(task, subtasks);
        long now = System.currentTimeMillis();
        task.setUpdatedAt(now);
        task.setVersion(Math.max(1, task.getVersion() + 1));
        task.setSyncStatus(SyncStatus.PENDING);
        task.setCompletedAt(TaskStatus.COMPLETED.equals(task.getStatus())
                ? (task.getCompletedAt() > 0 ? task.getCompletedAt() : now) : 0);
        taskDao.update(task);
        cloudSync.upsertTask(task, resolveAssigneeId(task));
        reminderScheduler.schedule(task);
    }

    private void recalculateParentIfNeeded(Task task) {
        if (!subtaskDao.findAllByTask(task.getTaskId()).isEmpty()) {
            recalculateParent(task);
            return;
        }

        // Khi checklist cuối cùng bị xóa, công việc quay về chế độ thủ công
        // với trạng thái ban đầu an toàn thay vì giữ lại 100%/Hoàn thành.
        task.setStatus(TaskStatus.TODO);
        task.setProgress(0);
        task.setUpdatedAt(System.currentTimeMillis());
        task.setCompletedAt(0);
        task.setVersion(Math.max(1, task.getVersion() + 1));
        task.setSyncStatus(SyncStatus.PENDING);
        taskDao.update(task);
        cloudSync.upsertTask(task, resolveAssigneeId(task));
        reminderScheduler.schedule(task);
    }

    private String resolveAssigneeId(Task task) {
        if (task.getProjectId() != null && !task.getProjectId().isBlank()) {
            return teamDao.findTaskAssigneeId(task.getTaskId());
        }
        return task.getCreatedBy();
    }

    private Task requireTask(String taskId, String userId) {
        if (isBlank(taskId) || isBlank(userId)) {
            throw new IllegalArgumentException("Thiếu thông tin công việc");
        }
        Task task = taskDao.findById(taskId);
        if (task == null) {
            throw new IllegalStateException("Công việc không tồn tại");
        }
        if (task.getProjectId() != null && !task.getProjectId().isBlank()) {
            TeamTaskItem item = teamDao.findTeamTaskById(taskId);
            WorkspaceMember member = teamDao.findMember(task.getWorkspaceId(), userId);
            if (item == null || member == null || !"ACTIVE".equals(member.getStatus()) ||
                    !TeamRules.canEditTask(member.getRole(), userId, task,
                            item.getAssigneeIds())) {
                throw new SecurityException("Bạn không có quyền chỉnh sửa checklist nhóm");
            }
        } else if (!userId.equals(task.getCreatedBy())) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa công việc này");
        }
        return task;
    }

    private boolean canEditSubtask(Task task, TaskSubtask subtask, String userId) {
        if (task.getProjectId() == null || task.getProjectId().isBlank()) {
            return userId.equals(subtask.getCreatedBy());
        }
        WorkspaceMember member = teamDao.findMember(task.getWorkspaceId(), userId);
        TeamTaskItem item = teamDao.findTeamTaskById(task.getTaskId());
        return member != null && item != null && "ACTIVE".equals(member.getStatus()) &&
                (TeamRules.canManageProjects(member.getRole()) ||
                        userId.equals(task.getCreatedBy()) ||
                        item.getAssigneeIds().contains(userId) ||
                        userId.equals(subtask.getAssigneeId()));
    }

    private void recordHistory(String taskId, String userId, String action, String detail) {
        TaskHistory history = historyDao.add(taskId, userId, action, detail);
        cloudSync.upsertTaskHistory(history);
    }

    private String requireTitle(String title) {
        if (isBlank(title)) {
            throw new IllegalArgumentException("Vui lòng nhập tên bước thực hiện");
        }
        String value = title.trim();
        if (value.length() > 200) {
            throw new IllegalArgumentException("Tên bước không được quá 200 ký tự");
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private <T> void executeInBackground(
            Callable<T> operation,
            RepositoryCallback<T> callback
    ) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback không được để trống");
        }
        executors.database().execute(() -> {
            try {
                T result = operation.call();
                executors.mainThread().execute(() -> callback.onSuccess(result));
            } catch (Exception exception) {
                executors.mainThread().execute(() -> callback.onError(exception));
            }
        });
    }
}
