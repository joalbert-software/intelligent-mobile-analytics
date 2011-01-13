package com.ima.web;

import com.ima.analytics.BreakdownRow;
import com.ima.analytics.CrashSummary;
import com.ima.analytics.Dimension;
import com.ima.analytics.EngagementRow;
import com.ima.analytics.FunnelResult;
import com.ima.analytics.MetricPoint;
import com.ima.analytics.RetentionRow;
import com.ima.analytics.RevenueSummary;
import com.ima.analytics.SessionSummary;
import com.ima.domain.TimeRange;
import com.ima.service.AnalyticsService;
import com.ima.service.CachedReportService;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/apps/{applicationId}/analytics", produces = "application/json")
public final class AnalyticsController {
    private final AnalyticsService analytics;
    private final CachedReportService cached;

    public AnalyticsController(AnalyticsService analytics, CachedReportService cached) {
        this.analytics = analytics;
        this.cached = cached;
    }

    @RequestMapping(value = "/series", method = RequestMethod.GET)
    public String series(@PathVariable("applicationId") final String applicationId,
                         @RequestParam("start") final long start,
                         @RequestParam("end") final long end,
                         @RequestParam(value = "width", defaultValue = "86400000") final long width,
                         final HttpServletRequest request) {
        Map<String, String> parameters = new TreeMap<String, String>();
        parameters.put("start", Long.toString(start));
        parameters.put("end", Long.toString(end));
        parameters.put("width", Long.toString(width));
        return cached.render(RequestContext.principal(request), applicationId, "series", parameters,
                new Callable<List<MetricPoint>>() {
                    @Override
                    public List<MetricPoint> call() {
                        return analytics.series(RequestContext.principal(request), applicationId,
                                new TimeRange(start, end), width);
                    }
                });
    }

    @RequestMapping(value = "/sessions", method = RequestMethod.GET)
    public SessionSummary sessions(@PathVariable("applicationId") String applicationId,
                                    @RequestParam("start") long start,
                                    @RequestParam("end") long end,
                                    @RequestParam(value = "gap", defaultValue = "1800000") long gap,
                                    HttpServletRequest request) {
        return analytics.sessions(RequestContext.principal(request), applicationId, new TimeRange(start, end), gap);
    }

    @RequestMapping(value = "/funnel", method = RequestMethod.POST, consumes = "application/json")
    public FunnelResult funnel(@PathVariable("applicationId") String applicationId,
                               @RequestBody FunnelInput input, HttpServletRequest request) {
        return analytics.funnel(RequestContext.principal(request), applicationId, input.range(), input.definition());
    }

    @RequestMapping(value = "/retention", method = RequestMethod.GET)
    public List<RetentionRow> retention(@PathVariable("applicationId") String applicationId,
                                        @RequestParam("observedUntil") long observedUntil,
                                        @RequestParam(value = "maximumDay", defaultValue = "30") int maximumDay,
                                        HttpServletRequest request) {
        return analytics.retention(RequestContext.principal(request), applicationId, observedUntil, maximumDay);
    }

    @RequestMapping(value = "/revenue", method = RequestMethod.GET)
    public Map<String, RevenueSummary> revenue(@PathVariable("applicationId") String applicationId,
                                               @RequestParam("start") long start,
                                               @RequestParam("end") long end,
                                               HttpServletRequest request) {
        return analytics.revenue(RequestContext.principal(request), applicationId, new TimeRange(start, end));
    }

    @RequestMapping(value = "/breakdown", method = RequestMethod.GET)
    public List<BreakdownRow> breakdown(@PathVariable("applicationId") String applicationId,
                                        @RequestParam("start") long start,
                                        @RequestParam("end") long end,
                                        @RequestParam("dimension") String dimension,
                                        @RequestParam(value = "limit", defaultValue = "20") int limit,
                                        HttpServletRequest request) {
        return analytics.breakdown(RequestContext.principal(request), applicationId,
                new TimeRange(start, end), new Dimension(dimension), limit);
    }

    @RequestMapping(value = "/crashes", method = RequestMethod.GET)
    public CrashSummary crashes(@PathVariable("applicationId") String applicationId,
                                 @RequestParam("start") long start,
                                 @RequestParam("end") long end,
                                 @RequestParam(value = "gap", defaultValue = "1800000") long gap,
                                 HttpServletRequest request) {
        return analytics.crashes(RequestContext.principal(request), applicationId, new TimeRange(start, end), gap);
    }

    @RequestMapping(value = "/engagement", method = RequestMethod.GET)
    public List<EngagementRow> engagement(@PathVariable("applicationId") String applicationId,
                                         @RequestParam("start") long start,
                                         @RequestParam("end") long end,
                                         HttpServletRequest request) {
        return analytics.engagement(RequestContext.principal(request), applicationId, new TimeRange(start, end));
    }
}
