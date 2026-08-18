package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.UserTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.ProjectTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskAssigneeTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TeamInviteTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkSessionTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkspaceMemberTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkspaceTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.User;

public class UserDao {

    private final TaskManagerDatabaseHelper databaseHelper;

    public UserDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public boolean insert(User user) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        long result = database.insert(
                UserTable.TABLE_NAME,
                null,
                toContentValues(user)
        );

        return result != -1;
    }

    public int update(User user) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        String selection = UserTable.USER_ID + " = ?";
        String[] selectionArgs = {user.getUserId()};

        return database.update(
                UserTable.TABLE_NAME,
                toContentValues(user),
                selection,
                selectionArgs
        );
    }

    public boolean save(User user) {
        if (existsById(user.getUserId())) {
            return update(user) > 0;
        }

        return insert(user);
    }

    public boolean saveAuthenticatedUser(User user) {
        User existingByEmail = findByEmail(user.getEmail());
        if (existingByEmail == null ||
                existingByEmail.getUserId().equals(user.getUserId())) {
            return save(user);
        }

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.execSQL("PRAGMA defer_foreign_keys = ON");
        database.beginTransaction();
        try {
            String oldUserId = existingByEmail.getUserId();
            String newUserId = user.getUserId();
            updateReference(database, WorkspaceTable.TABLE_NAME,
                    WorkspaceTable.MANAGER_ID, oldUserId, newUserId);
            updateReference(database, TaskTable.TABLE_NAME,
                    TaskTable.CREATED_BY, oldUserId, newUserId);
            updateReference(database, WorkSessionTable.TABLE_NAME,
                    WorkSessionTable.USER_ID, oldUserId, newUserId);
            updateReference(database, WorkspaceMemberTable.TABLE_NAME,
                    WorkspaceMemberTable.USER_ID, oldUserId, newUserId);
            updateReference(database, TeamInviteTable.TABLE_NAME,
                    TeamInviteTable.INVITED_BY, oldUserId, newUserId);
            updateReference(database, ProjectTable.TABLE_NAME,
                    ProjectTable.CREATED_BY, oldUserId, newUserId);
            updateReference(database, TaskAssigneeTable.TABLE_NAME,
                    TaskAssigneeTable.USER_ID, oldUserId, newUserId);
            updateReference(database, TaskAssigneeTable.TABLE_NAME,
                    TaskAssigneeTable.ASSIGNED_BY, oldUserId, newUserId);

            ContentValues values = toContentValues(user);
            int updated = database.update(
                    UserTable.TABLE_NAME,
                    values,
                    UserTable.USER_ID + " = ?",
                    new String[]{oldUserId}
            );
            if (updated <= 0) {
                throw new IllegalStateException("Không thể chuyển dữ liệu sang tài khoản đăng nhập");
            }
            database.setTransactionSuccessful();
            return true;
        } finally {
            database.endTransaction();
        }
    }

    private void updateReference(
            SQLiteDatabase database,
            String table,
            String column,
            String oldUserId,
            String newUserId
    ) {
        ContentValues values = new ContentValues();
        values.put(column, newUserId);
        database.update(table, values, column + " = ?", new String[]{oldUserId});
    }

    public User findById(String userId) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String selection = UserTable.USER_ID + " = ?";
        String[] selectionArgs = {userId};

        try (Cursor cursor = database.query(
                UserTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null,
                "1"
        )) {
            if (cursor.moveToFirst()) {
                return mapCursorToUser(cursor);
            }
        }

        return null;
    }

    public User findByEmail(String email) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        try (Cursor cursor = database.query(
                UserTable.TABLE_NAME,
                null,
                UserTable.EMAIL + " = ? COLLATE NOCASE",
                new String[]{email},
                null,
                null,
                null,
                "1"
        )) {
            if (cursor.moveToFirst()) {
                return mapCursorToUser(cursor);
            }
        }

        return null;
    }

    public boolean existsById(String userId) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String sql =
                "SELECT 1 FROM " + UserTable.TABLE_NAME +
                        " WHERE " + UserTable.USER_ID + " = ? LIMIT 1";

        try (Cursor cursor = database.rawQuery(
                sql,
                new String[]{userId}
        )) {
            return cursor.moveToFirst();
        }
    }

    public boolean existsByUserCode(String userCode) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();

        String sql =
                "SELECT 1 FROM " + UserTable.TABLE_NAME +
                        " WHERE " + UserTable.USER_CODE + " = ? LIMIT 1";

        try (Cursor cursor = database.rawQuery(
                sql,
                new String[]{userCode}
        )) {
            return cursor.moveToFirst();
        }
    }

    private ContentValues toContentValues(User user) {
        ContentValues values = new ContentValues();

        values.put(UserTable.USER_ID, user.getUserId());
        values.put(UserTable.USER_CODE, user.getUserCode());
        values.put(UserTable.EMAIL, user.getEmail());
        values.put(UserTable.DISPLAY_NAME, user.getDisplayName());
        values.put(UserTable.AVATAR_URL, user.getAvatarUrl());
        values.put(UserTable.CREATED_AT, user.getCreatedAt());
        values.put(UserTable.UPDATED_AT, user.getUpdatedAt());

        return values;
    }

    private User mapCursorToUser(Cursor cursor) {
        User user = new User();

        user.setUserId(cursor.getString(
                cursor.getColumnIndexOrThrow(UserTable.USER_ID)
        ));

        user.setUserCode(cursor.getString(
                cursor.getColumnIndexOrThrow(UserTable.USER_CODE)
        ));

        user.setEmail(cursor.getString(
                cursor.getColumnIndexOrThrow(UserTable.EMAIL)
        ));

        user.setDisplayName(cursor.getString(
                cursor.getColumnIndexOrThrow(UserTable.DISPLAY_NAME)
        ));

        user.setAvatarUrl(cursor.getString(
                cursor.getColumnIndexOrThrow(UserTable.AVATAR_URL)
        ));

        user.setCreatedAt(cursor.getLong(
                cursor.getColumnIndexOrThrow(UserTable.CREATED_AT)
        ));

        user.setUpdatedAt(cursor.getLong(
                cursor.getColumnIndexOrThrow(UserTable.UPDATED_AT)
        ));

        return user;
    }
}
