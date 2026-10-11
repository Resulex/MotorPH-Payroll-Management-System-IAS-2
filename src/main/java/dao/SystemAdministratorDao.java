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
import java.util.ArrayList;
import java.util.List;
import utils.DbConnection;

/**
 *
 * @author Alex Resurreccion
 */
public class SystemAdministratorDao {
    private Connection connection;

    // Constructor with custom connection
    public SystemAdministratorDao(Connection connection) {
        this.connection = connection;
    }

    // Constructor with default connection
    public SystemAdministratorDao() {
        this.connection = DbConnection.getConnection();
    }
    
    // Add a new employee to the system.
    public boolean addNewEmployee(User employee) {
        try {
            return AdminPersistence.insertEmployee(connection, employee, 1) == 1;
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
            return false;
        }
    }
    
    // Update an existing employee in the system
    public boolean updateEmployee(User employee) {
        int rowsAffected = 0;

        try {
            // Base employee update (shared for all types of users)
            String query = "UPDATE employees SET last_name = ?, first_name = ?, birthdate = ?, phone_number = ?";

            if (employee instanceof Employee) {
                query += ", status_id = ?, position_id = ?, supervisor_id = ?";
            }

            query += " WHERE employee_id = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, employee.getLastName());
                preparedStatement.setString(2, employee.getFirstName());
                preparedStatement.setDate(3, new java.sql.Date(employee.getBirthday().getTime()));
                preparedStatement.setString(4, employee.getPhoneNumber());

                if (employee instanceof Employee emp) {
                    preparedStatement.setInt(5, DaoMappers.getStatusId(connection, emp.getStatus()));
                    preparedStatement.setInt(6, DaoMappers.getPositionId(connection, emp.getPosition()));
                    if (emp.getImmediateSupervisor() != 0) {
                        preparedStatement.setObject(7, employee.getImmediateSupervisor(), java.sql.Types.INTEGER); // Nullable
                    } else {
                        preparedStatement.setNull(7, java.sql.Types.INTEGER);
                    }
//                preparedStatement.setInt(8, getSalaryId(emp.getSalary()));
//                preparedStatement.setInt(9, emp.getAddressId());
                    preparedStatement.setInt(8, emp.getEmployeeID());
                } else {
                    preparedStatement.setInt(5, employee.getEmployeeID()); // employee_id
                }

                System.out.println("Executing Employee Update: " + preparedStatement);
                preparedStatement.executeUpdate();
            }

            AdminPersistence.updateAddressSalaryGov(connection, employee);
            
            int roleId = (employee instanceof HRPersonnel) ? 2 :
                         (employee instanceof SystemAdministrator) ? 3 : 1;

            String credQuery = "UPDATE user_credentials SET role_id = ? WHERE employee_id = ?";
            try (PreparedStatement credStmt = connection.prepareStatement(credQuery)) {
                credStmt.setInt(1, roleId);
                credStmt.setInt(2, employee.getEmployeeID());

                System.out.println("Executing Role Update: " + credStmt);
                rowsAffected = credStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating employees table");
            e.printStackTrace();
        }
        
        System.out.println(rowsAffected);
        return rowsAffected == 1;
    }

    
    // Delete an employee from the system by their employee number
    public boolean deleteEmployee(User employee) {
        System.out.println("deleteEmployeeByNumber");
        try {
            return AdminPersistence.softDeleteEmployee(connection, employee.getEmployeeID());
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
            return false;
        }
    }
    
    // Load the list of employees (no search criteria)
    public List<User> loadEmployeeList(){
        try {
            // Define the query
            String query = "SELECT e.*, uc.role_id FROM employee_view e "
                    + "LEFT JOIN user_credentials uc "
                    + "ON e.employee_id = uc.employee_id "
                    + "WHERE e.is_deleted = False";

            // Execute the query
            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet result = preparedStatement.executeQuery()) {
                List<User> employees = mapEmployees(result);
                return employees;
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return null;
    }
    
    // Load the list of employees with a search filter
    public List<User> loadEmployeeList(String search){
        try {
            // Define the query
            String query = "SELECT e.*, uc.role_id FROM employee_view e LEFT JOIN user_credentials uc "
                    + "ON e.employee_id = uc.employee_id WHERE "
                    + "(e.employee_id LIKE ? OR first_name LIKE ? OR "
                    + "last_name LIKE ?) AND e.is_deleted = False";
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                // Setting parameters
                preparedStatement.setString(1, "%" + search + "%");
                preparedStatement.setString(2, "%" + search + "%");
                preparedStatement.setString(3, "%" + search + "%");

                // Execute the query
                try (ResultSet result = preparedStatement.executeQuery()) {
                    List<User> employees = mapEmployees(result);
                    return employees;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return null;
    }
    
    public List<User> loadNewUsers(){
        try {
            // Define the query
            String query = "SELECT e.*, uc.role_id FROM employee_view e "
                    + "LEFT JOIN user_credentials uc "
                    + " ON e.employee_id = uc.employee_id "
                    + "WHERE date_hired >= CURDATE() - INTERVAL 7 DAY ";
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet result = preparedStatement.executeQuery()) {
                List<User> employees = mapEmployees(result);
                return employees;
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            System.out.println("SQL Error Message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public List<Integer> loadDashboard(){
        try {
            // Define the query
            String query = "SELECT COUNT(*) AS total, " 
                    + " COUNT(CASE WHEN date_hired >= NOW() - INTERVAL 1 WEEK THEN 1 END) "
                    + " AS new_employees FROM employees e ";
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet result = preparedStatement.executeQuery()) {
                List<Integer> dashboardInfo = new ArrayList<>();
                if (result.next()) {
                    dashboardInfo.add(result.getInt("total"));
                    dashboardInfo.add(result.getInt("new_employees"));
                }
                return dashboardInfo;
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            System.out.println("SQL Error Message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // Unlocks a user's account and resets their login attempts.
    public boolean unlockUserAccount(int employeeId) {
        String sql = "UPDATE user_credentials SET is_locked = FALSE, login_attempts = 0 WHERE employee_id = ?";
        // Uses the existing 'connection' object. Does not create/close its own.
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, employeeId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0; // Return true if the update was successful
        } catch (SQLException e) {
            System.err.println("Error unlocking account: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Private helper method to map ResultSet to a list of User objectsHR
    private List<User> mapEmployees(ResultSet result) throws SQLException {
        List<User> employees = new ArrayList<>();
        while (result.next()) {
            User user = result.getInt("role_id") == 3
                    ? new SystemAdministrator() : 
                    result.getInt("role_id") == 2 ? 
                    new HRPersonnel() : new Employee();
            System.out.println(result.getInt("employee_id"));
            DaoMappers.populateUser(result, user);
            employees.add(user);
        }
        return employees;
    }
}
