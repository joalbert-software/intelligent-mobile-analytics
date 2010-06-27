package com.ima.analytics;

import com.ima.domain.Session;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SessionSummary {
    private final long sessions;
    private final long users;
    private final long bounces;
    private final long events;
    private final DescriptiveStatistics durations;

    public SessionSummary(Collection<Session> sessions) {
        Set<UserKey> uniqueUsers = new HashSet<UserKey>();
        List<Long> lengths = new ArrayList<Long>();
        long bounceCount = 0;
        long eventCount = 0;
        for (Session session : sessions) {
            uniqueUsers.add(session.getUser());
            lengths.add(session.getDuration());
            eventCount += session.getEventCount();
            if (session.isBounce()) {
                bounceCount++;
            }
        }
        this.sessions = sessions.size();
        this.users = uniqueUsers.size();
        this.bounces = bounceCount;
        this.events = eventCount;
        this.durations = new DescriptiveStatistics(lengths);
    }

    public long getSessions() {
        return sessions;
    }

    public long getUsers() {
        return users;
    }

    public long getBounces() {
        return bounces;
    }

    public long getEvents() {
        return events;
    }

    public double getBounceRate() {
        return sessions == 0 ? 0.0 : (double) bounces / sessions;
    }

    public double getSessionsPerUser() {
        return users == 0 ? 0.0 : (double) sessions / users;
    }

    public DescriptiveStatistics getDurations() {
        return durations;
    }
}
