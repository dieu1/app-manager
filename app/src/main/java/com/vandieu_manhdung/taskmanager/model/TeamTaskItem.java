package com.vandieu_manhdung.taskmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamTaskItem {

    private final Task task;
    private final String projectName;
    private final List<String> assigneeIds;
    private final List<String> assigneeNames;

    public TeamTaskItem(
            Task task,
            String projectName,
            String assigneeId,
            String assigneeName
    ) {
        this(task, projectName,
                assigneeId == null ? List.of() : List.of(assigneeId),
                assigneeName == null ? List.of() : List.of(assigneeName));
    }

    public TeamTaskItem(
            Task task,
            String projectName,
            List<String> assigneeIds,
            List<String> assigneeNames
    ) {
        this.task = task;
        this.projectName = projectName;
        this.assigneeIds = Collections.unmodifiableList(new ArrayList<>(assigneeIds));
        this.assigneeNames = Collections.unmodifiableList(new ArrayList<>(assigneeNames));
    }

    public Task getTask() {
        return task;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getAssigneeId() {
        return assigneeIds.isEmpty() ? null : assigneeIds.get(0);
    }

    public String getAssigneeName() {
        return assigneeNames.isEmpty() ? null : String.join(", ", assigneeNames);
    }

    public List<String> getAssigneeIds() {
        return assigneeIds;
    }

    public List<String> getAssigneeNames() {
        return assigneeNames;
    }
}
