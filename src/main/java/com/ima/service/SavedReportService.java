package com.ima.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.analytics.Dimension;
import com.ima.domain.Checks;
import com.ima.domain.Page;
import com.ima.persistence.ReportEntity;
import com.ima.persistence.ReportRepository;
import com.ima.security.Principal;
import com.ima.security.Scope;
import java.time.Clock;
import java.util.UUID;

public final class SavedReportService {
    private final ReportRepository reports;
    private final AnalyticsService analytics;
    private final ObjectMapper mapper;
    private final Clock clock;

    public SavedReportService(ReportRepository reports, AnalyticsService analytics, ObjectMapper mapper, Clock clock) {
        this.reports = reports;
        this.analytics = analytics;
        this.mapper = mapper;
        this.clock = clock;
    }

    public ReportEntity create(Principal principal, String applicationId,
                               String name, ReportType type, String json) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.text(name, "name", 160);
        ReportDefinition definition = new ReportDefinition(type, json, mapper);
        ReportEntity report = new ReportEntity();
        report.setId(UUID.randomUUID().toString());
        report.setApplicationId(applicationId);
        report.setName(name);
        report.setType(type.name());
        report.setDefinition(definition.toJson());
        report.setCreatedBy(principal.getKeyId());
        report.setCreatedAt(clock.millis());
        report.setUpdatedAt(report.getCreatedAt());
        reports.create(report);
        return report;
    }

    public Page<ReportEntity> list(Principal principal, String applicationId, int offset, int limit) {
        principal.require(applicationId, Scope.READ);
        return reports.list(applicationId, offset, limit);
    }

    public ReportEntity find(Principal principal, String applicationId, String reportId) {
        principal.require(applicationId, Scope.READ);
        ReportEntity report = reports.find(applicationId, reportId);
        if (report == null) {
            throw new ResourceNotFoundException("report not found");
        }
        return report;
    }

    public boolean delete(Principal principal, String applicationId, String reportId) {
        principal.require(applicationId, Scope.ADMIN);
        Checks.identifier(reportId, "reportId");
        return reports.delete(applicationId, reportId);
    }

    public Object run(Principal principal, String applicationId, String reportId) {
        ReportEntity report = find(principal, applicationId, reportId);
        ReportDefinition definition = new ReportDefinition(ReportType.valueOf(report.getType()),
                report.getDefinition(), mapper);
        switch (definition.getType()) {
            case SERIES:
                return analytics.series(principal, applicationId, definition.range(), definition.number("width", 86400000));
            case SESSIONS:
                return analytics.sessions(principal, applicationId, definition.range(), definition.number("gap", 1800000));
            case FUNNEL:
                return analytics.funnel(principal, applicationId, definition.range(), definition.funnel());
            case RETENTION:
                return analytics.retention(principal, applicationId, definition.requiredNumber("observedUntil"),
                        (int) definition.number("maximumDay", 30));
            case REVENUE:
                return analytics.revenue(principal, applicationId, definition.range());
            case BREAKDOWN:
                return analytics.breakdown(principal, applicationId, definition.range(),
                        new Dimension(definition.requiredText("dimension")), (int) definition.number("limit", 20));
            case CRASHES:
                return analytics.crashes(principal, applicationId, definition.range(), definition.number("gap", 1800000));
            case ENGAGEMENT:
                return analytics.engagement(principal, applicationId, definition.range());
            default:
                throw new IllegalArgumentException("unsupported saved report type");
        }
    }
}
