# P10 text critique — adjudication (2026-07-19)

Critique: scratchpad/p10-text-critique/final.md (xhigh, 24 findings).
Ruling principle: P10 ships AS-IS — every measurement attaches to this exact
text; edits spawn P10.1, which needs its own eval round before promotion.

## REJECTED — the critique's theory is falsified by our measurements

- #8 "delete the optimize-for-next-change rule (speculative refactoring)":
  this rule drove the agentic pairwise wins and the LOWEST verified extension
  cost (+198 lines vs current +208, baseline +229, verifier green). No judge
  ever cited over-refactoring in a P8/P10 diff. The predicted failure mode
  did not occur in any of 5 agentic runs.
- #18 "soften the structural-remedy prescription (over-refactoring bias)":
  this is THE paragraph that produces A1-full/A2+S3 detection (fires 3/3 in
  P9, 0/3 without it). Predicted bias would show as taste-trap hits: zero
  F1/F2/F3 penalties across every rep of every condition. Softening it is
  measurably equivalent to deleting the review lift.
- #14 "delete the pattern-cues section (duplication)": P12 dropped the cues
  and its agentic output regressed to a subclass tree + isinstance coupling
  (judge's words), losing its pairwise. The "duplication" is reinforcement
  that demonstrably changes output structure.
- #20 core "lenses-2-4 pressure manufactures findings": precision was ~100%
  in all 20+ scored reps; zero manufactured-nit pattern observed. The
  sentence exists because sol demonstrably SUPPRESSES quality findings
  without it (the documented lens-3 suppression axis). Adopt only the
  clarifying tail: "a lens may legitimately produce no findings."
- #19 "tags are ceremony": tag-free conditions (P1) correlate with missed
  systematic sweeps; cost is negligible. Keep.
- #23 "coordination-file workflow intrusive": deliberate owner preference
  (long-session workflow); out of scope.

## ADOPTED-IN-PRINCIPLE — real defects, queued for P10.1 (needs eval round)

- #1 partial: testing-policy tension is real. Fix by making Implementation
  rule 1 authoritative during implementation and deleting the redundant
  red-green line in the Testing checklist; do NOT soften "every requirement"
  (that strength is the 0-tests -> 13-tests delta).
- #2/#3: distinguish external validation from internal invariant assertions;
  make "trace invariants first" the authoritative anti-duplication rule.
- #4: "hardest real requirement" -> "hardest CONFIRMED requirement; not
  hypothetical scale" (matches the original design intent).
- #5, #6, #7, #10, #11, #13, #17, #21, #22: accepted as written (falsely
  diagnostic call-site line; bottleneck list; batch-rule scope; cleanup
  scope; generated-artifacts repo-convention exception; test-loop vs
  parameterization harmony; small-diff single-pass review; fan-out
  authorization; sub-agent context).
- #12: reframe LLM-authored-test rule from provenance to scaffolding intent.
- #16: order Working rules before Implementation rules; phase map gains the
  Testing-checklist pointer for implementers.

## Process note

The critique validates the gate: 24 findings, ~15 adopted-in-principle, but
the 4 highest-impact "fixes" would have deleted precisely the mechanisms the
harness measured as the source of P10's wins. Prompt criticism without an
eval harness optimizes for essay coherence, not model behavior.
