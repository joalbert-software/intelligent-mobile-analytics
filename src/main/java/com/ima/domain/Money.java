package com.ima.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/** Integer minor units; never sum unlike currencies. */
public final class Money implements Comparable<Money> {
    private final long minorUnits;
    private final String currency;

    public Money(long minorUnits, String currency) {
        Checks.required(currency, "currency");
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must be a three-letter uppercase code");
        }
        Currency unit = Currency.getInstance(currency);
        if (unit.getDefaultFractionDigits() < 0) {
            throw new IllegalArgumentException("currency has no defined minor unit");
        }
        this.currency = unit.getCurrencyCode();
        this.minorUnits = minorUnits;
    }

    public long getMinorUnits() {
        return minorUnits;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal asDecimal() {
        return BigDecimal.valueOf(minorUnits,
                Currency.getInstance(currency).getDefaultFractionDigits());
    }

    public Money plus(Money other) {
        requireCurrency(other);
        long sum = minorUnits + other.minorUnits;
        if (((minorUnits ^ sum) & (other.minorUnits ^ sum)) < 0) {
            throw new ArithmeticException("money overflow");
        }
        return new Money(sum, currency);
    }

    public Money negate() {
        if (minorUnits == Long.MIN_VALUE) {
            throw new ArithmeticException("money overflow");
        }
        return new Money(-minorUnits, currency);
    }

    private void requireCurrency(Money other) {
        Checks.required(other, "money");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
    }

    @Override
    public int compareTo(Money other) {
        requireCurrency(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Money)) {
            return false;
        }
        Money money = (Money) other;
        return minorUnits == money.minorUnits && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minorUnits, currency);
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s", currency, asDecimal().toPlainString());
    }
}
