package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.SyncQueueTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.SyncQueueItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SyncQueueDao {
    public static final String ENTITY_TASK = "TASK";
    public static final String ENTITY_SUBTASK = "SUBTASK";
    public static final String ENTITY_HISTORY = "HISTORY";
    public static final String ENTITY_WORKSPACE = "WORKSPACE";
    public static final String ENTITY_MEMBER = "MEMBER";
    public static final String ENTITY_PROJECT = "PROJECT";
    public static final String ENTITY_MILESTONE = "MILESTONE";
    public static final String ENTITY_INVITE = "INVITE";
    public static final String ENTITY_COMMENT = "COMMENT";
    public static final String ENTITY_ATTACHMENT = "ATTACHMENT";
    public static final String ENTITY_DEPENDENCY = "DEPENDENCY";
    public static final String UPSERT = "UPSERT";
    public static final String DELETE = "DELETE";

    private final TaskManagerDatabaseHelper databaseHelper;

    public SyncQueueDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public void enqueue(String entityType, String entityId, int version) {
        enqueue(entityType, entityId, version, UPSERT);
    }

    public void enqueueDelete(String entityType, String entityId) {
        enqueue(entityType, entityId, 1, DELETE);
    }

    private void enqueue(String entityType, String entityId, int version, String operation) {
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(SyncQueueTable.QUEUE_ID, UUID.randomUUID().toString());
        values.put(SyncQueueTable.ENTITY_TYPE, entityType);
        values.put(SyncQueueTable.ENTITY_ID, entityId);
        values.put(SyncQueueTable.OPERATION, operation);
        values.put(SyncQueueTable.VERSION, Math.max(1, version));
        values.put(SyncQueueTable.ATTEMPT_COUNT, 0);
        values.putNull(SyncQueueTable.LAST_ERROR);
        values.put(SyncQueueTable.CREATED_AT, now);
        values.put(SyncQueueTable.UPDATED_AT, now);
        databaseHelper.getWritableDatabase().insertWithOnConflict(
                SyncQueueTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<SyncQueueItem> findPending(int limit) {
        List<SyncQueueItem> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                SyncQueueTable.TABLE_NAME, null, null, null, null, null,
                "CASE " + SyncQueueTable.ENTITY_TYPE +
                        " WHEN 'TASK' THEN 0 WHEN 'SUBTASK' THEN 1 ELSE 2 END, " +
                        SyncQueueTable.CREATED_AT + " ASC", String.valueOf(limit))) {
            while (cursor.moveToNext()) result.add(map(cursor));
        }
        return result;
    }

    public void remove(String queueId, int version) {
        databaseHelper.getWritableDatabase().delete(
                SyncQueueTable.TABLE_NAME,
                SyncQueueTable.QUEUE_ID + " = ? AND " + SyncQueueTable.VERSION + " = ?",
                new String[]{queueId, String.valueOf(version)});
    }

    public void markFailed(String queueId, String error) {
        ContentValues values = new ContentValues();
        values.put(SyncQueueTable.LAST_ERROR, error == null ? "Không thể đồng bộ" : error);
        values.put(SyncQueueTable.UPDATED_AT, System.currentTimeMillis());
        databaseHelper.getWritableDatabase().execSQL(
                "UPDATE " + SyncQueueTable.TABLE_NAME + " SET " +
                        SyncQueueTable.ATTEMPT_COUNT + " = " + SyncQueueTable.ATTEMPT_COUNT +
                        " + 1, " + SyncQueueTable.LAST_ERROR + " = ?, " +
                        SyncQueueTable.UPDATED_AT + " = ? WHERE " + SyncQueueTable.QUEUE_ID + " = ?",
                new Object[]{error, System.currentTimeMillis(), queueId});
    }

    public int count() {
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + SyncQueueTable.TABLE_NAME, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private SyncQueueItem map(Cursor cursor) {
        SyncQueueItem item = new SyncQueueItem();
        item.setQueueId(cursor.getString(cursor.getColumnIndexOrThrow(SyncQueueTable.QUEUE_ID)));
        item.setEntityType(cursor.getString(cursor.getColumnIndexOrThrow(SyncQueueTable.ENTITY_TYPE)));
        item.setEntityId(cursor.getString(cursor.getColumnIndexOrThrow(SyncQueueTable.ENTITY_ID)));
        item.setOperation(cursor.getString(cursor.getColumnIndexOrThrow(SyncQueueTable.OPERATION)));
        item.setVersion(cursor.getInt(cursor.getColumnIndexOrThrow(SyncQueueTable.VERSION)));
        item.setAttemptCount(cursor.getInt(cursor.getColumnIndexOrThrow(SyncQueueTable.ATTEMPT_COUNT)));
        item.setLastError(cursor.getString(cursor.getColumnIndexOrThrow(SyncQueueTable.LAST_ERROR)));
        item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(SyncQueueTable.CREATED_AT)));
        item.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(SyncQueueTable.UPDATED_AT)));
        return item;
    }
}
