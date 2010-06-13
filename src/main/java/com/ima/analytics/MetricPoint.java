package com.ima.analytics;

import com.ima.domain.Checks;

public final class MetricPoint {
    private final long timestamp;
    private final long events;
    private final long users;

    public MetricPoint(long timestamp, long events, long users) {
        this.timestamp = Checks.nonnegative(timestamp, "timestamp");
        this.events = Checks.nonnegative(events, "events");
        this.users = Checks.nonnegative(users, "users");
        if (users > events) {
            throw new IllegalArgumentException("users cannot exceed events");
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getEvents() {
        return events;
    }

    public long getUsers() {
        return users;
    }

    public double getEventsPerUser() {
        return users == 0 ? 0.0 : (double) events / users;
    }
}
