# Metis

Metis is a coding skill for LLM agents. It gives the model strong defaults for design, implementation, testing, code review, and pre-commit verification, and loads section by section so only the current phase's guidance is in play.

## What it pushes models toward

- plain-data domain models over behavior-heavy object hierarchies
- top-down API design and call-site-first thinking
- explicit control flow
- boundary parsing and internal invariants instead of implicit assumptions
- behavior-focused testing
- early performance thinking
- sparse, useful comments instead of code narration
- tiered code review: sequential lens passes by default, sub-agent fan-out for large diffs

These are defaults, not laws. Metis is opinionated, but it should improve the local codebase, not fight it.

## Install

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g
```

Add `-a codex` or `-a claude-code` to install for a single agent. Update with `npx skills update -g`. Release notes live on the [releases page](https://github.com/gitRasheed/metis-skill/releases).

## Use

Metis auto-loads for relevant coding work. You can also invoke it explicitly with `$metis` or `/metis`, depending on the agent UI.

## Evals

Every non-trivial rule was validated or discovered through isolated baseline-vs-Metis runs, and changes that failed their A/B were rejected rather than shipped.

The latest round (`evals/cases-v6/`) ran two preregistered waves over six skill texts plus an external leg on the Aider polyglot benchmark: every arm passed all 72 test runs, and a blinded judge still preferred Metis-guided code 3:1 over the no-skill baseline. The winner shipped as v1.3.0. `evals/cases-v6/REPORT.md` has the full record, including scoring method; earlier rounds live in `evals/cases-v3/` through `evals/cases-v5/`.

## What is in this repo

- `skills/metis/` is the canonical skill
- `.claude/skills/metis/` mirrors it for Claude Code
- `PORTABLE_PROMPT.md` is the plain-markdown version for tools without skill support
- `evals/` holds the eval cases, scoring keys, and reports; raw run output stays git-excluded

## Sources

The core ideas are borrowed, then filtered through the evals:

- call-site-first design by wishful thinking: [SICP](https://sicp.sourceacademy.org/)
- plain data plus systems over object hierarchies: [Casey Muratori, "The Big OOPs"](https://www.computerenhance.com/p/the-big-oops-anatomy-of-a-thirty)
- typed domain state and unions for exclusive states: [Scott Wlaschin, *Domain Modeling Made Functional*](https://pragprog.com/titles/swdddf/domain-modeling-made-functional/)
- parse, don't validate: [Alexis King](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/)
- assertions, negative space, invariant locality, performance ordering: [TigerStyle](https://github.com/tigerbeetle/tigerbeetle/blob/main/docs/TIGER_STYLE.md)
- deep modules and defining errors out of existence: [John Ousterhout, *A Philosophy of Software Design*](https://web.stanford.edu/~ouster/cgi-bin/book.php)
- replay-safe effects with stable identities: [Eric Normand, *Grokking Simplicity*](https://grokkingsimplicity.com/)
- tests are code, minimize test logic: [ThePrimeagen's Boot.dev testing lecture](https://youtu.be/FknTw9bJsXM?t=2500)

The rest (review lenses, fan-out thresholds, thrash detection, LLM-test cleanup) came out of the eval rounds themselves.
