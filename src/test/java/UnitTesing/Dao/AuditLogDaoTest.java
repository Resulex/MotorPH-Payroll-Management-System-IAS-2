package UnitTesing.Dao;

import dao.AuditLogDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.DbConnection;

/**
 * Audit inserts use the caller-supplied connection and roll back with that connection.
 */
public class AuditLogDaoTest {
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        connection = DbConnection.getConnectionWithTransaction();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    public void insertPersistsOnCallerConnectionUntilRollback() throws SQLException {
        AuditLogDao auditLogDao = new AuditLogDao(connection);
        auditLogDao.insert(connection, AuditLogDao.LOGIN_SUCCESS, 10001, 10008, "10008", "role=Employee");
        assertEquals(1, countEvents(AuditLogDao.LOGIN_SUCCESS, "10008"));
        connection.rollback();
        assertEquals(0, countEvents(AuditLogDao.LOGIN_SUCCESS, "10008"));
    }

    @Test
    public void insertFailureIsReportedAndDoesNotClaimPersistence() {
        AuditLogDao auditLogDao = new AuditLogDao(connection);
        SQLException failure = assertThrows(SQLException.class, () ->
                auditLogDao.insert(connection, null, null, null, "not-persisted", "reason=test"));
        assertTrue(failure.getMessage().contains("not persisted"));
    }

    private int countEvents(String eventType, String attemptedEmployeeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM audit_logs WHERE event_type = ? AND attempted_employee_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            statement.setString(2, attemptedEmployeeId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }
}
