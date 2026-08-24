package com.vandieu_manhdung.taskmanager.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.InviteStatus;
import com.vandieu_manhdung.taskmanager.core.constant.MembershipStatus;
import com.vandieu_manhdung.taskmanager.core.constant.ProjectStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TeamRole;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceStatus;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceType;
import com.vandieu_manhdung.taskmanager.core.constant.SyncStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TaskHistoryAction;
import com.vandieu_manhdung.taskmanager.core.notification.TaskReminderScheduler;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.core.util.TeamRules;
import com.vandieu_manhdung.taskmanager.core.util.TeamFeatureRules;
import com.vandieu_manhdung.taskmanager.core.util.UserCodeRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.UserDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkspaceDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.NotificationDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskHistoryDao;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.ProjectMilestone;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;
import com.vandieu_manhdung.taskmanager.model.TaskHistory;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.Callable;

public class TeamRepository {

    private final TaskManagerDatabaseHelper databaseHelper;
    private final WorkspaceDao workspaceDao;
    private final UserDao userDao;
    private final TaskDao taskDao;
    private final TeamDao teamDao;
    private final AppExecutors executors;
    private final CloudSyncManager cloudSync;
    private final NotificationDao notificationDao;
    private final TaskHistoryDao historyDao;
    private final TaskReminderScheduler reminderScheduler;

    public TeamRepository(Context context) {
        Context appContext = context.getApplicationContext();
        databaseHelper = TaskManagerDatabaseHelper.getInstance(appContext);
        workspaceDao = new WorkspaceDao(appContext);
        userDao = new UserDao(appContext);
        taskDao = new TaskDao(appContext);
        teamDao = new TeamDao(appContext);
        executors = AppExecutors.getInstance();
        cloudSync = CloudSyncManager.getInstance(appContext);
        notificationDao = new NotificationDao(appContext);
        historyDao = new TaskHistoryDao(appContext);
        reminderScheduler = new TaskReminderScheduler(appContext);
    }

    public void getTeams(
            String userId,
            RepositoryCallback<List<Workspace>> callback
    ) {
        execute(() -> {
            requireText(userId, "Thiếu người dùng hiện tại");
            return teamDao.findTeamsForUser(userId);
        }, callback);
    }

    public void createTeam(
            String userId,
            String name,
            String description,
            RepositoryCallback<Workspace> callback
    ) {
        execute(() -> {
            requireText(userId, "Thiếu người dùng hiện tại");
            String cleanName = requireText(name, "Tên nhóm không được để trống");
            if (userDao.findById(userId) == null) {
                throw new IllegalStateException("Người dùng hiện tại không tồn tại");
            }

            long now = System.currentTimeMillis();
            Workspace workspace = new Workspace();
            workspace.setWorkspaceId(UUID.randomUUID().toString());
            workspace.setManagerId(userId);
            workspace.setName(cleanName);
            workspace.setDescription(cleanText(description));
            workspace.setType(WorkspaceType.TEAM);
            workspace.setStatus(WorkspaceStatus.ACTIVE);
            workspace.setCreatedAt(now);
            workspace.setUpdatedAt(now);

            WorkspaceMember owner = new WorkspaceMember();
            owner.setWorkspaceId(workspace.getWorkspaceId());
            owner.setUserId(userId);
            owner.setRole(TeamRole.OWNER);
            owner.setStatus(MembershipStatus.ACTIVE);
            owner.setJoinedAt(now);

            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            try {
                if (!workspaceDao.insert(workspace) || !teamDao.insertMember(owner)) {
                    throw new IllegalStateException("Không thể tạo nhóm");
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            cloudSync.upsertWorkspace(workspace, userId, TeamRole.OWNER);
            return workspace;
        }, callback);
    }

    public void updateTeam(
            String workspaceId,
            String actorId,
            String name,
            String description,
            RepositoryCallback<Workspace> callback
    ) {
        execute(() -> {
            Workspace workspace = requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            if (!TeamRules.canManageWorkspace(actor.getRole())) {
                throw new SecurityException("Chỉ chủ nhóm được sửa thông tin nhóm");
            }
            workspace.setName(requireText(name, "Tên nhóm không được để trống"));
            workspace.setDescription(cleanText(description));
            workspace.setUpdatedAt(System.currentTimeMillis());
            if (workspaceDao.update(workspace) <= 0) {
                throw new IllegalStateException("Không thể cập nhật nhóm");
            }
            cloudSync.upsertWorkspace(workspace, actorId, actor.getRole());
            return workspace;
        }, callback);
    }

    public void deleteTeam(
            String workspaceId,
            String actorId,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            if (!TeamRules.canManageWorkspace(actor.getRole())) {
                throw new SecurityException("Chỉ chủ nhóm được giải tán nhóm");
            }
            Workspace workspace = requireTeam(workspaceId);
            workspace.setStatus(WorkspaceStatus.ARCHIVED);
            workspace.setUpdatedAt(System.currentTimeMillis());
            if (workspaceDao.update(workspace) <= 0) {
                throw new IllegalStateException("Không thể giải tán nhóm");
            }
            cloudSync.upsertWorkspace(workspace, actorId, actor.getRole());
            return true;
        }, callback);
    }

