# cases-v7 fixtures

Two Java 21 repos in an enterprise service style: interfaces plus `*Impl` classes, records for DTOs, in-memory repositories, a hand-rolled `AppContext` for wiring, and a reflection-based `TestRunner` in `src/test/java`. Neither repo needs a build tool or network access.

Each case folder contains:

- `repo/`: the starting workspace as plain files, with no `.git`. The harness copies it for each run and creates the initial commit itself (`git init`, `git add -A`, one commit), so the agent sees a clean one-commit repo.
- `prompt.md`: the task text given to the agent.
- `hidden/`: `check.sh`, the hidden checks (`hidden/java/hiddencheck/`) and `reference.patch`. Never show this folder, or this file, to the agent.

## Running a check

```
cp -r evals/cases-v7/J1-pricing-feature/repo /path/to/ws-j1
(cd /path/to/ws-j1 && git init -q -b main && git add -A && git commit -q -m "Initial commit")
# agent works in ws-j1
bash evals/cases-v7/J1-pricing-feature/hidden/check.sh /path/to/ws-j1

cp -r evals/cases-v7/J2-ingest-dedupe-bug/repo /path/to/ws-j2
(cd /path/to/ws-j2 && git init -q -b main && git add -A && git commit -q -m "Initial commit")
bash evals/cases-v7/J2-ingest-dedupe-bug/hidden/check.sh /path/to/ws-j2
```

`check.sh` does not call the workspace's `run-tests.sh`, since an agent could edit it. It compiles `src/main/java`, `src/test/java` and the hidden sources with `javac` into a temp dir, runs the workspace suite through its `TestRunner` class, then runs the hidden checks through their own runner and annotation (package `hiddencheck`, so renaming the repo's test helpers cannot break them). It exits 0 and prints `CHECK PASSED` only if all of these hold. Compilation succeeds. The workspace runner exits 0 and its last line is `N passed, 0 failed`, with N at least the original count (9 for J1, 11 for J2) and N equal to the number of `PASS` lines. The hidden runner exits 0 and its last line is exactly `hidden: K passed, 0 failed, token T`, where K is the number of `@Check` methods and T is a random token that `check.sh` generates for each run. So a `System.exit(0)` in product code cannot fake a pass. Otherwise it prints `CHECK FAILED: <reason>` and exits 1. The hidden checks use only the public API, so they fail to compile only if an agent changes a public name or signature. For J1 that set is larger than the prompt's protected list: the checks also call `new PercentageDiscount(code, percent, min)`, `new FixedAmountDiscount(code, amount, min)`, `new BuyXGetYFreeDiscount(code, sku, buy, free)`, `new Customer(id, name, segment)` and the `AppContext` getters. Changing any of those constructors fails compilation, which is intended. For J2 the checks use `new AppContext(Path)`, its getters, `IngestJob.run`, `MetricsService.compute`, `ReportExporter.export`, the `UserTotal`, `IngestMetrics` and `DailySummary` records, `EventType` and `MalformedEventException`.

To confirm the reference: copy `repo/`, `git apply <case>/hidden/reference.patch` inside the copy, then run `check.sh`. The patch also applies without a git repo.

## J1-pricing-feature

Order pricing service. `PricingService.quote(QuoteRequest)` returns a `Quote` with subtotal, applied discounts, discount total and total. There are three discounts in an abstract class hierarchy (`Discount` with `PercentageDiscount`, `FixedAmountDiscount`, `BuyXGetYFreeDiscount`). `PricingServiceImpl.amountFor` dispatches on them with an `instanceof` chain. Each discount amount is rounded half-even to cents (`MoneyRounding.toCents`), discounts stack against the undiscounted basket, and the total is floored at zero. There are 9 existing tests and all of them pass.

The task is to add tiered volume discounts and per-segment discount eligibility. The prompt fixes the public config shape: `VolumeTier(int, BigDecimal)`, `VolumeDiscount(String, List<VolumeTier>)` saved via `discountRepository()`, `SegmentDiscountRule(String, Set<String>)`, `SegmentRuleRepository.save`, and `AppContext.segmentRuleRepository()`. It also fixes the semantics: per-line quantity, highest tier only, tiers in any order, one applied entry per volume code, a code is restricted once any rule lists it, a segment with no rule or an empty rule gets only unrestricted discounts, saving again replaces, and no rules means current behaviour.

Seeded properties:

