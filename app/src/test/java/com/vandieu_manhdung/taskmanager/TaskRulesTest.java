package com.vandieu_manhdung.taskmanager;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskRules;
import com.vandieu_manhdung.taskmanager.model.Task;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskRulesTest {

    @Test
    public void todoAlwaysHasZeroProgress() {
        assertEquals(0, TaskRules.normalizeProgress(TaskStatus.TODO, 70));
    }

    @Test
    public void completedAlwaysHasFullProgress() {
        assertEquals(100, TaskRules.normalizeProgress(TaskStatus.COMPLETED, 20));
    }

    @Test
    public void inProgressIsClampedBetweenOneAndNinetyNine() {
        assertEquals(1, TaskRules.normalizeProgress(TaskStatus.IN_PROGRESS, 0));
        assertEquals(99, TaskRules.normalizeProgress(TaskStatus.IN_PROGRESS, 100));
    }

    @Test
    public void activePastDueTaskIsOverdue() {
        Task task = new Task();
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setDueDate(1_000L);

        assertTrue(TaskRules.isOverdue(task, 2_000L));
    }

    @Test
    public void completedPastDueTaskIsNotOverdue() {
        Task task = new Task();
        task.setStatus(TaskStatus.COMPLETED);
        task.setDueDate(1_000L);

        assertFalse(TaskRules.isOverdue(task, 2_000L));
    }
}
