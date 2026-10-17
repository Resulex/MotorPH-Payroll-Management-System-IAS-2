# PreparedStatement / ResultSet resource leak - before & after demonstration

Terminal evidence for the refactor in commit
[`5785fa0`](https://github.com/Resulex/MotorPH-Payroll-Management-System-IAS-2/commit/5785fa0d10e0f5b70a66a0eecf9beecfafb90f31)
*"refactor: move preparedstatement to prevent resource leak"*.

It runs **the same workload, with the same daemon, against two different
`target/classes` trees** and counts how many JDBC objects were created and how
many were explicitly closed:

| | Repository | Role |
|---|---|---|
| BEFORE | `OOPMotorPh_Payroll_System` | pre-fix build (`Statement` fields, `createStatement()`, `ResultSet` returned to callers) |
| AFTER | `MotorPH-Payroll-Management-System-IAS-2` | post-fix build (`try (PreparedStatement ...)`, `try (ResultSet ...)`) |

Nothing in either repository's production code is modified. All instrumentation
lives in this folder.

## Quick start (copy-paste)

Run everything from the root of the `MotorPH-Payroll-Management-System-IAS-2`
repository:

```bash
# 0. instant static check - no database, no build, about a second
bash tools/leakdemo/check-sql-safety.sh

# 1. the full before/after evidence (builds both repos, runs both, writes reports)
bash tools/leakdemo/run-leak-demo.sh

# 2. the two reports, and the comparison to present
cat tools/leakdemo/reports/SIDE-BY-SIDE.txt
less tools/leakdemo/reports/before.txt
less tools/leakdemo/reports/after.txt
```

If MySQL is not running, step 1 cannot measure anything - use step 0 and the
committed reports under `reports/` instead.

## Why the numbers can be trusted

* Every `Statement` / `PreparedStatement` / `ResultSet` handed to the DAO layer
  is replaced by a `java.lang.reflect.Proxy`. The proxy counts creations and
  `close()` calls and records the DAO method that created each object, so a
  never-closed object can be attributed to the exact method. Connections are
  wrapped as well, but only so that the statements they create are tracked -
  connection usage is deliberately not measured here.
* The tracked connection is passed into the DAOs through the constructor they
  already expose (`AccountAccessDao`, `UserDao`, `HRPersonnelDao`,
  `EmployeeDao` all take a `Connection` in both builds), so the measurement does
  not depend on editing `utils.DbConnection`.
* A `Driver` shim is registered as well, so code paths that go through
  `DriverManager.getConnection(...)` instead of the injected connection are
  instrumented too.
* One single compiled harness runs against both builds. Every method it calls
  has an identical signature in both builds; `HRPersonnelDao.loadSummaryReport`
  is invoked reflectively because its return type changed from `ResultSet` to
  `List<Map<String, Object>>` - that change is itself part of the fix.
* The `METHOD EXECUTION HEALTH` section prints attempted/ok calls per method.
  If the two runs ever disagree there, the comparison is invalid - check it
  first.

## Run it

```bash
bash tools/leakdemo/run-leak-demo.sh       # full evidence: builds, runs, compares
bash tools/leakdemo/check-sql-safety.sh    # static check only: instant, no database
```

Requirements: MySQL on `localhost:3306` with the `payrollsystem_db_aoop` dump
restored, Maven, and a JDK new enough to read class files compiled for Java 21.
`check-sql-safety.sh` needs none of that - it only reads source files.

Environment overrides:

| Variable | Default |
|---|---|
| `BEFORE_REPO` | `/Users/alexresu/OOPMotorPh_Payroll_System` |
| `MYSQL_URL` | `MOTORPH_DB_URL`, then `db.url` in `config.properties`, then `jdbc:mysql://localhost:3306/payrollsystem_db_aoop` |
| `MYSQL_USER` | `MOTORPH_DB_USER`, then `db.user` in `config.properties`, then `root` |
| `MYSQL_PASS` | `MOTORPH_DB_PASSWORD`, then `MYSQL_PWD`, then `db.password` in `config.properties` (**required**) |
| `MOTORPH_CONFIG` | `config.properties` in the repository root |
| `ITERATIONS` | `100` |

No password is stored in this repository. Create a local, git-ignored file once:

```bash
cp config.properties.example config.properties
# then set db.user and db.password
```

Output:

```
tools/leakdemo/reports/before.txt        full report for the pre-fix build
tools/leakdemo/reports/after.txt         full report for the post-fix build
tools/leakdemo/reports/SIDE-BY-SIDE.txt  the comparison that gets presented
tools/leakdemo/reports/*.raw.log         raw stdout/stderr of each JVM
```

## What the output shows

Headline (100 iterations of 6 DAO calls):

```
RESOURCE                 BEFORE (c | cl | LEAK)   AFTER (c | cl | LEAK)
Statement                600 | 0 | 600            0 | 0 | 0
PreparedStatement        500 | 100 | 400          700 | 700 | 0
ResultSets               700 | 100 | 600          700 | 700 | 0
Statements (all)         1100 | 100 | 1000        700 | 700 | 0
```

* **Statements: 1000 leaked -> 0. Result sets: 600 leaked -> 0.** In the pre-fix
  build not a single `Statement`/`ResultSet` opened by the DAO layer was ever
  closed; the objects were dropped on the floor and waited for garbage
  collection. After the fix everything is closed deterministically.
* The leaked objects are attributed per method, e.g.
  `300 dao.AccountAccessDao.login`, `200 dao.EmployeeDao.loadAttendanceList`,
  `200 dao.HRPersonnelDao.loadSummaryReport`, `100 dao.UserDao.getUserInfo`.
* `UserDao.getRoleName` is kept as a **control**: it already used
  try-with-resources before the fix, so it closes its statement in both runs -
  proof that the harness distinguishes leaking from non-leaking code.
* `HRPersonnelDao.loadSummaryReport` returned a **live `ResultSet`** before the
  fix and a mapped `List` (already closed) after it.
* `'created'` is intentionally not identical in the two builds: the refactor
  replaced string-built `Statement` SQL with parameter-bound
  `PreparedStatement`, so the post-fix build opens more prepared statements and
  zero raw statements. The metric that matters is `LEAKED`.

### SQL built by concatenation (the second defect class)

`SIDE-BY-SIDE.txt` section 2 answers the SonarQube finding *"Make sure using a
dynamically formatted SQL query is safe here"* at `AccountAccessDao.java:90`
and `UserDao.java:288`. Same workload, same call sites, only the SQL differs:

```
BEFORE   dao.AccountAccessDao.login         100 literal, 100 prepared
            literal  SELECT CONCAT( ...  WHERE employee_id = 10001
            prepared SELECT employe ... ee_id = ? AND password = ?
         dao.UserDao.getUserInfo            100 literal
            literal  SELECT * FROM  ...  WHERE employee_id = 10001

AFTER    dao.AccountAccessDao.login         200 prepared
            prepared SELECT CONCAT( ... n_id WHERE employee_id = ?
            prepared SELECT employe ... ee_id = ? AND password = ?
         dao.UserDao.getUserInfo            100 prepared
            prepared SELECT * FROM  ... view WHERE employee_id = ?
```

* `literal` = the SQL string itself was handed to MySQL (a plain `Statement`),
  so every value inside it is part of the statement.
* `prepared` = the SQL text contains `?` and the values travelled as bound
  parameters.
* Runtime totals: **200 -> 0 literal-text executions**, while the total number
  of executions stays at 700 in both builds - the same SQL work, delivered
  differently.
* Whole-repo static check (section 3 / `check-sql-safety.sh`): **29 -> 0**
  `connection.createStatement()` sites and **2 -> 0** values concatenated into
  SQL text.

**Say this, and not more than this.** Both concatenated values were `int`s
(`employeeNumber`, `employeeId`), so those two sinks were never reachable with a
payload: SonarQube's message is a *hotspot* asking for a review decision, not a
confirmed injection. The claim is "the pattern is gone and the value is now a
bound parameter", not "we stopped an attack". Note also that
`AccountAccessDaoTest.testLoginSqlInjectionPayloadFails` and
`UserDaoTest.authenticateSqlInjectionPayloadFails` pass on **both** builds (the
old `login()` already bound its two `?`s and `authenticate()` is byte-identical
in both builds) - they are regression guards, not evidence of this fix.

### Server-side confirmation (MySQL's own view)

The harness forces `useServerPrepStmts=true` so MySQL can report the leak too.
The gap between `Com_stmt_prepare` and `Com_stmt_close` is the number of
prepared statements the server is still holding:

```
BEFORE   prepared_statements_instances   0 -> 200 -> 400     (growing)
         UNCLOSED (prepare - close)    400 -> 600 -> 800     (growing)
AFTER    prepared_statements_instances   0 ->   0 ->   0     (flat)
         UNCLOSED (prepare - close)   1200 ->1200 ->1200     (flat)
```

`performance_schema.prepared_statements_instances` lists the individual
statements, with SQL text, while they are still open. `sql/monitor.sql` runs the
same queries by hand.

### Bonus finding

The pre-fix `AccountAccessDao.login` prints
`Error while executing SQL query!` to `stderr` on the **success** path (a
misplaced `System.err.println`). It appears 100 times in `before.raw.log` and 0
times in `after.raw.log`.

## Layout

```
tools/leakdemo/
  run-leak-demo.sh              builds both repos, compiles the harness, runs both, prints the delta
  check-sql-safety.sh           instant static check of the dynamic-SQL finding (no database)
  src/demo/JdbcTracker.java     JDBC proxies, counting, Driver shim, callsite attribution
  src/demo/LeakDemo.java        workload, console report, server-side monitor sampling
  sql/monitor.sql               the same MySQL queries run by hand
  out/                          compiled harness + dependency classpaths (git-ignored)
  reports/                      generated evidence
```

## Troubleshooting

* `class file has wrong version` - the harness was compiled with an older JDK
  than the project. The script picks the JDK Maven itself runs on; set
  `JAVA_HOME` to a Java 21+ JDK to override.
* Everything shows 0 leaked in the BEFORE run - check the
  `METHOD EXECUTION HEALTH` section: the DAO calls are failing (usually the
  MySQL user/password or an unrestored database), so nothing is being executed.
* `useServerPrepStmts=true` is required for the server-side numbers; without it
  Connector/J emulates prepared statements client-side and MySQL sees nothing.
  The in-JVM counters work either way.
