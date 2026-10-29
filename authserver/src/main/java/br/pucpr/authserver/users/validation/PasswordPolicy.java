package br.pucpr.authserver.users.validation;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class PasswordPolicy {
    private static final Pattern PATTERN = Pattern.compile(
            "(?=.*\\p{L})(?=.*\\p{N})(?=.*[@$!%*#?&])[\\p{L}\\p{N}@$!%*#?&]{8,}");

    private PasswordPolicy() { }

    public static boolean withinBcryptLimit(String value) {
        return value != null && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public static boolean isValid(String value) {
        return withinBcryptLimit(value) && PATTERN.matcher(value).matches();
    }
}
