package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vandieu_manhdung.taskmanager.core.constant.SyncStatus;
import com.vandieu_manhdung.taskmanager.core.util.SyncConflictRules;

import org.junit.Test;

public class SyncConflictRulesTest {
    @Test public void pendingLocalSameVersionWins() {
        assertFalse(SyncConflictRules.shouldAcceptRemote(
                3, 200, SyncStatus.PENDING, 3, 300));
    }

    @Test public void failedLocalSameVersionWinsForRetry() {
        assertFalse(SyncConflictRules.shouldAcceptRemote(
                2, 200, SyncStatus.FAILED, 2, 300));
    }

    @Test public void newerRemoteVersionWins() {
        assertTrue(SyncConflictRules.shouldAcceptRemote(
                2, 300, SyncStatus.SYNCED, 3, 200));
    }

    @Test public void olderRemoteVersionIsRejected() {
        assertFalse(SyncConflictRules.shouldAcceptRemote(
                4, 300, SyncStatus.SYNCED, 3, 400));
    }

    @Test public void sameVersionUsesUpdatedTime() {
        assertTrue(SyncConflictRules.shouldAcceptRemote(
                4, 300, SyncStatus.SYNCED, 4, 301));
        assertFalse(SyncConflictRules.shouldAcceptRemote(
                4, 300, SyncStatus.SYNCED, 4, 299));
    }
}
