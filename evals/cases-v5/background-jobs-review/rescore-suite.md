# bgjobs suite hand-scores (key v5.1.1, max 30) — scored 2026-07-19
Transcripts: scratchpad suite/bgjobs-{baseline,metis}-rep{1,2,3}/final.md.
- baseline: r1 18.0 (B1 B2+D2 B4 B5 B6+D6+D7 S1 S2 S3 S4 S8 S10 S7x1),
  r2 21.0 (adds B9, B3-via-generalized-atomicity, S7x2; B1 sev-down tolerated),
  r3 18.0 (B3 B6+D6+D7 B1 B2 B5 B4 S2 S3 S4 S8 S10). Mean 19.0 (63%).
- metis (current skill + refs): r1 23.0 (B3+B9 compound 4.0, B2+D4, B6+D6+D7,
  N1, S1, migrations-index finding ruled neutral-noise),
  r2 21.0 (B9, both S4 halves, N1), r3 18.5. Mean 20.83 (69%).
- Zero FP-trap hits in all six. Metis uniquely consistent on B9 ownership
  fencing (3/3 vs 1/3) and the only condition reporting nits (N1 2/3).
- Unfound by all six: B8 stale-scan, S5 classification, S11 cross-host
  clocks, N2, N3 — stable headroom (matches formal-round finding).

# P13kit reps (2026-07-19 final wave, kit protocol, key v5.1.1 max 30)
- P13kit-bg-r1 = 23.75 — CAMPAIGN MAX. B1 B2+D2 B3(generalized-atomicity,
  baseline-r2 precedent) B4 B5 B6+D6+D7 B8+S8-merge B9; S1, S7x2, N1;
  severity inflation on S10/S2/S3/S4 (blocking-claimed, 75% each); zero FP.
- P13kit-bg-r2 = 20.0. B1 B2+D2+D4 B4 B5 B6+D6+D7 B9; S8 S1 S7x2;
  S10/S2/S3/S4 at 75% (inflated); missed B3/B8; zero FP.
- Mean 21.875 (72.9%) > P9kit 21.75 > current 20.83 > P10kit 17.25.
  P13 range [20, 23.75] vs P10kit [16, 18.5]: DISJOINT — the adopted
  critique wording recovers the impl-rules review tax on this case.

# P14a reps (P10 + review-mode-only sentences; key v5.1.1 max 30)
- P14akit-bg-r1 = 19.0: B6+D6+D7, B5, B1, B2+D2+D4 (3.0), B4, B3(generalized-
  atomicity), S4(75%), S8, S7x2; S10/S2/S3 at 75% (blocking-claimed).
  Missed B8 B9 S1 S5 S11 N1-3. Two off-key neutrals noted (schedule priority
  default; claim-index key order) — key frozen, recorded as candidates only.
- P14akit-bg-r2 = 15.5: FP −2 (LIGHTWEIGHT_TYPES "absent" — same factual FP
  as P10kit-bg-r1, 2nd occurrence on the P10 chassis); B5 B4 B1 B6+D6(2.5)
  B9+B3 merged at should-fix (3.0), B2+D2+D4 at should-fix (2.5), S4(75%),
  S10/S2/S3(75% ea), N1. Missed B8 S1 S5 S7 S8 S11 N2 N3.
