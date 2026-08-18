package com.vandieu_manhdung.taskmanager.model;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;

import java.util.Collections;
import java.util.List;

public class TeamDashboardSummary {

    private final int total;
    private final int todo;
    private final int inProgress;
    private final int completed;
    private final int cancelled;
    private final int overdue;

    public TeamDashboardSummary(List<TeamTaskItem> items, long currentTime) {
        List<TeamTaskItem> safeItems = items == null
                ? Collections.emptyList()
                : items;
        total = safeItems.size();
        int todoCount = 0;
        int inProgressCount = 0;
        int completedCount = 0;
        int cancelledCount = 0;
        int overdueCount = 0;
        for (TeamTaskItem item : safeItems) {
            Task task = item.getTask();
            if (TaskStatus.TODO.equals(task.getStatus())) {
                todoCount++;
            } else if (TaskStatus.IN_PROGRESS.equals(task.getStatus())) {
                inProgressCount++;
            } else if (TaskStatus.COMPLETED.equals(task.getStatus())) {
                completedCount++;
            } else if (TaskStatus.CANCELLED.equals(task.getStatus())) {
                cancelledCount++;
            }
            if (TaskRules.isOverdue(task, currentTime)) {
                overdueCount++;
            }
        }
        todo = todoCount;
        inProgress = inProgressCount;
        completed = completedCount;
        cancelled = cancelledCount;
        overdue = overdueCount;
    }

    public int getTotal() {
        return total;
    }

    public int getTodo() {
        return todo;
    }

    public int getInProgress() {
        return inProgress;
    }

    public int getCompleted() {
        return completed;
    }

    public int getCancelled() {
        return cancelled;
    }

    public int getOverdue() {
        return overdue;
    }

    public int getCompletionRate() {
        return total == 0 ? 0 : Math.round(completed * 100f / total);
    }
}
