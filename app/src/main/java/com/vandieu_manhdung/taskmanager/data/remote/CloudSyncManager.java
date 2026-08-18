package com.vandieu_manhdung.taskmanager.data.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.MembershipStatus;
import com.vandieu_manhdung.taskmanager.core.constant.TeamRole;
import com.vandieu_manhdung.taskmanager.core.sync.SyncBus;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.UserDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkSessionDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.WorkspaceDao;
import com.vandieu_manhdung.taskmanager.model.Project;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.User;
import com.vandieu_manhdung.taskmanager.model.WorkSession;
import com.vandieu_manhdung.taskmanager.model.Workspace;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CloudSyncManager {

    private static final String COLLECTION_USER_CODES = "user_codes";
    private static final String COLLECTION_WORKSPACES = "workspaces";
    private static final String COLLECTION_MEMBERS = "workspace_members";
    private static final String COLLECTION_PROJECTS = "projects";
    private static final String COLLECTION_TASKS = "tasks";
    private static final String COLLECTION_SESSIONS = "work_sessions";
    private static final String PREFS = "cloud_sync_state";

    private static volatile CloudSyncManager instance;

    private final Context context;
    private final AppExecutors executors;
    private final WorkspaceDao workspaceDao;
    private final UserDao userDao;
    private final TaskDao taskDao;
    private final TeamDao teamDao;
    private final WorkSessionDao workSessionDao;
    private final SharedPreferences preferences;
    private final List<ListenerRegistration> rootListeners = new ArrayList<>();
    private final Map<String, List<ListenerRegistration>> workspaceListeners =
            new ConcurrentHashMap<>();
    private final Map<String, List<WorkSession>> pendingSessionsByTask =
            new ConcurrentHashMap<>();
    private final Map<String, PendingTask> pendingTasks = new ConcurrentHashMap<>();

    private FirebaseFirestore firestore;
    private String activeUserId;

    private CloudSyncManager(Context context) {
        this.context = context.getApplicationContext();
        executors = AppExecutors.getInstance();
        workspaceDao = new WorkspaceDao(this.context);
        userDao = new UserDao(this.context);
        taskDao = new TaskDao(this.context);
        teamDao = new TeamDao(this.context);
        workSessionDao = new WorkSessionDao(this.context);
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
        pendingSessionsByTask.clear();
        pendingTasks.clear();
        activeUserId = null;
    }

    public void upsertWorkspace(Workspace workspace, String userId, String role) {
        if (!ready() || workspace == null) return;
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
        if (!ready() || member == null) return;
        firestore.collection(COLLECTION_MEMBERS)
                .document(membershipId(member.getWorkspaceId(), member.getUserId()))
                .set(memberMap(member), SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
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
        values.put("displayName", member.getDisplayName());
        values.put("email", member.getEmail());
        values.put("updatedAt", System.currentTimeMillis());
        return values;
    }

    public void removeMember(String workspaceId, String userId) {
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
        if (!ready() || project == null) return;
        firestore.collection(COLLECTION_PROJECTS)
                .document(project.getProjectId())
                .set(projectMap(project), SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
    }

    public void deleteProject(String projectId) {
        if (ready()) {
            firestore.collection(COLLECTION_PROJECTS).document(projectId).delete()
                    .addOnFailureListener(this::logSyncFailure);
        }
    }

    public void upsertTask(Task task, String assigneeId) {
        if (!ready() || task == null) return;
        Map<String, Object> values = taskMap(task);
        values.put("assigneeId", assigneeId);
        firestore.collection(COLLECTION_TASKS)
                .document(task.getTaskId())
                .set(values, SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
    }

    public void deleteTask(String taskId) {
        if (ready()) {
            firestore.collection(COLLECTION_TASKS).document(taskId).delete()
                    .addOnFailureListener(this::logSyncFailure);
        }
    }

    public void upsertWorkSession(WorkSession session, String workspaceId) {
        if (!ready() || session == null) return;
        Map<String, Object> values = new HashMap<>();
        values.put("sessionId", session.getSessionId());
        values.put("taskId", session.getTaskId());
        values.put("workspaceId", workspaceId);
        values.put("userId", session.getUserId());
        values.put("startTime", session.getStartTime());
        values.put("endTime", session.getEndTime());
        values.put("durationMinutes", session.getDurationMinutes());
        values.put("updatedAt", System.currentTimeMillis());
        firestore.collection(COLLECTION_SESSIONS)
                .document(session.getSessionId())
                .set(values, SetOptions.merge())
                .addOnFailureListener(this::logSyncFailure);
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
            for (WorkSession session : workSessionDao.findAllByUser(userId)) {
                upsertWorkSession(session, personal.getWorkspaceId());
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
                            persistRemoteMember(member);
                            fetchWorkspaceAndAttach(member.getWorkspaceId());
                        }
                    }
                });
        rootListeners.add(listener);
        rootListeners.add(firestore.collection(COLLECTION_SESSIONS)
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null ||
                            !userId.equals(activeUserId)) {
                        return;
                    }
                    for (DocumentChange change : snapshot.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.REMOVED) continue;
                        WorkSession session = sessionFrom(change.getDocument());
                        executors.database().execute(() -> {
                            if (taskDao.findById(session.getTaskId()) != null) {
                                workSessionDao.save(session);
                            } else {
                                pendingSessionsByTask
                                        .computeIfAbsent(
                                                session.getTaskId(),
                                                ignored -> new ArrayList<>()
                                        )
                                        .add(session);
                            }
                            notifyLocalChange();
                        });
                    }
                }));
    }

    private void fetchWorkspaceAndAttach(String workspaceId) {
        firestore.collection(COLLECTION_WORKSPACES)
                .document(workspaceId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Workspace workspace = workspaceFrom(document);
                        executors.database().execute(() -> {
                            workspaceDao.save(workspace);
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
                        String assigneeId = change.getDocument().getString("assigneeId");
                        executors.database().execute(() -> {
                            if (change.getType() == DocumentChange.Type.REMOVED) {
                                taskDao.delete(task.getTaskId());
                                pendingTasks.remove(task.getTaskId());
                            } else {
                                persistRemoteTask(task, assigneeId);
                            }
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

    private void persistPendingSessions(String taskId) {
        List<WorkSession> sessions = pendingSessionsByTask.remove(taskId);
        if (sessions == null) return;
        for (WorkSession session : sessions) {
            workSessionDao.save(session);
        }
    }

    private void persistRemoteTask(Task task, String assigneeId) {
        if (!taskDependenciesExist(task, assigneeId)) {
            pendingTasks.put(task.getTaskId(), new PendingTask(task, assigneeId));
            return;
        }
        taskDao.save(task);
        pendingTasks.remove(task.getTaskId());
        persistPendingSessions(task.getTaskId());
        teamDao.clearTaskAssignees(task.getTaskId());
        if (assigneeId != null && !assigneeId.isBlank()) {
            teamDao.insertTaskAssignee(
                    task.getTaskId(),
                    assigneeId,
                    task.getCreatedBy(),
                    task.getUpdatedAt()
            );
        }
    }

    private boolean taskDependenciesExist(Task task, String assigneeId) {
        if (workspaceDao.findById(task.getWorkspaceId()) == null ||
                userDao.findById(task.getCreatedBy()) == null) {
            return false;
        }
        if (assigneeId != null && !assigneeId.isBlank() &&
                userDao.findById(assigneeId) == null) {
            return false;
        }
        return task.getProjectId() == null || task.getProjectId().isBlank() ||
                teamDao.findProjectById(task.getProjectId()) != null;
    }

    private void persistReadyPendingTasks(String workspaceId) {
        for (PendingTask pending : new ArrayList<>(pendingTasks.values())) {
            if (workspaceId.equals(pending.task.getWorkspaceId()) &&
                    taskDependenciesExist(pending.task, pending.assigneeId)) {
                persistRemoteTask(pending.task, pending.assigneeId);
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
        values.put("createdAt", project.getCreatedAt());
        values.put("updatedAt", project.getUpdatedAt());
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
        values.put("createdAt", task.getCreatedAt());
        values.put("updatedAt", task.getUpdatedAt());
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
        project.setCreatedAt(number(document, "createdAt"));
        project.setUpdatedAt(number(document, "updatedAt"));
        return project;
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
        task.setCreatedAt(number(document, "createdAt"));
        task.setUpdatedAt(number(document, "updatedAt"));
        return task;
    }

    private WorkSession sessionFrom(DocumentSnapshot document) {
        WorkSession session = new WorkSession();
        session.setSessionId(string(document, "sessionId", document.getId()));
        session.setTaskId(string(document, "taskId", ""));
        session.setUserId(string(document, "userId", ""));
        session.setStartTime(number(document, "startTime"));
        session.setEndTime(number(document, "endTime"));
        session.setDurationMinutes(integer(document, "durationMinutes"));
        return session;
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

    private static final class PendingTask {
        private final Task task;
        private final String assigneeId;

        private PendingTask(Task task, String assigneeId) {
            this.task = task;
            this.assigneeId = assigneeId;
        }
    }
}
