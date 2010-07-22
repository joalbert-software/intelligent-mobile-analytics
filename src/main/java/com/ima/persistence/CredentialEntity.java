package com.ima.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "api_credentials")
public class CredentialEntity {
    @Id
    @Column(name = "key_id", length = 128, nullable = false)
    private String id;

    @Column(name = "application_id", length = 128, nullable = false)
    private String applicationId;

    @Column(name = "key_digest", length = 64, unique = true, nullable = false)
    private String digest;

    @Column(name = "scopes", length = 64, nullable = false)
    private String scopes;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
