package com.vandieu_manhdung.taskmanager.ui.team.task;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.repository.TeamRepository;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;

import java.util.List;

public class TeamTaskFormViewModel extends AndroidViewModel {

    private final TeamRepository repository;
    private final MutableLiveData<TeamWorkspaceSnapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<TeamTaskItem> editingItem = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private String workspaceId;
    private String userId;
    private String taskId;

    public TeamTaskFormViewModel(@NonNull Application application) {
        super(application);
        repository = new TeamRepository(application);
    }

    public void initialize(String workspaceId, String userId, String taskId) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.taskId = taskId;
        load();
    }

    public void load() {
        loading.setValue(true);
        repository.getTeamSnapshot(
                workspaceId,
                userId,
                null,
                null,
                null,
                new RepositoryCallback<TeamWorkspaceSnapshot>() {
                    @Override
                    public void onSuccess(TeamWorkspaceSnapshot result) {
                        loading.setValue(false);
                        snapshot.setValue(result);
                        if (taskId != null && !taskId.isBlank()) {
                            for (TeamTaskItem item : result.getTasks()) {
                                if (taskId.equals(item.getTask().getTaskId())) {
                                    editingItem.setValue(item);
                                    return;
                                }
                            }
                            error.setValue("Không tìm thấy công việc nhóm");
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        loading.setValue(false);
                        error.setValue(exception.getMessage());
                    }
                }
        );
    }

    public void save(
            String title,
            String description,
            String status,
            String priority,
            int progress,
            long startDate,
            long dueDate,
            int estimatedMinutes,
            String projectId,
            List<String> assigneeIds
    ) {
        Task task;
        TeamTaskItem existing = editingItem.getValue();
        if (taskId != null && !taskId.isBlank()) {
            if (existing == null) {
                error.setValue("Chưa tải xong công việc");
                return;
            }
            task = existing.getTask();
        } else {
            task = new Task();
            task.setWorkspaceId(workspaceId);
        }
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setProgress(progress);
        task.setStartDate(startDate);
        task.setDueDate(dueDate);
        task.setEstimatedMinutes(estimatedMinutes);

        loading.setValue(true);
        repository.saveTeamTask(task, assigneeIds, userId, new RepositoryCallback<Task>() {
            @Override
            public void onSuccess(Task result) {
                loading.setValue(false);
                saved.setValue(true);
            }

            @Override
            public void onError(Exception exception) {
                loading.setValue(false);
                error.setValue(exception.getMessage());
            }
        });
    }

    public void delete() {
        if (taskId == null || taskId.isBlank()) {
            return;
        }
        loading.setValue(true);
        repository.deleteTeamTask(taskId, userId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                loading.setValue(false);
                deleted.setValue(true);
            }

            @Override
            public void onError(Exception exception) {
                loading.setValue(false);
                error.setValue(exception.getMessage());
            }
        });
    }

    public LiveData<TeamWorkspaceSnapshot> getSnapshot() {
        return snapshot;
    }

    public LiveData<TeamTaskItem> getEditingItem() {
        return editingItem;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void clearResult() {
        saved.setValue(false);
        deleted.setValue(false);
    }

    public void clearError() {
        error.setValue(null);
    }
}
