WITH cohort_sizes AS (
    SELECT
        application_id,
        cohort_day,
        COUNT(*) AS cohort_users
    FROM ima.user_acquisition
    GROUP BY application_id, cohort_day
),
returning_users AS (
    SELECT
        e.application_id,
        a.cohort_day,
        CAST(FLOOR(e.event_timestamp / 86400000) - a.cohort_day / 86400000 AS INT) AS day_offset,
        COUNT(DISTINCT e.user_id) AS retained_users
    FROM ima.unique_events e
    JOIN ima.user_acquisition a
      ON e.application_id = a.application_id
     AND e.user_id = a.user_id
    WHERE e.event_timestamp < ${hiveconf:observed_until}
      AND FLOOR(e.event_timestamp / 86400000) < FLOOR(${hiveconf:observed_until} / 86400000)
    GROUP BY e.application_id, a.cohort_day,
        CAST(FLOOR(e.event_timestamp / 86400000) - a.cohort_day / 86400000 AS INT)
)
SELECT
    r.application_id,
    r.cohort_day,
    r.day_offset,
    c.cohort_users,
    r.retained_users,
    CAST(r.retained_users AS DOUBLE) / c.cohort_users AS retention_rate
FROM returning_users r
JOIN cohort_sizes c
  ON r.application_id = c.application_id
 AND r.cohort_day = c.cohort_day
WHERE r.day_offset <= ${hiveconf:maximum_day}
ORDER BY r.application_id, r.cohort_day, r.day_offset;
