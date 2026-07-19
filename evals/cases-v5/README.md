# Eval round v5: the skill-permutation campaign (2026-07)

Earlier rounds asked whether Metis beats a bare model. This round asked
which exact skill text is best. Sixteen full-text permutations ran through
a five-case suite under preregistered gates, and the winner (P15) shipped
as v1.2.0. Model under test: gpt-5.6-sol at high reasoning effort, with
case authoring and adversarial critiques at xhigh.

[FINAL-REPORT.md](FINAL-REPORT.md) is the complete record: the verdict,
every score table, the refused candidates, the bisect that isolated why
review gains kept breaking code quality, the confirmation round, the
prompt before-and-after, and the known limits of the method.

## The suite

| case | type | discriminator |
|---|---|---|
| [api-evolution-review](api-evolution-review/) | review | frozen key (max 20.5) with atomic-cause credit and false-positive traps |
| [background-jobs-review](background-jobs-review/) | review | frozen key (max 30) with depth markers and false-positive traps |
| [torefall-v2](torefall-v2/) | agentic | 19 hidden gates, blinded pairwise quality judging, extension-cost probe |
| [charge-controller-agentic](charge-controller-agentic/) | agentic | 12 hidden gates from a pinned spec; starter passes 0, verified golden passes 12 |
| catalog-sync-review (../cases-v4/) | holdout | key frozen before the campaign; used once, as a final gate |

The `rescore-*.md` files hold per-finding hand scores for every run the
report cites, so any number can be re-derived. `skill-permutations/`
contains the ten texts that carried the campaign (P8 through P15), each a
complete SKILL.md, plus the adjudication of the adversarial critique that
tried to edit the winner.

## What made this round different

Ship rules were preregistered in the journal with numeric thresholds
before deciding runs landed, and they refused two candidates (P13, P14c)
whose review gains turned out to hide code-quality regressions. A case no
permutation had ever seen served as the final gate, answering the
winner's-curse objection an adversarial methodology review raised against
the campaign itself. Quality judging was blinded and order-randomized
against an anchored reference, with a same-condition pair to measure the
judge's own noise floor.

The round's main finding is about placement: skill text taxes whichever
phase holds it. The same guidance that degraded generated code when it
sat in implementer-held sections improved review results, at no cost,
once it moved inside the review lens that needed it. P15 is that finding
applied to every line of the shipped diff.
