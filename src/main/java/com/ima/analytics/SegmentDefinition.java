package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SegmentDefinition {
    private final List<EventPredicate> predicates;
    private final boolean requireAll;
    private final int minimumEvents;
    private final int maximumEvents;

    public SegmentDefinition(List<EventPredicate> predicates, boolean requireAll,
                              int minimumEvents, int maximumEvents) {
        Checks.bounded(predicates, "predicates", 1, 20);
        for (EventPredicate predicate : predicates) {
            Checks.required(predicate, "predicate");
        }
        this.predicates = Collections.unmodifiableList(new ArrayList<EventPredicate>(predicates));
        this.requireAll = requireAll;
        this.minimumEvents = Checks.range(minimumEvents, "minimumEvents", 1, 1000000);
        this.maximumEvents = Checks.range(maximumEvents, "maximumEvents", minimumEvents, 1000000);
    }

    public List<EventPredicate> getPredicates() {
        return predicates;
    }

    public boolean isRequireAll() {
        return requireAll;
    }

    public int getMinimumEvents() {
        return minimumEvents;
    }

    public int getMaximumEvents() {
        return maximumEvents;
    }

    public boolean matchesEvent(Event event) {
        for (EventPredicate predicate : predicates) {
            boolean matches = predicate.matches(event);
            if (requireAll && !matches) {
                return false;
            }
            if (!requireAll && matches) {
                return true;
            }
        }
        return requireAll;
    }
}
