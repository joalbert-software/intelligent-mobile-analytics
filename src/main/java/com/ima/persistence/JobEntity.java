package com.ima.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "batch_jobs")
public class JobEntity {
    @Id
    @Column(name = "job_id", length = 128, nullable = false)
    private String id;

    @Column(name = "application_id", length = 128, nullable = false)
    private String applicationId;

    @Column(name = "job_type", length = 32, nullable = false)
    private String type;

    @Column(name = "job_state", length = 32, nullable = false)
    private String state;

    @Column(name = "range_start", nullable = false)
    private long rangeStart;

    @Column(name = "range_end", nullable = false)
    private long rangeEnd;

    @Column(name = "attempt_count", nullable = false)
    private int attempts;

    @Column(name = "lease_owner", length = 128)
    private String leaseOwner;

    @Column(name = "lease_until", nullable = false)
    private long leaseUntil;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Version
    @Column(name = "revision", nullable = false)
    private long revision;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(long rangeStart) {
        this.rangeStart = rangeStart;
    }

    public long getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(long rangeEnd) {
        this.rangeEnd = rangeEnd;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getLeaseOwner() {
        return leaseOwner;
    }

    public void setLeaseOwner(String leaseOwner) {
        this.leaseOwner = leaseOwner;
    }

    public long getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(long leaseUntil) {
        this.leaseUntil = leaseUntil;
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

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public long getRevision() {
        return revision;
    }
}
