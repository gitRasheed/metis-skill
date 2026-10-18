# Performance And Safety

Think about correctness and cost early, not only after the code exists.

## Performance order of operations

Before coding, ask which resource is likely to dominate:

1. network
2. disk
3. memory
4. CPU

This is only a heuristic, but it is a useful default. Large gains usually come from changing architecture, batching, or data movement rather than polishing hot loops too early.

## Serial chains are architecture

The runtime of a program is set by its longest chain of dependent steps, and a chain of request, wait, request across I/O cannot be shortened later without a rewrite. Gather what an operation needs first.

Avoid:

```python
for order_id in order_ids:
    order = api.get_order(order_id)
    customer = api.get_customer(order.customer_id)
```

Prefer:

```python
orders = api.get_orders(order_ids)
customers = api.get_customers({order.customer_id for order in orders})
```

## Estimate the cost before optimizing

For performance work, first estimate what the work should cost on the hardware: bytes moved, round trips, operations per item. Then measure the gap between that and what you get, and explain the gap before tuning. A profile alone finds a local minimum. Large programs often have flat profiles, so do not assume one hot loop holds most of the time.

## Boundary parsing and internal invariants

Where untrusted data enters (request parsing, file and database I/O, API inputs, deserialization), parse it once into a typed shape and report bad input through normal error handling. A malformed request is an expected outcome, not a programming error.

Assertions belong past that boundary, on invariants whose failure means a bug and would corrupt state or stored data:

- state transitions that should be impossible
- cross-record contracts between values that are valid alone
- postconditions an earlier internal step already promised

Example:

```python
def parse_transfer(raw: dict) -> Transfer:
    ...  # missing fields, bad amounts -> InputError, not a crash

def apply_reserved_transfer(
    src: Account, dst: Account, transfer: ReservedTransfer
) -> tuple[Account, Account]:
    assert (transfer.source_id, transfer.dest_id) == (src.id, dst.id)
    assert src.id != dst.id
    assert src.balance >= transfer.amount
    assert transfer.flags & RESERVED_FLAGS_MASK == 0
    new_src = replace(src, balance=src.balance - transfer.amount)
    new_dst = replace(dst, balance=dst.balance + transfer.amount)
    return new_src, new_dst
```

The parser owns rejecting bad input. The assertions guard promises the reservation step made, including that a transfer never targets its own source, which would otherwise create or destroy money silently. Re-validating `amount > 0` here would be noise, because `parse_transfer` already guarantees it. Assert both positive space and negative space: not only what must be true, but reserved or impossible cases when they matter.

## Re-check only what can change

A parsed type removes re-checks for what it encodes. Data that can change after parsing, such as mutable state or rows another process writes, gets checked at the point of use, not once early and trusted much later. When another process can write it, the check and the action must be one atomic operation (next section).

Avoid:

```python
is_valid = validate(data)
# many lines later
if is_valid:
    write_to_db(data)
```

## Check-then-act is one operation

A check and the action that depends on it race when another request can run between them. Make the pair atomic with a unique constraint, a conditional write, or a transaction.

Avoid:

```python
if not users.email_taken(new_user.email):
    users.insert(new_user)
```

Prefer:

```python
try:
    users.insert(new_user)
except UniqueViolation:
    return EmailTaken(new_user.email)
return Registered(new_user.id)
```

The unique index on email is the check, so two concurrent signups cannot both pass it.
