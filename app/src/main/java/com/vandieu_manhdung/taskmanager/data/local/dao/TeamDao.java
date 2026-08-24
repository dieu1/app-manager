package com.vandieu_manhdung.taskmanager.data.local.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.core.constant.MembershipStatus;
import com.vandieu_manhdung.taskmanager.core.constant.ProjectStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceStatus;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceType;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.ProjectTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.ProjectMilestoneTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskAssigneeTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TaskTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.TeamInviteTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.UserTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkspaceMemberTable;
import com.vandieu_manhdung.taskmanager.data.local.database.DatabaseContract.WorkspaceTable;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.ProjectMilestone;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.ArrayList;
import java.util.List;

public class TeamDao {

    private final TaskManagerDatabaseHelper databaseHelper;

    public TeamDao(Context context) {
        databaseHelper = TaskManagerDatabaseHelper.getInstance(context);
    }

    public List<Workspace> findTeamsForUser(String userId) {
        List<Workspace> workspaces = new ArrayList<>();
        String sql = "SELECT w.* FROM " + WorkspaceTable.TABLE_NAME + " w " +
                "INNER JOIN " + WorkspaceMemberTable.TABLE_NAME + " m ON " +
                "m." + WorkspaceMemberTable.WORKSPACE_ID + " = w." +
                WorkspaceTable.WORKSPACE_ID + " WHERE m." +
                WorkspaceMemberTable.USER_ID + " = ? AND m." +
                WorkspaceMemberTable.STATUS + " = ? AND w." +
                WorkspaceTable.TYPE + " = ? AND w." + WorkspaceTable.STATUS +
                " = ? ORDER BY w." + WorkspaceTable.UPDATED_AT + " DESC";

        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql,
                new String[]{
                        userId,
                        MembershipStatus.ACTIVE,
                        WorkspaceType.TEAM,
                        WorkspaceStatus.ACTIVE
                }
        )) {
            while (cursor.moveToNext()) {
                workspaces.add(mapWorkspace(cursor));
            }
        }
        return workspaces;
    }

    public boolean insertMember(WorkspaceMember member) {
        ContentValues values = new ContentValues();
        values.put(WorkspaceMemberTable.WORKSPACE_ID, member.getWorkspaceId());
        values.put(WorkspaceMemberTable.USER_ID, member.getUserId());
        values.put(WorkspaceMemberTable.ROLE, member.getRole());
        values.put(WorkspaceMemberTable.STATUS, member.getStatus());
        values.put(WorkspaceMemberTable.JOINED_AT, member.getJoinedAt());
        values.put(WorkspaceMemberTable.INVITE_ID, member.getInviteId());
        return databaseHelper.getWritableDatabase().insertWithOnConflict(
                WorkspaceMemberTable.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        ) != -1;
    }

    public WorkspaceMember findMember(String workspaceId, String userId) {
        String selection = WorkspaceMemberTable.WORKSPACE_ID + " = ? AND " +
                WorkspaceMemberTable.USER_ID + " = ?";
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                WorkspaceMemberTable.TABLE_NAME,
                null,
                selection,
                new String[]{workspaceId, userId},
                null,
                null,
                null,
                "1"
        )) {
            if (cursor.moveToFirst()) {
                return mapMember(cursor);
            }
        }
        return null;
    }

    public List<WorkspaceMember> findMembers(String workspaceId) {
        List<WorkspaceMember> members = new ArrayList<>();
        String sql = "SELECT m.*, u." + UserTable.DISPLAY_NAME +
                " AS member_name, u." + UserTable.EMAIL + " AS member_email, u." +
                UserTable.USER_CODE + " AS member_code, " +
                "(SELECT COUNT(*) FROM " + TaskAssigneeTable.TABLE_NAME + " ta " +
                "INNER JOIN " + TaskTable.TABLE_NAME + " t ON t." +
                TaskTable.TASK_ID + " = ta." + TaskAssigneeTable.TASK_ID +
                " WHERE ta." + TaskAssigneeTable.USER_ID + " = m." +
                WorkspaceMemberTable.USER_ID + " AND t." + TaskTable.WORKSPACE_ID +
                " = m." + WorkspaceMemberTable.WORKSPACE_ID + ") AS total_tasks, " +
                "(SELECT COUNT(*) FROM " + TaskAssigneeTable.TABLE_NAME + " ta " +
                "INNER JOIN " + TaskTable.TABLE_NAME + " t ON t." +
                TaskTable.TASK_ID + " = ta." + TaskAssigneeTable.TASK_ID +
                " WHERE ta." + TaskAssigneeTable.USER_ID + " = m." +
                WorkspaceMemberTable.USER_ID + " AND t." + TaskTable.WORKSPACE_ID +
                " = m." + WorkspaceMemberTable.WORKSPACE_ID + " AND t." +
                TaskTable.STATUS + " = ?) AS completed_tasks FROM " +
                WorkspaceMemberTable.TABLE_NAME + " m INNER JOIN " +
                UserTable.TABLE_NAME + " u ON u." + UserTable.USER_ID + " = m." +
                WorkspaceMemberTable.USER_ID + " WHERE m." +
                WorkspaceMemberTable.WORKSPACE_ID + " = ? AND m." +
                WorkspaceMemberTable.STATUS + " = ? ORDER BY CASE m." +
                WorkspaceMemberTable.ROLE + " WHEN 'OWNER' THEN 0 WHEN 'ADMIN' " +
                "THEN 1 ELSE 2 END, u." + UserTable.DISPLAY_NAME + " COLLATE NOCASE";

        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql,
                new String[]{TaskStatus.COMPLETED, workspaceId, MembershipStatus.ACTIVE}
        )) {
            while (cursor.moveToNext()) {
                WorkspaceMember member = mapMember(cursor);
                member.setDisplayName(cursor.getString(
                        cursor.getColumnIndexOrThrow("member_name")));
                member.setEmail(cursor.getString(
                        cursor.getColumnIndexOrThrow("member_email")));
                member.setUserCode(cursor.getString(
                        cursor.getColumnIndexOrThrow("member_code")));
                member.setTotalTasks(cursor.getInt(
                        cursor.getColumnIndexOrThrow("total_tasks")));
                member.setCompletedTasks(cursor.getInt(
                        cursor.getColumnIndexOrThrow("completed_tasks")));
                members.add(member);
            }
        }
        return members;
    }

    public int updateMemberRole(String workspaceId, String userId, String role) {
        ContentValues values = new ContentValues();
        values.put(WorkspaceMemberTable.ROLE, role);
        return databaseHelper.getWritableDatabase().update(
                WorkspaceMemberTable.TABLE_NAME,
                values,
                WorkspaceMemberTable.WORKSPACE_ID + " = ? AND " +
                        WorkspaceMemberTable.USER_ID + " = ?",
                new String[]{workspaceId, userId}
        );
    }

    public int removeMember(String workspaceId, String userId) {
        ContentValues values = new ContentValues();
        values.put(WorkspaceMemberTable.STATUS, MembershipStatus.REMOVED);
        return databaseHelper.getWritableDatabase().update(
                WorkspaceMemberTable.TABLE_NAME,
                values,
                WorkspaceMemberTable.WORKSPACE_ID + " = ? AND " +
                        WorkspaceMemberTable.USER_ID + " = ?",
                new String[]{workspaceId, userId}
        );
    }

    public int clearMemberAssignments(String workspaceId, String userId) {
        return databaseHelper.getWritableDatabase().delete(
                TaskAssigneeTable.TABLE_NAME,
                TaskAssigneeTable.USER_ID + " = ? AND " +
                        TaskAssigneeTable.TASK_ID + " IN (SELECT " +
                        TaskTable.TASK_ID + " FROM " + TaskTable.TABLE_NAME +
                        " WHERE " + TaskTable.WORKSPACE_ID + " = ?)",
                new String[]{userId, workspaceId}
        );
    }

    public boolean insertInvite(TeamInvite invite) {
        ContentValues values = new ContentValues();
        values.put(TeamInviteTable.INVITE_ID, invite.getInviteId());
        values.put(TeamInviteTable.WORKSPACE_ID, invite.getWorkspaceId());
        values.put(TeamInviteTable.EMAIL, invite.getEmail());
        values.put(TeamInviteTable.INVITED_USER_ID, invite.getInvitedUserId());
        values.put(TeamInviteTable.INVITED_USER_CODE, invite.getInvitedUserCode());
        values.put(TeamInviteTable.INVITED_DISPLAY_NAME, invite.getInvitedDisplayName());
        values.put(TeamInviteTable.WORKSPACE_NAME, invite.getWorkspaceName());
        values.put(TeamInviteTable.ROLE, invite.getRole());
        values.put(TeamInviteTable.STATUS, invite.getStatus());
        values.put(TeamInviteTable.INVITED_BY, invite.getInvitedBy());
        values.put(TeamInviteTable.CREATED_AT, invite.getCreatedAt());
        if (invite.getRespondedAt() > 0) {
            values.put(TeamInviteTable.RESPONDED_AT, invite.getRespondedAt());
        } else {
            values.putNull(TeamInviteTable.RESPONDED_AT);
        }
        values.put(TeamInviteTable.EXPIRES_AT, invite.getExpiresAt());
        return databaseHelper.getWritableDatabase().insert(
                TeamInviteTable.TABLE_NAME,
                null,
                values
        ) != -1;
    }

    public boolean saveInvite(TeamInvite invite) {
        ContentValues values = inviteValues(invite);
        return databaseHelper.getWritableDatabase().insertWithOnConflict(
                TeamInviteTable.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public TeamInvite findInvite(String inviteId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TeamInviteTable.TABLE_NAME, null,
                TeamInviteTable.INVITE_ID + " = ?", new String[]{inviteId},
                null, null, null, "1")) {
            return cursor.moveToFirst() ? mapInvite(cursor) : null;
        }
    }

    public TeamInvite findPendingInvite(String workspaceId, String invitedUserId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TeamInviteTable.TABLE_NAME, null,
                TeamInviteTable.WORKSPACE_ID + " = ? AND " +
                        TeamInviteTable.INVITED_USER_ID + " = ? AND " +
                        TeamInviteTable.STATUS + " = ?",
                new String[]{workspaceId, invitedUserId, "PENDING"},
                null, null, TeamInviteTable.CREATED_AT + " DESC", "1")) {
            return cursor.moveToFirst() ? mapInvite(cursor) : null;
        }
    }

    public List<TeamInvite> findPendingInvitesForUser(String userId, long now) {
        List<TeamInvite> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TeamInviteTable.TABLE_NAME, null,
                TeamInviteTable.INVITED_USER_ID + " = ? AND " +
                        TeamInviteTable.STATUS + " = ? AND (" +
                        TeamInviteTable.EXPIRES_AT + " = 0 OR " +
                        TeamInviteTable.EXPIRES_AT + " > ?)",
                new String[]{userId, "PENDING", String.valueOf(now)},
                null, null, TeamInviteTable.CREATED_AT + " DESC")) {
            while (cursor.moveToNext()) result.add(mapInvite(cursor));
        }
        return result;
    }

    public int updateInviteStatus(String inviteId, String status, long respondedAt) {
        ContentValues values = new ContentValues();
        values.put(TeamInviteTable.STATUS, status);
        values.put(TeamInviteTable.RESPONDED_AT, respondedAt);
        return databaseHelper.getWritableDatabase().update(
                TeamInviteTable.TABLE_NAME, values,
                TeamInviteTable.INVITE_ID + " = ?", new String[]{inviteId});
    }

    public boolean insertProject(Project project) {
        return databaseHelper.getWritableDatabase().insert(
                ProjectTable.TABLE_NAME,
                null,
                projectValues(project)
        ) != -1;
    }

    public boolean saveProject(Project project) {
        return findProjectById(project.getProjectId()) == null
                ? insertProject(project)
                : updateProject(project) > 0;
    }

    public int deleteProject(String projectId) {
        return databaseHelper.getWritableDatabase().delete(
                ProjectTable.TABLE_NAME,
                ProjectTable.PROJECT_ID + " = ?",
                new String[]{projectId}
        );
    }

    public int updateProject(Project project) {
        return databaseHelper.getWritableDatabase().update(
                ProjectTable.TABLE_NAME,
                projectValues(project),
                ProjectTable.PROJECT_ID + " = ?",
                new String[]{project.getProjectId()}
        );
    }

    public Project findProjectById(String projectId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                ProjectTable.TABLE_NAME,
                null,
                ProjectTable.PROJECT_ID + " = ?",
                new String[]{projectId},
                null,
                null,
                null,
                "1"
        )) {
            return cursor.moveToFirst() ? mapProject(cursor) : null;
        }
    }

    public List<Project> findProjects(String workspaceId) {
        List<Project> projects = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                ProjectTable.TABLE_NAME,
                null,
                ProjectTable.WORKSPACE_ID + " = ? AND " +
                        ProjectTable.STATUS + " = ?",
                new String[]{workspaceId, ProjectStatus.ACTIVE},
                null,
                null,
                ProjectTable.NAME + " COLLATE NOCASE"
        )) {
            while (cursor.moveToNext()) {
                projects.add(mapProject(cursor));
            }
        }
        return projects;
    }

    public boolean saveMilestone(ProjectMilestone item) {
        ContentValues values = new ContentValues();
        values.put(ProjectMilestoneTable.MILESTONE_ID, item.getMilestoneId());
        values.put(ProjectMilestoneTable.PROJECT_ID, item.getProjectId());
        values.put(ProjectMilestoneTable.WORKSPACE_ID, item.getWorkspaceId());
        values.put(ProjectMilestoneTable.TITLE, item.getTitle());
        values.put(ProjectMilestoneTable.DUE_DATE, item.getDueDate());
        values.put(ProjectMilestoneTable.COMPLETED_AT, item.getCompletedAt());
        values.put(ProjectMilestoneTable.CREATED_BY, item.getCreatedBy());
        values.put(ProjectMilestoneTable.CREATED_AT, item.getCreatedAt());
        return databaseHelper.getWritableDatabase().insertWithOnConflict(
                ProjectMilestoneTable.TABLE_NAME, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public List<ProjectMilestone> findMilestones(String projectId) {
        List<ProjectMilestone> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                ProjectMilestoneTable.TABLE_NAME, null,
                ProjectMilestoneTable.PROJECT_ID + " = ?", new String[]{projectId},
                null, null, ProjectMilestoneTable.DUE_DATE + " ASC")) {
            while (cursor.moveToNext()) {
                ProjectMilestone item = new ProjectMilestone();
                item.setMilestoneId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.MILESTONE_ID)));
                item.setProjectId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.PROJECT_ID)));
                item.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.WORKSPACE_ID)));
                item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.TITLE)));
                item.setDueDate(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.DUE_DATE)));
                item.setCompletedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.COMPLETED_AT)));
                item.setCreatedBy(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.CREATED_BY)));
                item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.CREATED_AT)));
                result.add(item);
            }
        }
        return result;
    }

    public ProjectMilestone findMilestone(String milestoneId) {
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                ProjectMilestoneTable.TABLE_NAME, null,
                ProjectMilestoneTable.MILESTONE_ID + " = ?", new String[]{milestoneId},
                null, null, null, "1")) {
            if (!cursor.moveToFirst()) return null;
            ProjectMilestone item = new ProjectMilestone();
            item.setMilestoneId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.MILESTONE_ID)));
            item.setProjectId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.PROJECT_ID)));
            item.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.WORKSPACE_ID)));
            item.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.TITLE)));
            item.setDueDate(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.DUE_DATE)));
            item.setCompletedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.COMPLETED_AT)));
            item.setCreatedBy(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.CREATED_BY)));
            item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectMilestoneTable.CREATED_AT)));
            return item;
        }
    }

    public int clearTaskAssignees(String taskId) {
        return databaseHelper.getWritableDatabase().delete(
                TaskAssigneeTable.TABLE_NAME,
                TaskAssigneeTable.TASK_ID + " = ?",
                new String[]{taskId}
        );
    }

    public String findTaskAssigneeId(String taskId) {
        List<String> assigneeIds = findTaskAssigneeIds(taskId);
        return assigneeIds.isEmpty() ? null : assigneeIds.get(0);
    }

    public List<String> findTaskAssigneeIds(String taskId) {
        List<String> result = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().query(
                TaskAssigneeTable.TABLE_NAME,
                new String[]{TaskAssigneeTable.USER_ID},
                TaskAssigneeTable.TASK_ID + " = ?",
                new String[]{taskId}, null, null,
                TaskAssigneeTable.ASSIGNED_AT + " ASC")) {
            while (cursor.moveToNext()) result.add(cursor.getString(0));
        }
        return result;
    }

    public boolean insertTaskAssignee(
            String taskId,
            String userId,
            String assignedBy,
            long assignedAt
    ) {
        ContentValues values = new ContentValues();
        values.put(TaskAssigneeTable.TASK_ID, taskId);
        values.put(TaskAssigneeTable.USER_ID, userId);
        values.put(TaskAssigneeTable.ASSIGNED_BY, assignedBy);
        values.put(TaskAssigneeTable.ASSIGNED_AT, assignedAt);
        return databaseHelper.getWritableDatabase().insert(
                TaskAssigneeTable.TABLE_NAME,
                null,
                values
        ) != -1;
    }

    public List<TeamTaskItem> queryTeamTasks(
            String workspaceId,
            String projectId,
            String assigneeId,
            String status
    ) {
        return queryTeamTasks(workspaceId, projectId, assigneeId, status, 0);
    }

    public List<TeamTaskItem> queryTeamTasks(
            String workspaceId,
            String projectId,
            String assigneeId,
            String status,
            int limit
    ) {
        StringBuilder where = new StringBuilder("t." + TaskTable.WORKSPACE_ID + " = ?");
        List<String> args = new ArrayList<>();
        args.add(workspaceId);
        if (projectId != null && !projectId.isBlank()) {
            where.append(" AND t.").append(TaskTable.PROJECT_ID).append(" = ?");
            args.add(projectId);
        }
        if (assigneeId != null && !assigneeId.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM ")
                    .append(TaskAssigneeTable.TABLE_NAME).append(" taf WHERE taf.")
                    .append(TaskAssigneeTable.TASK_ID).append(" = t.")
                    .append(TaskTable.TASK_ID).append(" AND taf.")
                    .append(TaskAssigneeTable.USER_ID).append(" = ?)");
            args.add(assigneeId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND t.").append(TaskTable.STATUS).append(" = ?");
            args.add(status);
        }

        String sql = taskJoinSql() + " WHERE " + where +
                " ORDER BY CASE WHEN t." + TaskTable.DUE_DATE +
                " IS NULL OR t." + TaskTable.DUE_DATE + " = 0 THEN 1 ELSE 0 END, " +
                "t." + TaskTable.DUE_DATE + " ASC, t." + TaskTable.CREATED_AT + " DESC" +
                (limit > 0 ? " LIMIT " + limit : "");
        List<TeamTaskItem> tasks = new ArrayList<>();
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql,
                args.toArray(new String[0])
        )) {
            while (cursor.moveToNext()) {
                tasks.add(mapTeamTask(cursor));
            }
        }
        return tasks;
    }

    public TeamTaskItem findTeamTaskById(String taskId) {
        String sql = taskJoinSql() + " WHERE t." + TaskTable.TASK_ID + " = ? LIMIT 1";
        try (Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                sql,
                new String[]{taskId}
        )) {
            return cursor.moveToFirst() ? mapTeamTask(cursor) : null;
        }
    }

    private String taskJoinSql() {
        return "SELECT t.*, p." + ProjectTable.NAME + " AS project_name, " +
                "(SELECT GROUP_CONCAT(ta." + TaskAssigneeTable.USER_ID + ", CHAR(31)) FROM " +
                TaskAssigneeTable.TABLE_NAME + " ta WHERE ta." + TaskAssigneeTable.TASK_ID +
                " = t." + TaskTable.TASK_ID + ") AS assignee_ids, " +
                "(SELECT GROUP_CONCAT(COALESCE(u." + UserTable.DISPLAY_NAME + ", ta." +
                TaskAssigneeTable.USER_ID + "), CHAR(31)) FROM " + TaskAssigneeTable.TABLE_NAME +
                " ta LEFT JOIN " + UserTable.TABLE_NAME + " u ON u." + UserTable.USER_ID +
                " = ta." + TaskAssigneeTable.USER_ID + " WHERE ta." + TaskAssigneeTable.TASK_ID +
                " = t." + TaskTable.TASK_ID + ") AS assignee_names FROM " +
                TaskTable.TABLE_NAME + " t LEFT JOIN " + ProjectTable.TABLE_NAME +
                " p ON p." + ProjectTable.PROJECT_ID + " = t." + TaskTable.PROJECT_ID +
                " ";
    }

    private ContentValues projectValues(Project project) {
        ContentValues values = new ContentValues();
        values.put(ProjectTable.PROJECT_ID, project.getProjectId());
        values.put(ProjectTable.WORKSPACE_ID, project.getWorkspaceId());
        values.put(ProjectTable.NAME, project.getName());
        values.put(ProjectTable.DESCRIPTION, project.getDescription());
        values.put(ProjectTable.STATUS, project.getStatus());
        values.put(ProjectTable.CREATED_BY, project.getCreatedBy());
        values.put(ProjectTable.MANAGER_ID, project.getManagerId());
        values.put(ProjectTable.START_DATE, project.getStartDate());
        values.put(ProjectTable.DUE_DATE, project.getDueDate());
        values.put(ProjectTable.COMPLETED_AT, project.getCompletedAt());
        values.put(ProjectTable.DELETED_AT, project.getDeletedAt());
        values.put(ProjectTable.VERSION, Math.max(1, project.getVersion()));
        values.put(ProjectTable.SYNC_STATUS, project.getSyncStatus());
        values.put(ProjectTable.CREATED_AT, project.getCreatedAt());
        values.put(ProjectTable.UPDATED_AT, project.getUpdatedAt());
        return values;
    }

    private ContentValues inviteValues(TeamInvite invite) {
        ContentValues values = new ContentValues();
        values.put(TeamInviteTable.INVITE_ID, invite.getInviteId());
        values.put(TeamInviteTable.WORKSPACE_ID, invite.getWorkspaceId());
        values.put(TeamInviteTable.EMAIL, invite.getEmail());
        values.put(TeamInviteTable.INVITED_USER_ID, invite.getInvitedUserId());
        values.put(TeamInviteTable.INVITED_USER_CODE, invite.getInvitedUserCode());
        values.put(TeamInviteTable.INVITED_DISPLAY_NAME, invite.getInvitedDisplayName());
        values.put(TeamInviteTable.WORKSPACE_NAME, invite.getWorkspaceName());
        values.put(TeamInviteTable.ROLE, invite.getRole());
        values.put(TeamInviteTable.STATUS, invite.getStatus());
        values.put(TeamInviteTable.INVITED_BY, invite.getInvitedBy());
        values.put(TeamInviteTable.CREATED_AT, invite.getCreatedAt());
        values.put(TeamInviteTable.RESPONDED_AT, invite.getRespondedAt());
        values.put(TeamInviteTable.EXPIRES_AT, invite.getExpiresAt());
        return values;
    }

    private Workspace mapWorkspace(Cursor cursor) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceTable.WORKSPACE_ID)));
        workspace.setManagerId(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceTable.MANAGER_ID)));
        workspace.setName(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceTable.NAME)));
        workspace.setType(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceTable.TYPE)));
        workspace.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceTable.DESCRIPTION)));
        workspace.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceTable.STATUS)));
        workspace.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(WorkspaceTable.CREATED_AT)));
        workspace.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(WorkspaceTable.UPDATED_AT)));
        return workspace;
    }

    private WorkspaceMember mapMember(Cursor cursor) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceMemberTable.WORKSPACE_ID)));
        member.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceMemberTable.USER_ID)));
        member.setRole(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceMemberTable.ROLE)));
        member.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(WorkspaceMemberTable.STATUS)));
        member.setJoinedAt(cursor.getLong(cursor.getColumnIndexOrThrow(WorkspaceMemberTable.JOINED_AT)));
        int inviteColumn = cursor.getColumnIndex(WorkspaceMemberTable.INVITE_ID);
        if (inviteColumn >= 0 && !cursor.isNull(inviteColumn)) {
            member.setInviteId(cursor.getString(inviteColumn));
        }
        return member;
    }

    private TeamInvite mapInvite(Cursor cursor) {
        TeamInvite invite = new TeamInvite();
        invite.setInviteId(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.INVITE_ID)));
        invite.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.WORKSPACE_ID)));
        invite.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.EMAIL)));
        invite.setInvitedUserId(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.INVITED_USER_ID)));
        invite.setInvitedUserCode(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.INVITED_USER_CODE)));
        invite.setInvitedDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.INVITED_DISPLAY_NAME)));
        invite.setWorkspaceName(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.WORKSPACE_NAME)));
        invite.setRole(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.ROLE)));
        invite.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.STATUS)));
        invite.setInvitedBy(cursor.getString(cursor.getColumnIndexOrThrow(TeamInviteTable.INVITED_BY)));
        invite.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(TeamInviteTable.CREATED_AT)));
        invite.setRespondedAt(cursor.getLong(cursor.getColumnIndexOrThrow(TeamInviteTable.RESPONDED_AT)));
        invite.setExpiresAt(cursor.getLong(cursor.getColumnIndexOrThrow(TeamInviteTable.EXPIRES_AT)));
        return invite;
    }

    private Project mapProject(Cursor cursor) {
        Project project = new Project();
        project.setProjectId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.PROJECT_ID)));
        project.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.WORKSPACE_ID)));
        project.setName(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.NAME)));
        project.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.DESCRIPTION)));
        project.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.STATUS)));
        project.setCreatedBy(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.CREATED_BY)));
        project.setManagerId(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.MANAGER_ID)));
        project.setStartDate(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectTable.START_DATE)));
        project.setDueDate(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectTable.DUE_DATE)));
        project.setCompletedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectTable.COMPLETED_AT)));
        project.setDeletedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectTable.DELETED_AT)));
        project.setVersion(cursor.getInt(cursor.getColumnIndexOrThrow(ProjectTable.VERSION)));
        project.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(ProjectTable.SYNC_STATUS)));
        project.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectTable.CREATED_AT)));
        project.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectTable.UPDATED_AT)));
        return project;
    }

    private TeamTaskItem mapTeamTask(Cursor cursor) {
        Task task = new Task();
        task.setTaskId(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.TASK_ID)));
        task.setWorkspaceId(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.WORKSPACE_ID)));
        int projectColumn = cursor.getColumnIndexOrThrow(TaskTable.PROJECT_ID);
        task.setProjectId(cursor.isNull(projectColumn) ? null : cursor.getString(projectColumn));
        task.setCreatedBy(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.CREATED_BY)));
        task.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.TITLE)));
        task.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.DESCRIPTION)));
        task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.STATUS)));
        task.setPriority(cursor.getString(cursor.getColumnIndexOrThrow(TaskTable.PRIORITY)));
        task.setProgress(cursor.getInt(cursor.getColumnIndexOrThrow(TaskTable.PROGRESS)));
        int startColumn = cursor.getColumnIndexOrThrow(TaskTable.START_DATE);
        task.setStartDate(cursor.isNull(startColumn) ? 0 : cursor.getLong(startColumn));
        int dueColumn = cursor.getColumnIndexOrThrow(TaskTable.DUE_DATE);
        task.setDueDate(cursor.isNull(dueColumn) ? 0 : cursor.getLong(dueColumn));
        task.setEstimatedMinutes(cursor.getInt(cursor.getColumnIndexOrThrow(TaskTable.ESTIMATED_MINUTES)));
        task.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.CREATED_AT)));
        task.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(TaskTable.UPDATED_AT)));

        int projectNameColumn = cursor.getColumnIndexOrThrow("project_name");
        int assigneeIdsColumn = cursor.getColumnIndexOrThrow("assignee_ids");
        int assigneeNamesColumn = cursor.getColumnIndexOrThrow("assignee_names");
        return new TeamTaskItem(
                task,
                cursor.isNull(projectNameColumn) ? null : cursor.getString(projectNameColumn),
                splitAssigneeValues(cursor, assigneeIdsColumn),
                splitAssigneeValues(cursor, assigneeNamesColumn)
        );
    }

    private List<String> splitAssigneeValues(Cursor cursor, int column) {
        if (cursor.isNull(column)) return List.of();
        return List.of(cursor.getString(column).split(String.valueOf((char) 31), -1));
    }
}
