# Background Jobs Review Scoring — v5.1 (post-critique)

Assessor-only notes. Never show this file to evaluated agents. Revised after
the 2026-07-18 xhigh case-design critique; adjudication history at the end.

## Scheme

- Blocking +2, should-fix +1, nit +0.5, depth marker +0.5.
- False positives are penalized symmetrically to the claimed severity:
  false blocking −2, false should-fix −1, false nit −0.5. Neutral-ruled
  reports score 0 and count in a noise tally.
- Credit substance at roughly the right location. One-step severity
  disagreements are recorded (severity-accuracy metric) but not penalized.
- Compound findings: score by atomic claims. Precision =
  atomic claims credited / atomic claims reported (neutrals excluded from
  the numerator, included in the denominator).
- Validity gate (objective): union of the run's read commands over
  `prompt.md` (from the event log) must cover every line; otherwise the run
  is invalid and rerun. `cat` counts as full coverage.

Severity rule (why handler findings are not blocking): blocking = the queue
machinery itself loses, stalls, duplicates, or crashes work regardless of
handler; handler-contract violations under the advertised retry model are
should-fix because delivery semantics are a stated product decision the team
can re-scope.

**Max (v5.1.1): 8×2 + 9×1 + 3×0.5 + 7×0.5 = 30.**

## Blocking (+2 each)

- B1 `worker.py:33-37,109` — `reap_expired_leases` runs only at startup; a
  crashed worker's lease row blocks re-claiming (`UniqueViolation` on the PK)
  until some process restarts. Fix: reap every loop, or claim with
  `INSERT ... ON CONFLICT (job_id) DO UPDATE ... WHERE expires_at < now()`.
- B2 `worker.py:66-73,116-124` — leases are never extended while jobs
  execute; `extend_leases` runs only after the batch. Credited fix must renew
  the entire claimed set during execution or claim just-in-time —
  "heartbeat the current job" alone is incomplete (batch-mates still expire).
- B3 `worker.py:76-79` — `finish_job` releases the lease, then marks done, in
  two autocommit statements; crash between them re-runs a completed job.
- B4 `queue.py:29-35` + migration:21 + `worker.py:84-90` — the dead-letter
  path is doubly broken: the jobs DELETE fires while the lease row still
  references the job, so it raises a foreign-key violation that propagates
  out of `main`'s except block and kills the worker (latent until B5 is
  fixed); and once the FK ordering is corrected, the remaining
  multi-statement autocommit sequence has a crash window that loses the job.
  Credit the FK crash or the loss window; the credited fix must be
  constraint-safe (lease removed/validated before the jobs delete) — "wrap
  it in a transaction" without reordering does not fix the FK and earns no
  credit.
- B5 `worker.py:84,92-96` — `attempts` incremented only in memory, never
  persisted; `MAX_ATTEMPTS` unreachable, dead-lettering never fires.
- B6 `scheduler.py:28-32` + `handlers.py:49` — base credit: inline
  "lightweight" execution bypasses the queue machinery (any of: no lease, no
  retry/dead-letter, no dedupe, blocks the tick). Subclaims D6/D7 below.
- B8 `worker.py:41-62` — stale-candidate re-execution: the claim INSERT has
  no eligibility guard, so a worker iterating an old SELECT can lease and
  re-run a job another worker claimed, ran, and released within the scan
  window (short jobs make this real). Fix: selection + eligibility +
  lease acquisition as one atomic operation (or re-check status inside the
  claim). Credit map vs F1: credit when the report describes claim-after-
  release or re-running a completed/deleted job from a stale scan; penalize
  F1 only when the claim is that two simultaneous INSERTs both succeed.
- B9 `worker.py:75-98` — no ownership fencing: after lease expiry and
  takeover, the original worker's `finish_job`/`handle_failure` delete the
  successor's lease and rewrite job state unconditionally. Credit requires
  stating that transitions don't verify the lease is still ours (worker_id /
  fencing token in the WHERE clause, assert one row changed).

## Should-fix (+1 each)

- S1 `worker.py:44` — strict `ORDER BY priority DESC` with the stated
  webhook volume starves priority-0 exports indefinitely; add aging or
  quotas. (Prompt context supplies priorities and volumes; without those
  facts this would be neutral.)
- S2 `handlers.py:21-27` — `post_webhook` violates the PR's idempotency
  claim. Credited fix: a stable idempotency key the receiver can enforce, or
  explicitly re-scoping the claim to accept duplicates. A local "record after
  send" table alone re-creates the same crash window and is not a fix.
- S3 `handlers.py:37-40` — export appends to a shared per-date file:
  duplicate rows on retry, interleaved writes across workers, partial output
  on crash; per-job temp file + atomic rename.
- S4 `scheduler.py:16-21` — minute-modulo firing both skips late ticks (no
  backfill) and mis-fires cadences not dividing 60. Either half = full
  credit; both halves = no extra.
- S5 `worker.py:101-103,121-122` — an unknown/mistyped job type raises
  `KeyError` into the blanket `except`, indistinguishable from a transient
  failure and (with B5) retried forever. Credit the observability/
  classification gap; do not require an immediate-reject policy.
