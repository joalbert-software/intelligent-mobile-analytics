package com.ima.security;

import com.ima.domain.Checks;
import java.time.Clock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local limiter for a single-process deployment; not a distributed quota. */
public final class TokenBucketLimiter implements RateLimiter {
    private static final class Bucket {
        private double tokens;
        private long updated;

        private Bucket(double tokens, long updated) {
            this.tokens = tokens;
            this.updated = updated;
        }
    }

    private final int capacity;
    private final double tokensPerMillis;
    private final int maximumApplications;
    private final Clock clock;
    private final Map<String, Bucket> buckets = new LinkedHashMap<String, Bucket>();

    public TokenBucketLimiter(int capacity, int refillPerSecond,
                              int maximumApplications, Clock clock) {
        this.capacity = Checks.range(capacity, "capacity", 1, 1000000);
        this.tokensPerMillis = Checks.range(refillPerSecond, "refillPerSecond", 1, 1000000) / 1000.0;
        this.maximumApplications = Checks.range(maximumApplications, "maximumApplications", 1, 1000000);
        this.clock = Checks.required(clock, "clock");
    }

    @Override
    public synchronized RateDecision acquire(String applicationId, int cost) {
        Checks.identifier(applicationId, "applicationId");
        Checks.range(cost, "cost", 1, capacity);
        long now = clock.millis();
        Bucket bucket = buckets.get(applicationId);
        if (bucket == null) {
            removeIdle(now);
            if (buckets.size() >= maximumApplications) {
                return new RateDecision(false, 0, 1000);
            }
            bucket = new Bucket(capacity, now);
            buckets.put(applicationId, bucket);
        }
        long elapsed = Math.max(0, now - bucket.updated);
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * tokensPerMillis);
        bucket.updated = Math.max(bucket.updated, now);
        if (bucket.tokens >= cost) {
            bucket.tokens -= cost;
            return new RateDecision(true, (long) bucket.tokens, 0);
        }
        return new RateDecision(false, (long) bucket.tokens,
                (long) Math.ceil((cost - bucket.tokens) / tokensPerMillis));
    }

    private void removeIdle(long now) {
        long refillTime = (long) Math.ceil(capacity / tokensPerMillis);
        Iterator<Bucket> iterator = buckets.values().iterator();
        while (iterator.hasNext()) {
            Bucket bucket = iterator.next();
            if (now >= bucket.updated && now - bucket.updated >= refillTime) {
                iterator.remove();
            }
        }
    }
}
