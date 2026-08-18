package com.vandieu_manhdung.taskmanager.model;

public class TeamTaskItem {

    private final Task task;
    private final String projectName;
    private final String assigneeId;
    private final String assigneeName;

    public TeamTaskItem(
            Task task,
            String projectName,
            String assigneeId,
            String assigneeName
    ) {
        this.task = task;
        this.projectName = projectName;
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }

    public Task getTask() {
        return task;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }
}
