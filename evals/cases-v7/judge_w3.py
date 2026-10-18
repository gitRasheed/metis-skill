"""Wave 3 judging with GPT-6 Astra through opencode: one blind judge per A/B3 pair, one blind scorer per review case.

usage: judge_w3.py <runs_root> <out_dir> <cases_v7_dir> build|run|collect
"""
import json
import os
import random
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor

runs_root, out, here, step = sys.argv[1:5]
MODEL_DIR = "openai_gpt-6.1-sol_high"
REPS = range(11, 17)
CODE = {"J1": "J1-pricing-feature", "J2": "J2-ingest-dedupe-bug", "P1": "P1-habits-rename"}
REVIEW = {"R1": "api-evolution-review", "R2": "background-jobs-review"}
SKILL_LINE = re.compile(r"metis|\bskill\b", re.I)
DENY = {"$schema": "https://opencode.ai/config.json",
        "permission": {"skill": {k: "deny" for k in ("metis", "deslop", "unslop", "humanizer", "grilling", "catchup", "wrap",
                                                     "overnight", "morning", "import-memory", "skill-creator", "report", "deep-research")}}}
PAIR_BRIEF = """You are a senior engineer comparing two independent changes, FIRST and SECOND, that different engineers made to the same repository for the same task (task.md). Both pass the hidden acceptance tests, so judge quality, not correctness. Read only task.md and changes.md in this directory.

Answer two questions:
1. Which codebase would you rather inherit and maintain for a year in a large team? Weigh readability, how cheap the next feature in this area is, fit with the repo's existing conventions, and test quality.
2. How noisy or pedantic is each change, from 1 (clean) to 5 (very noisy)? Count comments that narrate or cite principles, pointless defensive checks or assertions, ceremony, scope creep and over-engineering against it. Don't count necessary tests or a short README update the task implies.

Your final message must end with exactly these two lines:
inherit: FIRST | SECOND | TIE
noise: FIRST=<1-5> SECOND=<1-5>
"""
SCORE_BRIEF = """You are a blind assessor scoring code reviews against a frozen answer key. Read only prompt.md (the PR the reviewers saw), scoring.md (the frozen key and its rules; follow them exactly, including partial credit, severity handling, false-positive penalties and compound-finding rules) and reviews.md (reviews labelled Z01 to Z12) in this directory.

For each review, map each finding to a key item, a false positive (with its penalty) or neutral, then total it under the key's scheme. Apply the same credit decision to the same substance across reviews.

Your final message must end with one line per review, exactly in this form:
SCORE Z01 <total>
...
SCORE Z12 <total>
"""


def clean(text):
    return "\n".join(line for line in text.splitlines() if not SKILL_LINE.search(line))


def git_init(d):
    # opencode resolves the project root from git; without a repo it reads the wrong directory.
    for cmd in (["git", "init", "-q", "."], ["git", "add", "-A"], ["git", "-c", "user.name=j", "-c", "user.email=j@e", "commit", "-qm", "packet"]):
        subprocess.run(cmd, cwd=d, check=True)


