from __future__ import absolute_import, division

import contextlib
import hashlib
import os
import re
import time

from .compat import open_text
from .validation import identifier, integer


def connect(environment=None):
    environment = os.environ if environment is None else environment
    required = ("IMA_MYSQL_HOST", "IMA_MYSQL_USER", "IMA_MYSQL_PASSWORD", "IMA_MYSQL_DATABASE")
    if any(not environment.get(name) for name in required):
        raise ValueError("MySQL connection environment is incomplete")
    try:
        import pymysql
    except ImportError:
        raise RuntimeError("install the optional PyMySQL adapter to use warehouse loading")
    port = int(environment.get("IMA_MYSQL_PORT", "3306"))
    integer(port, "port", 1, 65535)
    arguments = {
        "host": environment["IMA_MYSQL_HOST"],
        "port": port,
        "user": environment["IMA_MYSQL_USER"],
        "password": environment["IMA_MYSQL_PASSWORD"],
        "db": environment["IMA_MYSQL_DATABASE"],
        "charset": "utf8mb4",
        "autocommit": False,
        "connect_timeout": 10,
        "read_timeout": 30,
        "write_timeout": 30,
    }
    if environment.get("IMA_MYSQL_SSL_CA"):
        arguments["ssl"] = {"ca": environment["IMA_MYSQL_SSL_CA"]}
    return pymysql.connect(**arguments)


@contextlib.contextmanager
def transaction(connection):
    connection.begin()
    try:
        yield connection
        connection.commit()
    except Exception:
        connection.rollback()
        raise


def split_statements(source):
    statements = []
    current = []
    quote = None
    line_comment = False
    block_comment = False
    index = 0
    while index < len(source):
        char = source[index]
        following = source[index + 1] if index + 1 < len(source) else ""
        if line_comment:
            if char == "\n":
                line_comment = False
                current.append("\n")
        elif block_comment:
            if char == "*" and following == "/":
                block_comment = False
                current.append(" ")
                index += 1
        elif quote is not None:
            current.append(char)
            if char == "\\" and following:
                current.append(following)
                index += 1
            elif char == quote:
                if following == quote:
                    current.append(following)
                    index += 1
                else:
                    quote = None
        elif char in ("'", '"', "`"):
            quote = char
            current.append(char)
        elif char == "-" and following == "-":
            line_comment = True
            index += 1
        elif char == "#":
            line_comment = True
        elif char == "/" and following == "*":
            block_comment = True
            index += 1
        elif char == ";":
            statement = "".join(current).strip()
            if statement:
                statements.append(statement)
            current = []
        else:
            current.append(char)
        index += 1
    if quote is not None or block_comment:
        raise ValueError("unterminated SQL quote or comment")
    remainder = "".join(current).strip()
    if remainder:
        statements.append(remainder)
    if any(re.match(r"\ADELIMITER\b", statement, re.I) for statement in statements):
        raise ValueError("DELIMITER directives are not supported")
    return statements


class MigrationRunner(object):
    def __init__(self, connection, directory):
        self.connection = connection
        self.directory = os.path.abspath(directory)

    def discover(self):
        result = []
        versions = set()
        for name in sorted(os.listdir(self.directory)):
            match = re.match(r"\A(\d{3})_[a-z0-9_]+\.sql\Z", name)
            if not match:
                continue
            version = int(match.group(1))
            if version in versions:
                raise ValueError("duplicate migration version")
            versions.add(version)
            path = os.path.join(self.directory, name)
            with open_text(path) as stream:
                source = stream.read()
            result.append({"version": version, "name": name, "source": source,
                           "sha256": hashlib.sha256(source.encode("utf-8")).hexdigest()})
        return result

    def _prepare(self, cursor):
        cursor.execute("CREATE TABLE IF NOT EXISTS migration_history ("
                       "version INTEGER NOT NULL PRIMARY KEY, "
                       "file_name VARCHAR(160) NOT NULL, sha256 CHAR(64) NOT NULL, "
                       "state VARCHAR(16) NOT NULL, started_at BIGINT NOT NULL, "
                       "completed_at BIGINT NULL) ENGINE=InnoDB")
        self.connection.commit()

    def apply(self):
        applied = []
        cursor = self.connection.cursor()
        acquired = False
        try:
            cursor.execute("SELECT GET_LOCK('ima_schema_migrations', 10)")
            acquired = cursor.fetchone()[0] == 1
            if not acquired:
                raise RuntimeError("could not acquire schema migration lock")
            self._prepare(cursor)
            cursor.execute("SELECT version, sha256, state FROM migration_history ORDER BY version")
            existing = dict((version, (digest, state)) for version, digest, state in cursor.fetchall())
            for migration in self.discover():
                previous = existing.get(migration["version"])
                if previous is not None:
                    if previous[0] != migration["sha256"]:
                        raise ValueError("an applied migration has changed")
                    if previous[1] != "completed":
                        raise ValueError("partial migration requires operator inspection before proceeding")
                    continue
                now = int(time.time() * 1000)
                cursor.execute("INSERT INTO migration_history "
                               "(version, file_name, sha256, state, started_at) VALUES (%s, %s, %s, %s, %s)",
                               (migration["version"], migration["name"], migration["sha256"], "running", now))
                self.connection.commit()
                for statement in split_statements(migration["source"]):
                    cursor.execute(statement)
                cursor.execute("UPDATE migration_history SET state = %s, completed_at = %s WHERE version = %s",
                               ("completed", int(time.time() * 1000), migration["version"]))
                self.connection.commit()
                applied.append(migration["name"])
            return applied
        finally:
            if acquired:
                cursor.execute("SELECT RELEASE_LOCK('ima_schema_migrations')")
            cursor.close()


