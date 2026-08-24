package com.vandieu_manhdung.taskmanager.core.util;

import com.vandieu_manhdung.taskmanager.model.Task;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GanttLayoutRules {

    public static final long DAY_MILLIS = TimeUnit.DAYS.toMillis(1);

    private GanttLayoutRules() {
    }

    public static long[] range(List<Task> tasks, long fallbackTime) {
        long minimum = Long.MAX_VALUE;
        long maximum = Long.MIN_VALUE;
        for (Task task : tasks) {
            long start = taskStart(task, fallbackTime);
            long end = taskEnd(task, start);
            minimum = Math.min(minimum, start);
            maximum = Math.max(maximum, end);
        }
        if (minimum == Long.MAX_VALUE) {
            minimum = fallbackTime;
            maximum = fallbackTime + 6 * DAY_MILLIS;
        }
        return new long[]{startOfDay(minimum), startOfDay(maximum)};
    }

    public static long taskStart(Task task, long fallbackTime) {
        if (task.getStartDate() > 0) return task.getStartDate();
        if (task.getCreatedAt() > 0) return task.getCreatedAt();
        return fallbackTime;
    }

    public static long taskEnd(Task task, long resolvedStart) {
        return task.getDueDate() >= resolvedStart
                ? task.getDueDate()
                : resolvedStart + DAY_MILLIS;
    }

    public static int inclusiveDayCount(long rangeStart, long rangeEnd) {
        return Math.max(1, dayIndex(rangeStart, rangeEnd) + 1);
    }

    public static int dayIndex(long rangeStart, long timestamp) {
        return Math.max(0, (int) ((startOfDay(timestamp) - startOfDay(rangeStart)) /
                DAY_MILLIS));
    }

    public static float progressFraction(int progress) {
        return Math.max(0, Math.min(100, progress)) / 100f;
    }

    public static long startOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
