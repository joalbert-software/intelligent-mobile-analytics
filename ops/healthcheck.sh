#!/usr/bin/env bash
set -euo pipefail
endpoint="${IMA_ENDPOINT:-http://127.0.0.1:8080/ima}"
case "$endpoint" in
    http://127.0.0.1:*|http://localhost:*|https://*) ;;
    *) printf '%s\n' 'Use HTTPS or a loopback endpoint.' >&2; exit 2 ;;
esac
curl --fail --silent --show-error --connect-timeout 3 --max-time 10 \
    "${endpoint%/}/healthz"
printf '\n'
curl --fail --silent --show-error --connect-timeout 3 --max-time 15 \
    "${endpoint%/}/readyz"
printf '\n'
