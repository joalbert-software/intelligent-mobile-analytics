# Intelligent Mobile Analytics

## Purpose

Analyze events from mobile applications: active users, sessions, conversion
funnels, retention cohorts, application errors, and campaign attribution.
Applications send pseudonymous identifiers; sample data is synthetic.

## Implementation boundaries

- `src/main/java/com/ima/domain` and `analytics`: domain models and calculations.
- `persistence` and `store` packages: Hibernate metadata and MongoDB events.
- `service` and `cache` packages: ingestion, reports, authorization, and caching.
- `web` and `config` packages: Spring MVC in one Maven Tomcat WAR project.
- `batch/ima_batch`: Python processing, clients, Hadoop streaming, and warehouse tools.
- `sql`: versioned MySQL migrations and optional Hive table definitions/queries.
- `ops`: Linux deployment, health checks, backups, and Jenkins build automation.

## Event contract

An event has an application ID, client event ID, pseudonymous user ID, event
name, UTC event time, platform, application version, and bounded properties.
Ingestion authenticates the application before accepting data. Deduplication
uses the application ID and client event ID together. No report query may
cross an application boundary. Server reception time is recorded separately
from client event time.

## Analytics semantics

Sessions split on application/user boundaries or an inactivity gap greater
than 30 minutes. Events must be ordered by event time and event ID before
sessionization. A user's first event date determines their acquisition cohort.
Retention measures distinct returning users at a specified day offset.
Funnels require ordered steps within a configured conversion window.
Revenue uses integer minor units and remains partitioned by currency.
Date ranges are UTC, inclusive at the start and exclusive at the end.

## Persistence and failure handling

MySQL owns application configuration and report definitions. MongoDB owns
accepted raw events. Reports derive from raw events and can be rebuilt.
Redis is a disposable cache, never the sole copy of accepted events.
There is no assumed transaction spanning MySQL, MongoDB, and Redis.
An ingestion response is successful only after durable raw-event storage.
Batch processing must tolerate retries without inflating aggregates.

## Verification

JUnit/Mockito and Python unittest source describe selected boundary and failure
cases. They have not been executed. Neither the build nor application has been
run, at the owner's request. Static source inventory and syntax checks are
separate from runtime verification. Integration testing remains future work.

## Historical scope

The requested technology window is 2013–2017. Any recreated history must be
described as reconstructed history rather than contemporaneous development.
Historical dependency versions belong to a legacy demonstration environment.
A dated milestone must not claim features or dependencies introduced later.
The exact reconstructed schedule is recorded in docs/history-manifest.json.
Historical snapshots are partial assembly steps, not runnable historical releases.

## Size accounting

The target is at least 10,000 nonblank, noncomment implementation lines in Java,
Python, SQL, and executable operational scripts. Tests, documentation, configuration,
maintenance tools, Python docstrings, dependencies, and generated artifacts are
excluded. See docs/source-inventory.json for the per-file count.
