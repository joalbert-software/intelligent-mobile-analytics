package com.ima.domain;

import java.util.Objects;

public final class UserKey implements Comparable<UserKey> {
    private final String applicationId;
    private final String userId;

    public UserKey(String applicationId, String userId) {
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.userId = Checks.identifier(userId, "userId");
    }

    public static UserKey of(Event event) {
        return new UserKey(event.getApplicationId(), event.getUserId());
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getUserId() {
        return userId;
    }

    @Override
    public int compareTo(UserKey other) {
        int applicationOrder = applicationId.compareTo(other.applicationId);
        return applicationOrder != 0 ? applicationOrder : userId.compareTo(other.userId);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UserKey)) {
            return false;
        }
        UserKey key = (UserKey) other;
        return applicationId.equals(key.applicationId) && userId.equals(key.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, userId);
    }
}
