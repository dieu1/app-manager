package com.vandieu_manhdung.taskmanager.core.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.vandieu_manhdung.taskmanager.R;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;
import com.vandieu_manhdung.taskmanager.ui.main.MainActivity;

public final class TaskNotificationManager {

    public static final String CHANNEL_ID = "task_reminders";

    private TaskNotificationManager() {
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_tasks),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_tasks_description));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void showTaskReminder(
            Context context,
            int notificationId,
            String title,
            String message
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createChannel(context);
        Intent openApp = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }

    public static void showSubtaskCompleted(
            Context context,
            Task task,
            TaskSubtask subtask,
            int completed,
            int total
    ) {
        String message = context.getString(
                R.string.notification_subtask_completed_message,
                subtask.getTitle(),
                completed,
                total
        );
        showTaskReminder(
                context,
                positiveId("subtask:" + subtask.getSubtaskId()),
                context.getString(R.string.notification_subtask_completed_title, task.getTitle()),
                message
        );
    }

    public static int positiveId(String value) {
        return value == null ? 1 : value.hashCode() & 0x7fffffff;
    }
}
