package com.ima.service;

public final class IngestionItem {
    private final int index;
    private final String eventId;
    private final String status;
    private final String error;

    public IngestionItem(int index, String eventId, String status, String error) {
        this.index = index;
        this.eventId = eventId;
        this.status = status;
        this.error = error;
    }

    public int getIndex() {
        return index;
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
}
