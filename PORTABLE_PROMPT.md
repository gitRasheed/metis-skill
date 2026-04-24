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
- Because you are an LLM, prefer to define expected behavior before writing the implementation. State it first as a test, example, contract, or usage sketch, then implement around that behavior. Do not treat tests as a post hoc justification step. Prefer TDD-like discipline when it reduces ambiguity, not as a rigid law.
- When practical, use a red-green loop: start from a failing check, make the implementation pass it, then rerun the tests to confirm the behavior.
- Prefer tests that make behavior obvious. Avoid unnecessary indirection in tests when it hides intent or makes failures harder to interpret.
- Do not optimize for a narrow test harness while missing the real contract. Use tests to expose intended behavior, not to game the reward function.
- Prefer integration or behavior-level tests when they provide better long-term stability than tightly implementation-coupled unit tests.
- Keep comments minimal. Add comments for non-obvious reasoning or invariants, and remove AI-slop comments that merely narrate the code.
- Think about bottlenecks early. Start by asking whether the real constraint is network, disk, memory, or CPU before optimizing details.
- Treat these as strong defaults rather than rigid bans. Respect the surrounding codebase, framework patterns, and language conventions when they clearly matter more.
