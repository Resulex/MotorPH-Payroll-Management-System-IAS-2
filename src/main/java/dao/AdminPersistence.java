/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.logging.Level;
import java.util.logging.Logger;
import models.Benefits;
import models.User;

/**
 * Shared persistence operations for the administrative DAOs.
 *
 * @author Alex Resurreccion
 */
final class AdminPersistence {

    private static final Logger LOGGER = Logger.getLogger(AdminPersistence.class.getName());

    private AdminPersistence() {
    }

    /**
     * Inserts a new employee (address, salary, employees, gov_id and user_credentials)
     * within a single transaction. Returns the user_credentials rows affected.
     */
    static int insertEmployee(Connection connection, User employee, int roleId) throws SQLException {
        int rowsAffected = 0;

        boolean initialCommit = connection.getAutoCommit();
        connection.setAutoCommit(false); // Transaction start

        try {
            int addressId = 0;
            // 1. Insert into address
            String addressQuery = "INSERT INTO address (street, barangay, city, province, zipcode) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement psAddress = connection.prepareStatement(addressQuery, Statement.RETURN_GENERATED_KEYS)) {
                psAddress.setString(1, employee.getAddress().getStreet());
                psAddress.setString(2, employee.getAddress().getBarangay());
                psAddress.setString(3, employee.getAddress().getCity());
                psAddress.setString(4, employee.getAddress().getProvince());
                psAddress.setString(5, employee.getAddress().getZipcode());
                psAddress.executeUpdate();
                addressId = readGeneratedId(psAddress);
            }

            int salaryId = 0;
            // 2. Insert into salary
            String salaryQuery = "INSERT INTO salary (basic_salary, gross_semi_monthly_rate, hourly_rate, effective_date) VALUES (?, ?, ?, CURDATE())";
            try (PreparedStatement psSalary = connection.prepareStatement(salaryQuery, Statement.RETURN_GENERATED_KEYS)) {
                Benefits benefit = employee.getBenefits();
                psSalary.setDouble(1, benefit.getBasicSalary());
                psSalary.setDouble(2, benefit.getGrossSemiMonthlyRate());
                psSalary.setDouble(3, benefit.getHourlyRate());
                psSalary.executeUpdate();
                salaryId = readGeneratedId(psSalary);
            }

            int employeeId = 0;
            // 3. Insert into employees
            String empQuery = "INSERT INTO employees (last_name, first_name, phone_number, birthdate, is_deleted, date_hired, salary_id, supervisor_id, status_id, position_id, address_id) VALUES (?, ?, ?, ?, 0, CURDATE(), ?, ?, ?, ?, ?)";
            try (PreparedStatement psEmp = connection.prepareStatement(empQuery, Statement.RETURN_GENERATED_KEYS)) {
                psEmp.setString(1, employee.getLastName());
                psEmp.setString(2, employee.getFirstName());
                psEmp.setString(3, employee.getPhoneNumber());
                psEmp.setDate(4, java.sql.Date.valueOf(employee.getBirthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));
                psEmp.setInt(5, salaryId);
                psEmp.setObject(6, employee.getImmediateSupervisor(), java.sql.Types.INTEGER);
                psEmp.setInt(7, DaoMappers.getStatusId(connection, employee.getStatus()));
                psEmp.setInt(8, DaoMappers.getPositionId(connection, employee.getPosition()));
                psEmp.setInt(9, addressId);
                psEmp.executeUpdate();
                employeeId = readGeneratedId(psEmp);
            }

            // 4. Insert into gov_id
            String govQuery = "INSERT INTO gov_id (sss_num, tin, philhealth, pagibig, employee_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement psGov = connection.prepareStatement(govQuery)) {
                psGov.setString(1, employee.getSssNumber());
                psGov.setString(2, employee.getTinNumber());
                psGov.setString(3, employee.getPhilhealthNumber());
                psGov.setString(4, employee.getPagibigNumber());
                psGov.setInt(5, employeeId);
                psGov.executeUpdate();
            }

