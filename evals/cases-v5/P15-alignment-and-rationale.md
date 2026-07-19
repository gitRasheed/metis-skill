# P15: before/after, why it wins, and vault alignment

## 1. The complete before → after (every changed line vs `skills/metis/SKILL.md`)

P15-production is +14/−5 on 174 lines. The entire delta:

**Added — "Implementation rules (when implementing or fixing)"** (new section,
5 rules, from the P8/P10 lineage that won every agentic instrument):

1. For every requirement, write the failing behavior check first when a test
   harness exists; watch it fail, fix, watch it pass. Commit the check with
   the fix — a fix without a guarding test is half done. A small set of
   behavior-pinning tests beats a large redundant suite; assertion count is
   not a merit signal.
2. Fix causes, not sites: when two symptoms share a root, restructure the
   root; when a defect class exists once, look for its siblings.
3. Optimize for the next change: if your structure makes the next feature
   expensive (touching many branches or classes), restructure to data plus
   one system now, while context is loaded.
4. Long task lists do not suspend quality.
5. Leave the campsite cleaner; never commit generated artifacts.

**Added — lens 3 structural-remedy sentence** (from P9): for every surface
defect, prescribe the STRUCTURAL remedy — a bloated signature wants a config
object or split, not keyword-only markers; parsing+shaping+transport+policy
want separation (pure builders, an I/O port, caller-owned policy); a
hard-wired dependency wants a seam. Name the new shape concretely.

**Added — review-mode intro sentence** (#17): small diff → one careful
combined pass; large diff → one pass per lens.

**Added — lens 2–4 tail** (#20): a lens may legitimately produce no
findings; do not manufacture one to fill a category.

**Added — lens 1 validate-vs-assert sentence** (the P15 novelty): distinguish
validation of untrusted data at a trust boundary from assertions of internal
invariants whose failure means a programming error — missing internal-
invariant protection on state transitions and cross-record contracts is
report-worthy substance; validation that duplicates what a parser, type, or
earlier boundary already guarantees is noise.

**Removed:** nothing from the design principles, working rules, SOLID,
pattern cues, testing checklist, or verification checklist. The 5 deletions
are the phase-map line update and sentence splices for the insertions.

## 2. Why this performs better as an LLM prompt (measured, not argued)

- **Frontier models already have taste; they lack process.** The cold
  baseline finds most blocking bugs but commits ZERO tests in all five
  agentic calibrations across three cases, probing with throwaway scripts
  instead. Implementation rule 1 converts that testing instinct into
  committed behavior-pinning suites (9–16 tests per run, zero narrating
  comments) — the single feature blinded judges cite most in wins.
- **Placement is the mechanism.** Sixteen texts established that skill
  content taxes whichever phase holds it: review guidance diffused through
  implementer-held sections regressed agentic architecture 0–2 in two
  independent arms; the same content scoped inside the review lens lifted
  review MORE (21.5 vs 18.6) at zero agentic cost, because the implementing
  agent never holds the sentence. P15 is the only text of sixteen built to
  respect that mechanism everywhere, and the only one to pass every
  preregistered gate.
- **The review lens now buys signal, not volume.** The validate-vs-assert
  sentence tells the reviewer what counts as substance (missing invariant
  protection) versus noise (duplicate validation). Effect: zero false
  positives in six consecutive scored transcripts, both S7 vacuous-test
  halves found consistently, and the campaign's first single findings to
  merge B9 ownership-fencing with B3 atomicity correctly.

## 3. Vault alignment: nothing weakened, several things strengthened

Read against the six provenance notes (`Macro-Programming Philosophies`,
`🐯 TigerBeetle Style Principles`, `The 35-Year OOP Mistake`, `Wishful
Thinking Top-Down API Design`, `Testing Philosophy — Procedural Over
Table-Driven`, `Skill Draft — Coding Philosophy Skill`):

**Untouched.** All seven points of the Macro master note keep their 1:1
encoding: call-site-first (SICP note, `register_user` intact), plain data +
systems and unions-over-inheritance (Muratori note, Trade example intact),
ifs-up-fors-down, boundary asserts positive AND negative space, bottleneck
order, minimal-test-logic. The Skill Draft note's deliberate exclusions
(70-line caps, Zig rules, zero-dependency, no-recursion) remain excluded.

**Strengthened, with the note it strengthens:**

- *TigerBeetle — assertion discipline.* The lens-1 sentence is TIGER_STYLE's
  own distinction made explicit for reviewers: TigerBeetle asserts are about
  programmer-error invariants (function contracts, state transitions,
  pre/postconditions) — not about re-validating what a parser already
  guaranteed. "Missing internal-invariant protection on state transitions is
  report-worthy substance" is the review-mode form of "minimum 2 assertions
  per function / assert positive and negative space"; "duplicate validation
  is noise" is the review-mode form of the vault's POCPOU / single-source-of-
  truth rule. The reviewer now demands MORE TigerBeetle, more precisely.
- *TigerBeetle — zero technical debt.* "Fix causes, not sites" and "look for
  the defect's siblings" operationalize "do it right the first time" for an
  agent that would otherwise patch symptoms.
- *35-Year OOP — the red-flag test.* "If adding one feature requires touching
  8 parent classes, your boundaries are wrong" is now an implementation rule
  ("restructure to data plus one system") AND a review prescription (the
  structural-remedy sentence). Judges' win rationales quote it back
  unprompted: "data-driven class model, ability dispatch table… make future
  changes more localized."
- *TigerBeetle — options-struct signatures.* The lens-3 config-object remedy
  is the named-arguments pattern from TIGER_STYLE applied at review time.
- *Testing Philosophy.* The anti-assert-spam sentence enforces "tests are
  code: minimize test logic" against the failure mode LLMs actually have
  (spraying asserts to look thorough). Measured: P15 suites are smaller and
  better-targeted than P13's (48–52 asserts vs 68) and judged stronger.

**The blinded-judge check.** The quality judge — which never saw the skill,
only anonymized diffs — describes P15's winning code in the vault's own
vocabulary: "data-driven class model, ability dispatch table, explicit
encounter state" (plain data + systems), "enforces stronger boundary
contracts, replay safety, atomicity, and gold-conservation invariants"
(TigerBeetle asserts), "tests more consistently exercise observable public
behavior" (integration-over-unit). The style the vault encodes is what wins
blind comparisons.
