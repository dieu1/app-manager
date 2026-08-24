package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertEquals;

import com.vandieu_manhdung.taskmanager.core.util.TaskScheduleRules;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TaskScheduleRulesTest {

    @Test
    public void calculateEstimatedMinutes_usesStartAndEnd() {
        long start = 1_000L;
        long end = start + TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30);
        assertEquals(150, TaskScheduleRules.calculateEstimatedMinutes(start, end));
    }

    @Test
    public void dueSoonAt_isOneHourBeforeDeadline() {
        long due = TimeUnit.HOURS.toMillis(10);
        assertEquals(TimeUnit.HOURS.toMillis(9), TaskScheduleRules.dueSoonAt(due));
    }

    @Test
    public void nextDueSoonAt_schedulesImmediatelyWhenLessThanOneHourRemains() {
        long now = TimeUnit.HOURS.toMillis(10);
        long due = now + TimeUnit.MINUTES.toMillis(20);
        assertEquals(now + 1_000L, TaskScheduleRules.nextDueSoonAt(due, now));
    }

    @Test
    public void nextDueSoonAt_ignoresExpiredTask() {
        long now = TimeUnit.HOURS.toMillis(10);
        assertEquals(0, TaskScheduleRules.nextDueSoonAt(now, now));
    }
}
