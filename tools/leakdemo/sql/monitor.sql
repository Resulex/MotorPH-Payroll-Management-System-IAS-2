-- ---------------------------------------------------------------------------
-- MySQL-side view of the resource leak, used to corroborate the in-JVM
-- counters printed by demo.LeakDemo.
--
-- Run this while (and after) a leak-demo run to see the statements the server
-- still holds for the dangling clients:
--
--     mysql -u root -p < tools/leakdemo/sql/monitor.sql
-- ---------------------------------------------------------------------------

-- 1. Currently open prepared statements, per connection/thread.
--    Rows appear here for every PreparedStatement that was prepared but never
--    closed (COM_STMT_CLOSE never sent). Requires useServerPrepStmts=true,
--    which the harness forces on every tracked connection.
SELECT COUNT(*) AS unclosed_prepared_statements
FROM performance_schema.prepared_statements_instances;

SELECT OWNER_THREAD_ID,
       SQL_TEXT,
       COUNT_REPREPARE,
       TIMER_PREPARE / 1000000000 AS prepare_ms
FROM performance_schema.prepared_statements_instances
ORDER BY OWNER_THREAD_ID, SQL_TEXT
LIMIT 50;

-- 2. Point-in-time server counters. Prepared_stmt_count is the number of
--    statements the server currently has prepared; the gap between
--    Com_stmt_prepare and Com_stmt_close is the leak measured server-side.
SHOW GLOBAL STATUS LIKE 'Prepared_stmt_count';
SHOW GLOBAL STATUS LIKE 'Com_stmt_prepare';
SHOW GLOBAL STATUS LIKE 'Com_stmt_close';

-- 3. Connections left open. utils.DbConnection.getConnection() opens a new
--    physical connection per call and never closes it - a separate leak that
--    commit 5785fa0 does NOT address, so this number grows in BOTH builds.
SHOW GLOBAL STATUS LIKE 'Threads_connected';
SHOW GLOBAL STATUS LIKE 'Max_used_connections';
SELECT COUNT(*) AS sessions FROM information_schema.processlist;

-- 4. Reset helper (optional): if you want the Com_stmt_* deltas to start near
--    zero between runs, restart MySQL, otherwise just compare prepare vs close.
