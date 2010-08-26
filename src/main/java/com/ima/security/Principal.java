package com.ima.security;

import com.ima.domain.Checks;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class Principal {
    private final String applicationId;
    private final String keyId;
    private final Set<Scope> scopes;

    public Principal(String applicationId, String keyId, Set<Scope> scopes) {
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.keyId = Checks.identifier(keyId, "keyId");
        Checks.bounded(scopes, "scopes", 1, Scope.values().length);
        this.scopes = Collections.unmodifiableSet(EnumSet.copyOf(scopes));
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getKeyId() {
        return keyId;
    }

    public Set<Scope> getScopes() {
        return scopes;
    }

    public void require(String applicationId, Scope scope) {
        if (!this.applicationId.equals(applicationId) || !scopes.contains(scope)) {
            throw new AccessDeniedException("permission denied");
        }
    }
}
