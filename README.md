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

If symlinks are blocked on Windows, add `--copy`.

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g -a codex --copy
```

## Update

Skill installs are pull-based, so nothing notifies you of new versions. Run:

```bash
npx skills update -g
```

or watch this repo's [releases](https://github.com/gitRasheed/metis-skill/releases). `CHANGELOG.md` lists what changed in each version.

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

The current round (`evals/cases-v5/`) ran sixteen skill-text permutations through a five-case suite under preregistered ship rules, with blinded pairwise quality judging and about ninety hand-scored runs. The shipped text won 7 of 10 blinded quality comparisons against its predecessor and regressed on nothing. `evals/cases-v5/FINAL-REPORT.md` has the full record; earlier rounds live in `evals/cases-v3/` and `evals/cases-v4/`.

## What is in this repo

- `skills/metis/` is the canonical skill
- `.claude/skills/metis/` mirrors it for Claude Code; regenerate after skill edits with `sed 's|\`references/|\`../../../skills/metis/references/|g' skills/metis/SKILL.md > .claude/skills/metis/SKILL.md`
- `PORTABLE_PROMPT.md` is the plain-markdown version for tools without skill support
- `evals/` holds the eval cases, scoring keys, and reports; raw run output stays git-excluded
