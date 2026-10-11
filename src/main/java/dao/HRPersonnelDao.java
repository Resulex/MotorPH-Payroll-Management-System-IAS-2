/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import models.AttendanceRecord;
import models.Employee;
import models.HRPersonnel;
import models.LeaveRequest;
import models.Payroll;
import models.SystemAdministrator;
import models.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.DbConnection;

/**
 *
 * @author Alex Resurreccion
 */
public class HRPersonnelDao {
    private Connection connection;

    // Constructor with custom connection
    public HRPersonnelDao(Connection connection) {
        this.connection = connection;
    }

    // Constructor with default connection
    public HRPersonnelDao() {
        this.connection = DbConnection.getConnection();
    }
    
    /**
     * Approves a leave request for an employee.
     * 
     * @param leaveRequest The leave request to approve.
     * @return true if leave request is approved, false otherwise.
     */
    public boolean approveLeaveRequest(LeaveRequest leaveRequest, HRPersonnel hrPersonnel) {
        
        int rowsAffected = 0;
        try {
            // Define the query
            String query = "UPDATE leave_records " +
               "SET approved_by = ?, remarks = 'APPROVED' " +
               "WHERE id = ? AND remarks = 'PENDING'";

            // Prepare the statement
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                // Set the parameters
                preparedStatement.setInt(1, hrPersonnel.getEmployeeID());
                preparedStatement.setInt(2, leaveRequest.getLeaveID());
                // Execute the query
                rowsAffected = preparedStatement.executeUpdate();
            }
            if(rowsAffected == 1) {
                leaveRequest.setApprovedBy(hrPersonnel);
                leaveRequest.setLeaveStatus("APPROVED");
                EmailSenderDao emailSenderDao = new EmailSenderDao();
                emailSenderDao.newLeaveUpdateNotification(leaveRequest);
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        
        return rowsAffected == 1;
    }
    
     /**
     * Declines a leave request for an employee.
     * 
     * @param leaveRequest The leave request to decline.
     * @return true if leave request is declined, false otherwise.
     */
    public boolean declineLeaveRequest(LeaveRequest leaveRequest, HRPersonnel hrPersonnel) {
        int rowsAffected = 0;
        try {
            // Define the query
            String query = "UPDATE leave_records "
                    + "SET approved_by = ?, remarks = 'DECLINED' WHERE id = ? AND remarks = 'PENDING'";

            // Prepare the statement
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                // Set the parameters
                preparedStatement.setInt(1, hrPersonnel.getEmployeeID());
                preparedStatement.setInt(2, leaveRequest.getLeaveID());
                // Execute the query
                rowsAffected = preparedStatement.executeUpdate();
            }
            if(rowsAffected == 1){
                leaveRequest.setApprovedBy(hrPersonnel);
                leaveRequest.setLeaveStatus("DECLINED");
                EmailSenderDao emailSenderDao = new EmailSenderDao();
                emailSenderDao.newLeaveUpdateNotification(leaveRequest);
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        
        return rowsAffected == 1;
    }
    
    /**
     * Adds a new employee to the system.
     * 
     * @param employee The employee to add.
     * @return true if employee is added successfully, false otherwise.
     */
    public boolean addNewEmployee(Employee employee) {
        try {
            return AdminPersistence.insertEmployee(connection, employee, getEmployeeRoleId()) == 1;
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            System.out.println("SQL Error Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private int getEmployeeRoleId() throws SQLException {
        String query = "SELECT role_id FROM roles WHERE role_name = 'Employee'";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("role_id") : 0;
            }
        }
    }
    
    /**
     * Updates an existing employee's details in the system.
     * 
     * @param employee The employee to update.
     * @return true if employee is updated successfully, false otherwise.
     */
    public boolean updateEmployee(Employee employee) {
        int rowsAffected = 0;

        try {
            // Update employees table
            String query = "UPDATE employees SET last_name = ?, first_name = ?, birthdate = ?, "
                         + "phone_number = ?, status_id = ?, position_id = ?, supervisor_id = ? "
                         + " WHERE employee_id = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, employee.getLastName());
                preparedStatement.setString(2, employee.getFirstName());
                preparedStatement.setDate(3, new java.sql.Date(employee.getBirthday().getTime()));
                preparedStatement.setString(4, employee.getPhoneNumber());
                preparedStatement.setInt(5, DaoMappers.getStatusId(connection, employee.getStatus()));
                preparedStatement.setInt(6, DaoMappers.getPositionId(connection, employee.getPosition()));
                preparedStatement.setObject(7, employee.getImmediateSupervisor(), java.sql.Types.INTEGER); // Nullable
                preparedStatement.setInt(8, employee.getEmployeeID());

                System.out.println("Executing Query: " + preparedStatement.toString());
                preparedStatement.executeUpdate();
            }

            rowsAffected = AdminPersistence.updateAddressSalaryGov(connection, employee);

        } catch (SQLException e) {
            System.err.println("Error while executing employee update SQL!");
            System.out.println("SQL Error Message: " + e.getMessage());
            e.printStackTrace();
        }

        return rowsAffected == 1;
    }
    
    /**
     * Deletes an employee from the system based on their employee ID.
     * 
     * @param employee The employee to delete.
     * @return true if employee is deleted successfully, false otherwise.
     */
    public boolean deleteEmployee(Employee employee) {
        try {
            return AdminPersistence.softDeleteEmployee(connection, employee.getEmployeeID());
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Processes payroll for all employees in a specified date range.
     * 
     * @param startDate The start date for payroll processing.
     * @param endDate The end date for payroll processing.
     * @return true if payroll is successfully processed, false otherwise.
     */
    public boolean processPayroll(Date startDate, Date endDate, HRPersonnel hrPersonnel) {
        try {
            String checkSql = "SELECT id FROM payroll WHERE period_start = ? AND period_end = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(checkSql)) {
                preparedStatement.setDate(1, new java.sql.Date(startDate.getTime()));
                preparedStatement.setDate(2, new java.sql.Date(endDate.getTime()));
                try (ResultSet result = preparedStatement.executeQuery()) {
                    if (result.next()) {
                        // Payroll already exists
                        return false;
                    }
                }
            }

            // Insert new payroll record
            String insertSql = "INSERT INTO payroll ("
                    + "processed_by_employee_id, period_start, period_end, date_generated, status) "
                    + "VALUES (?, ?, ?, ?, 'PENDING')";
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setInt(1, hrPersonnel.getEmployeeID());
                insertStmt.setDate(2, new java.sql.Date(startDate.getTime()));
                insertStmt.setDate(3, new java.sql.Date(endDate.getTime()));
                insertStmt.setDate(4, new java.sql.Date(System.currentTimeMillis()));

                int rowsInserted = insertStmt.executeUpdate();
                return rowsInserted > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching employee details:");
            e.printStackTrace();
        }

        return false;
    }

    
    /**
     * Loads the list of employees from the database.
     * 
     * @return A list of employees.
     */
    public List<User> loadEmployeeList(){
        try {
            // Define the query
            String query = "SELECT e.* FROM employee_view e "
                    + "INNER JOIN user_credentials uc "
                    + "ON e.employee_id = uc.employee_id "
                    + "WHERE uc.role_id = 1 AND e.is_deleted = False";

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
    
    /**
     * Loads a filtered list of employees based on a search query.
     * 
     * @param search The search query for employee filtering.
     * @return A list of filtered employees.
     */
    public List<User> loadEmployeeList(String search){
        try {
            // Define the query
            String query = "SELECT * FROM employee_view e INNER JOIN user_credentials uc "
                    + "ON e.employee_id = uc.employee_id WHERE uc.role_id = 1 "
                    + "AND is_deleted = False AND "
                    + "(e.employee_id LIKE ? OR first_name LIKE ? OR "
                    + "last_name LIKE ?)";
            
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
    
    public List<User> loadNewEmployees(){
        try {
            // Define the query
            String query = "SELECT e.*, uc.role_id FROM employee_view e "
                    + "LEFT JOIN user_credentials uc "
                    + " ON e.employee_id = uc.employee_id "
                    + "WHERE date_hired >= CURDATE() - INTERVAL 7 DAY "
                    + "AND role_id = 1 ";
            
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
    
    /**
     * Loads the list of attendance records from the database.
     * 
     * @return A list of attendance records.
     */
    public List<AttendanceRecord> loadAttendanceList(){
        try {
            // Define the query
            String query = "SELECT a.*, e.first_name, e.last_name, p.name AS position " +
               "FROM attendance_record a " +
               "INNER JOIN employees e ON e.employee_id = a.employee_id " +
               "INNER JOIN positions p ON p.position_id = e.position_id " +
               "ORDER BY a.date DESC";
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                // Execute the query
                System.out.println("Executing Query: " + preparedStatement.toString());
                try (ResultSet result = preparedStatement.executeQuery()) {
                    return DaoMappers.mapAttendanceList(result);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Loads the attendance records for a specific date range.
     * 
     * @param startDate The start date for the attendance records.
     * @param endDate The end date for the attendance records.
     * @return A list of attendance records for the given date range.
     */
    public List<AttendanceRecord> loadAttendanceList(
            Date startDate, Date endDate){
        try {
            // Define the query
            String query = "SELECT a.*, e.first_name, e.last_name, p.name AS position " +
               "FROM attendance_record a " +
               "INNER JOIN employees e ON e.employee_id = a.employee_id " +
               "INNER JOIN positions p ON p.position_id = e.position_id " +
               "WHERE a.date BETWEEN ? AND ?";
            query += "ORDER BY date DESC";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setDate(1, new java.sql.Date(startDate.getTime()));
                preparedStatement.setDate(2, new java.sql.Date(endDate.getTime()));

                try (ResultSet result = preparedStatement.executeQuery()) {
                    return DaoMappers.mapAttendanceList(result);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return null;
    }
    
     /**
     * Loads the summary payroll report for a specific month and year.
     * 
     * @param month The month for the payroll summary.
     * @param year The year for the payroll summary.
     * @return A list of payroll summary records for the given month and year.
     */
    public List<Map<String, Object>> loadSummaryReport(int month, int year){
        List<Map<String, Object>> rows = new ArrayList<>();
        // Define the query
        String query = "SELECT * FROM payroll_summary_report_view WHERE `Month` = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            String period = String.format("%04d-%02d", year, month);
            preparedStatement.setString(1, period);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Employee No", rs.getString("Employee No"));
                    row.put("Employee Full Name", rs.getString("Employee Full Name"));
                    row.put("Position", rs.getString("Position"));
                    row.put("Gross Income", rs.getString("Gross Income"));
                    row.put("Social Security Contribution", rs.getDouble("Social Security Contribution"));
                    row.put("Philhealth Contribution", rs.getDouble("Philhealth Contribution"));
                    row.put("Pag-Ibig Contribution", rs.getDouble("Pag-Ibig Contribution"));
                    row.put("Withholding Tax", rs.getDouble("Withholding Tax"));
                    row.put("Rice Subsidy", rs.getDouble("Rice Subsidy"));
                    row.put("Phone Allowance", rs.getDouble("Phone Allowance"));
                    row.put("Clothing Allowance", rs.getDouble("Clothing Allowance"));
                    row.put("Net Pay", rs.getDouble("Net Pay"));
                    rows.add(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            System.out.println("SQL Error Message: " + e.getMessage());
            e.printStackTrace();
        }
        return rows;
    }
    
    // Maps a ResultSet containing employee data to a List of User objects.
    private List<User> mapEmployees(ResultSet result) throws SQLException {
        List<User> employees = new ArrayList<>();
        while (result.next()) {
            employees.add(DaoMappers.mapEmployee(result));
        }
        return employees;
    }
    
    // Loads payroll list within a date range and specific status into a list of Maps
    public List<Map<String, Object>> loadPayrollList(Date startDate, Date endDate){
        List payroll = new ArrayList<>();
        try {
            // Define the query
            String query = "SELECT * " +
                    "FROM payslip_view p\n" +
                    "INNER JOIN user_credentials uc "
                    + "ON p.employee_id = uc.employee_id\n" +
                    "INNER JOIN roles r ON uc.role_id = r.role_id\n" +
                    "WHERE r.role_name = 'Employee' " +
                    "    AND period_start = ? " + 
                    "    AND period_end = ? " ;
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setDate(1, new java.sql.Date(startDate.getTime()));
                preparedStatement.setDate(2, new java.sql.Date(endDate.getTime()));

                // Execute the query
                System.out.println("Executing Query: " + preparedStatement.toString());
                try (ResultSet result = preparedStatement.executeQuery()) {
                    while (result.next()) {
                        // Store data in a Map instead of a separate class
                        Map<String, Object> payrollData = new HashMap<>();
                        payrollData.put("payslip_no", result.getString("payslip_no"));
                        payrollData.put("employee_id", result.getString("employee_id"));
                        payrollData.put("employee_name", result.getString("employee_name"));
                        payrollData.put("position_department", result.getString("position_department"));
                        payrollData.put("payPeriod", result.getDate("period_start")
                                + " - " + result.getDate("period_end"));
                        payrollData.put("take_home_pay", result.getDouble("take_home_pay"));
//                payrollData.put("status", result.getString("payslip_status"));

                        payroll.add(payrollData);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        
        return payroll;
    }
    
    // Loads all leave requests
    public List<LeaveRequest> loadLeaveRequest(){
        try {
            // Define the query
            String query = "SELECT " +
               "l.*, " +
               "lt.type_name AS leave_type, " +
               "d.name AS department, " +
               "e.first_name, " +
               "e.last_name, " +
               "e2.employee_id AS approver_id, " +
               "e2.first_name AS approver_first_name, " +
               "e2.last_name AS approver_last_name " +
               "FROM leave_records l " +
               "LEFT JOIN employees e ON e.employee_id = l.employee_id " +
               "LEFT JOIN employees e2 ON e2.employee_id = l.approved_by " +
               "LEFT JOIN positions p ON e.position_id = p.position_id " +
               "LEFT JOIN department d ON p.department_id = d.department_id " +
               "LEFT JOIN leave_types lt ON l.leave_type_id = lt.leave_type_id " +
               "ORDER BY date_filed DESC";

            // Execute the query
            try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                 ResultSet result = preparedStatement.executeQuery()) {
                List<LeaveRequest> leaves = mapLeaveRequest(result);
                return leaves;
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return null;
    }
    
    // Loads leave requests based on a search filter
    public List<LeaveRequest> loadLeaveRequest(String search){
        try {
            // Define the query
            String query = "SELECT " +
               "l.*, " +
               "lt.type_name AS leave_type, " +
               "d.name AS department, " +
               "e.employee_id, " +
               "e.first_name, " +
               "e.last_name, " +
               "e2.employee_id AS approver_id, " +
               "e2.first_name AS approver_first_name, " +
               "e2.last_name AS approver_last_name " +
               "FROM leave_records l " +
               "LEFT JOIN employees e ON e.employee_id = l.employee_id " +
               "LEFT JOIN employees e2 ON e2.employee_id = l.approved_by " +
               "LEFT JOIN positions p ON e.position_id = p.position_id " +
               "LEFT JOIN department d ON p.department_id = d.department_id " +
               "LEFT JOIN leave_types lt ON l.leave_type_id = lt.leave_type_id ";
            
            if(!search.equals("All")){
                query += "WHERE remarks = ? ";
            }
            query += " ORDER BY date_filed DESC";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                if(!search.equals("All")){
                    preparedStatement.setString(1, search);
                }

                // Execute the query
                try (ResultSet result = preparedStatement.executeQuery()) {
                    List<LeaveRequest> leaves = mapLeaveRequest(result);
                    return leaves;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return null;
    }
    
    // Maps a ResultSet containing leave requests to a List of LeaveRequest objects
    private List<LeaveRequest> mapLeaveRequest(ResultSet result){
        List<LeaveRequest> leaves = new ArrayList<>();
        try { 
            while (result.next()) {
                Employee emp = new Employee();
                emp.setEmployeeID(result.getInt("employee_id"));
                emp.setLastName(result.getString("last_name"));
                emp.setFirstName(result.getString("first_name"));
                emp.setDepartment(result.getString("department"));
                
                LeaveRequest leave = new LeaveRequest();
                leave.setLeaveID(result.getInt("id"));
                leave.setEmployee(emp);
                
                leave.setDateCreated(result.getDate("date_filed"));
                leave.setStartDate(result.getDate("date_from"));
                leave.setEndDate(result.getDate("date_until"));
                leave.setLeaveType(result.getString("leave_type"));
                leave.setDuration(result.getInt("number_of_days"));
                leave.setReason(result.getString("reason"));
                leave.setLeaveStatus(result.getString("remarks"));
                
                if(result.getInt("approver_id") != 0) {
                    HRPersonnel approver = new HRPersonnel();
                    approver.setEmployeeID(result.getInt("approver_id"));
                    approver.setFirstName(result.getString("approver_first_name"));
                    approver.setLastName(result.getString("approver_last_name"));
                    
                    leave.setApprovedBy(approver);
                }
                
                leaves.add(leave);
            }
        } catch (SQLException ex) {
            System.out.println(ex);
        }
        return leaves;
    }
    
    // Loads a specific payslip for an employee within a date range
    public Payroll loadPayslip(Date startDate, Date endDate, Employee employee){
        Payroll payslip = new Payroll();
        try {
            // Define the query
            String query = "SELECT * FROM payslip_view p "
                    + "WHERE period_start = ? AND period_end = ? "
                    + "AND employee_id = ? ";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setDate(1, new java.sql.Date(startDate.getTime()));
                preparedStatement.setDate(2, new java.sql.Date(endDate.getTime()));

                preparedStatement.setInt(3, employee.getEmployeeID());

                // Execute the query
                System.out.println("Executing Query: " + preparedStatement.toString());
                try (ResultSet result = preparedStatement.executeQuery()) {
                    if(result.next()){
                        payslip = DaoMappers.mapPayroll(result);
                        return payslip;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }
        return null;
    }
    
    public List<Integer> loadDashboard(){
        try {
            // Define the query
            String query = "SELECT COUNT(*) AS total, " 
                    + " COUNT(CASE WHEN date_hired >= NOW() - INTERVAL 1 WEEK THEN 1 END) "
                    + " AS new_employees FROM employees e "
                    + " INNER JOIN user_credentials uc "
                    + " ON e.employee_id = uc.employee_id"
                    + " WHERE role_id = 1";

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
            e.printStackTrace();
        }
        return null;
    }
}
