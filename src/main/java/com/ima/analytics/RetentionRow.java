package com.ima.analytics;

import com.ima.domain.Checks;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** Missing offsets are unobserved, rather than observed zero retention. */
public final class RetentionRow {
    private final String applicationId;
    private final long cohortDay;
    private final long cohortUsers;
    private final Map<Integer, Long> retained;

    public RetentionRow(String applicationId, long cohortDay,
                        long cohortUsers, Map<Integer, Long> retained) {
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.cohortDay = Checks.nonnegative(cohortDay, "cohortDay");
        this.cohortUsers = Checks.nonnegative(cohortUsers, "cohortUsers");
        Map<Integer, Long> copy = new TreeMap<Integer, Long>();
        for (Map.Entry<Integer, Long> entry : retained.entrySet()) {
            Checks.range(entry.getKey(), "offset", 0, 365);
            if (entry.getValue() < 0 || entry.getValue() > cohortUsers) {
                throw new IllegalArgumentException("invalid retained count");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        this.retained = Collections.unmodifiableMap(copy);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public long getCohortDay() {
        return cohortDay;
    }

    public long getCohortUsers() {
        return cohortUsers;
    }

    public Map<Integer, Long> getRetained() {
        return retained;
    }

    public Double rate(int offset) {
        Long count = retained.get(offset);
        return count == null ? null : cohortUsers == 0 ? 0.0 : (double) count / cohortUsers;
    }
}
