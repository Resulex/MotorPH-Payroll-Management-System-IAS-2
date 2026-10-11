/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;
import models.AccountAccess;
import models.User;

/**
 *
 * @author Alex Resurreccion
 */
public class AccountAccessTest {
    private AccountAccess accountAccess;

    @BeforeEach
    void setUp() {
        // Initialize with sample permissions
        List<String> permissions = Arrays.asList("VIEW_PAYROLL", "EDIT_EMPLOYEE");
        accountAccess = new AccountAccess(permissions);
    }

    @Test
    void testGetAndSetEmployeeNumber() {
        accountAccess.setEmployeeNumber(12345);
        assertEquals(12345, accountAccess.getEmployeeNumber());
    }

    @Test
    void testGetAndSetRole() {
        // Role is private, so we test indirectly by checking the getter
        accountAccess.setRole("HR Personnel");
        assertEquals("HR Personnel", accountAccess.getRole());
    }

    @Test
    void testGetAndSetIsAuthenticated() {
        // `isAuthenticated` is private, so we check using getter
        accountAccess.setIsAuthenticated(true);
        assertTrue(accountAccess.getIsAuthenticated());
    }

    @Test
    void testGetAndSetPermissions() {
        List<String> newPermissions = Arrays.asList("MANAGE_USERS", "ACCESS_REPORTS");
        accountAccess.setPermissions(newPermissions);
        assertEquals(newPermissions, accountAccess.getPermissions());
    }

    @Test
    void testLogoutUser() {
        accountAccess.setIsAuthenticated(true);
        accountAccess.setRole("HR Personnel");

        accountAccess.logoutUser(new User() {}); // Mock user object

        assertFalse(accountAccess.getIsAuthenticated());
        assertNull(accountAccess.getRole());
    }

    @Test
    void testGetAndSetEmail() {
        assertNull(accountAccess.getEmail());
        accountAccess.setEmail("employee@motorph.com");
        assertEquals("employee@motorph.com", accountAccess.getEmail());
    }

    @Test
    void testDefaultConstructorDefaults() {
        AccountAccess empty = new AccountAccess();
        assertEquals(0, empty.getEmployeeNumber());
        assertFalse(empty.getIsAuthenticated());
        assertNull(empty.getPermissions());
        assertNull(empty.getRole());
        assertNull(empty.getEmail());
    }
}
