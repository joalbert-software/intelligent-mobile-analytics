package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BreakdownCalculator {
    public List<BreakdownRow> calculate(Collection<Event> events, Dimension dimension, int limit) {
        Checks.required(dimension, "dimension");
        Checks.range(limit, "limit", 1, 1000);
        Map<String, Long> counts = new HashMap<String, Long>();
        Map<String, Set<UserKey>> users = new HashMap<String, Set<UserKey>>();
        for (Event event : EventOrdering.unique(events)) {
            String value = dimension.extract(event);
            Long previous = counts.get(value);
            counts.put(value, previous == null ? 1 : previous + 1);
            Set<UserKey> active = users.get(value);
            if (active == null) {
                active = new HashSet<UserKey>();
                users.put(value, active);
            }
            active.add(UserKey.of(event));
        }
        List<BreakdownRow> rows = new ArrayList<BreakdownRow>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            rows.add(new BreakdownRow(entry.getKey(), entry.getValue(),
                    users.get(entry.getKey()).size()));
        }
        Collections.sort(rows, new Comparator<BreakdownRow>() {
            @Override
            public int compare(BreakdownRow first, BreakdownRow second) {
                int countOrder = Long.compare(second.getEvents(), first.getEvents());
                if (countOrder != 0) {
                    return countOrder;
                }
                if (first.getValue() == null) {
                    return second.getValue() == null ? 0 : -1;
                }
                return second.getValue() == null ? 1 : first.getValue().compareTo(second.getValue());
            }
        });
        return new ArrayList<BreakdownRow>(rows.subList(0, Math.min(limit, rows.size())));
    }
}
