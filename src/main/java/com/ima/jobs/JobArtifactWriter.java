package com.ima.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ima.domain.Checks;
import com.ima.persistence.JobEntity;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Each attempt gets a separate artifact; consumers must check the job's state. */
public final class JobArtifactWriter {
    private final Path root;
    private final ObjectMapper mapper;

    public JobArtifactWriter(Path root, ObjectMapper mapper) {
        this.root = Checks.required(root, "root").toAbsolutePath().normalize();
        this.mapper = Checks.required(mapper, "mapper");
    }

    public Path write(JobEntity job, Object report) throws IOException {
        Checks.identifier(job.getId(), "jobId");
        Checks.identifier(job.getApplicationId(), "applicationId");
        Files.createDirectories(root);
        Path application = root.resolve(job.getApplicationId()).normalize();
        if (!application.startsWith(root) || Files.isSymbolicLink(application)) {
            throw new IllegalArgumentException("invalid artifact application directory");
        }
        Files.createDirectories(application);
        String filename = job.getId() + "-attempt-" + job.getAttempts() + ".json";
        Path destination = application.resolve(filename);
        Path temporary = Files.createTempFile(application, ".ima-", ".partial");
        Map<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("formatVersion", 1);
        artifact.put("applicationId", job.getApplicationId());
        artifact.put("jobId", job.getId());
        artifact.put("attempt", job.getAttempts());
        artifact.put("type", job.getType());
        artifact.put("rangeStart", job.getRangeStart());
        artifact.put("rangeEnd", job.getRangeEnd());
        artifact.put("result", report);
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                mapper.writeValue(output, artifact);
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            return destination;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
