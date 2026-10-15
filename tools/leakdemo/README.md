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

## Why the numbers can be trusted

* Every `Connection` / `Statement` / `PreparedStatement` / `ResultSet` handed to
  the DAO layer is replaced by a `java.lang.reflect.Proxy`. The proxy counts
  creations and `close()` calls and records the DAO method that created each
  object, so a never-closed object can be attributed to the exact method.
* The tracked connection is passed into the DAOs through the constructor they
  already expose (`AccountAccessDao`, `UserDao`, `HRPersonnelDao`,
  `EmployeeDao` all take a `Connection` in both builds), so the measurement does
  not depend on editing `utils.DbConnection`.
* A `Driver` shim is registered as well, so code paths that go through
  `DriverManager.getConnection(...)` (for example `new AccountAccessDao()`) are
  instrumented too - that is what the *connection probe* measures.
* One single compiled harness runs against both builds. Every method it calls
  has an identical signature in both builds; `HRPersonnelDao.loadSummaryReport`
  is invoked reflectively because its return type changed from `ResultSet` to
  `List<Map<String, Object>>` - that change is itself part of the fix.
* The `METHOD EXECUTION HEALTH` section prints attempted/ok calls per method.
  If the two runs ever disagree there, the comparison is invalid - check it
  first.

## Run it

```bash
tools/leakdemo/run-leak-demo.sh
```

Requirements: MySQL on `localhost:3306` with the `payrollsystem_db_aoop` dump
restored, Maven, and a JDK new enough to read class files compiled for Java 21.

Environment overrides:

| Variable | Default |
|---|---|
| `BEFORE_REPO` | `/Users/alexresu/OOPMotorPh_Payroll_System` |
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/payrollsystem_db_aoop` |
| `MYSQL_USER` | `root` |
| `MYSQL_PASS` | `Alex123!!` |
| `ITERATIONS` | `100` |

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
Connections              11 | 0 | 11              11 | 0 | 11
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

### One thing the fix does *not* change

`Connections: 11 created, 0 closed` appears in **both** builds.
`utils.DbConnection.getConnection()` opens a new physical connection on every
call and nothing ever closes it, so every DAO construction leaks a connection.
Commit `5785fa0` scoped itself to statements and result sets, so this is
reported as a known, still-open finding rather than hidden. `Threads_connected`
in the server view starts high for the same reason: connections leaked by
earlier runs are still open on the server.

### Bonus finding

The pre-fix `AccountAccessDao.login` prints
`Error while executing SQL query!` to `stderr` on the **success** path (a
misplaced `System.err.println`). It appears 100 times in `before.raw.log` and 0
times in `after.raw.log`.

## Layout

```
tools/leakdemo/
  run-leak-demo.sh              builds both repos, compiles the harness, runs both, prints the delta
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
