package com.ima.analytics;

import com.ima.domain.Checks;
import com.ima.domain.Event;
import com.ima.domain.UserKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ExperimentCalculator {
    public List<ExperimentResult> calculate(Collection<Event> events, String experimentId,
                                             String conversionEvent, long windowMillis) {
        Checks.identifier(experimentId, "experimentId");
        Checks.identifier(conversionEvent, "conversionEvent");
        Checks.positive(windowMillis, "windowMillis");
        Map<String, Map<String, long[]>> totals = new TreeMap<String, Map<String, long[]>>();
        Map<String, Long> contaminated = new TreeMap<String, Long>();
        for (Map.Entry<UserKey, List<Event>> entry : EventOrdering.byUser(events).entrySet()) {
            long exposure = -1;
            String variant = null;
            boolean conflict = false;
            boolean converted = false;
            for (Event event : entry.getValue()) {
                if ("experiment_exposure".equals(event.getName()) && experimentId.equals(event.property("experiment"))) {
                    String current = Checks.identifier(event.property("variant"), "variant");
                    if (variant != null && !variant.equals(current)) {
                        conflict = true;
                    }
                    if (exposure < 0) {
                        exposure = event.getTimestamp();
                        variant = current;
                    }
                    continue;
                }
                if (exposure >= 0 && conversionEvent.equals(event.getName())
                        && event.getTimestamp() - exposure <= windowMillis) {
                    converted = true;
                }
            }
            String app = entry.getKey().getApplicationId();
            if (exposure < 0) {
                continue;
            }
            if (conflict) {
                Long previous = contaminated.get(app);
                contaminated.put(app, previous == null ? 1 : previous + 1);
                continue;
            }
            Map<String, long[]> application = totals.get(app);
            if (application == null) {
                application = new TreeMap<String, long[]>();
                totals.put(app, application);
            }
            long[] counts = application.get(variant);
            if (counts == null) {
                counts = new long[2];
                application.put(variant, counts);
            }
            counts[0]++;
            if (converted) {
                counts[1]++;
            }
        }
        List<ExperimentResult> result = new ArrayList<ExperimentResult>();
        for (Map.Entry<String, Map<String, long[]>> app : totals.entrySet()) {
            for (Map.Entry<String, long[]> variant : app.getValue().entrySet()) {
                result.add(new ExperimentResult(app.getKey(), experimentId, variant.getKey(),
                        variant.getValue()[0], variant.getValue()[1],
                        contaminated.containsKey(app.getKey()) ? contaminated.get(app.getKey()) : 0));
            }
        }
        return result;
    }
}
