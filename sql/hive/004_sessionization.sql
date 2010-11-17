WITH ordered_events AS (
    SELECT
        application_id,
        user_id,
        event_id,
        event_name,
        event_timestamp,
        LAG(event_timestamp) OVER (
            PARTITION BY application_id, user_id
            ORDER BY event_timestamp, event_id
        ) AS previous_timestamp
    FROM ima.unique_events
    WHERE event_timestamp >= ${hiveconf:range_start}
      AND event_timestamp < ${hiveconf:range_end}
),
boundaries AS (
    SELECT
        application_id,
        user_id,
        event_id,
        event_name,
        event_timestamp,
        CASE WHEN previous_timestamp IS NULL
            OR event_timestamp - previous_timestamp > ${hiveconf:session_gap}
            THEN 1 ELSE 0 END AS new_session
    FROM ordered_events
),
numbered AS (
    SELECT
        application_id,
        user_id,
        event_id,
        event_name,
        event_timestamp,
        SUM(new_session) OVER (
            PARTITION BY application_id, user_id
            ORDER BY event_timestamp, event_id
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS session_number
    FROM boundaries
)
SELECT
    application_id,
    user_id,
    session_number,
    MIN(event_timestamp) AS session_start,
    MAX(event_timestamp) AS session_end,
    MAX(event_timestamp) - MIN(event_timestamp) AS duration_millis,
    COUNT(*) AS event_count,
    SUM(CASE WHEN event_name = 'crash' THEN 1 ELSE 0 END) AS crash_count
FROM numbered
GROUP BY application_id, user_id, session_number;
