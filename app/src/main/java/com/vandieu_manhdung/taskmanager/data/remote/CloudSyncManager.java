package com.vandieu_manhdung.taskmanager.data.remote;

import com.vandieu_manhdung.taskmanager.BuildConfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.android.gms.tasks.Tasks;
import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.MembershipStatus;
import com.vandieu_manhdung.taskmanager.core.constant.InviteStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TeamRole;
import com.vandieu_manhdung.taskmanager.core.constant.SyncStatus;
import com.vandieu_manhdung.taskmanager.core.notification.TaskReminderScheduler;
import com.vandieu_manhdung.taskmanager.core.notification.TeamMessagingService;
import com.vandieu_manhdung.taskmanager.core.sync.CloudSyncWorker;
import com.vandieu_manhdung.taskmanager.core.sync.SyncBus;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.TaskSubtaskRules;
import com.vandieu_manhdung.taskmanager.core.util.SyncConflictRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.SyncQueueDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.NotificationDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskSubtaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskHistoryDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamCollaborationDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.UserDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkspaceDao;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.ProjectMilestone;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;
import com.vandieu_manhdung.taskmanager.model.TaskHistory;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;
import com.vandieu_manhdung.taskmanager.model.TaskComment;
import com.vandieu_manhdung.taskmanager.model.TaskAttachment;
import com.vandieu_manhdung.taskmanager.model.TaskDependency;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;
import com.vandieu_manhdung.taskmanager.model.SyncQueueItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class CloudSyncManager {

    private static final String COLLECTION_USER_CODES = "user_codes";
    private static final String COLLECTION_WORKSPACES = "workspaces";
    private static final String COLLECTION_MEMBERS = "workspace_members";
    private static final String COLLECTION_INVITES = "team_invites";
    private static final String COLLECTION_PROJECTS = "projects";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_SUBTASKS = "task_subtasks";
    private static final String COLLECTION_HISTORIES = "task_histories";
    private static final String COLLECTION_COMMENTS = "task_comments";
    private static final String COLLECTION_ATTACHMENTS = "task_attachments";
    private static final String COLLECTION_DEPENDENCIES = "task_dependencies";
    private static final String COLLECTION_MILESTONES = "project_milestones";
    private static final String PREFS = "cloud_sync_state";

    private static volatile CloudSyncManager instance;

    private final Context context;
    private final AppExecutors executors;
    private final WorkspaceDao workspaceDao;
    private final UserDao userDao;
    private final TaskDao taskDao;
    private final TaskSubtaskDao subtaskDao;
    private final SyncQueueDao syncQueueDao;
    private final TaskHistoryDao historyDao;
    private final TeamDao teamDao;
    private final TeamCollaborationDao collaborationDao;
    private final TaskReminderScheduler reminderScheduler;
    private final SharedPreferences preferences;
    private final List<ListenerRegistration> rootListeners = new ArrayList<>();
    private final Map<String, List<ListenerRegistration>> workspaceListeners =
            new ConcurrentHashMap<>();
    private final Map<String, PendingTask> pendingTasks = new ConcurrentHashMap<>();
    private final Map<String, List<TaskSubtask>> pendingSubtasksByTask =
            new ConcurrentHashMap<>();
    private final Map<String, List<TaskHistory>> pendingHistoriesByTask =
            new ConcurrentHashMap<>();

    private FirebaseFirestore firestore;
    private String activeUserId;

    private CloudSyncManager(Context context) {
        this.context = context.getApplicationContext();
        executors = AppExecutors.getInstance();
        workspaceDao = new WorkspaceDao(this.context);
        userDao = new UserDao(this.context);
        taskDao = new TaskDao(this.context);
        subtaskDao = new TaskSubtaskDao(this.context);
        syncQueueDao = new SyncQueueDao(this.context);
        historyDao = new TaskHistoryDao(this.context);
        teamDao = new TeamDao(this.context);
        collaborationDao = new TeamCollaborationDao(this.context);
        reminderScheduler = new TaskReminderScheduler(this.context);
        preferences = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static CloudSyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CloudSyncManager.class) {
                if (instance == null) {
                    instance = new CloudSyncManager(context);
                }
            }
        }
        return instance;
    }

    public boolean isAvailable() {
        return FirebaseProvider.isConfigured(context);
    }

    public void start(String userId, RepositoryCallback<Boolean> callback) {
        if (!isAvailable()) {
            callback.onError(new IllegalStateException("Firebase chưa được cấu hình"));
            return;
        }
        stop();
        firestore = FirebaseProvider.firestore(context);
        activeUserId = userId;
        executors.database().execute(() -> {
            try {
                bootstrapLocalDataOnce(userId);
                executors.mainThread().execute(() -> {
                    attachMembershipListener(userId);
                    attachInviteListener(userId);
                    TeamMessagingService.registerCurrentToken(context);
                    CloudSyncWorker.schedule(context);
                    callback.onSuccess(true);
                });
            } catch (Exception exception) {
                executors.mainThread().execute(() -> callback.onError(exception));
            }
        });
    }

    public void stop() {
        for (ListenerRegistration listener : rootListeners) {
            listener.remove();
        }
        rootListeners.clear();
        for (List<ListenerRegistration> listeners : workspaceListeners.values()) {
            for (ListenerRegistration listener : listeners) {
                listener.remove();
            }
        }
        workspaceListeners.clear();
        pendingTasks.clear();
        pendingSubtasksByTask.clear();
        pendingHistoriesByTask.clear();
        activeUserId = null;
    }

    public void upsertWorkspace(Workspace workspace, String userId, String role) {
        if (workspace == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_WORKSPACE, workspace.getWorkspaceId(), 1);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(workspace.getWorkspaceId());
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(MembershipStatus.ACTIVE);
        member.setJoinedAt(workspace.getCreatedAt());
        User user = userDao.findById(userId);
        if (user != null) {
            member.setUserCode(user.getUserCode());
            member.setDisplayName(user.getDisplayName());
            member.setEmail(user.getEmail());
        }
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_MEMBER,
                member.getWorkspaceId() + "|" + member.getUserId(), 1);
        CloudSyncWorker.schedule(context);
        if (!ready()) return;
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        batch.set(
                firestore.collection(COLLECTION_WORKSPACES)
                        .document(workspace.getWorkspaceId()),
                workspaceMap(workspace),
                SetOptions.merge()
        );
        batch.set(
                firestore.collection(COLLECTION_MEMBERS)
                        .document(membershipId(member.getWorkspaceId(), member.getUserId())),
                memberMap(member),
                SetOptions.merge()
        );
        batch.commit().addOnFailureListener(this::logSyncFailure);
    }

    public void archiveWorkspace(String workspaceId) {
        if (!ready()) return;
        Map<String, Object> values = new HashMap<>();
        values.put("status", "ARCHIVED");
        values.put("updatedAt", System.currentTimeMillis());
        firestore.collection(COLLECTION_WORKSPACES)
                .document(workspaceId)
                .set(values, SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
    }

    public void upsertMember(WorkspaceMember member) {
        if (member == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_MEMBER,
                member.getWorkspaceId() + "|" + member.getUserId(), 1);
        CloudSyncWorker.schedule(context);
        if (!ready()) return;
        firestore.collection(COLLECTION_MEMBERS)
                .document(membershipId(member.getWorkspaceId(), member.getUserId()))
                .set(memberMap(member), SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
    }

    public void upsertInvite(TeamInvite invite, RepositoryCallback<Boolean> callback) {
        if (!ready() || invite == null) {
            callback.onError(new IllegalStateException("Firebase chưa sẵn sàng"));
            return;
        }
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_INVITE, invite.getInviteId(), 1);
        CloudSyncWorker.schedule(context);
        firestore.collection(COLLECTION_INVITES).document(invite.getInviteId())
                .set(inviteMap(invite), SetOptions.merge())
                .addOnSuccessListener(ignored -> callback.onSuccess(true))
                .addOnFailureListener(error -> callback.onError(
                        new IllegalStateException("Không thể gửi lời mời", error)));
    }

    public void respondToInvite(
            TeamInvite invite,
            User user,
            boolean accept,
            RepositoryCallback<Boolean> callback
    ) {
        if (!ready() || invite == null || user == null) {
            callback.onError(new IllegalStateException("Firebase chưa sẵn sàng"));
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Object> inviteValues = new HashMap<>();
        inviteValues.put("status", accept ? InviteStatus.ACCEPTED : InviteStatus.REJECTED);
        inviteValues.put("respondedAt", now);
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        batch.set(firestore.collection(COLLECTION_INVITES).document(invite.getInviteId()),
                inviteValues, SetOptions.merge());
        if (accept) {
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspaceId(invite.getWorkspaceId());
            member.setUserId(user.getUserId());
            member.setUserCode(user.getUserCode());
            member.setDisplayName(user.getDisplayName());
            member.setEmail(user.getEmail());
            member.setRole(invite.getRole());
            member.setStatus(MembershipStatus.ACTIVE);
            member.setJoinedAt(now);
            member.setInviteId(invite.getInviteId());
            batch.set(firestore.collection(COLLECTION_MEMBERS)
                            .document(membershipId(member.getWorkspaceId(), member.getUserId())),
                    memberMap(member), SetOptions.merge());
        }
        batch.commit()
                .addOnSuccessListener(ignored -> callback.onSuccess(true))
                .addOnFailureListener(error -> callback.onError(
                        new IllegalStateException("Không thể phản hồi lời mời", error)));
    }

    private Map<String, Object> memberMap(WorkspaceMember member) {
        if (member.getUserCode() == null || member.getUserCode().isBlank()) {
            User localUser = userDao.findById(member.getUserId());
            if (localUser != null) {
                member.setUserCode(localUser.getUserCode());
                if (member.getDisplayName() == null) {
                    member.setDisplayName(localUser.getDisplayName());
                }
                if (member.getEmail() == null) {
                    member.setEmail(localUser.getEmail());
                }
            }
        }
        Map<String, Object> values = new HashMap<>();
        values.put("workspaceId", member.getWorkspaceId());
        values.put("userId", member.getUserId());
        values.put("userCode", member.getUserCode());
        values.put("role", member.getRole());
        values.put("status", member.getStatus());
        values.put("joinedAt", member.getJoinedAt());
        values.put("inviteId", member.getInviteId());
        values.put("displayName", member.getDisplayName());
        values.put("email", member.getEmail());
        values.put("updatedAt", System.currentTimeMillis());
        return values;
    }

    public void removeMember(String workspaceId, String userId) {
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_MEMBER, workspaceId + "|" + userId, 1);
        CloudSyncWorker.schedule(context);
        if (!ready()) return;
        Map<String, Object> values = new HashMap<>();
        values.put("status", "REMOVED");
        values.put("updatedAt", System.currentTimeMillis());
        firestore.collection(COLLECTION_MEMBERS)
                .document(membershipId(workspaceId, userId))
                .set(values, SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
    }

    public void upsertProject(Project project) {
        if (project == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_PROJECT,
                project.getProjectId(), Math.max(1, project.getVersion()));
        CloudSyncWorker.schedule(context);
        if (!ready()) return;
        firestore.collection(COLLECTION_PROJECTS)
                .document(project.getProjectId())
                .set(projectMap(project), SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
    }

    public void upsertMilestone(ProjectMilestone item) {
        if (item == null || item.getMilestoneId() == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_MILESTONE, item.getMilestoneId(), 1);
        CloudSyncWorker.schedule(context);
    }

    public void deleteProject(String projectId) {
        if (ready()) {
            firestore.collection(COLLECTION_PROJECTS).document(projectId).delete()
                    .addOnFailureListener(this::logSyncFailure);
        }
    }

    public void upsertTask(Task task, String assigneeId) {
        if (task == null || task.getTaskId() == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_TASK, task.getTaskId(), task.getVersion());
        CloudSyncWorker.schedule(context);
    }

    public void deleteTask(String taskId) {
        if (ready()) {
            firestore.collection(COLLECTION_TASKS).document(taskId).delete()
                    .addOnFailureListener(this::logSyncFailure);
        }
    }

    public void upsertSubtask(TaskSubtask subtask) {
        if (subtask == null || subtask.getSubtaskId() == null) return;
        syncQueueDao.enqueue(
                SyncQueueDao.ENTITY_SUBTASK, subtask.getSubtaskId(), subtask.getVersion());
        CloudSyncWorker.schedule(context);
    }

    public void deleteSubtask(String subtaskId) {
        if (ready()) {
            firestore.collection(COLLECTION_SUBTASKS)
                    .document(subtaskId)
                    .delete()
                    .addOnFailureListener(this::logSyncFailure);
        }
    }

    public void upsertTaskHistory(TaskHistory history) {
        if (history == null || history.getHistoryId() == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_HISTORY, history.getHistoryId(), 1);
        CloudSyncWorker.schedule(context);
    }

    public void upsertComment(TaskComment comment) {
        if (comment == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_COMMENT, comment.getCommentId(), 1);
        CloudSyncWorker.schedule(context);
    }

    public void upsertAttachment(TaskAttachment attachment) {
        if (attachment == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_ATTACHMENT, attachment.getAttachmentId(), 1);
        CloudSyncWorker.schedule(context);
    }

    public void upsertDependency(TaskDependency dependency) {
        if (dependency == null) return;
        syncQueueDao.enqueue(SyncQueueDao.ENTITY_DEPENDENCY,
                dependency.getTaskId() + "|" + dependency.getDependsOnTaskId(), 1);
        CloudSyncWorker.schedule(context);
    }

    public void deleteDependency(String taskId, String dependsOnTaskId) {
        syncQueueDao.enqueueDelete(SyncQueueDao.ENTITY_DEPENDENCY,
                taskId + "|" + dependsOnTaskId);
        CloudSyncWorker.schedule(context);
    }

    public boolean flushPendingBlocking() {
        if (!ready()) return false;
        boolean allSucceeded = true;
        for (SyncQueueItem item : syncQueueDao.findPending(100)) {
            try {
                if (SyncQueueDao.ENTITY_WORKSPACE.equals(item.getEntityType())) {
                    Workspace workspace = workspaceDao.findById(item.getEntityId());
                    if (workspace == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_WORKSPACES)
                            .document(workspace.getWorkspaceId())
                            .set(workspaceMap(workspace), SetOptions.merge()), 30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_MEMBER.equals(item.getEntityType())) {
                    String[] ids = item.getEntityId().split("\\|", 2);
                    WorkspaceMember member = ids.length == 2 ? teamDao.findMember(ids[0], ids[1]) : null;
                    if (member == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_MEMBERS)
                            .document(membershipId(member.getWorkspaceId(), member.getUserId()))
                            .set(memberMap(member), SetOptions.merge()), 30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_PROJECT.equals(item.getEntityType())) {
                    Project project = teamDao.findProjectById(item.getEntityId());
                    if (project == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_PROJECTS)
                            .document(project.getProjectId())
                            .set(projectMap(project), SetOptions.merge()), 30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_MILESTONE.equals(item.getEntityType())) {
                    ProjectMilestone milestone = teamDao.findMilestone(item.getEntityId());
                    if (milestone == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_MILESTONES)
                            .document(milestone.getMilestoneId())
                            .set(milestoneMap(milestone), SetOptions.merge()),
                            30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_INVITE.equals(item.getEntityType())) {
                    TeamInvite invite = teamDao.findInvite(item.getEntityId());
                    if (invite == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_INVITES)
                            .document(invite.getInviteId())
                            .set(inviteMap(invite), SetOptions.merge()), 30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_COMMENT.equals(item.getEntityType())) {
                    TaskComment comment = collaborationDao.findComment(item.getEntityId());
                    if (comment == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_COMMENTS)
                            .document(comment.getCommentId())
                            .set(commentMap(comment), SetOptions.merge()), 30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_ATTACHMENT.equals(item.getEntityType())) {
                    TaskAttachment attachment = collaborationDao.findAttachment(item.getEntityId());
                    if (attachment == null || attachment.getRemoteUrl() == null ||
                            attachment.getRemoteUrl().isBlank()) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_ATTACHMENTS)
                            .document(attachment.getAttachmentId())
                            .set(attachmentMap(attachment), SetOptions.merge()), 30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_DEPENDENCY.equals(item.getEntityType())) {
                    String[] ids = item.getEntityId().split("\\|", 2);
                    if (ids.length == 2 && SyncQueueDao.DELETE.equals(item.getOperation())) {
                        Tasks.await(firestore.collection(COLLECTION_DEPENDENCIES)
                                        .document(ids[0] + "_" + ids[1]).delete(),
                                30, TimeUnit.SECONDS);
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    TaskDependency dependency = ids.length == 2
                            ? collaborationDao.findDependency(ids[0], ids[1]) : null;
                    Task ownerTask = dependency == null ? null : taskDao.findById(dependency.getTaskId());
                    if (dependency == null || ownerTask == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(firestore.collection(COLLECTION_DEPENDENCIES)
                            .document(dependency.getTaskId() + "_" + dependency.getDependsOnTaskId())
                            .set(dependencyMap(dependency, ownerTask.getWorkspaceId()), SetOptions.merge()),
                            30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else if (SyncQueueDao.ENTITY_TASK.equals(item.getEntityType())) {
                    Task task = taskDao.findByIdIncludingDeleted(item.getEntityId());
                    if (task == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Map<String, Object> values = taskMap(task);
                    List<String> assigneeIds = task.getProjectId() == null ||
                            task.getProjectId().isBlank()
                            ? List.of(task.getCreatedBy())
                            : teamDao.findTaskAssigneeIds(task.getTaskId());
                    String assigneeId = assigneeIds.isEmpty() ? null : assigneeIds.get(0);
                    values.put("assigneeId", assigneeId);
                    values.put("assigneeIds", assigneeIds);
                    Tasks.await(
                            firestore.collection(COLLECTION_TASKS)
                                    .document(task.getTaskId())
                                    .set(values, SetOptions.merge()),
                            30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                    taskDao.markSyncStatus(task.getTaskId(), item.getVersion(), SyncStatus.SYNCED);
                } else if (SyncQueueDao.ENTITY_SUBTASK.equals(item.getEntityType())) {
                    TaskSubtask subtask = subtaskDao.findByIdIncludingDeleted(item.getEntityId());
                    if (subtask == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Tasks.await(
                            firestore.collection(COLLECTION_SUBTASKS)
                                    .document(subtask.getSubtaskId())
                                    .set(subtaskMap(subtask), SetOptions.merge()),
                            30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                    subtaskDao.markSyncStatus(
                            subtask.getSubtaskId(), item.getVersion(), SyncStatus.SYNCED);
                } else if (SyncQueueDao.ENTITY_HISTORY.equals(item.getEntityType())) {
                    TaskHistory history = historyDao.findById(item.getEntityId());
                    if (history == null) {
                        syncQueueDao.remove(item.getQueueId(), item.getVersion());
                        continue;
                    }
                    Task task = taskDao.findByIdIncludingDeleted(history.getTaskId());
                    if (task == null) throw new IllegalStateException("Thiếu công việc của lịch sử");
                    Tasks.await(
                            firestore.collection(COLLECTION_HISTORIES)
                                    .document(history.getHistoryId())
                                    .set(historyMap(history, task.getWorkspaceId())),
                            30, TimeUnit.SECONDS);
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                } else {
                    syncQueueDao.remove(item.getQueueId(), item.getVersion());
                }
            } catch (Exception exception) {
                allSucceeded = false;
                syncQueueDao.markFailed(item.getQueueId(), exception.getMessage());
                if (SyncQueueDao.ENTITY_TASK.equals(item.getEntityType())) {
                    taskDao.markSyncStatus(item.getEntityId(), item.getVersion(), SyncStatus.FAILED);
                } else if (SyncQueueDao.ENTITY_SUBTASK.equals(item.getEntityType())) {
                    subtaskDao.markSyncStatus(item.getEntityId(), item.getVersion(), SyncStatus.FAILED);
                }
                logSyncFailure(exception);
            }
        }
        return allSucceeded;
    }

    public void findRegisteredUserByCode(
            String userCode,
            RepositoryCallback<User> callback
    ) {
        if (!ready()) {
            callback.onError(new IllegalStateException("Dịch vụ đồng bộ chưa sẵn sàng"));
            return;
        }
        String normalizedCode = userCode == null
                ? ""
                : userCode.trim().toUpperCase(Locale.ROOT);
        firestore.collection(COLLECTION_USER_CODES)
                .document(normalizedCode)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onSuccess(null);
                        return;
                    }
                    User user = new User();
                    user.setUserId(string(document, "userId", ""));
                    user.setUserCode(string(document, "userCode", normalizedCode));
                    user.setEmail(string(document, "email", ""));
                    user.setDisplayName(string(document, "displayName", user.getEmail()));
                    user.setCreatedAt(number(document, "updatedAt"));
                    user.setUpdatedAt(number(document, "updatedAt"));
                    callback.onSuccess(user);
                })
                .addOnFailureListener(error -> callback.onError(
                        new IllegalStateException(
                                error.getMessage() == null
                                        ? "Không thể tìm mã người dùng"
                                        : error.getMessage(),
                                error
                        )
                ));
    }

    private void bootstrapLocalDataOnce(String userId) {
        String key = "bootstrapped_" + userId;
        if (preferences.getBoolean(key, false)) {
            return;
        }
        Workspace personal = workspaceDao.findPersonalWorkspace(userId);
        if (personal != null) {
            upsertWorkspace(personal, userId, TeamRole.OWNER);
            for (Task task : taskDao.findAllPersonalTasks(personal.getWorkspaceId())) {
                upsertTask(task, userId);
            }
            for (TaskSubtask subtask : subtaskDao.findAllByWorkspace(personal.getWorkspaceId())) {
                upsertSubtask(subtask);
            }
        }
        for (Workspace team : teamDao.findTeamsForUser(userId)) {
            WorkspaceMember currentMember = teamDao.findMember(team.getWorkspaceId(), userId);
            if (currentMember != null && TeamRole.OWNER.equals(currentMember.getRole())) {
                upsertWorkspace(team, userId, TeamRole.OWNER);
                for (WorkspaceMember member : teamDao.findMembers(team.getWorkspaceId())) {
                    upsertMember(member);
                }
                for (Project project : teamDao.findProjects(team.getWorkspaceId())) {
                    upsertProject(project);
                }
                for (TeamTaskItem item : teamDao.queryTeamTasks(
                        team.getWorkspaceId(), null, null, null)) {
                    upsertTask(item.getTask(), item.getAssigneeId());
                }
            }
        }
        preferences.edit().putBoolean(key, true).apply();
    }

    private void attachMembershipListener(String userId) {
        ListenerRegistration listener = firestore.collection(COLLECTION_MEMBERS)
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", MembershipStatus.ACTIVE)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null ||
                            !userId.equals(activeUserId)) {
                        return;
                    }
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        WorkspaceMember member = memberFrom(change.getDocument());
                        if (change.getType() == DocumentChange.Type.REMOVED) {
                            executors.database().execute(() -> {
                                teamDao.removeMember(member.getWorkspaceId(), member.getUserId());
                                notifyLocalChange();
                            });
                            detachWorkspaceListeners(member.getWorkspaceId());
                        } else {
                            fetchWorkspaceAndAttach(member);
                        }
                    }
                });
        rootListeners.add(listener);
    }

    private void attachInviteListener(String userId) {
        ListenerRegistration listener = firestore.collection(COLLECTION_INVITES)
                .whereEqualTo("invitedUserId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !userId.equals(activeUserId)) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        TeamInvite invite = inviteFrom(change.getDocument());
                        executors.database().execute(() -> {
                            teamDao.saveInvite(invite);
                            String notifiedKey = "invite_notified_" + invite.getInviteId();
                            if (change.getType() == DocumentChange.Type.ADDED &&
                                    InviteStatus.PENDING.equals(invite.getStatus()) &&
                                    !preferences.getBoolean(notifiedKey, false)) {
                                new NotificationDao(context).add(
                                        userId, invite.getWorkspaceId(), null, "TEAM_INVITE",
                                        "Lời mời tham gia nhóm",
                                        "Bạn được mời tham gia " + invite.getWorkspaceName());
                                preferences.edit().putBoolean(notifiedKey, true).apply();
                            }
                            notifyLocalChange();
                        });
                    }
                });
        rootListeners.add(listener);
    }

    private void fetchWorkspaceAndAttach(WorkspaceMember member) {
        String workspaceId = member.getWorkspaceId();
        firestore.collection(COLLECTION_WORKSPACES)
                .document(workspaceId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Workspace workspace = workspaceFrom(document);
                        executors.database().execute(() -> {
                            workspaceDao.save(workspace);
                            persistRemoteMember(member);
                            persistReadyPendingTasks(workspaceId);
                            notifyLocalChange();
                        });
                        attachWorkspaceListeners(workspaceId);
                    }
                });
    }

    private void attachWorkspaceListeners(String workspaceId) {
        if (workspaceListeners.containsKey(workspaceId)) {
            return;
        }
        List<ListenerRegistration> listeners = new ArrayList<>();
        listeners.add(firestore.collection(COLLECTION_WORKSPACES)
                .document(workspaceId)
                .addSnapshotListener((document, error) -> {
                    if (error != null || document == null || !document.exists()) return;
                    Workspace workspace = workspaceFrom(document);
                    executors.database().execute(() -> {
                        workspaceDao.save(workspace);
                        persistReadyPendingTasks(workspaceId);
                        notifyLocalChange();
                    });
                }));
        listeners.add(firestore.collection(COLLECTION_MEMBERS)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        WorkspaceMember member = memberFrom(change.getDocument());
                        if (change.getType() == DocumentChange.Type.REMOVED ||
                                !MembershipStatus.ACTIVE.equals(member.getStatus())) {
                            executors.database().execute(() -> {
                                teamDao.removeMember(workspaceId, member.getUserId());
                                notifyLocalChange();
                            });
                        } else {
                            persistRemoteMember(member);
                        }
                    }
                }));
        listeners.add(firestore.collection(COLLECTION_PROJECTS)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        Project project = projectFrom(change.getDocument());
                        executors.database().execute(() -> {
                            if (change.getType() == DocumentChange.Type.REMOVED) {
                                teamDao.deleteProject(project.getProjectId());
                            } else {
                                teamDao.saveProject(project);
                                persistReadyPendingTasks(project.getWorkspaceId());
                            }
                            notifyLocalChange();
                        });
                    }
                }));
        listeners.add(firestore.collection(COLLECTION_TASKS)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        Task task = taskFrom(change.getDocument());
                        List<String> assigneeIds = stringList(
                                change.getDocument(), "assigneeIds");
                        if (assigneeIds.isEmpty()) {
                            String legacyAssigneeId = change.getDocument().getString("assigneeId");
                            if (legacyAssigneeId != null && !legacyAssigneeId.isBlank()) {
                                assigneeIds = List.of(legacyAssigneeId);
                            }
                        }
                        List<String> remoteAssigneeIds = assigneeIds;
                        executors.database().execute(() -> {
                            if (change.getType() == DocumentChange.Type.REMOVED) {
                                taskDao.delete(task.getTaskId());
                                reminderScheduler.cancel(task.getTaskId());
                                pendingTasks.remove(task.getTaskId());
                            } else {
                                persistRemoteTask(task, remoteAssigneeIds);
                            }
                            notifyLocalChange();
                        });
                    }
                }));
        listeners.add(firestore.collection(COLLECTION_SUBTASKS)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        TaskSubtask subtask = subtaskFrom(change.getDocument());
                        executors.database().execute(() -> {
                            if (change.getType() == DocumentChange.Type.REMOVED) {
                                subtaskDao.delete(subtask.getSubtaskId());
                                List<TaskSubtask> pending = pendingSubtasksByTask.get(subtask.getTaskId());
                                if (pending != null) {
                                    pending.removeIf(item -> subtask.getSubtaskId().equals(item.getSubtaskId()));
                                }
                            } else if (taskDao.findByIdIncludingDeleted(subtask.getTaskId()) == null) {
                                pendingSubtasksByTask
                                        .computeIfAbsent(subtask.getTaskId(), ignored -> new ArrayList<>())
                                        .add(subtask);
                            } else {
                                persistRemoteSubtask(subtask);
                            }
                            recalculateLocalParent(subtask.getTaskId());
                            notifyLocalChange();
                        });
                    }
                }));
        listeners.add(firestore.collection(COLLECTION_HISTORIES)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.REMOVED) continue;
                        TaskHistory history = historyFrom(change.getDocument());
                        executors.database().execute(() -> {
                            if (taskDao.findByIdIncludingDeleted(history.getTaskId()) != null) {
                                historyDao.save(history);
                                notifyLocalChange();
                            } else {
                                pendingHistoriesByTask
                                        .computeIfAbsent(history.getTaskId(), ignored -> new ArrayList<>())
                                        .add(history);
                            }
                        });
                    }
                }));
        listeners.add(firestore.collection(COLLECTION_COMMENTS)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.REMOVED) continue;
                        TaskComment comment = commentFrom(change.getDocument());
                        executors.database().execute(() -> {
                            collaborationDao.saveComment(comment);
                            notifyLocalChange();
                        });
                    }
                }));
        if (BuildConfig.CLOUD_STORAGE_ENABLED) {
            listeners.add(firestore.collection(COLLECTION_ATTACHMENTS)
                    .whereEqualTo("workspaceId", workspaceId)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null || snapshot == null) return;
                        for (DocumentChange change : snapshot.getDocumentChanges()) {
                            if (change.getType() == DocumentChange.Type.REMOVED) continue;
                            TaskAttachment attachment = attachmentFrom(change.getDocument());
                            executors.database().execute(() -> {
                                collaborationDao.saveAttachment(attachment);
                                notifyLocalChange();
                            });
                        }
                    }));
        }
        listeners.add(firestore.collection(COLLECTION_DEPENDENCIES)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        TaskDependency dependency = dependencyFrom(change.getDocument());
                        executors.database().execute(() -> {
                            if (change.getType() == DocumentChange.Type.REMOVED) {
                                collaborationDao.deleteDependency(
                                        dependency.getTaskId(), dependency.getDependsOnTaskId());
                            } else {
                                collaborationDao.saveDependency(dependency);
                            }
                            notifyLocalChange();
                        });
                    }
                }));
        listeners.add(firestore.collection(COLLECTION_MILESTONES)
                .whereEqualTo("workspaceId", workspaceId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.REMOVED) continue;
                        ProjectMilestone milestone = milestoneFrom(change.getDocument());
                        executors.database().execute(() -> {
                            teamDao.saveMilestone(milestone);
                            notifyLocalChange();
                        });
                    }
                }));
        workspaceListeners.put(workspaceId, listeners);
    }

    private void detachWorkspaceListeners(String workspaceId) {
        List<ListenerRegistration> listeners = workspaceListeners.remove(workspaceId);
        if (listeners == null) return;
        for (ListenerRegistration listener : listeners) {
            listener.remove();
        }
    }

    private void persistRemoteMember(WorkspaceMember member) {
        executors.database().execute(() -> {
            if (member.getEmail() != null && !member.getEmail().isBlank()) {
                User user = new User();
                user.setUserId(member.getUserId());
                user.setUserCode(member.getUserCode() == null ||
                        member.getUserCode().isBlank()
                        ? legacyUserCode(member.getUserId())
                        : member.getUserCode());
                user.setEmail(member.getEmail());
                user.setDisplayName(member.getDisplayName() == null ||
                        member.getDisplayName().isBlank()
                        ? member.getEmail()
                        : member.getDisplayName());
                user.setCreatedAt(member.getJoinedAt());
                user.setUpdatedAt(System.currentTimeMillis());
                userDao.saveAuthenticatedUser(user);
            }
            teamDao.insertMember(member);
            persistReadyPendingTasks(member.getWorkspaceId());
            notifyLocalChange();
        });
    }

    private void notifyLocalChange() {
        executors.mainThread().execute(() -> SyncBus.getInstance().notifyChanged());
    }

    private void persistRemoteTask(Task task, List<String> assigneeIds) {
        Task local = taskDao.findByIdIncludingDeleted(task.getTaskId());
        if (local != null && !SyncConflictRules.shouldAcceptRemote(
                local.getVersion(), local.getUpdatedAt(), local.getSyncStatus(),
                task.getVersion(), task.getUpdatedAt())) {
            return;
        }
        if (!taskDependenciesExist(task, assigneeIds)) {
            pendingTasks.put(task.getTaskId(), new PendingTask(task, assigneeIds));
            return;
        }
        taskDao.save(task);
        pendingTasks.remove(task.getTaskId());
        persistPendingSubtasks(task.getTaskId());
        persistPendingHistories(task.getTaskId());
        teamDao.clearTaskAssignees(task.getTaskId());
        for (String assigneeId : assigneeIds) {
            if (assigneeId == null || assigneeId.isBlank()) continue;
            teamDao.insertTaskAssignee(
                    task.getTaskId(),
                    assigneeId,
                    task.getCreatedBy(),
                    task.getUpdatedAt()
            );
        }
        if (task.getDeletedAt() > 0) reminderScheduler.cancel(task.getTaskId());
        else reminderScheduler.schedule(task);
    }

    private void persistPendingSubtasks(String taskId) {
        List<TaskSubtask> pending = pendingSubtasksByTask.remove(taskId);
        if (pending == null) return;
        for (TaskSubtask subtask : pending) {
            persistRemoteSubtask(subtask);
        }
        recalculateLocalParent(taskId);
    }

    private void persistPendingHistories(String taskId) {
        List<TaskHistory> pending = pendingHistoriesByTask.remove(taskId);
        if (pending == null) return;
        for (TaskHistory history : pending) historyDao.save(history);
    }

    private void recalculateLocalParent(String taskId) {
        Task task = taskDao.findById(taskId);
        if (task == null) return;
        List<TaskSubtask> subtasks = subtaskDao.findAllByTask(taskId);
        if (subtasks.isEmpty()) return;
        TaskSubtaskRules.applyToTask(task, subtasks);
        taskDao.updateStatusAndProgressFromRemote(taskId, task.getStatus(), task.getProgress());
        reminderScheduler.schedule(task);
    }

    private void persistRemoteSubtask(TaskSubtask subtask) {
        TaskSubtask local = subtaskDao.findByIdIncludingDeleted(subtask.getSubtaskId());
        if (local != null && !SyncConflictRules.shouldAcceptRemote(
                local.getVersion(), local.getUpdatedAt(), local.getSyncStatus(),
                subtask.getVersion(), subtask.getUpdatedAt())) {
            return;
        }
        subtaskDao.save(subtask);
    }

    private boolean taskDependenciesExist(Task task, List<String> assigneeIds) {
        if (workspaceDao.findById(task.getWorkspaceId()) == null ||
                userDao.findById(task.getCreatedBy()) == null) {
            return false;
        }
        for (String assigneeId : assigneeIds) {
            if (assigneeId != null && !assigneeId.isBlank() &&
                    userDao.findById(assigneeId) == null) {
                return false;
            }
        }
        return task.getProjectId() == null || task.getProjectId().isBlank() ||
                teamDao.findProjectById(task.getProjectId()) != null;
    }

    private void persistReadyPendingTasks(String workspaceId) {
        for (PendingTask pending : new ArrayList<>(pendingTasks.values())) {
            if (workspaceId.equals(pending.task.getWorkspaceId()) &&
                    taskDependenciesExist(pending.task, pending.assigneeIds)) {
                persistRemoteTask(pending.task, pending.assigneeIds);
            }
        }
    }

    private boolean ready() {
        if (!isAvailable()) return false;
        if (firestore == null) {
            firestore = FirebaseProvider.firestore(context);
        }
        return true;
    }

    private void logSyncFailure(Exception exception) {
        Log.e("CLOUD_SYNC", "Không thể đồng bộ dữ liệu", exception);
    }

    private String membershipId(String workspaceId, String userId) {
        return workspaceId + "_" + userId;
    }

    private String legacyUserCode(String userId) {
        String value = userId.replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        return "USR-" + value.substring(0, Math.min(10, value.length()));
    }

    private Map<String, Object> workspaceMap(Workspace workspace) {
        Map<String, Object> values = new HashMap<>();
        values.put("workspaceId", workspace.getWorkspaceId());
        values.put("managerId", workspace.getManagerId());
        values.put("name", workspace.getName());
        values.put("type", workspace.getType());
        values.put("description", workspace.getDescription());
        values.put("status", workspace.getStatus());
        values.put("createdAt", workspace.getCreatedAt());
        values.put("updatedAt", workspace.getUpdatedAt());
        return values;
    }

    private Map<String, Object> projectMap(Project project) {
        Map<String, Object> values = new HashMap<>();
        values.put("projectId", project.getProjectId());
        values.put("workspaceId", project.getWorkspaceId());
        values.put("name", project.getName());
        values.put("description", project.getDescription());
        values.put("status", project.getStatus());
        values.put("createdBy", project.getCreatedBy());
        values.put("managerId", project.getManagerId());
        values.put("startDate", project.getStartDate());
        values.put("dueDate", project.getDueDate());
        values.put("completedAt", project.getCompletedAt());
        values.put("deletedAt", project.getDeletedAt());
        values.put("version", Math.max(1, project.getVersion()));
        values.put("createdAt", project.getCreatedAt());
        values.put("updatedAt", project.getUpdatedAt());
        return values;
    }

    private Map<String, Object> inviteMap(TeamInvite invite) {
        Map<String, Object> values = new HashMap<>();
        values.put("inviteId", invite.getInviteId());
        values.put("workspaceId", invite.getWorkspaceId());
        values.put("workspaceName", invite.getWorkspaceName());
        values.put("invitedUserId", invite.getInvitedUserId());
        values.put("invitedUserCode", invite.getInvitedUserCode());
        values.put("invitedDisplayName", invite.getInvitedDisplayName());
        values.put("email", invite.getEmail());
        values.put("role", invite.getRole());
        values.put("status", invite.getStatus());
        values.put("invitedBy", invite.getInvitedBy());
        values.put("createdAt", invite.getCreatedAt());
        values.put("respondedAt", invite.getRespondedAt());
        values.put("expiresAt", invite.getExpiresAt());
        return values;
    }

    private Map<String, Object> taskMap(Task task) {
        Map<String, Object> values = new HashMap<>();
        values.put("taskId", task.getTaskId());
        values.put("workspaceId", task.getWorkspaceId());
        values.put("projectId", task.getProjectId());
        values.put("createdBy", task.getCreatedBy());
        values.put("title", task.getTitle());
        values.put("description", task.getDescription());
        values.put("status", task.getStatus());
        values.put("priority", task.getPriority());
        values.put("progress", task.getProgress());
        values.put("startDate", task.getStartDate());
        values.put("dueDate", task.getDueDate());
        values.put("estimatedMinutes", task.getEstimatedMinutes());
        values.put("completedAt", task.getCompletedAt());
        values.put("deletedAt", task.getDeletedAt());
        values.put("version", Math.max(1, task.getVersion()));
        values.put("createdAt", task.getCreatedAt());
        values.put("updatedAt", task.getUpdatedAt());
        return values;
    }

    private Map<String, Object> subtaskMap(TaskSubtask subtask) {
        Map<String, Object> values = new HashMap<>();
        values.put("subtaskId", subtask.getSubtaskId());
        values.put("taskId", subtask.getTaskId());
        values.put("workspaceId", subtask.getWorkspaceId());
        values.put("createdBy", subtask.getCreatedBy());
        values.put("assigneeId", subtask.getAssigneeId());
        values.put("title", subtask.getTitle());
        values.put("estimatedMinutes", subtask.getEstimatedMinutes());
        values.put("completed", subtask.isCompleted());
        values.put("completedAt", subtask.getCompletedAt());
        values.put("deletedAt", subtask.getDeletedAt());
        values.put("version", Math.max(1, subtask.getVersion()));
        values.put("sortOrder", subtask.getSortOrder());
        values.put("createdAt", subtask.getCreatedAt());
        values.put("updatedAt", subtask.getUpdatedAt());
        return values;
    }

    private Map<String, Object> historyMap(TaskHistory history, String workspaceId) {
        Map<String, Object> values = new HashMap<>();
        values.put("historyId", history.getHistoryId());
        values.put("workspaceId", workspaceId);
        values.put("taskId", history.getTaskId());
        values.put("userId", history.getUserId());
        values.put("action", history.getAction());
        values.put("detail", history.getDetail());
        values.put("createdAt", history.getCreatedAt());
        return values;
    }

    private Map<String, Object> commentMap(TaskComment item) {
        Map<String, Object> values = new HashMap<>();
        values.put("commentId", item.getCommentId());
        values.put("taskId", item.getTaskId());
        values.put("workspaceId", item.getWorkspaceId());
        values.put("userId", item.getUserId());
        values.put("message", item.getMessage());
        values.put("createdAt", item.getCreatedAt());
        values.put("updatedAt", item.getUpdatedAt());
        values.put("deletedAt", item.getDeletedAt());
        return values;
    }

    private Map<String, Object> attachmentMap(TaskAttachment item) {
        Map<String, Object> values = new HashMap<>();
        values.put("attachmentId", item.getAttachmentId());
        values.put("taskId", item.getTaskId());
        values.put("workspaceId", item.getWorkspaceId());
        values.put("userId", item.getUserId());
        values.put("displayName", item.getDisplayName());
        values.put("mimeType", item.getMimeType());
        values.put("remoteUrl", item.getRemoteUrl());
        values.put("sizeBytes", item.getSizeBytes());
        values.put("createdAt", item.getCreatedAt());
        values.put("deletedAt", item.getDeletedAt());
        return values;
    }

    private Map<String, Object> dependencyMap(TaskDependency item, String workspaceId) {
        Map<String, Object> values = new HashMap<>();
        values.put("taskId", item.getTaskId());
        values.put("dependsOnTaskId", item.getDependsOnTaskId());
        values.put("workspaceId", workspaceId);
        values.put("createdBy", item.getCreatedBy());
        values.put("createdAt", item.getCreatedAt());
        return values;
    }

    private Map<String, Object> milestoneMap(ProjectMilestone item) {
        Map<String, Object> values = new HashMap<>();
        values.put("milestoneId", item.getMilestoneId());
        values.put("projectId", item.getProjectId());
        values.put("workspaceId", item.getWorkspaceId());
        values.put("title", item.getTitle());
        values.put("dueDate", item.getDueDate());
        values.put("completedAt", item.getCompletedAt());
        values.put("createdBy", item.getCreatedBy());
        values.put("createdAt", item.getCreatedAt());
        return values;
    }

    private Workspace workspaceFrom(DocumentSnapshot document) {
        Workspace workspace = new Workspace();
        workspace.setWorkspaceId(string(document, "workspaceId", document.getId()));
        workspace.setManagerId(string(document, "managerId", ""));
        workspace.setName(string(document, "name", ""));
        workspace.setType(string(document, "type", "TEAM"));
        workspace.setDescription(string(document, "description", ""));
        workspace.setStatus(string(document, "status", "ACTIVE"));
        workspace.setCreatedAt(number(document, "createdAt"));
        workspace.setUpdatedAt(number(document, "updatedAt"));
        return workspace;
    }

    private WorkspaceMember memberFrom(DocumentSnapshot document) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspaceId(string(document, "workspaceId", ""));
        member.setUserId(string(document, "userId", ""));
        member.setUserCode(string(document, "userCode", ""));
        member.setRole(string(document, "role", TeamRole.MEMBER));
        member.setStatus(string(document, "status", MembershipStatus.ACTIVE));
        member.setJoinedAt(number(document, "joinedAt"));
        member.setInviteId(document.getString("inviteId"));
        member.setDisplayName(string(document, "displayName", ""));
        member.setEmail(string(document, "email", ""));
        return member;
    }

    private Project projectFrom(DocumentSnapshot document) {
        Project project = new Project();
        project.setProjectId(string(document, "projectId", document.getId()));
        project.setWorkspaceId(string(document, "workspaceId", ""));
        project.setName(string(document, "name", ""));
        project.setDescription(string(document, "description", ""));
        project.setStatus(string(document, "status", "ACTIVE"));
        project.setCreatedBy(string(document, "createdBy", ""));
        project.setManagerId(document.getString("managerId"));
        project.setStartDate(number(document, "startDate"));
        project.setDueDate(number(document, "dueDate"));
        project.setCompletedAt(number(document, "completedAt"));
        project.setDeletedAt(number(document, "deletedAt"));
        project.setVersion(Math.max(1, integer(document, "version")));
        project.setSyncStatus(SyncStatus.SYNCED);
        project.setCreatedAt(number(document, "createdAt"));
        project.setUpdatedAt(number(document, "updatedAt"));
        return project;
    }

    private TeamInvite inviteFrom(DocumentSnapshot document) {
        TeamInvite invite = new TeamInvite();
        invite.setInviteId(string(document, "inviteId", document.getId()));
        invite.setWorkspaceId(string(document, "workspaceId", ""));
        invite.setWorkspaceName(string(document, "workspaceName", "Nhóm"));
        invite.setInvitedUserId(string(document, "invitedUserId", ""));
        invite.setInvitedUserCode(string(document, "invitedUserCode", ""));
        invite.setInvitedDisplayName(string(document, "invitedDisplayName", ""));
        invite.setEmail(string(document, "email", ""));
        invite.setRole(string(document, "role", TeamRole.MEMBER));
        invite.setStatus(string(document, "status", InviteStatus.PENDING));
        invite.setInvitedBy(string(document, "invitedBy", ""));
        invite.setCreatedAt(number(document, "createdAt"));
        invite.setRespondedAt(number(document, "respondedAt"));
        invite.setExpiresAt(number(document, "expiresAt"));
        return invite;
    }

    private Task taskFrom(DocumentSnapshot document) {
        Task task = new Task();
        task.setTaskId(string(document, "taskId", document.getId()));
        task.setWorkspaceId(string(document, "workspaceId", ""));
        task.setProjectId(document.getString("projectId"));
        task.setCreatedBy(string(document, "createdBy", ""));
        task.setTitle(string(document, "title", ""));
        task.setDescription(string(document, "description", ""));
        task.setStatus(string(document, "status", "TODO"));
        task.setPriority(string(document, "priority", "MEDIUM"));
        task.setProgress(integer(document, "progress"));
        task.setStartDate(number(document, "startDate"));
        task.setDueDate(number(document, "dueDate"));
        task.setEstimatedMinutes(integer(document, "estimatedMinutes"));
        task.setCompletedAt(number(document, "completedAt"));
        task.setDeletedAt(number(document, "deletedAt"));
        task.setVersion(Math.max(1, integer(document, "version")));
        task.setSyncStatus(SyncStatus.SYNCED);
        task.setCreatedAt(number(document, "createdAt"));
        task.setUpdatedAt(number(document, "updatedAt"));
        return task;
    }

    private TaskSubtask subtaskFrom(DocumentSnapshot document) {
        TaskSubtask subtask = new TaskSubtask();
        subtask.setSubtaskId(string(document, "subtaskId", document.getId()));
        subtask.setTaskId(string(document, "taskId", ""));
        subtask.setWorkspaceId(string(document, "workspaceId", ""));
        subtask.setCreatedBy(string(document, "createdBy", ""));
        subtask.setAssigneeId(document.getString("assigneeId"));
        subtask.setTitle(string(document, "title", ""));
        subtask.setEstimatedMinutes(integer(document, "estimatedMinutes"));
        Boolean completed = document.getBoolean("completed");
        subtask.setCompleted(Boolean.TRUE.equals(completed));
        subtask.setCompletedAt(number(document, "completedAt"));
        subtask.setDeletedAt(number(document, "deletedAt"));
        subtask.setVersion(Math.max(1, integer(document, "version")));
        subtask.setSyncStatus(SyncStatus.SYNCED);
        subtask.setSortOrder(integer(document, "sortOrder"));
        subtask.setCreatedAt(number(document, "createdAt"));
        subtask.setUpdatedAt(number(document, "updatedAt"));
        return subtask;
    }

    private TaskHistory historyFrom(DocumentSnapshot document) {
        TaskHistory history = new TaskHistory();
        history.setHistoryId(string(document, "historyId", document.getId()));
        history.setTaskId(string(document, "taskId", ""));
        history.setUserId(string(document, "userId", ""));
        history.setAction(string(document, "action", "UPDATED"));
        history.setDetail(string(document, "detail", ""));
        history.setCreatedAt(number(document, "createdAt"));
        return history;
    }

    private TaskComment commentFrom(DocumentSnapshot document) {
        TaskComment item = new TaskComment();
        item.setCommentId(string(document, "commentId", document.getId()));
        item.setTaskId(string(document, "taskId", ""));
        item.setWorkspaceId(string(document, "workspaceId", ""));
        item.setUserId(string(document, "userId", ""));
        item.setMessage(string(document, "message", ""));
        item.setCreatedAt(number(document, "createdAt"));
        item.setUpdatedAt(number(document, "updatedAt"));
        item.setDeletedAt(number(document, "deletedAt"));
        return item;
    }

    private TaskAttachment attachmentFrom(DocumentSnapshot document) {
        TaskAttachment item = new TaskAttachment();
        item.setAttachmentId(string(document, "attachmentId", document.getId()));
        item.setTaskId(string(document, "taskId", ""));
        item.setWorkspaceId(string(document, "workspaceId", ""));
        item.setUserId(string(document, "userId", ""));
        item.setDisplayName(string(document, "displayName", "Tệp đính kèm"));
        item.setMimeType(string(document, "mimeType", "application/octet-stream"));
        item.setRemoteUrl(string(document, "remoteUrl", ""));
        item.setSizeBytes(number(document, "sizeBytes"));
        item.setCreatedAt(number(document, "createdAt"));
        item.setDeletedAt(number(document, "deletedAt"));
        return item;
    }

    private TaskDependency dependencyFrom(DocumentSnapshot document) {
        TaskDependency item = new TaskDependency();
        item.setTaskId(string(document, "taskId", ""));
        item.setDependsOnTaskId(string(document, "dependsOnTaskId", ""));
        item.setCreatedBy(string(document, "createdBy", ""));
        item.setCreatedAt(number(document, "createdAt"));
        return item;
    }

    private ProjectMilestone milestoneFrom(DocumentSnapshot document) {
        ProjectMilestone item = new ProjectMilestone();
        item.setMilestoneId(string(document, "milestoneId", document.getId()));
        item.setProjectId(string(document, "projectId", ""));
        item.setWorkspaceId(string(document, "workspaceId", ""));
        item.setTitle(string(document, "title", ""));
        item.setDueDate(number(document, "dueDate"));
        item.setCompletedAt(number(document, "completedAt"));
        item.setCreatedBy(string(document, "createdBy", ""));
        item.setCreatedAt(number(document, "createdAt"));
        return item;
    }

    private String string(DocumentSnapshot document, String field, String fallback) {
        String value = document.getString(field);
        return value == null ? fallback : value;
    }

    private long number(DocumentSnapshot document, String field) {
        Long value = document.getLong(field);
        return value == null ? 0 : value;
    }

    private int integer(DocumentSnapshot document, String field) {
        return (int) number(document, field);
    }

    private List<String> stringList(DocumentSnapshot document, String field) {
        Object raw = document.get(field);
        if (!(raw instanceof List<?> values)) return List.of();
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String text && !text.isBlank()) result.add(text);
        }
        return result;
    }

    private static final class PendingTask {
        private final Task task;
        private final List<String> assigneeIds;

        private PendingTask(Task task, List<String> assigneeIds) {
            this.task = task;
            this.assigneeIds = new ArrayList<>(assigneeIds);
        }
    }
}
