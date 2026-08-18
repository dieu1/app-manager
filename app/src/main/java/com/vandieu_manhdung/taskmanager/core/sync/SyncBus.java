package com.vandieu_manhdung.taskmanager.core.sync;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class SyncBus {

    private static final SyncBus INSTANCE = new SyncBus();
    private final MutableLiveData<Long> changes = new MutableLiveData<>(0L);

    private SyncBus() {
    }

    public static SyncBus getInstance() {
        return INSTANCE;
    }

    public LiveData<Long> changes() {
        return changes;
    }

    public void notifyChanged() {
        changes.setValue(System.currentTimeMillis());
    }
}
