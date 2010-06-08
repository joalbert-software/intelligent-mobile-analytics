package com.ima.analytics;

public final class CrashSummary {
    private final long users;
    private final long affectedUsers;
    private final long crashes;
    private final long sessions;
    private final long affectedSessions;

    public CrashSummary(long users, long affectedUsers, long crashes,
                        long sessions, long affectedSessions) {
        this.users = users;
        this.affectedUsers = affectedUsers;
        this.crashes = crashes;
        this.sessions = sessions;
        this.affectedSessions = affectedSessions;
    }

    public long getUsers() {
        return users;
    }

    public long getAffectedUsers() {
        return affectedUsers;
    }

    public long getCrashes() {
        return crashes;
    }

    public long getSessions() {
        return sessions;
    }

    public long getAffectedSessions() {
        return affectedSessions;
    }

    public Double getCrashFreeUserRate() {
        return users == 0 ? null : 1.0 - (double) affectedUsers / users;
    }

    public Double getCrashFreeSessionRate() {
        return sessions == 0 ? null : 1.0 - (double) affectedSessions / sessions;
    }
}
