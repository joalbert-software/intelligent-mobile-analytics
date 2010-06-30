package com.ima.domain;

import java.util.Collection;
import java.util.regex.Pattern;

/** Shared boundary validation. Identifiers are opaque, case-sensitive tokens. */
public final class Checks {
    private static final Pattern IDENTIFIER =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.:-]{0,127}");

    private Checks() {
    }

    public static <T> T required(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    public static String identifier(String value, String field) {
        required(value, field);
        if (!IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " is not a valid identifier");
        }
        return value;
    }

    public static String text(String value, String field, int maximum) {
        required(value, field);
        if (value.trim().isEmpty() || value.length() > maximum) {
            throw new IllegalArgumentException(field + " has invalid length");
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                throw new IllegalArgumentException(field + " contains a control character");
            }
        }
        return value;
    }

    public static int range(int value, String field, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(field + " is outside its allowed range");
        }
        return value;
    }

    public static long nonnegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    public static long positive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    public static <T extends Collection<?>> T bounded(
            T value, String field, int minimum, int maximum) {
        required(value, field);
        range(value.size(), field + " count", minimum, maximum);
        return value;
    }
}
