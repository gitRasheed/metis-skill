# event-ingest

Nightly batch job that turns the event export from the payments queue into per-user daily totals for finance, plus ingest metrics for the ops dashboard.

## How it works

1. The queue consumer drops batch files into a data directory: `*.jsonl` (one JSON object per line) or `*.csv` (with a header row). Each record has `eventId`, `userId`, `type` (`PAYMENT`, `REFUND`, `VIEW`), `amount` and an ISO-8601 UTC `timestamp`.
2. `IngestJob.run(day)` reads every batch through `EventSource`, aggregates the events for that UTC day with `EventAggregator` and stores the `DailySummary` in `SummaryRepository`.
3. `ReportExporter.export(day, outDir)` writes `summary-<day>.csv` (net amount and event count per user; net is payments minus refunds) and `metrics-<day>.csv` (counts from `MetricsService`).

`config/AppContext` wires the components; construct it with the data directory.

## Build and test

Requires JDK 21. No build tool is needed.

```
./run-tests.sh
```

This compiles `src/main/java` and `src/test/java` into `build/classes` and runs every `@Test` method in classes named `*Test` with the in-repo `TestRunner`.

## Run against the sample data

```
./run-tests.sh
java -cp build/classes com.acme.ingest.Main data 2026-10-17 out
```
