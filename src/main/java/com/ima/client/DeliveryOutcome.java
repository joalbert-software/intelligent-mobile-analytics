package com.ima.client;

public final class DeliveryOutcome {
    private final String eventId;
    private final String status;
    private final String error;

    public DeliveryOutcome(String eventId, String status, String error) {
        this.eventId = eventId;
        this.status = status;
        this.error = error;
    }

    public String getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public boolean isAccepted() {
        return "CREATED".equals(status) || "DUPLICATE".equals(status);
    }

    public boolean isRetryable() {
        return "UNAVAILABLE".equals(status);
    }
}
