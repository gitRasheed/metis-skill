# Metis

Metis is a coding skill for LLMs that improves engineering judgment during implementation, refactoring, and code review.

## What it pushes models toward

- plain-data domain models over behavior-heavy object hierarchies
- top-down API design and call-site-first thinking
- explicit control flow
- boundary assertions instead of implicit assumptions
- behavior-focused testing
- early performance thinking
- sparse, useful comments instead of code narration
- tiered code review: sequential lens passes by default, sub-agent fan-out for large diffs

These are defaults, not laws. Metis is opinionated, but it should improve the local codebase, not fight it.

## Measured results

Metis is eval-driven: every non-trivial rule was validated or discovered through
isolated baseline-vs-Metis runs in clean sandboxed homes.

### v1.2.0 — the skill-permutation campaign (2026-07, `evals/cases-v5/`)

Sixteen full skill-text permutations ran head-to-head through a five-case
suite (two seeded review cases, two hidden-gate agentic cases, one untouched
holdout) under preregistered ship rules, blinded pairwise quality judging,
and ~90 hand-scored runs (`gpt-5.6-sol`). The shipped text (P15) is the only
one that cleared every gate:

- **Blinded code-quality judging:** the P15 implementer core won **7 of 10**
  anonymized pairwise comparisons against the previous skill (mean rating
  8.1 vs 7.8), taking every 9/10 awarded to a candidate all campaign. The
  baseline (no skill) rated 5 and committed **zero tests** in every agentic
  calibration across three cases; skill runs committed 6–16 behavior tests.
- **No regressions at honest n:** review parity on both seeded cases
  (n=5 and n=3 per side), perfect hidden-gate record (17/17 + 19/19 on all
  five torefall reps), favorable direction on the controller gates and on
  the never-tuned holdout case.
- **The placement mechanism** (the campaign's core finding): skill text
  taxes whichever phase holds it. Guidance moved into the phase section that
  needs it keeps the benefit and drops the tax — two seemingly-stronger
  candidates were refused by the preregistered rules for exactly that
  hidden tax, and the shipped diff is only **+14/−5 lines**.
- Full report, per-finding score derivations, adversarial-critique
  adjudications, and the prompt-alignment analysis: `evals/cases-v5/`.

### v1.1.0 round (2026-07, `evals/cases-v3/`, `evals/cases-v4/`)

Highlights from the earlier round (Codex `gpt-5.5`, xhigh reasoning, 60+ runs):

- **Code review, seeded ground truth** (`evals/cases-v4/`): PR-review cases with
  known planted defects, scored on objective recall and precision. Metis found
  **~60% more defect value than baseline** on the tuned case (11.3 vs 7.0 avg,
  max 15.5) and **+53% on a held-out case in a different domain** (12.7 vs 8.3,
  max 19.5), winning or tying every rep.
- **Zero false positives in 39 consecutive review runs** across all conditions —
  the verify-before-reporting rule holds under pressure.
- **Five real unplanted bugs discovered** by Metis conditions across the two review
  cases (e.g. refund ledger rows indistinguishable from charges; an event marked
  processed before its transaction commits). Baseline found one.
- **Design cases** (`evals/cases-v3/`): Metis beat or tied baseline in 9/9
  case-reps; its one reproducible defect (applying batch elements before
  whole-batch classification) became design principle 10, which flipped that
  failure from 1/3 correct to 3/3 on reruns.
- Changes that failed their A/B (e.g. a mandatory per-lens ledger) were rejected,
  not shipped.

Methodology, rubrics, and per-run scoring live in `evals/`; raw run output stays
git-excluded.

## Install

Install from GitHub with:

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g
```

Install only for a specific agent if you want:

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g -a codex
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g -a claude-code
```

If symlinks are blocked on Windows, add `--copy`.

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g -a codex --copy
```

## Use

Metis is meant to auto-load for relevant coding work.

You can also invoke it explicitly with `$metis` or `/metis`, depending on the agent UI.

Example prompts:

```text
Use /metis to refactor this module.
Use $metis to review this design.
Use /metis to implement this feature.
```

## What is in this repo

- `skills/metis/` is the main skill and the canonical copy
- `.claude/skills/metis/` mirrors it for Claude Code with adjusted reference paths; when editing the skill, regenerate it with `sed 's|\`references/|\`../../../skills/metis/references/|g' skills/metis/SKILL.md > .claude/skills/metis/SKILL.md`
- `PORTABLE_PROMPT.md` is the plain markdown version for tools that do not support skills directly
- `evals/` contains isolated baseline-vs-Metis eval cases and a local runner; raw eval runs stay git-excluded
