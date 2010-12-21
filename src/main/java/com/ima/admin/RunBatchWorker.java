package com.ima.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.config.ApplicationConfiguration;
import com.ima.config.Settings;
import com.ima.jobs.JobArtifactWriter;
import com.ima.jobs.LocalBatchWorker;
import com.ima.persistence.HibernateTransactions;
import com.ima.persistence.JobRepository;
import com.ima.store.EventStore;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class RunBatchWorker {
    private RunBatchWorker() {
    }

    public static void main(String[] arguments) {
        int maximumJobs = 10;
        if (arguments.length > 1) {
            throw new IllegalArgumentException("usage: RunBatchWorker [MAXIMUM_JOBS]");
        }
        if (arguments.length == 1) {
            maximumJobs = com.ima.domain.Checks.range(Integer.parseInt(arguments[0]), "maximumJobs", 1, 10000);
        }
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("persistent");
            context.register(ApplicationConfiguration.class);
            context.refresh();
            Settings settings = context.getBean(Settings.class);
            JobRepository jobs = new JobRepository(context.getBean(HibernateTransactions.class));
            JobArtifactWriter artifacts = new JobArtifactWriter(Paths.get(settings.required("IMA_ARTIFACT_DIRECTORY")),
                    context.getBean(ObjectMapper.class));
            LocalBatchWorker worker = new LocalBatchWorker(jobs, context.getBean(EventStore.class), artifacts,
                    context.getBean(Clock.class), "worker-" + UUID.randomUUID().toString());
            int completed = 0;
            while (completed < maximumJobs && !Thread.currentThread().isInterrupted() && worker.runOne()) {
                completed++;
            }
            System.out.println("Processed job attempts: " + completed);
        }
    }
}
