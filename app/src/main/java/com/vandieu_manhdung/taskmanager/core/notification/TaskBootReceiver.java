package com.vandieu_manhdung.taskmanager.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TaskBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
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
