package com.ima.web;

import com.ima.domain.Checks;
import com.ima.domain.Page;
import com.ima.persistence.ReportEntity;
import com.ima.service.ReportType;
import com.ima.service.SavedReportService;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("persistent")
@RequestMapping(value = "/api/v1/apps/{applicationId}/saved-reports", produces = "application/json")
public final class SavedReportController {
    private final SavedReportService reports;

    public SavedReportController(SavedReportService reports) {
        this.reports = reports;
    }

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json")
    public ReportEntity create(@PathVariable("applicationId") String applicationId,
                                @RequestBody SavedReportInput input, HttpServletRequest request) {
        ReportType type;
        try {
            type = ReportType.valueOf(Checks.required(input.getType(), "type"));
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("unknown report type");
        }
        return reports.create(RequestContext.principal(request), applicationId, input.getName(), type,
                Checks.required(input.getDefinition(), "definition").toString());
    }

    @RequestMapping(method = RequestMethod.GET)
    public Page<ReportEntity> list(@PathVariable("applicationId") String applicationId,
                                   @RequestParam(value = "offset", defaultValue = "0") int offset,
                                   @RequestParam(value = "limit", defaultValue = "20") int limit,
                                   HttpServletRequest request) {
        return reports.list(RequestContext.principal(request), applicationId, offset, limit);
    }

    @RequestMapping(value = "/{reportId}", method = RequestMethod.GET)
    public ReportEntity find(@PathVariable("applicationId") String applicationId,
                              @PathVariable("reportId") String reportId, HttpServletRequest request) {
        return reports.find(RequestContext.principal(request), applicationId, reportId);
    }

    @RequestMapping(value = "/{reportId}", method = RequestMethod.DELETE)
    public Map<String, Boolean> delete(@PathVariable("applicationId") String applicationId,
                                        @PathVariable("reportId") String reportId, HttpServletRequest request) {
        return Collections.singletonMap("deleted", reports.delete(RequestContext.principal(request), applicationId, reportId));
    }

    @RequestMapping(value = "/{reportId}/result", method = RequestMethod.GET)
    public Object run(@PathVariable("applicationId") String applicationId,
                       @PathVariable("reportId") String reportId, HttpServletRequest request) {
        return reports.run(RequestContext.principal(request), applicationId, reportId);
    }
}
