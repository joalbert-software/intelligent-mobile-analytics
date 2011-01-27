package com.ima.web;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public final class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(RequestContext.REQUEST_ID, requestId);
        response.setHeader("X-Request-ID", requestId);
        response.setHeader("X-Content-Type-Options", "nosniff");
        long started = System.nanoTime();
        com.ima.metrics.RequestMetrics.instance().started();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMicros = (System.nanoTime() - started) / 1000;
            com.ima.metrics.RequestMetrics.instance().completed(response.getStatus(), elapsedMicros);
            // Do not log authorization headers, query strings, URLs, or request bodies.
            LOG.info("request_id={} status={} duration_micros={}",
                    requestId, response.getStatus(), elapsedMicros);
        }
    }
}