class AggregateLoader(object):
    TABLES = {
        "activity": (
            "daily_application_metrics",
            ("application_id", "metric_day", "event_count", "active_users", "new_users", "session_count",
             "bounce_count", "session_duration_total", "crash_count", "affected_users", "computed_at", "source_job_id"),
            ("application_id", "metric_day"),
        ),
        "events": (
            "daily_event_metrics",
            ("application_id", "metric_day", "event_name", "platform", "event_count", "distinct_users",
             "computed_at", "source_job_id"),
            ("application_id", "metric_day", "event_name", "platform"),
        ),
        "revenue": (
            "daily_revenue_metrics",
            ("application_id", "metric_day", "currency", "gross_minor", "refund_minor", "purchase_count",
             "refund_count", "purchaser_count", "computed_at", "source_job_id"),
            ("application_id", "metric_day", "currency"),
        ),
        "retention": (
            "retention_cohorts",
            ("application_id", "cohort_day", "day_offset", "cohort_users", "retained_users",
             "observed_until", "computed_at", "source_job_id"),
            ("application_id", "cohort_day", "day_offset"),
        ),
    }

    def __init__(self, connection):
        self.connection = connection

    def _row(self, row, columns, application_id, job_id, now):
        result = dict(row)
        if result.get("application_id") != application_id:
            raise ValueError("aggregate belongs to another application")
        result["source_job_id"] = job_id
        result["computed_at"] = now
        if set(result) != set(columns):
            raise ValueError("aggregate columns do not match the target schema")
        for name in columns:
            value = result[name]
            if name in ("application_id", "source_job_id", "event_name"):
                identifier(value, name)
            elif name in ("metric_day", "cohort_day"):
                from .timeutil import parse_date
                parse_date(value)
            elif name == "currency":
                if not re.match(r"\A[A-Z]{3}\Z", value):
                    raise ValueError("invalid aggregate currency")
            elif name == "platform":
                if value not in ("ANDROID", "IOS", "WEB", "OTHER"):
                    raise ValueError("invalid aggregate platform")
            else:
                integer(value, name, 0, 9223372036854775807)
        for subset, population in (("affected_users", "active_users"), ("new_users", "active_users"),
                                    ("bounce_count", "session_count"), ("retained_users", "cohort_users")):
            if subset in result and result[subset] > result[population]:
                raise ValueError("aggregate subset exceeds its population")
        return tuple(result[name] for name in columns)

    def upsert(self, kind, application_id, job_id, rows, maximum_rows=100000):
        identifier(application_id, "applicationId")
        identifier(job_id, "jobId")
        if kind not in self.TABLES:
            raise ValueError("unknown aggregate kind")
        table, columns, keys = self.TABLES[kind]
        update_columns = [name for name in columns if name not in keys]
        sql = "INSERT INTO {0} ({1}) VALUES ({2}) ON DUPLICATE KEY UPDATE {3}".format(
            table, ", ".join(columns), ", ".join(["%s"] * len(columns)),
            ", ".join("{0} = VALUES({0})".format(name) for name in update_columns))
        now = int(time.time() * 1000)
        count = 0
        seen = set()
        with transaction(self.connection):
            cursor = self.connection.cursor()
            try:
                batch = []
                for row in rows:
                    if count >= maximum_rows:
                        raise ValueError("aggregate load exceeds transaction row limit")
                    values = self._row(row, columns, application_id, job_id, now)
                    identity = tuple(row[name] for name in keys)
                    if identity in seen:
                        raise ValueError("duplicate aggregate identity in input")
                    seen.add(identity)
                    batch.append(values)
                    count += 1
                    if len(batch) == 500:
                        cursor.executemany(sql, batch)
                        batch = []
                if batch:
                    cursor.executemany(sql, batch)
            finally:
                cursor.close()
        return count
