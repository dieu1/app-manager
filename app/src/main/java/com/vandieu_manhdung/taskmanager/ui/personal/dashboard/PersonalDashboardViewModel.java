package com.vandieu_manhdung.taskmanager.ui.personal.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.reponsitory.TaskRepository;
import com.vandieu_manhdung.taskmanager.model.PersonalDashboardSummary;

public class PersonalDashboardViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final MutableLiveData<PersonalDashboardSummary> summary =
            new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);
    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private String workspaceId;

    public PersonalDashboardViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
    }

    public LiveData<PersonalDashboardSummary> getSummary() {
        return summary;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void loadSummary() {
        if (workspaceId == null || workspaceId.isBlank()) {
            error.setValue("Thiếu Workspace ID");
            return;
        }

        loading.setValue(true);
        taskRepository.getPersonalDashboardSummary(
                workspaceId,
                new RepositoryCallback<PersonalDashboardSummary>() {
                    @Override
                    public void onSuccess(PersonalDashboardSummary result) {
                        loading.setValue(false);
                        summary.setValue(result);
                    }

                    @Override
                    public void onError(Exception exception) {
                        loading.setValue(false);
                        error.setValue(exception.getMessage());
                    }
                }
        );
    }

    public void clearError() {
        error.setValue(null);
    }
}
