package com.vandieu_manhdung.taskmanager.data.local.database;

public final class DatabaseContract {

    private DatabaseContract() {
    }

    public static final class UserTable {
        public static final String TABLE_NAME = "users";

        public static final String USER_ID = "user_id";
        public static final String USER_CODE = "user_code";
        public static final String EMAIL = "email";
        public static final String DISPLAY_NAME = "display_name";
        public static final String AVATAR_URL = "avatar_url";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        private UserTable() {
        }
    }

    public static final class WorkspaceTable {
        public static final String TABLE_NAME = "workspaces";

        public static final String WORKSPACE_ID = "workspace_id";
        public static final String MANAGER_ID = "manager_id";
        public static final String NAME = "name";
        public static final String TYPE = "type";
        public static final String DESCRIPTION = "description";
        public static final String STATUS = "status";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        private WorkspaceTable() {
        }
    }

    public static final class TaskTable {
        public static final String TABLE_NAME = "tasks";

        public static final String TASK_ID = "task_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String PROJECT_ID = "project_id";
        public static final String CREATED_BY = "created_by";
        public static final String TITLE = "title";
        public static final String DESCRIPTION = "description";
        public static final String STATUS = "status";
        public static final String PRIORITY = "priority";
        public static final String PROGRESS = "progress";
        public static final String START_DATE = "start_date";
        public static final String DUE_DATE = "due_date";
        public static final String ESTIMATED_MINUTES = "estimated_minutes";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        private TaskTable() {
        }
    }

    public static final class WorkSessionTable {
        public static final String TABLE_NAME = "work_sessions";

        public static final String SESSION_ID = "session_id";
        public static final String TASK_ID = "task_id";
        public static final String USER_ID = "user_id";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String DURATION_MINUTES = "duration_minutes";

        private WorkSessionTable() {
        }
    }

    public static final class WorkspaceMemberTable {
        public static final String TABLE_NAME = "workspace_members";

        public static final String WORKSPACE_ID = "workspace_id";
        public static final String USER_ID = "user_id";
        public static final String ROLE = "role";
        public static final String STATUS = "status";
        public static final String JOINED_AT = "joined_at";

        private WorkspaceMemberTable() {
        }
    }

    public static final class TeamInviteTable {
        public static final String TABLE_NAME = "team_invites";

        public static final String INVITE_ID = "invite_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String EMAIL = "email";
        public static final String ROLE = "role";
        public static final String STATUS = "status";
        public static final String INVITED_BY = "invited_by";
        public static final String CREATED_AT = "created_at";
        public static final String RESPONDED_AT = "responded_at";

        private TeamInviteTable() {
        }
    }

    public static final class ProjectTable {
        public static final String TABLE_NAME = "projects";

        public static final String PROJECT_ID = "project_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String STATUS = "status";
        public static final String CREATED_BY = "created_by";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        private ProjectTable() {
        }
    }

    public static final class TaskAssigneeTable {
        public static final String TABLE_NAME = "task_assignees";

        public static final String TASK_ID = "task_id";
        public static final String USER_ID = "user_id";
        public static final String ASSIGNED_BY = "assigned_by";
        public static final String ASSIGNED_AT = "assigned_at";

        private TaskAssigneeTable() {
        }
    }
}
