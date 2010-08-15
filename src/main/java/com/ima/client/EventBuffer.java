package com.ima.client;

import com.ima.domain.Checks;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/** Bounded process-local queue; callers can persist events before enqueueing. */
public final class EventBuffer {
    private final int capacity;
    private final Map<String, ClientEvent> pending = new LinkedHashMap<String, ClientEvent>();
    private final ReentrantLock flushLock = new ReentrantLock();

    public EventBuffer(int capacity) {
        this.capacity = Checks.range(capacity, "capacity", 1, 100000);
    }

    public synchronized boolean offer(ClientEvent event) {
        Checks.required(event, "event");
        if (pending.containsKey(event.getEventId())) {
            throw new IllegalArgumentException("event ID already queued");
        }
        if (pending.size() >= capacity) {
            return false;
        }
        pending.put(event.getEventId(), event);
        return true;
    }

    public synchronized int size() {
        return pending.size();
    }

    public List<DeliveryOutcome> flush(AnalyticsClient client, int batchSize)
            throws IOException, InterruptedException {
        Checks.range(batchSize, "batchSize", 1, 500);
        flushLock.lockInterruptibly();
        try {
            List<ClientEvent> batch = new ArrayList<ClientEvent>();
            synchronized (this) {
                for (ClientEvent event : pending.values()) {
                    batch.add(event);
                    if (batch.size() == batchSize) {
                        break;
                    }
                }
            }
            if (batch.isEmpty()) {
                return new ArrayList<DeliveryOutcome>();
            }
            List<DeliveryOutcome> result = client.send(batch);
            synchronized (this) {
                for (DeliveryOutcome outcome : result) {
                    if (!outcome.isRetryable()) {
                        pending.remove(outcome.getEventId());
                    }
                }
            }
            return result;
        } finally {
            flushLock.unlock();
        }
    }
}
