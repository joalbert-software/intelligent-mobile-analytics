package com.ima.config;

import com.ima.cache.RedisReportCache;
import com.ima.cache.ReportCache;
import com.ima.persistence.ApplicationEntity;
import com.ima.persistence.AuditEntity;
import com.ima.persistence.CredentialEntity;
import com.ima.persistence.HibernateCredentialStore;
import com.ima.persistence.HibernateTransactions;
import com.ima.persistence.JobEntity;
import com.ima.persistence.ReportEntity;
import com.ima.store.CredentialStore;
import com.ima.store.EventStore;
import com.ima.store.MongoEventStore;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import java.time.Clock;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@Profile("persistent")
public class PersistentConfiguration {
    @Bean
    public com.ima.service.JobService jobService(HibernateTransactions transactions, Clock clock) {
        return new com.ima.service.JobService(new com.ima.persistence.JobRepository(transactions), clock);
    }

    @Bean
    public com.ima.service.ApplicationService applicationService(HibernateTransactions transactions, Clock clock) {
        return new com.ima.service.ApplicationService(new com.ima.persistence.ApplicationRepository(transactions), clock);
    }

    @Bean
    public com.ima.service.SavedReportService savedReportService(HibernateTransactions transactions,
            com.ima.service.AnalyticsService analytics, com.fasterxml.jackson.databind.ObjectMapper mapper,
            Clock clock) {
        return new com.ima.service.SavedReportService(new com.ima.persistence.ReportRepository(transactions),
                analytics, mapper, clock);
    }

    @Bean(destroyMethod = "close")
    public MongoClient mongoClient(Settings settings) {
        return new MongoClient(new MongoClientURI(settings.required("IMA_MONGO_URI")));
    }

    @Bean
    public EventStore eventStore(MongoClient client, Settings settings) {
        MongoEventStore store = new MongoEventStore(client.getDatabase(
                settings.optional("IMA_MONGO_DATABASE", "ima")));
        store.ensureIndexes();
        return store;
    }

    @Bean(destroyMethod = "close")
    public SessionFactory sessionFactory(Settings settings) {
        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();
        configuration.setProperty("hibernate.connection.driver_class", "com.mysql.jdbc.Driver");
        configuration.setProperty("hibernate.connection.url", settings.required("IMA_MYSQL_URL"));
        configuration.setProperty("hibernate.connection.username", settings.required("IMA_MYSQL_USER"));
        configuration.setProperty("hibernate.connection.password", settings.required("IMA_MYSQL_PASSWORD"));
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
        configuration.setProperty("hibernate.hbm2ddl.auto", "validate");
        configuration.setProperty("hibernate.show_sql", "false");
        configuration.setProperty("hibernate.connection.pool_size", "10");
        configuration.addAnnotatedClass(ApplicationEntity.class);
        configuration.addAnnotatedClass(CredentialEntity.class);
        configuration.addAnnotatedClass(ReportEntity.class);
        configuration.addAnnotatedClass(AuditEntity.class);
        configuration.addAnnotatedClass(JobEntity.class);
        return configuration.buildSessionFactory();
    }

    @Bean
    public HibernateTransactions transactions(SessionFactory sessions) {
        return new HibernateTransactions(sessions);
    }

    @Bean
    public CredentialStore credentialStore(HibernateTransactions transactions, Clock clock) {
        return new HibernateCredentialStore(transactions, clock);
    }

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool(Settings settings) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(16);
        config.setMaxIdle(8);
        config.setMaxWaitMillis(1000);
        return new JedisPool(config, settings.optional("IMA_REDIS_HOST", "127.0.0.1"),
                settings.integer("IMA_REDIS_PORT", 6379, 1, 65535), 2000,
                settings.required("IMA_REDIS_PASSWORD"));
    }

    @Bean
    public ReportCache reportCache(JedisPool pool) {
        return new RedisReportCache(pool);
    }
}
