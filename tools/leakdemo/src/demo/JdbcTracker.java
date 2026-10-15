package demo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * JDBC instrumentation for the before/after PreparedStatement resource-leak demo.
 *
 * Every {@link Statement}, {@link PreparedStatement} and {@link ResultSet} handed
 * to the code under test is replaced by a dynamic proxy. The proxy counts
 * creations and explicit {@code close()} calls, and remembers the DAO call site
 * that created each object, so never-closed statements and result sets can be
 * reported per method. Connections are wrapped too, but only so that the
 * statements they create can be tracked - connection usage is not measured.
 *
 * No production class is modified: the connection handed to the DAOs is wrapped
 * here, and a {@link TrackingDriver} is registered so that even code paths that
 * call {@code DriverManager.getConnection(...)} (for example
 * {@code utils.DbConnection}) are instrumented.
 */
public final class JdbcTracker {

    public static final String STATEMENT = "Statement";
    public static final String PREPARED = "PreparedStatement";
    public static final String CALLABLE = "CallableStatement";
    public static final String RESULTSET = "ResultSet";

    public static final String[] STATEMENT_KINDS = {STATEMENT, PREPARED, CALLABLE};

    /** A prepared statement: the SQL has placeholders and the values travel separately. */
    public static final String MODE_PREPARED = "prepared";
    /** A plain Statement: the SQL text is sent as-is, so any value in it is part of the statement. */
    public static final String MODE_LITERAL_TEXT = "literal-text";

    private static final class Entry {
        final String kind;
        final String site;
        final String sql;

        Entry(String kind, String site, String sql) {
            this.kind = kind;
            this.site = site;
            this.sql = sql;
        }
    }

    private final Driver realDriver;
    private final ClassLoader loader = JdbcTracker.class.getClassLoader();

    /** Currently open objects, keyed by tracking id. Removed on close(). */
    private final Map<Integer, Entry> open = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> createdCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> closedCounts = new ConcurrentHashMap<>();
    /** How the SQL reached the server: MODE_PREPARED or MODE_LITERAL_TEXT. */
    private final Map<String, AtomicInteger> executionsByMode = new ConcurrentHashMap<>();
    /** Executions of literal (non-parameterized) SQL, per DAO method. */
    private final Map<String, AtomicInteger> literalExecutionsBySite = new ConcurrentHashMap<>();
    /** Executions of prepared statements, per DAO method. */
    private final Map<String, AtomicInteger> preparedExecutionsBySite = new ConcurrentHashMap<>();
    /** Distinct SQL texts sent as-is, per DAO method - shows whether values sit inside the SQL. */
    private final Map<String, Set<String>> literalSqlBySite = new ConcurrentHashMap<>();
    /** Distinct SQL texts of prepared statements (with placeholders), per DAO method. */
    private final Map<String, Set<String>> preparedSqlBySite = new ConcurrentHashMap<>();
    /** Guards against wrapping the same ResultSet twice (execute() then getResultSet()). */
    private final Map<Object, Object> resultSetProxies = Collections.synchronizedMap(new IdentityHashMap<>());
    private final AtomicInteger sequence = new AtomicInteger();

    private String driverShimStatus = "not installed";

    private JdbcTracker(Driver realDriver) {
        this.realDriver = realDriver;
    }

    // ------------------------------------------------------------------ install

