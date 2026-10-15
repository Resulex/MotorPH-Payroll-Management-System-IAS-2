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
printf '%-48s %-13s %-13s\n' 'METRIC (whole src/main/java)' 'BEFORE' 'AFTER'
printf '%s\n' "-------------------------------------------------------------------------------"
printf '%-48s %-13s %-13s\n' 'connection.createStatement() sites' \
  "$(count_matches "$BEFORE_REPO" 'createStatement(')" \
  "$(count_matches "$AFTER_REPO" 'createStatement(')"
printf '%-48s %-13s %-13s\n' 'SQL text with an appended value  = " +' \
  "$(count_matches "$BEFORE_REPO" '= " +')" \
  "$(count_matches "$AFTER_REPO" '= " +')"
printf '%s\n' "-------------------------------------------------------------------------------"

printf '%s\n' " BEFORE - the pattern SonarQube flags (value concatenated into the SQL text):"
find "$BEFORE_REPO/src/main/java" -name '*.java' -print0 2>/dev/null \
  | xargs -0 grep -nH -- '= " +' 2>/dev/null \
  | sed "s|^$BEFORE_REPO/||" \
  | sed 's/^\([^:]*:[0-9]*:\)[[:space:]]*/\1 /' \
  | sed 's/^/    /'
printf '%s\n' " AFTER  - the same lines after the refactor (parameter bound with ?):"
find "$AFTER_REPO/src/main/java" -name '*.java' -print0 2>/dev/null \
  | xargs -0 grep -nH -- '= " +' 2>/dev/null \
  | sed "s|^$AFTER_REPO/||" \
  | sed 's/^\([^:]*:[0-9]*:\)[[:space:]]*/\1 /' \
  | sed 's/^/    /'
printf '%s\n' "    (no matches above = no runtime value is concatenated into SQL any more)"
printf '%s\n' "-------------------------------------------------------------------------------"
printf '%s\n' " SAY THIS: SonarQube reports these as dynamic-SQL hotspots at the lines that"
printf '%s\n' " execute the statement - AccountAccessDao.java:90 and UserDao.java:288 in the"
printf '%s\n' " pre-fix build. Both values were ints, so they were never exploitable through"
printf '%s\n' " those signatures; the fix removes the pattern (and the risk of it becoming"
printf '%s\n' " reachable) by binding the parameter instead of building the SQL string."
printf '%s\n' " The runtime half of the evidence is in reports/SIDE-BY-SIDE.txt section 2 -"
printf '%s\n' " run bash tools/leakdemo/run-leak-demo.sh to regenerate it."
rule
