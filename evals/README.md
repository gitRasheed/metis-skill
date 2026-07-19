# Metis Evals

Maintainer materials, not loaded by the skill at runtime. Every
non-trivial change to Metis earns its place here before it ships: the
candidate text runs isolated A/B against the shipped text, under ship
rules with numeric thresholds journaled before any scoring, and
candidates that fail their gates are refused. More texts have been
refused than shipped.

## The method, in short

- Preregistered gates: pass/fail thresholds written down before the
  runs they judge. They have refused strong-looking candidates,
  including one that won its blinded quality comparison but regressed
  review depth.
- Frozen scoring keys for review cases, with per-finding credit rules,
  depth markers, and false-positive traps. Hand-derived score sheets
  are committed so any number can be re-derived.
- Hidden verifiers for agentic cases, injected only after the run;
  workspaces are scanned so no key or compiled artifact leaks to the
  agent under test.
- Blinded, order-randomized pairwise quality judging ("which codebase
  would you rather inherit"), with skill-identifying lines stripped
  from the diffs.
- Adversarial critique gates during case authoring: a cold model
  attacks each case before it freezes. This caught false seeds, wrong
  formulas, and traps that would have punished correct findings.
- Honest reporting: parity results, walked-back small-n claims, and
  known limits are in the round reports next to the wins.

## Rounds

| Round | What it asked | Outcome |
|---|---|---|
| [cases-v3](cases-v3/) | Does the skill beat a bare model at all? | First baseline-vs-Metis A/Bs; rubric era |
| [cases-v4](cases-v4/) | Review cases with frozen keys | One case later served as an untouched holdout for v5 |
| [cases-v5](cases-v5/) | Which exact skill text is best? | Sixteen permutations, five cases, about ninety hand-scored runs; found that guidance placement, not content volume, drives interference; shipped v1.2.0. [FINAL-REPORT.md](cases-v5/FINAL-REPORT.md) |
| [cases-v6](cases-v6/) | Parse-don't-validate, book-derived rules, and external validity | Six texts, two preregistered waves, about 195 runs; per-transcript sub-agent scoring adjudicated by hand; external leg on the Aider polyglot benchmark where every arm passed all 72 test runs and a blinded judge still preferred Metis code 3:1 over baseline; shipped v1.3.0. [REPORT.md](cases-v6/REPORT.md) |

Across the last two rounds, twenty-two skill texts were tested and two
shipped.

## What is committed

Case prompts, frozen scoring keys, verifiers, per-finding rescore
derivations, the frozen candidate texts, and the round reports. Raw
agent transcripts and run artifacts stay out of git; scores are
re-derivable from what is committed.
