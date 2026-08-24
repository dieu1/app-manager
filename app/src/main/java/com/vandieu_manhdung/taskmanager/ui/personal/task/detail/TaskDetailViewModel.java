package com.vandieu_manhdung.taskmanager.ui.personal.task.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.repository.TaskRepository;
import com.vandieu_manhdung.taskmanager.data.repository.TaskSubtaskRepository;
import com.vandieu_manhdung.taskmanager.data.repository.TeamRepository;
import com.vandieu_manhdung.taskmanager.data.repository.TeamCollaborationRepository;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;
import com.vandieu_manhdung.taskmanager.model.TaskHistory;
import com.vandieu_manhdung.taskmanager.model.TaskComment;
import com.vandieu_manhdung.taskmanager.model.TaskAttachment;
import com.vandieu_manhdung.taskmanager.model.TaskDependency;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import android.net.Uri;

import java.util.Collections;
import java.util.List;

public class TaskDetailViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final TaskSubtaskRepository subtaskRepository;
    private final TeamRepository teamRepository;
    private final TeamCollaborationRepository collaborationRepository;
    private final MutableLiveData<Task> task = new MutableLiveData<>();
    private final MutableLiveData<List<TaskSubtask>> subtasks =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<TaskHistory>> history =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<TaskComment>> comments =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<TaskAttachment>> attachments =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<TaskDependency>> dependencies =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<TeamTaskItem>> dependencyCandidates =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleting = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private boolean currentTeamMode;

    public TaskDetailViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        subtaskRepository = new TaskSubtaskRepository(application);
        teamRepository = new TeamRepository(application);
        collaborationRepository = new TeamCollaborationRepository(application);
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

    public LiveData<List<TaskHistory>> getHistory() { return history; }
    public LiveData<List<TaskComment>> getComments() { return comments; }
    public LiveData<List<TaskAttachment>> getAttachments() { return attachments; }
    public LiveData<List<TaskDependency>> getDependencies() { return dependencies; }
    public LiveData<List<TeamTaskItem>> getDependencyCandidates() { return dependencyCandidates; }

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
        loadDetails(taskId, userId, false);
    }

    public void loadDetails(String taskId, String userId, boolean teamMode) {
        currentTeamMode = teamMode;
        if (teamMode) {
            loading.setValue(true);
            teamRepository.getTeamTask(taskId, userId,
                    new RepositoryCallback<com.vandieu_manhdung.taskmanager.model.TeamTaskItem>() {
                        @Override public void onSuccess(com.vandieu_manhdung.taskmanager.model.TeamTaskItem result) {
                            loading.setValue(false);
                            task.setValue(result.getTask());
                        }
                        @Override public void onError(Exception exception) {
                            loading.setValue(false);
                            error.setValue(exception.getMessage());
                        }
                    });
        } else {
            loadTask(taskId);
        }
        loadSubtasks(taskId, userId);
        loadHistory(taskId);
        if (teamMode) loadCollaboration(taskId, userId);
    }

    private void loadCollaboration(String taskId, String userId) {
        collaborationRepository.getComments(taskId, userId, listCallback(comments));
        collaborationRepository.getAttachments(taskId, userId, listCallback(attachments));
        collaborationRepository.getDependencies(taskId, userId, listCallback(dependencies));
        collaborationRepository.getDependencyCandidates(taskId, userId,
                listCallback(dependencyCandidates));
    }

    public void addComment(String taskId, String userId, String message) {
        collaborationRepository.addComment(taskId, userId, message,
                collaborationAction(taskId, userId));
    }

    public void editComment(String taskId, String commentId, String userId, String message) {
        collaborationRepository.editComment(commentId, userId, message,
                collaborationAction(taskId, userId));
    }

    public void deleteComment(String taskId, String commentId, String userId) {
        collaborationRepository.deleteComment(commentId, userId,
                collaborationAction(taskId, userId));
    }

    public void addAttachment(String taskId, String userId, Uri uri) {
        collaborationRepository.addAttachment(taskId, userId, uri,
                collaborationAction(taskId, userId));
    }

    public void addDependency(String taskId, String dependsOnId, String userId) {
        collaborationRepository.addDependency(taskId, dependsOnId, userId,
                collaborationAction(taskId, userId));
    }

    public void deleteDependency(String taskId, String dependsOnId, String userId) {
        collaborationRepository.deleteDependency(taskId, dependsOnId, userId,
                collaborationAction(taskId, userId));
    }

    private RepositoryCallback<Boolean> collaborationAction(String taskId, String userId) {
        return new RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) { loadCollaboration(taskId, userId); }
            @Override public void onError(Exception exception) { error.setValue(exception.getMessage()); }
        };
    }

    private <T> RepositoryCallback<List<T>> listCallback(MutableLiveData<List<T>> target) {
        return new RepositoryCallback<List<T>>() {
            @Override public void onSuccess(List<T> result) {
                target.setValue(result == null ? Collections.emptyList() : result);
            }
            @Override public void onError(Exception exception) { error.setValue(exception.getMessage()); }
        };
    }

    private void loadHistory(String taskId) {
        taskRepository.getTaskHistory(taskId, new RepositoryCallback<List<TaskHistory>>() {
            @Override public void onSuccess(List<TaskHistory> result) {
                history.setValue(result == null ? Collections.emptyList() : result);
            }
            @Override public void onError(Exception exception) {
                error.setValue(exception.getMessage());
            }
        });
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
                loadDetails(taskId, userId, currentTeamMode);
            }

            @Override
            public void onError(Exception exception) {
                error.setValue(exception.getMessage());
            }
        };
    }

    public void deleteTask(String taskId) {
        deleteTask(taskId, null, false);
    }

    public void deleteTask(String taskId, String userId, boolean teamMode) {
        deleting.setValue(true);
        if (teamMode) {
            teamRepository.deleteTeamTask(taskId, userId, deleteCallback());
            return;
        }
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

    private RepositoryCallback<Boolean> deleteCallback() {
        return new RepositoryCallback<Boolean>() {
            @Override public void onSuccess(Boolean result) {
                deleting.setValue(false);
                deleted.setValue(Boolean.TRUE.equals(result));
            }
            @Override public void onError(Exception exception) {
                deleting.setValue(false);
                error.setValue(exception.getMessage());
            }
        };
    }

    public void clearDeleted() {
        deleted.setValue(false);
    }

    public void clearError() {
        error.setValue(null);
    }
}
