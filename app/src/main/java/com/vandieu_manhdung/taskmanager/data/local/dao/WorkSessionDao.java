package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkSessionTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.WorkSession;

import java.util.ArrayList;
import java.util.List;

public class WorkSessionDao {

    private final TaskManagerDatabaseHelper databaseHelper;

    public WorkSessionDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public boolean insert(WorkSession session) {
        return databaseHelper.getWritableDatabase().insert(
                WorkSessionTable.TABLE_NAME,
                null,
                toValues(session)
        ) != -1;
    }

    public boolean save(WorkSession session) {
        ContentValues values = toValues(session);
        int updated = databaseHelper.getWritableDatabase().update(
                WorkSessionTable.TABLE_NAME,
                values,
                WorkSessionTable.SESSION_ID + " = ?",
                new String[]{session.getSessionId()}
        );
        return updated > 0 || insert(session);
    }

    public WorkSession findById(String sessionId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                WorkSessionTable.TABLE_NAME,
                null,
                WorkSessionTable.SESSION_ID + " = ?",
                new String[]{sessionId},
                null,
                null,
                null,
                "1"
        )) {
            return cursor.moveToFirst() ? mapCursor(cursor) : null;
        }
    }

    public List<WorkSession> findAllByUser(String userId) {
        List<WorkSession> sessions = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                WorkSessionTable.TABLE_NAME,
                null,
                WorkSessionTable.USER_ID + " = ?",
                new String[]{userId},
                null,
                null,
                WorkSessionTable.START_TIME + " ASC"
        )) {
            while (cursor.moveToNext()) {
                sessions.add(mapCursor(cursor));
            }
        }
        return sessions;
    }

    public WorkSession findActiveByUser(String userId) {
        String selection = WorkSessionTable.USER_ID + " = ? AND " +
                WorkSessionTable.END_TIME + " IS NULL";

        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                WorkSessionTable.TABLE_NAME,
                null,
                selection,
                new String[]{userId},
                null,
                null,
                WorkSessionTable.START_TIME + " DESC",
                "1"
        )) {
            return cursor.moveToFirst() ? mapCursor(cursor) : null;
        }
    }

    public int stop(
            String sessionId,
            long endTime,
            int durationMinutes
    ) {
        ContentValues values = new ContentValues();
        values.put(WorkSessionTable.END_TIME, endTime);
        values.put(WorkSessionTable.DURATION_MINUTES, durationMinutes);

        return databaseHelper.getWritableDatabase().update(
                WorkSessionTable.TABLE_NAME,
                values,
                WorkSessionTable.SESSION_ID + " = ? AND " +
                        WorkSessionTable.END_TIME + " IS NULL",
                new String[]{sessionId}
        );
    }

    public int totalMinutes(String taskId, String userId) {
        String sql = "SELECT COALESCE(SUM(" +
                WorkSessionTable.DURATION_MINUTES + "), 0) FROM " +
                WorkSessionTable.TABLE_NAME + " WHERE " +
                WorkSessionTable.TASK_ID + " = ? AND " +
                WorkSessionTable.USER_ID + " = ? AND " +
                WorkSessionTable.END_TIME + " IS NOT NULL";

        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql,
                new String[]{taskId, userId}
        )) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private ContentValues toValues(WorkSession session) {
        ContentValues values = new ContentValues();
        values.put(WorkSessionTable.SESSION_ID, session.getSessionId());
        values.put(WorkSessionTable.TASK_ID, session.getTaskId());
        values.put(WorkSessionTable.USER_ID, session.getUserId());
        values.put(WorkSessionTable.START_TIME, session.getStartTime());
        if (session.getEndTime() <= 0) {
            values.putNull(WorkSessionTable.END_TIME);
        } else {
            values.put(WorkSessionTable.END_TIME, session.getEndTime());
        }
        values.put(WorkSessionTable.DURATION_MINUTES, session.getDurationMinutes());
        return values;
    }

    private WorkSession mapCursor(Cursor cursor) {
        WorkSession session = new WorkSession();
        session.setSessionId(cursor.getString(cursor.getColumnIndexOrThrow(
                WorkSessionTable.SESSION_ID
        )));
        session.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(
                WorkSessionTable.TASK_ID
        )));
        session.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(
                WorkSessionTable.USER_ID
        )));
        session.setStartTime(cursor.getLong(cursor.getColumnIndexOrThrow(
                WorkSessionTable.START_TIME
        )));

        int endTimeColumn = cursor.getColumnIndexOrThrow(
                WorkSessionTable.END_TIME
        );
        session.setEndTime(cursor.isNull(endTimeColumn)
                ? 0
                : cursor.getLong(endTimeColumn));
        session.setDurationMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(
                WorkSessionTable.DURATION_MINUTES
        )));
        return session;
    }
}
