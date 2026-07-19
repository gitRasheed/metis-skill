# API Evolution Review Scoring — v1.3 key (consolidated)

Assessor-only. blocking +2, should-fix +1; FP penalties by claimed
severity; atomic-cause (a finding merging two listed causes earns both if
it correctly states both; an umbrella item is credited once); adjacent-
severity 75% / two-step 50%; precision reported. Line anchors are
INDICATIVE — match findings by content, not line number (v1 anchors were
unreliable per xhigh critique). This case deliberately loads lens 2/3
(data/state, call-site & API shape). Most findings are taste defects,
not crashes; that is the point.

## Blocking (+2)

- A1 — the constructor exposes 11 positional-capable params in one
  enduring signature; region, sandbox, telemetry, and _legacy_mode are
  accepted but never affect behavior, on_error works but has no caller,
  and transport config, delivery defaults, callbacks, and migration
  state are conflated. FULL credit requires a structural remedy (config
  object / split of responsibilities + removal or isolation of inert
  params); naming the bloat with only a kwonly-marker or rename fix =
  half credit.
- A3 — send_batch reuses one dedupe_key for every item, so a provider
  honoring dedupe suppresses all but one distinct notification; per-item
  keys or derived stable unique keys required. (Dedupe scope adjudicated
  as provider-global — the standard reading.)

## Should-fix (+1)

- A2 (demoted from blocking, v1.3) — send conflates validation, payload
  shaping, and error-callback dispatch; full credit requires the
  structural remedy: pure request builder(s), transport behind a seam,
  error policy left with the caller. A finding that prescribes the full
  separation earns A2 + S3 together.
- S1 (umbrella, credited once) — on_error / telemetry / metadata are
  speculative surface with no caller and one provider; remove until a
  real caller exists. Metadata-silently-discarded scores HERE unless the
  finding is batch-specific (then S16).
- S2 — _raw lets callers overwrite validated fields (to, channel,
  priority); isolate behind a legacy adapter or reject reserved keys.
- S3 — transport (urllib, JSON, auth header, retry policy) is hard-wired
  in the client with no injectable seam; introduce a narrow transport
  port. (See F3 for the softened trap boundary.)
- S4 — send_batch ignores the provider's REAL batch capability (context:
  batch is a real requirement), doing N sequential posts with no batch
  semantics; submit through the provider batch endpoint or define the
  batch contract.
- S5 — no total deadline: per-attempt timeout × retries × backoff is
  unbounded wall-clock for callers; bound total time, not per-attempt.
- S6 — truthiness validation admits invalid content states (both-None,
  empty strings); validate exactly-one-of message/template with explicit
  None checks.
- S7 — max_retries means total attempts, and 0 (or negative) yields an
  implicit None return that crashes callers; validate and perform one
  initial attempt plus max_retries.
- S8 — every new v2 param is positional-capable, so call sites silently
  misbind as the API evolves; keyword-only boundary after the two v1
  args. (Counts alone; also folded into A1 full-credit remedies.)
- S9 — template delivery requires the awkward send(to, None,
  template=...) call shape; default message=None or add send_template.
- S10 — catching every Exception retries permanent HTTP failures and
  hides programming/response-shape errors; classify transient vs
  permanent explicitly.
- S11 — retrying a non-idempotent send after ambiguous failure can
  double-deliver when no dedupe key is supplied; generate/require one
  stable idempotency key per logical send across its retry sequence.
- S12 — batch applies items incrementally, so a malformed later item
  leaves earlier sends already committed (partial application);
  classify/validate the whole batch before any I/O.
- S13 — response bodies are trusted without shape validation at the
  transport boundary; validate before returning provider results.
- S15 — _legacy_mode is REQUIRED by two named migrations yet never
  affects behavior; implement it in an isolated compat path or remove
  it. (Distinct from A1's surface complaint — this is missing required
  behavior.)
- S16 — send_batch accepts _raw (and metadata) but silently drops them,
  so batch and single-send contracts diverge; forward through the legacy
  path or remove from the batch signature.

## Nits

None. (v1's N1 in-method imports demoted to NEUTRAL — style preference
with no stated repo convention; N2 deleted — its _raw claim was false,
its metadata half lives in S1.)

## False-positive traps (penalty)

- F1 — "templates/scheduling/dedupe/batch are speculative generality":
  context states these are REAL provider capabilities and requirements.
- F2 — "remove the backward-compat two-arg send": required by context.
- F3 (softened, v1.3) — penalize ONLY the claim that no test seam is
  needed at all ("one provider means transport needs no abstraction").
  A reviewer who acknowledges the testability need but argues the
  overridable _post is a sufficient seam scores NEUTRAL (0) on S3 — a
  defensible engineering position, not an error.

## Neutral (0)

Type hints/docstrings; async version; renaming `to`; in-method imports
(ex-N1); "_post subclass override is a sufficient test seam" (see F3).

**Max: 2×2 + 16×1 = 20.**

## Adjudication history

- v1 (2026-07-19 overnight): A1, A2 blocking; S1-S5; N1, N2; F1-F3.
  Max 10.
- v1.1: cold baseline adopted 5 unseeded-real: A3 (blocking, shared
  batch dedupe_key), S6 both-None validation, S7 max_retries semantics,
  S8 kwonly boundary, S10 transient-vs-permanent retry. Max 16.
- v1.2: metis run adopted S11 (retry-sans-dedupe double delivery);
  P1-condensed run adopted S12 (batch partial application) and S13
  (response-shape validation). Max 19. S9 (template call ergonomics)
  formalized from repeated cross-condition findings.
- v1.3 (2026-07-19 morning, xhigh critique adjudication — critique at
  scratchpad/critique-api/final.md): ACCEPTED — A2 demoted to should-fix
  (transport partially separated already; builder extraction is not
  merge-blocking); S4 rewritten to the provider-batch defect (old text
  described absent code); S2 split into S2 (_raw hatch) + S15
  (_legacy_mode inert-but-required); N2 deleted (contained a false
  claim); N1 neutralized (style, no convention); F3 softened (overridable
  _post = defensible seam position); line anchors demoted to indicative.
  REJECTED — S1 split into three (same cause: speculative surface with
  no caller; same fix: remove; splitting double-counts A1) ; dropping S5
  (unbounded total wall-clock stands as should-fix without the bogus
  ~90s arithmetic, which is removed). Critique's unseeded finds were
  all already v1.1/v1.2 adoptions (S11, S6, S10, S7, S8) — independent
  convergence recorded as key validation. PROMPT stays v1 (frozen for
  transcript comparability); prompt-v2 candidates if ever re-authored:
  dedupe scope sentence, schedule_at type, error contract. All prior
  transcripts re-scored under v1.3 (max 20); v1.1/v1.2 numbers are not
  comparable and are superseded.
