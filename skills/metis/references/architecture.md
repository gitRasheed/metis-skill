# Architecture

Use architecture that models capabilities cleanly rather than mirroring domain nouns too literally.

## Prefer data plus systems

When the domain is rich or combinatorial, prefer:

- plain data structures for domain state
- focused modules or systems for behavior
- explicit state transitions

This usually scales better than deep inheritance or large classes with mixed responsibilities.

## Reach for unions before inheritance

If a thing can only be in one of several mutually exclusive states, prefer a discriminated union or tagged variant over subclass trees and runtime type checks.

Good fits:

- message kinds
- payment methods
- workflow states
- request variants

## Reach for composition when features combine freely

If an entity can mix and match many independent traits, prefer composition over inheritance.

Good fits:

- domain records with optional capabilities
- systems that process only the fields they need
- pipelines where each stage enriches or transforms plain data

## Smells to watch for

- adding one feature requires touching many parent and child classes
- behavior is scattered across type hierarchies
- runtime type checks are compensating for a poor state model
- objects exist mainly to hide data rather than protect invariants

## Practical default

Start simple:

- a plain struct, record, dict, or typed object for data
- one module or set of functions per behavior cluster
- one obvious place for orchestration

Only introduce richer abstractions when they clearly earn their keep.
