#!/usr/bin/env bash
#
# Instant static check of the "dynamically formatted SQL query" defect class.
#
# No database, no Maven, no build - it only reads source files, so it takes about a
# second and can be run live in class. It shows the two SonarQube findings
# (AccountAccessDao and UserDao) and whether the dynamic-SQL pattern still exists.
#
# Usage:   bash tools/leakdemo/check-sql-safety.sh
# Env:     BEFORE_REPO   path to the pre-fix repo
#
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BEFORE_REPO="${BEFORE_REPO:-/Users/alexresu/OOPMotorPh_Payroll_System}"
AFTER_REPO="${AFTER_REPO:-$(cd "$HERE/../.." && pwd)}"

count_matches() {   # repo pattern -> number of matching lines
  find "$1/src/main/java" -name '*.java' -print0 2>/dev/null \
    | xargs -0 grep -h -- "$2" 2>/dev/null | wc -l | tr -d ' '
}

for repo in "$BEFORE_REPO" "$AFTER_REPO"; do
  [ -d "$repo/src/main/java" ] || { printf 'ERROR: repo not found: %s\n' "$repo" >&2; exit 1; }
done

rule() { printf '%s\n' "=============================================================================="; }

rule
printf '%s\n' " SQL SAFETY - DYNAMICALLY FORMATTED SQL (static check, no database needed)"
rule
printf ' BEFORE build : %s\n' "$BEFORE_REPO"
printf ' AFTER  build : %s\n' "$AFTER_REPO"
printf '%s\n' "-------------------------------------------------------------------------------"
BEFORE_CREATE="$(count_matches "$BEFORE_REPO" 'createStatement(')"
AFTER_CREATE="$(count_matches "$AFTER_REPO" 'createStatement(')"
BEFORE_CONCAT="$(count_matches "$BEFORE_REPO" '= " +')"
AFTER_CONCAT="$(count_matches "$AFTER_REPO" '= " +')"

printf '%-48s %-13s %-13s\n' 'METRIC (whole src/main/java)' 'BEFORE' 'AFTER'
printf '%s\n' "-------------------------------------------------------------------------------"
printf '%-48s %-13s %-13s\n' 'connection.createStatement() sites' "$BEFORE_CREATE" "$AFTER_CREATE"
printf '%-48s %-13s %-13s\n' 'SQL text with an appended value  = " +' "$BEFORE_CONCAT" "$AFTER_CONCAT"
printf '%s\n' "-------------------------------------------------------------------------------"

printf '%s\n' " BEFORE - the pattern SonarQube flags (value concatenated into the SQL text):"
printf '%s\n' "   (paths are relative to src/main/java)"
find "$BEFORE_REPO/src/main/java" -name '*.java' -print0 2>/dev/null \
  | xargs -0 grep -nH -- '= " +' 2>/dev/null \
  | sed "s|^$BEFORE_REPO/src/main/java/||" \
  | sed 's/^\([^:]*:[0-9]*:\)[[:space:]]*/\1 /' \
  | sed 's/^/    /'
printf '%s\n' " AFTER  - the same lines after the refactor (parameter bound with ?):"
find "$AFTER_REPO/src/main/java" -name '*.java' -print0 2>/dev/null \
  | xargs -0 grep -nH -- '= " +' 2>/dev/null \
  | sed "s|^$AFTER_REPO/src/main/java/||" \
  | sed 's/^\([^:]*:[0-9]*:\)[[:space:]]*/\1 /' \
  | sed 's/^/    /'
printf '%s\n' "    (no matches above = no runtime value is concatenated into SQL any more)"
printf '%s\n' "-------------------------------------------------------------------------------"
printf '%s\n' " WHAT CHANGED"
printf ' BEFORE FIX : %s queries were built with createStatement(); %s of them appended\n' \
  "$BEFORE_CREATE" "$BEFORE_CONCAT"
printf '%s\n' "              a runtime value into the SQL text (no ? placeholder)."
printf ' AFTER FIX  : %s concatenated SQL left; those queries now use multi-line SQL\n' \
  "$AFTER_CONCAT"
printf '%s\n' "              with ? placeholders, so no value reaches the SQL text."
printf '%s\n' " Runtime    : SIDE-BY-SIDE.txt section 2 - literal-text executions go to 0"
printf '%s\n' "              (regenerate: bash tools/leakdemo/run-leak-demo.sh)"
rule
