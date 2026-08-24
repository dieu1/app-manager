package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.NotificationTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.AppNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationDao {
    private final TaskManagerDatabaseHelper databaseHelper;

    public NotificationDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public AppNotification add(
            String userId, String taskId, String type, String title, String message
    ) {
        return add(userId, null, taskId, type, title, message);
    }

    public AppNotification add(
            String userId, String workspaceId, String taskId,
            String type, String title, String message
    ) {
        AppNotification notification = new AppNotification();
        notification.setNotificationId(UUID.randomUUID().toString());
        notification.setUserId(userId);
        notification.setTaskId(taskId);
        notification.setWorkspaceId(workspaceId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCreatedAt(System.currentTimeMillis());
        notification.setReadAt(0);
        ContentValues values = new ContentValues();
        values.put(NotificationTable.NOTIFICATION_ID, notification.getNotificationId());
        values.put(NotificationTable.USER_ID, notification.getUserId());
        if (taskId == null || taskId.isBlank()) values.putNull(NotificationTable.TASK_ID);
        else values.put(NotificationTable.TASK_ID, taskId);
        if (workspaceId == null || workspaceId.isBlank()) {
            values.putNull(NotificationTable.WORKSPACE_ID);
        } else {
            values.put(NotificationTable.WORKSPACE_ID, workspaceId);
        }
        values.put(NotificationTable.TYPE, type);
        values.put(NotificationTable.TITLE, title);
        values.put(NotificationTable.MESSAGE, message);
        values.put(NotificationTable.CREATED_AT, notification.getCreatedAt());
        values.putNull(NotificationTable.READ_AT);
        databaseHelper.getWritableDatabase().insertOrThrow(
                NotificationTable.TABLE_NAME, null, values);
        return notification;
    }

    public List<AppNotification> findByUser(String userId) {
        List<AppNotification> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                NotificationTable.TABLE_NAME, null,
                NotificationTable.USER_ID + " = ?", new String[]{userId},
                null, null, NotificationTable.CREATED_AT + " DESC", "200")) {
            while (cursor.moveToNext()) result.add(map(cursor));
        }
        return result;
    }

    public int unreadCount(String userId) {
        String sql = "SELECT COUNT(*) FROM " + NotificationTable.TABLE_NAME +
                " WHERE " + NotificationTable.USER_ID + " = ? AND " +
                NotificationTable.READ_AT + " IS NULL";
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql, new String[]{userId})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public boolean existsForTaskTypeSince(String taskId, String type, long since) {
        String sql = "SELECT 1 FROM " + NotificationTable.TABLE_NAME +
                " WHERE " + NotificationTable.TASK_ID + " = ? AND " +
                NotificationTable.TYPE + " = ? AND " + NotificationTable.CREATED_AT +
                " >= ? LIMIT 1";
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql, new String[]{taskId, type, String.valueOf(Math.max(0, since))})) {
            return cursor.moveToFirst();
        }
    }

    public void markRead(String notificationId) {
        ContentValues values = new ContentValues();
        values.put(NotificationTable.READ_AT, System.currentTimeMillis());
        databaseHelper.getWritableDatabase().update(
                NotificationTable.TABLE_NAME, values,
                NotificationTable.NOTIFICATION_ID + " = ? AND " +
                        NotificationTable.READ_AT + " IS NULL",
                new String[]{notificationId});
    }

    public void markAllRead(String userId) {
        ContentValues values = new ContentValues();
        values.put(NotificationTable.READ_AT, System.currentTimeMillis());
        databaseHelper.getWritableDatabase().update(
                NotificationTable.TABLE_NAME, values,
                NotificationTable.USER_ID + " = ? AND " + NotificationTable.READ_AT + " IS NULL",
                new String[]{userId});
    }

    private AppNotification map(Cursor cursor) {
        AppNotification item = new AppNotification();
        item.setNotificationId(cursor.getString(cursor.getColumnIndexOrThrow(NotificationTable.NOTIFICATION_ID)));
        item.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(NotificationTable.USER_ID)));
        item.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(NotificationTable.TASK_ID)));
        item.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(NotificationTable.WORKSPACE_ID)));
        item.setType(cursor.getString(cursor.getColumnIndexOrThrow(NotificationTable.TYPE)));
        item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(NotificationTable.TITLE)));
        item.setMessage(cursor.getString(cursor.getColumnIndexOrThrow(NotificationTable.MESSAGE)));
        item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(NotificationTable.CREATED_AT)));
        int readColumn = cursor.getColumnIndexOrThrow(NotificationTable.READ_AT);
        item.setReadAt(cursor.isNull(readColumn) ? 0 : cursor.getLong(readColumn));
        return item;
    }
}
