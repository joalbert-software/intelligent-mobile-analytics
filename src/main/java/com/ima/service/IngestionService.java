package com.ima.service;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.security.Principal;
import com.ima.security.RateDecision;
import com.ima.security.RateLimiter;
import com.ima.security.Scope;
import com.ima.store.EventStore;
import com.ima.store.SaveResult;
import com.ima.store.StoreUnavailableException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Batches are explicitly partial-success; item-level retries are idempotent. */
public final class IngestionService {
    private final EventStore store;
    private final IngestionPolicy policy;
    private final RateLimiter limiter;
    private final Clock clock;

    public IngestionService(EventStore store, IngestionPolicy policy, RateLimiter limiter, Clock clock) {
        this.store = Checks.required(store, "store");
        this.policy = Checks.required(policy, "policy");
        this.limiter = Checks.required(limiter, "limiter");
        this.clock = Checks.required(clock, "clock");
    }

    public IngestionResult ingest(Principal principal, String applicationId, List<EventInput> inputs) {
        principal.require(applicationId, Scope.INGEST);
        Checks.bounded(inputs, "events", 1, policy.getMaximumBatch());
        RateDecision decision = limiter.acquire(applicationId, inputs.size());
        if (!decision.isAllowed()) {
            throw new RateLimitException(decision.getRetryAfterMillis());
        }
        long now = clock.millis();
        List<IngestionItem> results = new ArrayList<IngestionItem>();
        for (int index = 0; index < inputs.size(); index++) {
            EventInput input = inputs.get(index);
            String eventId = input == null ? null : input.getEventId();
            try {
                Checks.required(input, "event");
                Event event = input.toEvent(applicationId, now);
                policy.validate(event, now);
                SaveResult saved = store.save(event);
                results.add(new IngestionItem(index, eventId, saved.name(),
                        saved == SaveResult.CONFLICT ? "event_id_conflict" : null));
            } catch (IllegalArgumentException failure) {
                results.add(new IngestionItem(index, eventId, "INVALID", failure.getMessage()));
            } catch (StoreUnavailableException failure) {
                results.add(new IngestionItem(index, eventId, "UNAVAILABLE", "storage_unavailable"));
            }
        }
        return new IngestionResult(results);
    }
}
