# Eval Runbook

Use this runbook for three-way local evaluations.

## Setup

1. Pick one case from `cases-v3/`.
2. Create a git-excluded output folder such as `eval-runs/2026-06-06/cases-v3/gpt-5.5-xhigh/<case>/`.
3. Run three isolated agents:
   - Baseline: case prompt only.
   - Metis core: same case prompt plus the Metis skill, without optional examples.
   - Metis examples: same case prompt plus the Metis skill and `references/examples.md`.
4. Save raw outputs for each condition.
5. Score with `rubric.md`.
6. Save scoring notes as `scores.md`.

## Agent Prompt Wrapper

Use the exact case prompt. Add only this wrapper:

```text
Do not edit files. Return your answer in Markdown. If you would normally create files or run commands, describe exactly what you would do and what evidence would prove it worked.
```

For Metis examples, provide only the examples reference under test. Do not expose scoring notes or expected answers.

## Scoring Notes

Keep scoring grounded in observable output. Do not reward a response for merely mentioning a principle if it does not apply the principle to the task.

Prefer pairwise comparisons when total scores are close.

## Reporting

A useful result summary includes:

- case name
- baseline score
- Metis core score
- Metis examples score
- winner or pairwise verdict
- behavior differences
- failure modes still present
- efficiency metadata availability