            // 5. Insert into user_credentials
            String credQuery = "INSERT INTO user_credentials (employee_id, password, role_id) VALUES (?, ?, ?)";
            try (PreparedStatement psCred = connection.prepareStatement(credQuery)) {
                psCred.setInt(1, employeeId);
                psCred.setString(2, employee.getLastName() + "123");
                psCred.setInt(3, roleId);
                rowsAffected = psCred.executeUpdate();
            }

            if (initialCommit) {
                connection.commit(); // Commit if all succeeded
                connection.setAutoCommit(initialCommit);
            }
        } catch (SQLException e) {
            try {
                connection.rollback(); // Rollback if error
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Rollback failed", ex);
            }
            throw e;
        }

        return rowsAffected;
    }

    /**
     * Updates the address, salary and gov_id rows of an existing employee.
     * Returns the gov_id rows affected.
     */
    static int updateAddressSalaryGov(Connection connection, User employee) throws SQLException {
        String addressQuery = "UPDATE address SET street = ?, barangay = ?, "
                + "city = ?, province = ?, zipcode = ? "
                + "WHERE address_id = ?";
        try (PreparedStatement psAddress = connection.prepareStatement(addressQuery, Statement.RETURN_GENERATED_KEYS)) {
            psAddress.setString(1, employee.getAddress().getStreet());
            psAddress.setString(2, employee.getAddress().getBarangay());
            psAddress.setString(3, employee.getAddress().getCity());
            psAddress.setString(4, employee.getAddress().getProvince());
            psAddress.setString(5, employee.getAddress().getZipcode());
            psAddress.setInt(6, DaoMappers.getEmployeeAddressId(connection, employee.getEmployeeID()));
            psAddress.executeUpdate();
        }

        String salaryQuery = "UPDATE salary SET basic_salary = ?, "
                + "gross_semi_monthly_rate = ?, hourly_rate = ? "
                + "WHERE salary_id = ?";
        try (PreparedStatement psSalary = connection.prepareStatement(salaryQuery, Statement.RETURN_GENERATED_KEYS)) {
            Benefits benefit = employee.getBenefits();
            psSalary.setDouble(1, benefit.getBasicSalary());
            psSalary.setDouble(2, benefit.getGrossSemiMonthlyRate());
            psSalary.setDouble(3, benefit.getHourlyRate());
            psSalary.setInt(4, DaoMappers.getEmployeeSalaryId(connection, employee.getEmployeeID()));
            psSalary.executeUpdate();
        }

        String govQuery = "UPDATE gov_id SET sss_num = ?, philhealth = ?, tin = ?, pagibig = ? WHERE employee_id = ?";
        try (PreparedStatement govStmt = connection.prepareStatement(govQuery)) {
            govStmt.setString(1, employee.getSssNumber());
            govStmt.setString(2, employee.getPhilhealthNumber());
            govStmt.setString(3, employee.getTinNumber());
            govStmt.setString(4, employee.getPagibigNumber());
            govStmt.setInt(5, employee.getEmployeeID());

            LOGGER.log(Level.FINE, "Executing Gov ID Update: {0}", govStmt);
            return govStmt.executeUpdate();
        }
    }

    /**
     * Soft-deletes an employee by setting their is_deleted flag.
     */
    static boolean softDeleteEmployee(Connection connection, int employeeId) throws SQLException {
        String query = "UPDATE employees SET is_deleted = ? WHERE employee_id = ? AND is_deleted = False";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setBoolean(1, true);
            preparedStatement.setInt(2, employeeId);
            return preparedStatement.executeUpdate() == 1;
        }
    }

    /**
     * Reads the auto-generated key produced by an insert, or 0 if none was returned.
     */
    private static int readGeneratedId(PreparedStatement statement) throws SQLException {
        try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
            return generatedKeys.next() ? generatedKeys.getInt(1) : 0;
        }
    }
}
