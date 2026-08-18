package com.vandieu_manhdung.taskmanager.core.callback;

public interface RepositoryCallback<T> {

    void onSuccess(T result);

    void onError(Exception exception);
}
