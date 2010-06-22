package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class SegmentCalculator {
    public List<SegmentMember> calculate(Collection<Event> events, SegmentDefinition definition) {
        Checks.required(definition, "definition");
        List<SegmentMember> members = new ArrayList<SegmentMember>();
        for (Map.Entry<UserKey, List<Event>> entry : EventOrdering.byUser(events).entrySet()) {
            int matches = 0;
            long first = -1;
            long last = -1;
            for (Event event : entry.getValue()) {
                if (definition.matchesEvent(event)) {
                    matches++;
                    if (first < 0) {
                        first = event.getTimestamp();
                    }
                    last = event.getTimestamp();
                }
            }
            if (matches >= definition.getMinimumEvents() && matches <= definition.getMaximumEvents()) {
                members.add(new SegmentMember(entry.getKey(), matches, first, last));
            }
        }
        return members;
    }
}
