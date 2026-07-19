# Metis — reviewing a diff or PR

Do not check every rule in one read; make several passes over the diff, each asking one question.

Lens passes, in order:

1. Correctness and contracts — does the change do what it claims; do all variants honor the advertised contract; are assertions present where data crosses trust boundaries; are failure paths handled; are retried or replayed operations idempotent.
2. Data and state — plain data vs behavior-heavy objects; unions for mutually exclusive states; mutation and I/O isolated at the edges.
3. Control flow and API shape — call sites read cleanly; ifs up, fors down; S/O/I/D; no speculative abstractions. For every surface defect you find here, prescribe the STRUCTURAL remedy, not the cosmetic one: a bloated signature wants a config object or a split of responsibilities, not keyword-only markers; a method doing parsing+shaping+transport+error policy wants those responsibilities separated (pure builders, an I/O port the caller can fake, policy left with the caller); a hard-wired dependency wants a seam because its consumers must test against it. Name the new shape concretely.
4. Tests and slop — test logic that hides intent, goalpost-moving, comments that narrate code, multi-line comments that should be one line, duplicated defensive checks, dead scaffolding, casts that dodge type errors, doc spam, committed generated artifacts.

Findings:

- Verify before reporting. Trace the invariant upstream and downstream first; do not flag "missing validation" that a parser, type, or earlier boundary already guarantees.
- Report each finding as `file:line`, severity (blocking / should-fix / nit), and one sentence stating the problem and the fix. No essays.
- Tag each finding with the lens or rule that produced it, e.g. `[lens 2: data and state]`. Producing the tag forces a systematic sweep of every lens.
- Quality findings from lenses 2-4 are not padding. Report state-modeling, hierarchy, control-flow, test-logic, and comment defects at should-fix or nit severity even when blocking correctness findings dominate.

Sub-agent fan-out, only when a sub-agent tool exists AND the diff is large (more than ~400 changed lines or 8 files): at most one agent per lens, each given only the diff and its lens's checklist; dedupe and verify each finding against the actual code before reporting. Never fan out for small diffs.
