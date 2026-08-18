package com.vandieu_manhdung.taskmanager;

import com.vandieu_manhdung.taskmanager.model.PersonalDashboardSummary;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PersonalDashboardSummaryTest {

    @Test
    public void completionRateIsRoundedFromCompletedTasks() {
        PersonalDashboardSummary summary =
                new PersonalDashboardSummary(3, 1, 0, 2, 0, 0);

        assertEquals(67, summary.getCompletionRate());
    }

    @Test
    public void emptyDashboardHasZeroCompletionRate() {
        PersonalDashboardSummary summary =
                new PersonalDashboardSummary(0, 0, 0, 0, 0, 0);

        assertEquals(0, summary.getCompletionRate());
    }
}
