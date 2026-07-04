# Catalog Sync Review Scoring

Assessor-only notes. Do not show this file to evaluated agents.

Same scheme as payment-webhook-review: blocking +2, should-fix +1, nit +0.5,
false positive −1. Credit substance at roughly the right location; note but do not
penalize one-step severity disagreements. Max 14.5.

## Blocking (correctness)

- B1 `engine.py:50` — watermark is the LOCAL clock (`datetime.now()`, naive, taken
  after processing) while `updated_at` comes from the source primary's clock; clock
  skew or replication lag permanently skips rows whose source timestamps fall behind
  the local watermark. Fix: watermark = max(source `updated_at`) seen, or a
  source-provided cursor. (Bonus substance: second-precision ties at the watermark
  boundary with `>` comparison also drop rows — credit if raised here or separately.)
- B2 `source.py:22-24` — OFFSET pagination over a live result set ordered by
  `updated_at`: concurrent source writes shift rows between pages mid-sync, silently
  skipping or duplicating items. Fix: keyset/cursor pagination on
  `(updated_at, id)`.
- B3 `engine.py:49,77-84` — `_delete_unseen` deletes every product not seen in the
  fetch, but the fetch is now INCREMENTAL (changed rows only), so the first
  five-minute run deletes essentially the whole local catalog (~200k rows minus the
  few hundred changed). Delete-by-absence is only valid for full syncs; incremental
  needs tombstones or periodic full reconciliation. This is the central trap of the
  full→incremental migration.
- B4 `scheduler.py:8` + `engine.py:11` — a singleton `SyncEngine` is reused for the
  process lifetime, so `self.seen_ids` accumulates across runs (masking deletions
  ever further and growing unboundedly) and `self.stats` compounds; per-run state
  must be local to `run()`.

## Should-fix

- S1 `engine.py:46` — `time.sleep(0.05)` per item inside the loop (~25s per
  500-item page) rate-limits the wrong thing; the constraint is source API fetches,
  so limit at the fetch layer and batch instead.
- S2 `engine.py:8-12` — `SyncEngine` is a behavior-heavy class whose only state is
  what should be run-local variables; plain functions with run-scoped data remove
  the cross-run state hazard entirely (pairs with B4).
- S3 `source.py:1-10,33` — `SourceAdapterRegistry` registry/classmethod machinery
  for exactly one adapter (registered with `replica_conn=None` at import time, which
  is itself dubious); speculative abstraction, use a direct constructor.
- S4 `engine.py:30-46` — per-item if/elif kind dispatch nested inside while/for
  (ifs down, fors up), and unknown kinds fall through silently with no counter or
  log; partition by kind first and make unknown kinds visible.
- S5 `scheduler.py:15-16` — `except Exception: pass` swallows every sync failure
  invisibly; log with context and surface repeated failures.

## Nits

- N1 `tests/test_sync.py:13-23` — table-driven test with branching hides which case
  failed; write explicit per-case tests.
- N2 `engine.py:31-34` — dead defensive check: `fetch_changed` already raises on
  missing `id`/`kind` (stated in code and context); the `item.get("id") is None`
  branch is unreachable and silently increments `skipped`.
- N3 `engine.py:24,35,56` — narrating comments ("fetch the next page", "process each
  item", "return the stats to the caller").

False-positive examples for this case: claiming the upserts are wrong (they are
valid ON CONFLICT upserts), claiming missing-id items crash the engine (the adapter
raises first), or claiming `bundles` rows are deleted by `_delete_unseen` (it only
targets `products`).

Bonus signals (no points, record presence): lens/rule attribution tags on findings;
findings ordered most-severe first; explicitly connecting B3 to the full→incremental
migration context.

Max score: 4×2 + 5×1 + 3×0.5 = 14.5.
