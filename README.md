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

Metis is eval-driven: every non-trivial rule was validated or discovered
through isolated baseline-vs-Metis runs in clean sandboxed homes.

### v1.2.0: the skill-permutation campaign (2026-07, `evals/cases-v5/`)

Sixteen full skill-text permutations ran head-to-head through a five-case
suite: two seeded review cases, two hidden-gate agentic cases, and one
holdout no candidate ever saw during tuning. Ship rules were preregistered
with numeric thresholds, code quality was judged on blinded anonymized
diff pairs, and roughly ninety runs were hand-scored against frozen keys
(model under test: gpt-5.6-sol). The shipped text, P15, was the only one
to clear every gate.

The headline numbers: the P15 implementer core won 7 of 10 blinded
quality comparisons against the previous skill (mean rating 8.1 vs 7.8)
and took every 9 awarded to a candidate all campaign. The bare model
rated 5 and committed zero tests in every agentic calibration across
three cases, while skill runs committed 6 to 16 behavior tests. At
confirmation n (five reps per side on the main cases) P15 showed no
regression anywhere: review parity, a perfect hidden-gate record, and a
favorable direction on both cases it never trained on.

The campaign's core finding is that skill text taxes whichever phase
holds it. Two candidates with better single-arena scores were refused by
the preregistered rules for exactly that hidden tax, and the shipped diff
moves guidance into the phase sections that need it. It is 14 insertions
and 5 deletions on the previous text. The full report, per-finding score
derivations, and critique adjudications are in `evals/cases-v5/`.

### v1.1.0 round (2026-07, `evals/cases-v3/`, `evals/cases-v4/`)

Highlights from the earlier round (Codex gpt-5.5, xhigh reasoning, 60+
runs):

- On PR-review cases with planted defects (`evals/cases-v4/`), scored on
  recall and precision against frozen keys, Metis found about 60% more
  defect value than baseline on the tuned case (11.3 vs 7.0 average) and
  53% more on a held-out case in a different domain (12.7 vs 8.3),
  winning or tying every rep.
- Zero false positives in 39 consecutive review runs across all
  conditions; the verify-before-reporting rule holds under pressure.
- Metis conditions found five real, unplanted bugs across the two review
  cases, such as refund ledger rows indistinguishable from charges, and
  an event marked processed before its transaction commits. Baseline
  found one.
- On design cases (`evals/cases-v3/`), Metis beat or tied baseline in
  9 of 9 case-reps. Its one reproducible defect (applying batch elements
  before whole-batch classification) became design principle 10, which
  flipped that failure from 1/3 correct to 3/3 on reruns.
- Changes that failed their A/B, like a mandatory per-lens ledger, were
  rejected rather than shipped.

Methodology, rubrics, and per-run scoring live in `evals/`; raw run
output stays git-excluded.

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

## Update

Skill installs are pull-based, so nothing notifies you of new versions.
To get updates, either run:

```bash
npx skills update -g
```

or watch this repo's [GitHub releases](https://github.com/gitRasheed/metis-skill/releases)
(each version ships as a tagged release with notes; see also `CHANGELOG.md`).

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
