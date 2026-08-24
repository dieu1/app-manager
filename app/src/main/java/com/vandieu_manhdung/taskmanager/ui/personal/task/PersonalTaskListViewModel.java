package com.vandieu_manhdung.taskmanager.ui.personal.task;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.TaskSortOption;
import com.vandieu_manhdung.taskmanager.core.sync.SyncBus;
import com.vandieu_manhdung.taskmanager.data.repository.TaskRepository;
import com.vandieu_manhdung.taskmanager.model.Task;

import java.util.ArrayList;
import java.util.List;

public class PersonalTaskListViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final Observer<Long> syncObserver = ignored -> loadTasks();

    private final MutableLiveData<List<Task>> tasks =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private String workspaceId;
    private String keyword = "";
    private String statusFilter;
    private String priorityFilter;
    private String sortOption = TaskSortOption.CREATED_AT;
    private boolean ascending = false;

    public PersonalTaskListViewModel(
            @NonNull Application application
    ) {
        super(application);
        taskRepository = new TaskRepository(application);
        SyncBus.getInstance().changes().observeForever(syncObserver);
    }

    public LiveData<List<Task>> getTasks() {
        return tasks;
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

    public void setKeyword(String keyword) {
        this.keyword = keyword == null ? "" : keyword.trim();
        loadTasks();
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
        loadTasks();
    }

    public void setPriorityFilter(String priorityFilter) {
        this.priorityFilter = priorityFilter;
        loadTasks();
    }

    public void setSortOption(String sortOption) {
        this.sortOption = sortOption;
        loadTasks();
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
        loadTasks();
    }

    public void loadTasks() {
        if (workspaceId == null ||
                workspaceId.isBlank()) {
            return;
        }

        loading.setValue(true);

        taskRepository.getPersonalTasks(
                workspaceId,
                keyword,
                statusFilter,
                priorityFilter,
                sortOption,
                ascending,
                new RepositoryCallback<List<Task>>() {
                    @Override
                    public void onSuccess(List<Task> result) {
                        loading.setValue(false);
                        tasks.setValue(result);
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

    @Override
    protected void onCleared() {
        SyncBus.getInstance().changes().removeObserver(syncObserver);
    }
}
