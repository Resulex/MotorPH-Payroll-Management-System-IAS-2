package demo;

import dao.AccountAccessDao;
import dao.EmployeeDao;
import dao.HRPersonnelDao;
import dao.UserDao;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Before/after demonstration of the PreparedStatement / ResultSet resource leak
 * fixed by commit 5785fa0 ("refactor: move preparedstatement to prevent resource leak").
 *
 * The same workload is executed twice with this same harness - once against the
 * pre-fix build and once against the post-fix build - and the number of JDBC
 * objects created versus explicitly closed is reported.
 *
 * Usage:
 *   java -cp &lt;harness&gt;:&lt;build under test&gt;:&lt;deps&gt; demo.LeakDemo \
 *        --label BEFORE --build /path/to/OOPMotorPh_Payroll_System \
 *        --iterations 100 --user root --pass ***** --out reports/BEFORE.txt
 */
public final class LeakDemo {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/payrollsystem_db_aoop";
    private static final int LINE = 78;

    private static final String[] WORKLOAD = {
            "AccountAccessDao.login",
            "AccountAccessDao.getAccountEmail",
            "UserDao.getUserInfo",
            "EmployeeDao.loadAttendanceList",
            "HRPersonnelDao.loadSummaryReport (reflective)",
            "UserDao.getRoleName (control - already closed before the fix)",
    };

    private static final Map<String, Integer> attempted = new TreeMap<>();
    private static final Map<String, Integer> failed = new TreeMap<>();
    private static final Map<String, String> firstError = new TreeMap<>();

    private LeakDemo() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = parse(args);
        String label = opts.getOrDefault("label", "RUN");
        String build = opts.getOrDefault("build", "(current classpath)");
        String url = opts.getOrDefault("url", DEFAULT_URL);
        String user = opts.getOrDefault("user", System.getenv("MYSQL_USER") == null
                ? "root" : System.getenv("MYSQL_USER"));
        String pass = opts.getOrDefault("pass", System.getenv("MYSQL_PASS") == null
                ? "Alex123!!" : System.getenv("MYSQL_PASS"));
        int iterations = Integer.parseInt(opts.getOrDefault("iterations", "100"));
        Path out = opts.containsKey("out") ? Path.of(opts.get("out")) : null;

        StringBuilder report = new StringBuilder();

        JdbcTracker tracker = JdbcTracker.install(user, pass);

        Connection shared = tracker.openTrackedConnection(url, user, pass);
        Monitor monitor = new Monitor(tracker, url, user, pass);

        monitor.sample("START");

        AccountAccessDao accountDao = new AccountAccessDao(shared);
        UserDao userDao = new UserDao(shared);
        HRPersonnelDao hrDao = new HRPersonnelDao(shared);
        EmployeeDao employeeDao = new EmployeeDao(shared);
        Method loadSummaryReport = hrDao.getClass().getMethod("loadSummaryReport", int.class, int.class);

        String returnedType = "not called";
        int mid = Math.max(1, iterations / 2);
        for (int i = 1; i <= iterations; i++) {
            call("AccountAccessDao.login", () -> accountDao.login(10001, "Garcia01"));
            call("AccountAccessDao.getAccountEmail", () -> accountDao.getAccountEmail(10001));
            call("UserDao.getUserInfo", () -> userDao.getUserInfo(10001, "System Admin", true));
            call("EmployeeDao.loadAttendanceList", () -> employeeDao.loadAttendanceList(10001));
            Object summary = call("HRPersonnelDao.loadSummaryReport (reflective)",
                    () -> loadSummaryReport.invoke(hrDao, 6, 2024));
            if (summary != null) {
                returnedType = describe(summary);
            }
            call("UserDao.getRoleName (control - already closed before the fix)",
                    () -> userDao.getRoleName(10007));
            if (i == mid) {
                monitor.sample("MID");
            }
        }

        monitor.sample("END");

        buildReport(report, label, build, url, iterations, tracker, monitor, returnedType);

        System.out.print(report);
        if (out != null) {
            writeReport(out, report.toString());
        }

