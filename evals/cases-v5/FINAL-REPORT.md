# Skill-permutation campaign, final report

2026-07-19. Model under test: gpt-5.6-sol via codex exec, high reasoning
effort. Adversarial critiques and case authoring ran at xhigh. Claude
hand-scored every review transcript against frozen keys; a second
gpt-5.6-sol instance judged code quality on blinded, order-randomized,
anonymized diff pairs. Every score in this report can be re-derived from
`rescore-v1.3.md`, `rescore-suite.md`, and `rescore-holdout.md`.

The campaign asked which exact skill text is best, ran sixteen candidates
through a five-case suite under preregistered rules, and shipped the
winner (P15) as Metis v1.2.0. This document is the full record: the
suite, the numbers, what got refused and why, and the mechanism that
explains the results.

## 1. Outcome

P15 is now `skills/metis/SKILL.md`. The diff against v1.1.0 is 14
insertions and 5 deletions on a 174-line file: a new implementation-rules
section, a structural-remedy prescription in review lens 3, and three
review-scoped sentences (small diffs get one combined pass, lens 1
weighs invariant gaps over duplicate validation, a lens may produce no
findings).

P15 was the only candidate of sixteen to pass every preregistered gate.
Two texts that looked stronger on single measures were refused by the
rules: P13 had the best review scores of the campaign but lost the
torefall quality pairwise 0-2 with the worst extension cost, and P10 won
the agentic arena but failed the controller clause. A four-arm bisect of
their difference then isolated the cause and produced P15. A high-n
confirmation round (section 10) ran before promotion.

Diff sizes for the other finalists, measured the same way: P10 +10/-2,
P13 +24/-16.

## 2. The suite

| case | type | discriminator | calibration |
|---|---|---|---|
| api-evolution-review | review | key v1.3, max 20.5, atomic-cause credit | frozen after critique |
| background-jobs-review | review | key v5.1.1, max 30, depth markers | frozen after critique |
| torefall-v2 | agentic | 19 hidden gates, blinded pairwise, stage-B extension cost | starter 6/19, two independent 19/19 proofs |
| charge-controller | agentic | 12 hidden gates from a pinned spec | starter 0/12, verified golden 12/12 |
| catalog-sync (cases-v4) | holdout | key frozen before the campaign | never used for tuning |

Every case went through xhigh adversarial critique before its key froze.
A sixth case (export-agentic) was retired after the worst critique of the
set. Leak controls: verifiers live outside the case repos, every runner
scrubs `__pycache__`, and scoring keys never enter an eval workspace.

## 3. Review arena

### api-evolution, uniform skill-file protocol (max 20.5)

| condition | reps | mean | notes |
|---|---|---|---|
| P9-structural | 3 | 13.25 | zero variance; only condition to find A1-full and the A2+S3 pair |
| P11-gated | 3 | 12.08 | bimodal (13.25 / 10.25 / 12.75); gating is unreliable |
| P8-agentic | 1 | 11.75 | |
| P10-hybrid | 3 | 10.92 | the implementation-rules section costs review depth |
| P12-modular | 3 | 10.83 | failed even though transcripts show the reference files were read |
| baseline | 1 | 10.00 | |
| P1-condensed | 1 | 9.50 | |
| current, text only | 3 | 8.83 | 11.0 / 8.0 / 7.5 |
| P6-tiger | 1 | 8.00 | |

One early current-skill run (13.5) used the references kit while the
permutations ran text-only, so it is excluded from this table and
reported under the kit protocol below.

### api-evolution, references-kit protocol (deployment configuration)

| condition | reps | mean | per-rep |
|---|---|---|---|
| current | 3 | 12.42 | 13.5 / 12.5 / 11.25 |
| P10 | 3 | 12.83 | |
| P13 | 3 | 12.17 | 14.5 / 11.75 / 10.25 |
| P15 | 3 | 12.67 | 14.75 / 9.75 / 13.5 |

P15's first rep is the best api transcript of the campaign (14.75; the
previous best was P13's 14.5), and its spread is also the widest. At
n=3 per side the conditions are at parity on this case.

### background-jobs, references-kit protocol (max 30)

| condition | reps | mean | per-rep |
|---|---|---|---|
| P13 | 2 | 21.88 | 23.75 / 20.0 |
| P9 | 2 | 21.75 | 22.5 / 21.0 |
| current | 5 | 20.60 | 23.0 / 21.0 / 18.5 / 22.5 / 18.0 |
| P15 | 5 | 20.50 | 21.5 / 21.5 / 16.5 / 22.5 / 20.5 |
| baseline | 3 | 19.00 | 18.0 / 21.0 / 18.0 |
| P10 | 2 | 17.25 | 16.0 (one factual false positive, -2) / 18.5 |

