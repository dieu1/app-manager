package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vandieu_manhdung.taskmanager.model.AppNotification;

import org.junit.Test;

public class AppNotificationTest {
    @Test public void readStateDependsOnReadTimestamp() {
        AppNotification notification = new AppNotification();
        assertFalse(notification.isRead());
        notification.setReadAt(123L);
        assertTrue(notification.isRead());
    }
}
