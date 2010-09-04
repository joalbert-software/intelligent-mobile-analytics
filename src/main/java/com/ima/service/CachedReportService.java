package com.ima.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.cache.CacheKey;
import com.ima.cache.ReportCache;
import com.ima.domain.Checks;
import com.ima.security.Principal;
import com.ima.security.Scope;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/** Reports may be stale for ttlSeconds; authorization always precedes cache lookup. */
public final class CachedReportService {
    private final ReportCache cache;
    private final ObjectMapper mapper;
    private final int ttlSeconds;

    public CachedReportService(ReportCache cache, ObjectMapper mapper, int ttlSeconds) {
        this.cache = Checks.required(cache, "cache");
        this.mapper = Checks.required(mapper, "mapper");
        this.ttlSeconds = Checks.range(ttlSeconds, "ttlSeconds", 1, 3600);
    }

    public String render(Principal principal, String applicationId, String type,
                         Map<String, String> parameters, Callable<?> computation) {
        principal.require(applicationId, Scope.READ);
        String canonical = serialize(new TreeMap<String, String>(parameters));
        String key = CacheKey.report(applicationId, type, canonical);
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Object result;
        try {
            result = computation.call();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("report computation failed", failure);
        }
        String json = serialize(result);
        cache.put(key, json, ttlSeconds);
        return json;
    }

    private String serialize(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("report serialization failed", failure);
        }
    }
}
