# Reconstructed source history

Assembled in October 2026; author and committer dates are intentionally
reconstructed across 2013–2017 at the owner's request. Every commit carries
a `[reconstructed]` prefix. These dates do not establish employment,
contemporaneous development, or historical production use.

The schedule has irregular active weeks, inactive weeks, and varying commit
counts. Every commit adds reviewable content; there are no empty commits or
manufactured bug/fix cycles.

These are source-assembly steps, not independently buildable historical
releases. Early partial snapshots can reference components added later.
The final snapshot uses a late-2017 dependency baseline. Contemporary audit
scripts and inventories remain 2026 tooling even in the final dated snapshot.

| Simulated year | Source grouping |
| --- | --- |
| 2013 | Domain types and analytical algorithms |
| 2014 | Storage, persistence, MySQL schemas |
| 2015 | Security, services, catalogs, client |
| 2016 | Python batch, cache, Hive, Hadoop scripts |
| 2017 | HTTP/configuration, workers, documentation, verification source |

See [docs/history-manifest.json](docs/history-manifest.json) for exact dates,
file assignments, and weekly counts. The reconstruction utility refuses to
replace an existing `.git` directory or rewrite existing history. Publish
the repository with this disclosure intact.
