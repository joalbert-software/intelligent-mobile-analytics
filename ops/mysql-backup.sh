#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${IMA_MYSQL_DEFAULTS_FILE:?Set a protected MySQL client options file}"
: "${IMA_BACKUP_DIRECTORY:?Set the backup destination}"
database="${IMA_MYSQL_DATABASE:-ima}"
if [[ ! "$database" =~ ^[A-Za-z][A-Za-z0-9_]*$ ]]; then
    printf '%s\n' 'Invalid database identifier.' >&2
    exit 2
fi
mkdir -p "$IMA_BACKUP_DIRECTORY"
destination="$(cd "$IMA_BACKUP_DIRECTORY" && pwd -P)"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
partial="$destination/ima-$stamp.sql.gz.partial"
final="$destination/ima-$stamp.sql.gz"
if [[ -e "$partial" || -e "$final" ]]; then
    printf '%s\n' 'Backup path already exists.' >&2
    exit 2
fi
trap 'rm -f -- "$partial"' EXIT
mysqldump --defaults-extra-file="$IMA_MYSQL_DEFAULTS_FILE" \
    --single-transaction --routines --triggers --hex-blob "$database" | gzip -c > "$partial"
gzip -t "$partial"
mv -- "$partial" "$final"
sha256sum "$final" > "$final.sha256"
printf '%s\n' "$final"
