---
name: metis
description: Use when an LLM is doing non-trivial coding work, including implementing features, fixing bugs, refactoring, designing APIs or module boundaries, writing tests, reviewing code, or preparing to commit. Guides the model toward plain data, top-down API design, simple control flow, SOLID without ceremony, boundary assertions, behavior-first testing, final verification, and anti-slop cleanup. Not needed for trivial one-line edits, pure prose, or rote mechanical changes.
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
   Because you are an LLM, prefer to state the intended behavior before writing the implementation. When appropriate, write a local behavior check first: usually an integration test, macro behavior test, contract, acceptance check, or top-level usage sketch. Then let the implementation conform to that behavior. Do not treat tests as a post hoc justification step after the code already exists. Do not force strict TDD when the design is still moving quickly, but do prefer behavior-first development when it reduces ambiguity.

9. Think about performance before implementation details.
   Sanity-check the likely bottleneck first: network, disk, memory, then CPU. Prefer architecture changes over late micro-optimizations.

## Working rules

- Prefer pure functions by default. Introduce mutation when it clearly improves correctness, interoperability, or performance.
- Prefer data transformations over deep object hierarchies.
- Prefer small, explicit abstractions that read well at the call site.
- Keep invariants and checks close to the operation that depends on them. Avoid validating something early and then relying on that fact much later if the data can drift.
- If an external API has an awkward or rigid interface, hide it behind an adapter layer so the rest of the code can speak in the cleaner interface you wish you had.
- Use the SOLID checklist below for non-trivial modules, APIs, and code reviews, but adapt it to the local codebase instead of applying it mechanically.
- Prefer boundary validation over trusting implicit assumptions.
- Prefer code that is easy to verify by reading.
- Prefer self-explaining code with clear, not bloated, names. Avoid comments by default; when a comment is needed, keep it to one line unless it explains unusually complex behavior, a non-obvious invariant, or an out-of-the-ordinary approach.
- Remove AI-slop comments and other style that feels inconsistent with the surrounding file.
- Prefer adapting to an existing team style over forcing this skill mechanically into every file.

## SOLID checklist

For non-trivial design, implementation, refactoring, or review work, run through SOLID as a practical checklist. Skip or adapt any item when the framework, language, or surrounding repository makes it inappropriate.

- S: Does this unit have one clear reason to change? Split orchestration, parsing, persistence, and domain rules when they are tangled.
- O: For behavior that is actually needed or clearly imminent, can it be added through a focused function, variant, adapter, or module instead of fragile edits across many branches? Do not pre-build extension points for imaginary future cases.
- L: Can callers rely on the advertised contract for every implementation or variant without special-case surprises?
- I: Are interfaces narrow enough that callers depend only on operations they actually use?
- D: Does high-level policy depend on stable abstractions, plain data, or ports rather than low-level I/O clients and framework details?

Do not use SOLID as an excuse for class hierarchies, factories, or ceremony. Prefer the simplest structure that preserves the checklist's intent.

## Pattern examples

Use these examples as direction, not rigid templates.

### 1. Call-site-first API design

Good:

```python
def register_user(form_data):
    user = parse_signup_form(form_data)
    ensure_email_available(user.email)
    password_hash = hash_password(user.password)
    return save_user(user, password_hash)
```

Bad:

```python
def hash_password(password, rounds=12, salt=None, pepper=None): ...
def save_user_record(conn, table, user_dict, password_hash): ...

def register_user(form_data):
    # Caller is shaped around helper internals instead of the desired flow.
    ...
```

Write the top-level flow first. Let helper names and signatures conform to that flow.

### 2. Plain data plus systems over inheritance-heavy objects

Good:

```python
@dataclass
class Trade:
    symbol: str
    quantity: int
    limit_price: float | None

def validate_trade(trade: Trade) -> None: ...
def price_trade(trade: Trade, market) -> Price: ...
def execute_trade(trade: Trade, broker) -> Receipt: ...
```

Bad:

```python
class Trade:
    def validate(self): ...
    def price(self): ...
    def execute(self): ...

class OptionTrade(Trade): ...
class EquityTrade(Trade): ...
class CoveredCallTrade(OptionTrade): ...
```

When behavior is split across large class hierarchies, adding one feature often means touching many parent and child classes. Prefer data plus focused systems or modules.

### 3. Use unions for mutually exclusive states

Good:

```python
PaymentMethod = CardPayment | CashPayment | BankTransfer
```

Bad:

```python
class PaymentMethod: ...
class CardPayment(PaymentMethod): ...
class CashPayment(PaymentMethod): ...

if isinstance(method, CardPayment):
    ...
elif isinstance(method, CashPayment):
    ...
```

If a value is one of several mutually exclusive variants, model that directly instead of simulating it with inheritance and runtime type checks.

