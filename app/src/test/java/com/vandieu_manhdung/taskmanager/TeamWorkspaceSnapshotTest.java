package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertEquals;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TeamTaskItem;
import com.vandieu_manhdung.taskmanager.model.TeamWorkspaceSnapshot;
import com.vandieu_manhdung.taskmanager.model.Workspace;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class TeamWorkspaceSnapshotTest {

    @Test
    public void summaryUsesAllTasksInsteadOfFilteredTasks() {
        long now = 10_000L;
        TeamTaskItem completed = item(TaskStatus.COMPLETED, 5_000L);
        TeamTaskItem overdue = item(TaskStatus.IN_PROGRESS, 5_000L);
        TeamTaskItem future = item(TaskStatus.TODO, 20_000L);

        TeamWorkspaceSnapshot snapshot = new TeamWorkspaceSnapshot(
                new Workspace(),
                "OWNER",
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(future),
                List.of(completed, overdue, future),
                now
        );

        assertEquals(3, snapshot.getTotalTasks());
        assertEquals(1, snapshot.getCompletedTasks());
        assertEquals(1, snapshot.getOverdueTasks());
        assertEquals(1, snapshot.getTasks().size());
    }

    private TeamTaskItem item(String status, long dueDate) {
        Task task = new Task();
        task.setStatus(status);
        task.setDueDate(dueDate);
        return new TeamTaskItem(task, "Dự án", "user", "Thành viên");
    }
}
