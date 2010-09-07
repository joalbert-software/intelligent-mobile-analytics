package com.ima.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IngestionResult {
    private final List<IngestionItem> items;

    public IngestionResult(List<IngestionItem> items) {
        this.items = Collections.unmodifiableList(new ArrayList<IngestionItem>(items));
    }

    public List<IngestionItem> getItems() {
        return items;
    }

    public int getAccepted() {
        int accepted = 0;
        for (IngestionItem item : items) {
            if (item.isAccepted()) {
                accepted++;
            }
        }
        return accepted;
    }

    public int getRejected() {
        return items.size() - getAccepted();
    }

    public boolean isRetryable() {
        for (IngestionItem item : items) {
            if ("UNAVAILABLE".equals(item.getStatus())) {
                return true;
            }
        }
        return false;
    }
}
