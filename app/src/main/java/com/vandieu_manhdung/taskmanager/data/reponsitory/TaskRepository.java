package com.vandieu_manhdung.taskmanager.data.reponsitory;
import android.content.Context;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceType;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkspaceDao;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.PersonalDashboardSummary;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.Workspace;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public class TaskRepository {

    private final TaskDao taskDao;
    private final WorkspaceDao workspaceDao;
    private final AppExecutors executors;
    private final CloudSyncManager cloudSync;

    public TaskRepository(Context context) {
        Context applicationContext =
                context.getApplicationContext();

        taskDao = new TaskDao(applicationContext);
        workspaceDao = new WorkspaceDao(applicationContext);
        executors = AppExecutors.getInstance();
        cloudSync = CloudSyncManager.getInstance(applicationContext);
    }

    /*
     * =========================================================
     * THÊM CÔNG VIỆC CÁ NHÂN
     * =========================================================
     */

    public void createPersonalTask(
            Task task,
            RepositoryCallback<Task> callback
    ) {
        executeInBackground(() -> {
            prepareForCreate(task);

            boolean inserted = taskDao.insert(task);

            if (!inserted) {
                throw new IllegalStateException(
                        "Không thể thêm công việc"
                );
            }

            cloudSync.upsertTask(task, task.getCreatedBy());
            return task;
        }, callback);
    }

    /*
     * =========================================================
     * CẬP NHẬT CÔNG VIỆC
     * =========================================================
     */

    public void updatePersonalTask(
            Task task,
            RepositoryCallback<Task> callback
    ) {
        executeInBackground(() -> {
            if (task == null ||
                    isBlank(task.getTaskId())) {
                throw new IllegalArgumentException(
                        "Task ID không được để trống"
                );
            }

            Task existingTask =
                    taskDao.findById(task.getTaskId());

            if (existingTask == null) {
                throw new IllegalStateException(
                        "Công việc không tồn tại"
                );
            }

            ensurePersonalTask(existingTask);

            // Không cho phép giao diện thay đổi các trường bất biến.
            task.setWorkspaceId(
                    existingTask.getWorkspaceId()
            );

            task.setProjectId(null);

            task.setCreatedBy(
                    existingTask.getCreatedBy()
            );

            task.setCreatedAt(
                    existingTask.getCreatedAt()
            );

            prepareCommonFields(task);
            validatePersonalOwnership(task);

            task.setUpdatedAt(
                    System.currentTimeMillis()
            );

            int updatedRows = taskDao.update(task);

            if (updatedRows <= 0) {
                throw new IllegalStateException(
                        "Không thể cập nhật công việc"
                );
            }

            cloudSync.upsertTask(task, task.getCreatedBy());
            return task;
        }, callback);
    }

    /*
     * =========================================================
     * XÓA CÔNG VIỆC
     * =========================================================
     */

    public void deletePersonalTask(
            String taskId,
            RepositoryCallback<Boolean> callback
    ) {
        executeInBackground(() -> {
            requireNotBlank(
                    taskId,
                    "Task ID không được để trống"
            );

            Task existingTask =
                    taskDao.findById(taskId);

            if (existingTask == null) {
                throw new IllegalStateException(
                        "Công việc không tồn tại"
                );
            }

            ensurePersonalTask(existingTask);

            int deletedRows = taskDao.delete(taskId);

            if (deletedRows <= 0) {
                throw new IllegalStateException(
                        "Không thể xóa công việc"
                );
            }

            cloudSync.deleteTask(taskId);
            return true;
        }, callback);
    }

    /*
     * =========================================================
     * LẤY CHI TIẾT CÔNG VIỆC
     * =========================================================
     */

    public void getPersonalTaskById(
            String taskId,
            RepositoryCallback<Task> callback
    ) {
        executeInBackground(() -> {
            requireNotBlank(
                    taskId,
                    "Task ID không được để trống"
            );

            Task task = taskDao.findById(taskId);

            if (task == null) {
                throw new IllegalStateException(
                        "Không tìm thấy công việc"
                );
            }

            ensurePersonalTask(task);

            return task;
        }, callback);
    }

    /*
     * =========================================================
     * TÌM KIẾM, LỌC VÀ SẮP XẾP
     * =========================================================
     */

    public void getPersonalTasks(
            String workspaceId,
            String keyword,
            String status,
            String priority,
            String sortOption,
            boolean ascending,
            RepositoryCallback<List<Task>> callback
    ) {
        executeInBackground(() -> {
            ensurePersonalWorkspace(workspaceId);

            validateStatusFilter(status);
            validatePriorityFilter(priority);

            return taskDao.queryPersonalTasks(
                    workspaceId,
                    keyword,
                    status,
                    priority,
                    sortOption,
                    ascending
            );
        }, callback);
    }

    public void getAllPersonalTasks(
            String workspaceId,
            RepositoryCallback<List<Task>> callback
    ) {
        executeInBackground(() -> {
            ensurePersonalWorkspace(workspaceId);

            return taskDao.findAllPersonalTasks(
                    workspaceId
            );
        }, callback);
    }

    /*
     * =========================================================
     * CẬP NHẬT TRẠNG THÁI VÀ TIẾN ĐỘ
     * =========================================================
     */

    public void updateStatusAndProgress(
            String taskId,
            String status,
            int progress,
            RepositoryCallback<Task> callback
    ) {
        executeInBackground(() -> {
            requireNotBlank(
                    taskId,
                    "Task ID không được để trống"
            );

            validateStatus(status);

            Task existingTask =
                    taskDao.findById(taskId);

            if (existingTask == null) {
                throw new IllegalStateException(
                        "Không tìm thấy công việc"
                );
            }

            ensurePersonalTask(existingTask);

            int normalizedProgress =
                    normalizeProgress(status, progress);

            int updatedRows =
                    taskDao.updateStatusAndProgress(
                            taskId,
                            status,
                            normalizedProgress
                    );

            if (updatedRows <= 0) {
                throw new IllegalStateException(
                        "Không thể cập nhật trạng thái"
                );
            }

            existingTask.setStatus(status);
            existingTask.setProgress(
                    normalizedProgress
            );

            existingTask.setUpdatedAt(
                    System.currentTimeMillis()
            );

            cloudSync.upsertTask(existingTask, existingTask.getCreatedBy());
            return existingTask;
        }, callback);
    }

    /*
     * =========================================================
     * CÔNG VIỆC QUÁ HẠN
     * =========================================================
     */

    public void getOverduePersonalTasks(
            String workspaceId,
            RepositoryCallback<List<Task>> callback
    ) {
        executeInBackground(() -> {
            ensurePersonalWorkspace(workspaceId);

            return taskDao.findOverduePersonalTasks(
                    workspaceId,
                    System.currentTimeMillis()
            );
        }, callback);
    }

    /*
     * =========================================================
     * THỐNG KÊ
     * =========================================================
     */

    public void countPersonalTasksByStatus(
            String workspaceId,
            String status,
            RepositoryCallback<Integer> callback
    ) {
        executeInBackground(() -> {
            ensurePersonalWorkspace(workspaceId);
            validateStatus(status);

            return taskDao.countPersonalTasksByStatus(
                    workspaceId,
                    status
            );
        }, callback);
    }

    public void countAllPersonalTasks(
            String workspaceId,
            RepositoryCallback<Integer> callback
    ) {
        executeInBackground(() -> {
            ensurePersonalWorkspace(workspaceId);

            return taskDao.countPersonalTasks(
                    workspaceId
            );
        }, callback);
    }

    public void getPersonalDashboardSummary(
            String workspaceId,
            RepositoryCallback<PersonalDashboardSummary> callback
    ) {
        executeInBackground(() -> {
            ensurePersonalWorkspace(workspaceId);

            long currentTime = System.currentTimeMillis();

            return new PersonalDashboardSummary(
                    taskDao.countPersonalTasks(workspaceId),
                    taskDao.countPersonalTasksByStatus(
                            workspaceId,
                            TaskStatus.TODO
                    ),
                    taskDao.countPersonalTasksByStatus(
                            workspaceId,
                            TaskStatus.IN_PROGRESS
                    ),
                    taskDao.countPersonalTasksByStatus(
                            workspaceId,
                            TaskStatus.COMPLETED
                    ),
                    taskDao.countPersonalTasksByStatus(
                            workspaceId,
                            TaskStatus.CANCELLED
                    ),
                    taskDao.countOverduePersonalTasks(
                            workspaceId,
                            currentTime
                    )
            );
        }, callback);
    }

    /*
     * =========================================================
     * CHUẨN BỊ DỮ LIỆU KHI THÊM
     * =========================================================
     */

    private void prepareForCreate(Task task) {
        if (task == null) {
            throw new IllegalArgumentException(
                    "Công việc không được để trống"
            );
        }

        requireNotBlank(
                task.getWorkspaceId(),
                "Workspace ID không được để trống"
        );

        requireNotBlank(
                task.getCreatedBy(),
                "Người tạo không được để trống"
        );

        validatePersonalOwnership(task);

        if (isBlank(task.getTaskId())) {
            task.setTaskId(
                    UUID.randomUUID().toString()
            );
        } else if (taskDao.existsById(task.getTaskId())) {
            throw new IllegalStateException(
                    "Task ID đã tồn tại"
            );
        }

        // Công việc cá nhân không thuộc dự án.
        task.setProjectId(null);

        prepareCommonFields(task);

        long currentTime =
                System.currentTimeMillis();

        if (task.getCreatedAt() <= 0) {
            task.setCreatedAt(currentTime);
        }

        task.setUpdatedAt(currentTime);
    }

    /*
     * =========================================================
     * CHUẨN HÓA CÁC TRƯỜNG CHUNG
     * =========================================================
     */

    private void prepareCommonFields(Task task) {
        requireNotBlank(
                task.getTitle(),
                "Tên công việc không được để trống"
        );

        String normalizedTitle =
                task.getTitle().trim();

        if (normalizedTitle.length() > 200) {
            throw new IllegalArgumentException(
                    "Tên công việc không được quá 200 ký tự"
            );
        }

        task.setTitle(normalizedTitle);

        if (task.getDescription() != null) {
            String description =
                    task.getDescription().trim();

            if (description.length() > 2000) {
                throw new IllegalArgumentException(
                        "Mô tả không được quá 2000 ký tự"
                );
            }

            task.setDescription(description);
        }

        if (isBlank(task.getStatus())) {
            task.setStatus(TaskStatus.TODO);
        }

        if (isBlank(task.getPriority())) {
            task.setPriority(TaskPriority.MEDIUM);
        }

        validateStatus(task.getStatus());
        validatePriority(task.getPriority());

        task.setProgress(
                normalizeProgress(
                        task.getStatus(),
                        task.getProgress()
                )
        );

        if (task.getEstimatedMinutes() < 0) {
            throw new IllegalArgumentException(
                    "Thời gian dự kiến không được âm"
            );
        }

        if (task.getStartDate() > 0 &&
                task.getDueDate() > 0 &&
                task.getDueDate() < task.getStartDate()) {
            throw new IllegalArgumentException(
                    "Hạn hoàn thành phải sau ngày bắt đầu"
            );
        }
    }

    /*
     * =========================================================
     * KIỂM TRA WORKSPACE VÀ QUYỀN SỞ HỮU
     * =========================================================
     */

    private Workspace ensurePersonalWorkspace(
            String workspaceId
    ) {
        requireNotBlank(
                workspaceId,
                "Workspace ID không được để trống"
        );

        Workspace workspace =
                workspaceDao.findById(workspaceId);

        if (workspace == null) {
            throw new IllegalStateException(
                    "Workspace không tồn tại"
            );
        }

        if (!WorkspaceType.PERSONAL.equals(
                workspace.getType()
        )) {
            throw new IllegalStateException(
                    "Workspace không phải không gian cá nhân"
            );
        }

        return workspace;
    }

    private void validatePersonalOwnership(Task task) {
        Workspace workspace =
                ensurePersonalWorkspace(
                        task.getWorkspaceId()
                );

        if (!workspace.getManagerId().equals(
                task.getCreatedBy()
        )) {
            throw new SecurityException(
                    "Người dùng không sở hữu Workspace này"
            );
        }
    }

    private void ensurePersonalTask(Task task) {
        if (task.getProjectId() != null &&
                !task.getProjectId().isBlank()) {
            throw new IllegalStateException(
                    "Đây không phải công việc cá nhân"
            );
        }

        validatePersonalOwnership(task);
    }

    /*
     * =========================================================
     * CHUẨN HÓA TIẾN ĐỘ
     * =========================================================
     */

    private int normalizeProgress(
            String status,
            int progress
    ) {
        return TaskRules.normalizeProgress(status, progress);
    }

    /*
     * =========================================================
     * KIỂM TRA TRẠNG THÁI VÀ ĐỘ ƯU TIÊN
     * =========================================================
     */

    private void validateStatus(String status) {
        if (!TaskRules.isValidStatus(status)) {
            throw new IllegalArgumentException(
                    "Trạng thái công việc không hợp lệ"
            );
        }
    }

    private void validatePriority(String priority) {
        if (!TaskRules.isValidPriority(priority)) {
            throw new IllegalArgumentException(
                    "Độ ưu tiên không hợp lệ"
            );
        }
    }

    private void validateStatusFilter(String status) {
        if (status == null ||
                status.isBlank() ||
                "ALL".equalsIgnoreCase(status)) {
            return;
        }

        validateStatus(status);
    }

    private void validatePriorityFilter(String priority) {
        if (priority == null ||
                priority.isBlank() ||
                "ALL".equalsIgnoreCase(priority)) {
            return;
        }

        validatePriority(priority);
    }

    /*
     * =========================================================
     * CHẠY TÁC VỤ TRÊN LUỒNG NỀN
     * =========================================================
     */

    private <T> void executeInBackground(
            Callable<T> operation,
            RepositoryCallback<T> callback
    ) {
        if (callback == null) {
            throw new IllegalArgumentException(
                    "Callback không được để trống"
            );
        }

        executors.database().execute(() -> {
            try {
                T result = operation.call();

                executors.mainThread().execute(
                        () -> callback.onSuccess(result)
                );

            } catch (Exception exception) {
                executors.mainThread().execute(
                        () -> callback.onError(exception)
                );
            }
        });
    }

    private boolean isBlank(String value) {
        return value == null ||
                value.trim().isEmpty();
    }

    private void requireNotBlank(
            String value,
            String errorMessage
    ) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(
                    errorMessage
            );
        }
    }
}
