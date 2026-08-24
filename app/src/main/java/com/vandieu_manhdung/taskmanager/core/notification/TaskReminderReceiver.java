package com.vandieu_manhdung.taskmanager.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskScheduleRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.NotificationDao;
import com.vandieu_manhdung.taskmanager.model.Task;

public class TaskReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type";
    public static final String TYPE_START = "start";
    public static final String TYPE_DUE_SOON = "due_soon";
    public static final String TYPE_OVERDUE = "overdue";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);
        String type = intent.getStringExtra(EXTRA_REMINDER_TYPE);
        if (taskId == null || taskId.isBlank() ||
                (!TYPE_START.equals(type) && !TYPE_DUE_SOON.equals(type) &&
                        !TYPE_OVERDUE.equals(type))) {
            return;
        }

        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                showReminderIfStillRelevant(context.getApplicationContext(), taskId, type);
            } finally {
                pendingResult.finish();
            }
        }).start();
    }

    private void showReminderIfStillRelevant(Context context, String taskId, String type) {
        Task task = new TaskDao(context).findById(taskId);
        if (task == null || TaskStatus.COMPLETED.equals(task.getStatus()) ||
                TaskStatus.CANCELLED.equals(task.getStatus())) {
            return;
        }

        long now = System.currentTimeMillis();
        if (TYPE_START.equals(type) && task.getStartDate() > now + 60_000L) {
            new TaskReminderScheduler(context).schedule(task);
            return;
        }
        if (TYPE_DUE_SOON.equals(type)) {
            if (task.getDueDate() <= now) {
                return;
            }
            long dueSoonAt = TaskScheduleRules.dueSoonAt(task.getDueDate());
            if (dueSoonAt > now + 60_000L) {
                new TaskReminderScheduler(context).schedule(task);
                return;
            }
        }
        if (TYPE_OVERDUE.equals(type)) {
            if (task.getDueDate() <= 0 || task.getDueDate() > now) {
                new TaskReminderScheduler(context).schedule(task);
                return;
            }
            if (new NotificationDao(context).existsForTaskTypeSince(
                    taskId, TYPE_OVERDUE, task.getDueDate())) {
                return;
            }
        }

        String taskTitle = task.getTitle();
        if (taskTitle == null || taskTitle.isBlank()) {
            taskTitle = context.getString(R.string.personal_tasks);
        }
        boolean starting = TYPE_START.equals(type);
        boolean overdue = TYPE_OVERDUE.equals(type);
        TaskNotificationManager.showTaskReminder(
                context,
                TaskNotificationManager.positiveId(type + ":" + taskId),
                taskId,
                type,
                starting
                        ? context.getString(R.string.notification_task_start_title)
                        : overdue
                        ? context.getString(R.string.notification_task_overdue_title)
                        : context.getString(R.string.notification_task_due_soon_title),
                starting
                        ? context.getString(R.string.notification_task_start_message, taskTitle)
                        : overdue
                        ? context.getString(R.string.notification_task_overdue_message, taskTitle)
                        : context.getString(R.string.notification_task_due_soon_message, taskTitle)
        );
    }
}
