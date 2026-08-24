package com.vandieu_manhdung.taskmanager.data.local.database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.*;

public class TaskManagerDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "task_manager.db";
    private static final int DATABASE_VERSION = 7;

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

                    TaskTable.COMPLETED_AT + " INTEGER, " +
                    TaskTable.DELETED_AT + " INTEGER, " +
                    TaskTable.VERSION + " INTEGER NOT NULL DEFAULT 1, " +
                    TaskTable.SYNC_STATUS + " TEXT NOT NULL DEFAULT 'SYNCED', " +

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

    private static final String CREATE_TASK_SUBTASKS_TABLE =
            "CREATE TABLE " + TaskSubtaskTable.TABLE_NAME + " (" +
                    TaskSubtaskTable.SUBTASK_ID + " TEXT PRIMARY KEY, " +
                    TaskSubtaskTable.TASK_ID + " TEXT NOT NULL, " +
                    TaskSubtaskTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    TaskSubtaskTable.CREATED_BY + " TEXT NOT NULL, " +
                    TaskSubtaskTable.ASSIGNEE_ID + " TEXT, " +
                    TaskSubtaskTable.TITLE + " TEXT NOT NULL, " +
                    TaskSubtaskTable.ESTIMATED_MINUTES +
                    " INTEGER NOT NULL DEFAULT 0 CHECK (" +
                    TaskSubtaskTable.ESTIMATED_MINUTES + " >= 0), " +
                    TaskSubtaskTable.COMPLETED + " INTEGER NOT NULL DEFAULT 0, " +
                    TaskSubtaskTable.COMPLETED_AT + " INTEGER, " +
                    TaskSubtaskTable.DELETED_AT + " INTEGER, " +
                    TaskSubtaskTable.VERSION + " INTEGER NOT NULL DEFAULT 1, " +
                    TaskSubtaskTable.SYNC_STATUS + " TEXT NOT NULL DEFAULT 'SYNCED', " +
                    TaskSubtaskTable.SORT_ORDER + " INTEGER NOT NULL DEFAULT 0, " +
                    TaskSubtaskTable.CREATED_AT + " INTEGER NOT NULL, " +
                    TaskSubtaskTable.UPDATED_AT + " INTEGER, " +
                    "FOREIGN KEY (" + TaskSubtaskTable.TASK_ID + ") " +
                    "REFERENCES " + TaskTable.TABLE_NAME +
                    "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + TaskSubtaskTable.WORKSPACE_ID + ") " +
                    "REFERENCES " + WorkspaceTable.TABLE_NAME +
                    "(" + WorkspaceTable.WORKSPACE_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + TaskSubtaskTable.CREATED_BY + ") " +
                    "REFERENCES " + UserTable.TABLE_NAME +
                    "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_TASK_HISTORIES_TABLE =
            "CREATE TABLE " + TaskHistoryTable.TABLE_NAME + " (" +
                    TaskHistoryTable.HISTORY_ID + " TEXT PRIMARY KEY, " +
                    TaskHistoryTable.TASK_ID + " TEXT NOT NULL, " +
                    TaskHistoryTable.USER_ID + " TEXT NOT NULL, " +
                    TaskHistoryTable.ACTION + " TEXT NOT NULL, " +
                    TaskHistoryTable.DETAIL + " TEXT, " +
                    TaskHistoryTable.CREATED_AT + " INTEGER NOT NULL, " +
                    "FOREIGN KEY (" + TaskHistoryTable.TASK_ID + ") REFERENCES " +
                    TaskTable.TABLE_NAME + "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + TaskHistoryTable.USER_ID + ") REFERENCES " +
                    UserTable.TABLE_NAME + "(" + UserTable.USER_ID + ") ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_NOTIFICATIONS_TABLE =
            "CREATE TABLE " + NotificationTable.TABLE_NAME + " (" +
                    NotificationTable.NOTIFICATION_ID + " TEXT PRIMARY KEY, " +
                    NotificationTable.USER_ID + " TEXT NOT NULL, " +
                    NotificationTable.TASK_ID + " TEXT, " +
                    NotificationTable.WORKSPACE_ID + " TEXT, " +
                    NotificationTable.TYPE + " TEXT NOT NULL, " +
                    NotificationTable.TITLE + " TEXT NOT NULL, " +
                    NotificationTable.MESSAGE + " TEXT NOT NULL, " +
                    NotificationTable.CREATED_AT + " INTEGER NOT NULL, " +
                    NotificationTable.READ_AT + " INTEGER, " +
                    "FOREIGN KEY (" + NotificationTable.USER_ID + ") REFERENCES " +
                    UserTable.TABLE_NAME + "(" + UserTable.USER_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + NotificationTable.TASK_ID + ") REFERENCES " +
                    TaskTable.TABLE_NAME + "(" + TaskTable.TASK_ID + ") ON DELETE SET NULL" +
                    ")";

    private static final String CREATE_SYNC_QUEUE_TABLE =
            "CREATE TABLE " + SyncQueueTable.TABLE_NAME + " (" +
                    SyncQueueTable.QUEUE_ID + " TEXT PRIMARY KEY, " +
                    SyncQueueTable.ENTITY_TYPE + " TEXT NOT NULL, " +
                    SyncQueueTable.ENTITY_ID + " TEXT NOT NULL, " +
                    SyncQueueTable.OPERATION + " TEXT NOT NULL, " +
                    SyncQueueTable.VERSION + " INTEGER NOT NULL DEFAULT 1, " +
                    SyncQueueTable.ATTEMPT_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
                    SyncQueueTable.LAST_ERROR + " TEXT, " +
                    SyncQueueTable.CREATED_AT + " INTEGER NOT NULL, " +
                    SyncQueueTable.UPDATED_AT + " INTEGER NOT NULL" +
                    ")";

    private static final String CREATE_WORKSPACE_MEMBERS_TABLE =
            "CREATE TABLE " + WorkspaceMemberTable.TABLE_NAME + " (" +
                    WorkspaceMemberTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    WorkspaceMemberTable.USER_ID + " TEXT NOT NULL, " +
                    WorkspaceMemberTable.ROLE + " TEXT NOT NULL, " +
                    WorkspaceMemberTable.STATUS +
                    " TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    WorkspaceMemberTable.JOINED_AT + " INTEGER NOT NULL, " +
                    WorkspaceMemberTable.INVITE_ID + " TEXT, " +
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
                    TeamInviteTable.INVITED_USER_ID + " TEXT NOT NULL, " +
                    TeamInviteTable.INVITED_USER_CODE + " TEXT NOT NULL, " +
                    TeamInviteTable.INVITED_DISPLAY_NAME + " TEXT, " +
                    TeamInviteTable.WORKSPACE_NAME + " TEXT NOT NULL, " +
                    TeamInviteTable.ROLE + " TEXT NOT NULL, " +
                    TeamInviteTable.STATUS + " TEXT NOT NULL, " +
                    TeamInviteTable.INVITED_BY + " TEXT NOT NULL, " +
                    TeamInviteTable.CREATED_AT + " INTEGER NOT NULL, " +
                    TeamInviteTable.RESPONDED_AT + " INTEGER, " +
                    TeamInviteTable.EXPIRES_AT + " INTEGER NOT NULL" +
                    ")";

    private static final String CREATE_PROJECTS_TABLE =
            "CREATE TABLE " + ProjectTable.TABLE_NAME + " (" +
                    ProjectTable.PROJECT_ID + " TEXT PRIMARY KEY, " +
                    ProjectTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    ProjectTable.NAME + " TEXT NOT NULL, " +
                    ProjectTable.DESCRIPTION + " TEXT, " +
                    ProjectTable.STATUS + " TEXT NOT NULL DEFAULT 'ACTIVE', " +
                    ProjectTable.CREATED_BY + " TEXT NOT NULL, " +
                    ProjectTable.MANAGER_ID + " TEXT, " +
                    ProjectTable.START_DATE + " INTEGER, " +
                    ProjectTable.DUE_DATE + " INTEGER, " +
                    ProjectTable.COMPLETED_AT + " INTEGER, " +
                    ProjectTable.DELETED_AT + " INTEGER, " +
                    ProjectTable.VERSION + " INTEGER NOT NULL DEFAULT 1, " +
                    ProjectTable.SYNC_STATUS + " TEXT NOT NULL DEFAULT 'SYNCED', " +
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

    private static final String CREATE_TASK_COMMENTS_TABLE =
            "CREATE TABLE " + TaskCommentTable.TABLE_NAME + " (" +
                    TaskCommentTable.COMMENT_ID + " TEXT PRIMARY KEY, " +
                    TaskCommentTable.TASK_ID + " TEXT NOT NULL, " +
                    TaskCommentTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    TaskCommentTable.USER_ID + " TEXT NOT NULL, " +
                    TaskCommentTable.MESSAGE + " TEXT NOT NULL, " +
                    TaskCommentTable.CREATED_AT + " INTEGER NOT NULL, " +
                    TaskCommentTable.UPDATED_AT + " INTEGER NOT NULL, " +
                    TaskCommentTable.DELETED_AT + " INTEGER, " +
                    "FOREIGN KEY (" + TaskCommentTable.TASK_ID + ") REFERENCES " +
                    TaskTable.TABLE_NAME + "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE)";

    private static final String CREATE_TASK_ATTACHMENTS_TABLE =
            "CREATE TABLE " + TaskAttachmentTable.TABLE_NAME + " (" +
                    TaskAttachmentTable.ATTACHMENT_ID + " TEXT PRIMARY KEY, " +
                    TaskAttachmentTable.TASK_ID + " TEXT NOT NULL, " +
                    TaskAttachmentTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    TaskAttachmentTable.USER_ID + " TEXT NOT NULL, " +
                    TaskAttachmentTable.DISPLAY_NAME + " TEXT NOT NULL, " +
                    TaskAttachmentTable.MIME_TYPE + " TEXT, " +
                    TaskAttachmentTable.LOCAL_URI + " TEXT, " +
                    TaskAttachmentTable.REMOTE_URL + " TEXT, " +
                    TaskAttachmentTable.SIZE_BYTES + " INTEGER NOT NULL DEFAULT 0, " +
                    TaskAttachmentTable.CREATED_AT + " INTEGER NOT NULL, " +
                    TaskAttachmentTable.DELETED_AT + " INTEGER, " +
                    "FOREIGN KEY (" + TaskAttachmentTable.TASK_ID + ") REFERENCES " +
                    TaskTable.TABLE_NAME + "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE)";

    private static final String CREATE_TASK_DEPENDENCIES_TABLE =
            "CREATE TABLE " + TaskDependencyTable.TABLE_NAME + " (" +
                    TaskDependencyTable.TASK_ID + " TEXT NOT NULL, " +
                    TaskDependencyTable.DEPENDS_ON_TASK_ID + " TEXT NOT NULL, " +
                    TaskDependencyTable.CREATED_BY + " TEXT NOT NULL, " +
                    TaskDependencyTable.CREATED_AT + " INTEGER NOT NULL, " +
                    "PRIMARY KEY (" + TaskDependencyTable.TASK_ID + ", " +
                    TaskDependencyTable.DEPENDS_ON_TASK_ID + "), " +
                    "FOREIGN KEY (" + TaskDependencyTable.TASK_ID + ") REFERENCES " +
                    TaskTable.TABLE_NAME + "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY (" + TaskDependencyTable.DEPENDS_ON_TASK_ID + ") REFERENCES " +
                    TaskTable.TABLE_NAME + "(" + TaskTable.TASK_ID + ") ON DELETE CASCADE)";

    private static final String CREATE_PROJECT_MILESTONES_TABLE =
            "CREATE TABLE " + ProjectMilestoneTable.TABLE_NAME + " (" +
                    ProjectMilestoneTable.MILESTONE_ID + " TEXT PRIMARY KEY, " +
                    ProjectMilestoneTable.PROJECT_ID + " TEXT NOT NULL, " +
                    ProjectMilestoneTable.WORKSPACE_ID + " TEXT NOT NULL, " +
                    ProjectMilestoneTable.TITLE + " TEXT NOT NULL, " +
                    ProjectMilestoneTable.DUE_DATE + " INTEGER, " +
                    ProjectMilestoneTable.COMPLETED_AT + " INTEGER, " +
                    ProjectMilestoneTable.CREATED_BY + " TEXT NOT NULL, " +
                    ProjectMilestoneTable.CREATED_AT + " INTEGER NOT NULL, " +
                    "FOREIGN KEY (" + ProjectMilestoneTable.PROJECT_ID + ") REFERENCES " +
                    ProjectTable.TABLE_NAME + "(" + ProjectTable.PROJECT_ID + ") ON DELETE CASCADE)";

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
        database.execSQL(CREATE_TASK_SUBTASKS_TABLE);
        database.execSQL(CREATE_TASK_HISTORIES_TABLE);
        database.execSQL(CREATE_NOTIFICATIONS_TABLE);
        database.execSQL(CREATE_SYNC_QUEUE_TABLE);
        database.execSQL(CREATE_WORKSPACE_MEMBERS_TABLE);
        database.execSQL(CREATE_TEAM_INVITES_TABLE);
        database.execSQL(CREATE_TASK_ASSIGNEES_TABLE);
        database.execSQL(CREATE_TASK_COMMENTS_TABLE);
        database.execSQL(CREATE_TASK_ATTACHMENTS_TABLE);
        database.execSQL(CREATE_TASK_DEPENDENCIES_TABLE);
        database.execSQL(CREATE_PROJECT_MILESTONES_TABLE);

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
                "CREATE INDEX IF NOT EXISTS index_task_deleted " +
                        "ON " + TaskTable.TABLE_NAME +
                        "(" + TaskTable.WORKSPACE_ID + ", " + TaskTable.DELETED_AT + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_history_task " +
                        "ON " + TaskHistoryTable.TABLE_NAME +
                        "(" + TaskHistoryTable.TASK_ID + ", " +
                        TaskHistoryTable.CREATED_AT + " DESC)"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_notification_user " +
                        "ON " + NotificationTable.TABLE_NAME +
                        "(" + NotificationTable.USER_ID + ", " +
                        NotificationTable.READ_AT + ", " + NotificationTable.CREATED_AT + " DESC)"
        );

        database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_sync_entity " +
                        "ON " + SyncQueueTable.TABLE_NAME +
                        "(" + SyncQueueTable.ENTITY_TYPE + ", " + SyncQueueTable.ENTITY_ID + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_subtask_task " +
                        "ON " + TaskSubtaskTable.TABLE_NAME +
                        "(" + TaskSubtaskTable.TASK_ID + ", " +
                        TaskSubtaskTable.SORT_ORDER + ")"
        );

        database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_subtask_workspace " +
                        "ON " + TaskSubtaskTable.TABLE_NAME +
                        "(" + TaskSubtaskTable.WORKSPACE_ID + ")"
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
        if (oldVersion < 4) {
            database.execSQL(CREATE_TASK_SUBTASKS_TABLE);
            createIndexes(database);
        }
        if (oldVersion < 5) {
            database.execSQL("ALTER TABLE " + TaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskTable.COMPLETED_AT + " INTEGER");
            database.execSQL("ALTER TABLE " + TaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskTable.DELETED_AT + " INTEGER");
            database.execSQL("ALTER TABLE " + TaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskTable.VERSION + " INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE " + TaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskTable.SYNC_STATUS + " TEXT NOT NULL DEFAULT 'SYNCED'");
            database.execSQL("ALTER TABLE " + TaskSubtaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskSubtaskTable.COMPLETED_AT + " INTEGER");
            database.execSQL("ALTER TABLE " + TaskSubtaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskSubtaskTable.DELETED_AT + " INTEGER");
            database.execSQL("ALTER TABLE " + TaskSubtaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskSubtaskTable.VERSION + " INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE " + TaskSubtaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskSubtaskTable.SYNC_STATUS + " TEXT NOT NULL DEFAULT 'SYNCED'");
            database.execSQL(CREATE_TASK_HISTORIES_TABLE);
            database.execSQL(CREATE_NOTIFICATIONS_TABLE);
            database.execSQL(CREATE_SYNC_QUEUE_TABLE);
            createIndexes(database);
        }
        if (oldVersion < 6) {
            database.execSQL("ALTER TABLE " + TaskSubtaskTable.TABLE_NAME +
                    " ADD COLUMN " + TaskSubtaskTable.ASSIGNEE_ID + " TEXT");
            database.execSQL("ALTER TABLE " + NotificationTable.TABLE_NAME +
                    " ADD COLUMN " + NotificationTable.WORKSPACE_ID + " TEXT");
            database.execSQL("ALTER TABLE " + WorkspaceMemberTable.TABLE_NAME +
                    " ADD COLUMN " + WorkspaceMemberTable.INVITE_ID + " TEXT");
            database.execSQL("ALTER TABLE " + TeamInviteTable.TABLE_NAME +
                    " ADD COLUMN " + TeamInviteTable.INVITED_USER_ID + " TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE " + TeamInviteTable.TABLE_NAME +
                    " ADD COLUMN " + TeamInviteTable.INVITED_USER_CODE + " TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE " + TeamInviteTable.TABLE_NAME +
                    " ADD COLUMN " + TeamInviteTable.INVITED_DISPLAY_NAME + " TEXT");
            database.execSQL("ALTER TABLE " + TeamInviteTable.TABLE_NAME +
                    " ADD COLUMN " + TeamInviteTable.WORKSPACE_NAME + " TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE " + TeamInviteTable.TABLE_NAME +
                    " ADD COLUMN " + TeamInviteTable.EXPIRES_AT + " INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE " + TeamInviteTable.TABLE_NAME +
                    " RENAME TO team_invites_legacy");
            database.execSQL(CREATE_TEAM_INVITES_TABLE);
            database.execSQL("INSERT INTO " + TeamInviteTable.TABLE_NAME + " (" +
                    TeamInviteTable.INVITE_ID + "," + TeamInviteTable.WORKSPACE_ID + "," +
                    TeamInviteTable.EMAIL + "," + TeamInviteTable.INVITED_USER_ID + "," +
                    TeamInviteTable.INVITED_USER_CODE + "," +
                    TeamInviteTable.INVITED_DISPLAY_NAME + "," + TeamInviteTable.WORKSPACE_NAME + "," +
                    TeamInviteTable.ROLE + "," + TeamInviteTable.STATUS + "," +
                    TeamInviteTable.INVITED_BY + "," + TeamInviteTable.CREATED_AT + "," +
                    TeamInviteTable.RESPONDED_AT + "," + TeamInviteTable.EXPIRES_AT + ") SELECT " +
                    TeamInviteTable.INVITE_ID + "," + TeamInviteTable.WORKSPACE_ID + "," +
                    TeamInviteTable.EMAIL + "," + TeamInviteTable.INVITED_USER_ID + "," +
                    TeamInviteTable.INVITED_USER_CODE + "," +
                    TeamInviteTable.INVITED_DISPLAY_NAME + "," + TeamInviteTable.WORKSPACE_NAME + "," +
                    TeamInviteTable.ROLE + "," + TeamInviteTable.STATUS + "," +
                    TeamInviteTable.INVITED_BY + "," + TeamInviteTable.CREATED_AT + "," +
                    TeamInviteTable.RESPONDED_AT + "," + TeamInviteTable.EXPIRES_AT +
                    " FROM team_invites_legacy");
            database.execSQL("DROP TABLE team_invites_legacy");
            database.execSQL("ALTER TABLE " + ProjectTable.TABLE_NAME +
                    " ADD COLUMN " + ProjectTable.MANAGER_ID + " TEXT");
            database.execSQL("ALTER TABLE " + ProjectTable.TABLE_NAME +
                    " ADD COLUMN " + ProjectTable.START_DATE + " INTEGER");
            database.execSQL("ALTER TABLE " + ProjectTable.TABLE_NAME +
                    " ADD COLUMN " + ProjectTable.DUE_DATE + " INTEGER");
            database.execSQL("ALTER TABLE " + ProjectTable.TABLE_NAME +
                    " ADD COLUMN " + ProjectTable.COMPLETED_AT + " INTEGER");
            database.execSQL("ALTER TABLE " + ProjectTable.TABLE_NAME +
                    " ADD COLUMN " + ProjectTable.DELETED_AT + " INTEGER");
            database.execSQL("ALTER TABLE " + ProjectTable.TABLE_NAME +
                    " ADD COLUMN " + ProjectTable.VERSION + " INTEGER NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE " + ProjectTable.TABLE_NAME +
                    " ADD COLUMN " + ProjectTable.SYNC_STATUS + " TEXT NOT NULL DEFAULT 'SYNCED'");
            database.execSQL(CREATE_TASK_COMMENTS_TABLE);
            database.execSQL(CREATE_TASK_ATTACHMENTS_TABLE);
            database.execSQL(CREATE_TASK_DEPENDENCIES_TABLE);
            database.execSQL(CREATE_PROJECT_MILESTONES_TABLE);
            createIndexes(database);
        }
        if (oldVersion < 7) {
            // Chức năng bấm giờ đã bị loại khỏi sản phẩm. Xóa bảng cũ để
            // không tiếp tục giữ hoặc đồng bộ dữ liệu phiên làm việc.
            database.execSQL("DROP TABLE IF EXISTS work_sessions");
            database.execSQL("DROP INDEX IF EXISTS index_session_task");
            database.execSQL("DROP INDEX IF EXISTS index_one_active_session_per_user");
        }
    }
}
