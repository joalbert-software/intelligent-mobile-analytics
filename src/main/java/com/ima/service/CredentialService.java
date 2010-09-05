package com.ima.service;

import com.ima.domain.Checks;
import com.ima.security.ApiCredential;
import com.ima.security.ApiKeyHasher;
import com.ima.security.Principal;
import com.ima.security.Scope;
import com.ima.store.CredentialStore;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;

public final class CredentialService {
    private final CredentialStore store;
    private final ApiKeyHasher hasher;
    private final Clock clock;

    public CredentialService(CredentialStore store, ApiKeyHasher hasher, Clock clock) {
        this.store = store;
        this.hasher = hasher;
        this.clock = clock;
    }

    public IssuedCredential issue(Principal principal, String applicationId,
                                  Set<Scope> scopes, long expiresAt) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.bounded(scopes, "scopes", 1, 3);
        if (!principal.getScopes().containsAll(scopes)) {
            throw new com.ima.security.AccessDeniedException("cannot grant scopes the issuer lacks");
        }
        if (expiresAt <= clock.millis()) {
            throw new IllegalArgumentException("credential expiration must be in the future");
        }
        String token = hasher.generate();
        String id = UUID.randomUUID().toString();
        store.save(new ApiCredential(id, applicationId, hasher.digest(token), scopes, expiresAt, false));
        return new IssuedCredential(id, token, expiresAt);
    }

    public boolean revoke(Principal principal, String applicationId, String keyId) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.identifier(keyId, "keyId");
        return store.revoke(applicationId, keyId);
    }

    public static final class IssuedCredential {
        private final String keyId;
        private final String token;
        private final long expiresAt;

        private IssuedCredential(String keyId, String token, long expiresAt) {
            this.keyId = keyId;
            this.token = token;
            this.expiresAt = expiresAt;
        }

        public String getKeyId() {
            return keyId;
        }

        public String getToken() {
            return token;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}
