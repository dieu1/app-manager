package com.vandieu_manhdung.taskmanager.data.local.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.*;

public class TaskManagerDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "task_manager.db";
    private static final int DATABASE_VERSION = 3;

    private static volatile TaskManagerDatabaseHelper instance;

    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE " + UserTable.TABLE_NAME + " (" +
                    UserTable.USER_ID + " TEXT PRIMARY KEY, " +
                    UserTable.USER_CODE + " TEXT NOT NULL UNIQUE, " +
                    UserTable.EMAIL + " TEXT NOT NULL UNIQUE, " +
                    UserTable.DISPLAY_NAME + " TEXT NOT NULL, " +
                    UserTable.AVATAR_URL + " TEXT, " +
                    UserTable.CREATED_AT + " INTEGER NOT NULL, " +
                    UserTable.UPDATED_AT + " INTEGER" +
                    ")";

    private static final String CREATE_WORKSPACES_TABLE =
            "CREATE TABLE " + WorkspaceTable.TABLE_NAME + " (" +
                    WorkspaceTable.WORKSPACE_ID + " TEXT PRIMARY KEY, " +
                    WorkspaceTable.MANAGER_ID + " TEXT NOT NULL, " +
                    WorkspaceTable.NAME + " TEXT NOT NULL, " +
                    WorkspaceTable.TYPE + " TEXT NOT NULL, " +
                    WorkspaceTable.DESCRIPTION + " TEXT, " +
                    WorkspaceTable.STATUS +
                    " TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    WorkspaceTable.CREATED_AT + " INTEGER NOT NULL, " +
                    WorkspaceTable.UPDATED_AT + " INTEGER, " +

                    "FOREIGN KEY (" + WorkspaceTable.MANAGER_ID + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TASKS_TABLE =
            "CREATE TABLE " + TaskTable.TABLE_NAME + " (" +
                    TaskTable.TASK_ID + " TEXT PRIMARY KEY, " +
                    TaskTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    TaskTable.PROJECT_ID + " TEXT, " +
                    TaskTable.CREATED_BY + " TEXT NOT NULL, " +
                    TaskTable.TITLE + " TEXT NOT NULL, " +
                    TaskTable.DESCRIPTION + " TEXT, " +
                    TaskTable.STATUS +
                    " TEXT NOT NULL DEFAULT 'TODO', " +
                    TaskTable.PRIORITY +
                    " TEXT NOT NULL DEFAULT 'MEDIUM', " +

                    TaskTable.PROGRESS +
                    " INTEGER NOT NULL DEFAULT 0 CHECK (" +
                    TaskTable.PROGRESS + " BETWEEN 0 AND 100), " +

                    TaskTable.START_DATE + " INTEGER, " +
                    TaskTable.DUE_DATE + " INTEGER, " +

                    TaskTable.ESTIMATED_MINUTES +
                    " INTEGER NOT NULL DEFAULT 0 CHECK (" +
                    TaskTable.ESTIMATED_MINUTES + " >= 0), " +

                    TaskTable.CREATED_AT + " INTEGER NOT NULL, " +
                    TaskTable.UPDATED_AT + " INTEGER, " +

                    "FOREIGN KEY (" + TaskTable.WORKSPACE_ID + ") " +
                    "REFERENCES " + WorkspaceTable.TABLE_NAME +
                    "(" + WorkspaceTable.WORKSPACE_ID +
                    ") ON DELETE CASCADE, " +

                    "FOREIGN KEY (" + TaskTable.CREATED_BY + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_WORK_SESSIONS_TABLE =
            "CREATE TABLE " + WorkSessionTable.TABLE_NAME + " (" +
                    WorkSessionTable.SESSION_ID + " TEXT PRIMARY KEY, " +
                    WorkSessionTable.TASK_ID + " TEXT NOT NULL, " +
                    WorkSessionTable.USER_ID + " TEXT NOT NULL, " +
                    WorkSessionTable.START_TIME + " INTEGER NOT NULL, " +
                    WorkSessionTable.END_TIME + " INTEGER, " +

                    WorkSessionTable.DURATION_MINUTES +
                    " INTEGER NOT NULL DEFAULT 0 CHECK (" +
                    WorkSessionTable.DURATION_MINUTES + " >= 0), " +

                    "FOREIGN KEY (" + WorkSessionTable.TASK_ID + ") " +
                    "REFERENCES " + TaskTable.TABLE_NAME +
                    "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE, " +

                    "FOREIGN KEY (" + WorkSessionTable.USER_ID + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_WORKSPACE_MEMBERS_TABLE =
            "CREATE TABLE " + WorkspaceMemberTable.TABLE_NAME + " (" +
                    WorkspaceMemberTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    WorkspaceMemberTable.USER_ID + " TEXT NOT NULL, " +
                    WorkspaceMemberTable.ROLE + " TEXT NOT NULL, " +
                    WorkspaceMemberTable.STATUS +
                    " TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    WorkspaceMemberTable.JOINED_AT + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + WorkspaceMemberTable.WORKSPACE_ID +
                    ", " + WorkspaceMemberTable.USER_ID + "), " +
                    "FOREIGN KEY (" + WorkspaceMemberTable.WORKSPACE_ID + ") " +
                    "REFERENCES " + WorkspaceTable.TABLE_NAME +
                    "(" + WorkspaceTable.WORKSPACE_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + WorkspaceMemberTable.USER_ID + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TEAM_INVITES_TABLE =
            "CREATE TABLE " + TeamInviteTable.TABLE_NAME + " (" +
                    TeamInviteTable.INVITE_ID + " TEXT PRIMARY KEY, " +
                    TeamInviteTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    TeamInviteTable.EMAIL + " TEXT NOT NULL, " +
                    TeamInviteTable.ROLE + " TEXT NOT NULL, " +
                    TeamInviteTable.STATUS + " TEXT NOT NULL, " +
                    TeamInviteTable.INVITED_BY + " TEXT NOT NULL, " +
                    TeamInviteTable.CREATED_AT + " INTEGER NOT NULL, " +
                    TeamInviteTable.RESPONDED_AT + " INTEGER, " +
                    "FOREIGN KEY (" + TeamInviteTable.WORKSPACE_ID + ") " +
                    "REFERENCES " + WorkspaceTable.TABLE_NAME +
                    "(" + WorkspaceTable.WORKSPACE_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + TeamInviteTable.INVITED_BY + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_PROJECTS_TABLE =
            "CREATE TABLE " + ProjectTable.TABLE_NAME + " (" +
                    ProjectTable.PROJECT_ID + " TEXT PRIMARY KEY, " +
                    ProjectTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    ProjectTable.NAME + " TEXT NOT NULL, " +
                    ProjectTable.DESCRIPTION + " TEXT, " +
                    ProjectTable.STATUS + " TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    ProjectTable.CREATED_BY + " TEXT NOT NULL, " +
                    ProjectTable.CREATED_AT + " INTEGER NOT NULL, " +
                    ProjectTable.UPDATED_AT + " INTEGER, " +
                    "FOREIGN KEY (" + ProjectTable.WORKSPACE_ID + ") " +
                    "REFERENCES " + WorkspaceTable.TABLE_NAME +
                    "(" + WorkspaceTable.WORKSPACE_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + ProjectTable.CREATED_BY + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TASK_ASSIGNEES_TABLE =
            "CREATE TABLE " + TaskAssigneeTable.TABLE_NAME + " (" +
                    TaskAssigneeTable.TASK_ID + " TEXT NOT NULL, " +
                    TaskAssigneeTable.USER_ID + " TEXT NOT NULL, " +
                    TaskAssigneeTable.ASSIGNED_BY + " TEXT NOT NULL, " +
                    TaskAssigneeTable.ASSIGNED_AT + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + TaskAssigneeTable.TASK_ID +
                    ", " + TaskAssigneeTable.USER_ID + "), " +
                    "FOREIGN KEY (" + TaskAssigneeTable.TASK_ID + ") " +
                    "REFERENCES " + TaskTable.TABLE_NAME +
                    "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + TaskAssigneeTable.USER_ID + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + TaskAssigneeTable.ASSIGNED_BY + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private TaskManagerDatabaseHelper(
            @Nullable Context context
    ) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static TaskManagerDatabaseHelper getInstance(
            @NonNull Context context
    ) {
        if (instance == null) {
            synchronized (TaskManagerDatabaseHelper.class) {
                if (instance == null) {
                    instance = new TaskManagerDatabaseHelper(
                            context.getApplicationContext()
                    );
                }
            }
        }

        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase database) {
        super.onConfigure(database);
        database.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_USERS_TABLE);
        database.execSQL(CREATE_WORKSPACES_TABLE);
        database.execSQL(CREATE_PROJECTS_TABLE);
        database.execSQL(CREATE_TASKS_TABLE);
        database.execSQL(CREATE_WORK_SESSIONS_TABLE);
        database.execSQL(CREATE_WORKSPACE_MEMBERS_TABLE);
        database.execSQL(CREATE_TEAM_INVITES_TABLE);
        database.execSQL(CREATE_TASK_ASSIGNEES_TABLE);

        createIndexes(database);
    }

    private void createIndexes(SQLiteDatabase database) {
        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_workspace_manager " +
                        "ON " + WorkspaceTable.TABLE_NAME +
                        "(" + WorkspaceTable.MANAGER_ID + ")"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_personal_workspace_manager " +
                        "ON " + WorkspaceTable.TABLE_NAME +
                        "(" + WorkspaceTable.MANAGER_ID + ") " +
                        "WHERE " + WorkspaceTable.TYPE +
                        " = 'PERSONAL'"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_workspace " +
                        "ON " + TaskTable.TABLE_NAME +
                        "(" + TaskTable.WORKSPACE_ID + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_status " +
                        "ON " + TaskTable.TABLE_NAME +
                        "(" + TaskTable.STATUS + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_priority " +
                        "ON " + TaskTable.TABLE_NAME +
                        "(" + TaskTable.PRIORITY + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_task_due_date " +
                        "ON " + TaskTable.TABLE_NAME +
                        "(" + TaskTable.DUE_DATE + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_session_task " +
                        "ON " + WorkSessionTable.TABLE_NAME +
                        "(" + WorkSessionTable.TASK_ID + ")"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_one_active_session_per_user " +
                        "ON " + WorkSessionTable.TABLE_NAME +
                        "(" + WorkSessionTable.USER_ID + ") " +
                        "WHERE " + WorkSessionTable.END_TIME +
                        " IS NULL"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_member_user " +
                        "ON " + WorkspaceMemberTable.TABLE_NAME +
                        "(" + WorkspaceMemberTable.USER_ID + ", " +
                        WorkspaceMemberTable.STATUS + ")"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_pending_invite_email " +
                        "ON " + TeamInviteTable.TABLE_NAME +
                        "(" + TeamInviteTable.WORKSPACE_ID + ", " +
                        TeamInviteTable.EMAIL + " COLLATE NOCASE) " +
                        "WHERE " + TeamInviteTable.STATUS + " = 'PENDING'"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_project_name " +
                        "ON " + ProjectTable.TABLE_NAME +
                        "(" + ProjectTable.WORKSPACE_ID + ", " +
                        ProjectTable.NAME + " COLLATE NOCASE)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_project_workspace " +
                        "ON " + ProjectTable.TABLE_NAME +
                        "(" + ProjectTable.WORKSPACE_ID + ", " +
                        ProjectTable.STATUS + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_assignee_user " +
                        "ON " + TaskAssigneeTable.TABLE_NAME +
                        "(" + TaskAssigneeTable.USER_ID + ")"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase database,
            int oldVersion,
            int newVersion
    ) {
        if (oldVersion < 3) {
            database.execSQL(CREATE_PROJECTS_TABLE);
            database.execSQL(CREATE_WORKSPACE_MEMBERS_TABLE);
            database.execSQL(CREATE_TEAM_INVITES_TABLE);
            database.execSQL(CREATE_TASK_ASSIGNEES_TABLE);
            database.execSQL(
                    "INSERT OR IGNORE INTO " + WorkspaceMemberTable.TABLE_NAME +
                            " (" + WorkspaceMemberTable.WORKSPACE_ID + ", " +
                            WorkspaceMemberTable.USER_ID + ", " +
                            WorkspaceMemberTable.ROLE + ", " +
                            WorkspaceMemberTable.STATUS + ", " +
                            WorkspaceMemberTable.JOINED_AT + ") " +
                            "SELECT " + WorkspaceTable.WORKSPACE_ID + ", " +
                            WorkspaceTable.MANAGER_ID + ", 'OWNER', 'ACTIVE', " +
                            WorkspaceTable.CREATED_AT + " FROM " +
                            WorkspaceTable.TABLE_NAME + " WHERE " +
                            WorkspaceTable.TYPE + " = 'TEAM'"
            );
            createIndexes(database);
        }
    }
}
