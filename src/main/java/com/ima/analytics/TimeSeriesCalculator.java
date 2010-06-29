package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.TimeRange;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TimeSeriesCalculator {
    public List<MetricPoint> calculate(Collection<Event> events, TimeRange range, long width) {
        Checks.required(range, "range");
        Checks.positive(width, "width");
        long first = UtcBuckets.floor(range.getStart(), width);
        long last = UtcBuckets.floor(range.getEnd() - 1, width);
        long buckets = (last - first) / width + 1;
        if (buckets > 10000) {
            throw new IllegalArgumentException("too many time buckets");
        }
        Map<Long, Long> counts = new HashMap<Long, Long>();
        Map<Long, Set<UserKey>> users = new HashMap<Long, Set<UserKey>>();
        for (Event event : EventOrdering.unique(events)) {
            if (!range.contains(event.getTimestamp())) {
                continue;
            }
            long bucket = UtcBuckets.floor(event.getTimestamp(), width);
            Long previous = counts.get(bucket);
            counts.put(bucket, previous == null ? 1 : previous + 1);
            Set<UserKey> active = users.get(bucket);
            if (active == null) {
                active = new HashSet<UserKey>();
                users.put(bucket, active);
            }
            active.add(UserKey.of(event));
        }
        List<MetricPoint> result = new ArrayList<MetricPoint>();
        for (long index = 0; index < buckets; index++) {
            long bucket = first + index * width;
            result.add(new MetricPoint(bucket,
                    counts.containsKey(bucket) ? counts.get(bucket) : 0,
                    users.containsKey(bucket) ? users.get(bucket).size() : 0));
        }
        return result;
    }
}
