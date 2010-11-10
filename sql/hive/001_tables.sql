CREATE DATABASE IF NOT EXISTS ima;

CREATE EXTERNAL TABLE IF NOT EXISTS ima.raw_events (
    application_id STRING,
    event_id STRING,
    user_id STRING,
    event_name STRING,
    event_timestamp BIGINT,
    received_at BIGINT,
    platform STRING,
    app_version STRING,
    properties MAP<STRING, STRING>
)
PARTITIONED BY (event_date STRING)
ROW FORMAT SERDE 'org.apache.hive.hcatalog.data.JsonSerDe'
STORED AS TEXTFILE
LOCATION '${hiveconf:raw_location}';

CREATE TABLE IF NOT EXISTS ima.unique_events (
    application_id STRING,
    event_id STRING,
    user_id STRING,
    event_name STRING,
    event_timestamp BIGINT,
    received_at BIGINT,
    platform STRING,
    app_version STRING,
    properties MAP<STRING, STRING>
)
PARTITIONED BY (event_date STRING)
STORED AS ORC;

CREATE TABLE IF NOT EXISTS ima.user_acquisition (
    application_id STRING,
    user_id STRING,
    first_timestamp BIGINT,
    cohort_day BIGINT
)
STORED AS ORC;

CREATE TABLE IF NOT EXISTS ima.daily_activity (
    application_id STRING,
    event_count BIGINT,
    active_users BIGINT
)
PARTITIONED BY (event_date STRING)
STORED AS ORC;

CREATE TABLE IF NOT EXISTS ima.daily_revenue (
    application_id STRING,
    currency STRING,
    gross_minor DECIMAL(38, 0),
    refund_minor DECIMAL(38, 0),
    purchases BIGINT,
    refunds BIGINT,
    purchasers BIGINT
)
PARTITIONED BY (event_date STRING)
STORED AS ORC;

CREATE TABLE IF NOT EXISTS ima.retention_counts (
    application_id STRING,
    cohort_day BIGINT,
    day_offset INT,
    cohort_users BIGINT,
    retained_users BIGINT
)
PARTITIONED BY (observation_date STRING)
STORED AS ORC;
