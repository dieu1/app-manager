package com.vandieu_manhdung.taskmanager.core.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private static volatile AppExecutors instance;

    private final ExecutorService databaseExecutor;
    private final Executor mainThreadExecutor;

    private AppExecutors() {
        // Một luồng riêng giúp các thao tác SQLite chạy tuần tự.
        databaseExecutor =
                Executors.newSingleThreadExecutor();

        Handler mainHandler =
                new Handler(Looper.getMainLooper());

        mainThreadExecutor = mainHandler::post;
    }

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }

        return instance;
    }

    public ExecutorService database() {
        return databaseExecutor;
    }

    public Executor mainThread() {
        return mainThreadExecutor;
    }
}