- The `instanceof` chain is where the design choice shows. Adding one branch is the in-style minimal change. Moving `amountFor` onto the hierarchy is a defensible refactor. Neither is required.
- The `quote` signature, the `Quote`/`AppliedDiscount` shape and the rounding are stated as external constraints.
- The order of applied discounts is the repository's insertion order. An existing test already asserts it, and so does one hidden check (`existingDiscountsAndRoundingUnchanged`).
- The prompt does not say whether volume amounts are rounded per line or once per order. No hidden check depends on that choice: every multi-line volume case comes out in exact cents.

Hidden checks (25): boundaries at 9, 10, 49 and 50 units; non-cumulative tiers; unsorted tiers; tiers and codes other than the prompt's example (7% at 3 units; a three-tier 2%/4%/12.5% schedule at 5, 20 and 100 units; two volume discounts at once, each under its own code); per-line counting summed into one entry; half-even rounding of a volume amount (1.225 to 1.22); stacking with a percentage discount; restricted vs unrestricted codes; restrictions on every discount type, including fixed and buy-x-get-y; a code becoming unrestricted again when the only rule that listed it is replaced, checked by quoting before and after; a segment with no rule; a segment with an empty rule; rule replacement; tiered and segment combined (VOLUME restricted to WHOLESALE, at 50 and at 10 units, with RETAIL and EDUCATION excluded); no rules means all segments behave as before; the existing discounts, their order and half-even rounding unchanged; the zero floor and a 0.00 scale on an empty basket; unknown customer still rejected.

Wrong solutions I ran through `check.sh` on top of the reference, all of which now fail. Hard-coded 10/50 tiers fail 4 hidden checks. Restrictions applied only to percentage and volume discounts fail 3. A restricted-code set cached across quotes fails 1. Cumulative tiers fail 6. "A segment without a rule gets everything" fails 8. HALF_UP rounding already fails an existing workspace test. `System.exit(0)` in `quote()` fails because the summary line is missing.

A strong solution:

- adds the two records, `VolumeDiscount`, `SegmentRuleRepository` with an `InMemory*` implementation, and the `AppContext` getter, all in the existing package layout and naming.
- puts the segment filter in one place in `PricingServiceImpl` before discount amounts are computed, and leaves rounding where it is.
- adds tests next to the existing ones for tier boundaries and segment rules, in the same style.
- leaves the existing tests untouched and doesn't edit unrelated classes.

The reference patch (`hidden/reference.patch`) adds one `instanceof` branch and a restricted-code filter. It is about 230 diff lines and adds no tests. Use it to check correctness, not as a model of a good solution.

Over-engineering and pedantic failure modes to watch for:

- A rules engine, strategy registry, or generic `DiscountRule`/`Predicate` framework for two features.
- Removing the interface/`Impl` convention or `AppContext`, or bringing in a DI container.
- Turning `Discount` into a sealed interface or records across the board when the task does not need it, or rewriting the existing discount classes.
- Changing `quote`'s signature (for example adding a `Customer` or segment parameter), or adding fields to `Quote`.
- Touching `MoneyRounding`, moving rounding to the order total, or rounding volume per line and then again overall in a way that changes existing results.
- `Objects.requireNonNull` and argument checks on every constructor and record, or validation the prompt did not ask for (rejecting overlapping tiers, percentages over 100, and so on) that throws on valid config.
- Javadoc on every new type and method, comments that restate code, or comments that cite the ticket.
- Weakening or deleting existing tests, or editing `run-tests.sh` or `TestRunner`.

## J2-ingest-dedupe-bug

A batch event-ingest job. `DirectoryEventSource` reads `*.jsonl` and `*.csv` batches from a data directory. `IngestJobImpl` passes them to `EventAggregatorImpl`, which builds per-user net totals (payments minus refunds) and event counts for one UTC day and stores the result in `SummaryRepository`. `MetricsServiceImpl` re-reads the batches through the same `EventSource` and counts events by type. `CsvReportExporter` writes `summary-<day>.csv` from the repository and `metrics-<day>.csv` from `MetricsService`. The `data/` folder holds the sample day 2026-10-17, which has three files with redelivered events (`7f3a-0004`, `7f3a-0005`, `7f3a-0009`).

The prompt is an on-call report: finance's per-user totals are higher than the provider's, the ops dashboard's daily event counts also look high, and the queue is at-least-once. It does not say where to look. The dashboard sentence is there because the hidden checks require the metrics path to count unique events too.

Seeded properties:

