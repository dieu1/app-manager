package com.vandieu_manhdung.taskmanager.core.util;

import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.model.Task;
import com.vandieu_manhdung.taskmanager.model.TaskSubtask;

import java.util.Collections;
import java.util.List;

public final class TaskSubtaskRules {

    private TaskSubtaskRules() {
    }

    public static int completedCount(List<TaskSubtask> subtasks) {
        int count = 0;
        for (TaskSubtask subtask : safe(subtasks)) {
            if (subtask.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public static int calculateProgress(List<TaskSubtask> subtasks) {
        List<TaskSubtask> safeSubtasks = safe(subtasks);
        if (safeSubtasks.isEmpty()) {
            return 0;
        }
        return Math.round(completedCount(safeSubtasks) * 100f / safeSubtasks.size());
    }

    public static void applyToTask(Task task, List<TaskSubtask> subtasks) {
        if (task == null || safe(subtasks).isEmpty()) {
            return;
        }

        int progress = calculateProgress(subtasks);
        task.setProgress(progress);
        if (progress >= 100) {
            task.setStatus(TaskStatus.COMPLETED);
        } else if (progress > 0) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        } else {
            task.setStatus(TaskStatus.TODO);
        }
    }

    private static List<TaskSubtask> safe(List<TaskSubtask> subtasks) {
        return subtasks == null ? Collections.emptyList() : subtasks;
    }
}
