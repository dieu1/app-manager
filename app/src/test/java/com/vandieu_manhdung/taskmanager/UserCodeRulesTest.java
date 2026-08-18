package com.vandieu_manhdung.taskmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vandieu_manhdung.taskmanager.core.util.UserCodeRules;

import org.junit.Test;

public class UserCodeRulesTest {

    @Test
    public void normalizeTrimsAndUppercasesCode() {
        assertEquals("USR-ABC12345", UserCodeRules.normalize("  usr-abc12345  "));
    }

    @Test
    public void acceptsGeneratedPublicCode() {
        assertTrue(UserCodeRules.isValid("USR-4F80A12BC991"));
    }

    @Test
    public void rejectsEmailUidAndMalformedCode() {
        assertFalse(UserCodeRules.isValid("member@example.com"));
        assertFalse(UserCodeRules.isValid("firebaseUid123"));
        assertFalse(UserCodeRules.isValid("USR-short"));
    }
}
