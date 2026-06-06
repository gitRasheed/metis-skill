# Metis Eval Rubric

Score each criterion `0`, `1`, or `2`.

- `0`: missing or actively bad
- `1`: partially present, vague, or inconsistent
- `2`: clearly present and useful

Use the same rubric for baseline and Metis outputs. Prefer judging concrete behavior over polish.

## Quality Criteria

1. **Expected behavior first**: States or tests intended behavior before locking in implementation.
2. **Call-site-first design**: Shapes APIs around clear top-level usage instead of helper internals.
3. **Plain data and focused systems**: Avoids behavior-heavy objects when data plus functions/modules would be simpler.
4. **SOLID without ceremony**: Uses single responsibility, stable contracts, narrow interfaces, and dependency direction without speculative class/factory scaffolding.
5. **Boundary and idempotency thinking**: Validates trust boundaries, state transitions, duplicate delivery, replay, and negative cases where relevant.
6. **Simple control flow**: Keeps high-level branching visible and leaf functions narrow.
7. **Behavior-focused testing**: Prefers integration, macro behavior, contract, or acceptance checks over low-value unit tests.
8. **Anti-gaming test discipline**: Avoids changing tests just to pass; treats weak generated tests as temporary scaffolding.
9. **Anti-slop hygiene**: Avoids unnecessary comments, docs, defensive wrappers, broad rewrites, and style-inconsistent code.
10. **Final verification**: Names concrete checks run or needed before claiming completion; does not claim success without evidence.

## Pairwise Verdict

After scoring, choose one:

- `Metis wins`
- `Metis core wins`
- `Metis examples wins`
- `Baseline wins`
- `Tie`
- `Inconclusive`

The winner should be the output more likely to produce maintainable, correctly scoped code in the Metis style.

## Efficiency Metadata

Record when available:

- input tokens
- output tokens
- total tokens
- tool calls
- shell commands
- files read or written
- wall-clock time
- model name
- run date

If the runner does not expose a value, write `not exposed`.

Efficiency is not the main score. A higher-cost Metis run can still win if it materially improves correctness, maintainability, or verification.
