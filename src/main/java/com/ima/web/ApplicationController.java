package com.ima.web;

import com.ima.domain.Checks;
import com.ima.persistence.ApplicationEntity;
import com.ima.service.ApplicationService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistent")
@RequestMapping(value = "/api/v1/apps/{applicationId}", produces = "application/json")
public final class ApplicationController {
    private final ApplicationService applications;

    public ApplicationController(ApplicationService applications) {
        this.applications = applications;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ApplicationEntity find(@PathVariable("applicationId") String applicationId,
                                    HttpServletRequest request) {
        return applications.find(RequestContext.principal(request), applicationId);
    }

    @RequestMapping(method = RequestMethod.PUT, consumes = "application/json")
    public ApplicationEntity configure(@PathVariable("applicationId") String applicationId,
                                         @RequestBody ApplicationInput input,
                                         HttpServletRequest request) {
        return applications.configure(RequestContext.principal(request), applicationId, input.getDisplayName(),
                Checks.required(input.getRetentionDays(), "retentionDays"),
                Checks.required(input.getSessionGapMillis(), "sessionGapMillis"),
                Checks.required(input.getRevision(), "revision"));
    }
}
