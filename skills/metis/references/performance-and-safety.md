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

Example:

```python
def process_transfer(amount: int, flags: int) -> None:
    assert amount > 0
    assert amount <= ACCOUNT_BALANCE_MAX
    assert flags & RESERVED_FLAGS_MASK == 0
    assert amount != SENTINEL_AMOUNT
```

This checks both positive space and negative space. Do not only assert what you want to allow; also assert obviously bad or reserved cases when they matter.

## Keep invariants close to use

Avoid checking something in one place and relying on it much later if the data can drift in between. Prefer to validate near the operation that depends on the invariant.

Bad pattern:

```python
is_valid = validate(data)
# many lines later
if is_valid:
    write_to_db(data)
```

Prefer keeping the check close to the action that depends on it.

## Keep control flow readable

Readable control flow improves both safety and speed of debugging.

Prefer:

- one obvious orchestration layer
- helper functions with narrow responsibilities
- clear branching around major cases

Example:

```python
def process_items(items):
    credits = [item for item in items if item.kind == "credit"]
    debits = [item for item in items if item.kind == "debit"]

    if credits:
        apply_credits(credits)
    if debits:
        apply_debits(debits)
```

Avoid burying many unrelated cases inside one mixed-purpose loop when the parent can separate the major decisions first.

Avoid:

- deeply nested mixed-purpose functions
- hidden mutation across distant scopes
- clever control flow that is hard to audit
