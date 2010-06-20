package com.ima.analytics;

import com.ima.domain.Event;
import com.ima.domain.Money;
import com.ima.domain.UserKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Purchase/refund amounts are positive minor-unit strings. */
public final class RevenueCalculator {
    private static final class Accumulator {
        private Money gross;
        private Money refunds;
        private long purchases;
        private final Set<UserKey> users = new HashSet<UserKey>();

        private Accumulator(String currency) {
            gross = new Money(0, currency);
            refunds = new Money(0, currency);
        }
    }

    public Map<String, RevenueSummary> calculate(Collection<Event> events) {
        Map<String, Accumulator> totals = new TreeMap<String, Accumulator>();
        for (Event event : EventOrdering.unique(events)) {
            boolean purchase = "purchase".equals(event.getName());
            boolean refund = "refund".equals(event.getName());
            if (!purchase && !refund) {
                continue;
            }
            long amount;
            try {
                amount = Long.parseLong(event.property("amount_minor"));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("revenue amount must be integer minor units");
            }
            if (amount < 0) {
                throw new IllegalArgumentException("revenue amounts must not be negative");
            }
            Money money = new Money(amount, event.property("currency"));
            Accumulator total = totals.get(money.getCurrency());
            if (total == null) {
                total = new Accumulator(money.getCurrency());
                totals.put(money.getCurrency(), total);
            }
            if (purchase) {
                total.gross = total.gross.plus(money);
                total.purchases++;
                total.users.add(UserKey.of(event));
            } else {
                total.refunds = total.refunds.plus(money);
            }
        }
        Map<String, RevenueSummary> result = new TreeMap<String, RevenueSummary>();
        for (Map.Entry<String, Accumulator> entry : totals.entrySet()) {
            Accumulator value = entry.getValue();
            result.put(entry.getKey(), new RevenueSummary(value.gross, value.refunds,
                    value.purchases, value.users.size()));
        }
        return result;
    }
}
