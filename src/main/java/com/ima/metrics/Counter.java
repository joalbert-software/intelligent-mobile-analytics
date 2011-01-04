package com.ima.metrics;

import java.util.concurrent.atomic.AtomicLong;

public final class Counter {
    private final AtomicLong value = new AtomicLong();

    public void increment() {
        add(1);
    }

    public void add(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("counter increments must be nonnegative");
        }
        while (true) {
            long current = value.get();
            long next = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
            if (value.compareAndSet(current, next)) {
                return;
            }
        }
    }

    public long get() {
        return value.get();
    }
}
