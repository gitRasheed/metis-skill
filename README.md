# Metis

A coding-style skill for SOTA LLM agents. Frontier models already get the code right; Metis makes them write code a team can still read and extend a year later: plain data over class hierarchies, call sites that read like the product, simple control flow, input parsed once at the edge, and tests that show what they check.

It's for people who let agents write substantial code in long-lived repos.

## Before and after

**Python: I/O at the edge, logic testable on plain values.**

```python
# Before: queries, scoring and writes in one loop
def refresh_recommendations(db, user_id):
    profile = db.load_profile(user_id)
    for row in db.load_candidates(user_id):
        db.save_recommendation(user_id, row.id, score(row, profile))

# After: a thin shell does I/O; ranking is a pure function
def refresh_recommendations(store, user_id):
    ranked = rank(store.profile(user_id), store.candidates(user_id))
    store.replace_recommendations(user_id, ranked)
```

**Rust: exclusive states are an enum, not a struct of options.** The after is from [mathom](https://github.com/gitRasheed/mathom)'s scanner.

```rust
// Before: every consumer decodes which fields are set
struct ScanUpdate {
    batch: Option<EntryBatch>,
    error: Option<(NodeId, String)>,
    progress: Option<ScanProgress>,
    finished: bool,
}

// After: one event, exactly one kind, matched exhaustively
pub enum ScanEvent {
    Batch(EntryBatch),
    DirError { id: NodeId, message: String },
    Progress(ScanProgress),
    /// Always the final event, even when cancelled or failed.
    Done(ScanStats),
}
```

**C++: parse once, then trust the type.**

```cpp
// Before: raw strings re-checked wherever they're used
void schedule(const std::map<std::string, std::string>& cfg);

// After: one parser at the boundary
struct Schedule { std::chrono::seconds interval; };
std::expected<Schedule, ConfigError> parse_schedule(const RawConfig& cfg);
void schedule(const Schedule& s);
```

## What it pushes toward

- plain data and focused functions over behavior-heavy hierarchies
- call-site-first APIs, with the shape of the work (data layout, I/O round trips) decided up front
- ifs up, fors down
- parsing at trust boundaries, atomic check-then-act, idempotent side effects
- behavior-first tests with no logic in the test body
- few comments, each saying why
- lens-based code review that names structural fixes

The repo's conventions and the task's scope win, and the agent applies all of this quietly: no rule citations in code, comments or commits.

## Install and use

```bash
npx skills add https://github.com/gitRasheed/metis-skill --skill metis -g
```

Add `-a codex` or `-a claude-code` for a single agent; update with `npx skills update -g`. Metis loads on its own for features, refactors, design, performance work and reviews, and skips small fixes. Invoke it with `$metis` or `/metis` anytime.

## Evals

Each release is A/B tested against the last one under thresholds set before scoring. Early rounds (`evals/cases-v3/` to `cases-v6/`) showed the skill made models more correct. Frontier models now pass hidden tests either way, so `evals/cases-v7/` measures what's left: blinded "which codebase would you rather inherit" judging on Java and Python tasks, review depth against frozen keys, a noise scan, and cost. v1.4.0 holds v1.3.2's quality with a shorter, quieter text; [REPORT.md](evals/cases-v7/REPORT.md) has the numbers.

## Repo

`skills/metis/` is the skill, `PORTABLE_PROMPT.md` is the plain-markdown version, and `evals/` holds cases, keys and reports.

## Sources

- call-site-first design: [SICP](https://sicp.sourceacademy.org/)
- plain data over hierarchies, and performance designed in: Casey Muratori's Better Software Conference talks, ["The Big OOPs"](https://www.computerenhance.com/p/the-big-oops-anatomy-of-a-thirty) (2025) and ["The Root of the Root of All Evil"](https://www.youtube.com/watch?v=hpj6r6CjJf8) (2026)
- typed domain state and unions: [Scott Wlaschin, *Domain Modeling Made Functional*](https://pragprog.com/titles/swdddf/domain-modeling-made-functional/)
- parse, don't validate: [Alexis King](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/)
- assertions and negative space: [TigerStyle](https://github.com/tigerbeetle/tigerbeetle/blob/main/docs/TIGER_STYLE.md)
- deep modules, defining errors out of existence: [John Ousterhout, *A Philosophy of Software Design*](https://web.stanford.edu/~ouster/cgi-bin/book.php)
- replay-safe effects: [Eric Normand, *Grokking Simplicity*](https://grokkingsimplicity.com/)
- minimal test logic: [ThePrimeagen's Boot.dev testing lecture](https://youtu.be/FknTw9bJsXM?t=2500)
