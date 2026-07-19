# cases-v6: the parse-don't-validate round

One overnight campaign (2026-07-19), two preregistered waves, six skill
texts. This round shipped Metis v1.3.0. `REPORT.md` holds the full
record; this file is the index.

## What was tested

Four candidate texts against the shipped v1.2.0 and a no-skill
baseline: a parse-don't-validate unification that also shortened the
skill (CONC), a book-concepts enrichment drawn from Wlaschin, Ousterhout
and Normand (RICH), and two wave-2 isolation arms that split the
difference (CLITE, SYNTH). The frozen arm texts are in `arms/`; each ran
with the production `references/` directory from `skills/metis/`.

## How this round differed from earlier ones

- Runs shipped the full skill directory and the prompt only mentioned
  that references exist. Every run's event stream was scanned for
  reference reads, token usage, and command counts. Result: references
  were read in 54 of 54 skill runs, in every mode.
- Review transcripts were scored by one sub-agent per transcript
  against the frozen v5.1.1 background-jobs key, then adjudicated by
  hand: every uncertain item resolved, penalties checked against the
  key, quotes verified against transcripts.
- An external leg: six design-heavy Python exercises from the Aider
  polyglot benchmark (commit `7e0611e` of Aider-AI/polyglot-benchmark,
  canonical solutions stripped before runs). All 72 solution runs
  passed the frozen tests, so the external signal is pure code quality,
  judged blind and order-randomized against the shipped text's
  solutions.
- Ship rules with numeric thresholds were journaled before each wave's
  scoring. Wave 1 refused CONC (review-depth regression); wave 2
  isolated the cause to its deletions and passed SYNTH.

## Results in one paragraph

SYNTH (v1.2.0 plus the parse-don't-validate rewrite and four
book-derived rules, deleting nothing) passed every gate: review parity
with the shipped text, 19 of 19 hidden verifier gates in all runs,
cheaper next-feature extensions (257 vs 300 changed lines), and a
blinded taste majority (8-5-1 pooled). The external headline: on public
benchmark tasks that every arm passes, the blinded judge preferred
Metis-guided code over baseline 9 to 3. The structural lesson repeated
from v5, sharpened: rewording that deepens a principle is free;
deleting phase-relevant text costs that phase.

## What is committed here

`REPORT.md` (full campaign record) and `arms/` (the four candidate
texts). Raw run output, judge transcripts, and score sheets stay out of
git, as in every round; scores are re-derivable from the frozen keys
and the report's derivation notes.
