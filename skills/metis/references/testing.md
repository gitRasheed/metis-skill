# Testing

Write tests that make behavior obvious.

## Prefer explicit tests

When practical, prefer tests where each case is written directly rather than hidden inside loops, branching, or layers of fixtures.

Prefer:

```python
def test_parse_signup_form_rejects_missing_email():
    result = parse_signup_form({"password": "secret"})
    assert result == {"error": "email_required"}
```

Avoid:

```python
for case in cases:
    result = parse_signup_form(case.input)
    if case.want_error and result.ok:
        raise AssertionError("expected error")
```

In the second example, the test harness itself contains logic that can be wrong. In the first, a failure points much more directly at the production behavior.

Benefits:

- failures point more directly at the broken case
- expected behavior is easier to read in sequence
- the test itself is less likely to contain subtle logic bugs

## Prefer behavior over implementation

Favor tests that verify:

- visible outputs
- state transitions
- contract boundaries
- cross-module behavior

Avoid overfitting tests to private structure unless the implementation detail is itself the contract.

For LLMs, this matters even more: a model can accidentally optimize for making the harness green instead of satisfying the real behavior. Favor tests that make the contract difficult to misread.

## Use indirection only when it helps

Parameterized or table-driven tests are still useful when:

- many cases truly share the same shape
- duplication would become harder to maintain than the table
- the test runner still produces readable failures

If the indirection hides the story of the test, simplify it.

Use indirection when it truly reduces maintenance cost, not by default.

## Good testing targets

- parsers
- state machines
- business rules
- transformations
- failure handling
- integrations with files, databases, services, or queues

These are good fits because breakage there is expensive and behavior usually matters more than internal decomposition.

## Lower-priority targets

- trivial passthroughs
- obvious getters and setters
- wrappers with no meaningful branching or transformation
