CREATE TABLE daily_application_metrics (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    metric_day DATE NOT NULL,
    event_count BIGINT NOT NULL,
    active_users BIGINT NOT NULL,
    new_users BIGINT NOT NULL,
    session_count BIGINT NOT NULL,
    bounce_count BIGINT NOT NULL,
    session_duration_total BIGINT NOT NULL,
    crash_count BIGINT NOT NULL,
    affected_users BIGINT NOT NULL,
    computed_at BIGINT NOT NULL,
    source_job_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (application_id, metric_day),
    CONSTRAINT daily_metrics_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE daily_event_metrics (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    metric_day DATE NOT NULL,
    event_name VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    platform VARCHAR(16) CHARACTER SET ascii NOT NULL,
    event_count BIGINT NOT NULL,
    distinct_users BIGINT NOT NULL,
    computed_at BIGINT NOT NULL,
    source_job_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (application_id, metric_day, event_name, platform),
    CONSTRAINT event_metrics_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE daily_revenue_metrics (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    metric_day DATE NOT NULL,
    currency CHAR(3) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    gross_minor BIGINT NOT NULL,
    refund_minor BIGINT NOT NULL,
    purchase_count BIGINT NOT NULL,
    refund_count BIGINT NOT NULL,
    purchaser_count BIGINT NOT NULL,
    computed_at BIGINT NOT NULL,
    source_job_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (application_id, metric_day, currency),
    CONSTRAINT revenue_metrics_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE retention_cohorts (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    cohort_day DATE NOT NULL,
    day_offset INTEGER NOT NULL,
    cohort_users BIGINT NOT NULL,
    retained_users BIGINT NOT NULL,
    observed_until BIGINT NOT NULL,
    computed_at BIGINT NOT NULL,
    source_job_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (application_id, cohort_day, day_offset),
    CONSTRAINT cohorts_application_fk FOREIGN KEY (application_id)
        REFERENCES applications (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE funnel_snapshots (
    application_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    report_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    range_start BIGINT NOT NULL,
    range_end BIGINT NOT NULL,
    step_index INTEGER NOT NULL,
    step_name VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_count BIGINT NOT NULL,
    computed_at BIGINT NOT NULL,
    source_job_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (application_id, report_id, range_start, range_end, step_index),
    CONSTRAINT funnel_snapshot_report_fk FOREIGN KEY (report_id)
        REFERENCES saved_reports (report_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_migrations (version, description, applied_at)
VALUES (3, 'Rebuildable reporting aggregates', UNIX_TIMESTAMP() * 1000);
