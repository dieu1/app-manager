package com.vandieu_manhdung.taskmanager.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.google.firebase.firestore.SetOptions;
import com.vandieu_manhdung.taskmanager.BuildConfig;
import com.vandieu_manhdung.taskmanager.core.callback.RepositoryCallback;
import com.vandieu_manhdung.taskmanager.core.constant.MembershipStatus;
import com.vandieu_manhdung.taskmanager.core.util.AppExecutors;
import com.vandieu_manhdung.taskmanager.core.util.TeamRules;
import com.vandieu_manhdung.taskmanager.data.local.dao.TaskDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamCollaborationDao;
import com.vandieu_manhdung.taskmanager.data.local.dao.TeamDao;
import com.vandieu_manhdung.taskmanager.data.remote.FirebaseProvider;
import com.vandieu_manhdung.taskmanager.data.remote.CloudSyncManager;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskAttachment;
import com.vandieu_manhdung.taskmanager.model.TaskComment;
import com.vandieu_manhdung.taskmanager.model.TaskDependency;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.WorkspaceMember;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

public class TeamCollaborationRepository {
    private static final long MAX_ATTACHMENT_BYTES = 20L * 1024 * 1024;

    private final Context context;
    private final AppExecutors executors;
    private final TaskDao taskDao;
    private final TeamDao teamDao;
    private final TeamCollaborationDao dao;
    private final CloudSyncManager cloudSync;

    public TeamCollaborationRepository(Context context) {
        this.context = context.getApplicationContext();
        executors = AppExecutors.getInstance();
        taskDao = new TaskDao(this.context);
        teamDao = new TeamDao(this.context);
        dao = new TeamCollaborationDao(this.context);
        cloudSync = CloudSyncManager.getInstance(this.context);
    }

    public void getComments(String taskId, String userId,
                            RepositoryCallback<List<TaskComment>> callback) {
        execute(() -> { requireTeamTask(taskId, userId); return dao.findComments(taskId); }, callback);
    }

    public void addComment(String taskId, String userId, String message,
                           RepositoryCallback<Boolean> callback) {
        execute(() -> {
            Task task = requireTeamTask(taskId, userId);
            String clean = message == null ? "" : message.trim();
            if (clean.isEmpty()) throw new IllegalArgumentException("Bình luận không được để trống");
            if (clean.length() > 2000) throw new IllegalArgumentException("Bình luận quá dài");
            long now = System.currentTimeMillis();
            TaskComment item = new TaskComment();
            item.setCommentId(UUID.randomUUID().toString());
            item.setTaskId(taskId);
            item.setWorkspaceId(task.getWorkspaceId());
            item.setUserId(userId);
            item.setMessage(clean);
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            dao.saveComment(item);
            cloudSync.upsertComment(item);
            return true;
        }, callback);
    }

    public void editComment(String commentId, String userId, String message,
                            RepositoryCallback<Boolean> callback) {
        execute(() -> {
            TaskComment item = requireComment(commentId, userId);
            if (!userId.equals(item.getUserId())) {
                throw new SecurityException("Chỉ tác giả được sửa bình luận");
            }
            String clean = cleanComment(message);
            item.setMessage(clean);
            item.setUpdatedAt(System.currentTimeMillis());
            dao.saveComment(item);
            cloudSync.upsertComment(item);
            return true;
        }, callback);
    }

    public void deleteComment(String commentId, String userId,
                              RepositoryCallback<Boolean> callback) {
        execute(() -> {
            TaskComment item = requireComment(commentId, userId);
            WorkspaceMember member = teamDao.findMember(item.getWorkspaceId(), userId);
            if (!userId.equals(item.getUserId()) &&
                    (member == null || !TeamRules.canManageMembers(member.getRole()))) {
                throw new SecurityException("Bạn không có quyền xóa bình luận này");
            }
            long now = System.currentTimeMillis();
            item.setDeletedAt(now);
            item.setUpdatedAt(now);
            dao.saveComment(item);
            cloudSync.upsertComment(item);
            return true;
        }, callback);
    }

    public void getAttachments(String taskId, String userId,
                               RepositoryCallback<List<TaskAttachment>> callback) {
        execute(() -> { requireTeamTask(taskId, userId); return dao.findAttachments(taskId); }, callback);
    }

