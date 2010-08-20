package com.ima.security;

import com.ima.domain.Checks;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class ApiCredential {
    private final String id;
    private final String applicationId;
    private final String digest;
    private final Set<Scope> scopes;
    private final long expiresAt;
    private final boolean revoked;

    public ApiCredential(String id, String applicationId, String digest,
                         Set<Scope> scopes, long expiresAt, boolean revoked) {
        this.id = Checks.identifier(id, "keyId");
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        if (digest == null || !digest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid credential digest");
        }
        Checks.bounded(scopes, "scopes", 1, 3);
        this.digest = digest;
        this.scopes = Collections.unmodifiableSet(EnumSet.copyOf(scopes));
        this.expiresAt = Checks.positive(expiresAt, "expiresAt");
        this.revoked = revoked;
    }

    public String getId() {
        return id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getDigest() {
        return digest;
    }

    public Set<Scope> getScopes() {
        return scopes;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isActive(long now) {
        return !revoked && now < expiresAt;
    }

    public Principal principal() {
        return new Principal(applicationId, id, scopes);
    }
}
