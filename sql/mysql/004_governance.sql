CREATE TABLE event_definitions (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_name VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    schema_version INTEGER NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (application_id, event_name),
    CONSTRAINT event_definition_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE property_definitions (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_name VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    property_name VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    value_type VARCHAR(32) CHARACTER SET ascii NOT NULL,
    required_property BOOLEAN NOT NULL,
    sensitive_property BOOLEAN NOT NULL,
    allowed_values TEXT NULL,
    PRIMARY KEY (application_id, event_name, property_name),
    CONSTRAINT property_definition_event_fk FOREIGN KEY (application_id, event_name)
        REFERENCES event_definitions (application_id, event_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE deletion_requests (
    request_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject_reference VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_state VARCHAR(32) CHARACTER SET ascii NOT NULL,
    requested_at BIGINT NOT NULL,
    completed_at BIGINT NULL,
    removed_events BIGINT NOT NULL DEFAULT 0,
    raw_store_complete BOOLEAN NOT NULL DEFAULT FALSE,
    warehouse_complete BOOLEAN NOT NULL DEFAULT FALSE,
    cache_complete BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (request_id),
    KEY deletion_by_application (application_id, requested_at),
    CONSTRAINT deletion_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE data_quality_runs (
    run_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_sha256 VARCHAR(64) CHARACTER SET ascii NOT NULL,
    checked_at BIGINT NOT NULL,
    total_rows BIGINT NOT NULL,
    invalid_rows BIGINT NOT NULL,
    duplicate_rows BIGINT NOT NULL,
    conflicting_rows BIGINT NOT NULL,
    passed BOOLEAN NOT NULL,
    report_json LONGTEXT NOT NULL,
    PRIMARY KEY (run_id),
    KEY quality_by_application (application_id, checked_at),
    CONSTRAINT quality_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_migrations (version, description, applied_at)
VALUES (4, 'Event catalog, deletion tracking, and quality manifests', UNIX_TIMESTAMP() * 1000);