    public void addAttachment(String taskId, String userId, Uri uri,
                              RepositoryCallback<Boolean> callback) {
        if (!BuildConfig.CLOUD_STORAGE_ENABLED) {
            callback.onError(new IllegalStateException(
                    "Tệp đính kèm đang tắt trong chế độ Firebase Spark"));
            return;
        }
        if (uri == null) { callback.onError(new IllegalArgumentException("Chưa chọn tệp")); return; }
        execute(() -> {
            Task task = requireTeamTask(taskId, userId);
            TaskAttachment item = attachmentMetadata(task, userId, uri);
            if (item.getSizeBytes() > MAX_ATTACHMENT_BYTES) {
                throw new IllegalArgumentException("Tệp không được lớn hơn 20 MB");
            }
            dao.saveAttachment(item);
            return item;
        }, new RepositoryCallback<TaskAttachment>() {
            @Override public void onSuccess(TaskAttachment item) {
                if (!FirebaseProvider.isConfigured(context)) {
                    callback.onError(new IllegalStateException("Cần Firebase để tải tệp lên"));
                    return;
                }
                String path = "team_attachments/" + item.getWorkspaceId() + "/" +
                        item.getTaskId() + "/" + item.getAttachmentId();
                FirebaseProvider.storage(context).getReference().child(path).putFile(uri)
                        .continueWithTask(task -> {
                            if (!task.isSuccessful()) throw task.getException();
                            return FirebaseProvider.storage(context).getReference()
                                    .child(path).getDownloadUrl();
                        })
                        .addOnSuccessListener(download -> executors.database().execute(() -> {
                            item.setRemoteUrl(download.toString());
                            dao.saveAttachment(item);
                            cloudSync.upsertAttachment(item);
                            FirebaseProvider.firestore(context).collection("task_attachments")
                                    .document(item.getAttachmentId())
                                    .set(attachmentMap(item), SetOptions.merge())
                                    .addOnSuccessListener(ignored -> callback.onSuccess(true))
                                    .addOnFailureListener(error -> callback.onError(error));
                        }))
                        .addOnFailureListener(error -> callback.onError(
                                new IllegalStateException("Không thể tải tệp lên", error)));
            }
            @Override public void onError(Exception exception) { callback.onError(exception); }
        });
    }

    public void getDependencies(String taskId, String userId,
                                RepositoryCallback<List<TaskDependency>> callback) {
        execute(() -> { requireTeamTask(taskId, userId); return dao.findDependencies(taskId); }, callback);
    }

    public void getDependencyCandidates(String taskId, String userId,
                                        RepositoryCallback<List<TeamTaskItem>> callback) {
        execute(() -> {
            Task task = requireTeamTask(taskId, userId);
            List<TeamTaskItem> result = new ArrayList<>();
            for (TeamTaskItem item : teamDao.queryTeamTasks(task.getWorkspaceId(), null, null, null)) {
                String candidateId = item.getTask().getTaskId();
                if (!taskId.equals(candidateId) &&
                        Objects.equals(task.getProjectId(), item.getTask().getProjectId()) &&
                        !dao.hasDependency(taskId, candidateId) &&
                        !dao.wouldCreateDependencyCycle(taskId, candidateId)) {
                    result.add(item);
                }
            }
            return result;
        }, callback);
    }

    public void addDependency(String taskId, String dependsOnTaskId, String userId,
                              RepositoryCallback<Boolean> callback) {
        execute(() -> {
            Task task = requireTeamTask(taskId, userId);
            Task dependency = requireTeamTask(dependsOnTaskId, userId);
            if (!task.getWorkspaceId().equals(dependency.getWorkspaceId())) {
                throw new IllegalArgumentException("Công việc phụ thuộc phải cùng Team");
            }
            if (!Objects.equals(task.getProjectId(), dependency.getProjectId())) {
                throw new IllegalArgumentException("Công việc phụ thuộc phải cùng dự án");
            }
            if (dao.hasDependency(taskId, dependsOnTaskId)) {
                throw new IllegalArgumentException("Quan hệ phụ thuộc đã tồn tại");
            }
            if (dao.wouldCreateDependencyCycle(taskId, dependsOnTaskId)) {
                throw new IllegalArgumentException("Quan hệ phụ thuộc tạo thành vòng lặp");
            }
            TaskDependency item = new TaskDependency();
            item.setTaskId(taskId);
            item.setDependsOnTaskId(dependsOnTaskId);
            item.setCreatedBy(userId);
            item.setCreatedAt(System.currentTimeMillis());
            dao.saveDependency(item);
            cloudSync.upsertDependency(item);
            return true;
        }, callback);
    }

