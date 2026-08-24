package com.vandieu_manhdung.taskmanager.core.util;

import com.vandieu_manhdung.taskmanager.core.constant.SyncStatus;

public final class SyncConflictRules {
    private SyncConflictRules() {
    }

    public static boolean shouldAcceptRemote(
            int localVersion,
            long localUpdatedAt,
            String localSyncStatus,
            int remoteVersion,
            long remoteUpdatedAt
    ) {
        if ((SyncStatus.PENDING.equals(localSyncStatus) ||
                SyncStatus.FAILED.equals(localSyncStatus)) &&
                localVersion >= remoteVersion) {
            return false;
        }
        if (remoteVersion != localVersion) {
            return remoteVersion > localVersion;
        }
        return remoteUpdatedAt >= localUpdatedAt;
    }
}
