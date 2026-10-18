#!/bin/bash
# check.sh <workspace> [python]: runs the repo's tests plus the hidden tests on a copy
set -u
WS=$1; PY=${2:-python3}; H=$(cd "$(dirname "$0")" && pwd)
T=$(mktemp -d); rsync -a --exclude .git --exclude .claude --exclude .opencode "$WS"/ "$T"/
cp "$H/test_hidden.py" "$T/tests/test_zz_hidden.py"
cd "$T" && "$PY" -m pytest -q -p no:cacheprovider tests > "$T/out.txt" 2>&1; rc=$?
tail -1 "$T/out.txt"; rm -rf "$T"; exit $rc
