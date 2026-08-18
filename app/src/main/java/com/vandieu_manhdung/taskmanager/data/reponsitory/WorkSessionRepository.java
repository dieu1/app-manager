package com.vandieu_manhdung.taskmanager.data.reponsitory;

import android.content.Context;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.WorkSessionRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkSessionDao;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.WorkSession;
import com.vandieu_manhdung.taskmanager.model.WorkTimerState;

import java.util.UUID;
import java.util.concurrent.Callable;

public class WorkSessionRepository {

    private final TaskDao taskDao;
    private final WorkSessionDao workSessionDao;
    private final AppExecutors executors;
    private final CloudSyncManager cloudSync;

    public WorkSessionRepository(Context context) {
        Context applicationContext = context.getApplicationContext();
        taskDao = new TaskDao(applicationContext);
        workSessionDao = new WorkSessionDao(applicationContext);
        executors = AppExecutors.getInstance();
        cloudSync = CloudSyncManager.getInstance(applicationContext);
    }

    public void getTimerState(
            String taskId,
            String userId,
            RepositoryCallback<WorkTimerState> callback
    ) {
        executeInBackground(() -> {
            ensurePersonalTaskOwner(taskId, userId);
            WorkSession activeSession = workSessionDao.findActiveByUser(userId);
            if (activeSession != null &&
                    !taskId.equals(activeSession.getTaskId())) {
                activeSession = null;
            }

            return new WorkTimerState(
                    activeSession,
                    workSessionDao.totalMinutes(taskId, userId)
            );
        }, callback);
    }

    public void start(
            String taskId,
            String userId,
            RepositoryCallback<WorkTimerState> callback
    ) {
        executeInBackground(() -> {
            Task task = ensurePersonalTaskOwner(taskId, userId);

            WorkSession activeSession = workSessionDao.findActiveByUser(userId);
            if (activeSession != null) {
                if (!taskId.equals(activeSession.getTaskId())) {
                    throw new IllegalStateException(
                            "Bạn đang bấm giờ cho một công việc khác"
                    );
                }

                return new WorkTimerState(
                        activeSession,
                        workSessionDao.totalMinutes(taskId, userId)
                );
            }

            WorkSession session = new WorkSession();
            session.setSessionId(UUID.randomUUID().toString());
            session.setTaskId(taskId);
            session.setUserId(userId);
            session.setStartTime(System.currentTimeMillis());

            if (!workSessionDao.insert(session)) {
                throw new IllegalStateException("Không thể bắt đầu bấm giờ");
            }
            cloudSync.upsertWorkSession(session, task.getWorkspaceId());

            return new WorkTimerState(
                    session,
                    workSessionDao.totalMinutes(taskId, userId)
            );
        }, callback);
    }

    public void stop(
            String taskId,
            String userId,
            RepositoryCallback<WorkTimerState> callback
    ) {
        executeInBackground(() -> {
            Task task = ensurePersonalTaskOwner(taskId, userId);

            WorkSession activeSession = workSessionDao.findActiveByUser(userId);
            if (activeSession == null ||
                    !taskId.equals(activeSession.getTaskId())) {
                throw new IllegalStateException("Không có phiên đang chạy");
            }

            long endTime = System.currentTimeMillis();
            int durationMinutes = WorkSessionRules.calculateDurationMinutes(
                    activeSession.getStartTime(),
                    endTime
            );

            if (workSessionDao.stop(
                    activeSession.getSessionId(),
                    endTime,
                    durationMinutes
            ) <= 0) {
                throw new IllegalStateException("Không thể dừng bấm giờ");
            }

            activeSession.setEndTime(endTime);
            activeSession.setDurationMinutes(durationMinutes);
            cloudSync.upsertWorkSession(activeSession, task.getWorkspaceId());

            return new WorkTimerState(
                    null,
                    workSessionDao.totalMinutes(taskId, userId)
            );
        }, callback);
    }

    private Task ensurePersonalTaskOwner(
            String taskId,
            String userId
    ) {
        if (taskId == null || taskId.isBlank() ||
                userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Thiếu thông tin công việc");
        }

        Task task = taskDao.findById(taskId);
        if (task == null) {
            throw new IllegalStateException("Công việc không tồn tại");
        }
        if (task.getProjectId() != null ||
                !userId.equals(task.getCreatedBy())) {
            throw new SecurityException("Bạn không có quyền bấm giờ công việc này");
        }
        return task;
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
