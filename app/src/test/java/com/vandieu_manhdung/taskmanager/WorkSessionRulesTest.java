package com.vandieu_manhdung.taskmanager;

import com.vandieu_manhdung.taskmanager.core.util.WorkSessionRules;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WorkSessionRulesTest {

    @Test
    public void positivePartialMinuteRoundsUp() {
        assertEquals(1, WorkSessionRules.calculateDurationMinutes(1_000L, 2_000L));
    }

    @Test
    public void fullMinutesAreCalculatedCorrectly() {
        assertEquals(3, WorkSessionRules.calculateDurationMinutes(0L, 180_000L));
    }

    @Test
    public void invalidNegativeDurationReturnsZero() {
        assertEquals(0, WorkSessionRules.calculateDurationMinutes(2_000L, 1_000L));
    }
}
