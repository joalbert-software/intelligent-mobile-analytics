package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Ordered, per-user funnel. One event may satisfy at most one step. */
public final class FunnelCalculator {
    public FunnelResult calculate(Collection<Event> events, FunnelDefinition definition) {
        Checks.required(definition, "definition");
        int stepCount = definition.getSteps().size();
        long[] counts = new long[stepCount];
        for (List<Event> userEvents : EventOrdering.byUser(events).values()) {
            long[] latestStart = new long[stepCount];
            Arrays.fill(latestStart, -1);
            int highest = -1;
            for (Event event : userEvents) {
                // Reverse traversal prevents a single repeated-name event advancing twice.
                for (int step = stepCount - 1; step >= 1; step--) {
                    if (event.getName().equals(definition.getSteps().get(step))
                            && latestStart[step - 1] >= 0
                            && event.getTimestamp() - latestStart[step - 1]
                            <= definition.getWindowMillis()) {
                        latestStart[step] = Math.max(latestStart[step], latestStart[step - 1]);
                        highest = Math.max(highest, step);
                    }
                }
                if (event.getName().equals(definition.getSteps().get(0))) {
                    latestStart[0] = event.getTimestamp();
                    highest = Math.max(highest, 0);
                }
            }
            for (int step = 0; step <= highest; step++) {
                counts[step]++;
            }
        }
        return new FunnelResult(definition, counts);
    }
}
