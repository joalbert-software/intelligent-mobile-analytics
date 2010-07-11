CREATE TABLE batch_jobs (
    job_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    job_type VARCHAR(32) CHARACTER SET ascii NOT NULL,
    job_state VARCHAR(32) CHARACTER SET ascii NOT NULL,
    range_start BIGINT NOT NULL,
    range_end BIGINT NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    lease_owner VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    lease_until BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    failure_code VARCHAR(64) CHARACTER SET ascii NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (job_id),
    KEY jobs_claimable (job_state, lease_until, created_at),
    KEY jobs_by_application (application_id, created_at),
    CONSTRAINT jobs_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE batch_artifacts (
    artifact_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    job_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    content_sha256 VARCHAR(64) CHARACTER SET ascii NOT NULL,
    content_type VARCHAR(128) CHARACTER SET ascii NOT NULL,
    byte_count BIGINT NOT NULL,
    row_count BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (artifact_id),
    KEY artifacts_by_job (job_id),
    CONSTRAINT artifacts_job_fk FOREIGN KEY (job_id)
        REFERENCES batch_jobs (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_migrations (version, description, applied_at)
VALUES (2, 'Leased batch jobs and immutable artifact manifests', UNIX_TIMESTAMP() * 1000);
