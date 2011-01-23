package com.ima.web;

import com.ima.service.EventInput;
import com.ima.service.IngestionResult;
import com.ima.service.IngestionService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/apps/{applicationId}/events")
public final class EventController {
    private final IngestionService ingestion;

    public EventController(IngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @RequestMapping(method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    public ResponseEntity<IngestionResult> ingest(@PathVariable("applicationId") String applicationId,
                                                 @RequestBody List<EventInput> events,
                                                 HttpServletRequest request) {
        IngestionResult result = ingestion.ingest(RequestContext.principal(request), applicationId, events);
        // 207 always contains authoritative item-level outcomes, including retryable failures.
        return ResponseEntity.status(result.getRejected() == 0 ? 200 : 207).body(result);
    }
}
