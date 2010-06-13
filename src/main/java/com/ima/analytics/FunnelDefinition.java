package com.ima.analytics;

import com.ima.domain.Checks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FunnelDefinition {
    private final List<String> steps;
    private final long windowMillis;

    public FunnelDefinition(List<String> steps, long windowMillis) {
        Checks.bounded(steps, "steps", 2, 20);
        for (String step : steps) {
            Checks.identifier(step, "step");
        }
        this.steps = Collections.unmodifiableList(new ArrayList<String>(steps));
        this.windowMillis = Checks.positive(windowMillis, "windowMillis");
    }

    public List<String> getSteps() {
        return steps;
    }

    public long getWindowMillis() {
        return windowMillis;
    }
}
