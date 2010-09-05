package com.ima.service;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.Platform;
import java.util.LinkedHashMap;
import java.util.Map;

/** HTTP input excludes application ID and reception time, which the server supplies. */
public class EventInput {
    private String eventId;
    private String userId;
    private String name;
    private Long timestamp;
    private String platform;
    private String appVersion;
    private Map<String, String> properties = new LinkedHashMap<String, String>();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Event toEvent(String applicationId, long receivedAt) {
        return new Event(applicationId, eventId, userId, name,
                Checks.required(timestamp, "timestamp"), receivedAt,
                Platform.parse(platform), appVersion, properties);
    }
}
