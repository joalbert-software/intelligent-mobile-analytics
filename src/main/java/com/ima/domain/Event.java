package com.ima.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Validated event; no mutable caller-owned values are retained. */
public final class Event {
    public static final int MAX_PROPERTIES = 32;
    public static final int MAX_PROPERTY_VALUE = 512;

    private final String applicationId;
    private final String eventId;
    private final String userId;
    private final String name;
    private final long timestamp;
    private final long receivedAt;
    private final Platform platform;
    private final String appVersion;
    private final Map<String, String> properties;

    public Event(String applicationId, String eventId, String userId,
                 String name, long timestamp, long receivedAt,
                 Platform platform, String appVersion,
                 Map<String, String> properties) {
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.eventId = Checks.identifier(eventId, "eventId");
        this.userId = Checks.identifier(userId, "userId");
        this.name = Checks.identifier(name, "name");
        this.timestamp = Checks.nonnegative(timestamp, "timestamp");
        this.receivedAt = Checks.nonnegative(receivedAt, "receivedAt");
        this.platform = Checks.required(platform, "platform");
        this.appVersion = Checks.text(appVersion, "appVersion", 64);
        Checks.required(properties, "properties");
        Checks.range(properties.size(), "property count", 0, MAX_PROPERTIES);
        Map<String, String> copy = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String key = Checks.identifier(property.getKey(), "property key");
            if (key.indexOf('.') >= 0) {
                throw new IllegalArgumentException("property keys must not contain dots");
            }
            String value = Checks.required(property.getValue(), "property value");
            if (value.length() > MAX_PROPERTY_VALUE) {
                throw new IllegalArgumentException("property value is too long");
            }
            copy.put(key, value);
        }
        this.properties = Collections.unmodifiableMap(copy);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String property(String key) {
        return properties.get(key);
    }

    public boolean samePayload(Event other) {
        return other != null
                && applicationId.equals(other.applicationId)
                && eventId.equals(other.eventId)
                && userId.equals(other.userId)
                && name.equals(other.name)
                && timestamp == other.timestamp
                && platform == other.platform
                && appVersion.equals(other.appVersion)
                && properties.equals(other.properties);
    }
}
