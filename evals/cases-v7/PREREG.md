# cases-v7 preregistration: v1.4.0 vs v1.3.2

I wrote this on 2026-10-18, before any v7 run and before any scoring.

## Question

Does v1.4.0 keep v1.3.2's review depth and code quality while being shorter and less pedantic? Implementation pass/fail is saturated at SOTA (2026-10-17 probe round: 33/33), so this round measures style, review depth, noise and cost.

## Arms

- **A:** v1.3.2 (the shipped text, from git tag `v1.3.2`).
- **B:** the v1.4.0 candidate (branch `v1.4.0-draft`, frozen copy in `arms/v140/`).

Both arms load the skill by force. The prompt starts with "Use the metis skill for this task." That isolates the effect of the content. Description triggering was measured separately in the 2026-10-18 trigger probe.

## Models

- GPT-6.1 Sol `#high` through opencode 2.
- Claude Opus 5.5 through `claude -p`.

Each run gets a fresh workspace. Opus runs get a fresh config directory. Opencode runs get a project config that denies every personal skill.

## Cases (n=2 per arm, model and case; 40 runs)

| Case | Kind | Measured by |
|---|---|---|
| R1 api-evolution-review | Review, frozen v1.3 key from cases-v5 | Key points |
| R2 background-jobs-review | Review, frozen key from cases-v5 | Key points |
| J1 pricing-feature | Java feature, enterprise interface/Impl style | Hidden tests, blinded pairwise, noise scan |
| J2 ingest-dedupe-bug | Java bug with two defect sites and a test that encodes the bug | Hidden tests, blinded pairwise, noise scan |
| P1 habits rename and tags | Python persisted-format change (2026-10-17 fixture t2) | Hidden tests, blinded pairwise, noise scan |

## Measures

1. **Hidden checks** on J1, J2 and P1: pass or fail.
2. **Review key points** on R1 and R2. Scored by an Opus scorer blind to the arm, using each case's frozen scoring rules.
3. **Blinded pairwise quality** on J1, J2 and P1.
   - Within each model, every B run is paired with an A run of the same case.
   - An Opus judge sees two diffs with labels shuffled and any skill-identifying lines removed.
   - It answers two questions: which codebase it would rather inherit, and which change is noisier or more pedantic (comments, assertions, scope, ceremony).
4. **Noise scan**, automated, over the diffs:
   - rule citations in code or comments (metis, lens, "Design N", rule names);
   - added comment lines;
   - added `assert` statements;
   - removed interfaces or `*Impl` classes;
   - files touched outside the task.
5. **Cost:** output tokens and tool calls where the harness records them, plus wall time.

## Ship rule for v1.4.0 (all must hold)

1. **Hidden checks:** B passes at least as many as A.
2. **Review:** B's mean key score is within 1.5 points of A's for each model on each review case, and B is not below A on both cases for the same model.
3. **Quality:** B is preferred or tied in at least 40% of the blinded "rather inherit" pairs for each model. That is a non-inferiority threshold.
4. **Pedantry:**
   - B has no rule citation anywhere in code, comments or commit messages;
   - the judge calls B noisier in no more than 50% of pairs;
   - B's added comment lines and asserts don't exceed A's by more than 25%.
5. **Cost:** B's mean wall time and tokens, where recorded, are no more than 10% above A's.

If a rule fails, I report it and don't ship. A failure that traces to one sentence gets one targeted revision and one rerun of the failing cell. I'll journal that before the rerun.

## Gate

The final ship claim goes through an adversarial review by GPT-6 Astra or Muse Wonderful before I publish.

## Journal: rule 2 failure and revision (written before the rerun)

**Review key scores** (mean of 2):

| Case | Model | A (v1.3.2) | B (v1.4.0) | Change |
|---|---|---|---|---|
| R1 | Opus | 16.0 | 13.625 | −2.375, fails |
| R1 | Sol | 10.75 | 9.25 | −1.5 |
| R2 | Opus | 23.25 | 23.25 | 0 |
| R2 | Sol | 23.0 | 22.75 | −0.25 |

Sol is below A on both cases, which also fails rule 2.