P10's implementation-rules section cost it review breadth here, and
P13's wording pass recovered it (the two ranges do not overlap). At n=5
per side, P15 and current are at parity. One recurring risk worth
naming: a specific false positive (claiming `LIGHTWEIGHT_TYPES` is
undefined when the prompt defines it) appeared in 3 of 9 runs across the
P10 text family, including one P15 rep, and in none of the 5 current
runs.

## 4. Agentic arena

### torefall-v2

Every condition saturated the behavioral gates, so this case
discriminates through the blinded pairwise judgment and the stage-B
extension probe (grow the codebase against a follow-on task, count the
verifier-green diff).

| condition | gates (17 then 19) | pairwise vs current | stage-B extension |
|---|---|---|---|
| current | pass | anchor, rated 7 to 9 | +208 lines |
| P10 | pass, 3 reps | won 2-1 (rated 8/8/7) | +198 lines |
| P15 | pass, 5 reps | won 3-2 (rated 9/8/9/7/8) | +254 lines |
| P13 | pass, 2 reps | lost 0-2 (rated 7/7) | +325 lines |
| P8 | pass | won its single pair | |
| P12 | pass | lost (its diff rebuilt the subclass tree) | |
| baseline | pass | lost (committed zero tests) | +229 lines |

The baseline result is the clearest single finding of the campaign: the
model passes the gates but commits no tests at all, probing with
throwaway scripts instead. Skill conditions committed 9 to 16 behavior
tests per run with no narrating comments, and judges cited the test
suites in most win rationales.

### charge-controller v2

Gates here sit far from ceiling (starter 0/12, verified golden 12/12),
so they discriminate directly, at the price of heavy per-run variance.

| condition | reps | gates per rep | mean | committed tests |
|---|---|---|---|---|
| P13 | 2 | 6 / 3 | 4.5 | 9 to 10 |
| P15 | 3 | 5 / 4 / 1 | 3.33 | 9 to 13 |
| current | 3 | 1 / 6 / 1 | 2.67 | 6 to 10 |
| baseline | 2 | 3 / 2 | 2.5 | 0 |
| P10 | 2 | 1 / 1 | 1.0 | 6 to 8 |

Current spans 1 to 6 across three runs, so treat condition means as
directional. The zero-tests baseline signature repeats.

The pairwise round used two anchors plus a current-vs-current pair to
measure the judge's own noise floor. That floor came out at one rating
point, and every skill-vs-skill margin sat exactly at it, so on this
case the judge separates skill from baseline (baseline lost both pairs)
but not skill from skill. On the baseline loss: "the cleaner reusable
PID model, but leaves its substantially more complex controller behavior
effectively untested." Note on the instrument: every diff was docked for
the rebuilt `libcharger.so`, which the starter repo tracks on purpose,
so the ding is uniform across conditions.

### catalog-sync holdout

An adversarial methodology review of this campaign said its most
damaging weakness was the lack of an untouched holdout. This case
answers that: its key froze before the campaign began, no permutation
ever ran on it during tuning, and it served only as a final gate.

| condition | reps | mean | percent of max |
|---|---|---|---|
| P13 | 2 | 16.75 | 85.9 |
| P10 | 2 | 16.5 | 84.6 |
| P15 | 2 | 16.0 | 82.1 |
| current | 2 | 15.25 | 78.2 |
| baseline | 2 | 14.0 | 71.8 |

Every condition found all six blocking defects. The entire skill margin
is the taste tier: rate limiting at the wrong layer, a silent
unknown-kind fallthrough, a dead defensive check, test shape, narrating
comments. That is the exact profile the skill exists to add, and the
ordering from the tuning cases transferred to a case none of the texts
had seen. Zero false positives across all ten transcripts.

## 5. Token cost

Uncached input plus output, which is what a run actually bills; raw
input totals are dominated by cache re-reads.

| condition | torefall | controller | api review |
|---|---|---|---|
| baseline | 56K + 22K | 47-52K + 17-20K | 14K + 5K |
| current | 105K + 34K | 66-86K + 23-24K | 16K + 6K |
| P9 | 65K + 27K | | |
| P10 | 93K + 41K | 65-81K + 25-26K | 14K + 7K |
| P13 | 83-91K + 34-36K | 79-102K + 28-33K | |
| P15 | 72-96K + 31-42K | | |

No variant costs meaningfully more than current. The skill's premium
over baseline (roughly 1.3 to 1.6 times the input) is what buys the
committed test suites and the taste findings.

## 6. What the blinded judge said about the code

Selected rationales from the torefall pairwise, quoted verbatim. The
judge saw only anonymized diffs.

