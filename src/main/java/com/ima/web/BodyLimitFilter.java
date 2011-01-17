package com.ima.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded buffering also enforces the limit for chunked requests. */
public final class BodyLimitFilter extends OncePerRequestFilter {
    private final int maximumBytes;

    public BodyLimitFilter(int maximumBytes) {
        if (maximumBytes < 1) {
            throw new IllegalArgumentException("maximumBytes must be positive");
        }
        this.maximumBytes = maximumBytes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())
                || "PATCH".equals(request.getMethod()))) {
            chain.doFilter(request, response);
            return;
        }
        if (request.getContentLengthLong() > maximumBytes) {
            reject(response);
            return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int size;
        while ((size = request.getInputStream().read(buffer)) != -1) {
            if (size > maximumBytes - bytes.size()) {
                reject(response);
                return;
            }
            bytes.write(buffer, 0, size);
        }
        final byte[] body = bytes.toByteArray();
        HttpServletRequestWrapper wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                final ByteArrayInputStream input = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override
                    public int read() {
                        return input.read();
                    }

                    @Override
                    public int read(byte[] destination, int offset, int length) {
                        return input.read(destination, offset, length);
                    }

                    @Override
                    public boolean isFinished() {
                        return input.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        throw new UnsupportedOperationException("asynchronous request bodies are not supported");
                    }
                };
            }

            @Override
            public int getContentLength() {
                return body.length;
            }

            @Override
            public long getContentLengthLong() {
                return body.length;
            }
        };
        chain.doFilter(wrapped, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(413);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"body_too_large\",\"message\":\"request exceeds body limit\"}");
    }
}
