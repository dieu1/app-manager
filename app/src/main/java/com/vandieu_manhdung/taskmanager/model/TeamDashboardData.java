package com.vandieu_manhdung.taskmanager.model;

import java.util.Collections;
import java.util.List;

public class TeamDashboardData {

    private final TeamWorkspaceSnapshot snapshot;
    private final TeamDashboardSummary summary;
    private final List<ProjectProgress> projectProgress;

    public TeamDashboardData(
            TeamWorkspaceSnapshot snapshot,
            TeamDashboardSummary summary,
            List<ProjectProgress> projectProgress
    ) {
        this.snapshot = snapshot;
        this.summary = summary;
        this.projectProgress = projectProgress == null
                ? Collections.emptyList()
                : projectProgress;
    }

    public TeamWorkspaceSnapshot getSnapshot() {
        return snapshot;
    }

    public TeamDashboardSummary getSummary() {
        return summary;
    }

    public List<ProjectProgress> getProjectProgress() {
        return projectProgress;
    }
}
