# Metis


Write code that favors plain data, pure logic, clear call sites, and early architectural thinking. These are strong defaults, not rigid laws: follow the surrounding codebase, framework constraints, and language norms when they clearly matter more.

Before anything else, name the phase you are in, then read ONLY the sections listed for that phase plus the Design principles. Actively skip the other sections — holding out-of-phase rules in mind dilutes the in-phase ones.

- Designing or starting a task: Design principles, LLM agent process
- Implementing or fixing code: Working rules, Implementation rules, plus the SOLID checklist for non-trivial modules
- Writing tests: Testing checklist
- Reviewing a diff or PR: Code review mode ONLY — skip Implementation rules and Working rules entirely; your whole attention belongs to the lenses
- Before claiming done, committing, or pushing: Final verification checklist

## Design principles

1. Start from the call site, by wishful thinking: pretend the perfect helpers already exist, name them the way you would want to call them, and get the top-level usage reading cleanly. If the calling code reads awkwardly, the abstractions are wrong — and you find out before building anything.
2. Prefer plain data plus focused functions, modules, or systems over behavior-heavy objects. Draw boundaries around what systems do, not what entities are.
3. Choose the simplest state model that matches reality: discriminated unions for mutually exclusive states, composable data for orthogonal features, and a plain flat record when neither pressure exists — do not over-architect the simple case.
4. Isolate mutation and I/O near the edges. Orchestration decides what happens; inner helpers do narrow, understandable work.
5. Push ifs up, fors down. Keep high-level control flow in parents and leaf functions low-branch and easy to test.
6. Assert at boundaries, both where data enters and where it leaves: parsing, persistence, external APIs, state transitions, and function contracts. Check what must be true and, when useful, what must not be.
7. Prefer explicit, behavior-focused tests without indirection that hides intent.
8. Sanity-check the likely bottleneck first — network, disk, memory, then CPU. Prefer architecture changes over late micro-optimizations.
9. Design for the hardest real requirement first, then simplify downward. Do not architect for the easy case and try to scale it up later.
10. When elements of a batch can invalidate each other — duplicates, conflicts, cross-record constraints — classify the whole batch before applying any element, even when applying incrementally looks cleaner.

## LLM agent process

1. Define expected behavior before locking in the implementation. When appropriate, write a local behavior check first — an integration test, macro behavior test, contract, acceptance check, or top-level usage sketch — and let the implementation conform to it. Do not treat tests as post hoc justification. Do not force strict TDD while the design is still moving, but prefer behavior-first when it reduces ambiguity.
2. Trace invariants before adding defensive checks. Before adding null/None checks, fallback branches, or worst-case guards, inspect upstream producers and downstream consumers. If a parser, type, or earlier boundary already guarantees the value, do not duplicate the check. Add one when data crosses a trust boundary, the invariant can drift, or the contract should be explicit.
3. Distinguish essential from accidental complexity. Existing workarounds, hacks, and tech debt in the codebase are not patterns to preserve unless they encode a real constraint — check what a workaround is for before replicating it in new code.
4. Detect thrash and re-derive. If you have fixed the same bug more than twice in different ways, stop iterating on patches: restate the intended behavior, re-read the plan or spec, and derive the fix from that understanding instead.

## Implementation rules (ONLY when implementing or fixing — skip entirely when reviewing)

1. For every requirement, write the failing behavior check first when a test harness exists; watch it fail, fix, watch it pass. Commit the check with the fix — a fix without a guarding test is half done. A small set of behavior-pinning tests beats a large redundant suite; assertion count is not a merit signal.
2. Fix causes, not sites: when two symptoms share a root, restructure the root; when a defect class exists once, look for its siblings before finishing — the same stale check or missing boundary usually appears more than once.
3. Optimize for the next change: after the fix works, ask what the next feature in this area costs; if your structure makes it expensive (touching many branches or classes), restructure to data plus one system now, while context is loaded.
4. Long task lists do not suspend quality: the last requirement gets the same test, assertion, and naming discipline as the first. Do not drop the quality pass because the functional list is long.
5. Leave the campsite cleaner: delete dead code and scaffolding you find mid-task if it is inside the code you already changed, and never commit generated artifacts (bytecode, build output) with your change.

## Working rules

