package com.vandieu_manhdung.taskmanager.model;
public class Task {

    private String taskId;
    private String workspaceId;
    private String projectId;
    private String createdBy;

    private String title;
    private String description;
    private String status;
    private String priority;

    private int progress;
    private long startDate;
    private long dueDate;
    private int estimatedMinutes;
    private long completedAt;
    private long deletedAt;
    private int version;
    private String syncStatus;

    private long createdAt;
    private long updatedAt;

    public Task() {
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public long getCompletedAt() { return completedAt; }

    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getDeletedAt() { return deletedAt; }

    public void setDeletedAt(long deletedAt) { this.deletedAt = deletedAt; }

    public int getVersion() { return version; }

    public void setVersion(int version) { this.version = version; }

    public String getSyncStatus() { return syncStatus; }

    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

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
