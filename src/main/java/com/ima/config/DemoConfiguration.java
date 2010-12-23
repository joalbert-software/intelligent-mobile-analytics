package com.ima.config;

import com.ima.cache.NoOpReportCache;
import com.ima.cache.ReportCache;
import com.ima.security.ApiCredential;
import com.ima.security.ApiKeyHasher;
import com.ima.security.Scope;
import com.ima.store.CredentialStore;
import com.ima.store.EventStore;
import com.ima.store.InMemoryCredentialStore;
import com.ima.store.InMemoryEventStore;
import java.time.Clock;
import java.util.EnumSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class DemoConfiguration {
    @Bean
    public EventStore eventStore() {
        return new InMemoryEventStore(100000);
    }

    @Bean
    public CredentialStore credentialStore(Settings settings, ApiKeyHasher hasher, Clock clock) {
        InMemoryCredentialStore store = new InMemoryCredentialStore();
        String application = settings.optional("IMA_DEMO_APPLICATION", "demo-app");
        String token = settings.required("IMA_DEMO_API_KEY");
        store.save(new ApiCredential("demo-key", application, hasher.digest(token),
                EnumSet.allOf(Scope.class), clock.millis() + 86400000L, false));
        return store;
    }

    @Bean
    public ReportCache reportCache() {
        return new NoOpReportCache();
    }
}
