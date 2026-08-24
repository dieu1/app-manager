package com.vandieu_manhdung.taskmanager.ui.team;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.sync.SyncBus;
import com.vandieu_manhdung.taskmanager.data.repository.TeamRepository;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;

import java.util.List;

public class TeamListViewModel extends AndroidViewModel {

    private final TeamRepository repository;
    private final Observer<Long> syncObserver = ignored -> loadTeams();
    private final MutableLiveData<List<Workspace>> teams = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Workspace> createdTeam = new MutableLiveData<>();
    private final MutableLiveData<List<TeamInvite>> invites = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private String userId;

    public TeamListViewModel(@NonNull Application application) {
        super(application);
        repository = new TeamRepository(application);
        SyncBus.getInstance().changes().observeForever(syncObserver);
    }

    public void setUserId(String userId) {
        this.userId = userId;
        loadTeams();
    }

    public void loadTeams() {
        if (userId == null || userId.isBlank()) {
            return;
        }
        loading.setValue(true);
        repository.getTeams(userId, new RepositoryCallback<List<Workspace>>() {
            @Override
            public void onSuccess(List<Workspace> result) {
                loading.setValue(false);
                teams.setValue(result);
            }

            @Override
            public void onError(Exception exception) {
                loading.setValue(false);
                error.setValue(exception.getMessage());
            }
        });
        repository.getPendingInvites(userId, new RepositoryCallback<List<TeamInvite>>() {
            @Override public void onSuccess(List<TeamInvite> result) { invites.setValue(result); }
            @Override public void onError(Exception exception) { error.setValue(exception.getMessage()); }
        });
    }

    public void createTeam(String name, String description) {
        loading.setValue(true);
        repository.createTeam(
                userId,
                name,
                description,
                new RepositoryCallback<Workspace>() {
                    @Override
                    public void onSuccess(Workspace result) {
                        loading.setValue(false);
                        createdTeam.setValue(result);
                        loadTeams();
                    }

                    @Override
                    public void onError(Exception exception) {
                        loading.setValue(false);
                        error.setValue(exception.getMessage());
                    }
                }
        );
    }

    public LiveData<List<Workspace>> getTeams() {
        return teams;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Workspace> getCreatedTeam() {
        return createdTeam;
    }

    public LiveData<List<TeamInvite>> getInvites() { return invites; }
    public LiveData<String> getMessage() { return message; }

    public void respondToInvite(TeamInvite invite, boolean accept) {
        loading.setValue(true);
        repository.respondToInvite(invite.getInviteId(), userId, accept,
                new RepositoryCallback<Boolean>() {
                    @Override public void onSuccess(Boolean result) {
                        loading.setValue(false);
                        message.setValue(accept ? "Đã tham gia nhóm" : "Đã từ chối lời mời");
                        loadTeams();
                    }
                    @Override public void onError(Exception exception) {
                        loading.setValue(false);
                        error.setValue(exception.getMessage());
                    }
                });
    }

    public void clearError() {
        error.setValue(null);
    }

    public void clearCreatedTeam() {
        createdTeam.setValue(null);
    }
    public void clearMessage() { message.setValue(null); }

    @Override
    protected void onCleared() {
        SyncBus.getInstance().changes().removeObserver(syncObserver);
    }
}
