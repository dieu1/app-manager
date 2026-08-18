package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vandieu_manhdung.taskmanager.core.constant.TeamRole;
import com.vandieu_manhdung.taskmanager.core.util.TeamRules;
import com.vandieu_manhdung.taskmanager.model.Task;

import org.junit.Test;

public class TeamRulesTest {

    @Test
    public void ownerCanManageWorkspaceMembersAndProjects() {
        assertTrue(TeamRules.canManageWorkspace(TeamRole.OWNER));
        assertTrue(TeamRules.canManageMembers(TeamRole.OWNER));
        assertTrue(TeamRules.canManageProjects(TeamRole.OWNER));
    }

    @Test
    public void adminCannotDisbandWorkspaceButCanManageMembers() {
        assertFalse(TeamRules.canManageWorkspace(TeamRole.ADMIN));
        assertTrue(TeamRules.canManageMembers(TeamRole.ADMIN));
    }

    @Test
    public void memberCanCreateTaskButCannotManageMembers() {
        assertTrue(TeamRules.canCreateTask(TeamRole.MEMBER));
        assertFalse(TeamRules.canManageMembers(TeamRole.MEMBER));
    }

    @Test
    public void creatorCanEditAndDeleteOwnTask() {
        Task task = taskCreatedBy("creator");
        assertTrue(TeamRules.canEditTask(
                TeamRole.MEMBER, "creator", task, "other"));
        assertTrue(TeamRules.canDeleteTask(
                TeamRole.MEMBER, "creator", task));
    }

    @Test
    public void assigneeCanEditButCannotDeleteTaskOfAnotherUser() {
        Task task = taskCreatedBy("creator");
        assertTrue(TeamRules.canEditTask(
                TeamRole.MEMBER, "assignee", task, "assignee"));
        assertFalse(TeamRules.canDeleteTask(
                TeamRole.MEMBER, "assignee", task));
    }

    @Test
    public void adminCanEditAndDeleteAnyTask() {
        Task task = taskCreatedBy("creator");
        assertTrue(TeamRules.canEditTask(
                TeamRole.ADMIN, "admin", task, "member"));
        assertTrue(TeamRules.canDeleteTask(
                TeamRole.ADMIN, "admin", task));
    }

    @Test
    public void unknownRoleHasNoPermission() {
        Task task = taskCreatedBy("creator");
        assertFalse(TeamRules.isValidRole("UNKNOWN"));
        assertFalse(TeamRules.canCreateTask("UNKNOWN"));
        assertFalse(TeamRules.canEditTask("UNKNOWN", "other", task, "assignee"));
    }

    private Task taskCreatedBy(String userId) {
        Task task = new Task();
        task.setCreatedBy(userId);
        return task;
    }
}
