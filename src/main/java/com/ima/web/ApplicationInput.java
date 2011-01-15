package com.ima.web;

public class ApplicationInput {
    private String displayName;
    private Integer retentionDays;
    private Long sessionGapMillis;
    private Long revision;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public Long getSessionGapMillis() {
        return sessionGapMillis;
    }

    public void setSessionGapMillis(Long sessionGapMillis) {
        this.sessionGapMillis = sessionGapMillis;
    }

    public Long getRevision() {
        return revision;
    }

    public void setRevision(Long revision) {
        this.revision = revision;
    }
}