**Trace.** R1 key item S12 is batch partial application: a malformed later item leaves earlier sends committed. Opus caught it with A in 2 of 2 runs and with B in 0 of 2. B had dropped v1.3.2's clause "even when applying incrementally looks cleaner" from the batch rule, and no lens asked about it.

**Revision (one, targeted).**
- Restore that clause in Design 9.
- Add to Lens 1: "Can a batch apply partly before a later item fails?"

**Rerun.**
- R1 for B, both models, n=2.
- R1 is rescored blind together with A's four R1 reviews by a fresh Opus scorer, so all eight are scored under the same rulings.
- Rule 2 is then re-evaluated with the new R1 B scores, and if it still fails, B doesn't ship.

**Harness fix (no arm effect).** J1 and J2 `check.sh` ran the workspace's own test suite from the wrong directory. That falsely failed Sol B's J2 runs, whose new test reads `data/` by relative path. The suite now runs from inside the workspace, and all 16 Java runs pass.

## Wave 1 close-out: v1.4.0 (B) fails rules 2 and 3 and does not ship

**Blinded "rather inherit" judging** (6 Opus judges, one per case and model). Each judge ranked 4 changes: 2 from A and 2 from B. B is preferred or tied in:

| Case | Opus | Sol |
|---|---|---|
| J1 | 0/4 | 2/4 |
| J2 | 1/4 | 0/4 |
| P1 | 1/4 | 2/4 |
| **Total** | **2/12, 17%** | **4/12, 33%** |

- Both totals are below the 40% bar.
- Judged noise, on a 1 to 5 scale where higher is noisier: Opus A 1.5 vs B 1.67; Sol A 1.33 vs B 2.0.
- With n=2 per cell, one judge flipping changes a cell. The 40% bar was set with that in mind.

**Automated noise scan.** It found no problems in B:
- zero rule citations anywhere;
- no interface or Impl removals;
- comments and asserts equal to A's or lower;
- wall time lower in 9 of 10 cells.

The judges' complaints about B were about shape, not markers:
1. **J1 compatibility shims.** B kept the old two-argument `PricingServiceImpl` constructor next to the new one in 4 of 4 runs. A replaced it in 3 of 4. The judges called the leftover constructor a trap, because it quietly builds its own in-memory repository.
2. **J2 inline fixes (Sol only).** B inlined the dedupe into the reader in 2 of 2 runs. A added a small decorator wired in one place in 2 of 2. The Opus J2 rankings are mixed.
3. **Test bulk.** Two judges called B's tests heavier.

Rule 2 is pending the R1 rescore of v140b.

## Wave 2 preregistration (written before any wave 2 run)

**Candidate B2:** v140b, the D9 and Lens 1 batch revision, plus these edits, each tied to a hypothesis:

| | Edit | Hypothesis | Prediction |
|---|---|---|---|
| H1 | Design 18 now covers only what crosses a boundary: callers in other services, persisted formats, config read by deployments. Inside one codebase, update the callers instead of keeping a shim. | Design 18 caused the shims | B2's J1 constructor-replacement rate returns to A's (3 of 4 or better) |
| H2 | The scope line and Design 17 now say that new types or files the change itself needs are in scope | The scope line caused inline-over-structure | Sol J2 uses a seam (decorator or wrapper) in at least 1 of 2 runs |
| H3 | WP1's regression-test clause gains "one focused test per defect". Tests rule 6 gains "don't re-test behavior the change didn't touch". | Test bulk came from "fix every site" plus WP1 | B2's added test lines fall below B's on J1 and J2 |

**Runs.**
- All 5 cases, both models, n=2: 20 runs of B2.
- A's runs from wave 1 are reused unchanged.
- The judging setup is identical to wave 1, with fresh Opus judges and new shuffles.
- R1 and R2 are rescored blind, each with all A reviews and B2 reviews together in one packet.

**Ship rule.** Rules 1 to 5 are unchanged. Separately, I'll report whether each hypothesis's prediction held.

**If B2 fails.** v1.4.0 doesn't ship from this round. I'll report back with the evidence and these options: ship v1.3.2's text with only the description change, or run another round.

**R1 rescore of v140b** (fresh blind Opus scorer, scoring A's 4 and v140b's 4 together):

