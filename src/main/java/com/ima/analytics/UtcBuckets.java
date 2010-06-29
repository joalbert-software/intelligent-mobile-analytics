package com.ima.analytics;

import com.ima.domain.Checks;

/** Fixed UTC buckets; reporting does not depend on the host timezone. */
public final class UtcBuckets {
    public static final long MINUTE = 60000L;
    public static final long HOUR = 60L * MINUTE;
    public static final long DAY = 24L * HOUR;

    private UtcBuckets() {
    }

    public static long floor(long timestamp, long width) {
        Checks.nonnegative(timestamp, "timestamp");
        Checks.positive(width, "width");
        return timestamp - timestamp % width;
    }

    public static long day(long timestamp) {
        return floor(timestamp, DAY);
    }

    public static int daysBetween(long first, long second) {
        long difference = (day(second) - day(first)) / DAY;
        if (difference < Integer.MIN_VALUE || difference > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("day distance exceeds supported range");
        }
        return (int) difference;
    }
}
