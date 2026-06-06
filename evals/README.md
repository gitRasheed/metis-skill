# Metis Evals

These evals test whether Metis changes coding-agent behavior in the intended direction.

They are not loaded by the skill at runtime. They are maintainer materials for testing prompt changes, comparing model behavior, and explaining observed skill lift.

## What These Evals Measure

- clearer call-site-first API design
- simpler plain-data/module boundaries
- less speculative abstraction
- better idempotency and boundary thinking
- better behavior-first local checks
- less unit-test and documentation slop
- stronger final verification before claiming completion

## How To Run

For each case in `cases-v3/`:

1. Run a baseline agent with only the case prompt.
2. Run a Metis core agent with the same case prompt plus `SKILL.md`.
3. Run a Metis examples agent with the case prompt, `SKILL.md`, and `references/examples.md`.
4. Save raw outputs under a git-excluded `eval-runs/<date>/cases-v3/<model>-<reasoning>/<case>/` directory.
5. Score all outputs using `rubric.md`.
6. Record any available cost metadata such as token usage, tool-call count, wall-clock time, and whether the agent used shell or file tools.

## What To Commit

Commit:

- eval case prompts
- rubrics
- stable summaries
- methodology changes

Do not commit by default:

- raw agent transcripts
- one-off local scoring scratch files
- large run artifacts

## Interpreting Results

Treat these as regression and behavior-shaping evals, not scientific proof. A useful eval catches a real failure mode or shows whether a prompt change moves behavior in the desired direction.
