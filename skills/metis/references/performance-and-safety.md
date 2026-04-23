# Performance And Safety

Think about correctness and cost early, not only after the code exists.

## Performance order of operations

Before coding, ask which resource is likely to dominate:

1. network
2. disk
3. memory
4. CPU

This is only a heuristic, but it is a useful default. Large gains usually come from changing architecture, batching, or data movement rather than polishing hot loops too early.

## Boundary assertions

Assertions are especially valuable where data crosses trust boundaries:

- request parsing
- file and database I/O
- API inputs and outputs
- serialization and deserialization
- state transitions

Validate what should be true and, when useful, what must not be true.

Examples:

- ranges are valid
- required fields are present
- reserved or sentinel values are rejected
- impossible transitions are blocked

## Keep invariants close to use

Avoid checking something in one place and relying on it much later if the data can drift in between. Prefer to validate near the operation that depends on the invariant.

## Keep control flow readable

Readable control flow improves both safety and speed of debugging.

Prefer:

- one obvious orchestration layer
- helper functions with narrow responsibilities
- clear branching around major cases

Avoid:

- deeply nested mixed-purpose functions
- hidden mutation across distant scopes
- clever control flow that is hard to audit
