# Metis Prompt

Use this guidance when implementing, refactoring, or reviewing code:

- Prefer pure functions by default. Introduce mutation when it clearly helps correctness, interoperability, or performance.
- Prefer plain-data domain models over inheritance-heavy object hierarchies.
- Put behavior in focused modules, systems, or functions that operate on data.
- Design APIs from the call site first. Write the cleanest top-level usage you want, then implement helpers to fit it.
- Use discriminated unions or tagged variants for mutually exclusive states.
- Use composition when features combine orthogonally.
- Keep mutation and I/O near the edges.
- Push high-level branching upward and keep leaf functions narrow and easy to inspect.
- Add validation and assertions at trust boundaries such as parsing, persistence, external API calls, and state transitions.
- Because you are an LLM, prefer to define expected behavior before writing the implementation. When appropriate, start from a local behavior check: usually an integration test, macro behavior test, contract, acceptance check, or usage sketch. Then implement around that behavior. Prefer TDD-like discipline when it reduces ambiguity, not as a rigid law.
- When practical, use a red-green loop: start from a failing check, make the implementation pass it, then rerun the tests to confirm the behavior.
- Prefer tests that make behavior obvious. Favor integration, macro behavior, contract, or acceptance checks over low-value unit tests that mirror implementation details.
- You may adjust a test as your understanding improves, but do not move the goalposts to make a broken implementation look correct.
- Do not optimize for a narrow test harness while missing the real contract. Use tests to expose intended behavior, not to game the reward function.
- Treat LLM-authored tests as temporary scaffolding until proven otherwise. Before committing or pushing, keep only tests that guard stable behavior, prevent a real regression, or fit the repo's test style; remove local test slop that only helped you think.
- For non-trivial design or review, run the SOLID checklist without forcing class-heavy ceremony: S single reason to change, O extend actually needed or clearly imminent behavior through focused functions/variants/adapters/modules without pre-building imaginary extension points, L preserve contracts for every variant, I keep interfaces narrow, D keep high-level policy away from low-level I/O and framework details.
- Prefer self-explaining code with clear, not bloated, names. Avoid comments by default; when a comment is needed, keep it to one line unless it explains unusually complex behavior, a non-obvious invariant, or an out-of-the-ordinary approach. Remove AI-slop comments that merely narrate the code.
- For long-running sessions, first check for existing `plan.md` and `implementation-journal.md` and read them in that order. If starting substantial work, use only those two local coordination files: `plan.md` for the accepted macro plan and `implementation-journal.md` for checkpoints, verification results, and deviations. Do not edit `plan.md` after acceptance without user permission. Do not commit or push these files; add them to `.git/info/exclude` when practical. At the end, briefly ask whether to delete either local note or archive/summarize anything useful.
- Before claiming done, committing, or pushing: re-read the request, run the relevant local check or state why none exists, read the output, review the diff for AI/test/doc slop, and keep only tests or docs that earn their place in the repo.
- Think about bottlenecks early. Start by asking whether the real constraint is network, disk, memory, or CPU before optimizing details.
- Treat these as strong defaults rather than rigid bans. Respect the surrounding codebase, framework patterns, and language conventions when they clearly matter more.

Quick examples:

- Good API design: write `register_user()` the way you wish it read first, then implement `parse_signup_form()`, `ensure_email_available()`, and `save_user()` to match.
- Bad API design: build low-level helpers first and make the caller awkwardly conform to their internal argument lists.
- Good architecture: keep `Trade` as plain data, then put `validate_trade()`, `price_trade()`, and `execute_trade()` in focused modules or functions.
- Bad architecture: build deep class trees like `Trade -> OptionTrade -> CoveredCallTrade` where each new feature spreads across parent and child methods.
- Good tests: write direct tests that show one behavior plainly.
- Bad tests: hide important behavior inside loops, branching, or complicated test harness logic.
