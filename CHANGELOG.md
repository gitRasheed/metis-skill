# Changelog

## v1.2.0 (2026-07-19)

The P15 "phase-scoped" text, winner of a sixteen-permutation eval campaign
(about ninety hand-scored runs, five cases, preregistered ship rules; the
full report lives in `evals/cases-v5/`). The skill diff against v1.1.0 is
14 insertions and 5 deletions.

- New implementation-rules section: write the failing behavior check
  first and commit it with the fix, keep test suites small and
  behavior-pinning rather than assert-heavy, fix causes not sites,
  restructure for the next change while context is loaded, hold quality
  on long task lists, and leave touched code cleaner without committing
  generated artifacts the repo does not track.
- Review lens 3 prescribes structural remedies (a config object, a
  responsibility split, a seam) instead of cosmetic fixes.
- Review lens 1 distinguishes untrusted-data validation from
  internal-invariant assertions: invariant gaps are substance, duplicate
  validation is noise.
- Small diffs get one combined review pass, and a lens may legitimately
  produce no findings.
- The Claude Code mirror and `PORTABLE_PROMPT.md` were regenerated to
  match.
- Post-release addition to the agent-process rules: search the codebase
  for an existing helper before writing a new one. Added by owner
  direction after the campaign's gates; it gets its A/B next round.

## v1.1.0 (2026-07-04)

Review-mode and agentic-discipline round (`evals/cases-v3/`,
`evals/cases-v4/`): tiered lens-based review with severity format and
verify-before-reporting, sub-agent fan-out policy for large diffs,
review-examples reference, batch-classification design principle 10,
long-running-session coordination files.

## v1.0.1 (2026-06-08)

Wording and reference-path fixes; portable prompt sync.

## v1.0.0 (2026-06-06)

Initial release: design principles, LLM agent process, working rules,
SOLID checklist, pattern cues, testing checklist, final verification,
references (architecture, api-design, testing, performance-and-safety,
examples).
