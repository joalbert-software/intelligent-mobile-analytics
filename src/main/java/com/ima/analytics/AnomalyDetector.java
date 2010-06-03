package com.ima.analytics;

import com.ima.domain.Checks;
import java.util.Collection;

/** Transparent baseline detector; not a trained prediction model. */
public final class AnomalyDetector {
    public AnomalyResult detect(Collection<? extends Number> baseline, double actual,
                                int minimumSamples, double threshold) {
        Checks.range(minimumSamples, "minimumSamples", 2, 100000);
        if (!Double.isFinite(actual) || !Double.isFinite(threshold) || threshold <= 0) {
            throw new IllegalArgumentException("invalid anomaly parameters");
        }
        DescriptiveStatistics statistics = new DescriptiveStatistics(baseline);
        if (statistics.getCount() < minimumSamples) {
            return new AnomalyResult(actual, statistics.getMean(), null, false);
        }
        double mean = statistics.getMean();
        double deviation = Math.sqrt(statistics.getSampleVariance());
        if (deviation == 0) {
            // A null score avoids non-JSON Infinity while preserving the decision.
            return new AnomalyResult(actual, mean, actual == mean ? 0.0 : null, actual != mean);
        }
        double score = (actual - mean) / deviation;
        return new AnomalyResult(actual, mean, score, Math.abs(score) > threshold);
    }
}
