package com.ima.analytics;

import com.ima.domain.Event;
import com.ima.domain.Session;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

public final class PathCalculator {
    public Map<PathEdge, Long> calculate(Collection<Event> events, long sessionGap,
                                         boolean collapseRepeats) {
        Map<PathEdge, Long> edges = new TreeMap<PathEdge, Long>();
        for (Session session : new Sessionizer(sessionGap).calculate(events)) {
            String previous = null;
            for (Event event : session.getEvents()) {
                String next = event.getName();
                if (previous != null && (!collapseRepeats || !previous.equals(next))) {
                    PathEdge edge = new PathEdge(previous, next);
                    Long count = edges.get(edge);
                    edges.put(edge, count == null ? 1 : count + 1);
                }
                previous = next;
            }
        }
        return edges;
    }
}
