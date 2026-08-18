package com.vandieu_manhdung.taskmanager.model;

public class WorkTimerState {

    private final WorkSession activeSession;
    private final int totalMinutes;

    public WorkTimerState(
            WorkSession activeSession,
            int totalMinutes
    ) {
        this.activeSession = activeSession;
        this.totalMinutes = totalMinutes;
    }

    public WorkSession getActiveSession() {
        return activeSession;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public boolean isRunning() {
        return activeSession != null;
    }
}
