package com.ima.analytics;

import com.ima.domain.Checks;

public final class BinomialInterval {
    private final long successes;
    private final long trials;
    private final double z;

    public BinomialInterval(long successes, long trials, double z) {
        this.successes = Checks.nonnegative(successes, "successes");
        this.trials = Checks.nonnegative(trials, "trials");
        if (successes > trials || !Double.isFinite(z) || z <= 0) {
            throw new IllegalArgumentException("invalid binomial interval parameters");
        }
        this.z = z;
    }

    public long getSuccesses() {
        return successes;
    }

    public long getTrials() {
        return trials;
    }

    public Double getRate() {
        return trials == 0 ? null : (double) successes / trials;
    }

    public Double getLower() {
        return trials == 0 ? null : Math.max(0, center() - radius());
    }

    public Double getUpper() {
        return trials == 0 ? null : Math.min(1, center() + radius());
    }

    private double center() {
        return (getRate() + z * z / (2.0 * trials)) / (1 + z * z / trials);
    }

    private double radius() {
        double rate = getRate();
        double size = trials;
        return z * Math.sqrt(rate * (1 - rate) / size + z * z / (4 * size * size)) / (1 + z * z / size);
    }
}