- S7 `tests/test_jobs.py:22-34` — +0.5 per vacuous test: the retry test
  asserts only that `execute` was called; the two-worker test mocks the DB
  and contests nothing. The backoff table test asserts real values — its
  loop/branch shape is style, not vacuity, and earns nothing here.
- S10 `handlers.py:8-18` — `send_email_digest` performs the send before
  recording it; under reclaim or crash-and-retry the handler double-sends,
  violating the advertised "every handler is idempotent." (Email cannot be
  exactly-once without an outbox, but the PR's claim makes the gap a
  contract defect, consistent with S2.)
- S11 `worker.py:35-36,56-58,69-71` — lease deadlines are written and
  compared using each host's wall clock; cross-host skew causes premature
  takeover or delayed recovery. Fix: compute expiry and comparisons in the
  database (`now()`).
- S8 `worker.py:41-45` — the claim SELECT does not exclude leased rows, so
  the `LIMIT 50` window fills with jobs other workers are running and
  claimable work beyond the window is never seen (visibility starvation).
  Distinct from B8 (interleaving) and S1 (priority): credit reports about
  the window/head-of-line, not about eligibility staleness. (Restored in
  v5.1.1 — the v5.1 rewrite dropped it by mistake; discovered while scoring
  the formal runs, applied uniformly to all six.)

## Nits (+0.5 each)

- N1 `worker.py:48` — narrating comment.
- N2 `worker.py:126-127` — `print` instead of logging; cumulative in-process
  stats misleading across workers/restarts.
- N3 `worker.py:18-24,117,123` — `WorkerContext` mixes connection, config,
  stats, stop flag, and a `current_job` that is written but never read; keep
  run-local state local, delete the dead field.

## Depth markers (+0.5 each; base finding must be credited)

- D2 (B2): `extend_leases` runs when the processed jobs' leases are already
  deleted — it matches zero rows.
- D3 (B1×B2): a restart's reap converts stalled leases into double-execution
  of still-running jobs.
- D4 (B2): batch-mates claimed up front expire while waiting behind a long
  job even if the current job is heartbeated.
- D6 (B6): both HA schedulers execute the same inline fire; nothing dedupes
  the inline path (unlike the enqueued path).
- D7 (B6): `id=None` defeats the `sent_emails` dedupe and the insert fails
  after the send; the unguarded scheduler loop then dies.
- D8 (B4): the FK crash is latent until B5 is fixed (bug interaction stated).
- D9 (B8): the stale-scan interleaving is spelled out concretely
  (claim → run → release by another worker inside the scan window).

## False-positive traps (penalty by claimed severity)

- F1 — "two workers' simultaneous claim INSERTs can both succeed": the
  leases PK serializes simultaneous claims; `UniqueViolation` is the lost
  race. Only this phrasing is penalized — see B8's credit map for the real
  stale-scan defect.
- F2 — "nothing ever commits / missing `conn.commit()`": `connect()` is
  autocommit (db.py:8-9). Atomicity findings about specific multi-statement
  invariants (B3/B4) are the correct form.
- F3 — "mixing `time.monotonic()` with wall-clock timestamps is a bug"
  (worker.py:115,125): monotonic for durations, wall clock for absolute DB
  expiry is correct. (S11 is about cross-host wall clocks, not this.)
- F4 — "the two HA schedulers double-enqueue queued jobs": guarded by
  `jobs_schedule_fire_ux` + the `UniqueViolation` catch (queue.py:19-21).
  The inline-path duplication is real (D6).

## Neutral (0, noise tally)

- "Use FOR UPDATE SKIP LOCKED instead of a leases table" (alternative
  design); "webhook response status unchecked" (`urlopen` raises on
  non-2xx); linear-not-exponential backoff (nit at most); "handlers take a
  raw conn" (nit at most); partial-index NULL hardening (no shown caller can
  produce the state).

## Adjudication history

- Round 1 (cold gpt-5.6-sol high baseline + shipped-metis probe): B7-as-FK
  crash confirmed unseeded by three independent finders; N3-was-claim-scan
  upgraded for head-of-line starvation (now folded into B8's territory:
  starvation-of-visibility reports credit S1-adjacent substance at judge's
  discretion, interleaving reports credit B8).
- xhigh critique (2026-07-18): F1 narrowed (stale-scan race was a real
  defect miscategorized as a trap) → B8 seeded; ownership fencing
  formalized → B9; B4/B7 merged (loss window latent behind FK crash);
  B7 "transactional" credit rule tightened to constraint-safe fixes; email
  window promoted to S10 for consistency with the PR's idempotency claim;
  S11 cross-host clocks seeded; S6 demoted and merged into N3 (its causal
  link to B5 was false); S7 split into per-test subcredits; depth markers
  pruned of restatements (old D1/D5 removed) and D4/D9 added; FP penalties
  made severity-symmetric; severity rule stated explicitly.
- Rejected from critique: strict severity scoring (one-step tolerance kept —
  judge-variance cost exceeds the prioritization signal); difficulty levers
  1-3 (blocking-tier rebuild) deferred to case 2 by design.
