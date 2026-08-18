package com.vandieu_manhdung.taskmanager.core.util;

public final class WorkSessionRules {

    private WorkSessionRules() {
    }

    public static int calculateDurationMinutes(
            long startTime,
            long endTime
    ) {
        long elapsed = Math.max(0, endTime - startTime);
        if (elapsed == 0) {
            return 0;
        }

        return (int) Math.max(1, (elapsed + 59_999L) / 60_000L);
    }
}
