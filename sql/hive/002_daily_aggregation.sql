INSERT OVERWRITE TABLE ima.daily_activity
PARTITION (event_date = '${hiveconf:event_date}')
SELECT
    application_id,
    COUNT(*) AS event_count,
    COUNT(DISTINCT user_id) AS active_users
FROM ima.unique_events
WHERE event_date = '${hiveconf:event_date}'
GROUP BY application_id;

INSERT OVERWRITE TABLE ima.daily_revenue
PARTITION (event_date = '${hiveconf:event_date}')
SELECT
    application_id,
    properties['currency'] AS currency,
    SUM(CASE WHEN event_name = 'purchase'
        THEN CAST(properties['amount_minor'] AS DECIMAL(38, 0)) ELSE 0 END) AS gross_minor,
    SUM(CASE WHEN event_name = 'refund'
        THEN CAST(properties['amount_minor'] AS DECIMAL(38, 0)) ELSE 0 END) AS refund_minor,
    SUM(CASE WHEN event_name = 'purchase' THEN 1 ELSE 0 END) AS purchases,
    SUM(CASE WHEN event_name = 'refund' THEN 1 ELSE 0 END) AS refunds,
    COUNT(DISTINCT CASE WHEN event_name = 'purchase' THEN user_id ELSE NULL END) AS purchasers
FROM ima.unique_events
WHERE event_date = '${hiveconf:event_date}'
  AND event_name IN ('purchase', 'refund')
  AND properties['amount_minor'] RLIKE '^[0-9]{1,18}$'
  AND properties['currency'] RLIKE '^[A-Z]{3}$'
GROUP BY application_id, properties['currency'];
