package com.vandieu_manhdung.taskmanager.model;

public class TaskSubtask {

    private String subtaskId;
    private String taskId;
    private String workspaceId;
    private String createdBy;
    private String assigneeId;
    private String title;
    private int estimatedMinutes;
    private boolean completed;
    private long completedAt;
    private long deletedAt;
    private int version;
    private String syncStatus;
    private int sortOrder;
    private long createdAt;
    private long updatedAt;

    public String getSubtaskId() {
        return subtaskId;
    }

    public void setSubtaskId(String subtaskId) {
        this.subtaskId = subtaskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCompletedAt() { return completedAt; }

    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getDeletedAt() { return deletedAt; }

    public void setDeletedAt(long deletedAt) { this.deletedAt = deletedAt; }

    public int getVersion() { return version; }

    public void setVersion(int version) { this.version = version; }

    public String getSyncStatus() { return syncStatus; }

    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
