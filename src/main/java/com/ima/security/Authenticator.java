package com.ima.security;

import com.ima.domain.Checks;
import com.ima.store.CredentialStore;
import java.time.Clock;

public final class Authenticator {
    private final CredentialStore credentials;
    private final ApiKeyHasher hasher;
    private final Clock clock;

    public Authenticator(CredentialStore credentials, ApiKeyHasher hasher, Clock clock) {
        this.credentials = Checks.required(credentials, "credentials");
        this.hasher = Checks.required(hasher, "hasher");
        this.clock = Checks.required(clock, "clock");
    }

    public Principal authenticate(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationException();
        }
        String token = authorization.substring(7);
        if (token.length() < 32 || token.length() > 256) {
            throw new AuthenticationException();
        }
        ApiCredential credential = credentials.findByDigest(hasher.digest(token));
        if (credential == null || !credential.isActive(clock.millis())
                || !hasher.matches(token, credential.getDigest())) {
            throw new AuthenticationException();
        }
        return credential.principal();
    }
}
