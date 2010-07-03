package com.ima.domain;

import java.util.Objects;

/** A tuple, rather than delimiter concatenation, avoids ambiguous keys. */
public final class EventKey {
    private final String applicationId;
    private final String eventId;

    public EventKey(String applicationId, String eventId) {
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.eventId = Checks.identifier(eventId, "eventId");
    }

    public static EventKey of(Event event) {
        return new EventKey(event.getApplicationId(), event.getEventId());
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getEventId() {
        return eventId;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EventKey)) {
            return false;
        }
        EventKey key = (EventKey) other;
        return applicationId.equals(key.applicationId) && eventId.equals(key.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, eventId);
    }
}
