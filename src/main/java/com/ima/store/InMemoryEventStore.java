package com.ima.store;

import com.ima.analytics.EventOrdering;
import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.EventKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit demo adapter; data is lost when the process exits. */
public final class InMemoryEventStore implements EventStore {
    private final Map<EventKey, Event> events = new LinkedHashMap<EventKey, Event>();
    private final int capacity;

    public InMemoryEventStore(int capacity) {
        this.capacity = Checks.range(capacity, "capacity", 1, 1000000);
    }

    @Override
    public synchronized SaveResult save(Event event) {
        Checks.required(event, "event");
        EventKey key = EventKey.of(event);
        Event previous = events.get(key);
        if (previous != null) {
            return previous.samePayload(event) ? SaveResult.DUPLICATE : SaveResult.CONFLICT;
        }
        if (events.size() >= capacity) {
            throw new StoreUnavailableException("demo event capacity reached");
        }
        events.put(key, event);
        return SaveResult.CREATED;
    }

    @Override
    public synchronized Event find(String applicationId, String eventId) {
        return events.get(new EventKey(applicationId, eventId));
    }

    @Override
    public synchronized List<Event> query(EventQuery query) {
        List<Event> result = new ArrayList<Event>();
        for (Event event : events.values()) {
            if (query.matches(event)) {
                result.add(event);
            }
        }
        Collections.sort(result, EventOrdering.BY_TIME);
        return new ArrayList<Event>(result.subList(0, Math.min(result.size(), query.getLimit())));
    }

    @Override
    public synchronized long deleteUser(String applicationId, String userId) {
        Checks.identifier(applicationId, "applicationId");
        Checks.identifier(userId, "userId");
        long removed = 0;
        Iterator<Event> iterator = events.values().iterator();
        while (iterator.hasNext()) {
            Event event = iterator.next();
            if (applicationId.equals(event.getApplicationId()) && userId.equals(event.getUserId())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    @Override
    public synchronized long deleteBefore(String applicationId, long timestamp) {
        Checks.identifier(applicationId, "applicationId");
        Checks.nonnegative(timestamp, "timestamp");
        long removed = 0;
        Iterator<Event> iterator = events.values().iterator();
        while (iterator.hasNext()) {
            Event event = iterator.next();
            if (applicationId.equals(event.getApplicationId()) && event.getTimestamp() < timestamp) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
