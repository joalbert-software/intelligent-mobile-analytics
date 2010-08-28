package com.ima.security;

public final class RateDecision {
    private final boolean allowed;
    private final long remaining;
    private final long retryAfterMillis;

    public RateDecision(boolean allowed, long remaining, long retryAfterMillis) {
        this.allowed = allowed;
        this.remaining = remaining;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
