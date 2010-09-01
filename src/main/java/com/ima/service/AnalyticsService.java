package com.ima.service;

import com.ima.analytics.BreakdownCalculator;
import com.ima.analytics.BreakdownRow;
import com.ima.analytics.CrashCalculator;
import com.ima.analytics.CrashSummary;
import com.ima.analytics.Dimension;
import com.ima.analytics.EngagementCalculator;
import com.ima.analytics.EngagementRow;
import com.ima.analytics.FunnelCalculator;
import com.ima.analytics.FunnelDefinition;
import com.ima.analytics.FunnelResult;
import com.ima.analytics.MetricPoint;
import com.ima.analytics.RetentionCalculator;
import com.ima.analytics.RetentionRow;
import com.ima.analytics.RevenueCalculator;
import com.ima.analytics.RevenueSummary;
import com.ima.analytics.SessionSummary;
import com.ima.analytics.Sessionizer;
import com.ima.analytics.TimeSeriesCalculator;
import com.ima.analytics.UtcBuckets;
import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.TimeRange;
import com.ima.security.Principal;
import com.ima.security.Scope;
import com.ima.store.EventQuery;
import com.ima.store.EventStore;
import java.util.List;
import java.util.Map;

public final class AnalyticsService {
    public static final int ONLINE_LIMIT = 100000;
    private final EventStore store;

    public AnalyticsService(EventStore store) {
        this.store = Checks.required(store, "store");
    }

    public List<Event> events(Principal principal, String applicationId, TimeRange range) {
        principal.require(applicationId, Scope.READ);
        if (range.duration() > 366 * UtcBuckets.DAY) {
            throw new IllegalArgumentException("online range must not exceed 366 days");
        }
        return boundedQuery(applicationId, range);
    }

    private List<Event> boundedQuery(String applicationId, TimeRange range) {
        List<Event> events = store.query(new EventQuery(applicationId, range,
                null, null, null, ONLINE_LIMIT + 1));
        if (events.size() > ONLINE_LIMIT) {
            throw new QueryTooLargeException();
        }
        return events;
    }

    public SessionSummary sessions(Principal principal, String applicationId,
                                    TimeRange range, long gap) {
        Checks.positive(gap, "sessionGap");
        if (gap > UtcBuckets.DAY) {
            throw new IllegalArgumentException("sessionGap exceeds one day");
        }
        return new SessionSummary(new Sessionizer(gap).calculate(events(principal, applicationId, range)));
    }

    public FunnelResult funnel(Principal principal, String applicationId,
                                TimeRange range, FunnelDefinition definition) {
        return new FunnelCalculator().calculate(events(principal, applicationId, range), definition);
    }

    public List<MetricPoint> series(Principal principal, String applicationId, TimeRange range, long width) {
        return new TimeSeriesCalculator().calculate(events(principal, applicationId, range), range, width);
    }

    public List<RetentionRow> retention(Principal principal, String applicationId,
                                         long observedUntil, int maximumDay) {
        principal.require(applicationId, Scope.READ);
        // Query from epoch so the first observed date is not mistaken for acquisition.
        List<Event> history = boundedQuery(applicationId, new TimeRange(0, observedUntil));
        return new RetentionCalculator().calculate(history, maximumDay, observedUntil);
    }

    public Map<String, RevenueSummary> revenue(Principal principal, String applicationId, TimeRange range) {
        return new RevenueCalculator().calculate(events(principal, applicationId, range));
    }

    public List<BreakdownRow> breakdown(Principal principal, String applicationId, TimeRange range,
                                        Dimension dimension, int limit) {
        return new BreakdownCalculator().calculate(events(principal, applicationId, range), dimension, limit);
    }

    public CrashSummary crashes(Principal principal, String applicationId, TimeRange range, long gap) {
        Checks.positive(gap, "sessionGap");
        if (gap > UtcBuckets.DAY) {
            throw new IllegalArgumentException("sessionGap exceeds one day");
        }
        return new CrashCalculator().calculate(events(principal, applicationId, range), gap);
    }

    public List<EngagementRow> engagement(Principal principal, String applicationId, TimeRange range) {
        return new EngagementCalculator().calculate(events(principal, applicationId, range), range);
    }
}
