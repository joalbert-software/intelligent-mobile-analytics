package com.ima.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Session {
    private final UserKey user;
    private final List<Event> events;

    public Session(UserKey user, List<Event> events) {
        this.user = Checks.required(user, "user");
        Checks.bounded(events, "events", 1, Integer.MAX_VALUE);
        long previous = -1;
        for (Event event : events) {
            Checks.required(event, "event");
            if (!user.equals(UserKey.of(event))) {
                throw new IllegalArgumentException("session events belong to different users");
            }
            if (event.getTimestamp() < previous) {
                throw new IllegalArgumentException("session events must be ordered");
            }
            previous = event.getTimestamp();
        }
        this.events = Collections.unmodifiableList(new ArrayList<Event>(events));
    }

    public UserKey getUser() {
        return user;
    }

    public String getId() {
        return events.get(0).getEventId();
    }

    public List<Event> getEvents() {
        return events;
    }

    public long getStart() {
        return events.get(0).getTimestamp();
    }

    public long getEnd() {
        return events.get(events.size() - 1).getTimestamp();
    }

    public long getDuration() {
        return getEnd() - getStart();
    }

    public int getEventCount() {
        return events.size();
    }

    public boolean isBounce() {
        return events.size() == 1;
    }
}