- Prefer pure functions; introduce mutation when it clearly improves correctness, interoperability, or performance.
- Prefer data transformations over deep object hierarchies.
- Prefer small, explicit abstractions that read well at the call site, and code that is easy to verify by reading.
- Keep invariants close to the operation that depends on them; do not validate early and rely on it much later if the data can drift.
- Hide awkward external APIs behind an adapter so the rest of the code speaks the interface you wish existed.
- Prefer boundary validation over trusting implicit assumptions.
- Prefer self-explaining code with clear, not bloated, names. Comments explain why — a non-obvious invariant, tradeoff, or reason for an unusual approach — never what the code already says. Default to no comment; when one is needed, keep it to one line unless it documents genuinely complex behavior.
- Remove AI-slop comments and style inconsistent with the surrounding file.
- Adapt to the existing team style instead of forcing this skill mechanically into every file.

## SOLID checklist

For non-trivial design, implementation, refactoring, or review, run SOLID as a practical checklist; skip or adapt items when the framework, language, or repo makes them inappropriate.

- S: one clear reason to change; split orchestration, parsing, persistence, and domain rules when tangled.
- O: add actually-needed or clearly imminent behavior through a focused function, variant, adapter, or module rather than fragile edits across many branches; never pre-build extension points for imaginary futures.
- L: every implementation or variant honors the advertised contract without special-case surprises.
- I: interfaces narrow enough that callers depend only on operations they actually use.
- D: high-level policy depends on stable abstractions, plain data, or ports — not low-level I/O clients and framework details.

SOLID is not an excuse for class hierarchies, factories, or ceremony. Prefer the simplest structure that preserves the intent.

## Pattern cues

Full do/don't code for each cue lives in `references/examples.md`; read it when the task is complex or ambiguous.

- Call-site-first: write `register_user()` the way you wish it read, then implement `parse_signup_form()`, `ensure_email_available()`, and `save_user()` to match — never shape the caller around helper internals.
- Plain data plus systems: `Trade` as a dataclass with `validate_trade()` / `price_trade()` / `execute_trade()`, not a `Trade -> OptionTrade -> CoveredCallTrade` hierarchy where one feature touches many classes.
- Unions for exclusive states: `PaymentMethod = CardPayment | CashPayment | BankTransfer`, not inheritance plus isinstance chains.
- Ifs up, fors down: the parent partitions and decides; leaves loop over homogeneous work.
- Boundary assertions: at parsing, I/O, API, and state-transition edges, assert positive and negative space (`amount > 0`, `flags & RESERVED_MASK == 0`).
- Explicit tests: one behavior visible per test; loops and branching in a test hide mistakes in the harness instead of the implementation.

Two contrasts worth keeping in front of you. Ifs up, fors down — good:

```python
def process_items(items):
    credits = [i for i in items if i.kind == "credit"]
    debits = [i for i in items if i.kind == "debit"]
    if credits:
        apply_credits(credits)
    if debits:
        apply_debits(debits)
```

Bad: one loop where every iteration re-decides `if item.kind == "credit": ... elif item.kind == "debit": ...`, so no leaf is testable alone.

Explicit tests — good:

```python
def test_parse_signup_form_rejects_missing_email():
    result = parse_signup_form({"password": "secret"})
    assert result == {"error": "email_required"}
```

Bad:

```python
for case in cases:
    result = parse_signup_form(case.input)
    if case.want_error and result.ok:
        raise AssertionError("expected error")
```

## Testing checklist

Tests are code: minimize test logic to minimize test bugs.

1. Spend test effort on parsers, state machines, business rules, transformations, integration seams, and failure modes — not trivial getters or one-line passthroughs.
2. Decide what correct behavior looks like before implementing. When practical, run a red-green loop: watch a check fail, make it pass, rerun to confirm.
3. Prefer integration, macro-behavior, contract, or acceptance tests over unit tests that mirror implementation details.
4. Adjust tests as understanding improves, but never move the goalposts to make a broken implementation look correct; if expected behavior changes, state why.
5. Because you are an LLM, do not optimize for a narrow harness while missing the real contract. Tests expose intended behavior; they are not a reward function to game.
6. Use table-driven or parameterized tests only when they genuinely improve coverage or maintainability.
7. Keep failing output obvious: a reader should see what behavior broke without reconstructing test control flow.
8. Treat LLM-authored tests as temporary scaffolding. Before committing, keep only tests that guard stable behavior, prevent a real regression, or fit the repo's test style; delete the rest.

## Code review mode

