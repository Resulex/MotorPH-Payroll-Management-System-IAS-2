/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import utils.DbConnection;
import models.AccountAccess;
import java.sql.PreparedStatement;
/**
 *
 * @author Alex Resurreccion
 */
public class AccountAccessDao {
    private Connection connection;

    // Constructor with custom connection
    public AccountAccessDao(Connection connection) {
        this.connection = connection;
    }

    // Constructor with default connection
    public AccountAccessDao() {
        this.connection = DbConnection.getConnection();
    }
    
    /**
     * This method handles user login by verifying the employee number and password.
     * If the credentials match, it retrieves the user's role and permissions.
     * 
     * @param employeeNumber The employee number of the user trying to log in
     * @param password The password entered by the user
     * @return true if login is successful, false otherwise
     */
    public AccountAccess login(int employeeNumber, String password) {
        // Define the query
        String query = "SELECT employee_id, password, role_name " +
           "FROM user_credentials uc " +
           "JOIN roles r ON r.role_id = uc.role_id " +
           "WHERE employee_id = ? AND password = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, employeeNumber);
            preparedStatement.setString(2, password);

            // Execute the query
            try (ResultSet result = preparedStatement.executeQuery()) {
                AccountAccess account = new AccountAccess();
                if (result.next()) {
                    // Populate model object from DB data
                    account.setEmployeeNumber(result.getInt("employee_id"));
                    account.setRole(result.getString("role_name"));
                } else {
                    return null;
                }

                // Get list of permissions
                String permQuery = """
                        SELECT CONCAT(
                            p.module,
                            CASE
                                WHEN p.submodule IS NOT NULL THEN CONCAT('-', p.submodule)
                                ELSE ''
                            END
                        ) AS permission FROM user_credentials uc
                        JOIN roles r ON r.role_id = uc.role_id
                        JOIN role_permissions rp ON r.role_id = rp.role_id
                        JOIN permissions p ON p.permission_id = rp.permission_id
                        WHERE employee_id = ?""";

                // Execute the query
                try (PreparedStatement permStmt = connection.prepareStatement(permQuery)) {
                    permStmt.setInt(1, employeeNumber);
                    try (ResultSet permResult = permStmt.executeQuery()) {
                        List<String> resultPermissions = new ArrayList<>();
                        while (permResult.next()) {
                            resultPermissions.add(permResult.getString("permission"));
                        }
                        account.setIsAuthenticated(true);
                        account.setPermissions(resultPermissions);
                        return account;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }

        return null;
    }
    
    public AccountAccess getAccountEmail(int employeeId){
        // Define the query
        String query = "SELECT email FROM user_credentials " +
           "WHERE employee_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, employeeId);

            // Execute the query
            try (ResultSet result = preparedStatement.executeQuery()) {
                AccountAccess account = new AccountAccess();
                if (result.next()) {
                    account.setEmail(result.getString("email"));
                    return account;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }

        return null;
    }
}
