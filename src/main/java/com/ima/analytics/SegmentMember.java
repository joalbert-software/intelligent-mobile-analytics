package com.ima.analytics;

import com.ima.domain.UserKey;

public final class SegmentMember {
    private final UserKey user;
    private final int matchingEvents;
    private final long firstMatch;
    private final long lastMatch;

    public SegmentMember(UserKey user, int matchingEvents, long firstMatch, long lastMatch) {
        this.user = user;
        this.matchingEvents = matchingEvents;
        this.firstMatch = firstMatch;
        this.lastMatch = lastMatch;
    }

    public UserKey getUser() {
        return user;
    }

    public int getMatchingEvents() {
        return matchingEvents;
    }

    public long getFirstMatch() {
        return firstMatch;
    }

    public long getLastMatch() {
        return lastMatch;
    }
}