    /**
     * Makes every {@code DriverManager.getConnection("jdbc:mysql:...")} call return a
     * tracked connection by removing the real MySQL driver from the registry and
     * registering a delegating driver that wraps whatever it returns.
     */
    public static JdbcTracker install(String user, String password) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        Driver real = null;
        List<Driver> stale = new ArrayList<>();
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            if (d.getClass().getName().startsWith("com.mysql")) {
                stale.add(d);
                if (real == null) {
                    real = d;
                }
            }
        }
        if (real == null) {
            real = (Driver) Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        }
        for (Driver d : stale) {
            try {
                DriverManager.deregisterDriver(d);
            } catch (SQLException ignored) {
                // Non fatal: openTrackedConnection() still works, the shim just may not win.
            }
        }

        JdbcTracker tracker = new JdbcTracker(real);
        DriverManager.registerDriver(new TrackingDriver(tracker, user, password));
        tracker.driverShimStatus = stale.isEmpty()
                ? "installed (no com.mysql driver found in DriverManager; shim may not intercept)"
                : "installed (" + stale.size() + " com.mysql driver(s) deregistered)";
        return tracker;
    }

    public String driverShimStatus() {
        return driverShimStatus;
    }

    /** Always-tracked connection, independent of the DriverManager shim. */
    public Connection openTrackedConnection(String url, String user, String password) throws SQLException {
        Connection real = realDriver.connect(url, connectionProperties(user, password));
        if (real == null) {
            throw new SQLException("The MySQL driver did not accept the URL: " + url);
        }
        return (Connection) trackConnection(real);
    }

    /** Untracked connection, used for the server-side monitor samples. */
    public Connection openRawConnection(String url, String user, String password) throws SQLException {
        Properties props = new Properties();
        if (user != null) {
            props.setProperty("user", user);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        return realDriver.connect(url, props);
    }

    private static Properties connectionProperties(String user, String password) {
        Properties props = new Properties();
        // Server-side prepared statements are what MySQL can report on; without this the
        // connector emulates prepared statements client-side and the server sees nothing.
        props.setProperty("useServerPrepStmts", "true");
        props.setProperty("cachePrepStmts", "false");
        if (user != null) {
            props.setProperty("user", user);
        }
        if (password != null) {
            props.setProperty("password", password);
        }
        return props;
    }

    // ------------------------------------------------------------------ counters

    public int createdOf(String kind) {
        return of(createdCounts, kind);
    }

    public int closedOf(String kind) {
        return of(closedCounts, kind);
    }

    public int leakedOf(String kind) {
        int n = 0;
        for (Entry e : open.values()) {
            if (e.kind.equals(kind)) {
                n++;
            }
        }
        return n;
    }

    public int leakedOf(String... kinds) {
        int n = 0;
        for (String kind : kinds) {
            n += leakedOf(kind);
        }
        return n;
    }

    /** Never-closed objects grouped by the DAO method that created them, biggest first. */
    public Map<String, Integer> leakedBySite(String... kinds) {
        Map<String, Integer> bySite = new ConcurrentHashMap<>();
        outer:
        for (Entry e : open.values()) {
            for (String kind : kinds) {
                if (e.kind.equals(kind)) {
                    bySite.merge(e.site, 1, Integer::sum);
                    continue outer;
                }
            }
        }
        return sorted(bySite);
    }

    /** SQL text of every still-open statement, trimmed - handy to show what was left hanging. */
    public List<String> leakedSqlTexts() {
        List<String> sql = new ArrayList<>();
        for (Entry e : open.values()) {
            if (e.sql != null && !e.sql.isBlank() && isStatementKind(e.kind)) {
                sql.add(e.sql.substring(0, Math.min(60, e.sql.length())).replaceAll("\\s+", " "));
            }
        }
        Collections.sort(sql);
        return sql;
    }

    private static boolean isStatementKind(String kind) {
        for (String k : STATEMENT_KINDS) {
            if (k.equals(kind)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Integer> sorted(Map<String, Integer> source) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(source.entrySet());
        list.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : list) {
            out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private static int of(Map<String, AtomicInteger> map, String kind) {
        AtomicInteger n = map.get(kind);
        return n == null ? 0 : n.get();
    }

    // ------------------------------------------------------- SQL safety counters

    /** Number of statement executions, by how the SQL reached the server. */
    public int executions(String mode) {
        return of(executionsByMode, mode);
    }

    /** DAO methods that executed SQL whose text carries the values, biggest first. */
    public Map<String, Integer> literalExecutionsBySite() {
        Map<String, Integer> bySite = new ConcurrentHashMap<>();
        literalExecutionsBySite.forEach((site, n) -> bySite.put(site, n.get()));
        return sorted(bySite);
    }

    /** DAO methods that executed SQL, by total executions (prepared + literal), biggest first. */
    public Map<String, Integer> sqlSites() {
        Map<String, Integer> totals = new ConcurrentHashMap<>();
        preparedExecutionsBySite.forEach((site, n) -> totals.merge(site, n.get(), Integer::sum));
        literalExecutionsBySite.forEach((site, n) -> totals.merge(site, n.get(), Integer::sum));
        return sorted(totals);
    }

    public int literalExecutionsOf(String site) {
        return of(literalExecutionsBySite, site);
    }

    public int preparedExecutionsOf(String site) {
        return of(preparedExecutionsBySite, site);
    }

    /** The distinct SQL texts a DAO method sent as literal text, alphabetically. */
    public List<String> literalSql(String site) {
        return textsOf(literalSqlBySite, site);
    }

    /** The distinct SQL texts of the prepared statements a DAO method used, alphabetically. */
    public List<String> preparedSql(String site) {
        return textsOf(preparedSqlBySite, site);
    }

    private static List<String> textsOf(Map<String, Set<String>> map, String site) {
        Set<String> texts = map.get(site);
        if (texts == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>(texts);
        Collections.sort(out);
        return out;
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    /**
     * Records one statement execution.
     *
     * @param site       the DAO method that ran the statement
     * @param literalSql the SQL text when the statement was executed with the SQL as an
     *                   argument (a plain Statement), or null when the statement was
     *                   prepared with placeholders and the values were bound separately
     */
    private void recordExecution(String site, String literalSql) {
        bump(executionsByMode, literalSql == null ? MODE_PREPARED : MODE_LITERAL_TEXT);
        if (literalSql == null) {
            bump(preparedExecutionsBySite, site);
        } else {
            bump(literalExecutionsBySite, site);
            literalSqlBySite.computeIfAbsent(site, k -> ConcurrentHashMap.newKeySet())
                    .add(normalizeSql(literalSql));
        }
    }

    private static void bump(Map<String, AtomicInteger> map, String key) {
        map.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    private int register(String kind, String site, String sql) {
        int id = sequence.incrementAndGet();
        open.put(id, new Entry(kind, site, sql));
        bump(createdCounts, kind);
        if (sql != null) {
            preparedSqlBySite.computeIfAbsent(site, k -> ConcurrentHashMap.newKeySet())
                    .add(normalizeSql(sql));
        }
        return id;
    }

    private void unregister(int id, String kind) {
        if (open.remove(id) != null) {
            bump(closedCounts, kind);
        }
    }

    /**
     * Name of the calling DAO method: the first stack frame inside {@code dao.}.
     * Falls back to the first application frame when the caller is not a DAO.
     */
    private static String callerSite() {
        StackTraceElement[] frames = new Throwable().getStackTrace();
        for (StackTraceElement f : frames) {
            if (f.getClassName().startsWith("dao.")) {
                return f.getClassName() + "." + f.getMethodName();
            }
        }
        for (StackTraceElement f : frames) {
            String c = f.getClassName();
            if (!c.startsWith("demo.") && !c.startsWith("java.") && !c.startsWith("jdk.")
                    && !c.startsWith("com.mysql") && !c.startsWith("sun.")) {
                return c + "." + f.getMethodName();
            }
        }
        return "unknown";
    }

    private static Object call(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    // ------------------------------------------------------------------ wrapping

    private Object trackConnection(Connection real) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{Connection.class}, new ConnectionHandler(real));
    }

    private Object wrapStatement(Statement real, String kind, String sql, Object connectionProxy) {
        String site = callerSite();
        int id = register(kind, site, sql);
        Class<?> iface = switch (kind) {
            case PREPARED -> PreparedStatement.class;
            case CALLABLE -> CallableStatement.class;
            default -> Statement.class;
        };
        return Proxy.newProxyInstance(loader, new Class<?>[]{iface},
                new StatementHandler(real, kind, id, connectionProxy, site));
    }

    private Object wrapResultSet(ResultSet real, Object statementProxy) {
        Object existing = resultSetProxies.get(real);
        if (existing != null) {
            return existing;
        }
        int id = register(RESULTSET, callerSite(), null);
        Object proxy = Proxy.newProxyInstance(loader, new Class<?>[]{ResultSet.class},
                new ResultSetHandler(real, id, statementProxy));
        resultSetProxies.put(real, proxy);
        return proxy;
    }

    private final class ConnectionHandler implements InvocationHandler {
        private final Connection real;

        ConnectionHandler(Connection real) {
            this.real = real;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "createStatement": {
                    Object st = call(real, method, args);
                    return st == null ? null : wrapStatement((Statement) st, STATEMENT, null, proxy);
                }
                case "prepareStatement": {
                    Object st = call(real, method, args);
                    String sql = (args != null && args.length > 0 && args[0] instanceof String s) ? s : null;
                    return st == null ? null : wrapStatement((Statement) st, PREPARED, sql, proxy);
                }
                case "prepareCall": {
                    Object st = call(real, method, args);
                    String sql = (args != null && args.length > 0 && args[0] instanceof String s) ? s : null;
                    return st == null ? null : wrapStatement((Statement) st, CALLABLE, sql, proxy);
                }
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "TrackedConnection";
                default:
                    return call(real, method, args);
            }
        }
    }

    private final class StatementHandler implements InvocationHandler {
        private final Statement real;
        private final String kind;
        private final int id;
        private final Object connectionProxy;
        private final String site;

        StatementHandler(Statement real, String kind, int id, Object connectionProxy, String site) {
            this.real = real;
            this.kind = kind;
            this.id = id;
            this.connectionProxy = connectionProxy;
            this.site = site;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                // A plain Statement is executed with the SQL as an argument, so the SQL
                // text carries every value. A prepared statement is executed without an
                // argument - the values were bound separately. That difference is what
                // makes the SonarQube dynamic-SQL finding visible at runtime.
                case "executeQuery":
                case "execute":
                case "executeUpdate":
                case "executeLargeUpdate": {
                    String literalSql = (args != null && args.length > 0 && args[0] instanceof String s) ? s : null;
                    recordExecution(site, literalSql);
                    Object result = call(real, method, args);
                    if (result instanceof ResultSet rs) {
                        return wrapResultSet(rs, proxy);
                    }
                    return result;
                }
                case "getResultSet": {
                    Object rs = call(real, method, args);
                    return rs == null ? null : wrapResultSet((ResultSet) rs, proxy);
                }
                case "getGeneratedKeys": {
                    Object rs = call(real, method, args);
                    return rs == null ? null : wrapResultSet((ResultSet) rs, proxy);
                }
                case "getConnection":
                    return connectionProxy != null ? connectionProxy : call(real, method, args);
                case "close":
                    unregister(id, kind);
                    return call(real, method, args);
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "Tracked" + kind + "#" + id;
                default:
                    return call(real, method, args);
            }
        }
    }

    private final class ResultSetHandler implements InvocationHandler {
        private final ResultSet real;
        private final int id;
        private final Object statementProxy;

        ResultSetHandler(ResultSet real, int id, Object statementProxy) {
            this.real = real;
            this.id = id;
            this.statementProxy = statementProxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            switch (method.getName()) {
                case "close":
                    unregister(id, RESULTSET);
                    resultSetProxies.remove(real);
                    return call(real, method, args);
                case "getStatement":
                    return statementProxy;
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "TrackedResultSet#" + id;
                default:
                    return call(real, method, args);
            }
        }
    }

    /** Driver that wraps every connection it opens. */
    private static final class TrackingDriver implements Driver {
        private final JdbcTracker tracker;
        private final String user;
        private final String password;

        TrackingDriver(JdbcTracker tracker, String user, String password) {
            this.tracker = tracker;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            Properties props = new Properties();
            if (info != null) {
                props.putAll(info);
            }
            props.putAll(connectionProperties(user, password));
            if (user != null) {
                props.setProperty("user", user);
            }
            if (password != null) {
                props.setProperty("password", password);
            }
            Connection real = tracker.realDriver.connect(url, props);
            if (real == null) {
                return null;
            }
            return (Connection) tracker.trackConnection(real);
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:mysql:");
        }

        @Override
        public java.sql.DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new java.sql.DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger("demo.trackingdriver");
        }
    }
}
