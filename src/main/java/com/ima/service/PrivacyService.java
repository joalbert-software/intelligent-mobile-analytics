package com.ima.service;

import com.ima.domain.Checks;
import com.ima.security.Principal;
import com.ima.security.Scope;
import com.ima.store.EventStore;

public final class PrivacyService {
    private final EventStore events;

    public PrivacyService(EventStore events) {
        this.events = Checks.required(events, "events");
    }

    public long deleteUser(Principal principal, String applicationId, String userId) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.identifier(userId, "userId");
        // Cached aggregates expire separately; derived offline datasets need rebuilding.
        return events.deleteUser(applicationId, userId);
    }

    public long expire(Principal principal, String applicationId, long before) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.nonnegative(before, "before");
        return events.deleteBefore(applicationId, before);
    }
}
