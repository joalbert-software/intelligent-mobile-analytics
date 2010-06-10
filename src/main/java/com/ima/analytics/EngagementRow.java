package com.ima.analytics;

public final class EngagementRow {
    private final int activeDays;
    private final long users;

    public EngagementRow(int activeDays, long users) {
        this.activeDays = activeDays;
        this.users = users;
    }

    public int getActiveDays() {
        return activeDays;
    }

    public long getUsers() {
        return users;
    }
}
