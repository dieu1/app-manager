package com.vandieu_manhdung.taskmanager.core.util;

import com.vandieu_manhdung.taskmanager.core.constant.TeamRole;
import com.vandieu_manhdung.taskmanager.model.Task;

public final class TeamRules {

    private TeamRules() {
    }

    public static boolean isValidRole(String role) {
        return TeamRole.OWNER.equals(role) ||
                TeamRole.ADMIN.equals(role) ||
                TeamRole.MEMBER.equals(role);
    }

    public static boolean canManageWorkspace(String role) {
        return TeamRole.OWNER.equals(role);
    }

    public static boolean canManageMembers(String role) {
        return TeamRole.OWNER.equals(role) ||
                TeamRole.ADMIN.equals(role);
    }

    public static boolean canManageProjects(String role) {
        return canManageMembers(role);
    }

    public static boolean canCreateTask(String role) {
        return isValidRole(role);
    }

    public static boolean canEditTask(
            String role,
            String actorId,
            Task task,
            String assigneeId
    ) {
        return canManageMembers(role) ||
                actorId != null && task != null &&
                        (actorId.equals(task.getCreatedBy()) ||
                                actorId.equals(assigneeId));
    }

    public static boolean canDeleteTask(
            String role,
            String actorId,
            Task task
    ) {
        return canManageMembers(role) ||
                actorId != null && task != null &&
                        actorId.equals(task.getCreatedBy());
    }
}
