package com.ima.analytics;

import com.ima.domain.Event;
import com.ima.domain.TimeRange;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Distribution over active users; dormant users require a separate population. */
public final class EngagementCalculator {
    public List<EngagementRow> calculate(Collection<Event> events, TimeRange range) {
        Map<UserKey, Set<Long>> days = new HashMap<UserKey, Set<Long>>();
        for (Event event : EventOrdering.unique(events)) {
            if (!range.contains(event.getTimestamp())) {
                continue;
            }
            UserKey user = UserKey.of(event);
            Set<Long> active = days.get(user);
            if (active == null) {
                active = new HashSet<Long>();
                days.put(user, active);
            }
            active.add(UtcBuckets.day(event.getTimestamp()));
        }
        Map<Integer, Long> histogram = new TreeMap<Integer, Long>();
        for (Set<Long> active : days.values()) {
            Long previous = histogram.get(active.size());
            histogram.put(active.size(), previous == null ? 1 : previous + 1);
        }
        List<EngagementRow> rows = new ArrayList<EngagementRow>();
        for (Map.Entry<Integer, Long> entry : histogram.entrySet()) {
            rows.add(new EngagementRow(entry.getKey(), entry.getValue()));
        }
        return rows;
    }
}
