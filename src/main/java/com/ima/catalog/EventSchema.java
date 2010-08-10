package com.ima.catalog;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EventSchema {
    private final String applicationId;
    private final String eventName;
    private final int version;
    private final boolean allowUnknown;
    private final Map<String, PropertyRule> rules;

    public EventSchema(String applicationId, String eventName, int version,
                        boolean allowUnknown, Collection<PropertyRule> rules) {
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.eventName = Checks.identifier(eventName, "eventName");
        this.version = Checks.range(version, "version", 1, 1000000);
        this.allowUnknown = allowUnknown;
        Checks.bounded(rules, "rules", 0, 32);
        Map<String, PropertyRule> byName = new LinkedHashMap<String, PropertyRule>();
        for (PropertyRule rule : rules) {
            Checks.required(rule, "rule");
            if (byName.put(rule.getName(), rule) != null) {
                throw new IllegalArgumentException("duplicate property rule");
            }
        }
        this.rules = Collections.unmodifiableMap(byName);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getEventName() {
        return eventName;
    }

    public int getVersion() {
        return version;
    }

    public Map<String, PropertyRule> getRules() {
        return rules;
    }

    public void validate(Event event) {
        if (!applicationId.equals(event.getApplicationId()) || !eventName.equals(event.getName())) {
            throw new IllegalArgumentException("event does not match schema identity");
        }
        for (PropertyRule rule : rules.values()) {
            rule.validate(event.property(rule.getName()));
        }
        if (!allowUnknown) {
            for (String name : event.getProperties().keySet()) {
                if (!rules.containsKey(name)) {
                    throw new IllegalArgumentException("event contains an undeclared property");
                }
            }
        }
    }

    public Map<String, String> publicProperties(Event event) {
        validate(event);
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> property : event.getProperties().entrySet()) {
            PropertyRule rule = rules.get(property.getKey());
            if (rule != null && !rule.isSensitive()) {
                result.put(property.getKey(), property.getValue());
            }
        }
        return result;
    }
}
