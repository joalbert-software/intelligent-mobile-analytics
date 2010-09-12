package com.ima.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.analytics.Dimension;
import com.ima.analytics.FunnelDefinition;
import com.ima.domain.Checks;
import com.ima.domain.TimeRange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class ReportDefinition {
    private final ReportType type;
    private final JsonNode parameters;

    public ReportDefinition(ReportType type, String json, ObjectMapper mapper) {
        this.type = Checks.required(type, "type");
        Checks.required(json, "definition");
        if (json.length() > 16000) {
            throw new IllegalArgumentException("report definition is too large");
        }
        try {
            this.parameters = mapper.readTree(json);
        } catch (IOException failure) {
            throw new IllegalArgumentException("report definition must be JSON");
        }
        if (parameters == null || !parameters.isObject()) {
            throw new IllegalArgumentException("report definition must be an object");
        }
        validate();
    }

    private void validate() {
        Set<String> allowed = new HashSet<String>(Arrays.asList("start", "end"));
        switch (type) {
            case SERIES:
                allowed.add("width");
                Checks.positive(number("width", 86400000), "width");
                break;
            case SESSIONS:
            case CRASHES:
                allowed.add("gap");
                boundedNumber("gap", 1800000, 1, 86400000);
                break;
            case FUNNEL:
                allowed.add("steps");
                allowed.add("windowMillis");
                funnel();
                break;
            case RETENTION:
                allowed.clear();
                allowed.add("observedUntil");
                allowed.add("maximumDay");
                Checks.positive(requiredNumber("observedUntil"), "observedUntil");
                boundedNumber("maximumDay", 30, 0, 365);
                break;
            case BREAKDOWN:
                allowed.add("dimension");
                allowed.add("limit");
                new Dimension(requiredText("dimension"));
                boundedNumber("limit", 20, 1, 1000);
                break;
            default:
                break;
        }
        Iterator<String> names = parameters.fieldNames();
        while (names.hasNext()) {
            if (!allowed.contains(names.next())) {
                throw new IllegalArgumentException("unknown report parameter");
            }
        }
        if (type != ReportType.RETENTION) {
            range();
        }
    }

    public ReportType getType() {
        return type;
    }

    public String toJson() {
        return parameters.toString();
    }

    public long requiredNumber(String name) {
        JsonNode value = parameters.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
        return value.longValue();
    }

    public long number(String name, long fallback) {
        return parameters.has(name) ? requiredNumber(name) : fallback;
    }

    private long boundedNumber(String name, long fallback, long minimum, long maximum) {
        long value = number(name, fallback);
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " is outside its allowed range");
        }
        return value;
    }

    public String requiredText(String name) {
        JsonNode value = parameters.get(name);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException(name + " must be text");
        }
        return value.textValue();
    }

    public TimeRange range() {
        return new TimeRange(requiredNumber("start"), requiredNumber("end"));
    }

    public FunnelDefinition funnel() {
        JsonNode value = parameters.get("steps");
        if (value == null || !value.isArray()) {
            throw new IllegalArgumentException("steps must be an array");
        }
        List<String> steps = new ArrayList<String>();
        for (JsonNode step : value) {
            if (!step.isTextual()) {
                throw new IllegalArgumentException("funnel steps must be text");
            }
            steps.add(step.textValue());
        }
        return new FunnelDefinition(steps, requiredNumber("windowMillis"));
    }
}
