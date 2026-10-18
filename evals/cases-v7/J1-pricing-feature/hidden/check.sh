#!/usr/bin/env bash
# Usage: check.sh <workspace>   (the agent's finished copy of repo/)
# Exit 0 only if everything compiles, the workspace suite reports at least 9 passing tests and 0 failed,
# and every hidden check reports a pass in a summary carrying this run's token.
set -euo pipefail

WS="$(cd "${1:?usage: check.sh <workspace>}" && pwd)"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

MIN_WORKSPACE_TESTS=9
EXPECTED_HIDDEN=$(grep -c '^ *@Check$' "$HERE/java/hiddencheck/PricingHiddenChecks.java")
TOKEN="$(od -An -N8 -tx1 /dev/urandom | tr -d ' \n')"

fail() { echo "CHECK FAILED: $*"; exit 1; }

mkdir -p "$WORK/classes"
{
  find "$WS/src/main/java" "$WS/src/test/java" -name '*.java'
  find "$HERE/java" -name '*.java'
} > "$WORK/sources.txt"

echo "== compile"
javac -d "$WORK/classes" @"$WORK/sources.txt" || fail "compilation"

echo "== workspace test suite"
(cd "$WS" && java -cp "$WORK/classes" com.acme.pricing.testing.TestRunner "$WORK/classes" ) | tee "$WORK/workspace.log" || fail "workspace suite exit code"
summary="$(tail -n 1 "$WORK/workspace.log")"
[[ "$summary" =~ ^([0-9]+)\ passed,\ 0\ failed$ ]] || fail "workspace suite summary missing or has failures"
ran="${BASH_REMATCH[1]}"
(( ran >= MIN_WORKSPACE_TESTS )) || fail "workspace suite ran $ran tests, expected at least $MIN_WORKSPACE_TESTS"
(( $(grep -c '^PASS ' "$WORK/workspace.log") == ran )) || fail "workspace PASS lines do not match summary"

echo "== hidden checks"
java -cp "$WORK/classes" hiddencheck.HiddenRunner "$TOKEN" hiddencheck.PricingHiddenChecks | tee "$WORK/hidden.log" || fail "hidden exit code"
[[ "$(tail -n 1 "$WORK/hidden.log")" == "hidden: $EXPECTED_HIDDEN passed, 0 failed, token $TOKEN" ]] \
  || fail "hidden summary missing, wrong count (expected $EXPECTED_HIDDEN) or failures"

echo "CHECK PASSED"
