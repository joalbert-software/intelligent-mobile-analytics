package com.ima.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.domain.Event;
import com.ima.domain.TimeRange;
import com.ima.export.EventExporter;
import com.ima.service.AnalyticsService;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/apps/{applicationId}/export")
public final class ExportController {
    private final AnalyticsService analytics;
    private final EventExporter exporter;

    public ExportController(AnalyticsService analytics, ObjectMapper mapper) {
        this.analytics = analytics;
        this.exporter = new EventExporter(mapper);
    }

    @RequestMapping(value = "/events.csv", method = RequestMethod.GET)
    public void csv(@PathVariable("applicationId") String applicationId,
                    @RequestParam("start") long start, @RequestParam("end") long end,
                    HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Event> events = analytics.events(RequestContext.principal(request), applicationId, new TimeRange(start, end));
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=ima-events.csv");
        exporter.csv(events, response.getWriter());
    }

    @RequestMapping(value = "/events.jsonl", method = RequestMethod.GET)
    public void jsonLines(@PathVariable("applicationId") String applicationId,
                          @RequestParam("start") long start, @RequestParam("end") long end,
                          HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<Event> events = analytics.events(RequestContext.principal(request), applicationId, new TimeRange(start, end));
        response.setContentType("application/x-ndjson");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=ima-events.jsonl");
        exporter.jsonLines(events, response.getWriter());
    }
}
