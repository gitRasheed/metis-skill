# Metis

Write code that favors plain data, pure logic, clear call sites, and early architectural thinking. These are strong defaults, not rigid laws: follow the surrounding codebase, framework constraints, and language norms when they clearly matter more.

FIRST, name the phase you are in. THEN read the matching phase file below IN FULL before starting, and hold only it plus the Design principles in mind. Do not read the other phase's file — out-of-phase rules dilute in-phase attention.

- Implementing, fixing, or building anything: read `references/implementing.md` now.
- Reviewing a diff, PR, or another agent's work: read `references/reviewing.md` now.

## Design principles

1. Start from the call site, by wishful thinking: pretend the perfect helpers already exist, name them the way you would want to call them, and get the top-level usage reading cleanly. If the calling code reads awkwardly, the abstractions are wrong — and you find out before building anything.
2. Prefer plain data plus focused functions, modules, or systems over behavior-heavy objects. Draw boundaries around what systems do, not what entities are.
3. Choose the simplest state model that matches reality: discriminated unions for mutually exclusive states, composable data for orthogonal features, and a plain flat record when neither pressure exists — do not over-architect the simple case.
4. Isolate mutation and I/O near the edges. Orchestration decides what happens; inner helpers do narrow, understandable work. Push ifs up, fors down.
5. Assert at boundaries, both where data enters and where it leaves: parsing, persistence, external APIs, state transitions, and function contracts. Check what must be true and, when useful, what must not be.
6. Design for the hardest real requirement first, then simplify downward. When elements of a batch can invalidate each other, classify the whole batch before applying any element.
7. Trace invariants before adding defensive checks: if an upstream parser, type, or boundary already guarantees the value, do not duplicate the check. Add one only where data crosses a trust boundary or the invariant can drift.
8. Prefer self-explaining code with clear, not bloated, names. Comments explain a non-obvious why in one line — never what the code already says.

## Before claiming done

1. Re-read the request and confirm the implementation matches the intended behavior.
2. Run the relevant checks and READ the output; a started command is not a passed command.
3. Review the diff for AI slop: narrating comments, duplicated guards, speculative abstractions, casts that dodge type errors, doc/test spam, committed generated artifacts.
4. Keep only tests and docs that earn their place; delete scaffolding that only helped you think.

## When to relax the defaults

Relax when the codebase has a strong local convention, the framework prefers a different structure, an object-oriented interface is the natural boundary, or mutation is the clearest correct option. Preserve the spirit: clear responsibilities, understandable state transitions, readable tests, intentional API shape.
