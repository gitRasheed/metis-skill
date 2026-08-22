# Performance And Safety

Think about correctness and cost early, not only after the code exists.

## Performance order of operations

Before coding, ask which resource is likely to dominate:

1. network
2. disk
3. memory
4. CPU

This is only a heuristic, but it is a useful default. Large gains usually come from changing architecture, batching, or data movement rather than polishing hot loops too early.

## Boundary parsing and internal invariants

Where untrusted data enters — request parsing, file and database I/O, API inputs, deserialization — parse it once into a typed shape and report bad input through normal error handling. A malformed request is an expected outcome, not a programming error.

Assertions belong past that boundary, on invariants whose failure means a bug:

- state transitions that should be impossible
- cross-record contracts between values that are valid alone
- postconditions an earlier internal step already promised

Example:

```python
def parse_transfer(raw: dict) -> Transfer:
    ...  # missing fields, bad amounts -> InputError, not a crash

def apply_reserved_transfer(account: Account, transfer: ReservedTransfer) -> None:
    assert transfer.account_id == account.id
    assert account.balance >= transfer.amount
    assert transfer.flags & RESERVED_FLAGS_MASK == 0
```

The parser owns rejecting bad input; the assertions guard promises the reservation step already made. Assert both positive space and negative space: not only what must be true, but reserved or impossible cases when they matter.

## Keep invariants close to use

Avoid checking something in one place and relying on it much later if the data can drift in between. Prefer to validate near the operation that depends on the invariant.

Bad pattern:

```python
is_valid = validate(data)
# many lines later
if is_valid:
    write_to_db(data)
```

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
