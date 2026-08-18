package com.vandieu_manhdung.taskmanager.ui.personal.task.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.reponsitory.TaskRepository;
import com.vandieu_manhdung.taskmanager.model.Task;

public class TaskDetailViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final MutableLiveData<Task> task = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleting = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public TaskDetailViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
    }

    public LiveData<Task> getTask() {
        return task;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
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
