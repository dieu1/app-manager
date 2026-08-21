package com.vandieu_manhdung.taskmanager.ui.personal.task.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.reponsitory.TaskRepository;
import com.vandieu_manhdung.taskmanager.data.reponsitory.TaskSubtaskRepository;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;

import java.util.Collections;
import java.util.List;

public class TaskDetailViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final TaskSubtaskRepository subtaskRepository;
    private final MutableLiveData<Task> task = new MutableLiveData<>();
    private final MutableLiveData<List<TaskSubtask>> subtasks =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleting = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public TaskDetailViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        subtaskRepository = new TaskSubtaskRepository(application);
    }

    public LiveData<Task> getTask() {
        return task;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<List<TaskSubtask>> getSubtasks() {
        return subtasks;
    }

    public LiveData<Boolean> getDeleting() {
        return deleting;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadTask(String taskId) {
        loading.setValue(true);
        taskRepository.getPersonalTaskById(taskId, new RepositoryCallback<Task>() {
            @Override
            public void onSuccess(Task result) {
                loading.setValue(false);
                task.setValue(result);
            }

            @Override
            public void onError(Exception exception) {
                loading.setValue(false);
                error.setValue(exception.getMessage());
            }
        });
    }

    public void loadDetails(String taskId, String userId) {
        loadTask(taskId);
        loadSubtasks(taskId, userId);
    }

    public void loadSubtasks(String taskId, String userId) {
        subtaskRepository.getSubtasks(taskId, userId, new RepositoryCallback<List<TaskSubtask>>() {
            @Override
            public void onSuccess(List<TaskSubtask> result) {
                subtasks.setValue(result == null ? Collections.emptyList() : result);
            }

            @Override
            public void onError(Exception exception) {
                error.setValue(exception.getMessage());
            }
        });
    }

    public void createSubtask(
            String taskId,
            String userId,
            String title,
            int estimatedMinutes
    ) {
        subtaskRepository.createSubtask(
                taskId,
                userId,
                title,
                estimatedMinutes,
                actionCallback(taskId, userId)
        );
    }

    public void toggleSubtask(
            String taskId,
            String userId,
            String subtaskId,
            boolean completed
    ) {
        subtaskRepository.toggleSubtask(
                subtaskId,
                userId,
                completed,
                actionCallback(taskId, userId)
        );
    }

    public void deleteSubtask(
            String taskId,
            String userId,
            String subtaskId
    ) {
        subtaskRepository.deleteSubtask(
                subtaskId,
                userId,
                actionCallback(taskId, userId)
        );
    }

    private RepositoryCallback<Boolean> actionCallback(String taskId, String userId) {
        return new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loadDetails(taskId, userId);
            }

            @Override
            public void onError(Exception exception) {
                error.setValue(exception.getMessage());
            }
        };
    }

    public void deleteTask(String taskId) {
        deleting.setValue(true);
        taskRepository.deletePersonalTask(taskId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                deleting.setValue(false);
                deleted.setValue(Boolean.TRUE.equals(result));
            }

            @Override
            public void onError(Exception exception) {
                deleting.setValue(false);
                error.setValue(exception.getMessage());
            }
        });
    }

    public void clearDeleted() {
        deleted.setValue(false);
    }

    public void clearError() {
        error.setValue(null);
    }
}
