package com.ima.client;

import com.ima.domain.Checks;
import java.net.URI;
import java.net.URISyntaxException;

public final class ClientOptions {
    private final URI endpoint;
    private final String applicationId;
    private final String token;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final int maximumAttempts;

    public ClientOptions(String endpoint, String applicationId, String token,
                         int connectTimeoutMillis, int readTimeoutMillis, int maximumAttempts) {
        try {
            this.endpoint = new URI(Checks.required(endpoint, "endpoint"));
        } catch (URISyntaxException failure) {
            throw new IllegalArgumentException("invalid endpoint");
        }
        String host = this.endpoint.getHost();
        String scheme = this.endpoint.getScheme();
        boolean local = "localhost".equals(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
        if (host == null || !("https".equals(scheme) || ("http".equals(scheme) && local))) {
            throw new IllegalArgumentException("remote endpoints require HTTPS");
        }
        if (this.endpoint.getUserInfo() != null || this.endpoint.getQuery() != null
                || this.endpoint.getFragment() != null) {
            throw new IllegalArgumentException("endpoint contains forbidden components");
        }
        this.applicationId = Checks.identifier(applicationId, "applicationId");
        this.token = Checks.text(token, "token", 256);
        if (token.length() < 32) {
            throw new IllegalArgumentException("token is too short");
        }
        this.connectTimeoutMillis = Checks.range(connectTimeoutMillis, "connectTimeoutMillis", 1, 120000);
        this.readTimeoutMillis = Checks.range(readTimeoutMillis, "readTimeoutMillis", 1, 120000);
        this.maximumAttempts = Checks.range(maximumAttempts, "maximumAttempts", 1, 10);
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public String getApplicationId() {
        return applicationId;
    }

    String authorization() {
        return "Bearer " + token;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public int getMaximumAttempts() {
        return maximumAttempts;
    }

    public String eventEndpoint() {
        String base = endpoint.toString();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v1/apps/" + applicationId + "/events";
    }
}