- Mean 17.25 = EXACTLY P10kit's mean. VERDICT: the two review-mode sentences
  (#17, #20-tail) are NOT the source of P13's bgjobs recovery. P14a fails
  its preregistered gate (a) 17.25 < 20.0; does not advance to holdout.

# P14b reps (P10 + validate-vs-assert #2/#3 only; key v5.1.1 max 30)
- P14bkit-bg-r1 = 17.5: B4 B5 B6+D6+D7 B2 B1 B3(should-fix,1.5) S4(75%)
  S8 S7x2; S10/S2/S3 at 75%. Zero FP. Missed B8 B9 S1 S5 S11 N1-3.
- P14bkit-bg-r2 = 19.75: B6+D6+D7(3.0) B2+D2(2.5) B9 B5 B4 B1; S4 S8 S1
  at CORRECT severities (+3); S10/S2/S3(75%); S7x2. Zero FP. Missed B3 B8
  S5 S11 N1-3. (Off-key SIGTERM-drain finding noted as key candidate.)
- Mean 18.625: +1.4 over P10kit/P14a (17.25) but BELOW the 20.0 gate.
  Component result: #2/#3 contributes real review lift, insufficient alone.

# P14c reps (P10 + #17/#20-tail + #2/#3 combined; key v5.1.1 max 30)
- P14ckit-bg-r1 = 20.0: B6+D6+D7(3.0) B5 B2 B1 B3(generalized) B9 B4;
  S4(75%) S1 S7x2; S10/S2/S3 at 75%. Zero FP. Missed B8 S5 S8 S11 N1-3.
- P14ckit-bg-r2 = 21.5: B6+D6+D7(3.0) B5 B2+D2+D4(3.0) B1 B9+B3 merge
  (4.0 — release-then-mark crash window + ownership fencing in one
  finding, both stated) B4; S4(75%) S8 S7x2 N1; S10/S2/S3(75%).
  Zero FP. Missed B8 S1 S5 S11 N2 N3. (Claim-index off-key finding
  recurs — 2nd time; recorded as key candidate.)
- Mean 20.75: GATE (a) PASSED (>= 20.0). SUPER-ADDITIVE: P14a 17.25 +
  P14b 18.625 -> combined 20.75. Within noise of current 20.83; far above
  P10 17.25. Zero FP across all four P14c-family bgjobs reps.

# P15 reps (P14a + lens-1-scoped validate-vs-assert sentence; v5.1.1 max 30)
- P15kit-bg-r1 = 21.5: B6+D6+D7(3.0) B5 B4 B3(generalized) B2+D2(2.5) B1
  B9; S4(75%) S8 S1 S7x2; S10/S2/S3(75%). Zero FP. Missed B8 S5 S11 N1-3.
- P15kit-bg-r2 = 21.5: B6+D6+D7(3.0) B2 B1 B9+B3 merge(4.0) B5 B4;
  S8(75%, blocking-claimed) S1 S4 S7x2 N1; S10/S2/S3(75%). Zero FP.
  Missed B8 S5 S11 N2 N3.
- Mean 21.5, ZERO VARIANCE — gate (a) PASSED. Beats current 20.83; second
  only to P13's 21.88. PLACEMENT MECHANISM CONFIRMED (review side): the
  same distinction placed inside lens 1 outperforms the diffuse placement
  (P14b 18.625) AND the P10 base (17.25). Six consecutive P1x-family
  bgjobs reps with zero FP.

# Confirmation round (n=5 each side, kit protocol, key v5.1.1 max 30)
- P15kit-bg r3 16.5 (FP −2 LIGHTWEIGHT_TYPES, 3rd chassis occurrence;
  B5 B4 B1 B6+D6 B3+B9@sf 3.0, B2+D2+D4@sf 2.5, S4@75 S8 N1, S10/S2/S3@75),
  r4 22.5 (B6+D6+D7, B9+B3 4.0, B2+D2+D4 3.0, B5 B4 B1, S4@75 S8 S1 S7x2 N1),
  r5 20.5 (B6+D6+D7, B9+B3 4.0, B2+D4 2.5, B5 B4 B1, S4@75 S8 S7x2).
- curkit-bg r4 22.5 (B6+D6+D7, B3+B9 4.0, B2+D2+D4 3.0, B5 B1 B4, S4@75
  S8 S1 S7x2 N1), r5 18.0 (B6+D6+D7, B2+D4 2.5, B5 B4 B1, S4@75 S8 S1
  S7x2 N1; missed B3 B8 B9).
- FULL n=5: P15 {21.5, 21.5, 16.5, 22.5, 20.5} mean 20.5, median 21.5.
  current {23.0, 21.0, 18.5, 22.5, 18.0} mean 20.6, median 21.0.
- VERDICT at n=5: PARITY. The n=2 superiority claim (21.5 vs 20.83) did
  NOT survive; the honest claim is P15 == current on bgjobs. FP pattern:
  LIGHTWEIGHT_TYPES trap hit 3/9 P10-chassis reps vs 0/5 current — real
  chassis-correlated risk on THIS case, cause unknown (n small).
