package com.vandieu_manhdung.taskmanager.core.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskScheduleRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.model.Task;

public class TaskReminderScheduler {

    private final Context context;
    private final AlarmManager alarmManager;

    public TaskReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
        TaskNotificationManager.createChannel(this.context);
    }

    public void schedule(Task task) {
        if (task == null || task.getTaskId() == null) {
            return;
        }
        cancel(task.getTaskId());
        if (TaskStatus.COMPLETED.equals(task.getStatus()) ||
                TaskStatus.CANCELLED.equals(task.getStatus())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (task.getStartDate() > now) {
            scheduleOne(task, TaskReminderReceiver.TYPE_START, task.getStartDate());
        }
        long dueSoonAt = TaskScheduleRules.dueSoonAt(task.getDueDate());
        if (dueSoonAt > now) {
            scheduleOne(task, TaskReminderReceiver.TYPE_DUE_SOON, dueSoonAt);
        }
    }

    public void cancel(String taskId) {
        if (taskId == null || alarmManager == null) {
            return;
        }
        alarmManager.cancel(pendingIntent(taskId, "", TaskReminderReceiver.TYPE_START));
        alarmManager.cancel(pendingIntent(taskId, "", TaskReminderReceiver.TYPE_DUE_SOON));
    }

    public void rescheduleAll() {
        for (Task task : new TaskDao(context).findAllScheduledTasks(System.currentTimeMillis())) {
            schedule(task);
        }
    }

    private void scheduleOne(Task task, String type, long triggerAt) {
        if (alarmManager == null) {
            return;
        }
        PendingIntent operation = pendingIntent(task.getTaskId(), task.getTitle(), type);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
            return;
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation);
    }

    private PendingIntent pendingIntent(String taskId, String title, String type) {
        Intent intent = new Intent(context, TaskReminderReceiver.class)
                .setAction(context.getPackageName() + ".TASK_REMINDER." + type + "." + taskId)
                .putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId)
                .putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, title)
                .putExtra(TaskReminderReceiver.EXTRA_REMINDER_TYPE, type);
        return PendingIntent.getBroadcast(
                context,
                TaskNotificationManager.positiveId(type + ":" + taskId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
