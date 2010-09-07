package com.ima.service;

import com.ima.domain.Checks;
import com.ima.domain.Page;
import com.ima.domain.TimeRange;
import com.ima.persistence.JobEntity;
import com.ima.persistence.JobRepository;
import com.ima.security.Principal;
import com.ima.security.Scope;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class JobService {
    private static final Set<String> TYPES = new HashSet<String>(Arrays.asList(
            "SESSIONS", "DAILY_ACTIVITY", "REVENUE", "RETENTION", "QUALITY"));
    private final JobRepository jobs;
    private final Clock clock;

    public JobService(JobRepository jobs, Clock clock) {
        this.jobs = jobs;
        this.clock = clock;
    }

    public JobEntity enqueue(Principal principal, String applicationId, String type, TimeRange range) {
        principal.require(applicationId, Scope.ADMIN);
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("unsupported batch job type");
        }
        if (range.getEnd() > clock.millis()) {
            throw new IllegalArgumentException("batch job range must end in the past");
        }
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID().toString());
        job.setApplicationId(applicationId);
        job.setType(type);
        job.setRangeStart(range.getStart());
        job.setRangeEnd(range.getEnd());
        job.setCreatedAt(clock.millis());
        job.setUpdatedAt(job.getCreatedAt());
        jobs.enqueue(job);
        return job;
    }

    public JobEntity find(Principal principal, String applicationId, String jobId) {
        principal.require(applicationId, Scope.READ);
        Checks.identifier(jobId, "jobId");
        JobEntity job = jobs.find(applicationId, jobId);
        if (job == null) {
            throw new ResourceNotFoundException("batch job not found");
        }
        return job;
    }

    public Page<JobEntity> list(Principal principal, String applicationId, int offset, int limit) {
        principal.require(applicationId, Scope.READ);
        return jobs.list(applicationId, offset, limit);
    }

    public boolean retry(Principal principal, String applicationId, String jobId) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.identifier(jobId, "jobId");
        return jobs.retry(applicationId, jobId, clock.millis());
    }

    public boolean cancel(Principal principal, String applicationId, String jobId) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.identifier(jobId, "jobId");
        return jobs.cancel(applicationId, jobId, clock.millis());
    }
}
