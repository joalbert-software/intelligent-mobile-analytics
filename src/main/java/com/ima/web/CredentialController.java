package com.ima.web;

import com.ima.domain.Checks;
import com.ima.service.CredentialService;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/apps/{applicationId}/keys", produces = "application/json")
public final class CredentialController {
    private final CredentialService credentials;

    public CredentialController(CredentialService credentials) {
        this.credentials = credentials;
    }

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json")
    public CredentialService.IssuedCredential issue(@PathVariable("applicationId") String applicationId,
                                                     @RequestBody CredentialInput input,
                                                     HttpServletRequest request) {
        return credentials.issue(RequestContext.principal(request), applicationId,
                input.parsedScopes(), Checks.required(input.getExpiresAt(), "expiresAt"));
    }

    @RequestMapping(value = "/{keyId}", method = RequestMethod.DELETE)
    public Map<String, Boolean> revoke(@PathVariable("applicationId") String applicationId,
                                       @PathVariable("keyId") String keyId,
                                       HttpServletRequest request) {
        return Collections.singletonMap("revoked",
                credentials.revoke(RequestContext.principal(request), applicationId, keyId));
    }
}
