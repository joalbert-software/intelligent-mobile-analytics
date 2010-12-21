package com.ima.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.cache.ReportCache;
import com.ima.security.ApiKeyHasher;
import com.ima.security.Authenticator;
import com.ima.security.RateLimiter;
import com.ima.security.TokenBucketLimiter;
import com.ima.service.AnalyticsService;
import com.ima.service.CachedReportService;
import com.ima.service.CredentialService;
import com.ima.service.IngestionPolicy;
import com.ima.service.IngestionService;
import com.ima.service.PrivacyService;
import com.ima.store.CredentialStore;
import com.ima.store.EventStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({DemoConfiguration.class, PersistentConfiguration.class})
public class ApplicationConfiguration {
    @Bean
    public Settings settings() {
        return new Settings(System.getenv());
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
        return mapper;
    }

    @Bean
    public ApiKeyHasher apiKeyHasher() {
        return new ApiKeyHasher();
    }

    @Bean
    public Authenticator authenticator(CredentialStore store, ApiKeyHasher hasher, Clock clock) {
        return new Authenticator(store, hasher, clock);
    }

    @Bean
    public RateLimiter rateLimiter(Clock clock, Settings settings) {
        return new TokenBucketLimiter(settings.integer("IMA_RATE_CAPACITY", 1000, 500, 1000000),
                settings.integer("IMA_RATE_PER_SECOND", 100, 1, 1000000), 10000, clock);
    }

    @Bean
    public IngestionService ingestionService(EventStore store, RateLimiter limiter, Clock clock) {
        return new IngestionService(store, IngestionPolicy.defaults(), limiter, clock);
    }

    @Bean
    public AnalyticsService analyticsService(EventStore store) {
        return new AnalyticsService(store);
    }

    @Bean
    public CachedReportService cachedReportService(ReportCache cache, ObjectMapper mapper) {
        return new CachedReportService(cache, mapper, 30);
    }

    @Bean
    public CredentialService credentialService(CredentialStore store, ApiKeyHasher hasher, Clock clock) {
        return new CredentialService(store, hasher, clock);
    }

    @Bean
    public PrivacyService privacyService(EventStore store) {
        return new PrivacyService(store);
    }
}
