package com.ima.analytics;

import com.ima.domain.Event;
import com.ima.domain.Session;
import com.ima.domain.UserKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CrashCalculator {
    public CrashSummary calculate(Collection<Event> events, long sessionGap) {
        List<Session> sessions = new Sessionizer(sessionGap).calculate(events);
        Set<UserKey> users = new HashSet<UserKey>();
        Set<UserKey> affected = new HashSet<UserKey>();
        long crashes = 0;
        long affectedSessions = 0;
        for (Session session : sessions) {
            users.add(session.getUser());
            boolean hasCrash = false;
            for (Event event : session.getEvents()) {
                if ("crash".equals(event.getName())) {
                    crashes++;
                    hasCrash = true;
                    affected.add(session.getUser());
                }
            }
            if (hasCrash) {
                affectedSessions++;
            }
        }
        return new CrashSummary(users.size(), affected.size(), crashes,
                sessions.size(), affectedSessions);
    }
}
