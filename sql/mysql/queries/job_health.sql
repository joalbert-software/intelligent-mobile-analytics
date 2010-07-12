SELECT
    application_id,
    job_state,
    COUNT(*) AS jobs,
    MIN(created_at) AS oldest_created_at,
    MAX(updated_at) AS most_recent_update,
    SUM(CASE WHEN job_state = 'RUNNING' AND lease_until <= ? THEN 1 ELSE 0 END) AS expired_leases,
    MAX(attempt_count) AS maximum_attempts
FROM batch_jobs
WHERE application_id = ?
  AND created_at >= ?
GROUP BY application_id, job_state
ORDER BY job_state;
