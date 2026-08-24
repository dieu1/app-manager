package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vandieu_manhdung.taskmanager.core.constant.InviteStatus;
import com.vandieu_manhdung.taskmanager.core.util.TeamFeatureRules;
import com.vandieu_manhdung.taskmanager.model.TeamInvite;

import org.junit.Test;

import java.util.List;
import java.util.Map;

public class TeamFeatureRulesTest {

    @Test
    public void dependencyCycleDetectsIndirectPath() {
        Map<String, List<String>> graph = Map.of(
                "B", List.of("C"),
                "C", List.of("A")
        );
        assertTrue(TeamFeatureRules.wouldCreateDependencyCycle(
                "A", "B", node -> graph.getOrDefault(node, List.of())));
    }

    @Test
    public void dependencyCycleAllowsAcyclicPath() {
        Map<String, List<String>> graph = Map.of("B", List.of("C"));
        assertFalse(TeamFeatureRules.wouldCreateDependencyCycle(
                "A", "B", node -> graph.getOrDefault(node, List.of())));
    }
    @Test public void scheduleRequiresDueAfterStart() {
        assertTrue(TeamFeatureRules.isValidSchedule(100, 200));
        assertFalse(TeamFeatureRules.isValidSchedule(200, 100));
    }

    @Test public void onlyTargetCanAcceptPendingUnexpiredInvite() {
        TeamInvite invite = new TeamInvite();
        invite.setInvitedUserId("target");
        invite.setStatus(InviteStatus.PENDING);
        invite.setExpiresAt(2_000);
        assertTrue(TeamFeatureRules.canRespondToInvite(invite, "target", 1_000));
        assertFalse(TeamFeatureRules.canRespondToInvite(invite, "other", 1_000));
        assertFalse(TeamFeatureRules.canRespondToInvite(invite, "target", 3_000));
    }

    @Test public void directAndReverseDependenciesAreRejected() {
        assertTrue(TeamFeatureRules.isDirectDependencyCycle("a", "a", false));
        assertTrue(TeamFeatureRules.isDirectDependencyCycle("a", "b", true));
        assertFalse(TeamFeatureRules.isDirectDependencyCycle("a", "b", false));
    }
}
