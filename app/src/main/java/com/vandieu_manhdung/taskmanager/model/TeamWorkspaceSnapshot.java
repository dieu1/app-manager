package com.vandieu_manhdung.taskmanager.model;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;

import java.util.Collections;
import java.util.List;

public class TeamWorkspaceSnapshot {

    private final Workspace workspace;
    private final String currentRole;
    private final List<WorkspaceMember> members;
    private final List<Project> projects;
    private final List<TeamTaskItem> tasks;
    private final int totalTasks;
    private final int completedTasks;
    private final int overdueTasks;
    private final boolean hasMoreTasks;

    public TeamWorkspaceSnapshot(
            Workspace workspace,
            String currentRole,
            List<WorkspaceMember> members,
            List<Project> projects,
            List<TeamTaskItem> tasks,
            List<TeamTaskItem> summaryTasks,
            long currentTime
    ) {
        this(workspace, currentRole, members, projects, tasks, summaryTasks,
                currentTime, false);
    }

    public TeamWorkspaceSnapshot(
            Workspace workspace,
            String currentRole,
            List<WorkspaceMember> members,
            List<Project> projects,
            List<TeamTaskItem> tasks,
            List<TeamTaskItem> summaryTasks,
            long currentTime,
            boolean hasMoreTasks
    ) {
        this.workspace = workspace;
        this.currentRole = currentRole;
        this.members = members == null ? Collections.emptyList() : members;
        this.projects = projects == null ? Collections.emptyList() : projects;
        this.tasks = tasks == null ? Collections.emptyList() : tasks;
        List<TeamTaskItem> countedTasks = summaryTasks == null
                ? Collections.emptyList()
                : summaryTasks;
        totalTasks = countedTasks.size();
        int completed = 0;
        int overdue = 0;
        for (TeamTaskItem item : countedTasks) {
            if (TaskStatus.COMPLETED.equals(item.getTask().getStatus())) {
                completed++;
            }
            if (TaskRules.isOverdue(item.getTask(), currentTime)) {
                overdue++;
            }
        }
        completedTasks = completed;
        overdueTasks = overdue;
        this.hasMoreTasks = hasMoreTasks;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public List<WorkspaceMember> getMembers() {
        return members;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public List<TeamTaskItem> getTasks() {
        return tasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public int getOverdueTasks() {
        return overdueTasks;
    }

    public boolean hasMoreTasks() {
        return hasMoreTasks;
    }
}
