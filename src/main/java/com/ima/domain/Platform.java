package com.ima.domain;

import java.util.Locale;

public enum Platform {
    ANDROID,
    IOS,
    WEB,
    OTHER;

    public static Platform parse(String value) {
        Checks.required(value, "platform");
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported platform");
        }
    }
}
