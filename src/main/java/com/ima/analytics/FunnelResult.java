package com.ima.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FunnelResult {
    private final List<String> steps;
    private final List<Long> users;

    public FunnelResult(FunnelDefinition definition, long[] users) {
        if (users.length != definition.getSteps().size()) {
            throw new IllegalArgumentException("step count mismatch");
        }
        this.steps = definition.getSteps();
        List<Long> counts = new ArrayList<Long>();
        long previous = Long.MAX_VALUE;
        for (long count : users) {
            if (count < 0 || count > previous) {
                throw new IllegalArgumentException("funnel counts must be nonincreasing");
            }
            counts.add(count);
            previous = count;
        }
        this.users = Collections.unmodifiableList(counts);
    }

    public List<String> getSteps() {
        return steps;
    }

    public List<Long> getUsers() {
        return users;
    }

    public double getConversionRate() {
        return users.get(0) == 0 ? 0.0 : (double) users.get(users.size() - 1) / users.get(0);
    }
}
