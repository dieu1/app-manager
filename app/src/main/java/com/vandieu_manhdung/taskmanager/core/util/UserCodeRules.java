package com.vandieu_manhdung.taskmanager.core.util;

import java.util.Locale;

public final class UserCodeRules {

    private UserCodeRules() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String value) {
        return normalize(value).matches("USR-[A-Z0-9]{8,32}");
    }
}
