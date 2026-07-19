# v6 campaign report: parse-don't-validate, book concepts, and external validity

Date: 2026-07-19 (single overnight campaign, two preregistered waves).
Status: complete. Ship recommendation: SYNTH, pending owner sign-off.
Nothing has been committed or released from this campaign.

## 1. Outcome

SYNTH is the only arm that passed every preregistered gate. It is the
shipped v1.2.0 text plus six edits: the parse-don't-validate rewrite of
principle 6, a compression of the trace-invariants rule, Wlaschin's
typed-domain-records tail on principle 3, Ousterhout's
define-errors-out-of-existence as principle 11, effects-need-identity as
principle 12, and a deep-modules working rule. 187 lines / 2,694 words
against the current 184 / 2,549.

Refused by gates: CONC (parse unification with review-side deletions;
failed the review gate), CLITE (parse rewrite alone; review-safe but
taste 7-7-0, no majority win), RICH (book concepts alone; all gates
green but superseded by SYNTH passing its stricter wave-2 bar).

## 2. Arms

Wave 1 (texts frozen before any scoring):

- BASE: no skill.
- CUR: shipped v1.2.0 (184L / 2,549w).
- CONC: P6 rewritten to parse-don't-validate, LLM rule 2 and review
  lens 1 compressed, two working rules deleted (181L / 2,498w).
- RICH: CUR + typed-records tail on P3, define-errors P11,
  effects-identity P12, deep-modules working rule (187L / 2,684w).

Wave 2 (preregistered after wave 1 attribution, before any wave-2 scoring):

- CLITE: CUR + P6 rewrite + rule-2 compression only (184L / 2,559w).
- SYNTH: RICH + the same two edits (187L / 2,694w).

## 3. Protocol changes from v5

- Skill arms shipped the full skill directory (SKILL.md plus all six
  reference files), matching a real install. The run prompt mentioned
  that references exist but did not command reading them; that is the
  reference-loading instrumentation condition.
