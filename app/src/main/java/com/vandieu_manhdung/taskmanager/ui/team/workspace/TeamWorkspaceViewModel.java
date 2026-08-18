package com.vandieu_manhdung.taskmanager.ui.team.workspace;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.sync.SyncBus;
import com.vandieu_manhdung.taskmanager.data.reponsitory.TeamRepository;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

public class TeamWorkspaceViewModel extends AndroidViewModel {

    private final TeamRepository repository;
    private final Observer<Long> syncObserver = ignored -> load();
    private final MutableLiveData<TeamWorkspaceSnapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);

    private String workspaceId;
    private String userId;
    private String projectFilter;
    private String assigneeFilter;
    private String statusFilter;

    public TeamWorkspaceViewModel(@NonNull Application application) {
        super(application);
        repository = new TeamRepository(application);
        SyncBus.getInstance().changes().observeForever(syncObserver);
    }

    public void initialize(String workspaceId, String userId) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        load();
    }

    public void load() {
        if (workspaceId == null || userId == null) {
            return;
        }
        loading.setValue(true);
        repository.getTeamSnapshot(
                workspaceId,
                userId,
                projectFilter,
                assigneeFilter,
                statusFilter,
                callback(result -> snapshot.setValue(result), null)
        );
    }

    public void setFilters(String projectId, String assigneeId, String status) {
        projectFilter = projectId;
        assigneeFilter = assigneeId;
        statusFilter = status;
        load();
    }

    public void updateTeam(String name, String description) {
        loading.setValue(true);
        repository.updateTeam(
                workspaceId,
                userId,
                name,
                description,
                callback(result -> {
                    message.setValue("Đã cập nhật nhóm");
                    load();
                }, null)
        );
    }

    public void deleteTeam() {
        loading.setValue(true);
        repository.deleteTeam(
                workspaceId,
                userId,
                callback(result -> {
                    loading.setValue(false);
                    deleted.setValue(true);
                }, null)
        );
    }

    public void addMember(String userCode, String role) {
        loading.setValue(true);
        repository.addMember(
                workspaceId,
                userId,
                userCode,
                role,
                callback(result -> {
                    message.setValue("Đã thêm thành viên");
                    load();
                }, null)
        );
    }

    public void changeMemberRole(String memberId, String role) {
        loading.setValue(true);
        repository.changeMemberRole(
                workspaceId,
                userId,
                memberId,
                role,
                callback(result -> {
                    message.setValue("Đã cập nhật vai trò");
                    load();
                }, null)
        );
    }

    public void removeMember(String memberId) {
        loading.setValue(true);
        repository.removeMember(
                workspaceId,
                userId,
                memberId,
                callback(result -> {
                    message.setValue("Đã xóa thành viên khỏi nhóm");
                    load();
                }, null)
        );
    }

    public void createProject(String name, String description) {
        loading.setValue(true);
        repository.createProject(
                workspaceId,
                userId,
                name,
                description,
                callback(result -> {
                    message.setValue("Đã tạo dự án");
                    load();
                }, null)
        );
    }

    public LiveData<TeamWorkspaceSnapshot> getSnapshot() {
        return snapshot;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getDeleted() {
        return deleted;
    }

    public void clearError() {
        error.setValue(null);
    }

    public void clearMessage() {
        message.setValue(null);
    }

    public void clearDeleted() {
        deleted.setValue(false);
    }

    private <T> RepositoryCallback<T> callback(
            SuccessHandler<T> successHandler,
            String successMessage
    ) {
        return new RepositoryCallback<T>() {
            @Override
            public void onSuccess(T result) {
                loading.setValue(false);
                if (successMessage != null) {
                    message.setValue(successMessage);
                }
                successHandler.onSuccess(result);
            }

            @Override
            public void onError(Exception exception) {
                loading.setValue(false);
                error.setValue(exception.getMessage());
            }
        };
    }

    private interface SuccessHandler<T> {
        void onSuccess(T result);
    }

    @Override
    protected void onCleared() {
        SyncBus.getInstance().changes().removeObserver(syncObserver);
    }
}
