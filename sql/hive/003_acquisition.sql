INSERT OVERWRITE TABLE ima.user_acquisition
SELECT
    application_id,
    user_id,
    MIN(event_timestamp) AS first_timestamp,
    CAST(FLOOR(MIN(event_timestamp) / 86400000) * 86400000 AS BIGINT) AS cohort_day
FROM ima.unique_events
WHERE event_timestamp < ${hiveconf:observed_until}
GROUP BY application_id, user_id;
