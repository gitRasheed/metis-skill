# API Design

Design APIs from the caller outward.

## Call-site-first method

Before implementing helpers, write the code that should use them.

Example:

```python
def register_user(form_data):
    user = parse_signup_form(form_data)
    ensure_email_available(user.email)
    password_hash = hash_password(user.password)
    return save_user(user, password_hash)
```

At this stage, the helpers do not need to exist yet. The point is to lock in the desired flow, names, and boundaries first.

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

Counterexample:

```python
def hash_password(password, rounds=12, salt=None, pepper=None): ...
def save_user_record(conn, table, user_dict, password_hash): ...
```

If you build helpers like this first, the top-level flow often ends up shaped by helper internals rather than by the cleanest possible caller experience.

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

The goal is not fantasy for its own sake. The goal is to make the rest of your code speak in the cleanest interface you can responsibly maintain.
