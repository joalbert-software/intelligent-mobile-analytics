#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${CATALINA_BASE:?Set CATALINA_BASE to the intended Tomcat instance}"
artifact="${1:-$project_root/target/ima.war}"
if [[ ! -f "$artifact" || ! -d "$CATALINA_BASE/webapps" ]]; then
    printf '%s\n' 'WAR or Tomcat webapps directory is missing.' >&2
    exit 2
fi
tomcat_root="$(cd "$CATALINA_BASE" && pwd -P)"
if [[ "$tomcat_root" == / ]]; then
    printf '%s\n' 'Refusing a filesystem-root Tomcat instance.' >&2
    exit 2
fi
backup_root="$tomcat_root/ima-backups"
mkdir -p "$backup_root"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
if [[ -f "$tomcat_root/webapps/ima.war" ]]; then
    cp -p "$tomcat_root/webapps/ima.war" "$backup_root/ima-$stamp.war"
fi
temporary="$tomcat_root/webapps/.ima-$stamp.war"
trap 'rm -f -- "$temporary"' EXIT
install -m 0640 "$artifact" "$temporary"
mv -f -- "$temporary" "$tomcat_root/webapps/ima.war"
printf '%s\n' 'WAR installed. Tomcat deployment monitoring controls activation.'
