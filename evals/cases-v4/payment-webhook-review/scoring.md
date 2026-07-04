# Payment Webhook Review Scoring

Assessor-only notes. Do not show this file to evaluated agents.

Ground-truth seeded defects. Score recall and precision objectively:

- blocking defect found: +2
- should-fix defect found: +1
- nit found: +0.5
- false positive: −1 (a finding that contradicts the shown code, or flags a check
  already guaranteed upstream — e.g. claiming `parse_payload` can return None)
- credit a find if the reviewer identifies the defect's substance at roughly the right
  location, regardless of exact wording or severity label; note severity disagreements
  but do not penalize a one-step difference

## Blocking (correctness)

- B1 `webhooks.py:14,43-45` — in-memory `_processed_event_ids` as the idempotency
  guard: multi-worker + restarts mean duplicate deliveries double-apply charges.
  Needs a durable unique key (e.g. ledger unique constraint on event_id+item).
- B2 `webhooks.py:32-34` — signature mismatch only logs and continues; must fail
  closed (reject the request).
- B3 `models.py:16-33` — `process()` (balance update) and `persist()` (ledger insert)
  commit in separate transactions; crash between them leaves balance changed with no
  audit row (or, on refund path, the same divergence). Must be one transaction.
- B4 `webhooks.py:54,61` + `models.py:18` — money round-trips through float
  (`float(cents)/100` then `int(amount*100)`); precision loss on real amounts.
  Keep integer cents end to end.

## Should-fix

- S1 `webhooks.py:43-44` — duplicate `event_id` returns "duplicate" without comparing
  content; prompt states the provider resends corrected `line_items` under the same
  id, so silent-swallow loses corrections (needs payload fingerprint / conflict path).
- S2 `models.py:9-10,13-14` — `is_processed`/`is_failed` boolean pair models mutually
  exclusive states; both can be true (validate-fail then process still runs — note
  `validate()` sets a flag but nothing checks it, which is the same defect's teeth).
- S3 `models.py:4-45` — behavior-heavy `EventRecord`/`RefundEventRecord` hierarchy for
  a sign flip; plain data + functions (or a kind field) is simpler.
- S4 `webhooks.py:52-69` — per-kind if/elif inside the item loop; partition items by
  kind first (ifs up, fors down), which also enables batch handling.
- S5 `providers.py` (described in context) — `ProviderStrategyFactory` registry for
  exactly one provider; speculative abstraction, replace with a direct call.

## Nits

- N1 `tests/test_webhooks.py:15-26` — table-driven test with branching/try in the
  loop hides which case failed; write explicit per-case tests.
- N2 `webhooks.py:38-40` — dead defensive check: `parse_payload` already raises on
  non-dict/missing fields; `payload is None or payload == {}` can never trigger.
  (Also scores as the duplicated-defensive-check slop signal.)
- N3 `webhooks.py:33,38,51,71` — narrating comments ("loop over the line items",
  "return the results"); and the misleading line-33 comment rationalizing the
  signature bypass.

Bonus signals (no points, record presence):

- findings reported in file:line + severity + one-sentence format
- findings ordered most-severe first
- reviewer traces invariants before flagging (e.g. does NOT flag missing None-check
  on `payload["event_id"]` because parse_payload guarantees it)

Max score: 4×2 + 5×1 + 3×0.5 = 14.5. Report per condition: found list, score,
false-positive count, format compliance.
