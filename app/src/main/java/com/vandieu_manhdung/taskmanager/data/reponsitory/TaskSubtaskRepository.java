package com.vandieu_manhdung.taskmanager.data.reponsitory;

import android.content.Context;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.notification.TaskNotificationManager;
import com.vandieu_manhdung.taskmanager.core.notification.TaskReminderScheduler;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.TaskSubtaskRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskSubtaskDao;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public class TaskSubtaskRepository {

    private final Context context;
    private final TaskDao taskDao;
    private final TaskSubtaskDao subtaskDao;
    private final AppExecutors executors;
    private final CloudSyncManager cloudSync;
    private final TaskReminderScheduler reminderScheduler;

    public TaskSubtaskRepository(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext;
        taskDao = new TaskDao(applicationContext);
        subtaskDao = new TaskSubtaskDao(applicationContext);
        executors = AppExecutors.getInstance();
        cloudSync = CloudSyncManager.getInstance(applicationContext);
        reminderScheduler = new TaskReminderScheduler(applicationContext);
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
            subtask.setTitle(cleanTitle);
            subtask.setEstimatedMinutes(estimatedMinutes);
            subtask.setCompleted(false);
            subtask.setSortOrder(subtaskDao.nextSortOrder(taskId));
            subtask.setCreatedAt(now);
            subtask.setUpdatedAt(now);

            if (!subtaskDao.insert(subtask)) {
                throw new IllegalStateException("Không thể thêm bước thực hiện");
            }
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
            if (!userId.equals(subtask.getCreatedBy())) {
                throw new SecurityException("Bạn không có quyền cập nhật bước này");
            }

            subtask.setCompleted(completed);
            subtask.setUpdatedAt(System.currentTimeMillis());
            if (subtaskDao.update(subtask) <= 0) {
                throw new IllegalStateException("Không thể cập nhật bước thực hiện");
            }
            cloudSync.upsertSubtask(subtask);
            recalculateParent(task);
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
            if (!userId.equals(subtask.getCreatedBy())) {
                throw new SecurityException("Bạn không có quyền xóa bước này");
            }
            if (subtaskDao.delete(subtaskId) <= 0) {
                throw new IllegalStateException("Không thể xóa bước thực hiện");
            }
            cloudSync.deleteSubtask(subtaskId);
            recalculateParentIfNeeded(task);
            return true;
        }, callback);
    }

    private void recalculateParent(Task task) {
        List<TaskSubtask> subtasks = subtaskDao.findAllByTask(task.getTaskId());
        TaskSubtaskRules.applyToTask(task, subtasks);
        task.setUpdatedAt(System.currentTimeMillis());
        taskDao.updateStatusAndProgress(
                task.getTaskId(),
                task.getStatus(),
                task.getProgress()
        );
        cloudSync.upsertTask(task, task.getCreatedBy());
        reminderScheduler.schedule(task);
    }

    private void recalculateParentIfNeeded(Task task) {
        if (!subtaskDao.findAllByTask(task.getTaskId()).isEmpty()) {
            recalculateParent(task);
            return;
        }

        // Khi checklist cuối cùng bị xóa, công việc quay về chế độ thủ công
        // với trạng thái ban đầu an toàn thay vì giữ lại 100%/Hoàn thành.
        task.setStatus(com.vandieu_manhdung.taskmanager.core.constant.TaskStatus.TODO);
        task.setProgress(0);
        task.setUpdatedAt(System.currentTimeMillis());
        taskDao.updateStatusAndProgress(task.getTaskId(), task.getStatus(), task.getProgress());
        cloudSync.upsertTask(task, task.getCreatedBy());
        reminderScheduler.schedule(task);
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
            throw new IllegalStateException("Chỉ công việc cá nhân mới có checklist");
        }
        if (!userId.equals(task.getCreatedBy())) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa công việc này");
        }
        return task;
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
