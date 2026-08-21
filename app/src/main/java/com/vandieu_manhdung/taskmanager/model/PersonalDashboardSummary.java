package com.vandieu_manhdung.taskmanager.model;

public class PersonalDashboardSummary {

    private final int total;
    private final int todo;
    private final int inProgress;
    private final int completed;
    private final int cancelled;
    private final int overdue;
    private final int completionRate;

    public PersonalDashboardSummary(
            int total,
            int todo,
            int inProgress,
            int completed,
            int cancelled,
            int overdue
    ) {
        this(
                total,
                todo,
                inProgress,
                completed,
                cancelled,
                overdue,
                total == 0 ? 0 : Math.round(completed * 100f / total)
        );
    }

    public PersonalDashboardSummary(
            int total,
            int todo,
            int inProgress,
            int completed,
            int cancelled,
            int overdue,
            int completionRate
    ) {
        this.total = total;
        this.todo = todo;
        this.inProgress = inProgress;
        this.completed = completed;
        this.cancelled = cancelled;
        this.overdue = overdue;
        this.completionRate = Math.max(0, Math.min(100, completionRate));
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
        return completionRate;
    }
}