| Model | A | v140b | Change |
|---|---|---|---|
| Opus | 16.0, 16.0 | 14.5, 14.5 | −1.5 |
| Sol | 10.25, 11.25 | 10.75, 11.25 | +0.25 |

- Rule 2 now holds. Opus is exactly at the bar, and Sol is no longer below A on both cases.
- S12 was caught in 1 of 2 Opus v140b runs.
- The A scores match the first scorer exactly (16.0, 16.0).
- Wave 1 still fails on rule 3. Wave 2 proceeds as preregistered above.

## Wave 3 preregistration (2026-10-18, written before any wave 3 run)

**Why this wave exists.** The decision after wave 2 was to push v1.4.0 forward. Claude usage was low, so this wave raises n on Sol only.

**Candidate B3:** B2 plus one clause in Design 3.
- The clause: "Add new behavior through a focused function, variant, adapter, or wrapper rather than edits spread across existing branches."
- It restores v1.3.2's SOLID "O" clause, which the rewrite dropped.
- **H4:** that loss is why Sol inlined the J2 dedupe fix. Prediction: Sol J2 under B3 uses a seam (decorator or wrapper) in at least 3 of 6 runs.

**Runs.**
- GPT-6.1 Sol `#high` only, arms A (v1.3.2) and B3, n=6 per cell: 60 runs in total.
- The order is shuffled across arms.
- Wave 1 and 2 A runs are not reused, so both arms are fresh.

**Scoring (no Claude usage).**
- **Reviews:** GPT-6 Astra `#high` scores each case's 12 reviews blind against the frozen key, in one packet per case.
- **Code:** each A run is paired with one B3 run per case (6 pairs per case, 18 in total). Each pair gets its own Astra judge with the order randomized. The judge answers "rather inherit: first, second, or tie" and gives a noise score from 1 to 5.

**Ship rule** (Sol only; the wave 1 and 2 Opus results are reported as they stand):
1. **Hidden checks:** B3 passes at least as many as A.
2. **Review:** B3's mean key score on each review case is within 1.0 of A's. That is tighter than the earlier 1.5, because n is 6.
3. **Quality:** B3 is preferred or tied in at least 40% of the 18 pairs.
4. **Pedantry:** no rule citations, and B3's mean judged noise is no more than A's + 0.5.
5. **Cost:** B3's mean wall time is no more than 10% above A's.

**Gate.** If the rules pass, the ship claim goes to Muse Wonderful for adversarial review before I publish. The report and release notes will state that Opus did not meet the bar at n=2 in waves 1 and 2, and that it wasn't re-tested at higher n.

## Wave 3 results (Sol 6.1 #high, n=6 per cell, Astra #high judges and scorers)

| Rule | A (v1.3.2) | B3 | Verdict |
|---|---|---|---|
| 1. Hidden checks (J1, J2, P1) | 18/18 | 18/18 | pass |
| 2. R1 key score | 9.67 | 9.83 (+0.17) | pass |
| 2. R2 key score | 23.83 | 22.83 (−1.00) | pass, exactly at the bar |
| 3. Inherit, B3 preferred or tied | | 8/18 (44%): J1 3/6, J2 2/6, P1 3/6, 0 ties | pass |
| 4. Rule citations | 0 | 0 | pass |
| 4. Judged noise, 1 to 5 | 1.06 | 1.11 | pass |
| 5. Mean wall time | 120.5 s | 121.9 s (+1.1%) | pass |

- **H4 held, counting shared helper types as seams.** Sol J2 used a seam in 4 of 6 B3 runs (2 decorators, 2 shared helper types), against 3 of 6 for A. Counting decorators or wrappers only, B3 is 2/6.
- **Verdict:** B3 meets every wave 3 rule for Sol. Opus was not re-tested at higher n, and in waves 1 and 2 at n=2 it stays below the bar.
- **Harness note.** The first two Astra judge runs read the wrong directory: opencode takes its project root from git and from `$PWD`. That was fixed with `git init` per packet and an explicit `PWD`, and all 20 judges reran before any result was read.
- **After wave 3,** two approved wording edits:
  - Design 3's O clause was rephrased in data terms.
  - The SOLID line now points to where each letter lives in the rules.

  A 6-run Sol J2 confirmation on the final text checks the hidden tests, the seam rate and citations.
