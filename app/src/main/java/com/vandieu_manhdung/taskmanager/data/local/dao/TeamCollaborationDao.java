package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskAttachmentTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskCommentTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskDependencyTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.UserTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.TaskAttachment;
import com.vandieu_manhdung.taskmanager.model.TaskComment;
import com.vandieu_manhdung.taskmanager.model.TaskDependency;
import com.vandieu_manhdung.taskmanager.core.util.TeamFeatureRules;

import java.util.ArrayList;
import java.util.List;

public class TeamCollaborationDao {
    private final TaskManagerDatabaseHelper helper;

    public TeamCollaborationDao(Context context) {
        helper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public boolean saveComment(TaskComment item) {
        ContentValues values = new ContentValues();
        values.put(TaskCommentTable.COMMENT_ID, item.getCommentId());
        values.put(TaskCommentTable.TASK_ID, item.getTaskId());
        values.put(TaskCommentTable.WORKSPACE_ID, item.getWorkspaceId());
        values.put(TaskCommentTable.USER_ID, item.getUserId());
        values.put(TaskCommentTable.MESSAGE, item.getMessage());
        values.put(TaskCommentTable.CREATED_AT, item.getCreatedAt());
        values.put(TaskCommentTable.UPDATED_AT, item.getUpdatedAt());
        values.put(TaskCommentTable.DELETED_AT, item.getDeletedAt());
        return helper.getWritableDatabase().insertWithOnConflict(
                TaskCommentTable.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public List<TaskComment> findComments(String taskId) {
        List<TaskComment> result = new ArrayList<>();
        String sql = "SELECT c.*, u." + UserTable.DISPLAY_NAME + " AS author_name FROM " +
                TaskCommentTable.TABLE_NAME + " c LEFT JOIN " + UserTable.TABLE_NAME +
                " u ON u." + UserTable.USER_ID + " = c." + TaskCommentTable.USER_ID +
                " WHERE c." + TaskCommentTable.TASK_ID + " = ? AND (c." +
                TaskCommentTable.DELETED_AT + " IS NULL OR c." + TaskCommentTable.DELETED_AT +
                " = 0) ORDER BY c." + TaskCommentTable.CREATED_AT + " ASC";
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, new String[]{taskId})) {
            while (cursor.moveToNext()) {
                TaskComment item = new TaskComment();
                item.setCommentId(text(cursor, TaskCommentTable.COMMENT_ID));
                item.setTaskId(text(cursor, TaskCommentTable.TASK_ID));
                item.setWorkspaceId(text(cursor, TaskCommentTable.WORKSPACE_ID));
                item.setUserId(text(cursor, TaskCommentTable.USER_ID));
                item.setUserDisplayName(text(cursor, "author_name"));
                item.setMessage(text(cursor, TaskCommentTable.MESSAGE));
                item.setCreatedAt(number(cursor, TaskCommentTable.CREATED_AT));
                item.setUpdatedAt(number(cursor, TaskCommentTable.UPDATED_AT));
                item.setDeletedAt(number(cursor, TaskCommentTable.DELETED_AT));
                result.add(item);
            }
        }
        return result;
    }

    public TaskComment findComment(String commentId) {
        try (Cursor cursor = helper.getReadableDatabase().query(
                TaskCommentTable.TABLE_NAME, null,
                TaskCommentTable.COMMENT_ID + " = ?", new String[]{commentId},
                null, null, null, "1")) {
            if (!cursor.moveToFirst()) return null;
            TaskComment item = new TaskComment();
            item.setCommentId(text(cursor, TaskCommentTable.COMMENT_ID));
            item.setTaskId(text(cursor, TaskCommentTable.TASK_ID));
            item.setWorkspaceId(text(cursor, TaskCommentTable.WORKSPACE_ID));
            item.setUserId(text(cursor, TaskCommentTable.USER_ID));
            item.setMessage(text(cursor, TaskCommentTable.MESSAGE));
            item.setCreatedAt(number(cursor, TaskCommentTable.CREATED_AT));
            item.setUpdatedAt(number(cursor, TaskCommentTable.UPDATED_AT));
            item.setDeletedAt(number(cursor, TaskCommentTable.DELETED_AT));
            return item;
        }
    }

    public boolean saveAttachment(TaskAttachment item) {
        ContentValues values = new ContentValues();
        values.put(TaskAttachmentTable.ATTACHMENT_ID, item.getAttachmentId());
        values.put(TaskAttachmentTable.TASK_ID, item.getTaskId());
        values.put(TaskAttachmentTable.WORKSPACE_ID, item.getWorkspaceId());
        values.put(TaskAttachmentTable.USER_ID, item.getUserId());
        values.put(TaskAttachmentTable.DISPLAY_NAME, item.getDisplayName());
        values.put(TaskAttachmentTable.MIME_TYPE, item.getMimeType());
        values.put(TaskAttachmentTable.LOCAL_URI, item.getLocalUri());
        values.put(TaskAttachmentTable.REMOTE_URL, item.getRemoteUrl());
        values.put(TaskAttachmentTable.SIZE_BYTES, item.getSizeBytes());
        values.put(TaskAttachmentTable.CREATED_AT, item.getCreatedAt());
        values.put(TaskAttachmentTable.DELETED_AT, item.getDeletedAt());
        return helper.getWritableDatabase().insertWithOnConflict(
                TaskAttachmentTable.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public List<TaskAttachment> findAttachments(String taskId) {
        List<TaskAttachment> result = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                TaskAttachmentTable.TABLE_NAME, null,
                TaskAttachmentTable.TASK_ID + " = ? AND (" +
                        TaskAttachmentTable.DELETED_AT + " IS NULL OR " +
                        TaskAttachmentTable.DELETED_AT + " = 0)",
                new String[]{taskId}, null, null, TaskAttachmentTable.CREATED_AT + " ASC")) {
            while (cursor.moveToNext()) {
                TaskAttachment item = new TaskAttachment();
                item.setAttachmentId(text(cursor, TaskAttachmentTable.ATTACHMENT_ID));
                item.setTaskId(text(cursor, TaskAttachmentTable.TASK_ID));
                item.setWorkspaceId(text(cursor, TaskAttachmentTable.WORKSPACE_ID));
                item.setUserId(text(cursor, TaskAttachmentTable.USER_ID));
                item.setDisplayName(text(cursor, TaskAttachmentTable.DISPLAY_NAME));
                item.setMimeType(text(cursor, TaskAttachmentTable.MIME_TYPE));
                item.setLocalUri(text(cursor, TaskAttachmentTable.LOCAL_URI));
                item.setRemoteUrl(text(cursor, TaskAttachmentTable.REMOTE_URL));
                item.setSizeBytes(number(cursor, TaskAttachmentTable.SIZE_BYTES));
                item.setCreatedAt(number(cursor, TaskAttachmentTable.CREATED_AT));
                item.setDeletedAt(number(cursor, TaskAttachmentTable.DELETED_AT));
                result.add(item);
            }
        }
        return result;
    }

    public TaskAttachment findAttachment(String attachmentId) {
        for (TaskAttachment item : findAttachmentsByColumn(
                TaskAttachmentTable.ATTACHMENT_ID, attachmentId)) return item;
        return null;
    }

    private List<TaskAttachment> findAttachmentsByColumn(String column, String value) {
        List<TaskAttachment> result = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                TaskAttachmentTable.TABLE_NAME, null, column + " = ?",
                new String[]{value}, null, null, null)) {
            while (cursor.moveToNext()) {
                TaskAttachment item = new TaskAttachment();
                item.setAttachmentId(text(cursor, TaskAttachmentTable.ATTACHMENT_ID));
                item.setTaskId(text(cursor, TaskAttachmentTable.TASK_ID));
                item.setWorkspaceId(text(cursor, TaskAttachmentTable.WORKSPACE_ID));
                item.setUserId(text(cursor, TaskAttachmentTable.USER_ID));
                item.setDisplayName(text(cursor, TaskAttachmentTable.DISPLAY_NAME));
                item.setMimeType(text(cursor, TaskAttachmentTable.MIME_TYPE));
                item.setLocalUri(text(cursor, TaskAttachmentTable.LOCAL_URI));
                item.setRemoteUrl(text(cursor, TaskAttachmentTable.REMOTE_URL));
                item.setSizeBytes(number(cursor, TaskAttachmentTable.SIZE_BYTES));
                item.setCreatedAt(number(cursor, TaskAttachmentTable.CREATED_AT));
                item.setDeletedAt(number(cursor, TaskAttachmentTable.DELETED_AT));
                result.add(item);
            }
        }
        return result;
    }

    public TaskDependency findDependency(String taskId, String dependsOnTaskId) {
        for (TaskDependency item : findDependencies(taskId)) {
            if (dependsOnTaskId.equals(item.getDependsOnTaskId())) return item;
        }
        return null;
    }

    public boolean saveDependency(TaskDependency item) {
        ContentValues values = new ContentValues();
        values.put(TaskDependencyTable.TASK_ID, item.getTaskId());
        values.put(TaskDependencyTable.DEPENDS_ON_TASK_ID, item.getDependsOnTaskId());
        values.put(TaskDependencyTable.CREATED_BY, item.getCreatedBy());
        values.put(TaskDependencyTable.CREATED_AT, item.getCreatedAt());
        return helper.getWritableDatabase().insertWithOnConflict(
                TaskDependencyTable.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public List<TaskDependency> findDependencies(String taskId) {
        List<TaskDependency> result = new ArrayList<>();
        String sql = "SELECT d.*, t." + TaskTable.TITLE + " AS depends_title FROM " +
                TaskDependencyTable.TABLE_NAME + " d INNER JOIN " + TaskTable.TABLE_NAME +
                " t ON t." + TaskTable.TASK_ID + " = d." +
                TaskDependencyTable.DEPENDS_ON_TASK_ID + " WHERE d." +
                TaskDependencyTable.TASK_ID + " = ? ORDER BY d." +
                TaskDependencyTable.CREATED_AT + " ASC";
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, new String[]{taskId})) {
            while (cursor.moveToNext()) {
                TaskDependency item = new TaskDependency();
                item.setTaskId(text(cursor, TaskDependencyTable.TASK_ID));
                item.setDependsOnTaskId(text(cursor, TaskDependencyTable.DEPENDS_ON_TASK_ID));
                item.setDependsOnTitle(text(cursor, "depends_title"));
                item.setCreatedBy(text(cursor, TaskDependencyTable.CREATED_BY));
                item.setCreatedAt(number(cursor, TaskDependencyTable.CREATED_AT));
                result.add(item);
            }
        }
        return result;
    }

    public boolean hasDependency(String taskId, String dependsOnTaskId) {
        try (Cursor cursor = helper.getReadableDatabase().query(
                TaskDependencyTable.TABLE_NAME, new String[]{TaskDependencyTable.TASK_ID},
                TaskDependencyTable.TASK_ID + " = ? AND " +
                        TaskDependencyTable.DEPENDS_ON_TASK_ID + " = ?",
                new String[]{taskId, dependsOnTaskId}, null, null, null, "1")) {
            return cursor.moveToFirst();
        }
    }

    public int deleteDependency(String taskId, String dependsOnTaskId) {
        return helper.getWritableDatabase().delete(TaskDependencyTable.TABLE_NAME,
                TaskDependencyTable.TASK_ID + " = ? AND " +
                        TaskDependencyTable.DEPENDS_ON_TASK_ID + " = ?",
                new String[]{taskId, dependsOnTaskId});
    }

    public boolean wouldCreateDependencyCycle(String taskId, String dependsOnTaskId) {
        return TeamFeatureRules.wouldCreateDependencyCycle(
                taskId, dependsOnTaskId, this::findDependencyTargetIds);
    }

    private List<String> findDependencyTargetIds(String taskId) {
        List<String> result = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query(
                TaskDependencyTable.TABLE_NAME,
                new String[]{TaskDependencyTable.DEPENDS_ON_TASK_ID},
                TaskDependencyTable.TASK_ID + " = ?", new String[]{taskId},
                null, null, null)) {
            while (cursor.moveToNext()) result.add(cursor.getString(0));
        }
        return result;
    }

    private String text(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }

    private long number(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? 0 : cursor.getLong(index);
    }
}
