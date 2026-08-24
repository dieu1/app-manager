package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertEquals;

import com.vandieu_manhdung.taskmanager.core.util.GanttLayoutRules;
import com.vandieu_manhdung.taskmanager.model.Task;

import org.junit.Test;

import java.util.List;

public class GanttLayoutRulesTest {

    @Test
    public void rangeCoversEveryTaskAndUsesInclusiveDays() {
        long firstDay = GanttLayoutRules.startOfDay(1_800_000_000_000L);
        Task first = task(firstDay, firstDay + GanttLayoutRules.DAY_MILLIS);
        Task second = task(firstDay + 3 * GanttLayoutRules.DAY_MILLIS,
                firstDay + 5 * GanttLayoutRules.DAY_MILLIS);
        long[] range = GanttLayoutRules.range(List.of(first, second), firstDay);
        assertEquals(firstDay, range[0]);
        assertEquals(6, GanttLayoutRules.inclusiveDayCount(range[0], range[1]));
    }

    @Test
    public void progressFractionIsClamped() {
        assertEquals(0f, GanttLayoutRules.progressFraction(-10), 0f);
        assertEquals(.42f, GanttLayoutRules.progressFraction(42), 0f);
        assertEquals(1f, GanttLayoutRules.progressFraction(140), 0f);
    }

    private Task task(long start, long due) {
        Task task = new Task();
        task.setStartDate(start);
        task.setDueDate(due);
        return task;
    }
}