    public void getTeamSnapshot(
            String workspaceId,
            String actorId,
            String projectFilter,
            String assigneeFilter,
            String statusFilter,
            RepositoryCallback<TeamWorkspaceSnapshot> callback
    ) {
        execute(() -> {
            Workspace workspace = requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            List<TeamTaskItem> filteredTasks = teamDao.queryTeamTasks(
                    workspaceId,
                    projectFilter,
                    assigneeFilter,
                    statusFilter
            );
            List<TeamTaskItem> allTasks = teamDao.queryTeamTasks(
                    workspaceId,
                    null,
                    null,
                    null
            );
            return new TeamWorkspaceSnapshot(
                    workspace,
                    actor.getRole(),
                    teamDao.findMembers(workspaceId),
                    teamDao.findProjects(workspaceId),
                    filteredTasks,
                    allTasks,
                    System.currentTimeMillis()
            );
        }, callback);
    }

    public void getTeamTask(
            String taskId,
            String actorId,
            RepositoryCallback<TeamTaskItem> callback
    ) {
        execute(() -> {
            TeamTaskItem item = teamDao.findTeamTaskById(requireText(taskId, "Thiếu công việc"));
            if (item == null) throw new IllegalStateException("Công việc không tồn tại");
            requireActiveMember(item.getTask().getWorkspaceId(), actorId);
            return item;
        }, callback);
    }

    public void getTeamSnapshotPage(
            String workspaceId,
            String actorId,
            String projectFilter,
            String assigneeFilter,
            String statusFilter,
            int visibleLimit,
            RepositoryCallback<TeamWorkspaceSnapshot> callback
    ) {
        execute(() -> {
            Workspace workspace = requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            int safeLimit = Math.max(1, visibleLimit);
            List<TeamTaskItem> page = teamDao.queryTeamTasks(
                    workspaceId, projectFilter, assigneeFilter, statusFilter,
                    safeLimit + 1);
            boolean hasMore = page.size() > safeLimit;
            if (hasMore) page = new ArrayList<>(page.subList(0, safeLimit));
            List<TeamTaskItem> allTasks = teamDao.queryTeamTasks(
                    workspaceId, null, null, null);
            return new TeamWorkspaceSnapshot(
                    workspace, actor.getRole(), teamDao.findMembers(workspaceId),
                    teamDao.findProjects(workspaceId), page, allTasks,
                    System.currentTimeMillis(), hasMore);
        }, callback);
    }

