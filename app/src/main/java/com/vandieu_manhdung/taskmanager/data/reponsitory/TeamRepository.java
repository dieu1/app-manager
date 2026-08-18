package com.vandieu_manhdung.taskmanager.data.reponsitory;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.InviteStatus;
import com.vandieu_manhdung.taskmanager.core.constant.MembershipStatus;
import com.vandieu_manhdung.taskmanager.core.constant.ProjectStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TeamRole;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceStatus;
import com.vandieu_manhdung.taskmanager.core.constant.WorkspaceType;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.core.util.TeamRules;
import com.vandieu_manhdung.taskmanager.core.util.UserCodeRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.UserDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkspaceDao;
import com.vandieu_manhdung.taskmanager.data.local.database.TaskManagerDatabaseHelper;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.List;
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

    public TeamRepository(Context context) {
        Context appContext = context.getApplicationContext();
        databaseHelper = TaskManagerDatabaseHelper.getInstance(appContext);
        workspaceDao = new WorkspaceDao(appContext);
        userDao = new UserDao(appContext);
        taskDao = new TaskDao(appContext);
        teamDao = new TeamDao(appContext);
        executors = AppExecutors.getInstance();
        cloudSync = CloudSyncManager.getInstance(appContext);
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
            if (workspaceDao.delete(workspaceId) <= 0) {
                throw new IllegalStateException("Không thể giải tán nhóm");
            }
            cloudSync.archiveWorkspace(workspaceId);
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

    public void addMember(
            String workspaceId,
            String actorId,
            String userCode,
            String role,
            RepositoryCallback<WorkspaceMember> callback
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
                        execute(() -> addRegisteredMember(
                                workspaceId,
                                actorId,
                                user,
                                role
                        ), callback);
                    }

                    @Override
                    public void onError(Exception exception) {
                        callback.onError(exception);
                    }
                }
        );
    }

    private WorkspaceMember addRegisteredMember(
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
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspaceId);
        member.setUserId(user.getUserId());
        member.setUserCode(user.getUserCode());
        member.setRole(role);
        member.setStatus(MembershipStatus.ACTIVE);
        member.setJoinedAt(now);
        member.setDisplayName(user.getDisplayName());
        member.setEmail(user.getEmail());

        TeamInvite invite = new TeamInvite();
        invite.setInviteId(UUID.randomUUID().toString());
        invite.setWorkspaceId(workspaceId);
        invite.setEmail(user.getEmail());
        invite.setRole(role);
        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setInvitedBy(actorId);
        invite.setCreatedAt(now);
        invite.setRespondedAt(now);

        if (!userDao.saveAuthenticatedUser(user)) {
            throw new IllegalStateException("Không thể lưu tài khoản thành viên");
        }

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            if (!teamDao.insertInvite(invite) || !teamDao.insertMember(member)) {
                throw new IllegalStateException("Không thể thêm thành viên");
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        cloudSync.upsertMember(member);
        return member;
    }

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

    public void createProject(
            String workspaceId,
            String actorId,
            String name,
            String description,
            RepositoryCallback<Project> callback
    ) {
        execute(() -> {
            requireTeam(workspaceId);
            WorkspaceMember actor = requireActiveMember(workspaceId, actorId);
            if (!TeamRules.canManageProjects(actor.getRole())) {
                throw new SecurityException("Bạn không có quyền tạo dự án");
            }
            long now = System.currentTimeMillis();
            Project project = new Project();
            project.setProjectId(UUID.randomUUID().toString());
            project.setWorkspaceId(workspaceId);
            project.setName(requireText(name, "Tên dự án không được để trống"));
            project.setDescription(cleanText(description));
            project.setStatus(ProjectStatus.ACTIVE);
            project.setCreatedBy(actorId);
            project.setCreatedAt(now);
            project.setUpdatedAt(now);
            if (!teamDao.insertProject(project)) {
                throw new IllegalStateException("Không thể tạo dự án; tên có thể đã tồn tại");
            }
            cloudSync.upsertProject(project);
            return project;
        }, callback);
    }

    public void saveTeamTask(
            Task task,
            String assigneeId,
            String actorId,
            RepositoryCallback<Task> callback
    ) {
        execute(() -> {
            if (task == null) {
                throw new IllegalArgumentException("Thiếu dữ liệu công việc");
            }
            Workspace workspace = requireTeam(task.getWorkspaceId());
            WorkspaceMember actor = requireActiveMember(workspace.getWorkspaceId(), actorId);
            WorkspaceMember assignee = requireActiveMember(workspace.getWorkspaceId(), assigneeId);
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
                        existingItem.getAssigneeId()
                )) {
                    throw new SecurityException("Bạn không có quyền sửa công việc này");
                }
                task.setCreatedBy(existingItem.getTask().getCreatedBy());
                task.setCreatedAt(existingItem.getTask().getCreatedAt());
                task.setStartDate(existingItem.getTask().getStartDate());
            } else {
                if (!TeamRules.canCreateTask(actor.getRole())) {
                    throw new SecurityException("Bạn không có quyền tạo công việc");
                }
                task.setTaskId(UUID.randomUUID().toString());
                task.setCreatedBy(actorId);
                task.setCreatedAt(now);
                task.setStartDate(now);
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
                if (!teamDao.insertTaskAssignee(
                        task.getTaskId(),
                        assignee.getUserId(),
                        actorId,
                        now
                )) {
                    throw new IllegalStateException("Không thể giao công việc");
                }
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            cloudSync.upsertTask(task, assignee.getUserId());
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
            if (taskDao.delete(taskId) <= 0) {
                throw new IllegalStateException("Không thể xóa công việc");
            }
            cloudSync.deleteTask(taskId);
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
        task.setProgress(TaskRules.normalizeProgress(task.getStatus(), task.getProgress()));
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
