package com.ima.analytics;

public final class ExperimentResult {
    private final String applicationId;
    private final String experimentId;
    private final String variant;
    private final BinomialInterval conversion;
    private final long contaminatedUsers;

    public ExperimentResult(String applicationId, String experimentId, String variant,
                             long users, long converted, long contaminatedUsers) {
        this.applicationId = applicationId;
        this.experimentId = experimentId;
        this.variant = variant;
        this.conversion = new BinomialInterval(converted, users, 1.959963984540054);
        this.contaminatedUsers = contaminatedUsers;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public String getVariant() {
        return variant;
    }

    public BinomialInterval getConversion() {
        return conversion;
    }

    public long getContaminatedUsers() {
        return contaminatedUsers;
    }
}
