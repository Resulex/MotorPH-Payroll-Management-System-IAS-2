package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import utils.DbConnection;

/**
 * Inserts rows into audit_logs. The insert uses the connection supplied by the
 * caller and does not commit, roll back, or change auto-commit.
 *
 * <p>When that connection has auto-commit enabled, the insert is its own commit.
 * It is not atomic with an earlier security statement on the same connection.
 * When auto-commit is disabled, the insert remains in the caller's transaction.
 *
 * <p>A failed insert throws SQLException and does not report that the event was persisted.
 */
public class AuditLogDao {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED_BAD_PASSWORD = "LOGIN_FAILED_BAD_PASSWORD";
    public static final String LOGIN_FAILED_UNKNOWN_ID = "LOGIN_FAILED_UNKNOWN_ID";
    public static final String LOGIN_FAILED_LOCKED = "LOGIN_FAILED_LOCKED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ROLE_CHANGED = "ROLE_CHANGED";

    private final Connection connection;

    public AuditLogDao(Connection connection) {
        this.connection = connection;
    }

    public AuditLogDao() {
        this(DbConnection.getConnection());
    }

    /**
     * Inserts one audit row on the supplied connection.
     *
     * @throws SQLException when the event is not persisted
     */
    public void insert(Connection connection, String eventType, Integer actorEmployeeId,
            Integer subjectEmployeeId, String attemptedEmployeeId, String details) throws SQLException {
        if (connection == null || eventType == null || eventType.isBlank()) {
            throw new SQLException("Audit event was not persisted: event type and connection are required");
        }

        String sql = "INSERT INTO audit_logs "
                + "(event_type, actor_employee_id, subject_employee_id, attempted_employee_id, details) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            setNullableInt(statement, 2, actorEmployeeId);
            setNullableInt(statement, 3, subjectEmployeeId);
            setNullableString(statement, 4, attemptedEmployeeId);
            setNullableString(statement, 5, details);
            int rowsInserted = statement.executeUpdate();
            if (rowsInserted != 1) {
                throw new SQLException("Audit event was not persisted: INSERT affected "
                        + rowsInserted + " rows for event " + eventType);
            }
        } catch (SQLException failure) {
            if (failure.getMessage() != null && failure.getMessage().startsWith("Audit event was not persisted")) {
                throw failure;
            }
            throw new SQLException("Audit event was not persisted: " + eventType, failure);
        }
    }

    private static void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
