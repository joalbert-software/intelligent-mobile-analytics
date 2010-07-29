package com.ima.store;

import com.ima.security.ApiCredential;

public interface CredentialStore {
    ApiCredential findByDigest(String digest);

    void save(ApiCredential credential);

    boolean revoke(String applicationId, String keyId);
}
