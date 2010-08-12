package com.ima.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SchemaCompatibility {
    public List<String> breakingChanges(EventSchema previous, EventSchema replacement) {
        List<String> changes = new ArrayList<String>();
        if (!previous.getApplicationId().equals(replacement.getApplicationId())
                || !previous.getEventName().equals(replacement.getEventName())) {
            changes.add("schema_identity_changed");
            return changes;
        }
        if (replacement.getVersion() <= previous.getVersion()) {
            changes.add("schema_version_did_not_increase");
        }
        for (Map.Entry<String, PropertyRule> entry : previous.getRules().entrySet()) {
            PropertyRule before = entry.getValue();
            PropertyRule after = replacement.getRules().get(entry.getKey());
            if (after == null) {
                changes.add("property_removed:" + entry.getKey());
                continue;
            }
            if (before.getType() != after.getType()) {
                changes.add("property_type_changed:" + entry.getKey());
            }
            if (!before.isRequired() && after.isRequired()) {
                changes.add("property_became_required:" + entry.getKey());
            }
            if (before.getType() == PropertyType.ENUMERATION
                    && !after.getAllowedValues().containsAll(before.getAllowedValues())) {
                changes.add("enumeration_narrowed:" + entry.getKey());
            }
            if (before.isSensitive() && !after.isSensitive()) {
                changes.add("property_became_public:" + entry.getKey());
            }
        }
        for (PropertyRule rule : replacement.getRules().values()) {
            if (rule.isRequired() && !previous.getRules().containsKey(rule.getName())) {
                changes.add("new_required_property:" + rule.getName());
            }
        }
        return changes;
    }
}
