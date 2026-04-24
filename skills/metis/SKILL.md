---
name: metis
description: Prefer pure functions, plain-data domain models, system-oriented decomposition, top-down API design, boundary assertions, simple control flow, and explicit behavior-focused tests. Use when an LLM is implementing a feature, fixing a non-trivial bug, planning or executing a refactor, shaping module or API boundaries, or reviewing code and pull requests where cleaner architecture and behavior-first testing matter. Especially useful for complex coding or agentic tasks where the model might otherwise overcomplicate the design, mix concerns, or treat tests as post hoc justification. Less relevant for trivial one-line edits, pure prose tasks, or rote mechanical changes.
---

# Metis

Write code in a style that favors plain data, pure logic, clear call sites, and early architectural thinking.

Treat these as strong defaults, not rigid laws. Follow the surrounding codebase, framework constraints, and language norms when they clearly matter more than stylistic purity.

## Core workflow

1. Start from the call site.
   Sketch the top-level usage first. Let the desired calling code shape helper names, signatures, and boundaries.

2. Prefer plain data plus systems or modules over behavior-heavy objects.
   Keep domain entities mostly as data when possible. Put behavior in focused functions, modules, or systems that operate on that data.

3. Choose the simplest state model that matches reality.
   Use discriminated unions or tagged variants for mutually exclusive states. Use composable data or components when features combine orthogonally.

4. Isolate mutation and I/O.
   Keep side effects near the edges. Let orchestration code decide what happens and let inner helpers do narrow, understandable work.

5. Push ifs up, fors down.
   Keep high-level control flow in parent functions. Keep leaf functions focused, low-branch, and easy to test in isolation.

6. Assert at boundaries.
   Validate assumptions where data enters and leaves important boundaries such as parsing, persistence, external APIs, state transitions, and function contracts.

7. Prefer explicit, behavior-focused tests.
   When practical, write tests that are direct and easy to inspect. Avoid unnecessary indirection, loops, or branching in tests when that would hide intent or make failures harder to read.

8. Define expected behavior before locking in the implementation.
   Because you are an LLM, prefer to state the intended behavior before writing the implementation. Write that behavior first as a test, example, contract, acceptance check, or top-level usage sketch, then let the implementation conform to it. Do not treat tests as a post hoc justification step after the code already exists. Do not force strict TDD when the design is still moving quickly, but do prefer behavior-first development when it reduces ambiguity.

9. Think about performance before implementation details.
   Sanity-check the likely bottleneck first: network, disk, memory, then CPU. Prefer architecture changes over late micro-optimizations.

## Working rules

- Prefer pure functions by default. Introduce mutation when it clearly improves correctness, interoperability, or performance.
- Prefer data transformations over deep object hierarchies.
- Prefer small, explicit abstractions that read well at the call site.
- Prefer boundary validation over trusting implicit assumptions.
- Prefer code that is easy to verify by reading.
- Keep comments sparse. Add them for non-obvious reasoning, invariants, or structure, not to narrate obvious code line by line.
- Remove AI-slop comments and other style that feels inconsistent with the surrounding file.
- Prefer adapting to an existing team style over forcing this skill mechanically into every file.

## Testing guidance

- Focus test effort on parsers, state machines, business rules, transformations, integration seams, and failure modes.
- Prefer behavior-first development. For non-trivial work, decide what correct behavior looks like before writing the implementation.
- When practical, use a red-green loop: write or identify a check that should fail first, make the implementation pass it, then rerun the tests to confirm the behavior.
- Because you are an LLM, do not optimize for a narrow test harness while missing the real contract. Use tests to expose the intended behavior, not to game the reward function.
- Do not spend time proving trivial getters or one-line passthrough helpers unless they are unusually risky.
- Bias toward integration or behavior-level tests when they give more stable coverage than implementation-coupled unit tests.
- Use table-driven or parameterized tests when they genuinely improve coverage or maintainability, but avoid them when they mainly add indirection.
- Keep failing test output obvious. A reader should be able to tell what behavior broke without reconstructing test control flow.

## When to relax the defaults

Relax these preferences when:

- the codebase already uses a strong local convention that would be expensive to fight
- the framework strongly prefers a different structure
- an object-oriented interface is the natural integration boundary
- mutation is the clearest or fastest correct option
- generated code, DSLs, or third-party APIs impose a different shape

In those cases, preserve the spirit of the skill:

- keep responsibilities clear
- keep state transitions understandable
- keep tests readable
- keep the API shape intentional

## Read these references when needed

- For plain-data architecture, unions, and system boundaries: `references/architecture.md`
- For call-site-first API design: `references/api-design.md`
- For testing style and tradeoffs: `references/testing.md`
- For boundary assertions and performance framing: `references/performance-and-safety.md`
