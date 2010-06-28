package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.Session;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class Sessionizer {
    public static final long DEFAULT_GAP_MILLIS = 30L * 60L * 1000L;
    private final long gapMillis;

    public Sessionizer(long gapMillis) {
        this.gapMillis = Checks.positive(gapMillis, "gapMillis");
    }

    public Sessionizer() {
        this(DEFAULT_GAP_MILLIS);
    }

    public List<Session> calculate(Collection<Event> events) {
        List<Session> sessions = new ArrayList<Session>();
        for (Map.Entry<UserKey, List<Event>> group : EventOrdering.byUser(events).entrySet()) {
            List<Event> current = new ArrayList<Event>();
            long previous = -1;
            for (Event event : group.getValue()) {
                if (!current.isEmpty() && event.getTimestamp() - previous > gapMillis) {
                    sessions.add(new Session(group.getKey(), current));
                    current = new ArrayList<Event>();
                }
                current.add(event);
                previous = event.getTimestamp();
            }
            if (!current.isEmpty()) {
                sessions.add(new Session(group.getKey(), current));
            }
        }
        return sessions;
    }
}
