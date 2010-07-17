package com.ima.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "audit_records")
public class AuditEntity {
    @Id
    @Column(name = "audit_id", length = 36, nullable = false)
    private String id;

    @Column(name = "application_id", length = 128, nullable = false)
    private String applicationId;

    @Column(name = "actor_key_id", length = 128, nullable = false)
    private String actorKeyId;

    @Column(name = "action_name", length = 64, nullable = false)
    private String action;

    @Column(name = "resource_reference", length = 160, nullable = false)
    private String resource;

    @Column(name = "occurred_at", nullable = false)
    private long occurredAt;

    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "successful", nullable = false)
    private boolean successful;

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

    public String getActorKeyId() {
        return actorKeyId;
    }

    public void setActorKeyId(String actorKeyId) {
        this.actorKeyId = actorKeyId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(long occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}
