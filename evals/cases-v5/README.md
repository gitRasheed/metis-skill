# Eval round v5: the skill-permutation campaign (2026-07)

This round asked a harder question than v3/v4 ("does Metis beat baseline?"):
**which exact skill TEXT is best, and why?** Sixteen prompt permutations ran
through a five-case suite under preregistered gates. The winner, P15, is now
`skills/metis/SKILL.md` (v1.2.0). Model under test: gpt-5.6-sol (high effort);
adversarial critiques and case authoring at xhigh.

Read first: **[FINAL-REPORT.md](FINAL-REPORT.md)** — the full campaign report
(verdict, per-arena tables, the three measured regularities, the bisect that
isolated the placement mechanism, the high-n confirmation round, methodology
limits). Then **[P15-alignment-and-rationale.md](P15-alignment-and-rationale.md)**
— the before/after of every changed line and the provenance-note alignment.

## The suite

| case | type | discriminator |
|---|---|---|
| [api-evolution-review](api-evolution-review/) | review | frozen key v1.3 (max 20.5), atomic-cause credit, FP traps |
| [background-jobs-review](background-jobs-review/) | review | frozen key v5.1.1 (max 30), depth markers, FP traps |
| [torefall-v2](torefall-v2/) | agentic | 19 hidden gates + blinded ratings-pairwise + stage-B extension cost |
| [charge-controller-agentic](charge-controller-agentic/) | agentic | 12 hidden gates from a pinned spec; starter 0/12, golden 12/12 |
| catalog-sync-review (../cases-v4/) | holdout | key frozen pre-campaign; used only as a final gate, never for tuning |

Each `rescore-*.md` file contains per-finding, re-derivable hand scores for
every run referenced in the report. `skill-permutations/` holds the ten
load-bearing texts (P8–P15) plus the adversarial-critique adjudications;
each is a complete SKILL.md variant.

## Method highlights (what made this round different)

- **Preregistered ship rules** with numeric thresholds, journaled before
  deciding runs landed — they refused two seemingly-strong candidates (P13,
  P14c) whose review gains hid agentic regressions.
- **An untouched holdout** answering the winner's-curse critique from an
  adversarial methodology review of the report itself.
- **Blinded, order-randomized pairwise quality judging** with an anchored
  reference, a noise-floor pair, and an objective extension-cost metric that
  independently corroborated the judges.
- **The placement mechanism** (the round's core finding): skill content taxes
  whichever phase holds it; the same guidance scoped into the phase that
  needs it keeps the benefit and drops the tax. P15 is that finding as a
  shipping prompt.
