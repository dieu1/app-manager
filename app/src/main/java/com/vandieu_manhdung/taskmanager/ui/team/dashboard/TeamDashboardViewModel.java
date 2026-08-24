package com.vandieu_manhdung.taskmanager.ui.team.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.sync.SyncBus;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.data.repository.TeamRepository;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.ProjectProgress;
import com.vandieu_manhdung.taskmanager.model.TeamDashboardData;
import com.vandieu_manhdung.taskmanager.model.TeamDashboardSummary;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeamDashboardViewModel extends AndroidViewModel {

    private final TeamRepository repository;
    private final Observer<Long> syncObserver = ignored -> load();
    private final MutableLiveData<TeamDashboardData> data = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private String workspaceId;
    private String userId;

    public TeamDashboardViewModel(@NonNull Application application) {
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
                null,
                null,
                null,
                new RepositoryCallback<TeamWorkspaceSnapshot>() {
                    @Override
                    public void onSuccess(TeamWorkspaceSnapshot snapshot) {
                        loading.setValue(false);
                        long now = System.currentTimeMillis();
                        data.setValue(new TeamDashboardData(
                                snapshot,
                                new TeamDashboardSummary(snapshot.getTasks(), now),
                                calculateProjectProgress(snapshot, now)
                        ));
                    }

                    @Override
                    public void onError(Exception exception) {
                        loading.setValue(false);
                        error.setValue(exception.getMessage());
                    }
                }
        );
    }

    private List<ProjectProgress> calculateProjectProgress(
            TeamWorkspaceSnapshot snapshot,
            long currentTime
    ) {
        List<ProjectProgress> result = new ArrayList<>();
        for (Project project : snapshot.getProjects()) {
            int total = 0;
            int completed = 0;
            int overdue = 0;
            for (TeamTaskItem item : snapshot.getTasks()) {
                if (!project.getProjectId().equals(item.getTask().getProjectId())) {
                    continue;
                }
                total++;
                if (TaskStatus.COMPLETED.equals(item.getTask().getStatus())) {
                    completed++;
                }
                if (TaskRules.isOverdue(item.getTask(), currentTime)) {
                    overdue++;
                }
            }
            result.add(new ProjectProgress(
                    project.getProjectId(),
                    project.getName(),
                    total,
                    completed,
                    overdue
            ));
        }
        return result;
    }

    public LiveData<TeamDashboardData> getData() {
        return data;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void clearError() {
        error.setValue(null);
    }

    @Override
    protected void onCleared() {
        SyncBus.getInstance().changes().removeObserver(syncObserver);
    }
}
