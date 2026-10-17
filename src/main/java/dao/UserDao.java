/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import models.Employee;
import models.HRPersonnel;
import models.SystemAdministrator;
import models.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utils.DbConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alex Resurreccion
 */
public class UserDao {
    private Connection conn;
    private final AuditLogDao auditLogDao;

    // Kept your original constructor
    public UserDao(Connection conn) {
        this.conn = conn;
        this.auditLogDao = new AuditLogDao(conn);
    }

    // Kept your original constructor
    public UserDao() {
        this(DbConnection.getConnection());
    }

    /**
     * Authenticates a user and handles login attempts.
     * @param employeeId The user's employee ID.
     * @param password The user's password.
     * @return A User object if successful, null if failed, locked, or user not found.
     */
    public User authenticate(String employeeId, String password) {
        if (isAccountLocked(employeeId)) {
            System.out.println("Authentication failed: Account is locked for employee ID " + employeeId);
            recordLockedLoginAttempt(employeeId);
            return null;
        }

        // The SQL joins the user_credentials with roles to get the role_name
        String sql = "SELECT uc.password, r.role_name " +
                     "FROM user_credentials uc " +
                     "JOIN roles r ON uc.role_id = r.role_id " +
                     "WHERE uc.employee_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // User found, now check the password
                    if (rs.getString("password").equals(password)) {
                        // Password is correct. Reset login attempts.
                        resetLoginAttempts(employeeId);

                        String roleName = rs.getString("role_name");
                        User user; // Declare as the abstract User type

                        // Instantiate the correct concrete class based on the role name.
                        switch (roleName) {
                            case "Employee":
                                user = new Employee(Integer.parseInt(employeeId));
                                break;
                            case "HR Personnel":
                                user = new HRPersonnel(Integer.parseInt(employeeId));
                                break;
                            case "System Admin":
                                user = new SystemAdministrator(Integer.parseInt(employeeId));
                                break;
                            default:
                                // If the role from the DB is unknown, treat as a failure.
                                System.err.println("Unknown role encountered: " + roleName);
                                return null;
                        }
                        insertAuditEvent(AuditLogDao.LOGIN_SUCCESS, null, user.getEmployeeID(),
                                employeeId, "role=" + roleName);
                        return user; // Return the created Employee, HR, or Admin object
                    } else {
                        // Password was incorrect. Increment login attempts.
                        incrementLoginAttempts(employeeId);
                        insertAuditEvent(AuditLogDao.LOGIN_FAILED_BAD_PASSWORD, null,
                                knownSubjectId(employeeId), employeeId, "reason=incorrect_password");
                        return null;
                    }
                } else {
                    // Employee ID not found in the database.
                    insertAuditEvent(AuditLogDao.LOGIN_FAILED_UNKNOWN_ID, null, null,
                            employeeId, "reason=unknown_employee_id");
                    return null;
                }
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Error during authentication: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Increments the login attempt counter for a user. Locks the account if attempts exceed the limit.
     */
    public void incrementLoginAttempts(String employeeId) {
        String updateSql = "UPDATE user_credentials SET login_attempts = login_attempts + 1 WHERE employee_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, employeeId);
            pstmt.executeUpdate();
            // After incrementing, check if the account should be locked
            lockAccountIfNecessary(employeeId);
        } catch (SQLException e) {
            System.err.println("Error incrementing login attempts: " + e.getMessage());
        }
    }
    
    /**
     * Checks the number of login attempts and locks the account if it reaches the threshold (3).
     */
    private void lockAccountIfNecessary(String employeeId) {
        if (getLoginAttempts(employeeId) >= 3) {
            String lockSql = "UPDATE user_credentials SET is_locked = TRUE WHERE employee_id = ? AND is_locked = FALSE";
            try (PreparedStatement pstmt = conn.prepareStatement(lockSql)) {
                pstmt.setString(1, employeeId);
                int lockedRows = pstmt.executeUpdate();
                if (lockedRows > 0) {
                    System.out.println("Account locked for employee ID: " + employeeId);
                    insertAuditEvent(AuditLogDao.ACCOUNT_LOCKED, null, knownSubjectId(employeeId),
                            employeeId, "reason=lockout_threshold_reached");
                }
            } catch (SQLException e) {
                 System.err.println("Error locking account: " + e.getMessage());
            }
        }
    }

    /**
     * Records one already-locked login attempt.
     * LoginPage calls this and returns without calling authenticate, so that attempt is recorded once.
     * A failed insert is reported and is not treated as persisted. This method does not change the lock.
     */
    public void recordLockedLoginAttempt(String employeeId) {
        insertAuditEvent(AuditLogDao.LOGIN_FAILED_LOCKED, null, knownSubjectId(employeeId),
                employeeId, "reason=account_locked");
    }

    /**
     * Resets login attempts to 0 upon a successful login.
     */
    public void resetLoginAttempts(String employeeId) {
        String sql = "UPDATE user_credentials SET login_attempts = 0 WHERE employee_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employeeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error resetting login attempts: " + e.getMessage());
        }
    }

    /**
     * Checks if an account is currently locked.
     * @return true if the account is locked, false otherwise.
     */
    public boolean isAccountLocked(String employeeId) {
        String sql = "SELECT is_locked FROM user_credentials WHERE employee_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_locked");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if account is locked: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Retrieves the current number of failed login attempts for a user.
     */
    public int getLoginAttempts(String employeeId) {
        String sql = "SELECT login_attempts FROM user_credentials WHERE employee_id = ?";
         try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("login_attempts");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting login attempts: " + e.getMessage());
        }
        return 0;
    }

    // --- Admin Methods ---

    /**
     * Unlocks an account and resets its login attempts. (For Admin use)
     */
    public void unlockAccount(String employeeId) {
        String sql = "UPDATE user_credentials SET is_locked = FALSE, login_attempts = 0 WHERE employee_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, employeeId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error unlocking account: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves a list of all employee IDs whose accounts are locked. (For Admin use)
     */
    public List<String> getLockedAccounts() {
        List<String> lockedAccounts = new ArrayList<>();
        String sql = "SELECT employee_id FROM user_credentials WHERE is_locked = TRUE";
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                lockedAccounts.add(rs.getString("employee_id"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving locked accounts: " + e.getMessage());
        }
        return lockedAccounts;
    }
    
    /**
     * Retrieves a list of all users with their name and lock status for display.
     * @return A list of UserDetails objects.
     */
    public List<UserDetails> getAllUsersWithLockStatus() {
        List<UserDetails> users = new ArrayList<>();
        String sql = "SELECT e.employee_id, e.first_name, e.last_name, uc.is_locked " +
                     "FROM employees e " +
                     "JOIN user_credentials uc ON e.employee_id = uc.employee_id";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String employeeId = rs.getString("employee_id");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                boolean isLocked = rs.getBoolean("is_locked");
                users.add(new UserDetails(employeeId, name, isLocked));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user details: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }
    
    // --- YOUR ORIGINAL METHOD - REFACTORED FOR SECURITY ---

    /**
     * Gets the role name of a user from the database.
     * This method is now safer, using a PreparedStatement.
     */
    public String getRoleName(int employeeId) {
        String query = "SELECT r.role_name " +
                       "FROM user_credentials uc " +
                       "JOIN roles r ON r.role_id = uc.role_id " +
                       "WHERE uc.employee_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, employeeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting role name: " + e.getMessage());
            e.printStackTrace();
        }
        return ""; // Return empty string if not found
    }
    
    public User getUserInfo(int employeeId, String role, boolean isAuth){
        User user = null;
        
        // don't fetch info if not yet login
        if(employeeId == 0 || isAuth == false){
            return user;
        }
        // Define the query
        String query = "SELECT * FROM employee_view " 
                + "WHERE employee_id = ?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(query)) {
            preparedStatement.setInt(1, employeeId);

            // Execute the query
            try (ResultSet rs = preparedStatement.executeQuery()) {
                // If user exists, populate user information based on role
                if (rs.next()) {

                    if ("HR Personnel".equals(role)) {
                        user = new HRPersonnel();
                    } else if ("System Admin".equals(role)) {
                        user = new SystemAdministrator();
                    } else {
                        user = new Employee(); // Assign Employee role
                    }

                    DaoMappers.populateUser(rs, user);
                } else {
                    System.out.println("No user");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return user; // Return the populated user object or null if no data found
    }

    /**
     * Writes one audit row on this DAO's connection. The insert does not commit.
     * With auto-commit enabled, the insert is a separate commit from the security statement.
     * A failed insert is reported here and is not treated as persisted. Authentication and
     * lockout results are unchanged when the insert fails.
     */
    private void insertAuditEvent(String eventType, Integer actorEmployeeId, Integer subjectEmployeeId,
            String attemptedEmployeeId, String details) {
        try {
            auditLogDao.insert(conn, eventType, actorEmployeeId, subjectEmployeeId, attemptedEmployeeId, details);
        } catch (SQLException failure) {
            String cause = failure.getCause() == null ? "" : " " + failure.getCause().getMessage();
            System.err.println("Audit event was not persisted: eventType=" + eventType
                    + " attemptedEmployeeId=" + attemptedEmployeeId
                    + ". " + failure.getMessage() + cause);
        }
    }

    private Integer knownSubjectId(String employeeId) {
        try {
            return Integer.valueOf(employeeId);
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }
}

/**
 * A simple data container class to hold user information for the JTable.
 */
class UserDetails {
    private final String employeeId;
    private final String name;
    private final boolean isLocked;

    public UserDetails(String employeeId, String name, boolean isLocked) {
        this.employeeId = employeeId;
        this.name = name;
        this.isLocked = isLocked;
    }

    public String getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public boolean isLocked() { return isLocked; }
}


