package com.vandieu_manhdung.taskmanager.core.util;

import com.vandieu_manhdung.taskmanager.core.constant.TaskPriority;
import com.vandieu_manhdung.taskmanager.core.constant.TaskStatus;
import com.vandieu_manhdung.taskmanager.model.Task;

public final class TaskRules {

    private TaskRules() {
    }

    public static boolean isValidStatus(String status) {
        return TaskStatus.TODO.equals(status) ||
                TaskStatus.IN_PROGRESS.equals(status) ||
                TaskStatus.COMPLETED.equals(status) ||
                TaskStatus.CANCELLED.equals(status);
    }

    public static boolean isValidPriority(String priority) {
        return TaskPriority.LOW.equals(priority) ||
                TaskPriority.MEDIUM.equals(priority) ||
                TaskPriority.HIGH.equals(priority) ||
                TaskPriority.URGENT.equals(priority);
    }

    public static int normalizeProgress(
            String status,
            int progress
    ) {
        if (TaskStatus.TODO.equals(status)) {
            return 0;
        }

        if (TaskStatus.COMPLETED.equals(status)) {
            return 100;
        }

        if (TaskStatus.IN_PROGRESS.equals(status)) {
            return Math.max(1, Math.min(progress, 99));
        }

        return Math.max(0, Math.min(progress, 100));
    }

    public static boolean isOverdue(
            Task task,
            long currentTime
    ) {
        return task != null &&
                task.getDueDate() > 0 &&
                task.getDueDate() < currentTime &&
                !TaskStatus.COMPLETED.equals(task.getStatus()) &&
                !TaskStatus.CANCELLED.equals(task.getStatus());
    }
}
