package com.vandieu_manhdung.taskmanager.model;

public class TaskDependency {
    private String taskId;
    private String dependsOnTaskId;
    private String dependsOnTitle;
    private String createdBy;
    private long createdAt;

    public String getTaskId() { return taskId; }
    public void setTaskId(String value) { taskId = value; }
    public String getDependsOnTaskId() { return dependsOnTaskId; }
    public void setDependsOnTaskId(String value) { dependsOnTaskId = value; }
    public String getDependsOnTitle() { return dependsOnTitle; }
    public void setDependsOnTitle(String value) { dependsOnTitle = value; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String value) { createdBy = value; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long value) { createdAt = value; }
}
