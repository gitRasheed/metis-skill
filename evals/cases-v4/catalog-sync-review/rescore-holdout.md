# Catalog-sync HOLDOUT scores (2026-07-19 final wave)

Case + key frozen pre-campaign (a97cbc1/35e58a5); NO permutation ever ran on
it before this wave. Uniform protocol: production references kit for all
skill conditions; bare prompt for baseline. Scheme: blocking +2, should-fix
+1, nit +0.5, FP −1, one-step severity slack (noted, not penalized).
Max 19.5 (incl. unseeded B5/B6/S6). n=2 per condition, gpt-5.6-sol high.

| condition | r1 | r2 | mean | % |
|---|---|---|---|---|
| P13-final + kit | 17.5 | 16.0 | 16.75 | 85.9% |
| P10-hybrid + kit | 17.0 | 16.0 | 16.5 | 84.6% |
| current + kit | 16.0 | 14.5 | 15.25 | 78.2% |
| baseline (bare) | 14.0 | 14.0 | 14.0 | 71.8% |

Per-finding derivations:
- base-r1 14.0: B3 B1 B2 B6 B5 (all blocking, exact) + B4(sev-slack) + S6 +
  S5. Zero taste findings (S1-S4, N1-N3 all missed). One neutral (source
  deletion propagation — not in key, not FP).
- base-r2 14.0: same credit set as r1 (B3 B1 B2 B5 B6 B4 S6 S5).
- cur-r1 16.0: base set + S1 (sleep at wrong layer) + N1 + N3.
- cur-r2 14.5: base set + N1 only.
- p10-r1 17.0: base set + S1 + S4 (unknown-kind fallthrough) + N2
  (dead defensive check, [rule: trace invariants] tagged) + N3. Test-coverage
  finding neutral (not in key). Missed S2 S3 N1.
- p10-r2 16.0: base set + S4 + N1 + N2. Missed S1 S2 S3 N3.
- p13-r1 17.5 = HOLDOUT MAX: base set + S4 + S1 + N1 + N2 + N3.
  Missed only S2 S3.
- p13-r2 16.0: base set + S1 + N1 + N3. Missed S4 S2 S3 N2.
- FPs: ZERO in all eight (nobody hit the bundles/upsert/missing-id traps).
- Unfound by all 8: S2 (behavior-heavy class -> run-local functions),
  S3 (registry machinery for one adapter) — stable headroom.
- Bonus signals: all six skill runs used lens/rule tags and severity
  ordering; both P10 and P13 connected B3 to the full->incremental
  migration context explicitly.

READING: on the untouched case the ordering P13 > P10 > current > baseline
holds with the skill's taste-tier findings (S1/S4/N1-N3) as the entire
margin — the exact profile Metis is designed to add. Preregistered ship-rule
clause (4) (P13 >= current − 1.0): PASSED, margin +1.5.

## P15 holdout gate (2026-07-19 afternoon — final preregistered gate)
- p15-r1 = 16.5: B3 B1 B2 B6 B5 B4(sev-slack) + S6 S5 S1 + N1 N2 N3.
  Missed S2 S3 S4. Zero FP.
- p15-r2 = 15.5: B3 B1 B2 B5 B6 B4 + S6 S5 + N1 N2 N3. Missed S1 S2 S3 S4.
  Zero FP. (Two coverage-suggestion findings neutral, not in key.)
- Mean 16.0: non-inferiority gate (>= 14.25) PASSED; nominally ABOVE
  current 15.25; between P10 16.5 and current. Ordering on the holdout:
  P13 16.75 > P10 16.5 > P15 16.0 > current 15.25 > baseline 14.0.
- HOLDOUT STATUS NOTE: after this round the case has been used as a gate
  for 5 conditions; it remains unseen by tuning but its gate role is now
  spent for the P1x family — future rounds need a fresh holdout.
