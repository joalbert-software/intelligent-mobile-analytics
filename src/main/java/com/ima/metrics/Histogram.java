package com.ima.metrics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fixed buckets avoid unbounded cardinality and raw-value retention. */
public final class Histogram {
    private final long[] upperBounds;
    private final long[] counts;
    private long count;
    private double total;
    private long minimum = Long.MAX_VALUE;
    private long maximum;

    public Histogram(long[] upperBounds) {
        if (upperBounds == null || upperBounds.length == 0 || upperBounds.length > 100) {
            throw new IllegalArgumentException("histogram requires 1 to 100 bounds");
        }
        this.upperBounds = upperBounds.clone();
        long previous = -1;
        for (long bound : upperBounds) {
            if (bound <= previous) {
                throw new IllegalArgumentException("histogram bounds must strictly increase");
            }
            previous = bound;
        }
        this.counts = new long[upperBounds.length + 1];
    }

    public synchronized void observe(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("histogram observations must be nonnegative");
        }
        int bucket = upperBounds.length;
        for (int index = 0; index < upperBounds.length; index++) {
            if (value <= upperBounds[index]) {
                bucket = index;
                break;
            }
        }
        counts[bucket]++;
        count++;
        total += value;
        minimum = Math.min(minimum, value);
        maximum = Math.max(maximum, value);
    }

    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        List<Map<String, Object>> buckets = new ArrayList<Map<String, Object>>();
        long cumulative = 0;
        for (int index = 0; index < counts.length; index++) {
            cumulative += counts[index];
            Map<String, Object> bucket = new LinkedHashMap<String, Object>();
            bucket.put("upperBound", index == upperBounds.length ? null : upperBounds[index]);
            bucket.put("count", counts[index]);
            bucket.put("cumulativeCount", cumulative);
            buckets.add(bucket);
        }
        result.put("count", count);
        result.put("sum", total);
        result.put("minimum", count == 0 ? null : minimum);
        result.put("maximum", count == 0 ? null : maximum);
        result.put("mean", count == 0 ? null : total / count);
        result.put("buckets", buckets);
        return result;
    }
}
