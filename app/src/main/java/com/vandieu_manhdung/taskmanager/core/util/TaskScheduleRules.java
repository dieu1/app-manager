package com.vandieu_manhdung.taskmanager.core.util;

import java.util.concurrent.TimeUnit;

public final class TaskScheduleRules {

    public static final long DUE_SOON_OFFSET_MILLIS = TimeUnit.HOURS.toMillis(1);

    private TaskScheduleRules() {
    }

    public static int calculateEstimatedMinutes(long startAt, long dueAt) {
        if (startAt <= 0 || dueAt <= startAt) {
            return 0;
        }
        long duration = dueAt - startAt;
        long roundedMinutes = (duration + TimeUnit.MINUTES.toMillis(1) - 1)
                / TimeUnit.MINUTES.toMillis(1);
        return (int) Math.min(Integer.MAX_VALUE, roundedMinutes);
    }

    public static long dueSoonAt(long dueAt) {
        return dueAt <= 0 ? 0 : dueAt - DUE_SOON_OFFSET_MILLIS;
    }

    public static long nextDueSoonAt(long dueAt, long now) {
        if (dueAt <= now) {
            return 0;
        }
        return Math.max(now + 1_000L, dueSoonAt(dueAt));
    }
}
