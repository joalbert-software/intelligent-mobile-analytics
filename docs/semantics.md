# Analytical and operational semantics

Event identity is `(applicationId, eventId)`. Same-ID retries with matching
payloads are duplicates; differing payloads are conflicts. Reception time is
excluded from equality. User identities are application-scoped and case-sensitive.

Ingestion accepts events up to 30 days old and five minutes ahead of the
server clock. HTTP 207 means partial success: inspect each `CREATED`,
`DUPLICATE`, `INVALID`, `CONFLICT`, or `UNAVAILABLE` outcome. Persistent mode
acknowledges MongoDB writes; demo storage is explicitly ephemeral.

Ranges are half-open `[start, end)` in UTC milliseconds. Events order by time
then event ID. Sessions split on application/user changes or a gap strictly
greater than the selected threshold. Online reports clip sessions to the
requested range. Stored application gap settings are metadata; the report
parameter controls the calculation and defaults to 30 minutes.

Funnels count distinct users completing ordered steps within the conversion
window. One event cannot satisfy two steps. A newer first step can restart an
attempt. Same-timestamp ordering follows the event-ID tie-break.

Retention uses the first stored event as acquisition, not a guaranteed install
date. Incomplete observation days are omitted. Deleting old history changes
the available acquisition evidence: preserve acquisition data separately when
rebuilding reports after retention deletion.

Online reports reject more than 100,000 matching events rather than silently
truncating aggregates. Most reports restrict spans to 366 days. Retention scans
acquisition history with the same event cap. Use batch processing for larger
datasets.

Revenue is integer minor units, partitioned by currency. Refunds are separate
positive amounts. Different event IDs are not checked for duplicate business
orders. Python validates currency-label format; Java additionally uses the
runtime currency database.

Only series reports use the Redis cache, with a 30-second TTL. Authorization
precedes lookup. Cache failures fall back to computation. Cached aggregates
may remain stale until expiry. Rate limiting is local to each process.

Privacy endpoints delete raw events only. Backups, exports, cached aggregates,
and offline datasets require separate deletion/rebuild workflows. The stored
retention-day setting does not automatically schedule expiration.

Event catalog rules are reusable validators, not automatically enforced by
default ingestion. Governance tables are operator workflow schemas, not a
complete compliance system. AuditRepository must be called explicitly; HTTP
mutations do not automatically append audit records.

## Batch processing

Normalize/deduplicate before Hadoop session or daily aggregation. Conflicting
event identities stop processing. Mapper partitioning must keep an entire
application/user together for sessionization; the Bash scripts configure this.
External sorting caps simultaneously merged runs.

`python -m ima_batch.warehouse publish INPUT OUTPUT_DIRECTORY` creates globally
deduplicated snake-case JSON for Hive from a complete input export. Load the
raw partitions into `unique_events` after deduplication. The Hive scripts do
not resolve conflicting IDs. Set the Hive runtime timezone to UTC.

Hive retention emits observed returning rows; missing offsets need an
observation-calendar join. Python retention provides a complete zero-filled
curve for eligible observed days.

MySQL aggregate loading replaces supplied rows with upserts. It does not
delete old rows absent from later input. A full partition rebuild therefore
needs an operator-controlled deletion transaction. Distinct-user counts cannot
be summed across days or dimensions.

Checkpointed reports verify input/output digests and restart failed work from
the beginning. Inspect stale lock owners before removing lock files. Legacy
Python 2 atomic rename behavior targets Linux/POSIX.

The Java worker has a five-minute lease, renews before publication, and fences
completion by worker/attempt. Consumers must use only artifacts for the
matching attempt of a SUCCEEDED job. Cancelled/lost attempts can leave orphan
artifacts. No worker runs automatically with the WAR; the separate entry point
is `com.ima.admin.RunBatchWorker`. Long computations should use distributed
processing. After five attempts, create a new job to start a fresh retry budget.

## Deployment boundaries

Readiness checks event storage, not every dependency. The example Hibernate
connection pool is a reference configuration. Before deployment, upgrade
legacy dependencies and validate a managed pool, TLS, proxy timeouts,
service accounts, backup restore, and all integration paths.

No application, build, migration, deployment, or test has been run for this
delivery. Test files describe intended behavior for future validation.
