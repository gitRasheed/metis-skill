"""Noise scan over one run's diff.patch: rule citations, added comments and asserts, removed interfaces, files touched."""
import json
import re
import sys

CITATION = re.compile(r"metis|\blens \d|\bdesign \d+|parse,? don'?t validate|ifs up|fors down|trace invariants|check-then-act|"
                      r"plain data|call-site|wishful thinking|deep module", re.I)
COMMENT = re.compile(r"^\+\s*(//|#(?!!)|/\*|\*\s|\"\"\"|''')")
ASSERT = re.compile(r"^\+.*\b(assert\b|Objects\.requireNonNull|Preconditions\.check|checkState|checkArgument)")


def scan(diff_text):
    files, added_lines, comment_lines, asserts, citations, removed_types = [], 0, 0, 0, [], []
    current = None
    for line in diff_text.splitlines():
        if line.startswith("diff --git"):
            current = line.split(" b/")[-1]
            files.append(current)
            continue
        if line.startswith("+++") or line.startswith("---"):
            continue
        if line.startswith("-") and re.search(r"\b(interface|class)\s+\w+(Impl)?\b", line) and re.search(r"interface\s|Impl\b", line):
            removed_types.append(f"{current}: {line[1:].strip()[:80]}")
        if not line.startswith("+"):
            continue
        added_lines += 1
        if COMMENT.match(line):
            comment_lines += 1
            if CITATION.search(line):
                citations.append(f"{current}: {line[1:].strip()[:100]}")
        if ASSERT.match(line):
            asserts += 1
    return {"files": files, "added_lines": added_lines, "comment_lines": comment_lines, "asserts": asserts,
            "citations_in_comments": citations, "removed_interface_or_impl": removed_types}


if __name__ == "__main__":
    print(json.dumps(scan(open(sys.argv[1]).read()), indent=1))