    public void addMember(
            String workspaceId,
            String actorId,
            String userCode,
            String role,
            RepositoryCallback<TeamInvite> callback
    ) {
        final String normalizedUserCode;
        try {
            normalizedUserCode = UserCodeRules.normalize(requireText(
                    userCode,
                    "Mã người dùng không được để trống"
            ));
            if (!UserCodeRules.isValid(normalizedUserCode)) {
                throw new IllegalArgumentException(
                        "Mã người dùng không đúng định dạng USR-XXXXXXXXXXXX");
            }
        } catch (Exception exception) {
            callback.onError(exception);
            return;
        }

        if (!cloudSync.isAvailable()) {
            callback.onError(new IllegalStateException(
                    "Cần kết nối Firebase để thêm thành viên trên nhiều thiết bị"));
            return;
        }

        cloudSync.findRegisteredUserByCode(
                normalizedUserCode,
                new RepositoryCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        if (user == null) {
                            callback.onError(new IllegalStateException(
                                    "Không tìm thấy tài khoản có mã này"));
                            return;
                        }
                        execute(() -> createPendingInvite(
                                workspaceId, actorId, user, role),
                                new RepositoryCallback<TeamInvite>() {
                                    @Override
                                    public void onSuccess(TeamInvite invite) {
                                        cloudSync.upsertInvite(invite, new RepositoryCallback<Boolean>() {
                                            @Override public void onSuccess(Boolean ignored) {
                                                callback.onSuccess(invite);
                                            }
                                            @Override public void onError(Exception exception) {
                                                callback.onError(exception);
                                            }
                                        });
                                    }
                                    @Override public void onError(Exception exception) {
                                        callback.onError(exception);
                                    }
                                });
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                }
        );
    }

    private TeamInvite createPendingInvite(
            String workspaceId,
            String actorId,
            User user,
            String role
    ) {
        requireTeam(workspaceId);
        WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
        if (!TeamRules.canManageMembers(actor.getRole())) {
            throw new SecurityException("Bạn không có quyền thêm thành viên");
        }
        if (!TeamRole.ADMIN.equals(role) && !TeamRole.MEMBER.equals(role)) {
            throw new IllegalArgumentException("Vai trò thành viên không hợp lệ");
        }
        if (TeamRole.ADMIN.equals(role) && !TeamRole.OWNER.equals(actor.getRole())) {
            throw new SecurityException("Chỉ chủ nhóm được thêm quản trị viên");
        }

        WorkspaceMember existing = teamDao.findMember(workspaceId, user.getUserId());
        if (existing != null && MembershipStatus.ACTIVE.equals(existing.getStatus())) {
            throw new IllegalStateException("Thành viên đã có trong nhóm");
        }

        long now = System.currentTimeMillis();
        if (teamDao.findPendingInvite(workspaceId, user.getUserId()) != null) {
            throw new IllegalStateException("Đã có lời mời đang chờ phản hồi");
        }

        TeamInvite invite = new TeamInvite();
        invite.setInviteId(UUID.randomUUID().toString());
        invite.setWorkspaceId(workspaceId);
        invite.setEmail(user.getEmail());
        invite.setInvitedUserId(user.getUserId());
        invite.setInvitedUserCode(user.getUserCode());
        invite.setInvitedDisplayName(user.getDisplayName());
        invite.setWorkspaceName(requireTeam(workspaceId).getName());
        invite.setRole(role);
        invite.setStatus(InviteStatus.PENDING);
        invite.setInvitedBy(actorId);
        invite.setCreatedAt(now);
        invite.setRespondedAt(0);
        invite.setExpiresAt(now + 7L * 24 * 60 * 60 * 1000);

        if (!userDao.saveAuthenticatedUser(user)) {
            throw new IllegalStateException("Không thể lưu tài khoản thành viên");
        }

        if (!teamDao.insertInvite(invite)) {
            throw new IllegalStateException("Không thể tạo lời mời");
        }
        return invite;
    }

    public void getPendingInvites(
            String userId,
            RepositoryCallback<List<TeamInvite>> callback
    ) {
        execute(() -> teamDao.findPendingInvitesForUser(
                requireText(userId, "Thiếu người dùng hiện tại"),
                System.currentTimeMillis()), callback);
    }

    public void respondToInvite(
            String inviteId,
            String userId,
            boolean accept,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            TeamInvite invite = teamDao.findInvite(requireText(inviteId, "Thiếu lời mời"));
            if (invite == null || !userId.equals(invite.getInvitedUserId())) {
                throw new SecurityException("Lời mời không hợp lệ");
            }
            if (!InviteStatus.PENDING.equals(invite.getStatus())) {
                throw new IllegalStateException("Lời mời đã được phản hồi");
            }
            if (!TeamFeatureRules.canRespondToInvite(invite, userId, System.currentTimeMillis())) {
                teamDao.updateInviteStatus(inviteId, InviteStatus.EXPIRED, System.currentTimeMillis());
                throw new IllegalStateException("Lời mời đã hết hạn");
            }
            User user = userDao.findById(userId);
            if (user == null) throw new IllegalStateException("Không tìm thấy tài khoản");
            return new InviteResponse(invite, user);
        }, new RepositoryCallback<InviteResponse>() {
            @Override public void onSuccess(InviteResponse response) {
                cloudSync.respondToInvite(response.invite, response.user, accept,
                        new RepositoryCallback<Boolean>() {
                            @Override public void onSuccess(Boolean result) {
                                executors.database().execute(() -> {
                                    teamDao.updateInviteStatus(inviteId,
                                            accept ? InviteStatus.ACCEPTED : InviteStatus.REJECTED,
                                            System.currentTimeMillis());
                                    executors.mainThread().execute(() -> callback.onSuccess(true));
                                });
                            }
                            @Override public void onError(Exception exception) {
                                callback.onError(exception);
                            }
                        });
            }
            @Override public void onError(Exception exception) { callback.onError(exception); }
        });
    }

    private record InviteResponse(TeamInvite invite, User user) { }

    public void changeMemberRole(
            String workspaceId,
            String actorId,
            String memberId,
            String role,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            WorkspaceMember target = requireActiveMember(workspaceId, memberId);
            if (!TeamRole.OWNER.equals(actor.getRole())) {
                throw new SecurityException("Chỉ chủ nhóm được đổi vai trò");
            }
            if (TeamRole.OWNER.equals(target.getRole())) {
                throw new SecurityException("Không thể thay đổi vai trò chủ nhóm");
            }
            if (!TeamRole.ADMIN.equals(role) && !TeamRole.MEMBER.equals(role)) {
                throw new IllegalArgumentException("Vai trò không hợp lệ");
            }
            if (teamDao.updateMemberRole(workspaceId, memberId, role) <= 0) {
                throw new IllegalStateException("Không thể cập nhật vai trò");
            }
            WorkspaceMember updatedMember = findMemberDetails(workspaceId, memberId);
            if (updatedMember != null) {
                cloudSync.upsertMember(updatedMember);
            }
            return true;
        }, callback);
    }

    public void removeMember(
            String workspaceId,
            String actorId,
            String memberId,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            WorkspaceMember target = requireActiveMember(workspaceId, memberId);
            if (!TeamRules.canManageMembers(actor.getRole())) {
                throw new SecurityException("Bạn không có quyền xóa thành viên");
            }
            if (TeamRole.OWNER.equals(target.getRole())) {
                throw new SecurityException("Không thể xóa chủ nhóm");
            }
            if (TeamRole.ADMIN.equals(actor.getRole()) &&
                    TeamRole.ADMIN.equals(target.getRole())) {
                throw new SecurityException("Quản trị viên không thể xóa quản trị viên khác");
            }
            List<TeamTaskItem> affectedTasks = teamDao.queryTeamTasks(
                    workspaceId, null, memberId, null);
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            try {
                teamDao.clearMemberAssignments(workspaceId, memberId);
                if (teamDao.removeMember(workspaceId, memberId) <= 0) {
                    throw new IllegalStateException("Không thể xóa thành viên");
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            cloudSync.removeMember(workspaceId, memberId);
            for (TeamTaskItem item : affectedTasks) {
                cloudSync.upsertTask(item.getTask(), null);
            }
            return true;
        }, callback);
    }

    public void leaveTeam(
            String workspaceId,
            String userId,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            requireTeam(workspaceId);
            WorkspaceMember member = requireActiveMember(workspaceId, userId);
            if (TeamRole.OWNER.equals(member.getRole())) {
                throw new IllegalStateException("Hãy chuyển quyền chủ nhóm trước khi rời nhóm");
            }
            teamDao.clearMemberAssignments(workspaceId, userId);
            if (teamDao.removeMember(workspaceId, userId) <= 0) {
                throw new IllegalStateException("Không thể rời nhóm");
            }
            cloudSync.removeMember(workspaceId, userId);
            return true;
        }, callback);
    }

    public void transferOwnership(
            String workspaceId,
            String ownerId,
            String newOwnerId,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            Workspace workspace = requireTeam(workspaceId);
            WorkspaceMember owner = requireActiveMember(workspaceId, ownerId);
            WorkspaceMember target = requireActiveMember(workspaceId, newOwnerId);
            if (!TeamRole.OWNER.equals(owner.getRole())) {
                throw new SecurityException("Chỉ chủ nhóm được chuyển quyền");
            }
            if (ownerId.equals(newOwnerId)) {
                throw new IllegalArgumentException("Người này đã là chủ nhóm");
            }
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            try {
                teamDao.updateMemberRole(workspaceId, ownerId, TeamRole.ADMIN);
                teamDao.updateMemberRole(workspaceId, newOwnerId, TeamRole.OWNER);
                workspace.setManagerId(newOwnerId);
                workspace.setUpdatedAt(System.currentTimeMillis());
                if (workspaceDao.update(workspace) <= 0) {
                    throw new IllegalStateException("Không thể chuyển quyền chủ nhóm");
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            owner.setRole(TeamRole.ADMIN);
            target.setRole(TeamRole.OWNER);
            cloudSync.upsertWorkspace(workspace, newOwnerId, TeamRole.OWNER);
            cloudSync.upsertMember(owner);
            return true;
        }, callback);
    }

    public void createProject(
            String workspaceId,
            String actorId,
            String name,
            String description,
            RepositoryCallback<Project> callback
    ) {
        saveProject(null, workspaceId, actorId, name, description,
                0, 0, actorId, ProjectStatus.ACTIVE, callback);
    }

    public void saveProject(
            String projectId,
            String workspaceId,
            String actorId,
            String name,
            String description,
            long startDate,
            long dueDate,
            String managerId,
            String status,
            RepositoryCallback<Project> callback
    ) {
        execute(() -> {
            requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            if (!TeamRules.canManageProjects(actor.getRole())) {
                throw new SecurityException("Bạn không có quyền quản lý dự án");
            }
            if (!TeamFeatureRules.isValidSchedule(startDate, dueDate)) {
                throw new IllegalArgumentException("Hạn dự án phải sau thời gian bắt đầu");
            }
            String cleanManagerId = isBlank(managerId) ? actorId : managerId;
            requireActiveMember(workspaceId, cleanManagerId);
            long now = System.currentTimeMillis();
            Project project = isBlank(projectId)
                    ? new Project() : teamDao.findProjectById(projectId);
            boolean editing = project != null && !isBlank(project.getProjectId());
            if (project == null) throw new IllegalStateException("Dự án không tồn tại");
            if (!editing) {
                project.setProjectId(UUID.randomUUID().toString());
                project.setCreatedBy(actorId);
                project.setCreatedAt(now);
                project.setVersion(1);
            } else if (!workspaceId.equals(project.getWorkspaceId())) {
                throw new SecurityException("Dự án không thuộc nhóm này");
            }
            project.setWorkspaceId(workspaceId);
            project.setName(requireText(name, "Tên dự án không được để trống"));
            project.setDescription(cleanText(description));
            project.setStatus(isBlank(status) ? ProjectStatus.ACTIVE : status);
            project.setManagerId(cleanManagerId);
            project.setStartDate(startDate);
            project.setDueDate(dueDate);
            project.setCompletedAt(ProjectStatus.COMPLETED.equals(project.getStatus()) ? now : 0);
            project.setDeletedAt(0);
            project.setVersion(Math.max(1, project.getVersion() + (editing ? 1 : 0)));
            project.setSyncStatus(SyncStatus.PENDING);
            project.setUpdatedAt(now);
            if (!(editing ? teamDao.updateProject(project) > 0 : teamDao.insertProject(project))) {
                throw new IllegalStateException("Không thể tạo dự án; tên có thể đã tồn tại");
            }
            cloudSync.upsertProject(project);
            return project;
        }, callback);
    }

    public void archiveProject(
            String projectId,
            String workspaceId,
            String actorId,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            if (!TeamRules.canManageProjects(actor.getRole())) {
                throw new SecurityException("Bạn không có quyền lưu trữ dự án");
            }
            Project project = teamDao.findProjectById(projectId);
            if (project == null || !workspaceId.equals(project.getWorkspaceId())) {
                throw new IllegalStateException("Dự án không tồn tại");
            }
            project.setStatus(ProjectStatus.ARCHIVED);
            project.setUpdatedAt(System.currentTimeMillis());
            project.setVersion(Math.max(1, project.getVersion() + 1));
            project.setSyncStatus(SyncStatus.PENDING);
            if (teamDao.updateProject(project) <= 0) {
                throw new IllegalStateException("Không thể lưu trữ dự án");
            }
            cloudSync.upsertProject(project);
            return true;
        }, callback);
    }

    public void completeProject(
            String projectId, String workspaceId, String actorId,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            if (!TeamRules.canManageProjects(actor.getRole())) {
                throw new SecurityException("Bạn không có quyền hoàn thành dự án");
            }
            Project project = teamDao.findProjectById(projectId);
            if (project == null) throw new IllegalStateException("Dự án không tồn tại");
            project.setStatus(ProjectStatus.COMPLETED);
            project.setCompletedAt(System.currentTimeMillis());
            project.setUpdatedAt(System.currentTimeMillis());
            project.setVersion(Math.max(1, project.getVersion() + 1));
            project.setSyncStatus(SyncStatus.PENDING);
            teamDao.updateProject(project);
            cloudSync.upsertProject(project);
            return true;
        }, callback);
    }

    public void getMilestones(
            String projectId, String workspaceId, String actorId,
            RepositoryCallback<List<ProjectMilestone>> callback
    ) {
        execute(() -> {
            requireActiveMember(workspaceId, actorId);
            Project project = teamDao.findProjectById(projectId);
            if (project == null || !workspaceId.equals(project.getWorkspaceId())) {
                throw new IllegalStateException("Dự án không tồn tại");
            }
            return teamDao.findMilestones(projectId);
        }, callback);
    }

    public void addMilestone(
            String projectId, String workspaceId, String actorId,
            String title, long dueDate,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            if (!TeamRules.canManageProjects(actor.getRole())) {
                throw new SecurityException("Bạn không có quyền thêm mốc dự án");
            }
            Project project = teamDao.findProjectById(projectId);
            if (project == null) throw new IllegalStateException("Dự án không tồn tại");
            ProjectMilestone item = new ProjectMilestone();
            item.setMilestoneId(UUID.randomUUID().toString());
            item.setProjectId(projectId);
            item.setWorkspaceId(workspaceId);
            item.setTitle(requireText(title, "Tên mốc không được để trống"));
            item.setDueDate(dueDate);
            item.setCreatedBy(actorId);
            item.setCreatedAt(System.currentTimeMillis());
            teamDao.saveMilestone(item);
            cloudSync.upsertMilestone(item);
            return true;
        }, callback);
    }

    public void toggleMilestone(
            ProjectMilestone item, String actorId, boolean completed,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            WorkspaceMember actor = requireActiveMember(item.getWorkspaceId(), actorId);
            if (!TeamRules.canManageProjects(actor.getRole())) {
                throw new SecurityException("Bạn không có quyền cập nhật mốc dự án");
            }
            item.setCompletedAt(completed ? System.currentTimeMillis() : 0);
            teamDao.saveMilestone(item);
            cloudSync.upsertMilestone(item);
            return true;
        }, callback);
    }

    public void saveTeamTask(
            Task task,
            List<String> assigneeIds,
            String actorId,
            RepositoryCallback<Task> callback
    ) {
        execute(() -> {
            if (task == null) {
                throw new IllegalArgumentException("Thiếu dữ liệu công việc");
            }
            Workspace workspace = requireTeam(task.getWorkspaceId());
            WorkspaceMember actor = requireActiveMember(workspace.getWorkspaceId(), actorId);
            if (assigneeIds == null || assigneeIds.isEmpty()) {
                throw new IllegalArgumentException("Hãy chọn ít nhất một người thực hiện");
            }
            List<WorkspaceMember> assignees = new ArrayList<>();
            for (String assigneeId : new LinkedHashSet<>(assigneeIds)) {
                assignees.add(requireActiveMember(workspace.getWorkspaceId(), assigneeId));
            }
            Project project = teamDao.findProjectById(task.getProjectId());
            if (project == null || !workspace.getWorkspaceId().equals(project.getWorkspaceId()) ||
                    !ProjectStatus.ACTIVE.equals(project.getStatus())) {
                throw new IllegalArgumentException("Dự án không hợp lệ");
            }

            boolean editing = task.getTaskId() != null && !task.getTaskId().isBlank();
            TeamTaskItem existingItem = editing
                    ? teamDao.findTeamTaskById(task.getTaskId())
                    : null;
            long now = System.currentTimeMillis();
            if (editing) {
                if (existingItem == null || !workspace.getWorkspaceId().equals(
                        existingItem.getTask().getWorkspaceId())) {
                    throw new IllegalStateException("Công việc không tồn tại trong nhóm");
                }
                if (!TeamRules.canEditTask(
                        actor.getRole(),
                        actorId,
                        existingItem.getTask(),
                        existingItem.getAssigneeIds()
                )) {
                    throw new SecurityException("Bạn không có quyền sửa công việc này");
                }
                task.setCreatedBy(existingItem.getTask().getCreatedBy());
                task.setCreatedAt(existingItem.getTask().getCreatedAt());
                task.setVersion(Math.max(1, existingItem.getTask().getVersion() + 1));
            } else {
                if (!TeamRules.canCreateTask(actor.getRole())) {
                    throw new SecurityException("Bạn không có quyền tạo công việc");
                }
                task.setTaskId(UUID.randomUUID().toString());
                task.setCreatedBy(actorId);
                task.setCreatedAt(now);
                if (task.getStartDate() <= 0) task.setStartDate(now);
                task.setVersion(1);
            }

            prepareTask(task, now);
            SQLiteDatabase database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            try {
                boolean saved = editing ? taskDao.update(task) > 0 : taskDao.insert(task);
                if (!saved) {
                    throw new IllegalStateException("Không thể lưu công việc nhóm");
                }
                teamDao.clearTaskAssignees(task.getTaskId());
                for (WorkspaceMember assignee : assignees) {
                    if (!teamDao.insertTaskAssignee(
                            task.getTaskId(),
                            assignee.getUserId(),
                            actorId,
                            now
                    )) {
                        throw new IllegalStateException("Không thể giao công việc");
                    }
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            cloudSync.upsertTask(task, assignees.get(0).getUserId());
            TaskHistory history = historyDao.add(task.getTaskId(), actorId,
                    editing ? TaskHistoryAction.UPDATED : TaskHistoryAction.CREATED,
                    editing ? "Đã cập nhật công việc nhóm" : "Đã tạo và giao công việc nhóm");
            cloudSync.upsertTaskHistory(history);
            reminderScheduler.schedule(task);
            for (WorkspaceMember assignee : assignees) {
                if (!actorId.equals(assignee.getUserId())) {
                    notificationDao.add(assignee.getUserId(), task.getWorkspaceId(),
                            task.getTaskId(), "TASK_ASSIGNED", "Công việc mới được giao",
                            "Bạn được giao: " + task.getTitle());
                }
            }
            return task;
        }, callback);
    }

    public void deleteTeamTask(
            String taskId,
            String actorId,
            RepositoryCallback<Boolean> callback
    ) {
        execute(() -> {
            TeamTaskItem item = teamDao.findTeamTaskById(taskId);
            if (item == null) {
                throw new IllegalStateException("Không tìm thấy công việc");
            }
            requireTeam(item.getTask().getWorkspaceId());
            WorkspaceMember actor = requireActiveMember(
                    item.getTask().getWorkspaceId(), actorId);
            if (!TeamRules.canDeleteTask(actor.getRole(), actorId, item.getTask())) {
                throw new SecurityException("Bạn không có quyền xóa công việc này");
            }
            long now = System.currentTimeMillis();
            Task task = item.getTask();
            task.setDeletedAt(now);
            task.setUpdatedAt(now);
            task.setVersion(Math.max(1, task.getVersion() + 1));
            task.setSyncStatus(SyncStatus.PENDING);
            if (taskDao.softDelete(taskId, now, task.getVersion()) <= 0) {
                throw new IllegalStateException("Không thể xóa công việc");
            }
            cloudSync.upsertTask(task, item.getAssigneeId());
            TaskHistory history = historyDao.add(taskId, actorId,
                    TaskHistoryAction.DELETED, "Đã đưa công việc nhóm vào thùng rác");
            cloudSync.upsertTaskHistory(history);
            reminderScheduler.cancel(taskId);
            return true;
        }, callback);
    }

    private void prepareTask(Task task, long now) {
        task.setTitle(requireText(task.getTitle(), "Tên công việc không được để trống"));
        task.setDescription(cleanText(task.getDescription()));
        requireText(task.getWorkspaceId(), "Thiếu nhóm");
        requireText(task.getProjectId(), "Vui lòng chọn dự án");
        if (!TaskRules.isValidStatus(task.getStatus())) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
        if (!TaskRules.isValidPriority(task.getPriority())) {
            throw new IllegalArgumentException("Độ ưu tiên không hợp lệ");
        }
        if (task.getEstimatedMinutes() < 0) {
            throw new IllegalArgumentException("Thời gian dự kiến không hợp lệ");
        }
        if (!TeamFeatureRules.isValidSchedule(task.getStartDate(), task.getDueDate())) {
            throw new IllegalArgumentException("Hạn hoàn thành phải sau thời gian bắt đầu");
        }
        task.setProgress(TaskRules.normalizeProgress(task.getStatus(), task.getProgress()));
        task.setCompletedAt(com.vandieu_manhdung.taskmanager.core.constant.TaskStatus.COMPLETED
                .equals(task.getStatus()) ? (task.getCompletedAt() > 0 ? task.getCompletedAt() : now) : 0);
        task.setDeletedAt(0);
        task.setVersion(Math.max(1, task.getVersion()));
        task.setSyncStatus(SyncStatus.PENDING);
        task.setUpdatedAt(now);
    }

    private Workspace requireTeam(String workspaceId) {
        String cleanId = requireText(workspaceId, "Thiếu mã nhóm");
        Workspace workspace = workspaceDao.findById(cleanId);
        if (workspace == null || !WorkspaceType.TEAM.equals(workspace.getType()) ||
                !WorkspaceStatus.ACTIVE.equals(workspace.getStatus())) {
            throw new IllegalStateException("Nhóm không tồn tại hoặc đã ngừng hoạt động");
        }
        return workspace;
    }

    private WorkspaceMember requireActiveMember(String workspaceId, String userId) {
        String cleanUserId = requireText(userId, "Thiếu người dùng");
        WorkspaceMember member = teamDao.findMember(workspaceId, cleanUserId);
        if (member == null || !MembershipStatus.ACTIVE.equals(member.getStatus())) {
            throw new SecurityException("Bạn không phải thành viên đang hoạt động của nhóm");
        }
        return member;
    }

    private WorkspaceMember findMemberDetails(String workspaceId, String userId) {
        for (WorkspaceMember member : teamDao.findMembers(workspaceId)) {
            if (userId.equals(member.getUserId())) {
                return member;
            }
        }
        return null;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String cleanText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private <T> void execute(
            Callable<T> operation,
            RepositoryCallback<T> callback
    ) {
        executors.database().execute(() -> {
            try {
                T result = operation.call();
                executors.mainThread().execute(() -> callback.onSuccess(result));
            } catch (Exception exception) {
                executors.mainThread().execute(() -> callback.onError(exception));
            }
        });
    }
}