On the baseline (rated 5): "concentrates most orchestration and economy
in an increasingly monolithic `world.py` with subclass/type coupling...
adds no tests, a serious maintainability defect."

On P8 (rated 8, win): "more cleanly separates combat, economy,
orchestration, and boss lifecycle... tests pin subtle observable rules
such as join-order tie-breaking, exact expiry timing, and reconnect
idempotency."

On P9 (rated 6, loss, despite being the review champion): "increasingly
monolithic `world.py`... committed `__pycache__` binaries are a notable
hygiene defect." The no-committed-bytecode clause in the shipped skill
exists because of this run.

On P10 (rated 8, win): "deliberately enforces invariants, performs
atomic copy-before-commit mutations, defines deterministic targeting,
and models encounter lifecycle explicitly."

On P12 (rated 7, loss): "subclass tree and `isinstance` coupling." Its
pattern cues lived in a separate reference file, and the code shows they
did not fire.

On P15 (rated 9, win): "data-driven class model, ability dispatch table,
explicit encounter state, and isolated boss/economy logic make future
changes more localized. It also enforces stronger boundary contracts,
replay safety, atomicity, and gold-conservation invariants."

## 7. Three regularities

These held across the suite for this model. The permutation family gives
partial ablation (P8 is the implementation rules alone, P9 the review
amendment alone, P10 both, all on the same chassis), but ordering and
length were not factorially controlled, so they are regularities rather
than laws.

1. Interference. Text added for one phase taxed the other phase in
   every variant that showed a gain. P10's implementation rules won the
   agentic arena and cost review breadth; P13's review wording recovered
   the breadth and cost agentic quality. No single text scored best in
   both arenas.
2. Inline beats indirection. Skill content fired reliably only when it
   sat in the file the model holds during that phase. Skip-gating (P11)
   was bimodal. Modular reference files (P12) underperformed even when
   the transcript proves the file was read. The P9 amendment fired in
   three of three runs inline and zero of three runs one file-hop away.
3. Gate saturation depends on the task, not the model. Torefall's fully
   specified task let every condition pass every gate, pushing all
   discrimination into the pairwise judgment and the extension probe.
   The controller case, far from ceiling, discriminates through gates
   directly but pays for it in variance.

## 8. Adversarial critique, preregistration, and limits

Every case key and the leading prompt text went through xhigh
adversarial critique. The prompt critique returned 24 findings. Four
were rejected because the measurements said otherwise: each of those
four fixes would have removed something the harness had identified as a
win source (the next-change rule, the structural-remedy paragraph, the
pattern cues, the lens 2-4 mandate). About fifteen wording defects were
adopted and became P13. The general lesson: prompt criticism without an
eval harness optimizes for essay coherence, not model behavior.

A separate xhigh review attacked the methodology itself and drove four
fixes before the deciding runs landed: the untouched holdout became a
required gate, the ship rule was preregistered in the journal with
numeric thresholds, the controller pairwise gained a second anchor, and
protocol labels became explicit in every table.

Limits, disclosed rather than fixed: the hand-scorer knew which
condition produced each transcript (frozen keys and per-finding
derivations give auditability, not inter-rater validity). The quality
judge shares a model family with the systems under test, so
self-preference is possible; blinding, order randomization, and the
independent extension-cost metric are mitigations, not proof. Task-level
n is five, so arena-level claims are directional. The extension-cost
metric is a single unvalidated proxy. Early review cells were observed
before the ship rule was written; the agentic and holdout cells were
not.

## 9. The bisect: placement is the mechanism

P13's refusal left a question. Its fifteen wording edits recovered
P10's review breadth but broke its agentic quality; which edits did
what? Four arms, each preregistered against the same two gates (bgjobs
mean at least 20.0; torefall gates perfect and pairwise not
majority-lost), with the holdout reserved as a final gate:

| arm | change vs P10 | bgjobs | torefall pairwise | gates |
|---|---|---|---|---|
| P14a | two sentences, review mode only | 17.25 (same as P10) | won 2-0 (8, 9) | review gate failed |
| P14b | validate-vs-assert edits, spread through the text | 18.6 | 1-1 | review gate failed |
| P14c | both together, spread | 20.75 | lost 0-2 | agentic gate failed |
| P15 | P14a plus the same content as one sentence inside lens 1 | 21.5, no variance | 1-1 (9, 8) | both passed |

