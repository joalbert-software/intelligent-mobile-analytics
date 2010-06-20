package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Money;

public final class RevenueSummary {
    private final Money gross;
    private final Money refunds;
    private final long purchases;
    private final long purchasers;

    public RevenueSummary(Money gross, Money refunds, long purchases, long purchasers) {
        this.gross = Checks.required(gross, "gross");
        this.refunds = Checks.required(refunds, "refunds");
        if (!gross.getCurrency().equals(refunds.getCurrency())) {
            throw new IllegalArgumentException("currency mismatch");
        }
        this.purchases = Checks.nonnegative(purchases, "purchases");
        this.purchasers = Checks.nonnegative(purchasers, "purchasers");
    }

    public Money getGross() {
        return gross;
    }

    public Money getRefunds() {
        return refunds;
    }

    public Money getNet() {
        return gross.plus(refunds.negate());
    }

    public long getPurchases() {
        return purchases;
    }

    public long getPurchasers() {
        return purchasers;
    }

    public Double getAverageOrderMinorUnits() {
        return purchases == 0 ? null : (double) gross.getMinorUnits() / purchases;
    }
}
