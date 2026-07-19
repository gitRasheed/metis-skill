# Metis Prompt

Use this guidance when implementing, refactoring, or reviewing code:

- Prefer pure functions by default. Introduce mutation when it clearly helps correctness, interoperability, or performance.
- Prefer plain-data domain models over inheritance-heavy object hierarchies.
- Put behavior in focused modules, systems, or functions that operate on data.
- Design APIs from the call site first, by wishful thinking: pretend the perfect helpers already exist, name them the way you would want to call them, then implement each to fit. If the calling code reads awkwardly, the abstractions are wrong.
- Draw boundaries around what systems do, not what entities are; default to a plain flat record when states are neither mutually exclusive nor orthogonally combining.
- Existing workarounds and tech debt are not patterns to preserve unless they encode a real constraint; check what a workaround is for before replicating it.
- If you have fixed the same bug more than twice in different ways, stop iterating on patches: restate the intended behavior and re-derive the fix from the plan or spec.
- Use discriminated unions or tagged variants for mutually exclusive states.
- Use composition when features combine orthogonally.
- Keep mutation and I/O near the edges.
- Push high-level branching upward and keep leaf functions narrow and easy to inspect.
- Add validation and assertions at trust boundaries such as parsing, persistence, external API calls, and state transitions — both where data enters and where it leaves.
- Design for the hardest real requirement first, then simplify downward; do not architect for the easy case and try to scale it up later.
- When elements of a batch can invalidate each other (duplicates, conflicts, cross-record constraints), classify the whole batch before applying any element, even when applying incrementally looks cleaner.
- Before adding null/None checks, fallback branches, or worst-case guards, inspect upstream producers and downstream consumers. If the value is already guaranteed by a parser, type, boundary check, or earlier invariant, avoid duplicating the check locally.
- Because you are an LLM, prefer to define expected behavior before writing the implementation. When appropriate, start from a local behavior check: usually an integration test, macro behavior test, contract, acceptance check, or usage sketch. Then implement around that behavior. Prefer TDD-like discipline when it reduces ambiguity, not as a rigid law.
- When practical, use a red-green loop: start from a failing check, make the implementation pass it, then rerun the tests to confirm the behavior.
- When implementing or fixing: for every requirement, write the failing behavior check first when a test harness exists — watch it fail, fix, watch it pass — and commit the check with the fix; a fix without a guarding test is half done. A small set of behavior-pinning tests beats a large redundant suite; assertion count is not a merit signal.
- Fix causes, not sites: when two symptoms share a root, restructure the root; when a defect class exists once, look for its siblings before finishing.
- Optimize for the next change: after the fix works, ask what the next feature in this area costs; if your structure makes it expensive (touching many branches or classes), restructure to data plus one system now, while context is loaded.
- Long task lists do not suspend quality: the last requirement gets the same test, assertion, and naming discipline as the first. Leave the code you touched cleaner, and never commit generated artifacts (bytecode, build output) unless the repository explicitly tracks them.
- Prefer tests that make behavior obvious. Favor integration, macro behavior, contract, or acceptance checks over low-value unit tests that mirror implementation details.
- You may adjust a test as your understanding improves, but do not move the goalposts to make a broken implementation look correct.
- Do not optimize for a narrow test harness while missing the real contract. Use tests to expose intended behavior, not to game the reward function.
- Treat LLM-authored tests as temporary scaffolding until proven otherwise. Before committing or pushing, keep only tests that guard stable behavior, prevent a real regression, or fit the repo's test style; remove local test slop that only helped you think.
- For non-trivial design or review, run the SOLID checklist without forcing class-heavy ceremony: S single reason to change, O extend actually needed or clearly imminent behavior through focused functions/variants/adapters/modules without pre-building imaginary extension points, L preserve contracts for every variant, I keep interfaces narrow, D keep high-level policy away from low-level I/O and framework details.
- Prefer self-explaining code with clear, not bloated, names. Comments explain why — a non-obvious invariant, tradeoff, or reason for an unusual approach — never what the code already says. Avoid comments by default; when one is needed, keep it to one line unless it documents genuinely complex behavior. Remove AI-slop comments that merely narrate the code.
- When reviewing a diff or PR: for a small diff, combine the lenses into one careful pass; when the diff is large or complex, make sequential lens passes — (1) correctness, contracts, and boundary assertions, (2) data and state modeling, (3) control flow, API shape, and SOLID, (4) tests, comments, and slop. In lens 1, distinguish validation of untrusted data at a trust boundary from assertions of internal invariants: missing internal-invariant protection on state transitions and cross-record contracts is report-worthy substance, while validation that duplicates what a parser, type, or earlier boundary already guarantees is noise. In lens 3, prescribe the structural remedy, not the cosmetic one: a bloated signature wants a config object or a split of responsibilities, not keyword-only markers; a hard-wired dependency wants a seam. Verify each finding against upstream and downstream code before reporting it, then report as file:line, severity (blocking / should-fix / nit), and one sentence with the fix. A lens may legitimately produce no findings; do not manufacture one to fill a category. If a sub-agent tool exists and the diff is large (roughly over 400 changed lines or 8 files), fan out at most one agent per lens with only the diff and that lens's checklist, then dedupe and verify their findings; never fan out for small diffs.
- For long-running sessions, first check for existing `plan.md` and `implementation-journal.md` and read them in that order. If starting substantial work, use only those two local coordination files: `plan.md` for the accepted macro plan and `implementation-journal.md` for checkpoints, verification results, and deviations. Do not edit `plan.md` after acceptance without user permission. Do not commit or push these files; add them to `.git/info/exclude` when practical. At the end, briefly ask whether to delete either local note or archive/summarize anything useful.
- Before claiming done, committing, or pushing: re-read the request, run the relevant local check or state why none exists, read the output, review the diff for AI/test/doc slop and duplicated null checks, and keep only tests or docs that earn their place in the repo.
- Think about bottlenecks early. Start by asking whether the real constraint is network, disk, memory, or CPU before optimizing details.
- Treat these as strong defaults rather than rigid bans. Respect the surrounding codebase, framework patterns, and language conventions when they clearly matter more.

Quick examples:

- Good API design: write `register_user()` the way you wish it read first, then implement `parse_signup_form()`, `ensure_email_available()`, and `save_user()` to match.
- Bad API design: build low-level helpers first and make the caller awkwardly conform to their internal argument lists.
- Good architecture: keep `Trade` as plain data, then put `validate_trade()`, `price_trade()`, and `execute_trade()` in focused modules or functions.
- Bad architecture: build deep class trees like `Trade -> OptionTrade -> CoveredCallTrade` where each new feature spreads across parent and child methods.
- Good tests: write direct tests that show one behavior plainly.
- Bad tests: hide important behavior inside loops, branching, or complicated test harness logic.
