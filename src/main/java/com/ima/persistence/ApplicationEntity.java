package com.ima.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "applications")
public class ApplicationEntity {
    @Id
    @Column(name = "application_id", length = 128, nullable = false)
    private String id;

    @Column(name = "display_name", length = 160, nullable = false)
    private String displayName;

    @Column(name = "owner_reference", length = 128, nullable = false)
    private String ownerReference;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "session_gap_millis", nullable = false)
    private long sessionGapMillis;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Version
    @Column(name = "revision", nullable = false)
    private long revision;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getOwnerReference() {
        return ownerReference;
    }

    public void setOwnerReference(String ownerReference) {
        this.ownerReference = ownerReference;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public long getSessionGapMillis() {
        return sessionGapMillis;
    }

    public void setSessionGapMillis(long sessionGapMillis) {
        this.sessionGapMillis = sessionGapMillis;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getRevision() {
        return revision;
    }
}
