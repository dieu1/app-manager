package com.vandieu_manhdung.taskmanager.ui.personal.task.timer;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.reponsitory.WorkSessionRepository;
import com.vandieu_manhdung.taskmanager.model.WorkSession;
import com.vandieu_manhdung.taskmanager.model.WorkTimerState;

public class WorkTimerViewModel extends AndroidViewModel {

    private final WorkSessionRepository repository;
    private final MutableLiveData<WorkTimerState> state = new MutableLiveData<>();
    private final MutableLiveData<Long> elapsedSeconds = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String taskId;
    private String userId;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateElapsedTime();
            handler.postDelayed(this, 1_000L);
        }
    };

    public WorkTimerViewModel(@NonNull Application application) {
        super(application);
        repository = new WorkSessionRepository(application);
    }

    public LiveData<WorkTimerState> getState() {
        return state;
    }

    public LiveData<Long> getElapsedSeconds() {
        return elapsedSeconds;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void configure(String taskId, String userId) {
        this.taskId = taskId;
        this.userId = userId;
    }

    public void load() {
        loading.setValue(true);
        repository.getTimerState(taskId, userId, callback());
    }

    public void start() {
        loading.setValue(true);
        repository.start(taskId, userId, callback());
    }

    public void stop() {
        loading.setValue(true);
        repository.stop(taskId, userId, callback());
    }

    public void clearError() {
        error.setValue(null);
    }

    private RepositoryCallback<WorkTimerState> callback() {
        return new RepositoryCallback<WorkTimerState>() {
            @Override
            public void onSuccess(WorkTimerState result) {
                loading.setValue(false);
                applyState(result);
            }

            @Override
            public void onError(Exception exception) {
                loading.setValue(false);
                error.setValue(exception.getMessage());
            }
        };
    }

    private void applyState(WorkTimerState value) {
        state.setValue(value);
        handler.removeCallbacks(ticker);
        updateElapsedTime();
        if (value != null && value.isRunning()) {
            handler.postDelayed(ticker, 1_000L);
        }
    }

    private void updateElapsedTime() {
        WorkTimerState current = state.getValue();
        WorkSession activeSession = current == null
                ? null
                : current.getActiveSession();
        if (activeSession == null) {
            elapsedSeconds.setValue(0L);
            return;
        }

        elapsedSeconds.setValue(Math.max(
                0,
                (System.currentTimeMillis() - activeSession.getStartTime()) / 1_000L
        ));
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(ticker);
    }
}
