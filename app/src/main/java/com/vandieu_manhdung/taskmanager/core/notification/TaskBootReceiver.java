package com.vandieu_manhdung.taskmanager.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TaskBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) &&
                !Intent.ACTION_TIME_CHANGED.equals(action) &&
                !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }
        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                new TaskReminderScheduler(context).rescheduleAll();
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
