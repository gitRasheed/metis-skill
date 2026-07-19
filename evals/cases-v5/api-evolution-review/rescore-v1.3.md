# Re-scores under key v1.3 (max 20) — 2026-07-19 morning

All six existing transcripts re-scored by finding→item mapping. Conventions
applied: adjacent-severity 75% (claimed blocking on a should-fix item),
umbrella S1 credited once, A2+S3 merge allowed, A1 full=2 / naming-only=1,
strict atomic-cause (credit only causes actually stated — e.g. "dedupe
collisions" without the shared-key mechanism does NOT earn A3).

Transcripts: scratchpad api/{baseline,metis}-rep1/final.md and
perm/P{1-condensed,6-tiger,8-agentic,9-structural}-api/final.md.

## baseline-rep1 — 10.0/20 (50%)
S4@.75 S15@.75 S1@1 [S6+S9]@1.5 A3@2 S2@1 S8@1 S7@1 S10@1.
Missed: A1 A2 S3 S5 S11 S12 S13 S16. Traps: none hit. Precision 9/10
(metadata finding = S1 dup).

## current-metis-rep1 — 13.5/20 (67.5%)
S11@.75 A3@2 S15@.75 S7@1 S10@1 S6@1 S4@1 S16@1 A1@1(half: named the
11-param surface, kwonly-only remedy; inert-param removal facet folded)
S1@1 S9@1 S8@1 S2@1.
Missed: A2 S3 S5 S12 S13. Traps: none. Precision 14/14 (M9+M13 fold into A1).

## P1-condensed-api — 9.5/20 (47.5%)
[S9+S6]@1.5 S12@.75 S4@.75 S7@.75 S10@.75 S15@1 S2@1 S1@1 S13@1 S8@1.
Missed: A1 A2 A3 S3 S5 S11 S16. Traps: none. Precision 12/12 (region/
sandbox finding folds to A1 facet, uncredited alone).

## P6-tiger-api — 8.0/20 (40%)
[S12+S4]@1.5 S7@.75 S10@.75 [S9+S6]@2 S8@1 S2@1 S1@1.
Missed: A1 A2 A3 S3 S5 S11 S13 S15 S16. Traps: none. BELOW baseline —
tiger review cues narrowed breadth.

## P8-agentic-api — 11.75/20 (59%)
[S9+S6]@1.5 [A3@2+S4@.75]=2.75 [S10+S11]@1.5 S12@1 S15@1 S1@1 S8@1 S7@1
S13@1.
Missed: A1 A2 S2 S3 S5 S16. Traps: none. Only non-current condition to
state A3's shared-key mechanism.

## P9-structural-api — 13.25/20 (66%)
[S12+S4]@1.5 S7@.75 [S9+S6]@2 A1@2(FULL: ClientConfig split + hook
removal) S15@1 S1@1 S2@1 S16@1 [A2+S3]@2 S10@1.
Missed: A3 (named "dedupe collisions" but not the shared-key cause —
strict no credit), S5 S8 S11 S13. Traps: none.

## Table (v1.3, supersedes all v1.1/v1.2 numbers)

| condition | score | % | unique strengths |
|---|---|---|---|
| P6-tiger | 8.0 | 40% | — |
| P1-condensed | 9.5 | 47.5% | S13 early adopter |
| baseline | 10.0 | 50% | — |
| P8-agentic | 11.75 | 59% | A3 mechanism, S11 |
| P9-structural | 13.25 | 66% | A1 FULL, A2+S3 (found by nothing else) |
| current | 13.5 | 67.5% | breadth: S8+S11+A3+S16 |

Reading: the v1.2 "P9 breakthrough" headline (14 vs 10.75) was partly an
artifact of A2 being blocking. Under the corrected key P9 ≈ current on
points, with complementary profiles: P9 = structural depth (the lens-3
center), current = breadth. This is the exact complementarity P10-hybrid
is built to combine. No condition hit any trap; precision ~100% across
the board — the trap axis does not discriminate at these efforts.

## Rep 2-3 extension (2026-07-19 morning, perm protocol: skill file only)

N3 adopted (+0.5 nit): `_session = None` dead scaffolding at prompt line 18
(verified present); found only by P9-r2. Max now 20.5.

PROTOCOL NOTE: original current-metis-rep1 (13.5) ran with references/
examples.md + review-examples.md in workspace (run-one-eval.sh); ALL
permutation runs and reps 2-4 use run-perm-review.sh = SKILL file only.
For skill-TEXT comparison use the uniform perm protocol; current-r1 is
protocol-privileged and flagged, not mixed into the perm table.

Per-rep scores (v1.3 + N3, max 20.5):
- P9-structural: r1 13.25, r2 13.25 (incl. N3; A2+S3 split across two
  findings, half-A2), r3 13.25 (A2+S3 full pair again) — mean 13.25,
  ZERO variance. A2+S3 prescribed in 3/3 reps (full in r1/r3); A1-full
  in r1 only; A3 in 2/3 (r2, r3).
- P10-hybrid: r1 11.25, r2 11.0, r3 10.5 — mean 10.92. A1 config-object
  1/3 (r3 at 75% adjacent), A2 0/3, S3 seam-only 3/3, A3 1/3.
- current (perm protocol): r2 11.0, r3 8.0, r4 pending.
- Single-rep conditions unchanged: P8 11.75, baseline 10.0, P1 9.5, P6 8.0.

READING: P9 > P10 on review is a REAL effect (3v3 reps, ranges disjoint)
— adding the agentic implementation-rules section dilutes review depth,
mirroring P6 (review cues diluting agentic). Phase-map routing does not
fully prevent cross-phase attention dilution. Review-arena ranking:
P9 13.25 >> P8 11.75 ~ P10 10.92 ~ current-perm ~9.5 (n small) >
baseline 10.0 caveat n=1. Current WITH references (13.5, n=1) ~ P9
without references — references matter on review; P9+references untested.

## P11-gated + current-r4 (same key/protocol)

- P11-gated: r1 13.25 (A1 FULL, A3+S4 merged, S11, near-A2+S3, N3),
  r2 10.25 (gate fired — "Phase: reviewing a PR" — but no A1/A2/S3),
  r3 12.75 (A1 FULL, near-A2+S3, N3; missed A3) — mean 12.08, spread 3.0.
- current-api-r4 7.5 (8 findings; missed A1/A2/S3/S6/S11/S15/S16).
  Current perm-protocol final: 11.0 / 8.0 / 7.5 — mean 8.83.

## Review-arena final (uniform perm protocol, key v1.3+N3, max 20.5)

| condition | reps | mean | note |
|---|---|---|---|
| P9-structural | 13.25 ×3 | 13.25 | zero variance; A2+S3 3/3 |
| P11-gated | 13.25/10.25/12.75 | 12.08 | A1-full 2/3; bimodal |
| P8-agentic | 11.75 | 11.75 | n=1 |
| P10-hybrid | 11.25/11.0/10.5 | 10.92 | amendment suppressed |
| baseline | 10.0 | 10.0 | n=1 |
| P1-condensed | 9.5 | 9.5 | n=1 |
| current | 11.0/8.0/7.5 | 8.83 | text-only, no references |
| P6-tiger | 8.0 | 8.0 | n=1 |

P12-modular (core + phase-loaded references) in flight.

## P12-modular (core + phase-loaded references/, same key/protocol)

- r1 12.25 (A1 FULL, A3+S4, S11+S10), r2 9.0, r3 11.25 — mean 10.83.
- All 3 reps verifiably READ references/reviewing.md and skipped
  implementing.md (router worked), yet A2+S3 fired 0/3 and A1-full 1/3.
  The structural amendment reliably fires only when INLINE in the file
  held during review (P9: 3/3). Indirection weakens skill content.

## P13kit reps (2026-07-19 final wave, kit protocol, v1.3+N3 max 20.5)
- r1 = 14.5 — CAMPAIGN MAX. A3 full; S11+S10 merge (75% ea, blocking-claimed);
  S7; S6; S4+S12 merge; S9; S1; A1-half (inert-param cause, remove/implement
  remedy, no constructor-structural fix); S15+S2 merge; A2-half+S3 (builder+
  seam one finding, caller-policy in another — P9-r2 split precedent); N3.
  11/11 credited, zero FP.
- r2 = 11.75. A3+S4-merge(75%) 2.75; S9+S6 at 75% ea; S7; S10; S1+A1-half+
  S15 merged finding 3.0; S16; S3+A2-half. Missed S11, S12, S2, N3.
- r3 = 10.25. S7(75%) S4(75%) S9+S6(75% ea) A1-half+S15 S1 S2 S3 S10 N3.
  MISSED A3 entirely (only rep of any P1x condition to do so). Zero FP.
- Mean 12.17 (59.4%) vs P10kit 12.83, P9-text 13.25, current+kit 13.5 (n=1).
  High variance (10.25-14.5); r1 is the best single api transcript of the
  campaign (found 15 of 19 causes at least partially).

## Confirmation round (2026-07-19 late, n=3 kit protocol both sides)
- P15kit-api: r1 14.75 (NEW CAMPAIGN MAX: A3, A1@75% structural, S2+S15+S16
  triple merge, S13, S11@75%, S12@75%, S7@75%, S9+S6, S4, S1, S10),
  r2 9.75 (missed A3, S11, entire seam cluster), r3 13.5 (A3+S4 merge,
  A1@75%, S2+S15, S3 seam, S9+S6, S11@75%, S7, S10, S1, N3).
  Mean 12.67, range [9.75, 14.75].
- curkit-api: r1 13.5 (original, protocol-matched) / r2 12.5 (A3, S15@75%,
  S11@75%, S12 S6 S16 S7 S10 S9 S1 S8, A1-half) / r3 11.25 (missed A3;
  S12+S4, S6+S9, S2+S15, S8, S7, S10, S11@75%, S1, A1-half).
  Mean 12.42.
- VERDICT at n=3: PARITY (P15 +0.25, within noise). No regression from
  adding the P15 sections to the api case. Zero FP in all six.