    public void deleteDependency(String taskId, String dependsOnTaskId, String userId,
                                 RepositoryCallback<Boolean> callback) {
        execute(() -> {
            Task task = requireTeamTask(taskId, userId);
            TaskDependency dependency = dao.findDependency(taskId, dependsOnTaskId);
            if (dependency == null) throw new IllegalStateException("Quan hệ phụ thuộc không tồn tại");
            WorkspaceMember member = teamDao.findMember(task.getWorkspaceId(), userId);
            if (!userId.equals(dependency.getCreatedBy()) &&
                    (member == null || !TeamRules.canManageMembers(member.getRole()))) {
                throw new SecurityException("Bạn không có quyền xóa quan hệ phụ thuộc này");
            }
            if (dao.deleteDependency(taskId, dependsOnTaskId) <= 0) {
                throw new IllegalStateException("Không thể xóa quan hệ phụ thuộc");
            }
            cloudSync.deleteDependency(taskId, dependsOnTaskId);
            return true;
        }, callback);
    }

    private TaskComment requireComment(String commentId, String userId) {
        TaskComment item = dao.findComment(commentId);
        if (item == null || item.getDeletedAt() > 0) {
            throw new IllegalStateException("Bình luận không tồn tại");
        }
        requireTeamTask(item.getTaskId(), userId);
        return item;
    }

    private String cleanComment(String message) {
        String clean = message == null ? "" : message.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Bình luận không được để trống");
        if (clean.length() > 2000) throw new IllegalArgumentException("Bình luận quá dài");
        return clean;
    }

    private Task requireTeamTask(String taskId, String userId) {
        Task task = taskDao.findById(taskId);
        if (task == null || task.getProjectId() == null || task.getProjectId().isBlank()) {
            throw new IllegalStateException("Công việc Team không tồn tại");
        }
        WorkspaceMember member = teamDao.findMember(task.getWorkspaceId(), userId);
        if (member == null || !MembershipStatus.ACTIVE.equals(member.getStatus())) {
            throw new SecurityException("Bạn không còn là thành viên Team");
        }
        return task;
    }

    private TaskAttachment attachmentMetadata(Task task, String userId, Uri uri) {
        TaskAttachment item = new TaskAttachment();
        item.setAttachmentId(UUID.randomUUID().toString());
        item.setTaskId(task.getTaskId());
        item.setWorkspaceId(task.getWorkspaceId());
        item.setUserId(userId);
        item.setLocalUri(uri.toString());
        item.setMimeType(context.getContentResolver().getType(uri));
        item.setDisplayName("Tệp đính kèm");
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int size = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (name >= 0) item.setDisplayName(cursor.getString(name));
                if (size >= 0 && !cursor.isNull(size)) item.setSizeBytes(cursor.getLong(size));
            }
        }
        item.setCreatedAt(System.currentTimeMillis());
        return item;
    }

    private Map<String, Object> commentMap(TaskComment item) {
        Map<String, Object> map = new HashMap<>();
        map.put("commentId", item.getCommentId()); map.put("taskId", item.getTaskId());
        map.put("workspaceId", item.getWorkspaceId()); map.put("userId", item.getUserId());
        map.put("message", item.getMessage()); map.put("createdAt", item.getCreatedAt());
        map.put("updatedAt", item.getUpdatedAt()); map.put("deletedAt", item.getDeletedAt());
        return map;
    }

    private Map<String, Object> attachmentMap(TaskAttachment item) {
        Map<String, Object> map = new HashMap<>();
        map.put("attachmentId", item.getAttachmentId()); map.put("taskId", item.getTaskId());
        map.put("workspaceId", item.getWorkspaceId()); map.put("userId", item.getUserId());
        map.put("displayName", item.getDisplayName()); map.put("mimeType", item.getMimeType());
        map.put("remoteUrl", item.getRemoteUrl()); map.put("sizeBytes", item.getSizeBytes());
        map.put("createdAt", item.getCreatedAt()); map.put("deletedAt", item.getDeletedAt());
        return map;
    }

    private Map<String, Object> dependencyMap(TaskDependency item, String workspaceId) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", item.getTaskId()); map.put("dependsOnTaskId", item.getDependsOnTaskId());
        map.put("workspaceId", workspaceId); map.put("createdBy", item.getCreatedBy());
        map.put("createdAt", item.getCreatedAt()); return map;
    }

    private <T> void execute(Callable<T> operation, RepositoryCallback<T> callback) {
        executors.database().execute(() -> {
            try {
                T value = operation.call();
                executors.mainThread().execute(() -> callback.onSuccess(value));
            } catch (Exception exception) {
                executors.mainThread().execute(() -> callback.onError(exception));
            }
        });
    }
}
