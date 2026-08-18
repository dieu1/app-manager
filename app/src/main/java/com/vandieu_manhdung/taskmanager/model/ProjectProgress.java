package com.vandieu_manhdung.taskmanager.model;

public class ProjectProgress {

    private final String projectId;
    private final String projectName;
    private final int total;
    private final int completed;
    private final int overdue;

    public ProjectProgress(
            String projectId,
            String projectName,
            int total,
            int completed,
            int overdue
    ) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.total = total;
        this.completed = completed;
        this.overdue = overdue;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public int getTotal() {
        return total;
    }

    public int getCompleted() {
        return completed;
    }

    public int getOverdue() {
        return overdue;
    }

    public int getCompletionRate() {
        return total == 0 ? 0 : Math.round(completed * 100f / total);
    }
}
