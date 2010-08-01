package com.ima.store;

import com.ima.domain.Event;
import java.util.List;

public interface EventStore {
    SaveResult save(Event event);

    Event find(String applicationId, String eventId);

    List<Event> query(EventQuery query);

    long deleteUser(String applicationId, String userId);

    long deleteBefore(String applicationId, long timestamp);

    boolean isAvailable();
}
