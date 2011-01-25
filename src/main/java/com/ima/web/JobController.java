package com.ima.web;

import com.ima.domain.Page;
import com.ima.persistence.JobEntity;
import com.ima.service.JobService;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistent")
@RequestMapping(value = "/api/v1/apps/{applicationId}/jobs", produces = "application/json")
public final class JobController {
    private final JobService jobs;

    public JobController(JobService jobs) {
        this.jobs = jobs;
    }

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<JobEntity> enqueue(@PathVariable("applicationId") String applicationId,
                                              @RequestBody JobInput input, HttpServletRequest request) {
        return ResponseEntity.status(202).body(jobs.enqueue(RequestContext.principal(request),
                applicationId, input.getType(), input.range()));
    }

    @RequestMapping(method = RequestMethod.GET)
    public Page<JobEntity> list(@PathVariable("applicationId") String applicationId,
                                @RequestParam(value = "offset", defaultValue = "0") int offset,
                                @RequestParam(value = "limit", defaultValue = "20") int limit,
                                HttpServletRequest request) {
        return jobs.list(RequestContext.principal(request), applicationId, offset, limit);
    }

    @RequestMapping(value = "/{jobId}", method = RequestMethod.GET)
    public JobEntity find(@PathVariable("applicationId") String applicationId,
                           @PathVariable("jobId") String jobId, HttpServletRequest request) {
        return jobs.find(RequestContext.principal(request), applicationId, jobId);
    }

    @RequestMapping(value = "/{jobId}/retry", method = RequestMethod.POST)
    public Map<String, Boolean> retry(@PathVariable("applicationId") String applicationId,
                                       @PathVariable("jobId") String jobId, HttpServletRequest request) {
        return Collections.singletonMap("queued", jobs.retry(RequestContext.principal(request), applicationId, jobId));
    }

    @RequestMapping(value = "/{jobId}", method = RequestMethod.DELETE)
    public Map<String, Boolean> cancel(@PathVariable("applicationId") String applicationId,
                                        @PathVariable("jobId") String jobId, HttpServletRequest request) {
        return Collections.singletonMap("cancelled", jobs.cancel(RequestContext.principal(request), applicationId, jobId));
    }
}
