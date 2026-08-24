package com.vandieu_manhdung.taskmanager.model;

public class TaskComment {
    private String commentId;
    private String taskId;
    private String workspaceId;
    private String userId;
    private String userDisplayName;
    private String message;
    private long createdAt;
    private long updatedAt;
    private long deletedAt;

    public String getCommentId() { return commentId; }
    public void setCommentId(String value) { commentId = value; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String value) { workspaceId = value; }
    public String getUserId() { return userId; }
    public void setUserId(String value) { userId = value; }
    public String getUserDisplayName() { return userDisplayName; }
    public void setUserDisplayName(String value) { userDisplayName = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long value) { createdAt = value; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long value) { updatedAt = value; }
    public long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(long value) { deletedAt = value; }
}
