package com.ima.metrics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Process-level metrics carry no tenant IDs, user IDs, or dynamic route labels. */
public final class RequestMetrics {
    private static final RequestMetrics INSTANCE = new RequestMetrics();
    private final Counter requests = new Counter();
    private final Counter successful = new Counter();
    private final Counter clientErrors = new Counter();
    private final Counter serverErrors = new Counter();
    private final AtomicInteger active = new AtomicInteger();
    private final Histogram durationMicros = new Histogram(new long[] {
        1000, 5000, 10000, 25000, 50000, 100000, 250000, 500000, 1000000, 5000000
    });

    public static RequestMetrics instance() {
        return INSTANCE;
    }

    public void started() {
        active.incrementAndGet();
    }

    public void completed(int status, long elapsedMicros) {
        active.decrementAndGet();
        requests.increment();
        if (status >= 500) {
            serverErrors.increment();
        } else if (status >= 400) {
            clientErrors.increment();
        } else {
            successful.increment();
        }
        durationMicros.observe(Math.max(0, elapsedMicros));
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("requests", requests.get());
        result.put("successful", successful.get());
        result.put("clientErrors", clientErrors.get());
        result.put("serverErrors", serverErrors.get());
        result.put("active", active.get());
        result.put("durationMicros", durationMicros.snapshot());
        return result;
    }
}
