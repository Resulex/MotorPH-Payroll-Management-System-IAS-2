#!/usr/bin/env bash
#
# Before/after demonstration of the PreparedStatement / ResultSet resource leak
# fixed by commit 5785fa0 ("refactor: move preparedstatement to prevent resource leak").
#
# It builds both repositories, compiles the instrumentation harness once, then runs
# the SAME harness and SAME workload twice - once against the pre-fix build
# (OOPMotorPh_Payroll_System) and once against the post-fix build
# (MotorPH-Payroll-Management-System-IAS-2) - and prints a side-by-side delta.
#
# No production source file in either repository is modified.
#
# Usage:   tools/leakdemo/run-leak-demo.sh
# Env:     BEFORE_REPO, MYSQL_URL, MYSQL_USER, MYSQL_PASS, ITERATIONS
#
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AFTER_REPO="${AFTER_REPO:-$(cd "$HERE/../.." && pwd)}"
BEFORE_REPO="${BEFORE_REPO:-/Users/alexresu/OOPMotorPh_Payroll_System}"

OUT="$HERE/out"
REPORTS="$HERE/reports"

MYSQL_URL="${MYSQL_URL:-jdbc:mysql://localhost:3306/payrollsystem_db_aoop}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-Alex123!!}"
ITERATIONS="${ITERATIONS:-100}"

export MYSQL_URL MYSQL_USER MYSQL_PASS

say() { printf '%s\n' "$*"; }
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

command -v mvn >/dev/null 2>&1 || fail "mvn is not on PATH"
[ -d "$BEFORE_REPO/src/main/java" ] || fail "BEFORE repo not found: $BEFORE_REPO (override with BEFORE_REPO=...)"
[ -d "$AFTER_REPO/src/main/java" ] || fail "AFTER repo not found: $AFTER_REPO"

# The projects target Java 21, but the javac on PATH may be older, and
# /usr/libexec/java_home -v 21 silently falls back to an older JDK when 21 is not
# installed (it answers with JDK 17 on this machine) - so the version is verified.
resolve_jdk() {
  local candidate="" major=""
  # 1. explicit override
  if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then printf '%s' "$JAVA_HOME"; return; fi
  # 2. the JDK Maven itself runs on: guaranteed to read the class files it produced
  candidate="$(mvn -version 2>/dev/null | sed -n 's/.*runtime: *//p' | head -1)"
  if [ -n "$candidate" ] && [ -x "$candidate/bin/javac" ]; then printf '%s' "$candidate"; return; fi
  # 3. any JDK that actually reports 21 or newer
  candidate="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
  if [ -n "$candidate" ] && [ -x "$candidate/bin/javac" ]; then
    major="$("$candidate/bin/javac" -version 2>&1 | sed -n 's/^javac \([0-9]*\).*/\1/p')"
    if [ -n "$major" ] && [ "$major" -ge 21 ]; then printf '%s' "$candidate"; return; fi
  fi
  printf '%s' ""
}

JDK_HOME="$(resolve_jdk)"
if [ -n "$JDK_HOME" ]; then
  JAVAC="$JDK_HOME/bin/javac"
  JAVA_CMD="$JDK_HOME/bin/java"
else
  command -v javac >/dev/null 2>&1 || fail "no usable JDK found (set JAVA_HOME)"
  JAVAC="$(command -v javac)"
  JAVA_CMD="$(command -v java)"
fi
say "==> using JDK: $("$JAVAC" -version 2>&1)"

mkdir -p "$OUT" "$REPORTS"

# ----------------------------------------------------------------- 1. build both
build_project() {   # name repo_dir
  local name="$1" repo="$2"
  say "==> [$name] maven compile + dependency:build-classpath"
  ( cd "$repo" && mvn -q -DskipTests compile \
      dependency:build-classpath -Dmdep.outputFile="$OUT/cp-$name.txt" ) \
    || fail "maven build failed for $name ($repo)"
  [ -s "$OUT/cp-$name.txt" ] || fail "maven did not produce $OUT/cp-$name.txt"
}

build_project before "$BEFORE_REPO"
build_project after  "$AFTER_REPO"

# ----------------------------------------------------------------- 2. compile harness
# The harness is compiled against the post-fix classes only: every DAO method it
# calls has an identical signature in both builds, (HRPersonnelDao.loadSummaryReport
# is invoked reflectively because its return type changed), so one compiled harness
# can be executed against either build.
say "==> compiling harness"
rm -rf "$OUT/classes"
mkdir -p "$OUT/classes"
find "$HERE/src" -name '*.java' -print0 \
  | xargs -0 "$JAVAC" -cp "$AFTER_REPO/target/classes" -d "$OUT/classes" \
  || fail "harness compilation failed"

# ----------------------------------------------------------------- 3. run both
run_one() {   # label repo_dir
  local label="$1" repo="$2"
  local cp="$OUT/classes:$repo/target/classes:$(cat "$OUT/cp-$label.txt")"
  say "==> [$label] running workload ($ITERATIONS iterations per DAO call)"
  "$JAVA_CMD" -cp "$cp" demo.LeakDemo \
      --label "$label" \
      --build "$repo" \
      --iterations "$ITERATIONS" \
      --url "$MYSQL_URL" \
      --out "$REPORTS/$label.txt" \
      > "$REPORTS/$label.raw.log" 2>&1
  if [ ! -s "$REPORTS/$label.txt" ]; then
    tail -n 30 "$REPORTS/$label.raw.log" >&2
    fail "$label run did not produce a report"
  fi
}