def build():
    rng = random.Random(3033)
    os.makedirs(f"{out}/_maps", exist_ok=True)
    for case, folder in CODE.items():
        for rep in REPS:
            pair = [f"{MODEL_DIR}/{case}/v132-r{rep}", f"{MODEL_DIR}/{case}/b3-r{rep}"]
            rng.shuffle(pair)
            d = f"{out}/{case}-p{rep}"
            os.makedirs(d, exist_ok=True)
            body = []
            for label, run in zip(("FIRST", "SECOND"), pair):
                diff = "\n".join(l for l in open(f"{runs_root}/{run}/diff.patch").read().splitlines()
                                 if not (SKILL_LINE.search(l) and l.startswith(("+", "-"))))
                body.append(f"## {label}\n\n```diff\n{diff}\n```\n")
            open(f"{d}/changes.md", "w").write("\n".join(body))
            open(f"{d}/task.md", "w").write(open(f"{here}/{folder}/prompt.md").read())
            open(f"{d}/brief.md", "w").write(PAIR_BRIEF)
            json.dump(DENY, open(f"{d}/opencode.json", "w"))
            json.dump({"FIRST": pair[0], "SECOND": pair[1]}, open(f"{out}/_maps/{case}-p{rep}.json", "w"))
            git_init(d)
    for case, folder in REVIEW.items():
        runs = [f"{MODEL_DIR}/{case}/{arm}-r{rep}" for arm in ("v132", "b3") for rep in REPS]
        rng.shuffle(runs)
        d = f"{out}/{case}"
        os.makedirs(d, exist_ok=True)
        mapping, parts = {}, []
        for i, run in enumerate(runs, 1):
            label = f"Z{i:02d}"
            mapping[label] = run
            parts.append(f"## Review {label}\n\n{clean(open(f'{runs_root}/{run}/final.md').read())}\n")
        open(f"{d}/reviews.md", "w").write("\n".join(parts))
        for name in ("prompt.md", "scoring.md"):
            open(f"{d}/{name}", "w").write(open(f"{here}/../cases-v5/{folder}/{name}").read())
        open(f"{d}/brief.md", "w").write(SCORE_BRIEF)
        json.dump(DENY, open(f"{d}/opencode.json", "w"))
        json.dump(mapping, open(f"{out}/_maps/{case}.json", "w"))
        git_init(d)


def judge(d):
    if os.path.exists(f"{d}/answer.md") and os.path.getsize(f"{d}/answer.md") > 0:
        return
    with open(f"{d}/events.jsonl", "w") as events:
        subprocess.run(["opencode", "run", "--auto", "-m", "openai/gpt-6-astra#high", "--format", "json",
                        open(f"{d}/brief.md").read()], cwd=d, env={**os.environ, "PWD": d}, stdin=subprocess.DEVNULL, stdout=events,
                       stderr=subprocess.DEVNULL, timeout=2400)
    subprocess.run([sys.executable, f"{here}/extract.py", d])
    os.replace(f"{d}/final.md", f"{d}/answer.md")


def run():
    dirs = sorted(f"{out}/{x}" for x in os.listdir(out) if not x.startswith("_") and os.path.isdir(f"{out}/{x}"))
    with ThreadPoolExecutor(max_workers=6) as pool:
        list(pool.map(judge, dirs))


def collect():
    result = {"pairs": [], "reviews": {}}
    for name in sorted(os.listdir(out)):
        d = f"{out}/{name}"
        if name.startswith("_") or not os.path.isdir(d) or not os.path.exists(f"{d}/answer.md"):
            continue
        text = open(f"{d}/answer.md").read()
        mapping = json.load(open(f"{out}/_maps/{name}.json"))
        if name in REVIEW:
            scores = {lab: float(v) for lab, v in re.findall(r"SCORE (Z\d\d) (-?[\d.]+)", text)}
            result["reviews"][name] = {mapping[lab]: score for lab, score in scores.items()}
            continue
        inherit = re.findall(r"inherit:\s*(FIRST|SECOND|TIE)", text)
        noise = re.findall(r"noise:\s*FIRST=(\d)\s*SECOND=(\d)", text)
        arm = {lab: mapping[lab].split("/")[-1].split("-")[0] for lab in ("FIRST", "SECOND")}
        win = inherit[-1] if inherit else None
        result["pairs"].append({"pair": name, "winner_arm": "tie" if win == "TIE" else (arm[win] if win else None),
                                "noise": {arm["FIRST"]: int(noise[-1][0]), arm["SECOND"]: int(noise[-1][1])} if noise else None})
    json.dump(result, open(f"{out}/results.json", "w"), indent=1)
    print(json.dumps(result, indent=1))


{"build": build, "run": run, "collect": collect}[step]()
