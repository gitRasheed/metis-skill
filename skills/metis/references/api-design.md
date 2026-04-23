# API Design

Design APIs from the caller outward.

## Call-site-first method

Before implementing helpers, write the code that should use them.

Ask:

- does the top-level flow read cleanly
- are helper names obvious
- do argument lists feel natural
- are responsibilities separated cleanly

If the calling code feels awkward, fix the abstraction before writing the implementation.

## What this tends to improve

- helper boundaries become clearer
- signatures become easier to understand
- orchestration code becomes better documentation
- internal implementation can change without forcing call-site churn

## Practical pattern

1. Write the top-level path first.
2. Name the helpers the way you wish they already existed.
3. Keep each helper responsible for one coherent step.
4. Implement helpers to satisfy that contract.
5. Revisit the call site after the first draft and simplify again.

## When to bend this

If you are wrapping a rigid external API, you may not control the natural interface. In that case:

- create an adapter layer
- keep ugly external details near the boundary
- keep the rest of your code speaking in the interface you wish you had
