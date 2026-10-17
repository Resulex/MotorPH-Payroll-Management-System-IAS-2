/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing.Dao;

import models.Employee;
import models.HRPersonnel;
import models.SystemAdministrator;
import models.User;
import dao.AuditLogDao;
import dao.UserDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.DbConnection;

/**
 *
 * @author Alex Resurreccion
 */
public class UserDaoTest {
    UserDao userDao = null;
    Connection connection;
    
    @BeforeEach
    public void setUp() throws SQLException {
        connection = DbConnection.getConnectionWithTransaction();
        userDao = new UserDao(connection);
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.rollback();  // Roll back everything done in the test
            connection.close();
        }
    }
    
    @Test
    public void getRoleNameEmployeeSuccess() {
        System.out.println("Test for Get Role Name Employee - Success");
        String roleName = userDao.getRoleName(10008);
        assertEquals("Employee", roleName);
        System.out.println("Successfully get role name for employee");
    }
    
    @Test
    public void getRoleNameHRPersonnelSuccess() {
        System.out.println("Test for Get Role HR Personnel - Success");
        String roleName = userDao.getRoleName(10007);
        assertEquals("HR Personnel", roleName);
        System.out.println("Successfully get role name for employee");
    }
    
    @Test
    public void getRoleNameSystemAdminSuccess() {
        System.out.println("Test for Get Role Name System Admin - Success");
        String roleName = userDao.getRoleName(10001);
        assertEquals("System Admin", roleName);
        System.out.println("Successfully get role name for employee");
    }
    
    @Test
    public void getUserInfoSuccess() {
        System.out.println("Test for Get User Info - Success");
        User user = userDao.getUserInfo(10008, "Employee", true);
        assertNotNull(user);
        System.out.println(
                user.getEmployeeID() + " - " 
                + user.getFirstName() + " " 
                + user.getLastName());
        System.out.println("Successfully get user info for employee 10008");
    }
    
    @Test
    public void getUserInfoUnauthenticated() {
        System.out.println("Test for Get User Info - Unauthenticated");
        User user = userDao.getUserInfo(10008, "Employee", false);
        assertNull(user);
        System.out.println("Can't retrieve user info for unauthenticated user");
    }
    
    @Test
    public void authenticateEmployeeSuccess() {
        User user = userDao.authenticate("10008", "Romualdez08");
        assertNotNull(user);
        assertTrue(user instanceof Employee);
    }
    
    @Test
    public void authenticateHRPersonnelSuccess() {
        User user = userDao.authenticate("10007", "San Jose07");
        assertNotNull(user);
        assertTrue(user instanceof HRPersonnel);
    }
    
    @Test
    public void authenticateSystemAdminSuccess() {
        User user = userDao.authenticate("10001", "Garcia01");
        assertNotNull(user);
        assertTrue(user instanceof SystemAdministrator);
    }
    
    @Test
    public void authenticateWrongPassword() {
        User user = userDao.authenticate("10008", "wrong-password");
        assertNull(user);
    }
    
    @Test
    public void authenticateUnknownUser() {
        User user = userDao.authenticate("9999999", "whatever");
        assertNull(user);
    }
    
    @Test
    public void isAccountLockedInitiallyFalse() {
        assertFalse(userDao.isAccountLocked("10008"));
    }
    
    @Test
    public void incrementLoginAttemptsAndLockAccount() {
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        assertEquals(3, userDao.getLoginAttempts("10008"));
        assertTrue(userDao.isAccountLocked("10008"));
        assertTrue(userDao.getLockedAccounts().contains("10008"));
    }
    
    @Test
    public void authenticateLockedAccountReturnsNull() {
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        assertTrue(userDao.isAccountLocked("10008"));
        assertNull(userDao.authenticate("10008", "Romualdez08"));
    }
    
    @Test
    public void unlockAccountResetsLockAndAttempts() {
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        assertTrue(userDao.isAccountLocked("10008"));
        userDao.unlockAccount("10008");
        assertFalse(userDao.isAccountLocked("10008"));
        assertEquals(0, userDao.getLoginAttempts("10008"));
    }
    
    @Test
    public void resetLoginAttempts() {
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        assertEquals(2, userDao.getLoginAttempts("10008"));
        userDao.resetLoginAttempts("10008");
        assertEquals(0, userDao.getLoginAttempts("10008"));
    }
    
    @Test
    public void getLockedAccountsReturnsList() {
        List<String> locked = userDao.getLockedAccounts();
        assertNotNull(locked);
    }
    
    @Test
    public void getAllUsersWithLockStatusReturnsUsers() {
        List<?> users = userDao.getAllUsersWithLockStatus();
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    public void successfulLoginWritesOneAuditEventWithoutPassword() throws SQLException {
        User user = userDao.authenticate("10008", "Romualdez08");
        assertNotNull(user);
        assertEquals(1, countAuditEvents(AuditLogDao.LOGIN_SUCCESS, "10008"));
        assertEquals("role=Employee", auditDetails(AuditLogDao.LOGIN_SUCCESS, "10008"));
        assertEquals(Integer.valueOf(10008), auditSubjectId(AuditLogDao.LOGIN_SUCCESS, "10008"));
        assertSecretAbsent("Romualdez08");
    }

    @Test
    public void incorrectPasswordWritesOneFailureAndDoesNotLock() throws SQLException {
        assertNull(userDao.authenticate("10008", "wrong-password"));
        assertEquals(1, countAuditEvents(AuditLogDao.LOGIN_FAILED_BAD_PASSWORD, "10008"));
        assertEquals(0, countAuditEvents(AuditLogDao.ACCOUNT_LOCKED, "10008"));
        assertEquals("reason=incorrect_password", auditDetails(AuditLogDao.LOGIN_FAILED_BAD_PASSWORD, "10008"));
        assertSecretAbsent("wrong-password");
    }

    @Test
    public void unknownEmployeeIdWritesOneAuditEventWithoutSubject() throws SQLException {
        assertNull(userDao.authenticate("9999999", "whatever"));
        assertEquals(1, countAuditEvents(AuditLogDao.LOGIN_FAILED_UNKNOWN_ID, "9999999"));
        assertNull(auditSubjectId(AuditLogDao.LOGIN_FAILED_UNKNOWN_ID, "9999999"));
        assertEquals("reason=unknown_employee_id", auditDetails(AuditLogDao.LOGIN_FAILED_UNKNOWN_ID, "9999999"));
        assertSecretAbsent("whatever");
    }

    @Test
    public void alreadyLockedAccountWritesLockedEventWithoutAnotherLockout() throws SQLException {
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        assertTrue(userDao.isAccountLocked("10008"));
        assertEquals(1, countAuditEvents(AuditLogDao.ACCOUNT_LOCKED, "10008"));
        assertNull(userDao.authenticate("10008", "Romualdez08"));
        assertEquals(1, countAuditEvents(AuditLogDao.LOGIN_FAILED_LOCKED, "10008"));
        assertEquals(1, countAuditEvents(AuditLogDao.ACCOUNT_LOCKED, "10008"));
        assertEquals("reason=account_locked", auditDetails(AuditLogDao.LOGIN_FAILED_LOCKED, "10008"));
        assertSecretAbsent("Romualdez08");
    }

    @Test
    public void thirdIncorrectPasswordRecordsLockoutOnce() throws SQLException {
        userDao.incrementLoginAttempts("10008");
        userDao.incrementLoginAttempts("10008");
        assertNull(userDao.authenticate("10008", "wrong-password"));
        assertTrue(userDao.isAccountLocked("10008"));
        assertEquals(1, countAuditEvents(AuditLogDao.LOGIN_FAILED_BAD_PASSWORD, "10008"));
        assertEquals(1, countAuditEvents(AuditLogDao.ACCOUNT_LOCKED, "10008"));
        assertEquals("reason=lockout_threshold_reached", auditDetails(AuditLogDao.ACCOUNT_LOCKED, "10008"));
        assertSecretAbsent("wrong-password");
    }

    private int countAuditEvents(String eventType, String attemptedEmployeeId) throws SQLException {
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

    private String auditDetails(String eventType, String attemptedEmployeeId) throws SQLException {
        String sql = "SELECT details FROM audit_logs WHERE event_type = ? AND attempted_employee_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            statement.setString(2, attemptedEmployeeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("details") : null;
            }
        }
    }

    private Integer auditSubjectId(String eventType, String attemptedEmployeeId) throws SQLException {
        String sql = "SELECT subject_employee_id FROM audit_logs WHERE event_type = ? AND attempted_employee_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            statement.setString(2, attemptedEmployeeId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return (Integer) result.getObject("subject_employee_id");
            }
        }
    }

    private void assertSecretAbsent(String secret) throws SQLException {
        String sql = "SELECT event_type, attempted_employee_id, details FROM audit_logs";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                assertFalse(containsSecret(result.getString("event_type"), secret));
                assertFalse(containsSecret(result.getString("attempted_employee_id"), secret));
                assertFalse(containsSecret(result.getString("details"), secret));
            }
        }
    }

    private boolean containsSecret(String value, String secret) {
        return value != null && value.contains(secret);
    }
}
