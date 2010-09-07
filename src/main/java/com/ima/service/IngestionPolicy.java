package com.ima.service;

import com.ima.analytics.UtcBuckets;
import com.ima.domain.Checks;
import com.ima.domain.Event;

public final class IngestionPolicy {
    private final long maximumAge;
    private final long futureTolerance;
    private final int maximumBatch;

    public IngestionPolicy(long maximumAge, long futureTolerance, int maximumBatch) {
        this.maximumAge = Checks.positive(maximumAge, "maximumAge");
        this.futureTolerance = Checks.nonnegative(futureTolerance, "futureTolerance");
        this.maximumBatch = Checks.range(maximumBatch, "maximumBatch", 1, 1000);
    }

    public static IngestionPolicy defaults() {
        return new IngestionPolicy(30 * UtcBuckets.DAY, 5 * UtcBuckets.MINUTE, 500);
    }

    public int getMaximumBatch() {
        return maximumBatch;
    }

    public void validate(Event event, long now) {
        if (event.getTimestamp() > now && event.getTimestamp() - now > futureTolerance) {
            throw new IllegalArgumentException("event timestamp is too far in the future");
        }
        if (now > event.getTimestamp() && now - event.getTimestamp() > maximumAge) {
            throw new IllegalArgumentException("event timestamp is outside the ingestion window");
        }
        if ("purchase".equals(event.getName()) || "refund".equals(event.getName())) {
            String amount = event.property("amount_minor");
            if (amount == null || !amount.matches("[0-9]{1,18}")) {
                throw new IllegalArgumentException("revenue requires nonnegative integer amount_minor");
            }
            new com.ima.domain.Money(Long.parseLong(amount), event.property("currency"));
        }
    }
}
