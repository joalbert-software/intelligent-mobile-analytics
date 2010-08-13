package com.ima.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.domain.Checks;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class AnalyticsClient {
    private final ClientOptions options;
    private final ObjectMapper mapper;
    private final RetryPolicy retries;

    public AnalyticsClient(ClientOptions options) {
        this.options = Checks.required(options, "options");
        this.mapper = new ObjectMapper();
        this.retries = new RetryPolicy(500, 30000, new Random());
    }

    public List<DeliveryOutcome> send(List<ClientEvent> events) throws IOException, InterruptedException {
        Checks.bounded(events, "events", 1, 500);
        List<ClientEvent> pending = new ArrayList<ClientEvent>(events);
        Set<String> identifiers = new HashSet<String>();
        for (ClientEvent event : pending) {
            Checks.required(event, "event");
            if (!identifiers.add(event.getEventId())) {
                throw new IllegalArgumentException("a client batch must have distinct event IDs");
            }
        }
        List<DeliveryOutcome> completed = new ArrayList<DeliveryOutcome>();
        for (int attempt = 0; attempt < options.getMaximumAttempts(); attempt++) {
            long serverHint = -1;
            try {
                List<DeliveryOutcome> response = post(pending);
                List<ClientEvent> retry = new ArrayList<ClientEvent>();
                for (int index = 0; index < response.size(); index++) {
                    DeliveryOutcome outcome = response.get(index);
                    if (outcome.isRetryable()) {
                        retry.add(pending.get(index));
                    } else {
                        completed.add(outcome);
                    }
                }
                pending = retry;
                if (pending.isEmpty()) {
                    return ordered(events, completed);
                }
            } catch (ClientException failure) {
                if (!failure.isRetryable()) {
                    throw failure;
                }
                serverHint = failure.getRetryAfterMillis();
            } catch (IOException failure) {
                // Stable IDs allow retry after a lost response without double ingestion.
            }
            if (attempt + 1 < options.getMaximumAttempts()) {
                Thread.sleep(retries.delay(attempt, serverHint));
            }
        }
        for (ClientEvent event : pending) {
            completed.add(new DeliveryOutcome(event.getEventId(), "UNAVAILABLE", "retry_budget_exhausted"));
        }
        return ordered(events, completed);
    }

    private List<DeliveryOutcome> ordered(List<ClientEvent> original, List<DeliveryOutcome> values) {
        java.util.Map<String, DeliveryOutcome> byId = new java.util.HashMap<String, DeliveryOutcome>();
        for (DeliveryOutcome value : values) {
            byId.put(value.getEventId(), value);
        }
        List<DeliveryOutcome> result = new ArrayList<DeliveryOutcome>();
        for (ClientEvent event : original) {
            result.add(byId.get(event.getEventId()));
        }
        return result;
    }

    private List<DeliveryOutcome> post(List<ClientEvent> events) throws IOException {
        byte[] payload = mapper.writeValueAsBytes(events);
        if (payload.length > 1048576) {
            throw new ClientException("serialized batch exceeds the server limit", 0, false, -1);
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(options.eventEndpoint()).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(options.getConnectTimeoutMillis());
        connection.setReadTimeout(options.getReadTimeoutMillis());
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", options.authorization());
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "IMA-Java/1.0");
        connection.setFixedLengthStreamingMode(payload.length);
        connection.setDoOutput(true);
        try {
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            int status = connection.getResponseCode();
            if (status != 200 && status != 207) {
                boolean retryable = status == 429 || status == 502 || status == 503 || status == 504;
                throw new ClientException("ingestion returned HTTP " + status, status, retryable,
                        RetryPolicy.parseRetryAfter(connection.getHeaderField("Retry-After")));
            }
            byte[] body;
            try (InputStream input = connection.getInputStream()) {
                body = boundedRead(input);
            }
            return parse(body, events);
        } finally {
            connection.disconnect();
        }
    }

    private byte[] boundedRead(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = input.read(buffer)) != -1) {
            if (output.size() + length > 1048576) {
                throw new ClientException("server response exceeds size limit", 0, false, -1);
            }
            output.write(buffer, 0, length);
        }
        return output.toByteArray();
    }

    private List<DeliveryOutcome> parse(byte[] body, List<ClientEvent> events) throws IOException {
        JsonNode root = mapper.readTree(body);
        JsonNode items = root == null ? null : root.get("items");
        if (items == null || !items.isArray() || items.size() != events.size()) {
            throw new ClientException("invalid server result shape", 0, false, -1);
        }
        DeliveryOutcome[] values = new DeliveryOutcome[events.size()];
        Set<String> allowed = new HashSet<String>(Arrays.asList("CREATED", "DUPLICATE", "INVALID", "CONFLICT", "UNAVAILABLE"));
        for (JsonNode item : items) {
            JsonNode indexNode = item.get("index");
            if (indexNode == null || !indexNode.isInt()) {
                throw new ClientException("invalid result index", 0, false, -1);
            }
            int index = indexNode.intValue();
            if (index < 0 || index >= values.length || values[index] != null) {
                throw new ClientException("invalid result index", 0, false, -1);
            }
            String status = item.path("status").asText();
            String eventId = item.path("eventId").asText();
            if (!allowed.contains(status) || !events.get(index).getEventId().equals(eventId)) {
                throw new ClientException("invalid result identity or status", 0, false, -1);
            }
            values[index] = new DeliveryOutcome(eventId, status,
                    item.path("error").isTextual() ? item.path("error").asText() : null);
        }
        return new ArrayList<DeliveryOutcome>(Arrays.asList(values));
    }
}
