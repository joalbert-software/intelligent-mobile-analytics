SELECT
    application_id,
    day_offset,
    SUM(cohort_users) AS eligible_users,
    SUM(retained_users) AS retained_users,
    CAST(SUM(retained_users) AS DECIMAL(20, 6)) / NULLIF(SUM(cohort_users), 0) AS retention_rate
FROM retention_cohorts
WHERE application_id = ?
  AND cohort_day >= ?
  AND cohort_day < ?
  AND day_offset <= ?
GROUP BY application_id, day_offset
ORDER BY day_offset;
