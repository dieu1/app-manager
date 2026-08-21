package com.vandieu_manhdung.taskmanager;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.core.util.TaskSubtaskRules;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TaskSubtaskRulesTest {

    @Test
    public void progressUsesCompletedStepCount() {
        TaskSubtask first = subtask(true);
        TaskSubtask second = subtask(false);
        TaskSubtask third = subtask(false);

        assertEquals(33, TaskSubtaskRules.calculateProgress(
                Arrays.asList(first, second, third)
        ));
    }

    @Test
    public void progressUpdatesTaskStatus() {
        Task task = new Task();
        task.setStatus(TaskStatus.TODO);

        TaskSubtask first = subtask(true);
        TaskSubtask second = subtask(true);

        TaskSubtaskRules.applyToTask(task, Arrays.asList(first, second));

        assertEquals(100, task.getProgress());
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
    }

    @Test
    public void emptyChecklistDoesNotOverwriteManualTaskProgress() {
        Task task = new Task();
        task.setProgress(40);
        task.setStatus(TaskStatus.IN_PROGRESS);

        TaskSubtaskRules.applyToTask(task, Collections.emptyList());

        assertEquals(40, task.getProgress());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
    }

    private TaskSubtask subtask(boolean completed) {
        TaskSubtask subtask = new TaskSubtask();
        subtask.setCompleted(completed);
        return subtask;
    }
}
