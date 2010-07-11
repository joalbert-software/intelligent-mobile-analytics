CREATE TABLE schema_migrations (
    version INTEGER NOT NULL,
    description VARCHAR(160) NOT NULL,
    applied_at BIGINT NOT NULL,
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE applications (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    owner_reference VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    retention_days INTEGER NOT NULL DEFAULT 365,
    session_gap_millis BIGINT NOT NULL DEFAULT 1800000,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (application_id),
    KEY applications_by_owner (owner_reference, application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE api_credentials (
    key_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    key_digest VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    scopes VARCHAR(64) CHARACTER SET ascii NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (key_id),
    UNIQUE KEY credentials_digest (key_digest),
    KEY credentials_by_application (application_id, revoked, expires_at),
    CONSTRAINT credentials_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE saved_reports (
    report_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    report_name VARCHAR(160) NOT NULL,
    report_type VARCHAR(32) CHARACTER SET ascii NOT NULL,
    definition_json LONGTEXT NOT NULL,
    created_by VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (report_id),
    KEY reports_by_application (application_id, report_id),
    CONSTRAINT reports_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE audit_records (
    audit_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor_key_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    action_name VARCHAR(64) CHARACTER SET ascii NOT NULL,
    resource_reference VARCHAR(160) NOT NULL,
    occurred_at BIGINT NOT NULL,
    request_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    successful BOOLEAN NOT NULL,
    PRIMARY KEY (audit_id),
    KEY audit_by_application_time (application_id, occurred_at, audit_id),
    CONSTRAINT audit_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_migrations (version, description, applied_at)
VALUES (1, 'Application metadata, credentials, reports, and audits', UNIX_TIMESTAMP() * 1000);
