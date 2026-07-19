# Changelog

## v1.2.0 — 2026-07-19

The P15 "phase-scoped" text: winner of a 16-permutation eval campaign
(~90 hand-scored runs, five cases, preregistered ship rules; full report in
`evals/cases-v5/`). Skill diff vs v1.1.0: +14/−5 lines.

- New **Implementation rules** section: behavior-test-first with an
  anti-assert-spam clause, fix causes not sites, optimize for the next
  change, no quality decay on long task lists, campsite rule +
  tracked-artifact exception.
- Review **lens 3** prescribes structural remedies (config object /
  responsibility split / seam), not cosmetic fixes.
- Review **lens 1** distinguishes untrusted-data validation from
  internal-invariant assertions: invariant gaps are substance, duplicate
  validation is noise.
- Small diffs get one combined review pass; a lens may legitimately
  produce no findings.
- Mirror (`.claude/skills/metis/`) and `PORTABLE_PROMPT.md` updated to match.

## v1.1.0 — 2026-07-04

Review-mode and agentic-discipline round (`evals/cases-v3/`,
`evals/cases-v4/`): tiered lens-based review with severity format and
verify-before-reporting, sub-agent fan-out policy for large diffs,
review-examples reference, batch-classification design principle 10,
long-running-session coordination files.

## v1.0.1 — 2026-06-08

Wording and reference-path fixes; portable prompt sync.

## v1.0.0 — 2026-06-06

Initial release: design principles, LLM agent process, working rules,
SOLID checklist, pattern cues, testing checklist, final verification,
references (architecture, api-design, testing, performance-and-safety,
examples).
