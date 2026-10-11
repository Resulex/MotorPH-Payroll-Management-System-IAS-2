/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing.Dao;

import models.Employee;
import models.HRPersonnel;
import models.SystemAdministrator;
import models.User;
import dao.UserDao;
import java.sql.Connection;
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
}
