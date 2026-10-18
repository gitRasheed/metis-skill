# cases-v7: v1.4.0 candidates vs v1.3.2 (2026-10-18)

**Verdict:** v1.4.0 ships as B3 plus a one-line SOLID pointer, after three preregistered waves (`PREREG.md`). B and B2 were refused. B3 passed every wave 3 bar on GPT-6.1 Sol at n=6.

## Setup

- **Arms:** A is v1.3.2. B is the first v1.4.0 candidate, revised once to B' (v140b) for review. B2 is B' plus three hypothesis-tied edits.
- **Models:** Opus 5.5 (`claude -p`) and GPT-6.1 Sol `#high` (opencode 2), with the skill force-loaded and n=2 per cell.
- **Cases:**
  - R1 and R2: frozen v5 review keys.
  - J1: a Java pricing feature in enterprise interface/Impl style.
  - J2: a Java dedupe bug with two defect sites and a test that encodes the bug.
  - P1: a Python rename of a persisted field, plus tags.
- **Judging:** a fresh blind Opus judge per case and model ranked 2 A diffs against 2 B diffs, then rated noise. Review cases were scored blind against the frozen keys.

## Results

**Hidden checks:** every arm passes every code case. That is 36 code runs. (J1 and J2 `check.sh` ran the workspace suite from the wrong directory at first; that was fixed, and the arms weren't affected.)

**Review key scores** (mean of 2; R1 max 20, R2 max 30):

| | Opus R1 | Opus R2 | Sol R1 | Sol R2 |
|---|---|---|---|---|
| A, wave 1 | 16.0 | 23.25 | 10.75 | 23.0 |
| B, wave 1 | 13.6 | 23.25 | 9.25 | 22.75 |
| B', R1 rerun | 14.5 | n/a | 11.0 | n/a |
| A, wave 2 rescore | 15.5 | 23.25 | 10.5 | 22.75 |
| B2, wave 2 | 14.5 | **21.25** | 11.25 | 23.75 |

- The wave-2 scorer reproduced A's R2 scores exactly (22.5, 24.0 for Opus), so B2's Opus R2 drop comes from the B2 runs, not the scorer.
- The batch-partial-apply item (S12) was missed by B in 0 of 2 Opus runs. B' caught it in 1 of 2, and B2 in 2 of 2.

**Blinded "rather inherit" judging:** the share of A-vs-B pairs where B was preferred or tied. The bar is 40% for each model.

| | J1 | J2 | P1 | Total |
|---|---|---|---|---|
| Opus, wave 1 (B) | 0/4 | 1/4 | 1/4 | 2/12 (17%) |
| Sol, wave 1 (B) | 2/4 | 0/4 | 2/4 | 4/12 (33%) |
| Opus, wave 2 (B2) | 2/4 | 1/4 | 1/4 | 4/12 (33%) |
| Sol, wave 2 (B2) | 2/4 | 0/4 | 3/4 | 5/12 (42%) |

Judged noise, on a 1 to 5 scale:
- wave 1: Opus A 1.5 vs B 1.67; Sol A 1.33 vs B 2.0;
- wave 2: Opus A 2.0 vs B2 1.17; Sol 1.5 for both.

Fresh judges ranked the same A diffs quite differently between waves, so judge noise at n=2 is large.

**Automated noise scan:**
- Zero rule citations in code or comments in every arm.
- No interface or Impl class removed.
- Comment lines about equal across arms.
- B and B2 ran faster than A in most cells, for example Opus J1 99 s for B2 vs 112 s for A, and Sol R2 102 to 146 s vs 158 s.

**Wave 2 hypotheses:**

| | Prediction | Result |
|---|---|---|
| H1: Design 18 caused the J1 compatibility shims | B2 replaces the old constructor at least as often as A (3 of 4) | **Held.** B 0/4, B2 4/4 |
| H2: The scope line caused inline fixes over new structure | Sol J2 uses a seam in at least 1 of 2 runs | **Failed.** Still inline in 2 of 2. The judges preferred A's decorator both waves |
| H3: WP1 and the test rules caused test bulk | B2's added test lines drop below B's on J1 and J2 | **Mostly held.** Opus J1 164 to 128 lines, Sol J1 352 to 315, Sol J2 76 to 64; Opus J2 flat at 47 |

## Wave 3 (B3, Sol 6.1 #high, n=6, Astra #high judges)

- **B3** is B2 plus Design 3's restored "O" clause: "add new behavior through a focused function, variant, adapter, or wrapper rather than edits spread across existing branches."
- Both arms ran fresh, 60 runs in total.

| Rule | A (v1.3.2) | B3 | Verdict |
|---|---|---|---|
| Hidden checks | 18/18 | 18/18 | pass |
| R1 key score | 9.67 | 9.83 | pass |
| R2 key score | 23.83 | 22.83 (−1.00) | pass, at the bar |
| Inherit, preferred or tied | | 8/18 (44%) | pass |
| Rule citations | 0 | 0 | pass |
| Judged noise, 1 to 5 | 1.06 | 1.11 | pass |
| Mean wall time | 120.5 s | 121.9 s (+1.1%) | pass |

- **H4 held, counting shared helper types as seams.** B3 4/6 (2 decorators, 2 shared helper types), A 3/6. Counting decorators or wrappers only, B3 is 2/6.
- **Post-wave wording check.** I tried rephrasing the O clause in data terms, and the seam rate dropped to 1 or 2 of 6. I reverted it to B3's tested wording. The shipped text differs from B3 only by the SOLID pointer line. A 6-run Sol J2 confirmation on it passed 6/6 hidden checks with zero citations. 4 of 6 runs added a seam: 2 decorators and 2 shared helper types used by both paths.

## What I conclude

- **On waves 1 and 2.** v1.3.2's longer text produced code the Opus judges slightly preferred. The named causes were compatibility shims, inline fixes where a seam was cleaner, and heavier tests. Each one traced to a specific edit and was fixed.
- **On wave 3.** At n=6 the shipped candidate is non-inferior to v1.3.2 on every preregistered bar. It's shorter, applies its rules quietly, and loads only for chunkier work.
- **Judge noise.** At n=2 it was large: fresh judges ranked identical A diffs differently between waves.
- **Opus scope.** Opus did not meet the bar at n=2 in waves 1 and 2, and it was not re-tested at higher n.
- **Margins.** Rule 3 (8/18) and R2 (−1.00) passed at the smallest possible margin, each judged by a single scorer.
- **Added after the round, by Rasheed's decision and not A/B tested.** The Design 19 rewrite (no request-wait-request chains across I/O; decide the shape of the work up front) and two performance sections in performance-and-safety.md, all drawn from Casey Muratori's BSC 2026 talk and interviews.