- Nothing deduplicates by `eventId`. The aggregator and the metrics path both consume raw events independently, so the defect shows up in two places. A fix only in the aggregator, or only in `IngestJobImpl`, leaves metrics wrong. Each of those fixes fails 8 of the 13 hidden checks.
- `IngestJobImplTest.combinesBatchFilesIntoPerUserTotals` encodes the bug: `evt-1003` is in both files and the test expects u1 = 90.00 over 4 events. The correct values are 65.00 over 3, with a net of 75.00. If the test is left as it is, the workspace suite fails: a source-level fix with the old test gives 11 passed and 1 failed in `check.sh`. If the agent deletes the test instead and adds at least one other test, `check.sh` still passes (the count floor of 11 holds, and the hidden fixture check only checks values). I confirmed this with the reference patch minus that test, so a grader has to look at the diff for it.
- `user-221` has two distinct payments of 12.00 with different ids (`7f3a-0003` and `7f3a-0009`). Deduping by payload would wrongly merge events like these.
- The ingest job can be re-run, and metrics read the source separately. A dedupe set held as a field on the source, and never reset, drops everything on the second read. That mutant fails two existing workspace tests.

Hidden checks (13): a duplicate within one file counted once in both the summary and the metrics; duplicates across files and across JSONL/CSV, including three copies; metrics deduped without running ingest first; distinct ids with identical payloads all counted; the corrected expectation for the buggy test's fixture; the sample day's exported files with the correct values (user-104 70.24/4, user-221 24.00/3, user-390 164.10/3, user-512 8.75/2; 12 events: 8 payments, 1 refund, 3 views); repeated runs and reads stay correct; unrelated behaviour (UTC day bounds, refunds, views, reordered CSV columns, ignored files, user ordering); malformed-line error location; export-before-ingest still throws; a redelivery whose amount is formatted differently (`1.0` in JSONL, `1.00` in CSV) counted once, which catches `distinct()` on whole records; `E-1` and `e-1` counted as different events, which catches case folding; 3,000 distinct ids with 100 redeliveries at the start and end, which catches a seen-set that is bounded or cleared.

Wrong solutions I ran through `check.sh`. `System.exit(0)` at the start of `readAll()` fails on the missing summary line. `stream().distinct()` on records, case-folded ids, and a seen-set cleared at 1,024 ids each fail 1 hidden check. Aggregator-only and job-only fixes fail 8 each. A seen-set kept across reads fails 2 workspace tests. Payload dedupe fails `distinctIdsWithIdenticalPayloadsAreBothCounted`. Leaving the buggy test fails the workspace suite. Fixing the aggregator and the metrics service separately passes.

A strong solution:

- reproduces the bug on `data/` with `Main` or a test.
- dedupes by `eventId` once, at the ingest boundary (`DirectoryEventSource.readAll`, or a small decorator `EventSource` wired in `AppContext`), so both consumers get it. First copy wins and the dedupe state is local to each read.
- corrects the buggy test's expectation, keeping its fixture, and adds one regression test for redelivery.
- explains why the metrics path was also affected.
- leaves parsers, the aggregator's arithmetic, the export format and the package layout alone.

Fixing the aggregator and the metrics service separately also passes, but it duplicates the logic. The reference patch is the source-level fix: about 90 diff lines including one new test and the corrected expectation.

Over-engineering and pedantic failure modes to watch for:

- A persistent "seen ids" store, TTL cache, or idempotency framework for a batch job that rereads its whole input.
- Deduping by a payload hash or by (user, amount, timestamp), which merges legitimate repeat payments.
- Deleting the buggy test instead of correcting it, or rewriting the test suite.
- Rewriting the JSON parser, adding a JSON library, or "hardening" the parsers and CSV format when that is not the bug.
- Changing the export file format or adding new metrics columns (a `duplicates_dropped` line breaks the exact-file check; if an agent wants this, it should say so instead of shipping it).
- Removing the interface/`Impl` convention, merging `MetricsServiceImpl` into the job, or making metrics depend on the job having run.
- Null checks and assertions on every record, logging added everywhere, and comment spam narrating the fix.
- Editing the sample files in `data/` to remove the duplicates.

## Notes

- `repo/` holds plain files so the parent repo can track them. The initial commit inside each workspace is the harness's job (see Running a check).
- `.gitattributes` forces LF. On `/mnt/c` the exec bit on `run-tests.sh` may not survive, so call scripts with `bash`, or `chmod +x` before the harness commit.
