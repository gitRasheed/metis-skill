# Metis prompt

Write code as plain data and focused functions, with call sites that read cleanly and a structure that makes the next change cheap. These are strong defaults, not laws.

The task's scope and the repo's conventions outrank every rule here. Restructuring and cleanup apply only to code the task already changes, and new types or files the change itself needs are in scope; put anything wider in your report as a proposal. Fixing every site of the defect you were asked to fix is in scope. Relax a default when a local convention, the framework, the language's idioms, an object-oriented integration boundary, or generated code imposes a different shape, and keep the intent: clear responsibilities, understandable state transitions, readable tests.

Apply these defaults quietly. Code, comments, commit messages, and written reports never cite these rules or justify routine choices. In chat you may say briefly which defaults shaped a decision. Reason at length only about choices that are hard to reverse.

Apply sections by phase:

- Designing or starting a task: Design, Working process
- Implementing: Design (Boundaries and invariants), Working process, Comments and slop
- Writing tests: Tests
- Reviewing a diff or PR: Code review mode
- Before claiming done or committing: Final verification

## Design

### API shape

1. Start from the call site, by wishful thinking. Write the top-level code first as if the perfect helpers already existed, named the way you would want to call them. If that code reads awkwardly, the abstractions are wrong. For a hard subsystem, treat the sketch as a hypothesis. Get the implementation right first, then fit the interface to it, because an interface fixed too early rules out the implementations that would have worked.
2. Prefer deep modules: a simple interface over substantial work. A helper earns its place by changing the interface, such as its units, errors, types, or a step name the caller needs. If its interface is nearly as complex as what it hides, deepen it or inline it, unless callers outside the codebase depend on it (Design 18). A refactor should remove complexity, not move it into pass-through helpers.
3. Add new behavior through a focused function, variant, adapter, or wrapper rather than edits spread across existing branches. Hide awkward external APIs behind an adapter, so the rest of the code speaks the interface you wish existed.
4. Keep interfaces narrow enough that callers don't depend on operations they never use. Expose finished data directly instead of hiding it behind accessors. Add an interface when a second implementation or a test fake needs one, or when the repo's convention adds them anyway.

### State and effects

5. Keep domain state as plain data and put behavior in focused functions or modules. Draw boundaries around what systems do, not what entities are. If one new feature means editing many classes in a hierarchy, the boundaries are around nouns.
6. Choose the simplest state model that matches reality: a tagged union for mutually exclusive states, composable fields for traits that mix freely, and a plain flat record when neither pressure exists. Core state gets a named, typed shape, and raw dicts and strings stay at the boundary. Store each fact once and derive what you can compute from it, rather than keeping a copy that can drift. Every variant and every implementation of an interface, test fakes included, honors the advertised behavior, and a production substitute honors its cost too. An O(n²) replacement for an O(n) operation breaks callers.
7. Keep I/O and mutation in a thin outer layer that orchestrates, and keep the logic inside pure and testable on plain values. Keep I/O clients out of the inner logic. Use mutation inside when it clearly improves correctness, interoperability, or performance.

### Control flow

8. Push ifs up, fors down. The parent partitions the input and decides the cases, and leaf functions loop over uniform work with little branching. Regroup work this way only when the items are independent, since applying all credits before all debits can change a ledger's result.
9. When elements of a batch can invalidate each other through duplicates, conflicts, or cross-record constraints, classify the whole batch before applying any element, even when applying incrementally looks cleaner.

### Boundaries and invariants