run_one before "$BEFORE_REPO"
run_one after  "$AFTER_REPO"

# ----------------------------------------------------------------- 4. static metrics
# Whole-repo counts straight out of the source, no database involved. These are the
# same numbers a reviewer checks against the SonarQube findings.
count_java() {   # repo pattern
  find "$1/src/main/java" -name '*.java' -print0 2>/dev/null \
    | xargs -0 grep -h -- "$2" 2>/dev/null | wc -l | tr -d ' '
}

BEFORE_CREATE="$(count_java "$BEFORE_REPO" 'createStatement(')"
AFTER_CREATE="$(count_java "$AFTER_REPO" 'createStatement(')"
BEFORE_CONCAT="$(count_java "$BEFORE_REPO" '= " +')"
AFTER_CONCAT="$(count_java "$AFTER_REPO" '= " +')"

# ----------------------------------------------------------------- 5. side by side
SIDE="$REPORTS/SIDE-BY-SIDE.txt"
{
  printf '%s\n' "=============================================================================="
  printf '%s\n' " BEFORE vs AFTER - JDBC RESOURCE + SQL SAFETY (commit 5785fa0)"
  printf '%s\n' "=============================================================================="
  printf ' BEFORE build : %s\n' "$BEFORE_REPO"
  printf ' AFTER  build : %s\n' "$AFTER_REPO"
  printf ' Iterations   : %s per DAO call\n' "$ITERATIONS"
  printf '\n'
  printf '%s\n' "1. UNCLOSED JDBC OBJECTS (measured at runtime, one harness, two builds)"
  printf '%s\n' "------------------------------------------------------------------------------"
  printf '%-24s %-24s %-24s\n' 'RESOURCE' 'BEFORE (c | cl | LEAK)' 'AFTER (c | cl | LEAK)'
  printf '%s\n' "   c = created   cl = closed   LEAK = never closed (still open)"
  printf '%s\n' "------------------------------------------------------------------------------"
  awk -F'|' '
    FNR==NR && $1=="RESOURCE" { before[$2] = $3" | "$4" | "$5; next }
    $1=="RESOURCE"            { printf "%-24s %-24s %-24s\n", $2, before[$2], $3" | "$4" | "$5 }
  ' "$REPORTS/before.txt" "$REPORTS/after.txt"
  printf '\n'
  awk -F'|' '
    FNR==NR { if ($1=="RESOURCE") before[$2]=$5; next }
    $1=="RESOURCE" && $2=="Statements (all)" { printf " Statements leaked  : BEFORE %s  ->  AFTER %s\n", before["Statements (all)"], $5 }
    $1=="RESOURCE" && $2=="ResultSets"       { printf " Result sets leaked : BEFORE %s  ->  AFTER %s\n", before["ResultSets"], $5 }
  ' "$REPORTS/before.txt" "$REPORTS/after.txt"
  printf '%s\n' "------------------------------------------------------------------------------"
  printf '%s\n' " NOTE: 'created' differs between builds by design - the refactor also replaced"
  printf '%s\n' " Statement (string-built SQL) with PreparedStatement (bound parameters), so"
  printf '%s\n' " the post-fix build opens more prepared statements and zero raw statements."
  printf '%s\n' " The metric that matters is LEAKED, which goes to zero."
  printf '\n'
  printf '%s\n' "2. HOW THE SQL WAS SENT (measured at runtime, same workload)"
  printf '%s\n' "------------------------------------------------------------------------------"
  printf '%-36s %-12s %-12s\n' 'SQL EXECUTION MODE' 'BEFORE' 'AFTER'
  printf '%s\n' "------------------------------------------------------------------------------"
  awk -F'|' '
    FNR==NR { if ($1=="COUNTER") before[$2]=$3; next }
    $1=="COUNTER" { printf "%-36s %-12s %-12s\n", $2, before[$2], $3 }
  ' "$REPORTS/before.txt" "$REPORTS/after.txt"
  printf '%s\n' "------------------------------------------------------------------------------"
  printf '%s\n' " A literal-text execution means the SQL string itself was handed to MySQL, so"
  printf '%s\n' " any value in it is part of the statement. That is the SonarQube dynamic-SQL"
  printf '%s\n' " finding: after the fix the SQL text contains '?', so no value is inline."
  printf '\n'
  printf '%s\n' "3. STATIC CHECK OVER BOTH REPOS (grep, no database involved)"
  printf '%s\n' "------------------------------------------------------------------------------"
  printf '%-46s %-14s %-14s\n' 'METRIC (whole src/main/java)' 'BEFORE' 'AFTER'
  printf '%-46s %-14s %-14s\n' 'connection.createStatement() sites' "$BEFORE_CREATE" "$AFTER_CREATE"
  printf '%-46s %-14s %-14s\n' 'SQL text with an appended value  = " +' "$BEFORE_CONCAT" "$AFTER_CONCAT"
  printf '%s\n' "------------------------------------------------------------------------------"
  printf '%s\n' " Run tools/leakdemo/check-sql-safety.sh for the two findings with file:line."
  printf '%s\n' "=============================================================================="
} > "$SIDE"

say ""
cat "$SIDE"
say ""
say "full reports:"
say "  $REPORTS/before.txt"
say "  $REPORTS/after.txt"
say "  $SIDE"
say "raw process output (includes the leaked-statement error noise of the pre-fix build):"
say "  $REPORTS/before.raw.log"
say "  $REPORTS/after.raw.log"
say "instant static check (no database needed): bash tools/leakdemo/check-sql-safety.sh"
