package com.ima.store;

import com.ima.security.ApiCredential;
import java.util.HashMap;
import java.util.Map;

public final class InMemoryCredentialStore implements CredentialStore {
    private final Map<String, ApiCredential> credentials = new HashMap<String, ApiCredential>();

    @Override
    public synchronized ApiCredential findByDigest(String digest) {
        return credentials.get(digest);
    }

    @Override
    public synchronized void save(ApiCredential credential) {
        if (credentials.containsKey(credential.getDigest())) {
            throw new IllegalArgumentException("credential already exists");
        }
        for (ApiCredential existing : credentials.values()) {
            if (existing.getId().equals(credential.getId())) {
                throw new IllegalArgumentException("credential id already exists");
            }
        }
        credentials.put(credential.getDigest(), credential);
    }

    @Override
    public synchronized boolean revoke(String applicationId, String keyId) {
        for (Map.Entry<String, ApiCredential> entry : credentials.entrySet()) {
            ApiCredential credential = entry.getValue();
            if (credential.getApplicationId().equals(applicationId) && credential.getId().equals(keyId)) {
                entry.setValue(new ApiCredential(credential.getId(), applicationId,
                        credential.getDigest(), credential.getScopes(), credential.getExpiresAt(), true));
                return true;
            }
        }
        return false;
    }
}
