package com.ima.analytics;

public final class AnomalyResult {
    private final double actual;
    private final Double expected;
    private final Double score;
    private final boolean anomalous;

    public AnomalyResult(double actual, Double expected, Double score, boolean anomalous) {
        this.actual = actual;
        this.expected = expected;
        this.score = score;
        this.anomalous = anomalous;
    }

    public double getActual() {
        return actual;
    }

    public Double getExpected() {
        return expected;
    }

    public Double getScore() {
        return score;
    }

    public boolean isAnomalous() {
        return anomalous;
    }
}
