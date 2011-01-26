package com.ima.web;

import com.ima.domain.Checks;
import com.ima.domain.TimeRange;

public class JobInput {
    private String type;
    private Long start;
    private Long end;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public TimeRange range() {
        return new TimeRange(Checks.required(start, "start"), Checks.required(end, "end"));
    }
}