### 4. Push ifs up, fors down

Good:

```python
def process_items(items):
    credits = [item for item in items if item.kind == "credit"]
    debits = [item for item in items if item.kind == "debit"]

    if credits:
        apply_credits(credits)
    if debits:
        apply_debits(debits)
```

```python
def apply_credits(items):
    for item in items:
        post_credit(item)
```

Bad:

```python
def process_items(items):
    for item in items:
        if item.kind == "credit":
            post_credit(item)
        elif item.kind == "debit":
            post_debit(item)
        elif item.kind == "refund":
            post_refund(item)
```

Keep major decisions in the parent. Keep leaf functions narrow and low-branch so they are easy to verify and test.

### 5. Assert positive and negative space at boundaries

Good:

```python
def process_transfer(amount: int, flags: int) -> None:
    assert amount > 0
    assert amount <= ACCOUNT_BALANCE_MAX
    assert flags & RESERVED_FLAGS_MASK == 0
    assert amount != SENTINEL_AMOUNT
```

Bad:

```python
def process_transfer(amount: int, flags: int) -> None:
    # Trusts caller and hopes downstream code catches it.
    write_to_ledger(amount, flags)
```

Check what must be true and, when useful, what must not be true. This is especially important at parsing, I/O, API, and state-transition boundaries.

### 6. Prefer explicit procedural tests for non-trivial behavior

Good:

```python
def test_parse_signup_form_rejects_missing_email():
    result = parse_signup_form({"password": "secret"})
    assert result == {"error": "email_required"}
```

Bad:

```python
for case in cases:
    result = parse_signup_form(case.input)
    if case.want_error and result.ok:
        raise AssertionError("expected error")
```

For non-trivial cases, prefer tests where the behavior is obvious from direct code. The more logic in the test, the easier it is for the LLM to hide mistakes in the harness instead of the implementation.

## Testing checklist

1. Focus test effort on parsers, state machines, business rules, transformations, integration seams, and failure modes.
2. Prefer behavior-first development. For non-trivial work, decide what correct behavior looks like before writing the implementation.
3. Prefer integration tests, macro behavior tests, contract tests, or executable acceptance checks over low-value unit tests that only mirror implementation details.
4. When practical, use a red-green loop: write or identify a check that should fail first, make the implementation pass it, then rerun the tests to confirm the behavior.
5. You may adjust the test as your understanding improves, but do not move the goalposts to make a broken implementation look correct. If the expected behavior changes, make the reason explicit.
6. Because you are an LLM, do not optimize for a narrow test harness while missing the real contract. Use tests to expose the intended behavior, not to game the reward function.
7. Do not spend time proving trivial getters or one-line passthrough helpers unless they are unusually risky.
8. Bias toward integration or behavior-level tests when they give more stable coverage than implementation-coupled unit tests.
9. Use table-driven or parameterized tests when they genuinely improve coverage or maintainability, but avoid them when they mainly add indirection.
10. Keep failing test output obvious. A reader should be able to tell what behavior broke without reconstructing test control flow.
11. Treat LLM-authored tests as temporary scaffolding until proven otherwise. Before committing or pushing, keep only tests that guard stable behavior, prevent a real regression, or fit the repository's existing test style; remove local test slop that only helped you think.

## Final verification checklist

Before claiming the task is done, committing, or pushing:

1. Re-read the user request and confirm the implementation matches the intended behavior.
2. Run the relevant local checks, tests, build, or executable validation gate. If no useful check exists, say so explicitly.
3. Read the command output before claiming success. Do not treat a started command as a passed command.
4. Review the diff for AI slop: unnecessary comments, abnormal defensive code, speculative abstractions, broad rewrites, casts that dodge type errors, and test or documentation spam.
5. Keep only tests and docs that earn their place in the repository. Remove local scaffolding that helped you think but does not protect stable behavior.

## Long-running sessions

For substantial multi-step work, keep only two local coordination files when they help future agents continue the task:

1. When continuing an existing task, first check whether `plan.md` and `implementation-journal.md` exist. If they do, read `plan.md` first, then `implementation-journal.md`.
2. If no plan exists and the task is substantial, create `plan.md` for the macro plan, intended behavior, important boundaries, validation gates, and testing approach.
3. After the user accepts `plan.md`, do not edit it without user permission.
4. Write checkpoint notes, verification results, surprises, and deviations from the plan in `implementation-journal.md`.
5. When you deviate, record what changed and why in `implementation-journal.md`.
6. At the end of a completed task, briefly ask whether to delete either local note or archive/summarize anything useful for observability. Keep cleanup simple.
7. Do not commit or push these local working notes. When practical, add them to `.git/info/exclude` rather than the repo's `.gitignore`.

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
- For optional concrete do/don't examples when the task is complex or ambiguous: `references/examples.md`
