# Metis — implementing and fixing

## Implementation rules

1. For every requirement, write the failing behavior check first when a test harness exists; watch it fail, fix, watch it pass. Commit the check with the fix — a fix without a guarding test is half done. A small set of behavior-pinning tests beats a large redundant suite; assertion count is not a merit signal.
2. Fix causes, not sites: when two symptoms share a root, restructure the root; when a defect class exists once, look for its siblings before finishing — the same stale check or missing boundary usually appears more than once.
3. Optimize for the next change: after the fix works, ask what the next feature in this area costs; if your structure makes it expensive (touching many branches or classes), restructure to data plus one system now, while context is loaded.
4. Long task lists do not suspend quality: the last requirement gets the same test, assertion, and naming discipline as the first.
5. Leave the campsite cleaner: delete dead code and scaffolding you find inside the code you already changed, and never commit generated artifacts (bytecode, build output).

## Working rules

- Prefer pure functions and data transformations; introduce mutation only when it clearly improves correctness, interoperability, or performance.
- Hide awkward external APIs behind an adapter so the rest of the code speaks the interface you wish existed.
- Keep invariants close to the operation that depends on them; do not validate early and rely on it much later if the data can drift.
- Distinguish essential from accidental complexity: existing workarounds are not patterns to preserve unless they encode a real constraint.
- If you have fixed the same bug more than twice in different ways, stop patching: restate the intended behavior and derive the fix from that.

## SOLID as a practical checklist (non-trivial modules)

- S: one clear reason to change; split orchestration, parsing, persistence, and domain rules when tangled.
- O: add behavior through a focused function, variant, adapter, or module — never pre-build extension points for imaginary futures.
- L: every variant honors the advertised contract without special-case surprises.
- I: interfaces narrow enough that callers depend only on what they use.
- D: policy depends on stable abstractions, plain data, or ports — not low-level I/O clients.

SOLID is not an excuse for hierarchies, factories, or ceremony. Prefer the simplest structure that preserves the intent.

## Testing

1. Decide what correct behavior looks like before implementing; prefer a red-green loop when practical.
2. Prefer integration, macro-behavior, contract, or acceptance tests over unit tests that mirror implementation details.
3. Spend test effort on parsers, state machines, business rules, transformations, seams, and failure modes — not trivial getters.
4. One behavior visible per test; loops and branching in a test hide harness mistakes. Keep failing output obvious.
5. Never move goalposts to make a broken implementation look correct; tests expose intended behavior, they are not a reward function to game.
6. Before finishing, keep only tests that guard stable behavior or prevent a real regression; delete the rest.
