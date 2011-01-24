package com.ima.web;

import com.ima.store.EventStore;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class HealthController {
    private final EventStore events;

    public HealthController(EventStore events) {
        this.events = events;
    }

    @RequestMapping(value = "/healthz", method = RequestMethod.GET, produces = "application/json")
    public Map<String, String> live() {
        return Collections.singletonMap("status", "up");
    }

    @RequestMapping(value = "/readyz", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Map<String, String>> ready() {
        boolean ready = events.isAvailable();
        return ResponseEntity.status(ready ? 200 : 503)
                .body(Collections.singletonMap("eventStorage", ready ? "available" : "unavailable"));
    }
}
