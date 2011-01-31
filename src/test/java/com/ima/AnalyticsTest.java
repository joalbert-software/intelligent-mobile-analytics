package com.ima;

import com.ima.analytics.FunnelCalculator;
import com.ima.analytics.FunnelDefinition;
import com.ima.analytics.Sessionizer;
import com.ima.domain.Event;
import com.ima.domain.Money;
import com.ima.domain.Platform;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnalyticsTest {
    private Event event(String id, String name, long timestamp) {
        return new Event("app", id, "user", name, timestamp, timestamp,
                Platform.ANDROID, "1.0", Collections.<String, String>emptyMap());
    }

    @Test
    public void sessionGapIsStrictAndInputIsSorted() {
        assertEquals(1, new Sessionizer(100).calculate(Arrays.asList(
                event("second", "view", 100), event("first", "open", 0))).size());
        assertEquals(2, new Sessionizer(100).calculate(Arrays.asList(
                event("first", "open", 0), event("second", "view", 101))).size());
    }

    @Test
    public void repeatedFunnelStepsNeedSeparateEvents() {
        FunnelDefinition definition = new FunnelDefinition(Arrays.asList("tap", "tap"), 100);
        assertEquals(Arrays.asList(1L, 0L), new FunnelCalculator().calculate(
                Collections.singletonList(event("one", "tap", 1)), definition).getUsers());
    }

    @Test
    public void newerFunnelStartRecoversAnExpiredAttempt() {
        FunnelDefinition definition = new FunnelDefinition(Arrays.asList("open", "buy"), 100);
        assertEquals(Arrays.asList(1L, 1L), new FunnelCalculator().calculate(Arrays.asList(
                event("a", "open", 0), event("b", "open", 200), event("c", "buy", 250)), definition).getUsers());
    }

    @Test(expected = IllegalArgumentException.class)
    public void currenciesCannotBeMixed() {
        new Money(100, "USD").plus(new Money(100, "EUR"));
    }

    @Test(expected = ArithmeticException.class)
    public void moneyOverflowDoesNotWrap() {
        new Money(Long.MAX_VALUE, "USD").plus(new Money(1, "USD"));
    }
}
