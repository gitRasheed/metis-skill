#!/bin/bash
# run.sh <case> <arm> <model> <rep>
#   case: R1 R2 J1 J2 P1    arm: v132 v140    model: claude-opus-5-5 | openai/gpt-6.1-sol#high
# Workspaces live outside the skill repo so agents cannot wander into it.
set -u
CASE=$1; ARM=$2; MODEL=$3; REP=$4
HERE=$(cd "$(dirname "$0")" && pwd)
V7=${V7:-/tmp/claude-1000/-mnt-c-Users-rashe-Documents-Projects-AI-LLM-metis-skill/632d9e09-711a-4d88-b3a2-0728b65d2123/scratchpad/V7}
case $CASE in
  R1) SRC=$HERE/../cases-v5/api-evolution-review; KIND=review ;;
  R2) SRC=$HERE/../cases-v5/background-jobs-review; KIND=review ;;
  J1) SRC=$HERE/J1-pricing-feature; KIND=code ;;
  J2) SRC=$HERE/J2-ingest-dedupe-bug; KIND=code ;;
  P1) SRC=$HERE/P1-habits-rename; KIND=code ;;
esac
TAG=$(echo "$MODEL" | tr '/#' '__')
R=$V7/runs/$TAG/$CASE/$ARM-r$REP
rm -rf "$R"; mkdir -p "$R/ws"
if [ $KIND = code ]; then rsync -a --exclude .git "$SRC/repo/" "$R/ws/"; fi
cd "$R/ws"
git init -q . && git add -A && git -c user.name=dev -c user.email=dev@example.com commit -qm initial --allow-empty
BASE=$(git rev-parse HEAD)
printf '.claude/\n.opencode/\nopencode.json\n__pycache__/\n.pytest_cache/\n*.pyc\nbuild/\n' >> .git/info/exclude
SUFFIX="You are working autonomously; nobody will answer questions. When you are done, summarize what you changed and how you verified it."
[ $KIND = review ] && SUFFIX="You are working autonomously; nobody will answer questions. Give your complete review as your final message."
PROMPT="Use the metis skill for this task.

$(cat "$SRC/prompt.md")

$SUFFIX"
export PATH=$V7/venv/bin:$PATH
START=$(date +%s)
case "$MODEL" in
  claude-*)
    mkdir -p .claude/skills && cp -r "$V7/arms/$ARM/metis" .claude/skills/metis
    CFG=$R/cfg; mkdir -p "$CFG"; cp "$V7/cfgbase/.credentials.json" "$CFG/"
    CLAUDE_CONFIG_DIR=$CFG timeout 2700 claude -p --model "$MODEL" --permission-mode bypassPermissions \
      --no-session-persistence --output-format stream-json --verbose "$PROMPT" < /dev/null > "$R/events.jsonl" 2> "$R/stderr.log"
    rm -rf "$CFG"
    ;;
  openai/*)
    mkdir -p .opencode/skills && cp -r "$V7/arms/$ARM/metis" .opencode/skills/metis
    DENY='"deslop":"deny","unslop":"deny","humanizer":"deny","grilling":"deny","catchup":"deny","wrap":"deny","overnight":"deny","morning":"deny","import-memory":"deny","skill-creator":"deny","report":"deny","deep-research":"deny"'
    echo "{\"\$schema\":\"https://opencode.ai/config.json\",\"permission\":{\"skill\":{$DENY}}}" > opencode.json
    timeout 2700 opencode run --auto -m "$MODEL" --format json "$PROMPT" < /dev/null > "$R/events.jsonl" 2> "$R/stderr.log"
    ;;
esac
echo $(( $(date +%s) - START )) > "$R/seconds"
git add -A >/dev/null 2>&1; git diff --cached "$BASE" -- . > "$R/diff.patch"; git reset -q
python3 "$HERE/extract.py" "$R"
echo "DONE $TAG $CASE $ARM r$REP $(cat "$R/seconds")s"