Use this when reviewing a diff, PR, or another agent's work. Do not check every rule in one read; make several passes over the diff, each asking one question. Load `references/review-examples.md` first (compact review-time contrasts for replay safety, comment discipline, and cleanup); load other references only when a lens needs more depth.

Lens passes, in order:

1. Correctness and contracts — does the change do what it claims; do all variants honor the advertised contract (L); are assertions present where data crosses trust boundaries; are failure paths handled.
2. Data and state — plain data vs behavior-heavy objects; unions for mutually exclusive states; mutation and I/O isolated at the edges.
3. Control flow and API shape — call sites read cleanly; ifs up, fors down; S/O/I/D; no speculative abstractions. For every surface defect you find here, prescribe the STRUCTURAL remedy, not the cosmetic one: a bloated signature wants a config object or a split of responsibilities, not keyword-only markers; a method doing parsing+shaping+transport+error policy wants those responsibilities separated (pure builders, an I/O port the caller can fake, policy left with the caller); a hard-wired dependency wants a seam because forty callers must test against it. Name the new shape concretely.
4. Tests and slop — the Testing checklist, plus: comments that narrate code or restate the obvious, multi-line comments that should be one line, duplicated defensive checks, dead scaffolding, casts that dodge type errors, doc spam.

Findings:

- Verify before reporting. Trace the invariant upstream and downstream first; do not flag "missing validation" that a parser, type, or earlier boundary already guarantees.
- Report each finding as `file:line`, severity (blocking / should-fix / nit), and one sentence stating the problem and the fix. No essays.
- Tag each finding with the lens or rule that produced it, e.g. `[lens 2: data and state]` or `[rule: trace invariants]`. Producing the tag forces a systematic sweep of every lens; drop tags only when the surrounding tooling requires a fixed format.
- Quality findings from lenses 2–4 are not padding. Report state-modeling, hierarchy, control-flow, test-logic, and comment defects at should-fix or nit severity even when blocking correctness findings dominate the review.

Sub-agent fan-out, only when both hold: a sub-agent or task tool exists in your environment, and the diff is large (roughly more than 400 changed lines or 8 files):

- Spawn at most one agent per lens, max 4. Give each only the diff, its lens's checklist above, and the matching reference file from the list at the end of this skill — not the whole skill.
- Each agent returns findings in the format above.
- Dedupe overlapping findings, then verify each against the actual code before reporting; isolated reviewers produce false positives.
- Never fan out for small diffs. Sequential lens passes are cheaper and more accurate there.

## Final verification checklist

Before claiming done, committing, or pushing:

1. Re-read the user request and confirm the implementation matches the intended behavior.
2. Run the relevant checks, tests, build, or validation gate. If no useful check exists, say so explicitly.
3. Read the command output before claiming success; a started command is not a passed command.
4. Review the diff for AI slop: narrating comments, duplicated null checks, abnormal defensive code, speculative abstractions, broad rewrites, casts that dodge type errors, test or doc spam.
5. Keep only tests and docs that earn their place; remove scaffolding that only helped you think.

## Long-running sessions

For substantial multi-step work, use at most two local coordination files:

1. When continuing a task, check for `plan.md` and `implementation-journal.md`; read `plan.md` first.
2. If no plan exists and the task is substantial, create `plan.md` with the macro plan, intended behavior, important boundaries, validation gates, and testing approach.
3. After the user accepts `plan.md`, do not edit it without permission.
4. Record checkpoints, verification results, surprises, and deviations (with reasons) in `implementation-journal.md`.
5. At the end of a completed task, briefly ask whether to delete either note or archive anything useful.
6. Do not commit or push these notes; prefer `.git/info/exclude` over the repo's `.gitignore`.

## When to relax the defaults

Relax when the codebase has a strong local convention that would be expensive to fight, the framework prefers a different structure, an object-oriented interface is the natural integration boundary, mutation is the clearest correct option, or generated code, DSLs, or third-party APIs impose a different shape. Preserve the spirit: clear responsibilities, understandable state transitions, readable tests, intentional API shape.

## Read these references when needed

- For plain-data architecture, unions, and system boundaries (review lens 2): `references/architecture.md`
- For call-site-first API design (review lens 3): `references/api-design.md`
- For testing style and tradeoffs (review lens 4): `references/testing.md`
- For boundary assertions and performance framing (review lens 1): `references/performance-and-safety.md`
- For optional concrete do/don't examples when the task is complex or ambiguous: `references/examples.md`
- For compact review-time contrasts when reviewing a diff or PR: `references/review-examples.md`
