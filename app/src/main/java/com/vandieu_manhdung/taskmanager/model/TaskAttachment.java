package com.vandieu_manhdung.taskmanager.model;

public class TaskAttachment {
    private String attachmentId;
    private String taskId;
    private String workspaceId;
    private String userId;
    private String displayName;
    private String mimeType;
    private String localUri;
    private String remoteUrl;
    private long sizeBytes;
    private long createdAt;
    private long deletedAt;

    public String getAttachmentId() { return attachmentId; }
    public void setAttachmentId(String value) { attachmentId = value; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String value) { workspaceId = value; }
    public String getUserId() { return userId; }
    public void setUserId(String value) { userId = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String value) { mimeType = value; }
    public String getLocalUri() { return localUri; }
    public void setLocalUri(String value) { localUri = value; }
    public String getRemoteUrl() { return remoteUrl; }
    public void setRemoteUrl(String value) { remoteUrl = value; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long value) { sizeBytes = value; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long value) { createdAt = value; }
    public long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(long value) { deletedAt = value; }
}
