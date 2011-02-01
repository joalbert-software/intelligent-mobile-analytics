package com.ima;

import com.ima.security.Principal;
import com.ima.security.RateDecision;
import com.ima.security.RateLimiter;
import com.ima.security.Scope;
import com.ima.service.EventInput;
import com.ima.service.IngestionPolicy;
import com.ima.service.IngestionResult;
import com.ima.service.IngestionService;
import com.ima.store.EventStore;
import com.ima.store.StoreUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

public class IngestionServiceTest {
    @Test
    public void unavailableStorageNeverReportsAccepted() {
        EventStore store = mock(EventStore.class);
        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.acquire("app", 1)).thenReturn(new RateDecision(true, 999, 0));
        when(store.save(any(com.ima.domain.Event.class))).thenThrow(new StoreUnavailableException("offline"));
        Clock clock = Clock.fixed(Instant.ofEpochMilli(100000), ZoneOffset.UTC);
        IngestionService service = new IngestionService(store, IngestionPolicy.defaults(), limiter, clock);
        EventInput event = new EventInput();
        event.setEventId("event");
        event.setUserId("user");
        event.setName("open");
        event.setTimestamp(100000L);
        event.setPlatform("ANDROID");
        event.setAppVersion("1.0");
        IngestionResult result = service.ingest(new Principal("app", "key", EnumSet.of(Scope.INGEST)),
                "app", Collections.singletonList(event));
        assertEquals(0, result.getAccepted());
        assertTrue(result.isRetryable());
        assertEquals("UNAVAILABLE", result.getItems().get(0).getStatus());
    }

    @Test
    public void crossApplicationWritesNeverTouchStorage() {
        EventStore store = mock(EventStore.class);
        RateLimiter limiter = mock(RateLimiter.class);
        IngestionService service = new IngestionService(store, IngestionPolicy.defaults(), limiter, Clock.systemUTC());
        try {
            service.ingest(new Principal("app", "key", EnumSet.of(Scope.INGEST)),
                    "other", Collections.singletonList(new EventInput()));
            fail("must reject cross-application write");
        } catch (com.ima.security.AccessDeniedException expected) {
            verifyZeroInteractions(store, limiter);
        }
    }
}