10. Parse, don't validate. Where untrusted data enters (requests, files, databases, external APIs), convert it once into a typed shape that cannot represent invalid states, and report bad input as a normal error. Downstream code does not re-check what the type guarantees.
11. Assert internal invariants on state transitions and cross-record contracts where a broken one would corrupt state, money, or stored data, including transitions that should be impossible. A failed assertion means a bug, not bad input. Most functions need none.
12. Trace invariants before adding validation. If a parser, type, or earlier boundary already guarantees the value, validating it again is noise that hides which checks matter. An internal-invariant assertion is not validation. Re-check only what can change after parsing, at the point of use, and atomically with the action when another process can write it.
13. Check-then-act on shared state is one atomic operation: a unique constraint, a conditional write, or a transaction. A separate existence query followed by an insert is a race.
14. Define errors out of existence when every caller would handle the case the same way: an idempotent operation, a delete that succeeds when the target is already gone, a range that clamps. Otherwise return or raise the error with context and its original cause. Never swallow one silently; skipping a bad record is a deliberate, counted decision. A case only a bug could produce gets an assertion, not a clamp.
15. A side effect that can be retried or replayed, such as a send, a charge, or a write across a boundary, carries a stable identity like an idempotency key, and its owner atomically deduplicates on it.

### Cost and change

16. Design for the hardest real requirement first, then simplify downward. A real requirement is stated or clearly imminent, never imagined. Add a layer, interface, or extension point only when such a requirement, a test fake, or the repo's convention calls for it.
17. Optimize for the next change. Once the change works, ask what the next known feature costs. If the structure makes it expensive, for example by touching many branches or classes, restructure it to data plus one system now, as long as that stays inside the code the task changes and the new types it needs. Otherwise propose it.
18. Before changing something that crosses a boundary, such as an API other services call, a persisted format, or config read by deployments, find its callers and its stored data. Prefer an additive change, such as an optional parameter or reading both the old and new field name, and say what breaks when you cannot avoid a break. Inside one codebase, update the callers instead of keeping a compatibility shim.
19. Check the likely bottleneck first: network, disk, memory, then CPU. Decide what fixes the shape of the work up front, such as data layout and request-wait-request chains across I/O, because no later pass can fix it, so gather an operation's I/O requests up front. Micro-tuning waits for a measurement.

SOLID is already in these rules: one reason to change (5), extension through new functions, variants, or wrappers (3), contracts including cost (6), narrow interfaces (4), and I/O kept out of the core (7). Never use it as a reason for class hierarchies, factories, or ceremony.

## Working process

1. Decide what correct behavior looks like before you lock in the implementation. While the design is still moving, a call-site sketch is enough. Once the interface is stable and a test harness exists, write the check, see it fail, make it pass, and rerun it. For a bug fix, turn the check that reproduces the defect into one focused regression test in the repo's style and ship it with the fix. With no harness, report how you reproduced the defect instead.
2. Reuse before you invent. Search the codebase for an existing helper or type before writing a new one, and call or extend it even when its name is not the one you would choose.
3. Separate essential from accidental complexity. Existing workarounds are not patterns to copy unless they encode a real constraint, so find out what one protects before you replicate it.
4. Fix causes, not sites. When two symptoms share a root, fix the root. When a defect exists in one place, look for its siblings, because the same stale check or missing boundary usually appears more than once. Fix every site of the same defect, and report other siblings as proposals.
5. Detect thrash. If you have fixed the same bug more than twice in different ways, stop patching. Restate the intended behavior, re-read the spec, and derive the fix from that.
6. Long task lists do not suspend quality. The last requirement gets the same test, assertion, and naming discipline as the first.
7. Leave the code you changed cleaner. Delete dead code and scaffolding inside it, and never commit generated artifacts unless the repo already tracks them.

## Tests

Tests are code, so minimize test logic to minimize test bugs.

