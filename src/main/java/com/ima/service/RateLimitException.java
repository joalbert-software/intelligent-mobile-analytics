package com.ima.service;

public final class RateLimitException extends RuntimeException {
    private final long retryAfterMillis;

    public RateLimitException(long retryAfterMillis) {
        super("application ingestion limit exceeded");
        this.retryAfterMillis = retryAfterMillis;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
