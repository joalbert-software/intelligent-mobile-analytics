package com.ima.client;

import com.ima.domain.Checks;
import java.util.Random;

public final class RetryPolicy {
    private final long initialDelay;
    private final long maximumDelay;
    private final Random random;

    public RetryPolicy(long initialDelay, long maximumDelay, Random random) {
        this.initialDelay = Checks.positive(initialDelay, "initialDelay");
        this.maximumDelay = Checks.positive(maximumDelay, "maximumDelay");
        if (initialDelay > maximumDelay) {
            throw new IllegalArgumentException("initial delay exceeds maximum delay");
        }
        this.random = Checks.required(random, "random");
    }

    public synchronized long delay(int attempt, long serverHint) {
        Checks.range(attempt, "attempt", 0, 30);
        if (serverHint >= 0) {
            return Math.min(maximumDelay, serverHint);
        }
        long ceiling = initialDelay;
        for (int i = 0; i < attempt; i++) {
            ceiling = ceiling > maximumDelay / 2 ? maximumDelay : Math.min(maximumDelay, ceiling * 2);
        }
        return (long) (random.nextDouble() * ceiling);
    }

    public static long parseRetryAfter(String value) {
        if (value == null) {
            return -1;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds < 0 ? -1 : Math.min(seconds, 300) * 1000;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
