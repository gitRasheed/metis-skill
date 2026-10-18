"""Build blinded packets: review sets per case (R1, R2) and diff sets per case and model (J1, J2, P1).

Labels are shuffled; the label-to-run mapping goes to <out>/_maps/ and is never shown to a judge.
"""
import json
import os
import random
import re
import sys

runs_root, out = sys.argv[1], sys.argv[2]
MODELS = ["claude-opus-5-5", "openai_gpt-6.1-sol_high"]
SKILL_LINE = re.compile(r"metis|\bskill\b", re.I)
rng = random.Random(int(os.environ.get("SEED","1018")))
os.makedirs(f"{out}/_maps", exist_ok=True)


def runs_for(model, case):
    base = f"{runs_root}/{model}/{case}"
    return sorted(f"{base}/{d}" for d in os.listdir(base)) if os.path.isdir(base) else []


def clean(text):
    return "\n".join(line for line in text.splitlines() if not SKILL_LINE.search(line))


for case in ("R1", "R2"):
    items = [r for m in MODELS for r in runs_for(m, case)]
    rng.shuffle(items)
    mapping, parts = {}, []
    for i, run in enumerate(items, 1):
        label = f"X{i:02d}"
        mapping[label] = run.split(f"{runs_root}/")[1]
        parts.append(f"## Review {label}\n\n{clean(open(f'{run}/final.md').read())}\n")
    os.makedirs(f"{out}/{case}", exist_ok=True)
    open(f"{out}/{case}/reviews.md", "w").write("\n".join(parts))
    json.dump(mapping, open(f"{out}/_maps/{case}.json", "w"), indent=1)

for case in ("J1", "J2", "P1"):
    for model in MODELS:
        items = runs_for(model, case)
        rng.shuffle(items)
        mapping, parts = {}, []
        for i, run in enumerate(items):
            label = "ABCD"[i]
            mapping[label] = run.split(f"{runs_root}/")[1]
            diff = "\n".join(l for l in open(f"{run}/diff.patch").read().splitlines() if not SKILL_LINE.search(l) or not l.startswith(("+", "-")))
            parts.append(f"## Change {label}\n\n```diff\n{diff}\n```\n")
        tag = f"{case}-{'opus' if 'opus' in model else 'sol'}"
        os.makedirs(f"{out}/{tag}", exist_ok=True)
        open(f"{out}/{tag}/changes.md", "w").write("\n".join(parts))
        json.dump(mapping, open(f"{out}/_maps/{tag}.json", "w"), indent=1)
print("ok")