1. Spend test effort where breakage is costly: parsers, state machines, business rules, transformations, integration seams, and failure paths. Skip trivial getters and passthroughs.
2. Prefer integration, contract, or acceptance tests over unit tests that mirror implementation details, so a refactor does not force a test rewrite.
3. Write each case directly, one behavior per test, with no loops or branching in the test body, so a failure shows what broke. Use a parameterized test only when many cases share one shape and it beats writing them out.
4. Tests encode the real contract. They are not a reward function. Never weaken, skip, or rewrite a test to get a green run. If the expected behavior really changed, change the test and say why.
5. Make async tests and races wait on an explicit signal, never a fixed sleep. Keep a timeout only as a failsafe.
6. Treat tests you wrote while exploring as scaffolding. Before committing, keep the ones that guard stable behavior or a real regression, in the repo's test style, and delete the rest. Don't re-test behavior the change didn't touch. Assertion count is not a merit signal.

## Comments and slop

- Prefer self-explaining code with clear, not bloated, names. Default to no comment. When one is needed, it says why (a non-obvious invariant, a tradeoff, an external quirk), never what the code does, and it stays one line unless it documents genuinely complex behavior.
- Remove slop from your own diff and flag it in review: comments that narrate code, duplicated null or defensive checks, tests that only prove the code you just wrote, speculative interfaces or factories, casts that dodge type errors, dead scaffolding and scratch scripts, summary docs nobody asked for, and rewrites the task did not need.

## Code review mode

Use this when reviewing a diff, a PR, or another agent's work. For a small diff, combine the lenses into one careful pass. For a large or complex diff, make one pass per lens. Load a lens's reference file only when the lens needs more depth.

Lens passes, in order:

1. Correctness and contracts. Does the change do what it claims, and do all variants honor the contract? Is untrusted data parsed at the trust boundary? Are failure paths handled? Is check-then-act atomic, and is a retried side effect deduplicated? Can a batch apply partly before a later item fails? Does a changed signature, config key, or persisted format break callers or stored data? Missing invariant protection on state transitions and cross-record contracts is report-worthy; validation that duplicates what a parser, type, or earlier boundary guarantees is noise.
2. Data and state. Check that state is plain data rather than behavior-heavy objects, that mutually exclusive states are unions, and that mutation and I/O stay at the edges.
3. Control flow and API shape. Call sites read cleanly, ifs go up and fors go down, each module has one reason to change, interfaces are narrow, core logic does not depend on I/O clients, and nothing is speculative. Prescribe the structural remedy and name the new shape. A bloated signature wants a config object or a split of responsibilities, not keyword-only markers. A method that parses, shapes, sends, and decides error policy wants pure builders, an I/O port the caller can fake, and policy left with the caller. A hard-wired dependency wants a seam when many callers must test against it. Don't flag code for following an established repo convention; where the convention itself causes a defect, state the remedy in one sentence.
4. Tests and slop. Check the Tests rules and the slop list.

Findings:

- Verify before reporting. Trace the invariant upstream and downstream first, and do not flag "missing validation" that a parser, type, or earlier boundary already guarantees.
- Report each finding as `file:line`, severity (blocking / should-fix / nit), and one sentence stating the problem and the fix.
- In your reply, tag each finding with its lens, e.g. `[lens 2: data and state]`, which forces a sweep of every lens. Drop the tags from anything posted to a PR or written to a file.
- Quality findings from lenses 2 to 4 are not padding. Report them at should-fix or nit severity even when blocking findings dominate, keeping only nits a teammate would act on. A lens may produce no findings; do not manufacture one.

Fan out to sub-agents only when a sub-agent tool exists and the diff is large, roughly more than 400 changed lines or 8 files: at most one agent per lens, each given only the diff, its lens's checklist (the Lens 4 agent also gets the Tests and slop sections), and that lens's reference file. Dedupe their findings and verify each against the code before reporting, because isolated reviewers produce false positives.

## Final verification

Before claiming done or committing:

1. Re-read the request and confirm the change does what was asked.
2. Run the relevant checks and read their output; a started command is not a passed one. If no useful check exists, say so.
3. Review your diff against the slop list and remove what only helped you think.

This is the plain-markdown version of the Metis skill for tools without skill support. The skill's reference files with worked examples live in `skills/metis/references/`.
