package com.vandieu_manhdung.taskmanager.core.util;

import com.vandieu_manhdung.taskmanager.core.constant.InviteStatus;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class TeamFeatureRules {
    private TeamFeatureRules() { }

    public static boolean isValidSchedule(long startAt, long dueAt) {
        return dueAt <= 0 || startAt <= 0 || dueAt > startAt;
    }

    public static boolean canRespondToInvite(TeamInvite invite, String userId, long now) {
        return invite != null && userId != null &&
                userId.equals(invite.getInvitedUserId()) &&
                InviteStatus.PENDING.equals(invite.getStatus()) &&
                (invite.getExpiresAt() <= 0 || invite.getExpiresAt() > now);
    }

    public static boolean isDirectDependencyCycle(
            String taskId, String dependsOnId, boolean reverseDependencyExists
    ) {
        return taskId == null || taskId.equals(dependsOnId) || reverseDependencyExists;
    }

    public static boolean wouldCreateDependencyCycle(
            String taskId,
            String dependsOnId,
            Function<String, List<String>> dependencyTargets
    ) {
        if (taskId == null || taskId.equals(dependsOnId)) return true;
        ArrayDeque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(dependsOnId);
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (taskId.equals(current)) return true;
            if (!visited.add(current)) continue;
            List<String> targets = dependencyTargets.apply(current);
            if (targets != null) pending.addAll(targets);
        }
        return false;
    }
}
