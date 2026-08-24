package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskHistoryTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.TaskHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TaskHistoryDao {
    private final TaskManagerDatabaseHelper databaseHelper;

    public TaskHistoryDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public TaskHistory add(String taskId, String userId, String action, String detail) {
        TaskHistory history = new TaskHistory();
        history.setHistoryId(UUID.randomUUID().toString());
        history.setTaskId(taskId);
        history.setUserId(userId);
        history.setAction(action);
        history.setDetail(detail == null ? "" : detail);
        history.setCreatedAt(System.currentTimeMillis());
        ContentValues values = new ContentValues();
        values.put(TaskHistoryTable.HISTORY_ID, history.getHistoryId());
        values.put(TaskHistoryTable.TASK_ID, history.getTaskId());
        values.put(TaskHistoryTable.USER_ID, history.getUserId());
        values.put(TaskHistoryTable.ACTION, history.getAction());
        values.put(TaskHistoryTable.DETAIL, history.getDetail());
        values.put(TaskHistoryTable.CREATED_AT, history.getCreatedAt());
        databaseHelper.getWritableDatabase().insertOrThrow(
                TaskHistoryTable.TABLE_NAME, null, values);
        return history;
    }

    public void save(TaskHistory history) {
        ContentValues values = new ContentValues();
        values.put(TaskHistoryTable.HISTORY_ID, history.getHistoryId());
        values.put(TaskHistoryTable.TASK_ID, history.getTaskId());
        values.put(TaskHistoryTable.USER_ID, history.getUserId());
        values.put(TaskHistoryTable.ACTION, history.getAction());
        values.put(TaskHistoryTable.DETAIL, history.getDetail());
        values.put(TaskHistoryTable.CREATED_AT, history.getCreatedAt());
        databaseHelper.getWritableDatabase().insertWithOnConflict(
                TaskHistoryTable.TABLE_NAME, null, values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
    }

    public TaskHistory findById(String historyId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskHistoryTable.TABLE_NAME, null,
                TaskHistoryTable.HISTORY_ID + " = ?", new String[]{historyId},
                null, null, null, "1")) {
            if (!cursor.moveToFirst()) return null;
            TaskHistory history = new TaskHistory();
            history.setHistoryId(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.HISTORY_ID)));
            history.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.TASK_ID)));
            history.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.USER_ID)));
            history.setAction(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.ACTION)));
            history.setDetail(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.DETAIL)));
            history.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(TaskHistoryTable.CREATED_AT)));
            return history;
        }
    }

    public List<TaskHistory> findByTask(String taskId) {
        List<TaskHistory> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskHistoryTable.TABLE_NAME, null,
                TaskHistoryTable.TASK_ID + " = ?", new String[]{taskId},
                null, null, TaskHistoryTable.CREATED_AT + " DESC", "100")) {
            while (cursor.moveToNext()) {
                TaskHistory history = new TaskHistory();
                history.setHistoryId(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.HISTORY_ID)));
                history.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.TASK_ID)));
                history.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.USER_ID)));
                history.setAction(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.ACTION)));
                history.setDetail(cursor.getString(cursor.getColumnIndexOrThrow(TaskHistoryTable.DETAIL)));
                history.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(TaskHistoryTable.CREATED_AT)));
                result.add(history);
            }
        }
        return result;
    }
}
