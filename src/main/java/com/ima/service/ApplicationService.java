package com.ima.service;

import com.ima.domain.Checks;
import com.ima.persistence.ApplicationEntity;
import com.ima.persistence.ApplicationRepository;
import com.ima.security.Principal;
import com.ima.security.Scope;
import java.time.Clock;

public final class ApplicationService {
    private final ApplicationRepository applications;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, Clock clock) {
        this.applications = applications;
        this.clock = clock;
    }

    public ApplicationEntity find(Principal principal, String applicationId) {
        principal.require(applicationId, Scope.READ);
        ApplicationEntity application = applications.find(applicationId);
        if (application == null) {
            throw new ResourceNotFoundException("application not found");
        }
        return application;
    }

    public ApplicationEntity configure(Principal principal, String applicationId, String displayName,
                                        int retentionDays, long sessionGap, long expectedRevision) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.text(displayName, "displayName", 160);
        Checks.range(retentionDays, "retentionDays", 1, 3650);
        if (sessionGap < 1 || sessionGap > 86400000) {
            throw new IllegalArgumentException("session gap must be within one day");
        }
        ApplicationEntity application = applications.find(applicationId);
        if (application == null) {
            throw new ResourceNotFoundException("application not found");
        }
        if (application.getRevision() != expectedRevision) {
            throw new RevisionConflictException();
        }
        application.setDisplayName(displayName);
        application.setRetentionDays(retentionDays);
        application.setSessionGapMillis(sessionGap);
        application.setUpdatedAt(clock.millis());
        return applications.update(application);
    }
}
