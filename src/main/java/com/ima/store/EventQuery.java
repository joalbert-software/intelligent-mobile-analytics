package com.ima.store;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.Platform;
import com.ima.domain.TimeRange;

public final class EventQuery {
    private final String applicationId;
    private final TimeRange range;
    private final String eventName;
    private final String userId;
    private final Platform platform;
    private final int limit;

    public EventQuery(String applicationId, TimeRange range, String eventName,
                      String userId, Platform platform, int limit) {
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.range = Checks.required(range, "range");
        this.eventName = eventName == null ? null : Checks.identifier(eventName, "eventName");
        this.userId = userId == null ? null : Checks.identifier(userId, "userId");
        this.platform = platform;
        this.limit = Checks.range(limit, "limit", 1, 100001);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public TimeRange getRange() {
        return range;
    }

    public String getEventName() {
        return eventName;
    }

    public String getUserId() {
        return userId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public int getLimit() {
        return limit;
    }

    public boolean matches(Event event) {
        return applicationId.equals(event.getApplicationId())
                && range.contains(event.getTimestamp())
                && (eventName == null || eventName.equals(event.getName()))
                && (userId == null || userId.equals(event.getUserId()))
                && (platform == null || platform == event.getPlatform());
    }
}
