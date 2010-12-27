package com.ima.config;

import java.util.Map;

public final class Settings {
    private final Map<String, String> environment;

    public Settings(Map<String, String> environment) {
        this.environment = environment;
    }

    public String required(String name) {
        String value = environment.get(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("missing environment variable: " + name);
        }
        return value;
    }

    public String optional(String name, String fallback) {
        String value = environment.get(name);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public int integer(String name, int fallback, int minimum, int maximum) {
        String value = optional(name, Integer.toString(fallback));
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) {
                throw new IllegalArgumentException("environment variable outside range: " + name);
            }
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("environment variable is not an integer: " + name);
        }
    }
}
