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
        public static final String COMPLETED_AT = "completed_at";
        public static final String DELETED_AT = "deleted_at";
        public static final String VERSION = "version";
        public static final String SYNC_STATUS = "sync_status";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        private TaskTable() {
        }
    }

    public static final class TaskSubtaskTable {
        public static final String TABLE_NAME = "task_subtasks";

        public static final String SUBTASK_ID = "subtask_id";
        public static final String TASK_ID = "task_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String CREATED_BY = "created_by";
        public static final String ASSIGNEE_ID = "assignee_id";
        public static final String TITLE = "title";
        public static final String ESTIMATED_MINUTES = "estimated_minutes";
        public static final String COMPLETED = "completed";
        public static final String COMPLETED_AT = "completed_at";
        public static final String DELETED_AT = "deleted_at";
        public static final String VERSION = "version";
        public static final String SYNC_STATUS = "sync_status";
        public static final String SORT_ORDER = "sort_order";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        private TaskSubtaskTable() {
        }
    }

    public static final class TaskHistoryTable {
        public static final String TABLE_NAME = "task_histories";
        public static final String HISTORY_ID = "history_id";
        public static final String TASK_ID = "task_id";
        public static final String USER_ID = "user_id";
        public static final String ACTION = "action";
        public static final String DETAIL = "detail";
        public static final String CREATED_AT = "created_at";

        private TaskHistoryTable() {
        }
    }

    public static final class NotificationTable {
        public static final String TABLE_NAME = "notifications";
        public static final String NOTIFICATION_ID = "notification_id";
        public static final String USER_ID = "user_id";
        public static final String TASK_ID = "task_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String TYPE = "type";
        public static final String TITLE = "title";
        public static final String MESSAGE = "message";
        public static final String CREATED_AT = "created_at";
        public static final String READ_AT = "read_at";

        private NotificationTable() {
        }
    }

    public static final class SyncQueueTable {
        public static final String TABLE_NAME = "sync_queue";
        public static final String QUEUE_ID = "queue_id";
        public static final String ENTITY_TYPE = "entity_type";
        public static final String ENTITY_ID = "entity_id";
        public static final String OPERATION = "operation";
        public static final String VERSION = "version";
        public static final String ATTEMPT_COUNT = "attempt_count";
        public static final String LAST_ERROR = "last_error";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";

        private SyncQueueTable() {
        }
    }

    public static final class WorkspaceMemberTable {
        public static final String TABLE_NAME = "workspace_members";

        public static final String WORKSPACE_ID = "workspace_id";
        public static final String USER_ID = "user_id";
        public static final String ROLE = "role";
        public static final String STATUS = "status";
        public static final String JOINED_AT = "joined_at";
        public static final String INVITE_ID = "invite_id";

        private WorkspaceMemberTable() {
        }
    }

    public static final class TeamInviteTable {
        public static final String TABLE_NAME = "team_invites";

        public static final String INVITE_ID = "invite_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String EMAIL = "email";
        public static final String INVITED_USER_ID = "invited_user_id";
        public static final String INVITED_USER_CODE = "invited_user_code";
        public static final String INVITED_DISPLAY_NAME = "invited_display_name";
        public static final String WORKSPACE_NAME = "workspace_name";
        public static final String ROLE = "role";
        public static final String STATUS = "status";
        public static final String INVITED_BY = "invited_by";
        public static final String CREATED_AT = "created_at";
        public static final String RESPONDED_AT = "responded_at";
        public static final String EXPIRES_AT = "expires_at";

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
        public static final String MANAGER_ID = "manager_id";
        public static final String START_DATE = "start_date";
        public static final String DUE_DATE = "due_date";
        public static final String COMPLETED_AT = "completed_at";
        public static final String DELETED_AT = "deleted_at";
        public static final String VERSION = "version";
        public static final String SYNC_STATUS = "sync_status";
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

    public static final class TaskCommentTable {
        public static final String TABLE_NAME = "task_comments";
        public static final String COMMENT_ID = "comment_id";
        public static final String TASK_ID = "task_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String USER_ID = "user_id";
        public static final String MESSAGE = "message";
        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
        public static final String DELETED_AT = "deleted_at";

        private TaskCommentTable() { }
    }

    public static final class TaskAttachmentTable {
        public static final String TABLE_NAME = "task_attachments";
        public static final String ATTACHMENT_ID = "attachment_id";
        public static final String TASK_ID = "task_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String USER_ID = "user_id";
        public static final String DISPLAY_NAME = "display_name";
        public static final String MIME_TYPE = "mime_type";
        public static final String LOCAL_URI = "local_uri";
        public static final String REMOTE_URL = "remote_url";
        public static final String SIZE_BYTES = "size_bytes";
        public static final String CREATED_AT = "created_at";
        public static final String DELETED_AT = "deleted_at";

        private TaskAttachmentTable() { }
    }

    public static final class TaskDependencyTable {
        public static final String TABLE_NAME = "task_dependencies";
        public static final String TASK_ID = "task_id";
        public static final String DEPENDS_ON_TASK_ID = "depends_on_task_id";
        public static final String CREATED_BY = "created_by";
        public static final String CREATED_AT = "created_at";

        private TaskDependencyTable() { }
    }

    public static final class ProjectMilestoneTable {
        public static final String TABLE_NAME = "project_milestones";
        public static final String MILESTONE_ID = "milestone_id";
        public static final String PROJECT_ID = "project_id";
        public static final String WORKSPACE_ID = "workspace_id";
        public static final String TITLE = "title";
        public static final String DUE_DATE = "due_date";
        public static final String COMPLETED_AT = "completed_at";
        public static final String CREATED_BY = "created_by";
        public static final String CREATED_AT = "created_at";

        private ProjectMilestoneTable() { }
    }
}
