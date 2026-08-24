package com.vandieu_manhdung.taskmanager.ui.personal.task.form;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.repository.TaskRepository;
import com.vandieu_manhdung.taskmanager.core.util.TaskScheduleRules;
import com.vandieu_manhdung.taskmanager.model.Task;

public class TaskFormViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;

    private final MutableLiveData<Boolean> saving =
            new MutableLiveData<>(false);

    private final MutableLiveData<Task> savedTask =
            new MutableLiveData<>();

    private final MutableLiveData<Task> editingTask =
            new MutableLiveData<>();

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    public TaskFormViewModel(
            @NonNull Application application
    ) {
        super(application);
        taskRepository = new TaskRepository(application);
    }

    public LiveData<Boolean> getSaving() {
        return saving;
    }

    public LiveData<Task> getSavedTask() {
        return savedTask;
    }

    public LiveData<Task> getEditingTask() {
        return editingTask;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadTask(String taskId) {
        taskRepository.getPersonalTaskById(
                taskId,
                new RepositoryCallback<Task>() {
                    @Override
                    public void onSuccess(Task result) {
                        editingTask.setValue(result);
                    }

                    @Override
                    public void onError(Exception exception) {
                        error.setValue(exception.getMessage());
                    }
                }
        );
    }

    public void savePersonalTask(
            String taskId,
            String workspaceId,
            String userId,
            String title,
            String description,
            String status,
            String priority,
            long startDate,
            long dueDate
    ) {
        boolean editing = taskId != null && !taskId.isBlank();
        Task task = editing ? editingTask.getValue() : new Task();

        if (task == null) {
            error.setValue("Chưa tải xong dữ liệu công việc");
            return;
        }

        if (!editing) {
            task.setWorkspaceId(workspaceId);
            task.setCreatedBy(userId);
            task.setProjectId(null);
        }

        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        if (!editing) {
            task.setProgress(0);
        }
        task.setStartDate(startDate);
        task.setDueDate(dueDate);
        task.setEstimatedMinutes(TaskScheduleRules.calculateEstimatedMinutes(
                startDate,
                dueDate
        ));

        saving.setValue(true);

        RepositoryCallback<Task> callback =
                new RepositoryCallback<Task>() {
                    @Override
                    public void onSuccess(Task result) {
                        saving.setValue(false);
                        savedTask.setValue(result);
                    }

                    @Override
                    public void onError(Exception exception) {
                        saving.setValue(false);
                        error.setValue(exception.getMessage());
                    }
                };

        if (editing) {
            taskRepository.updatePersonalTask(task, callback);
        } else {
            taskRepository.createPersonalTask(task, callback);
        }
    }

    public void clearSavedTask() {
        savedTask.setValue(null);
    }

    public void clearError() {
        error.setValue(null);
    }
}