- Every run's events.jsonl was scanned for token usage (uncached input
  plus output, final turn.completed basis), commands touching
  references/*.md, and agent-message counts.
- Review scoring used one sub-agent per transcript against the frozen
  v5.1.1 key with the rescore-suite precedents, followed by a personal
  adjudication pass: every UNSURE resolved, FP penalties checked against
  the key's severity-symmetric rule, quotes spot-checked against
  transcripts, and one cross-sheet consistency correction applied
  (CLITE-r3 D7 credited on the wave-1 two-of-three precedent).
- v6 numbers are comparable within v6 only. The scorer regime differs
  from v5 hand-scoring, so no cross-campaign absolute comparisons.

## 4. Review case (background-jobs, key v5.1.1, max 30, n=3 per arm)

| Arm | Runs | Mean | FPs |
|---|---|---|---|
| BASE | 20.75 / 21.75 / 21.0 | 21.17 | 0 |
| CUR | 20.5 / 19.0 / 20.5 | 20.00 | 0 |
| CLITE | 21.0 / 22.5 / 17.5 | 20.33 | 0 |
| SYNTH | 18.5 / 22.25 / 19.5 | 20.08 | 0 |
| RICH | 19.25 / 21.75 / 18.5 | 19.83 | 0 |
| CONC | 15.5 / 17.25 / 19.5 | 17.42 | 1 |

Gate G2 (mean >= CUR - 1.0 = 19.0): CONC fails, everyone else passes.
CONC's FP is the LIGHTWEIGHT_TYPES factual error, fourth occurrence,
every occurrence on a chassis variant and never on the shipped text or
on the parse-rewrite arms.

BASE at parity with CUR on raw totals at n=3 is an honest surprise
worth keeping visible. Under this scorer regime and this case, the
skill does not buy raw review points; its review value in v5 showed up
in finding structure, severity discipline, and false-positive
avoidance. The result is within n=3 noise and uses a different prompt
shape for BASE (no skill to read), so it is a flag for a future
higher-n look, not a conclusion.

## 5. Internal agentic case (torefall v2.1, 19 hidden gates, stage A + B)

Stage A gates: 19/19 in all runs of all arms except RICH-r1 at 18/19
(g14 reconnect). SYNTH and CLITE: 19/19 in every run, stages A and B.

Stage-B extension cost (insertions plus deletions with verifier green,
the what-does-the-next-feature-cost metric):

| Arm | Mean | Note |
|---|---|---|
| BASE | 209 | smallest stage-A footprint, fewest tests to extend |
| CONC | 235 | |
| RICH | 240 | green runs only |
| CLITE | 252 | |
| SYNTH | 257 | |
| CUR | 300 | |

Every candidate extended cheaper than CUR. Gate ceiling was 390; all
pass.

## 6. External benchmark (Aider polyglot, 6 design-heavy Python exercises)

Runs: 72 solution runs across six arms. Result: 72/72 PASS, zero
test-file tampering. Frontier-model pass rates are at ceiling on this
suite with or without a skill, so the external signal is entirely in
code quality, which is exactly what the campaign wanted to measure.

Blinded pairwise taste on the diffs (order randomized, skill lines
stripped, anchor = CUR solution of the same exercise and rep):

| vs CUR | W-L-T | Mean ratings (cand vs CUR) |
|---|---|---|
| BASE | 3-9-0 | 4.92 vs 5.58 |
| CONC | 8-3-1 | 5.42 vs 5.00 |
| SYNTH | 7-4-1 | 5.42 vs 5.17 |
| RICH | 5-6-1 | 5.17 vs 5.25 |
| CLITE | 6-6-0 | 5.25 vs 5.25 |

The BASE row is the external-validity headline: on a public benchmark
whose tests everyone passes, a blinded judge preferred Metis-guided
code 3:1 over baseline.

## 7. Taste, pooled (external + torefall)

Torefall judging (n=3 wave 1, n=2 wave 2): BASE 0-3 (5.33 vs 8.67),
CONC 2-1, RICH 3-0 sweep (8.33 vs 7.00), CLITE 1-1, SYNTH 1-1.

Pooled vs CUR: CONC 10-4-1, SYNTH 8-5-1, RICH 8-6-1, CLITE 7-7-0,
BASE 3-12-0.

## 8. Reference loading and token cost

Reference-read instrumentation across 54 skill-arm runs (9 review, 9
torefall stage A, 36 polyglot): 54/54 read all six reference files.
Under a workspace-plus-mention protocol the references are always
read, in every mode. The concern that reference files silently go
unread did not reproduce here. The cost side is real, though: on
torefall, skill arms consumed about 110k uncached input tokens against
BASE's 62k, and on polyglot about +9k per run, dominated by the
references, not the skill text. Trimming SKILL.md wording (CONC) saved
only about 2% of input. If token cost ever matters, the lever is
reference size or read-on-demand behavior, not prose compression.

## 9. Attribution: what wave 2 settled

Wave 1 left a clean dissociation: CONC won implementation taste
decisively but failed review depth; RICH swept the big design task but
was neutral externally. The wave-2 isolation arms resolved it:

- CLITE (parse rewrite alone) is review-safe: 20.33, zero FPs. The
  rewrite did not cause CONC's review failure; the deletions did.
- CLITE's taste is neutral (7-7-0): the rewrite alone is not enough to
  move taste reliably.
- SYNTH (rewrite plus book concepts) keeps review parity (20.08) and
  the taste majority (8-5-1), with perfect gates and cheaper
  extensions.

The interference law from v5 holds in a sharper form: compression that
deletes phase-relevant text costs that phase, while rewording that
deepens a principle (parse-don't-validate) is free or positive. The
skill did not get shorter for free; the safe compression saved 2% of
tokens and the unsafe compression cost 2.6 review points.

## 10. Preregistration compliance and caveats

All gates and decision rules were journaled before their scoring, both
waves. Wave-2 caveats, disclosed rather than buried:

- SYNTH's pooled 8-5-1 at n=14 is a majority, not statistical
  significance on its own. Its components carry independent evidence
  (RICH's 3-0 torefall sweep at full n, the parse rewrite's 10-4-1 via
  CONC), and the composite is the basis of the recommendation.
- Running a second wave gives a second lottery ticket; the mitigation
  was preregistering wave-2 rules before wave-2 data and requiring a
  strictly harder bar for SYNTH than wave 1's.
- The sub-agent scorer regime reads slightly generous against v5
  hand-scoring. All gate comparisons are within-regime.
- Review BASE parity (section 4) is unexplained and queued for a
  higher-n look.

## 11. Recommendation

Ship SYNTH as v1.3.0 pending owner sign-off, mirroring the six edits
into the Claude Code mirror and PORTABLE_PROMPT.md. CLITE is the
conservative fallback if the owner prefers zero new principles; it is
review-safe and taste-neutral. Keeping v1.2.0 unchanged is defensible
but leaves a measured extension-cost improvement and the
external-taste-positive parse rewrite on the table.

## 12. Cost

195 codex runs (134 agent runs, 61 judge runs): 6.93M uncached input,
1.48M output tokens, priority tier, roughly five hours wall-clock
including two full preregistered waves. 18 Claude scoring sub-agents,
about 0.6M tokens. Wall-clock per decision roughly halved against v5
by running factorial waves and fanning scoring out to sub-agents.
