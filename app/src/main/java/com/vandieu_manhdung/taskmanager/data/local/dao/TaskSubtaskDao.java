package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.vandieu_manhdung.taskmanager.core.constant.SyncStatus;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskSubtaskTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;

import java.util.ArrayList;
import java.util.List;

public class TaskSubtaskDao {

    private final TaskManagerDatabaseHelper databaseHelper;

    public TaskSubtaskDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public boolean insert(TaskSubtask subtask) {
        return databaseHelper.getWritableDatabase().insert(
                TaskSubtaskTable.TABLE_NAME,
                null,
                toValues(subtask)
        ) != -1;
    }

    public boolean save(TaskSubtask subtask) {
        int updated = databaseHelper.getWritableDatabase().update(
                TaskSubtaskTable.TABLE_NAME,
                toValues(subtask),
                TaskSubtaskTable.SUBTASK_ID + " = ?",
                new String[]{subtask.getSubtaskId()}
        );
        return updated > 0 || insert(subtask);
    }

    public int update(TaskSubtask subtask) {
        return databaseHelper.getWritableDatabase().update(
                TaskSubtaskTable.TABLE_NAME,
                toValues(subtask),
                TaskSubtaskTable.SUBTASK_ID + " = ?",
                new String[]{subtask.getSubtaskId()}
        );
    }

    public int delete(String subtaskId) {
        return databaseHelper.getWritableDatabase().delete(
                TaskSubtaskTable.TABLE_NAME,
                TaskSubtaskTable.SUBTASK_ID + " = ?",
                new String[]{subtaskId}
        );
    }

    public int softDelete(String subtaskId, long deletedAt, int version) {
        ContentValues values = new ContentValues();
        values.put(TaskSubtaskTable.DELETED_AT, deletedAt);
        values.put(TaskSubtaskTable.UPDATED_AT, deletedAt);
        values.put(TaskSubtaskTable.VERSION, Math.max(1, version));
        values.put(TaskSubtaskTable.SYNC_STATUS, SyncStatus.PENDING);
        return databaseHelper.getWritableDatabase().update(
                TaskSubtaskTable.TABLE_NAME, values,
                TaskSubtaskTable.SUBTASK_ID + " = ?", new String[]{subtaskId});
    }

    public void markSyncStatus(String subtaskId, int version, String status) {
        ContentValues values = new ContentValues();
        values.put(TaskSubtaskTable.SYNC_STATUS, status);
        databaseHelper.getWritableDatabase().update(
                TaskSubtaskTable.TABLE_NAME, values,
                TaskSubtaskTable.SUBTASK_ID + " = ? AND " + TaskSubtaskTable.VERSION + " = ?",
                new String[]{subtaskId, String.valueOf(version)});
    }

