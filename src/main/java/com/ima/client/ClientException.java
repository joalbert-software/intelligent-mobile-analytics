package com.ima.client;

import java.io.IOException;

public final class ClientException extends IOException {
    private final int httpStatus;
    private final boolean retryable;
    private final long retryAfterMillis;

    public ClientException(String message, int httpStatus, boolean retryable, long retryAfterMillis) {
        super(message);
        this.httpStatus = httpStatus;
        this.retryable = retryable;
        this.retryAfterMillis = retryAfterMillis;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
