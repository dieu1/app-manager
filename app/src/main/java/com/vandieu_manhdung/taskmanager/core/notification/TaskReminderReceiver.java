package com.vandieu_manhdung.taskmanager.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vandieu_manhdung.taskmanager.R;

public class TaskReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";
    public static final String TYPE_START = "start";
    public static final String TYPE_DUE_SOON = "due_soon";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        String taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        String type = intent.getStringExtra(EXTRA_REMINDER_TYPE);
        if (taskTitle == null || taskTitle.isBlank()) {
            taskTitle = context.getString(R.string.personal_tasks);
        }
        boolean starting = TYPE_START.equals(type);
        TaskNotificationManager.showTaskReminder(
                context,
                TaskNotificationManager.positiveId(type + ":" + taskId),
                starting
                        ? context.getString(R.string.notification_task_start_title)
                        : context.getString(R.string.notification_task_due_soon_title),
                starting
                        ? context.getString(R.string.notification_task_start_message, taskTitle)
                        : context.getString(R.string.notification_task_due_soon_message, taskTitle)
        );
    }
}
