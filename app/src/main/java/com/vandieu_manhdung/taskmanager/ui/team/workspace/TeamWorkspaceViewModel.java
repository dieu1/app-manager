package com.vandieu_manhdung.taskmanager.ui.team.workspace;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.sync.SyncBus;
import com.vandieu_manhdung.taskmanager.data.repository.TeamRepository;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.ProjectMilestone;
import java.util.List;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

public class TeamWorkspaceViewModel extends AndroidViewModel {

    private static final int PAGE_SIZE = 20;

    private final TeamRepository repository;
    private final Observer<Long> syncObserver = ignored -> load();
    private final MutableLiveData<TeamWorkspaceSnapshot> snapshot = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);
    private final MutableLiveData<List<ProjectMilestone>> milestones = new MutableLiveData<>();

    private String workspaceId;
    private String userId;
    private String projectFilter;
    private String assigneeFilter;
    private String statusFilter;
    private int visibleTaskLimit = PAGE_SIZE;

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
        repository.getTeamSnapshotPage(
                workspaceId,
                userId,
                projectFilter,
                assigneeFilter,
                statusFilter,
                visibleTaskLimit,
                callback(result -> snapshot.setValue(result), null)
        );
    }

    public void setFilters(String projectId, String assigneeId, String status) {
        projectFilter = projectId;
        assigneeFilter = assigneeId;
        statusFilter = status;
        visibleTaskLimit = PAGE_SIZE;
        load();
    }

    public void loadMoreTasks() {
        visibleTaskLimit += PAGE_SIZE;
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

    public void leaveTeam() {
        loading.setValue(true);
        repository.leaveTeam(workspaceId, userId, callback(result -> {
            loading.setValue(false);
            deleted.setValue(true);
        }, null));
    }

    public void transferOwnership(String memberId) {
        loading.setValue(true);
        repository.transferOwnership(workspaceId, userId, memberId,
                callback(result -> {
                    message.setValue("Đã chuyển quyền chủ nhóm");
                    load();
                }, null));
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

    public void saveProject(
            Project existing, String name, String description,
            long startDate, long dueDate, String managerId
    ) {
        loading.setValue(true);
        repository.saveProject(existing == null ? null : existing.getProjectId(),
                workspaceId, userId, name, description, startDate, dueDate,
                managerId, existing == null ? "ACTIVE" : existing.getStatus(),
                callback(result -> {
                    message.setValue(existing == null ? "Đã tạo dự án" : "Đã cập nhật dự án");
                    load();
                }, null));
    }

    public void archiveProject(Project project) {
        loading.setValue(true);
        repository.archiveProject(project.getProjectId(), workspaceId, userId,
                callback(result -> {
                    message.setValue("Đã lưu trữ dự án");
                    load();
                }, null));
    }

    public void completeProject(Project project) {
        loading.setValue(true);
        repository.completeProject(project.getProjectId(), workspaceId, userId,
                callback(result -> {
                    message.setValue("Đã hoàn thành dự án");
                    load();
                }, null));
    }

    public void loadMilestones(Project project) {
        repository.getMilestones(project.getProjectId(), workspaceId, userId,
                callback(milestones::setValue, null));
    }

    public void addMilestone(Project project, String title, long dueDate) {
        repository.addMilestone(project.getProjectId(), workspaceId, userId,
                title, dueDate, callback(result -> loadMilestones(project), "Đã thêm mốc dự án"));
    }

    public void toggleMilestone(Project project, ProjectMilestone item, boolean completed) {
        repository.toggleMilestone(item, userId, completed,
                callback(result -> loadMilestones(project), null));
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
    public LiveData<List<ProjectMilestone>> getMilestones() { return milestones; }

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
