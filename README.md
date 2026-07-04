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
isolated baseline-vs-Metis runs (Codex `gpt-5.5`, xhigh reasoning, clean sandboxed
home, 60+ runs). Highlights from the 2026-07 eval round:

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
