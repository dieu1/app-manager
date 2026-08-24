package com.vandieu_manhdung.taskmanager.model;

public class ProjectMilestone {
    private String milestoneId;
    private String projectId;
    private String workspaceId;
    private String title;
    private long dueDate;
    private long completedAt;
    private String createdBy;
    private long createdAt;

    public String getMilestoneId() { return milestoneId; }
    public void setMilestoneId(String value) { milestoneId = value; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String value) { projectId = value; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String value) { workspaceId = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public long getDueDate() { return dueDate; }
    public void setDueDate(long value) { dueDate = value; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long value) { completedAt = value; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String value) { createdBy = value; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long value) { createdAt = value; }
    public boolean isCompleted() { return completedAt > 0; }
}
