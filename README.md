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

## Update

```bash
npx skills update -g
```

Release notes live on the [releases page](https://github.com/gitRasheed/metis-skill/releases).

## Use

Metis is meant to auto-load for relevant coding work.

You can also invoke it explicitly with `$metis` or `/metis`, depending on the agent UI.

```text
Use /metis to refactor this module.
Use $metis to review this design.
Use /metis to implement this feature.
```

## Evals

Every non-trivial rule in the skill was validated or discovered through isolated baseline-vs-Metis runs, and changes that failed their A/B were rejected rather than shipped.

The latest round (`evals/cases-v6/`) ran two preregistered waves over six skill texts, scored reviews with per-transcript sub-agents adjudicated by hand, and added an external leg on the Aider polyglot benchmark: every arm passed all 72 test runs, and a blinded judge still preferred Metis-guided code 3:1 over the no-skill baseline. The winner shipped as v1.3.0. `evals/cases-v6/REPORT.md` has the full record; earlier rounds live in `evals/cases-v3/` through `evals/cases-v5/`.

## What is in this repo

- `skills/metis/` is the canonical skill
- `.claude/skills/metis/` mirrors it for Claude Code
- `PORTABLE_PROMPT.md` is the plain-markdown version for tools without skill support
- `evals/` holds the eval cases, scoring keys, and reports; raw run output stays git-excluded
