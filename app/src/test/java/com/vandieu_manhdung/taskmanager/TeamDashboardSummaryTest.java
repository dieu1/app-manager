package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertEquals;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamDashboardSummary;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class TeamDashboardSummaryTest {

    @Test
    public void countsStatusesOverdueAndCompletionRate() {
        TeamDashboardSummary summary = new TeamDashboardSummary(
                List.of(
                        item(TaskStatus.TODO, 5_000),
                        item(TaskStatus.IN_PROGRESS, 20_000),
                        item(TaskStatus.COMPLETED, 5_000),
                        item(TaskStatus.CANCELLED, 5_000)
                ),
                10_000
        );

        assertEquals(4, summary.getTotal());
        assertEquals(1, summary.getTodo());
        assertEquals(1, summary.getInProgress());
        assertEquals(1, summary.getCompleted());
        assertEquals(1, summary.getCancelled());
        assertEquals(1, summary.getOverdue());
        assertEquals(25, summary.getCompletionRate());
    }

    @Test
    public void emptySummaryHasZeroCompletionRate() {
        TeamDashboardSummary summary = new TeamDashboardSummary(
                Collections.emptyList(), 10_000);
        assertEquals(0, summary.getTotal());
        assertEquals(0, summary.getCompletionRate());
    }

    private TeamTaskItem item(String status, long dueDate) {
        Task task = new Task();
        task.setStatus(status);
        task.setDueDate(dueDate);
        return new TeamTaskItem(task, "Dự án", "user", "Thành viên");
    }
}
