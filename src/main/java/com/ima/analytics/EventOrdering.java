package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.EventKey;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EventOrdering {
    public static final Comparator<Event> BY_TIME = new Comparator<Event>() {
        @Override
        public int compare(Event first, Event second) {
            int timeOrder = Long.compare(first.getTimestamp(), second.getTimestamp());
            return timeOrder != 0 ? timeOrder : first.getEventId().compareTo(second.getEventId());
        }
    };

    private EventOrdering() {
    }

    public static List<Event> unique(Collection<Event> events) {
        Checks.required(events, "events");
        Map<EventKey, Event> byId = new LinkedHashMap<EventKey, Event>();
        for (Event event : events) {
            Checks.required(event, "event");
            EventKey key = EventKey.of(event);
            Event previous = byId.get(key);
            if (previous != null && !previous.samePayload(event)) {
                throw new IllegalArgumentException("conflicting event identity");
            }
            if (previous == null) {
                byId.put(key, event);
            }
        }
        return new ArrayList<Event>(byId.values());
    }

    public static Map<UserKey, List<Event>> byUser(Collection<Event> events) {
        Map<UserKey, List<Event>> groups = new TreeMap<UserKey, List<Event>>();
        for (Event event : unique(events)) {
            UserKey user = UserKey.of(event);
            List<Event> group = groups.get(user);
            if (group == null) {
                group = new ArrayList<Event>();
                groups.put(user, group);
            }
            group.add(event);
        }
        for (List<Event> group : groups.values()) {
            Collections.sort(group, BY_TIME);
        }
        return groups;
    }
}
