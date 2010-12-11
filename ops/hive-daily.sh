#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ $# -ne 1 || ! "$1" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
    printf '%s\n' 'Usage: hive-daily.sh YYYY-MM-DD' >&2
    exit 2
fi
requested_date="$1"
normalized_date="$(date -u -d "$requested_date" +%F)"
if [[ "$requested_date" != "$normalized_date" ]]; then
    printf '%s\n' 'Invalid calendar date.' >&2
    exit 2
fi
hive --hiveconf "event_date=$requested_date" \
    -f "$project_root/sql/hive/002_daily_aggregation.sql"
