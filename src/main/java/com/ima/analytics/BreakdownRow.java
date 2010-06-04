package com.ima.analytics;

public final class BreakdownRow {
    private final String value;
    private final long events;
    private final long users;

    public BreakdownRow(String value, long events, long users) {
        this.value = value;
        this.events = events;
        this.users = users;
    }

    public String getValue() {
        return value;
    }

    public boolean isMissing() {
        return value == null;
    }

    public long getEvents() {
        return events;
    }

    public long getUsers() {
        return users;
    }
}
