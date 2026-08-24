package com.vandieu_manhdung.taskmanager.core.notification;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vandieu_manhdung.taskmanager.data.local.dao.NotificationDao;
import com.vandieu_manhdung.taskmanager.data.remote.FirebaseProvider;

import java.util.HashMap;
import java.util.Map;

public class TeamMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        registerToken(this, token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        String userId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;
        String title = message.getNotification() == null
                ? message.getData().getOrDefault("title", "Task Manager")
                : message.getNotification().getTitle();
        String body = message.getNotification() == null
                ? message.getData().getOrDefault("body", "Bạn có cập nhật mới")
                : message.getNotification().getBody();
        String taskId = message.getData().get("taskId");
        if (taskId != null && new com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao(this)
                .findByIdIncludingDeleted(taskId) == null) taskId = null;
        String workspaceId = message.getData().get("workspaceId");
        String type = message.getData().getOrDefault("type", "TEAM_UPDATE");
        new NotificationDao(this).add(userId, workspaceId, taskId, type, title, body);
        TaskNotificationManager.showTaskReminder(
                this, TaskNotificationManager.positiveId(message.getMessageId()), title, body);
    }

    public static void registerCurrentToken(android.content.Context context) {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                registerToken(context.getApplicationContext(), token));
    }

    private static void registerToken(android.content.Context context, String token) {
        if (token == null || token.isBlank() || !FirebaseProvider.isConfigured(context)) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;
        Map<String, Object> values = new HashMap<>();
        values.put("userId", userId);
        values.put("token", token);
        values.put("platform", "ANDROID");
        values.put("updatedAt", System.currentTimeMillis());
        FirebaseProvider.firestore(context).collection("user_devices")
                .document(userId + "_" + Integer.toHexString(token.hashCode()))
                .set(values);
    }
}
