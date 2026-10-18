# Metis

Metis is a coding-style skill for SOTA LLM agents working in real codebases. Current frontier models already get the code right; Metis makes them write code that a team can still read and extend a year later. That means plain data instead of class hierarchies, call sites that read like the product behavior, simple control flow, input parsed once at the edge, and tests that show what they check.

It's for people who let agents write substantial code in large or long-lived repos and care what that code looks like after the fifth feature, not just whether the first one passes.

## Before and after

Each pair shows a common agent habit and the shape Metis steers toward. They illustrate the rules; they are not eval transcripts.

**Python: keep I/O at the edge so the logic is testable on plain values.**

```python
# Before: queries, scoring and writes interleaved in one loop
def refresh_recommendations(db, user_id):
    profile = db.load_profile(user_id)
    for row in db.load_candidates(user_id):
        score = row.base_score + (10 if row.country == profile.country else 0)
        db.save_recommendation(user_id, row.id, score)
```

```python
# After: a thin shell does I/O; the ranking is a pure function
def refresh_recommendations(store, user_id):
    profile = store.profile(user_id)
    ranked = rank(profile, store.candidates(user_id))
    store.replace_recommendations(user_id, ranked)

def rank(profile, candidates):
    return [Recommendation(c.id, c.base_score + (10 if c.country == profile.country else 0))
            for c in candidates]
```

**Rust: mutually exclusive states are an enum, not a struct of options.**

```rust
// Before: every caller re-checks which combination of fields is set
struct Payment {
    card_last4: Option<String>,
    iban: Option<String>,
    is_cash: bool,
}
```

```rust
// After: exactly one state at a time, and match is exhaustive,
// so a new variant is a compile error at every call site that ignores it
enum Payment {
    Card { last4: String },
    Bank { iban: String },
    Cash,
}
```

**C++: parse untrusted input once, then trust the type.**

```cpp
// Before: raw strings checked everywhere they're used
void schedule(const std::map<std::string, std::string>& cfg) {
    if (!cfg.count("interval") || std::stoi(cfg.at("interval")) <= 0) throw ...;
    // ...and the same check again in three other functions
}

// After: one parser at the boundary, a type the core can rely on
struct Schedule { std::chrono::seconds interval; };
std::expected<Schedule, ConfigError> parse_schedule(const RawConfig& cfg);
void schedule(const Schedule& s);  // no re-validation inside
```

## What it pushes models toward

- plain-data domain models and focused functions over behavior-heavy hierarchies
- call-site-first API design: write the caller you wish existed, then the helpers
- ifs up, fors down: parents decide, leaves loop over uniform work
- parse at trust boundaries; assert only invariants that would corrupt state
- atomic check-then-act and idempotent side effects
- behavior-first tests with no logic in the test body
- few comments, each saying why
- a lens-based code review that reports structural fixes, not just bugs

These are defaults, not laws. The repo's conventions and the task's scope win, and the skill tells the agent to apply it quietly: no rule citations in code, comments or commits.

## Install

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g
```

Add `-a codex` or `-a claude-code` to install for a single agent. Update with `npx skills update -g`. Release notes live on the [releases page](https://github.com/gitRasheed/metis-skill/releases).

## Use

Metis loads on its own for chunkier work: building a feature, refactoring, designing APIs or module boundaries, performance work, and reviewing a pull request. It stays out of small bug fixes and one-line edits. Invoke it explicitly with `$metis` or `/metis` when you want it anyway.

## Evals

Metis is developed through isolated A/B runs against the previous release, under thresholds written down before scoring. More texts have been refused than shipped.

The early rounds (`evals/cases-v3/` to `cases-v6/`) measured whether the skill made models more correct, and it did. By 2026 that question was saturated: frontier models pass the hidden tests with or without the skill. So the latest round, `evals/cases-v7/`, measures what's left:
- blinded "which codebase would you rather inherit" judging on Java and Python tasks;
- review depth against frozen answer keys;
- an automated noise scan for rule citations, comment spam and assertion spam;
- cost.

v1.4.0 came out of that round. It holds v1.3.2's code quality and review depth across 60 blinded runs with a shorter, quieter text that loads only for chunkier work. `evals/cases-v7/REPORT.md` has every number.

## What is in this repo

- `skills/metis/` is the canonical skill
- `.claude/skills/metis/` mirrors it for Claude Code
- `PORTABLE_PROMPT.md` is the plain-markdown version for tools without skill support
- `evals/` holds the eval cases, scoring keys, and reports; raw run output stays git-excluded

## Sources

The core ideas are borrowed, then filtered through the evals:

- call-site-first design by wishful thinking: [SICP](https://sicp.sourceacademy.org/)
- plain data plus systems over object hierarchies: [Casey Muratori, "The Big OOPs"](https://www.computerenhance.com/p/the-big-oops-anatomy-of-a-thirty)
- typed domain state and unions for exclusive states: [Scott Wlaschin, *Domain Modeling Made Functional*](https://pragprog.com/titles/swdddf/domain-modeling-made-functional/)
- parse, don't validate: [Alexis King](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/)
- assertions, negative space, performance ordering: [TigerStyle](https://github.com/tigerbeetle/tigerbeetle/blob/main/docs/TIGER_STYLE.md)
- performance designed in, serial dependency chains, contracts that include cost: Casey Muratori, ["The Root of the Root of All Evil"](https://www.youtube.com/watch?v=hpj6r6CjJf8) and his [Pragmatic Engineer interview](https://youtu.be/8xBJPa_480Q)
- deep modules and defining errors out of existence: [John Ousterhout, *A Philosophy of Software Design*](https://web.stanford.edu/~ouster/cgi-bin/book.php)
- replay-safe effects with stable identities: [Eric Normand, *Grokking Simplicity*](https://grokkingsimplicity.com/)
- tests are code, minimize test logic: [ThePrimeagen's Boot.dev testing lecture](https://youtu.be/FknTw9bJsXM?t=2500)

The rest (review lenses, thrash detection, quiet application, LLM-test cleanup) came out of the eval rounds themselves.
