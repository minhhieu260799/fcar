package com.fcar.core.util;

import java.util.regex.Pattern;

/**
 * Shared password strength rule used by registration, change password, and forgot-password flows.
 */
public final class PasswordPolicy {

    public static final Pattern PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{8,}$"
    );

    private PasswordPolicy() {
    }

    public static boolean matches(String rawPassword) {
        return rawPassword != null && PATTERN.matcher(rawPassword).matches();
    }
}
