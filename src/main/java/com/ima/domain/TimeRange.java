package com.ima.domain;

import java.util.Objects;

/** Half-open UTC range, expressed in milliseconds since the Unix epoch. */
public final class TimeRange {
    private final long start;
    private final long end;

    public TimeRange(long start, long end) {
        this.start = Checks.nonnegative(start, "start");
        if (end <= start) {
            throw new IllegalArgumentException("end must be after start");
        }
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public long duration() {
        return end - start;
    }

    public boolean contains(long timestamp) {
        return start <= timestamp && timestamp < end;
    }

    public boolean overlaps(TimeRange other) {
        Checks.required(other, "range");
        return start < other.end && other.start < end;
    }

    public TimeRange intersection(TimeRange other) {
        if (!overlaps(other)) {
            return null;
        }
        return new TimeRange(Math.max(start, other.start), Math.min(end, other.end));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TimeRange)) {
            return false;
        }
        TimeRange range = (TimeRange) other;
        return start == range.start && end == range.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }
}
