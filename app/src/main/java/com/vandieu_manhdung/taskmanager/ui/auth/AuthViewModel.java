package com.vandieu_manhdung.taskmanager.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.data.reponsitory.AuthRepository;
import com.vandieu_manhdung.taskmanager.model.User;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<User> authenticatedUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registered = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> resetSent = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public void signIn(String email, String password) {
        loading.setValue(true);
        try {
            repository.signIn(email, password, userCallback());
        } catch (Exception exception) {
            loading.setValue(false);
            error.setValue(exception.getMessage());
        }
    }

    public void register(
            String displayName,
            String email,
            String password,
            String passwordConfirmation
    ) {
        if (!password.equals(passwordConfirmation)) {
            error.setValue("Mật khẩu xác nhận không khớp");
            return;
        }
        loading.setValue(true);
        try {
            repository.register(
                    displayName,
                    email,
                    password,
                    new RepositoryCallback<User>() {
                        @Override
                        public void onSuccess(User result) {
                            loading.setValue(false);
                            registered.setValue(true);
                        }

                        @Override
                        public void onError(Exception exception) {
                            loading.setValue(false);
                            error.setValue(exception.getMessage());
                        }
                    }
            );
        } catch (Exception exception) {
            loading.setValue(false);
            error.setValue(exception.getMessage());
        }
    }

    public void resetPassword(String email) {
        loading.setValue(true);
        try {
            repository.sendPasswordReset(email, new RepositoryCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    loading.setValue(false);
                    resetSent.setValue(true);
                }

                @Override
                public void onError(Exception exception) {
                    loading.setValue(false);
                    error.setValue(exception.getMessage());
                }
            });
        } catch (Exception exception) {
            loading.setValue(false);
            error.setValue(exception.getMessage());
        }
    }

    private RepositoryCallback<User> userCallback() {
        return new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                loading.setValue(false);
                authenticatedUser.setValue(result);
            }

            @Override
            public void onError(Exception exception) {
                loading.setValue(false);
                error.setValue(exception.getMessage());
            }
        };
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<User> getAuthenticatedUser() {
        return authenticatedUser;
    }

    public LiveData<Boolean> getRegistered() {
        return registered;
    }

    public LiveData<Boolean> getResetSent() {
        return resetSent;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void clearEvents() {
        authenticatedUser.setValue(null);
        registered.setValue(false);
        resetSent.setValue(false);
        error.setValue(null);
    }
}
