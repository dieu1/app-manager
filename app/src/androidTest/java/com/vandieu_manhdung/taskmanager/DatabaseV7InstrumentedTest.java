package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DatabaseV7InstrumentedTest {
    @Test public void schemaContainsCollaborationTablesAndRemovesWorkSessions() {
        Context context = ApplicationProvider.getApplicationContext();
        SQLiteDatabase database = TaskManagerDatabaseHelper.getInstance(context)
                .getReadableDatabase();
        assertTrue(tableExists(database, DatabaseContract.TaskHistoryTable.TABLE_NAME));
        assertTrue(tableExists(database, DatabaseContract.NotificationTable.TABLE_NAME));
        assertTrue(tableExists(database, DatabaseContract.SyncQueueTable.TABLE_NAME));
        assertTrue(tableExists(database, DatabaseContract.TaskCommentTable.TABLE_NAME));
        assertTrue(tableExists(database, DatabaseContract.TaskAttachmentTable.TABLE_NAME));
        assertTrue(tableExists(database, DatabaseContract.TaskDependencyTable.TABLE_NAME));
        assertTrue(tableExists(database, DatabaseContract.ProjectMilestoneTable.TABLE_NAME));
        assertFalse(tableExists(database, "work_sessions"));
        assertTrue(columnExists(database, DatabaseContract.TaskTable.TABLE_NAME,
                DatabaseContract.TaskTable.DELETED_AT));
        assertTrue(columnExists(database, DatabaseContract.TaskTable.TABLE_NAME,
                DatabaseContract.TaskTable.VERSION));
        assertTrue(columnExists(database, DatabaseContract.TaskTable.TABLE_NAME,
                DatabaseContract.TaskTable.SYNC_STATUS));
        assertTrue(columnExists(database, DatabaseContract.TeamInviteTable.TABLE_NAME,
                DatabaseContract.TeamInviteTable.INVITED_USER_ID));
        assertTrue(columnExists(database, DatabaseContract.ProjectTable.TABLE_NAME,
                DatabaseContract.ProjectTable.START_DATE));
        assertTrue(columnExists(database, DatabaseContract.TaskSubtaskTable.TABLE_NAME,
                DatabaseContract.TaskSubtaskTable.ASSIGNEE_ID));
    }

    private boolean tableExists(SQLiteDatabase database, String table) {
        try (Cursor cursor = database.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{table})) {
            return cursor.moveToFirst();
        }
    }

    private boolean columnExists(SQLiteDatabase database, String table, String column) {
        try (Cursor cursor = database.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameColumn = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameColumn))) return true;
            }
            return false;
        }
    }
}
