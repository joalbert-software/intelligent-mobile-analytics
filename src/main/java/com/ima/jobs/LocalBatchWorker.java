package com.ima.jobs;

import com.ima.analytics.EventOrdering;
import com.ima.analytics.Sessionizer;
import com.ima.analytics.UtcBuckets;
import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.TimeRange;
import com.ima.persistence.JobEntity;
import com.ima.persistence.JobRepository;
import com.ima.security.Principal;
import com.ima.security.Scope;
import com.ima.service.AnalyticsService;
import com.ima.store.EventStore;
import java.time.Clock;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bounded local worker. Large jobs belong to the Hadoop/Python pipeline. */
public final class LocalBatchWorker {
    private static final Logger LOG = LoggerFactory.getLogger(LocalBatchWorker.class);
    private static final long LEASE_MILLIS = 300000;
    private final JobRepository jobs;
    private final AnalyticsService analytics;
    private final JobArtifactWriter artifacts;
    private final Clock clock;
    private final String workerId;

    public LocalBatchWorker(JobRepository jobs, EventStore events, JobArtifactWriter artifacts,
                            Clock clock, String workerId) {
        this.jobs = Checks.required(jobs, "jobs");
        this.analytics = new AnalyticsService(events);
        this.artifacts = Checks.required(artifacts, "artifacts");
        this.clock = Checks.required(clock, "clock");
        this.workerId = Checks.identifier(workerId, "workerId");
    }

    public boolean runOne() {
        jobs.failExhausted(clock.millis(), 5);
        JobEntity job = jobs.claim(workerId, clock.millis(), LEASE_MILLIS, 5);
        if (job == null) {
            return false;
        }
        try {
            Object report = calculate(job);
            if (!jobs.heartbeat(job.getId(), workerId, job.getAttempts(), clock.millis(), LEASE_MILLIS)) {
                LOG.warn("batch_lease_lost job_id={}", job.getId());
                return true;
            }
            artifacts.write(job, report);
            boolean completed = jobs.complete(job.getId(), workerId, job.getAttempts(), clock.millis());
            LOG.info("batch_finished job_id={} completed={}", job.getId(), completed);
        } catch (Exception failure) {
            String code = failure instanceof com.ima.service.QueryTooLargeException
                    ? "use_distributed_batch" : "worker_failure";
            jobs.fail(job.getId(), workerId, job.getAttempts(), clock.millis(), code);
            LOG.warn("batch_failed job_id={} failure_type={}", job.getId(), failure.getClass().getSimpleName());
        }
        return true;
    }

    private Object calculate(JobEntity job) {
        Principal principal = new Principal(job.getApplicationId(), workerId, EnumSet.of(Scope.READ));
        TimeRange range = new TimeRange(job.getRangeStart(), job.getRangeEnd());
        if ("DAILY_ACTIVITY".equals(job.getType())) {
            return analytics.series(principal, job.getApplicationId(), range, UtcBuckets.DAY);
        }
        if ("REVENUE".equals(job.getType())) {
            return analytics.revenue(principal, job.getApplicationId(), range);
        }
        if ("RETENTION".equals(job.getType())) {
            return analytics.retention(principal, job.getApplicationId(), range.getEnd(), 30);
        }
        if ("SESSIONS".equals(job.getType())) {
            return analytics.sessions(principal, job.getApplicationId(), range, Sessionizer.DEFAULT_GAP_MILLIS);
        }
        if ("QUALITY".equals(job.getType())) {
            return quality(analytics.events(principal, job.getApplicationId(), range));
        }
        throw new IllegalArgumentException("unsupported local job type");
    }

    private Map<String, Object> quality(List<Event> events) {
        List<Event> unique = EventOrdering.unique(events);
        long negativeLag = 0;
        long maximumLag = 0;
        for (Event event : unique) {
            long lag = event.getReceivedAt() - event.getTimestamp();
            if (lag < 0) {
                negativeLag++;
            }
            maximumLag = Math.max(maximumLag, lag);
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("events", events.size());
        result.put("uniqueEvents", unique.size());
        result.put("duplicates", events.size() - unique.size());
        result.put("negativeLagEvents", negativeLag);
        result.put("maximumLagMillis", maximumLag);
        result.put("scope", "accepted events only; rejected ingestion rows are not stored");
        return result;
    }
}
