#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
command -v java >/dev/null
command -v mvn >/dev/null
mvn --batch-mode clean verify
PYTHONPATH="$project_root/batch${PYTHONPATH:+:$PYTHONPATH}" python -m unittest discover -s batch/tests -v
printf '%s\n' "Build artifacts are in target/."