    public List<TaskSubtask> findAllByTask(String taskId) {
        List<TaskSubtask> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskSubtaskTable.TABLE_NAME,
                null,
                TaskSubtaskTable.TASK_ID + " = ? AND (" +
                        TaskSubtaskTable.DELETED_AT + " IS NULL OR " +
                        TaskSubtaskTable.DELETED_AT + " = 0)",
                new String[]{taskId},
                null,
                null,
                TaskSubtaskTable.SORT_ORDER + " ASC, " +
                        TaskSubtaskTable.CREATED_AT + " ASC"
        )) {
            while (cursor.moveToNext()) {
                result.add(mapCursor(cursor));
            }
        }
        return result;
    }

    public List<TaskSubtask> findAllByWorkspace(String workspaceId) {
        List<TaskSubtask> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskSubtaskTable.TABLE_NAME,
                null,
                TaskSubtaskTable.WORKSPACE_ID + " = ? AND (" +
                        TaskSubtaskTable.DELETED_AT + " IS NULL OR " +
                        TaskSubtaskTable.DELETED_AT + " = 0)",
                new String[]{workspaceId},
                null,
                null,
                TaskSubtaskTable.TASK_ID + " ASC, " +
                        TaskSubtaskTable.SORT_ORDER + " ASC"
        )) {
            while (cursor.moveToNext()) {
                result.add(mapCursor(cursor));
            }
        }
        return result;
    }

    public TaskSubtask findById(String subtaskId) {
        TaskSubtask subtask = findByIdIncludingDeleted(subtaskId);
        return subtask != null && subtask.getDeletedAt() <= 0 ? subtask : null;
    }

    public TaskSubtask findByIdIncludingDeleted(String subtaskId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskSubtaskTable.TABLE_NAME,
                null,
                TaskSubtaskTable.SUBTASK_ID + " = ?",
                new String[]{subtaskId},
                null,
                null,
                null,
                "1"
        )) {
            return cursor.moveToFirst() ? mapCursor(cursor) : null;
        }
    }

    public int nextSortOrder(String taskId) {
        String sql = "SELECT COALESCE(MAX(" + TaskSubtaskTable.SORT_ORDER + "), -1) + 1 " +
                "FROM " + TaskSubtaskTable.TABLE_NAME +
                " WHERE " + TaskSubtaskTable.TASK_ID + " = ? AND (" +
                TaskSubtaskTable.DELETED_AT + " IS NULL OR " +
                TaskSubtaskTable.DELETED_AT + " = 0)";
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql,
                new String[]{taskId}
        )) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private ContentValues toValues(TaskSubtask subtask) {
        ContentValues values = new ContentValues();
        values.put(TaskSubtaskTable.SUBTASK_ID, subtask.getSubtaskId());
        values.put(TaskSubtaskTable.TASK_ID, subtask.getTaskId());
        values.put(TaskSubtaskTable.WORKSPACE_ID, subtask.getWorkspaceId());
        values.put(TaskSubtaskTable.CREATED_BY, subtask.getCreatedBy());
        values.put(TaskSubtaskTable.ASSIGNEE_ID, subtask.getAssigneeId());
        values.put(TaskSubtaskTable.TITLE, subtask.getTitle());
        values.put(TaskSubtaskTable.ESTIMATED_MINUTES, subtask.getEstimatedMinutes());
        values.put(TaskSubtaskTable.COMPLETED, subtask.isCompleted() ? 1 : 0);
        if (subtask.getCompletedAt() > 0) values.put(TaskSubtaskTable.COMPLETED_AT, subtask.getCompletedAt());
        else values.putNull(TaskSubtaskTable.COMPLETED_AT);
        if (subtask.getDeletedAt() > 0) values.put(TaskSubtaskTable.DELETED_AT, subtask.getDeletedAt());
        else values.putNull(TaskSubtaskTable.DELETED_AT);
        values.put(TaskSubtaskTable.VERSION, Math.max(1, subtask.getVersion()));
        values.put(TaskSubtaskTable.SYNC_STATUS,
                subtask.getSyncStatus() == null ? SyncStatus.SYNCED : subtask.getSyncStatus());
        values.put(TaskSubtaskTable.SORT_ORDER, subtask.getSortOrder());
        values.put(TaskSubtaskTable.CREATED_AT, subtask.getCreatedAt());
        values.put(TaskSubtaskTable.UPDATED_AT, subtask.getUpdatedAt());
        return values;
    }

    private TaskSubtask mapCursor(Cursor cursor) {
        TaskSubtask subtask = new TaskSubtask();
        subtask.setSubtaskId(cursor.getString(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.SUBTASK_ID)));
        subtask.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.TASK_ID)));
        subtask.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.WORKSPACE_ID)));
        subtask.setCreatedBy(cursor.getString(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.CREATED_BY)));
        int assigneeColumn = cursor.getColumnIndex(TaskSubtaskTable.ASSIGNEE_ID);
        if (assigneeColumn >= 0 && !cursor.isNull(assigneeColumn)) {
            subtask.setAssigneeId(cursor.getString(assigneeColumn));
        }
        subtask.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.TITLE)));
        subtask.setEstimatedMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.ESTIMATED_MINUTES)));
        subtask.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.COMPLETED)) != 0);
        int completedAt = cursor.getColumnIndexOrThrow(TaskSubtaskTable.COMPLETED_AT);
        subtask.setCompletedAt(cursor.isNull(completedAt) ? 0 : cursor.getLong(completedAt));
        int deletedAt = cursor.getColumnIndexOrThrow(TaskSubtaskTable.DELETED_AT);
        subtask.setDeletedAt(cursor.isNull(deletedAt) ? 0 : cursor.getLong(deletedAt));
        subtask.setVersion(cursor.getInt(cursor.getColumnIndexOrThrow(TaskSubtaskTable.VERSION)));
        subtask.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(TaskSubtaskTable.SYNC_STATUS)));
        subtask.setSortOrder(cursor.getInt(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.SORT_ORDER)));
        subtask.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.CREATED_AT)));
        subtask.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(
                TaskSubtaskTable.UPDATED_AT)));
        return subtask;
    }
}
