package com.ima.web;

import com.ima.service.PrivacyService;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/apps/{applicationId}/privacy", produces = "application/json")
public final class PrivacyController {
    private final PrivacyService privacy;

    public PrivacyController(PrivacyService privacy) {
        this.privacy = privacy;
    }

    @RequestMapping(value = "/users/{userId}", method = RequestMethod.DELETE)
    public Map<String, Long> deleteUser(@PathVariable("applicationId") String applicationId,
                                        @PathVariable("userId") String userId,
                                        HttpServletRequest request) {
        return Collections.singletonMap("deletedEvents",
                privacy.deleteUser(RequestContext.principal(request), applicationId, userId));
    }

    @RequestMapping(value = "/expired-events", method = RequestMethod.DELETE)
    public Map<String, Long> expire(@PathVariable("applicationId") String applicationId,
                                    @RequestParam("before") long before,
                                    HttpServletRequest request) {
        return Collections.singletonMap("deletedEvents",
                privacy.expire(RequestContext.principal(request), applicationId, before));
    }
}
