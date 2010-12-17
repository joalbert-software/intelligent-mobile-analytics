#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ $# -lt 3 ]]; then
    printf '%s\n' 'Usage: report.sh OPERATION INPUT OUTPUT [additional options]' >&2
    exit 2
fi
export PYTHONPATH="$project_root/batch${PYTHONPATH:+:$PYTHONPATH}"
exec python -m ima_batch report "$@"
