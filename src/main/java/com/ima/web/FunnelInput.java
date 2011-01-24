package com.ima.web;

import com.ima.analytics.FunnelDefinition;
import com.ima.domain.Checks;
import com.ima.domain.TimeRange;
import java.util.List;

public class FunnelInput {
    private Long start;
    private Long end;
    private Long windowMillis;
    private List<String> steps;

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Long getWindowMillis() {
        return windowMillis;
    }

    public void setWindowMillis(Long windowMillis) {
        this.windowMillis = windowMillis;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public TimeRange range() {
        return new TimeRange(Checks.required(start, "start"), Checks.required(end, "end"));
    }

    public FunnelDefinition definition() {
        return new FunnelDefinition(steps, Checks.required(windowMillis, "windowMillis"));
    }
}
