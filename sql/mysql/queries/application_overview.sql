SELECT
    application_id,
    metric_day,
    event_count,
    active_users,
    new_users,
    session_count,
    bounce_count,
    CAST(bounce_count AS DECIMAL(20, 6)) / NULLIF(session_count, 0) AS bounce_rate,
    CAST(session_duration_total AS DECIMAL(20, 6)) / NULLIF(session_count, 0) AS average_session_millis,
    CAST(session_count AS DECIMAL(20, 6)) / NULLIF(active_users, 0) AS sessions_per_user,
    crash_count,
    1 - CAST(affected_users AS DECIMAL(20, 6)) / NULLIF(active_users, 0) AS crash_free_user_rate
FROM daily_application_metrics
WHERE application_id = ?
  AND metric_day >= ?
  AND metric_day < ?
ORDER BY metric_day;
