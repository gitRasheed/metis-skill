# Metis (condensed)

Write code that favors plain data, pure logic, clear call sites, and
early architectural thinking. Strong defaults, not laws: follow the
codebase, framework, and language when they clearly matter more.

## Design

1. Start from the call site by wishful thinking: write the caller you
   wish existed, then make helpers match. Awkward calling code means
   wrong abstractions — found before building anything.
2. Plain data + focused functions/modules over behavior-heavy objects.
   Boundaries around what systems DO, not what entities ARE.
3. Simplest state model that matches reality: discriminated unions for
   mutually exclusive states, composable data for orthogonal features,
   a flat record when neither pressure exists.
4. Mutation and I/O at the edges; orchestration decides, leaves do
   narrow work; push ifs up, fors down.
5. Assert at boundaries both ways (entering and leaving): parsing,
   persistence, APIs, state transitions. Check what must be true AND
   what must not be.
6. Design for the hardest real requirement first, then simplify down.
   When batch elements can invalidate each other, classify the whole
   batch before applying any element.
7. Trace invariants before adding defensive checks: if an upstream
   parser/type/boundary already guarantees it, do not re-check.

## Working

- Prefer pure functions and data transformations; adapters around
  awkward external APIs; invariants next to the operations that need
  them (do not validate early and trust it later if data can drift).
- Self-explaining names; comments only for a non-obvious WHY, one line.
- SOLID as a checklist without ceremony: one reason to change; extend
  via a focused function/variant/adapter, never pre-built extension
  points; variants honor the contract; narrow interfaces; policy
  depends on stable abstractions, not I/O details.

## Tests

Decide expected behavior BEFORE implementing; prefer a failing behavior
check first (red-green) when practical. Prefer integration/contract
tests over implementation-mirroring unit tests. One visible behavior
per test; no loops/branches hiding the harness. Never move goalposts to
make broken code look right. Delete scaffolding tests before done.

## Review (diff/PR)

Passes, one question each: 1) correctness/contracts + failure paths;
2) data/state (plain data, unions, mutation at edges); 3) control flow
and API shape (call sites, ifs-up-fors-down, no speculation); 4) tests
and slop (narrating comments, duplicate checks, dead scaffolding).
Verify each finding against the code before reporting; report as
file:line, severity, one sentence. Quality findings are not padding.

## Before claiming done

Re-read the request; run the relevant checks and READ the output; review
the diff for slop (narrating comments, duplicated guards, speculative
abstractions, doc/test spam); keep only tests that earn their place.

## When implementing or fixing (agentic work)

1. For every requirement, write the failing behavior check FIRST when a
   test harness exists; watch it fail, fix, watch it pass. Commit the
   check with the fix — a fix without a guarding test is half done.
2. Fix causes, not sites: when two symptoms share a root, restructure
   the root; when a defect class exists once, look for its siblings
   before finishing (the same stale-check or missing-boundary usually
   appears more than once).
3. Optimize for the NEXT change: after the fix works, ask what the next
   feature in this area costs; if your structure makes it expensive
   (touching many branches/classes), restructure to data + one system
   now, while context is loaded.
4. Long task lists do not suspend quality: the last requirement gets the
   same test, assertion, and naming discipline as the first. Do not drop
   the quality pass because the functional list is long.
5. Leave the campsite cleaner: delete dead code and scaffolding you
   find mid-task if it is inside the code you already changed.
