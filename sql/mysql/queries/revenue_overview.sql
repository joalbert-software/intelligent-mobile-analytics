SELECT
    application_id,
    currency,
    SUM(gross_minor) AS gross_minor,
    SUM(refund_minor) AS refund_minor,
    SUM(gross_minor) - SUM(refund_minor) AS net_minor,
    SUM(purchase_count) AS purchases,
    SUM(refund_count) AS refunds,
    SUM(gross_minor) / NULLIF(SUM(purchase_count), 0) AS average_order_minor
FROM daily_revenue_metrics
WHERE application_id = ?
  AND metric_day >= ?
  AND metric_day < ?
GROUP BY application_id, currency
ORDER BY currency;
