package com.ima.client;

import com.ima.domain.Event;
import com.ima.domain.Platform;
import java.util.Map;
import java.util.UUID;

/** Immutable client payload. Reuse the same event ID on every retry. */
public final class ClientEvent {
    private final Event validated;

    public ClientEvent(String eventId, String userId, String name, long timestamp,
                       Platform platform, String appVersion, Map<String, String> properties) {
        this.validated = new Event("client", eventId, userId, name, timestamp,
                timestamp, platform, appVersion, properties);
    }

    public static ClientEvent create(String userId, String name, long timestamp,
                                     Platform platform, String appVersion, Map<String, String> properties) {
        return new ClientEvent(UUID.randomUUID().toString(), userId, name,
                timestamp, platform, appVersion, properties);
    }

    public String getEventId() {
        return validated.getEventId();
    }

    public String getUserId() {
        return validated.getUserId();
    }

    public String getName() {
        return validated.getName();
    }

    public long getTimestamp() {
        return validated.getTimestamp();
    }

    public String getPlatform() {
        return validated.getPlatform().name();
    }

    public String getAppVersion() {
        return validated.getAppVersion();
    }

    public Map<String, String> getProperties() {
        return validated.getProperties();
    }
}
