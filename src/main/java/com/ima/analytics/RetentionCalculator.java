package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Exact-day retention; callers supply full acquisition history. */
public final class RetentionCalculator {
    public List<RetentionRow> calculate(Collection<Event> events, int maximumDay,
                                         long observedUntilExclusive) {
        Checks.range(maximumDay, "maximumDay", 0, 365);
        Checks.nonnegative(observedUntilExclusive, "observedUntilExclusive");
        Map<String, Map<Long, List<Set<Integer>>>> applications =
                new TreeMap<String, Map<Long, List<Set<Integer>>>>();
        List<Event> observed = new ArrayList<Event>();
        for (Event event : events) {
            if (event.getTimestamp() < observedUntilExclusive) {
                observed.add(event);
            }
        }
        for (Map.Entry<UserKey, List<Event>> user : EventOrdering.byUser(observed).entrySet()) {
            String applicationId = user.getKey().getApplicationId();
            long firstDay = UtcBuckets.day(user.getValue().get(0).getTimestamp());
            Map<Long, List<Set<Integer>>> cohorts = applications.get(applicationId);
            if (cohorts == null) {
                cohorts = new TreeMap<Long, List<Set<Integer>>>();
                applications.put(applicationId, cohorts);
            }
            List<Set<Integer>> members = cohorts.get(firstDay);
            if (members == null) {
                members = new ArrayList<Set<Integer>>();
                cohorts.put(firstDay, members);
            }
            Set<Integer> activeDays = new HashSet<Integer>();
            for (Event event : user.getValue()) {
                int day = UtcBuckets.daysBetween(firstDay, event.getTimestamp());
                if (day <= maximumDay) {
                    activeDays.add(day);
                }
            }
            members.add(activeDays);
        }
        List<RetentionRow> rows = new ArrayList<RetentionRow>();
        for (Map.Entry<String, Map<Long, List<Set<Integer>>>> app : applications.entrySet()) {
            for (Map.Entry<Long, List<Set<Integer>>> cohort : app.getValue().entrySet()) {
                Map<Integer, Long> retained = new TreeMap<Integer, Long>();
                long completeDays = (observedUntilExclusive - cohort.getKey()) / UtcBuckets.DAY;
                for (int day = 0; day <= maximumDay && day < completeDays; day++) {
                    long count = 0;
                    for (Set<Integer> activeDays : cohort.getValue()) {
                        if (activeDays.contains(day)) {
                            count++;
                        }
                    }
                    retained.put(day, count);
                }
                rows.add(new RetentionRow(app.getKey(), cohort.getKey(),
                        cohort.getValue().size(), retained));
            }
        }
        return rows;
    }
}
