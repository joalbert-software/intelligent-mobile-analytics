package com.ima.web;

import com.ima.domain.Checks;
import com.ima.security.Scope;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class CredentialInput {
    private List<String> scopes;
    private Long expiresAt;

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Set<Scope> parsedScopes() {
        Checks.bounded(scopes, "scopes", 1, 3);
        EnumSet<Scope> parsed = EnumSet.noneOf(Scope.class);
        for (String scope : scopes) {
            try {
                parsed.add(Scope.valueOf(Checks.required(scope, "scope")));
            } catch (IllegalArgumentException failure) {
                throw new IllegalArgumentException("unknown credential scope");
            }
        }
        return parsed;
    }
}
