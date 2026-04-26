# Metis

Metis is a coding skill for LLMs that improves engineering judgment during implementation, refactoring, and code review.

## What it pushes models toward

- plain-data domain models over behavior-heavy object hierarchies
- top-down API design and call-site-first thinking
- explicit control flow
- boundary assertions instead of implicit assumptions
- behavior-focused testing
- early performance thinking
- sparse, useful comments instead of AI-slop narration

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

## Use

Metis is meant to auto-load for relevant coding work.

You can also invoke it explicitly:

- Codex: `$metis`
- Claude Code: `/metis`

Example prompts:

```text
Use $metis to refactor this module.
Use $metis to review this design.
Use $metis to implement this feature.
```

## What is in this repo

- `skills/metis/` is the main skill
- `.claude/skills/metis/` is the Claude Code version
- `PORTABLE_PROMPT.md` is the plain markdown version for tools that do not support skills directly
