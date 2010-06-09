package com.ima.analytics;

import com.ima.domain.Checks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Welford variance plus exact interpolated percentiles for bounded reports. */
public final class DescriptiveStatistics {
    private final List<Double> sorted;
    private final double mean;
    private final double m2;
    private final double sum;

    public DescriptiveStatistics(Collection<? extends Number> values) {
        Checks.required(values, "values");
        List<Double> copy = new ArrayList<Double>();
        double runningMean = 0;
        double runningM2 = 0;
        double runningSum = 0;
        double correction = 0;
        for (Number number : values) {
            Checks.required(number, "number");
            double value = number.doubleValue();
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("statistics require finite values");
            }
            copy.add(value);
            double delta = value - runningMean;
            runningMean += delta / copy.size();
            runningM2 += delta * (value - runningMean);
            double adjusted = value - correction;
            double next = runningSum + adjusted;
            correction = (next - runningSum) - adjusted;
            runningSum = next;
        }
        Collections.sort(copy);
        this.sorted = Collections.unmodifiableList(copy);
        this.mean = runningMean;
        this.m2 = runningM2;
        this.sum = runningSum;
    }

    public int getCount() {
        return sorted.size();
    }

    public double getSum() {
        return sum;
    }

    public Double getMean() {
        return sorted.isEmpty() ? null : mean;
    }

    public Double getMinimum() {
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    public Double getMaximum() {
        return sorted.isEmpty() ? null : sorted.get(sorted.size() - 1);
    }

    public Double getPopulationVariance() {
        return sorted.isEmpty() ? null : Math.max(0, m2 / sorted.size());
    }

    public Double getSampleVariance() {
        return sorted.size() < 2 ? null : Math.max(0, m2 / (sorted.size() - 1));
    }

    public Double percentile(double fraction) {
        if (Double.isNaN(fraction) || fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("percentile must be between zero and one");
        }
        if (sorted.isEmpty()) {
            return null;
        }
        double position = fraction * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        double weight = position - lower;
        return (1 - weight) * sorted.get(lower) + weight * sorted.get(upper);
    }
}