        shared.close();
        monitor.close();
        System.exit(0);
    }

    // ------------------------------------------------------------------ report

    /**
     * Makes the HRPersonnelDao.loadSummaryReport change visible: before the fix the DAO
     * handed a live ResultSet out of the persistence layer (the classic S2095 escape);
     * after the fix it returns a mapped, already-closed list.
     */
    private static String describe(Object returned) {
        if (returned instanceof ResultSet) {
            return "java.sql.ResultSet  <-- LIVE ResultSet handed to the caller (never closed)";
        }
        if (returned instanceof List<?> rows) {
            return "java.util.List  <-- " + rows.size() + " rows mapped inside the DAO, ResultSet closed";
        }
        return returned.getClass().getName();
    }

    private static void buildReport(StringBuilder sb, String label, String build, String url,
            int iterations, JdbcTracker tracker, Monitor monitor, String returnedType) {

        int stmtLeaked = tracker.leakedOf(JdbcTracker.STATEMENT_KINDS);
        int rsLeaked = tracker.leakedOf(JdbcTracker.RESULTSET);

        rule(sb, '=');
        line(sb, "MOTORPH PAYROLL - JDBC PREPAREDSTATEMENT / RESULTSET RESOURCE LEAK DEMO");
        rule(sb, '=');
        line(sb, "Run label         : " + label);
        line(sb, "Build under test  : " + build);
        line(sb, "Classes measured  : " + codeSource());        line(sb, "Iterations        : " + iterations + "  (" + WORKLOAD.length + " DAO calls per iteration)");
        line(sb, "JDBC URL          : " + url);
        line(sb, "Driver shim       : " + tracker.driverShimStatus());
        line(sb, "Server-side PS    : useServerPrepStmts=true (forced by the harness)");
        line(sb, "HRPersonnelDao.loadSummaryReport returned:");
        line(sb, "    " + returnedType);

        rule(sb, '-');
        line(sb, pad("JDBC OBJECT", 34) + lpad("CREATED", 12) + lpad("CLOSED", 12) + lpad("LEAKED", 12));
        rule(sb, '-');
        resource(sb, "Statements (all)", stmtSum(tracker, true), stmtSum(tracker, false), stmtLeaked);
        resource(sb, "  java.sql.Statement", tracker.createdOf(JdbcTracker.STATEMENT),
                tracker.closedOf(JdbcTracker.STATEMENT), tracker.leakedOf(JdbcTracker.STATEMENT));
        resource(sb, "  java.sql.PreparedStatement", tracker.createdOf(JdbcTracker.PREPARED),
                tracker.closedOf(JdbcTracker.PREPARED), tracker.leakedOf(JdbcTracker.PREPARED));
        resource(sb, "ResultSets", tracker.createdOf(JdbcTracker.RESULTSET),
                tracker.closedOf(JdbcTracker.RESULTSET), rsLeaked);
        rule(sb, '-');

        line(sb, "LEAKED OBJECTS BY CALL SITE (where the never-closed object was created)");
        rule(sb, '-');
        Map<String, Integer> stmtSites = tracker.leakedBySite(JdbcTracker.STATEMENT_KINDS);
        Map<String, Integer> rsSites = tracker.leakedBySite(JdbcTracker.RESULTSET);
        if (stmtSites.isEmpty() && rsSites.isEmpty()) {
            line(sb, "  none - every statement and result set was closed");
        } else {
            line(sb, "  statements:");
            appendSites(sb, stmtSites);
            line(sb, "  result sets:");
            appendSites(sb, rsSites);
        }
        rule(sb, '-');

        line(sb, "SQL LEFT OPEN (first " + Math.min(5, tracker.leakedSqlTexts().size()) + " of "
                + tracker.leakedSqlTexts().size() + ")");
        rule(sb, '-');
        List<String> sql = tracker.leakedSqlTexts();
        if (sql.isEmpty()) {
            line(sb, "  none");
        } else {
            for (int i = 0; i < Math.min(5, sql.size()); i++) {
                line(sb, "    " + sql.get(i));
            }
        }
        rule(sb, '-');

        line(sb, "METHOD EXECUTION HEALTH (a mismatch here invalidates the comparison)");
        rule(sb, '-');
        for (String name : WORKLOAD) {
            int ok = attempted.getOrDefault(name, 0) - failed.getOrDefault(name, 0);
            int tried = attempted.getOrDefault(name, 0);
            String status = ok + "/" + tried + " ok";
            line(sb, "  " + pad(name, 58) + lpad(status, 12)
                    + (failed.getOrDefault(name, 0) > 0 ? "   ERRORS: " + failed.get(name) : ""));
            String err = firstError.get(name);
            if (err != null) {
                line(sb, "      first error: " + err);
            }
        }
        rule(sb, '-');

        line(sb, "MYSQL SERVER-SIDE VIEW (what the database itself still holds)");
        rule(sb, '-');
        line(sb, pad("", 36) + lpad("START", 14) + lpad("MID", 14) + lpad("END", 14));
        monitor.line(sb, "prepared_statements_instances");
        monitor.line(sb, "Prepared_stmt_count");
        monitor.line(sb, "UNCLOSED (prepare - close)", true);
        monitor.line(sb, "Com_stmt_prepare", false);
        monitor.line(sb, "Com_stmt_close", false);
        if (monitor.error != null) {
            line(sb, "  monitor unavailable: " + monitor.error);
        }
        rule(sb, '-');

        line(sb, "VERDICT");
        rule(sb, '-');
        if (stmtLeaked == 0 && rsLeaked == 0) {
            line(sb, "  FIXED - 0 statements and 0 result sets left unclosed.");
            line(sb, "  Every JDBC object opened by the DAO layer was released deterministically");
            line(sb, "  (try-with-resources), instead of waiting for garbage collection.");
        } else {
            line(sb, "  LEAK - " + stmtLeaked + " statements and " + rsLeaked + " result sets were never");
            line(sb, "  closed. Each one keeps its server-side cursor/buffers alive until the JVM");
            line(sb, "  garbage collects it, which is the SonarQube S2095 defect this refactor fixes.");
        }
        rule(sb, '-');
        line(sb, "MACHINE-READABLE SUMMARY (consumed by run-leak-demo.sh)");
        for (String kind : JdbcTracker.STATEMENT_KINDS) {
            machine(sb, kind, tracker.createdOf(kind), tracker.closedOf(kind), tracker.leakedOf(kind));
        }
        machine(sb, "ResultSets", tracker.createdOf(JdbcTracker.RESULTSET),
                tracker.closedOf(JdbcTracker.RESULTSET), rsLeaked);
        machine(sb, "Statements (all)", stmtSum(tracker, true), stmtSum(tracker, false), stmtLeaked);
        rule(sb, '=');
    }

    private static void machine(StringBuilder sb, String kind, int created, int closed, int leaked) {
        line(sb, "RESOURCE|" + kind + "|" + created + "|" + closed + "|" + leaked);
    }

    private static void appendSites(StringBuilder sb, Map<String, Integer> sites) {
        if (sites.isEmpty()) {
            line(sb, "    none");
            return;
        }
        sites.forEach((site, n) -> line(sb, "    " + lpad(String.valueOf(n), 6) + "  " + site));
    }

    private static int stmtSum(JdbcTracker tracker, boolean created) {
        int total = 0;
        for (String kind : JdbcTracker.STATEMENT_KINDS) {
            total += created ? tracker.createdOf(kind) : tracker.closedOf(kind);
        }
        return total;
    }

    private static String codeSource() {
        try {
            var location = AccountAccessDao.class.getProtectionDomain().getCodeSource().getLocation();
            String path = java.net.URLDecoder.decode(String.valueOf(location),
                    java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = path.split("/");
            int n = parts.length;
            if (n > 3) {
                return ".../" + parts[n - 3] + "/" + parts[n - 2] + "/" + parts[n - 1];
            }
            return path;
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    private static void resource(StringBuilder sb, String name, int created, int closed, int leaked) {
        line(sb, pad(name, 34) + lpad(String.valueOf(created), 12)
                + lpad(String.valueOf(closed), 12) + lpad(String.valueOf(leaked), 12));
    }

    private static void rule(StringBuilder sb, char c) {
        sb.append(String.valueOf(c).repeat(LINE)).append(System.lineSeparator());
    }

    private static void line(StringBuilder sb, String text) {
        sb.append(text).append(System.lineSeparator());
    }

    private static String pad(String s, int n) {
        return s.length() >= n ? s : s + " ".repeat(n - s.length());
    }

    private static String lpad(String s, int n) {
        return s.length() >= n ? s : " ".repeat(n - s.length()) + s;
    }

    private static void writeReport(Path out, String text) {
        try {
            Path parent = out.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(out, text, StandardCharsets.UTF_8);
            System.out.println("report written to " + out.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("could not write report: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ workload

    @FunctionalInterface
    private interface Action {
        Object run() throws Throwable;
    }

    private static Object call(String name, Action action) {
        attempted.merge(name, 1, Integer::sum);
        try {
            return action.run();
        } catch (Throwable t) {
            failed.merge(name, 1, Integer::sum);
            firstError.putIfAbsent(name, t.getClass().getSimpleName() + ": " + t.getMessage());
            return null;
        }
    }

    private static Map<String, String> parse(String[] args) {
        Map<String, String> opts = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    opts.put(key, args[++i]);
                } else {
                    opts.put(key, "true");
                }
            }
        }
        return opts;
    }

    // ------------------------------------------------------------------ monitor

    /** Samples MySQL's own view of open prepared statements through an untracked connection. */
    private static final class Monitor implements AutoCloseable {
        private final Connection raw;
        private final Map<String, Map<String, String>> samples = new LinkedHashMap<>();
        private String error;

        Monitor(JdbcTracker tracker, String url, String user, String pass) {
            Connection c = null;
            try {
                c = tracker.openRawConnection(url, user, pass);
            } catch (SQLException e) {
                error = e.getMessage();
            }
            this.raw = c;
        }

        void sample(String label) {
            if (raw == null) {
                return;
            }
            Map<String, String> values = new LinkedHashMap<>();
            samples.put(label, values);
            try (Statement st = raw.createStatement()) {
                try (ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM performance_schema.prepared_statements_instances")) {
                    if (rs.next()) {
                        values.put("prepared_statements_instances", String.valueOf(rs.getInt(1)));
                    }
                } catch (SQLException e) {
                    error = "performance_schema.prepared_statements_instances: " + e.getMessage();
                }
                for (String variable : new String[]{"Prepared_stmt_count",
                        "Com_stmt_prepare", "Com_stmt_close"}) {
                    try (ResultSet rs = st.executeQuery("SHOW GLOBAL STATUS LIKE '" + variable + "'")) {
                        if (rs.next()) {
                            values.put(variable, rs.getString(2));
                        }
                    }
                }
            } catch (SQLException e) {
                error = e.getMessage();
            }
        }

        void line(StringBuilder sb, String key) {
            line(sb, key, false);
        }

        /**
         * @param computed when true, prints Com_stmt_prepare minus Com_stmt_close, i.e. the
         *                 number of prepared statements the server currently still holds open
         *                 for this client. This is the server-side equivalent of a leak.
         */
        void line(StringBuilder sb, String key, boolean computed) {
            StringBuilder row = new StringBuilder(pad("  " + key, 36));
            for (String label : new String[]{"START", "MID", "END"}) {
                row.append(lpad(value(label, key, computed), 14));
            }
            LeakDemo.line(sb, row.toString());
        }

        private String value(String label, String key, boolean computed) {
            Map<String, String> values = samples.get(label);
            if (values == null) {
                return "-";
            }
            if (computed) {
                String prepare = values.get("Com_stmt_prepare");
                String close = values.get("Com_stmt_close");
                if (prepare == null || close == null) {
                    return "-";
                }
                try {
                    return String.valueOf(Long.parseLong(prepare) - Long.parseLong(close));
                } catch (NumberFormatException e) {
                    return "-";
                }
            }
            return values.getOrDefault(key, "-");
        }

        @Override
        public void close() {
            if (raw != null) {
                try {
                    raw.close();
                } catch (SQLException ignored) {
                    // nothing useful to do at shutdown
                }
            }
        }
    }
}
