# Intelligent Mobile Analytics

A mobile-event analytics reference project using Java/Spring MVC, Python batch
processing, MySQL metadata, MongoDB events, Redis caching, and Hadoop/Hive.

**Reconstructed history:** assembled in October 2026, with intentionally
simulated 2013–2017 commit dates. This is not evidence of development or
employment during those years. See [HISTORY.md](HISTORY.md).

**Source-only delivery:** the application, Maven build, tests, database
migrations, and deployment scripts have **not been run**. Static source and
Git checks are separate from runtime verification. This repository is not
represented as production-ready.

## Features

- Application-scoped ingestion, duplicate/conflict detection, and stable-ID retries.
- Hashed API keys, scopes, expiration, revocation, and local rate limiting.
- Sessions, ordered funnels, exact-day retention, revenue, breakdowns,
  engagement, crash summaries, experiments, and segmentation.
- Hibernate metadata repositories, MongoDB events, and Redis report caching.
- Saved reports, CSV/JSONL exports, application settings, and leased batch jobs.
- Java delivery client; Python client, disk spool, checkpoints, and external sorting.
- Offline attribution, RFM analysis, forecasting baselines, anomaly detection,
  threshold alerts, data-quality checks, and aggregate reconciliation.
- MySQL schemas/loaders, Hive queries, Hadoop streaming, Linux scripts, and Jenkins.
- JUnit/Mockito and Python unittest source for future verification.

The statistical functions are transparent baselines, not trained AI models.
No employer data, fabricated benchmark results, or historical deployment claims
are included.

## Layout

| Path | Purpose |
| --- | --- |
| `src/main/java/com/ima/domain`, `analytics` | Domain values and calculations |
| `src/main/java/com/ima/store`, `persistence` | Storage adapters and repositories |
| `src/main/java/com/ima/service`, `web` | Services and Spring MVC API |
| `src/main/java/com/ima/client`, `catalog` | Delivery client and schema rules |
| `src/main/java/com/ima/jobs`, `admin` | Worker and operator entry points |
| `batch/ima_batch` | Python analytics, clients, streaming, and warehouse tools |
| `sql/mysql`, `sql/hive` | Schemas and analytical queries |
| `ops` | Linux build, deployment, backup, and batch scripts |
| `tools` | Static audit and reconstructed-history maintenance |
| `docs` | Architecture, semantics, inventories, and history manifest |

The backend is a single Maven WAR with package-level boundaries.

## Technology baseline

The final source targets **Java 8**, Spring Framework/MVC 4.3.13, Hibernate
5.2.12, Tomcat 8.5/Servlet 3.1, MySQL 5.7, MongoDB 3.4, Redis 3.x, Maven 3,
JUnit 4, and Mockito 2. Much domain code uses Java 7 language constructs;
the complete application requires Java 8.

Python application modules use Python 2.7/3-compatible syntax. Python 2.7
runtime compatibility is not verified. Maintenance tools require Python 3.8+
and are contemporary reconstruction tooling. The optional MySQL loader uses
PyMySQL. These legacy dependencies need upgrades before real deployment.

## Code size

The target is **at least 10,000 implementation code lines**. The audit counts
nonblank, noncomment Java, Python, SQL, and Bash lines. It excludes Python
docstrings, tests, documentation, configuration, tools, generated artifacts,
and dependencies. See [docs/source-inventory.json](docs/source-inventory.json).

Recalculate statically, without importing application modules:

```sh
python tools/source_audit.py
```

## Configuration for future use

These commands are supplied for a future operator and were not executed.
Select exactly one profile: `-Dspring.profiles.active=demo` or
`-Dspring.profiles.active=persistent`.

Demo mode requires a private random `IMA_DEMO_API_KEY` of at least 32
characters. `IMA_DEMO_APPLICATION` defaults to `demo-app`. Demo data is
process-local; its initial key expires 24 hours after startup.

Persistent mode requires:

| Variable | Meaning |
| --- | --- |
| `IMA_MYSQL_URL` | JDBC URL for a migrated database |
| `IMA_MYSQL_USER`, `IMA_MYSQL_PASSWORD` | MySQL credentials |
| `IMA_MONGO_URI` | MongoDB URI, including appropriate connection timeouts |
| `IMA_MONGO_DATABASE` | Optional database name, default `ima` |
| `IMA_REDIS_HOST`, `IMA_REDIS_PORT` | Optional Redis address |
| `IMA_REDIS_PASSWORD` | Redis password |
| `IMA_ARTIFACT_DIRECTORY` | Root for local worker artifacts |

Apply numbered MySQL migrations in order to an empty database. The optional
`python -m ima_batch.dbcli migrate sql/mysql` command instead uses separate
`IMA_MYSQL_HOST`, `IMA_MYSQL_PORT`, `IMA_MYSQL_DATABASE`, user, and password
variables. MySQL DDL auto-commits; a partial migration requires inspection.

The operator-only `com.ima.admin.ProvisionApplication` entry point takes
application ID, display name, and owner reference as arguments. It reads the
initial key from `IMA_BOOTSTRAP_API_KEY` without printing it.

```sh
mvn --batch-mode clean verify
export PYTHONPATH="$PWD/batch"
python -m unittest discover -s batch/tests -v
python -m ima_batch --help
```

The WAR path is `target/ima.war`. Jenkins expects configured tools named
`java8` and `maven3`, plus Python. No GitHub Actions run automatically.

## HTTP interface

Authenticated routes are under `/ima/api/v1/apps/{applicationId}` with
`Authorization: Bearer <token>`. Timestamps are UTC Unix milliseconds.

| Method and suffix | Scope | Purpose |
| --- | --- | --- |
| `POST /events` | INGEST | JSON array of 1–500 events |
| `GET /analytics/series`, `/sessions` | READ | Activity and session reports |
| `POST /analytics/funnel` | READ | Ordered conversion funnel |
| `GET /analytics/retention`, `/revenue` | READ | Cohort and revenue reports |
| `GET /analytics/breakdown`, `/crashes`, `/engagement` | READ | Additional reports |
| `GET /export/events.csv` or `.jsonl` | READ | Bounded exports |
| `POST /keys`, `DELETE /keys/{keyId}` | ADMIN | Credential management |
| `DELETE /privacy/users/{userId}` | ADMIN | Raw-event deletion |
| `DELETE /privacy/expired-events?before=...` | ADMIN | Raw-event expiration |
| `GET /saved-reports`, `GET /saved-reports/{id}/result` | READ | Saved reports |
| `POST /saved-reports`, `DELETE /saved-reports/{id}` | ADMIN | Manage reports |
| `GET` application root, `PUT` application root | READ / ADMIN | Settings |
| `GET /jobs`, `GET /jobs/{id}` | READ | Job status |
| `POST /jobs`, `POST /jobs/{id}/retry`, `DELETE /jobs/{id}` | ADMIN | Queue management |

Saved reports, application settings, and jobs require persistent mode.
`/ima/healthz` and `/ima/readyz` are unauthenticated probes.

Event fields are `eventId`, `userId`, `name`, `timestamp`, `platform`,
`appVersion`, and string-valued `properties`. The server supplies application
identity and reception time. Revenue events require `amount_minor` and
`currency` properties. See [docs/semantics.md](docs/semantics.md).

## GitHub publication

The delivery is a local Git repository on branch `main`. It does not create
a remote repository or push to GitHub. No token is stored in files or Git URLs.
After creating an empty remote repository, a future publishing step is:

```sh
git remote add origin https://github.com/YOUR_ACCOUNT/intelligent-mobile-analytics.git
git push -u origin main
```

Authenticate with a credential manager or GitHub CLI.
