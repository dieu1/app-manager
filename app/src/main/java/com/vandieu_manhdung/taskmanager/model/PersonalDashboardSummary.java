package com.vandieu_manhdung.taskmanager.model;

public class PersonalDashboardSummary {

    private final int total;
    private final int todo;
    private final int inProgress;
    private final int completed;
    private final int cancelled;
    private final int overdue;

    public PersonalDashboardSummary(
            int total,
            int todo,
            int inProgress,
            int completed,
            int cancelled,
            int overdue
    ) {
        this.total = total;
        this.todo = todo;
        this.inProgress = inProgress;
        this.completed = completed;
        this.cancelled = cancelled;
        this.overdue = overdue;
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
        if (total == 0) {
            return 0;
        }

        return Math.round(completed * 100f / total);
    }
}