P15 then passed the holdout gate (16.0 against current's 15.25) and
became the candidate.

The mechanism these four arms isolate: interference comes from where a
sentence sits, not what it says. The validate-vs-assert distinction
placed in sections the implementing agent holds (P13, P14c) degraded the
code both times, with near-identical judge rationales. The same
distinction scoped into the review lens lifted review scores further
than the spread-out version and left the code untouched, because the
implementing agent never reads it. The design rule that falls out: put
each phase's guidance inside that phase's section, keep the
implementer-held text minimal, and never let cross-phase principles
float in globally-held text.

## 10. Confirmation round

Run before promotion at the owner's direction: five bgjobs and five
torefall-pairwise reps per side, three api and three controller reps per
side, identical protocols.

| instrument | n per side | P15 | current | reading |
|---|---|---|---|---|
| bgjobs review (of 30) | 5 | 20.5 | 20.6 | parity |
| api review (of 20.5) | 3 | 12.67 | 12.42 | parity |
| torefall gates | 5 | all pass | all pass | parity at ceiling |
| torefall pairwise | 5 | 3-2, mean rating 8.2 | 7.8 | edge to P15 |
| controller gates (of 12) | 3 | 3.33 | 2.67 | edge to P15, noisy |
| holdout (of 19.5) | 2 | 16.0 | 15.25 | edge to P15 |
| stage-B extension | 1 | +254 | +208 | edge to current |
| tokens | | comparable | comparable | parity |

The small-sample review-superiority claims from earlier in the day did
not survive these n's, and the report stands on what did: no regression
on any instrument, a majority of blinded quality wins with a 0.4 mean
rating edge (P15 took the round's two 9s; its losses were to the
anchor's only 9s), favorable direction on the two cases P15 never
trained on, and one unfavorable single observation (stage-B). On that
basis P15 shipped as v1.2.0.

## 11. What changed in the prompt, and why it is still Metis

The shipped diff, in full:

1. An implementation-rules section: write the failing behavior check
   first when a harness exists and commit it with the fix; a small set
   of behavior-pinning tests beats a large redundant suite, and
   assertion count is not a merit signal. Fix causes, not sites, and
   look for a defect's siblings. After the fix works, ask what the next
   change costs, and restructure to data plus one system while context
   is loaded. The last requirement gets the same discipline as the
   first. Leave touched code cleaner; commit no generated artifacts
   unless the repo tracks them.
2. Review lens 3 now prescribes the structural remedy rather than the
   cosmetic one: a bloated signature wants a config object or a
   responsibility split, tangled parsing/transport/policy wants pure
   builders behind a seam with policy left to the caller, and the new
   shape gets named concretely.
3. Review lens 1 now distinguishes validating untrusted data at a trust
   boundary from asserting internal invariants: a missing invariant on a
   state transition is substance, validation that duplicates an earlier
   guarantee is noise.
4. Small diffs get one combined review pass, and a lens may produce no
   findings.

Nothing was removed from the design principles, working rules, SOLID
checklist, pattern cues, or testing checklist.

Checked against the six source notes the skill derives from (the
macro-principles note, TIGER_STYLE, the component-boundaries talk, the
SICP top-down design note, the testing-philosophy note, and the original
skill draft): all seven macro principles keep their one-to-one encoding,
and the draft's deliberate exclusions (line caps, Zig-specific rules,
zero-dependency policy, no-recursion) stay excluded. Several notes got
stronger. The lens 1 sentence is TIGER_STYLE's own position, that
asserts exist for programmer-error invariants rather than re-validating
parsed input, restated for reviewers. "Fix causes, not sites" is the
zero-technical-debt rule in agent form. The structural-remedy sentence
is the component-boundaries red-flag test (one feature touching eight
classes means the boundaries are wrong) turned into a review
prescription. The strongest evidence the style survived: the blinded
judge, which never saw the skill or the notes, described the winning
code in their vocabulary.

## 12. Retrospective and next round

What worked: adversarial critique gated every artifact including the
methodology; frozen keys with per-finding derivations kept every score
auditable; the preregistered rules refused two candidates that looked
good enough to ship on instinct; the holdout answered the
winner's-curse objection; two harness bugs (a verifier bytecode leak
and a scoring-layout error) were caught by reading outputs instead of
trusting exit codes, and all affected results were re-derived.

What did not: sample discipline arrived mid-campaign, so early small-n
claims had to be walked back. The scorer was not blinded to condition.
The judge shares a family with the model under test. Torefall's gate
saturation was discovered only after the case was built, and the
controller case over-corrected into variance. P13 bundled fifteen edits
into one arm, which forced the bisect that a finer first pass would
have avoided.

Queued for the next round: a fresh holdout (catalog-sync spent its gate
role this campaign), an investigation of the text-family false-positive
pattern on bgjobs, higher-n controller runs, and one untested idea from
the 9-rated diffs: a pattern cue for splitting subsystems out of a
growing orchestrator.
