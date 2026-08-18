package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceType;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.ProjectTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TeamInviteTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkspaceMemberTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkspaceTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.Workspace;

public class WorkspaceDao {

    private final TaskManagerDatabaseHelper databaseHelper;

    public WorkspaceDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public boolean insert(Workspace workspace) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        long result = database.insert(
                WorkspaceTable.TABLE_NAME,
                null,
                toContentValues(workspace)
        );

        return result != -1;
    }

    public boolean save(Workspace workspace) {
        return findById(workspace.getWorkspaceId()) == null
                ? insert(workspace)
                : update(workspace) > 0;
    }

    /** Đổi khóa workspace và toàn bộ tham chiếu con trong cùng một giao dịch. */
    public Workspace renameWorkspace(Workspace workspace, String newWorkspaceId) {
        if (workspace == null || newWorkspaceId == null || newWorkspaceId.isBlank() ||
                newWorkspaceId.equals(workspace.getWorkspaceId())) {
            return workspace;
        }
        Workspace target = findById(newWorkspaceId);
        if (target != null) {
            return target;
        }

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.execSQL("PRAGMA defer_foreign_keys = ON");
        database.beginTransaction();
        try {
            String oldWorkspaceId = workspace.getWorkspaceId();
            updateWorkspaceReference(database, TaskTable.TABLE_NAME,
                    TaskTable.WORKSPACE_ID, oldWorkspaceId, newWorkspaceId);
            updateWorkspaceReference(database, ProjectTable.TABLE_NAME,
                    ProjectTable.WORKSPACE_ID, oldWorkspaceId, newWorkspaceId);
            updateWorkspaceReference(database, WorkspaceMemberTable.TABLE_NAME,
                    WorkspaceMemberTable.WORKSPACE_ID, oldWorkspaceId, newWorkspaceId);
            updateWorkspaceReference(database, TeamInviteTable.TABLE_NAME,
                    TeamInviteTable.WORKSPACE_ID, oldWorkspaceId, newWorkspaceId);

            ContentValues values = new ContentValues();
            values.put(WorkspaceTable.WORKSPACE_ID, newWorkspaceId);
            if (database.update(
                    WorkspaceTable.TABLE_NAME,
                    values,
                    WorkspaceTable.WORKSPACE_ID + " = ?",
                    new String[]{oldWorkspaceId}
            ) <= 0) {
                throw new IllegalStateException("Không thể chuẩn hóa workspace cá nhân");
            }
            database.setTransactionSuccessful();
            workspace.setWorkspaceId(newWorkspaceId);
            return workspace;
        } finally {
            database.endTransaction();
        }
    }

    private void updateWorkspaceReference(
            SQLiteDatabase database,
            String table,
            String column,
            String oldWorkspaceId,
            String newWorkspaceId
    ) {
        ContentValues values = new ContentValues();
        values.put(column, newWorkspaceId);
        database.update(table, values, column + " = ?", new String[]{oldWorkspaceId});
    }

    public int update(Workspace workspace) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        String selection = WorkspaceTable.WORKSPACE_ID + " = ?";
        String[] selectionArgs = {workspace.getWorkspaceId()};

        return database.update(
                WorkspaceTable.TABLE_NAME,
                toContentValues(workspace),
                selection,
                selectionArgs
        );
    }

    public Workspace findById(String workspaceId) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String selection = WorkspaceTable.WORKSPACE_ID + " = ?";
        String[] selectionArgs = {workspaceId};

        try (Cursor cursor = database.query(
                WorkspaceTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null,
                "1"
        )) {
            if (cursor.moveToFirst()) {
                return mapCursorToWorkspace(cursor);
            }
        }

        return null;
    }

    public Workspace findPersonalWorkspace(String userId) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String selection =
                WorkspaceTable.MANAGER_ID + " = ? AND " +
                        WorkspaceTable.TYPE + " = ?";

        String[] selectionArgs = {
                userId,
                WorkspaceType.PERSONAL
        };

        try (Cursor cursor = database.query(
                WorkspaceTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                WorkspaceTable.CREATED_AT + " ASC",
                "1"
        )) {
            if (cursor.moveToFirst()) {
                return mapCursorToWorkspace(cursor);
            }
        }

        return null;
    }

    public boolean existsPersonalWorkspace(String userId) {
        return findPersonalWorkspace(userId) != null;
    }

    public int delete(String workspaceId) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        return database.delete(
                WorkspaceTable.TABLE_NAME,
                WorkspaceTable.WORKSPACE_ID + " = ?",
                new String[]{workspaceId}
        );
    }

    private ContentValues toContentValues(Workspace workspace) {
        ContentValues values = new ContentValues();

        values.put(
                WorkspaceTable.WORKSPACE_ID,
                workspace.getWorkspaceId()
        );

        values.put(
                WorkspaceTable.MANAGER_ID,
                workspace.getManagerId()
        );

        values.put(
                WorkspaceTable.NAME,
                workspace.getName()
        );

        values.put(
                WorkspaceTable.TYPE,
                workspace.getType()
        );

        values.put(
                WorkspaceTable.DESCRIPTION,
                workspace.getDescription()
        );

        values.put(
                WorkspaceTable.STATUS,
                workspace.getStatus()
        );

        values.put(
                WorkspaceTable.CREATED_AT,
                workspace.getCreatedAt()
        );

        values.put(
                WorkspaceTable.UPDATED_AT,
                workspace.getUpdatedAt()
        );

        return values;
    }

    private Workspace mapCursorToWorkspace(Cursor cursor) {
        Workspace workspace = new Workspace();

        workspace.setWorkspaceId(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.WORKSPACE_ID
                )
        ));

        workspace.setManagerId(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.MANAGER_ID
                )
        ));

        workspace.setName(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.NAME
                )
        ));

        workspace.setType(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.TYPE
                )
        ));

        workspace.setDescription(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.DESCRIPTION
                )
        ));

        workspace.setStatus(cursor.getString(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.STATUS
                )
        ));

        workspace.setCreatedAt(cursor.getLong(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.CREATED_AT
                )
        ));

        workspace.setUpdatedAt(cursor.getLong(
                cursor.getColumnIndexOrThrow(
                        WorkspaceTable.UPDATED_AT
                )
        ));

        return workspace;
    }
}
