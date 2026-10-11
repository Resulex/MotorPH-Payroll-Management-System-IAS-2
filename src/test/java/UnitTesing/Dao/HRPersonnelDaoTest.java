/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing.Dao;

import models.Address;
import models.AttendanceRecord;
import models.Benefits;
import models.Employee;
import models.HRPersonnel;
import models.LeaveRequest;
import models.Payroll;
import models.User;
import java.sql.Connection;
import dao.HRPersonnelDao;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
public class HRPersonnelDaoTest {
    HRPersonnelDao hrPersonnelDao = null;
    Connection connection;
    
    @BeforeEach
    public void setUp() throws SQLException {
        connection = DbConnection.getConnectionWithTransaction();
        hrPersonnelDao = new HRPersonnelDao(connection);
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.rollback();  // Roll back everything done in the test
            connection.close();
        }
    }
    
    @Test
    public void approveLeaveRequestSuccess() {
        System.out.println("Test for Approve Leave Request - Success");
        
        LeaveRequest leaveToFind = new LeaveRequest();
        leaveToFind.setLeaveID(10);
        leaveToFind.setStartDate(new Date());
        leaveToFind.setEndDate(new Date());
        Employee employee = new Employee(10001);
        leaveToFind.setEmployee(employee);
        HRPersonnel user = new HRPersonnel(10006);
        hrPersonnelDao.approveLeaveRequest(leaveToFind, user);
        
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest();
        for (LeaveRequest leave : leaves) {
            if(leave.getLeaveID() == 10){
                assertEquals("APPROVED", leave.getLeaveStatus());
                assertEquals(10006, leave.getApprovedBy().getEmployeeID());
                System.out.println("Leave request has been successfully approved by user");
            }
        }
    }
    
    @Test
    public void approveLeaveRequestInvalidId() {
        System.out.println("Test for Approve Leave Request - Invalid Id");
        
        LeaveRequest leaveToFind = new LeaveRequest();
        
        leaveToFind.setLeaveID(999999999);
        leaveToFind.setStartDate(new Date());
        leaveToFind.setEndDate(new Date());
        Employee employee = new Employee(10001);
        leaveToFind.setEmployee(employee);
        HRPersonnel user = new HRPersonnel(999999999);
        hrPersonnelDao.approveLeaveRequest(leaveToFind, user);
        
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest();
        for (LeaveRequest leave : leaves) {
            assertNotEquals(999999999, leave.getLeaveStatus());
        }
        System.out.println("No Leave Request found for invalid Id");
    }
    
    @Test
    public void approveLeaveRequestNotPending() {
        System.out.println("Test for Approve Leave Request - Not Pending");
        
        // Approve already Approved leave
        LeaveRequest leaveToFind = new LeaveRequest();
        leaveToFind.setLeaveID(2);
        leaveToFind.setStartDate(new Date());
        leaveToFind.setEndDate(new Date());
        Employee employee = new Employee(10001);
        leaveToFind.setEmployee(employee);
        HRPersonnel user = new HRPersonnel(10006);
        hrPersonnelDao.approveLeaveRequest(leaveToFind, user);
        
        // Approve already Declined Leave
        leaveToFind.setLeaveID(2);
        user = new HRPersonnel(10006);
        hrPersonnelDao.approveLeaveRequest(leaveToFind, user);
        
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest();
        for (LeaveRequest leave : leaves) {
            if(leave.getLeaveID() == 2){
                assertEquals("APPROVED", leave.getLeaveStatus());
                assertEquals(10002, leave.getApprovedBy().getEmployeeID());
                System.out.println("Leave request that is already approved is not updated");
            } else if(leave.getLeaveID() == 3){
                assertEquals("DECLINED", leave.getLeaveStatus());
                assertEquals(10002, leave.getApprovedBy().getEmployeeID());
                System.out.println("Leave request that is already declined is not updated");
            }
        }
    }
    
    @Test
    public void declineLeaveRequestSuccess() {
        System.out.println("Test for Decline Leave Request - Success");
        
        LeaveRequest leaveToFind = new LeaveRequest();
        leaveToFind.setLeaveID(10);
        leaveToFind.setStartDate(new Date());
        leaveToFind.setEndDate(new Date());
        Employee employee = new Employee(10001);
        leaveToFind.setEmployee(employee);
        HRPersonnel user = new HRPersonnel(10006);
        hrPersonnelDao.declineLeaveRequest(leaveToFind, user);
        
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest();
        for (LeaveRequest leave : leaves) {
            if(leave.getLeaveID() == 10){
                assertEquals("DECLINED", leave.getLeaveStatus());
                assertEquals(10006, leave.getApprovedBy().getEmployeeID());
                System.out.println("Leave request has been successfully approved by user");
            }
        }
    }
    
    @Test
    public void declineLeaveRequestInvalidId() {
        System.out.println("Test for Decline Leave Request - Invalid Id");
        
        LeaveRequest leaveToFind = new LeaveRequest();
        leaveToFind.setLeaveID(999999999);
        leaveToFind.setStartDate(new Date());
        leaveToFind.setEndDate(new Date());
        Employee employee = new Employee(10001);
        leaveToFind.setEmployee(employee);
        HRPersonnel user = new HRPersonnel(999999999);
        hrPersonnelDao.declineLeaveRequest(leaveToFind, user);
        
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest();
        for (LeaveRequest leave : leaves) {
            assertNotEquals(999999999, leave.getLeaveStatus());
        }
        System.out.println("No Leave Request found for invalid Id");
    }
    
    @Test
    public void declineLeaveRequestNotPending() {
        System.out.println("Test for Decline Leave Request - Not Pending");
        
        // Approve already Approved leave
        LeaveRequest leaveToFind = new LeaveRequest();
        leaveToFind.setLeaveID(2);
        leaveToFind.setStartDate(new Date());
        leaveToFind.setEndDate(new Date());
        Employee employee = new Employee(10001);
        leaveToFind.setEmployee(employee);
        HRPersonnel user = new HRPersonnel(10006);
        hrPersonnelDao.declineLeaveRequest(leaveToFind, user);
        
        // Approve already Declined Leave
        leaveToFind.setLeaveID(2);
        user = new HRPersonnel(10006);
        hrPersonnelDao.declineLeaveRequest(leaveToFind, user);
        
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest();
        for (LeaveRequest leave : leaves) {
            if(leave.getLeaveID() == 2){
                assertEquals("APPROVED", leave.getLeaveStatus());
                assertEquals(10002, leave.getApprovedBy().getEmployeeID());
                System.out.println("Leave request that is already approved is not updated");
            } else if(leave.getLeaveID() == 3){
                assertEquals("DECLINED", leave.getLeaveStatus());
                assertEquals(10002, leave.getApprovedBy().getEmployeeID());
                System.out.println("Leave request that is already declined is not updated");
            }
        }
    }
    
    @Test
    public void addNewEmployeeSuccess() {
        System.out.println("Test for Add New Employee - Success");
        LocalDate localDate = LocalDate.of(1995, 6, 10);
        Date birthday = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        Employee employee = new Employee();
        Address address = new Address();
        address.setStreet("Kalayaan street");
        address.setBarangay("Barangay Uno");
        address.setCity("Calamba City");
        address.setProvince("Laguna");
        address.setZipcode("4027");
        employee.setAddress(address);
        Benefits benefit = new Benefits();
        benefit.setBasicSalary(30000);
        benefit.setGrossSemiMonthlyRate(15000);
        benefit.setHourlyRate(125);
        employee.setBenefits(benefit);
        employee.setLastName("Doe");
        employee.setFirstName("John");
        employee.setPhoneNumber("099-123-456");
        employee.setBirthday(birthday);
        employee.setSssNumber("123456789");
        employee.setTinNumber("22222222");
        employee.setPhilhealthNumber("321-4535-312");
        employee.setPagibigNumber("9999321");
        employee.setStatus("Regular");
        employee.setPosition("Sales & Marketing");
        employee.setImmediateSupervisor(10001);
        boolean result = hrPersonnelDao.addNewEmployee(employee);
        assertTrue(result);
        System.out.println("Employee has been created successfully");
    }
    
    @Test
    public void addNewEmployeeIncomplete() {
        System.out.println("Test for Add New Employee - Incomplete");
        LocalDate localDate = LocalDate.of(1995, 6, 10);
        Date birthday = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        Employee employee = new Employee();
        Address address = new Address();
        address.setStreet("Kalayaan street");
        address.setBarangay("Barangay Uno");
        address.setCity("Calamba City");
        address.setProvince("Laguna");
        address.setZipcode("4027");
        employee.setAddress(address);
        Benefits benefit = new Benefits();
        benefit.setBasicSalary(30000);
        benefit.setGrossSemiMonthlyRate(15000);
        benefit.setHourlyRate(125);
        employee.setBenefits(benefit);
        employee.setLastName("Doe");
        employee.setFirstName("John");
        employee.setBirthday(birthday);
        boolean result = hrPersonnelDao.addNewEmployee(employee);
        assertFalse(result);
        System.out.println("Incomple employee has not been created");
    }
    
    @Test
    public void addNewEmployeeDuplicateGovId() {
        System.out.println("Test for Add New Employee - DuplicateGovId");
        LocalDate localDate = LocalDate.of(1995, 6, 10);
        Date birthday = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        Employee employee = new Employee();
        Address address = new Address();
        address.setStreet("Kalayaan street");
        address.setBarangay("Barangay Uno");
        address.setCity("Calamba City");
        address.setProvince("Laguna");
        address.setZipcode("4027");
        employee.setAddress(address);
        Benefits benefit = new Benefits();
        benefit.setBasicSalary(30000);
        benefit.setGrossSemiMonthlyRate(15000);
        benefit.setHourlyRate(125);
        employee.setBenefits(benefit);
        employee.setLastName("Doe");
        employee.setFirstName("John");
        employee.setPhoneNumber("099-123-456");
        employee.setBirthday(birthday);
        employee.setSssNumber("123456789");
        employee.setTinNumber("22222222");
        employee.setPhilhealthNumber("321-4535-312");
        employee.setPagibigNumber("9999321");
        boolean result = hrPersonnelDao.addNewEmployee(employee);
        assertFalse(result);
        System.out.println("Employee with duplicate gov id is not created.");
    }
    
    @Test
    public void updateEmployeeSuccess() {
        System.out.println("Test for Add New Employee - Success");
        LocalDate localDate = LocalDate.of(1995, 6, 10);
        Date birthday = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        Employee employee = new Employee(10034);
        Address address = new Address();
        address.setStreet("Kalayaan street");
        address.setBarangay("Barangay Uno");
        address.setCity("Calamba City");
        address.setProvince("Laguna");
        address.setZipcode("4027");
        employee.setAddress(address);
        Benefits benefit = new Benefits();
        benefit.setBasicSalary(30000);
        benefit.setGrossSemiMonthlyRate(15000);
        benefit.setHourlyRate(125);
        employee.setBenefits(benefit);
        employee.setLastName("Doe");
        employee.setFirstName("John");
        employee.setPhoneNumber("099-123-456");
        employee.setBirthday(birthday);
        employee.setSssNumber("123456789");
        employee.setTinNumber("22222222");
        employee.setPhilhealthNumber("321-4535-312");
        employee.setPagibigNumber("9999321");
        employee.setStatus("Regular");
        employee.setPosition("Sales & Marketing");
        employee.setImmediateSupervisor(10001);
        boolean result = hrPersonnelDao.updateEmployee(employee);
        assertTrue(result);
        System.out.println("Employee has been updated successfully");
    }
    
    @Test
    public void deleteEmployeeSuccess() {
        System.out.println("Test for Delete Employee - Success");
        Employee employeeToDelete = new Employee(10030);
        boolean result = hrPersonnelDao.deleteEmployee(employeeToDelete);
        assertTrue(result);
        System.out.println("Employee has been deleted");
    }
    
    @Test
    public void deleteEmployeeInvalidEmployee() {
        System.out.println("Test for Delete Employee - Invalid Employee");
        Employee employeeToDelete = new Employee(999999999);
        boolean result = hrPersonnelDao.deleteEmployee(employeeToDelete);
        assertFalse(result);
        System.out.println("Invalid Employee is not deleted");
    }
    
    @Test
    public void deleteEmployeeAlreadyDeleted() {
        System.out.println("Test for Delete Employee - Invalid Employee");
        boolean result = false;
        Employee employeeToDelete = new Employee(10030);
        result = hrPersonnelDao.deleteEmployee(employeeToDelete);
        assertTrue(result);
        result = hrPersonnelDao.deleteEmployee(employeeToDelete);
        assertFalse(result);
        System.out.println("Fail to delete already deleted employee");
    }
    
    @Test
    public void testLoadEmployeeListSuccess() {
        System.out.println("Test for Load Employee List - Success");
        List<User> employees = hrPersonnelDao.loadEmployeeList();
        assertFalse(employees.isEmpty());
        for (User employee : employees) {
            System.out.println(employee.getEmployeeID() + " - " + employee.getFirstName() + " " + employee.getLastName());
        }
    }
    
    @Test
    public void testLoadEmployeeListSearchSuccess() {
        System.out.println("Test for Load Employee List Search - Success");
        List<User> employees = hrPersonnelDao.loadEmployeeList("10015");
        assertFalse(employees.isEmpty());
        for (User employee : employees) {
            System.out.println(employee.getEmployeeID() + " - " + employee.getFirstName() + " " + employee.getLastName());
        }
        
        employees = hrPersonnelDao.loadEmployeeList("Perci");
        assertFalse(employees.isEmpty());
        for (User employee : employees) {
            System.out.println(employee.getEmployeeID() + " - " + employee.getFirstName() + " " + employee.getLastName());
        }
        
        employees = hrPersonnelDao.loadEmployeeList("Del");
        assertFalse(employees.isEmpty());
        for (User employee : employees) {
            System.out.println(employee.getEmployeeID() + " - " + employee.getFirstName() + " " + employee.getLastName());
        }
    }
    
    @Test
    public void testLoadEmployeeListSearchInvalid() {
        System.out.println("Test for Load Employee List Search - Invalid");
        List<User> employees = hrPersonnelDao.loadEmployeeList("2sdfa1239asdfz");
        assertTrue(employees.isEmpty());
        System.out.println("No employee found for search word 2sdfa1239asdfz");
    }
    
    @Test
    public void testLoadEmployeeListSearchDeletedEmployeeNotInList() {
        System.out.println("Test for Load Employee List Search - Deleted employee not in list");
        List<User> employees = hrPersonnelDao.loadEmployeeList("10032");
        assertFalse(employees.isEmpty());
        for (User employee : employees) {
            if(employee.getEmployeeID() == 10032){
                System.out.println(employee.getEmployeeID() + " - " + employee.getFirstName() + " " + employee.getLastName());
            }
        }
        
        // delete employee
        boolean result = hrPersonnelDao.deleteEmployee(new Employee(10032));
        assertTrue(result);
        
        employees = hrPersonnelDao.loadEmployeeList("10032");
        assertTrue(employees.isEmpty());
        System.out.println("Employee is not found on the list anymore after being deleted");
    }
    
    @Test
    public void testLoadNewEmployeeSuccess() {
        System.out.println("Test for Load New Employee - Success");
        LocalDate localDate = LocalDate.of(1995, 6, 10);
        Date birthday = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        Employee employee = new Employee();
        Address address = new Address();
        address.setStreet("Kalayaan street");
        address.setBarangay("Barangay Uno");
        address.setCity("Calamba City");
        address.setProvince("Laguna");
        address.setZipcode("4027");
        employee.setAddress(address);
        Benefits benefit = new Benefits();
        benefit.setBasicSalary(30000);
        benefit.setGrossSemiMonthlyRate(15000);
        benefit.setHourlyRate(125);
        employee.setBenefits(benefit);
        employee.setLastName("Doe");
        employee.setFirstName("John");
        employee.setPhoneNumber("099-123-456");
        employee.setBirthday(birthday);
        employee.setSssNumber("123456789");
        employee.setTinNumber("22222222");
        employee.setPhilhealthNumber("321-4535-312");
        employee.setPagibigNumber("9999321");
        employee.setStatus("Regular");
        employee.setPosition("Sales & Marketing");
        employee.setImmediateSupervisor(10001);
        boolean result = hrPersonnelDao.addNewEmployee(employee);
        assertTrue(result);
        
        List<User> newUsers = hrPersonnelDao.loadNewEmployees();
        assertFalse(newUsers.isEmpty());
        for (User user : newUsers) {
            System.out.println(
                    user.getEmployeeID() + " - " + 
                    user.getFirstName() + " " + user.getLastName());
        }
    }
    
    @Test
    public void testLoadAttendanceListSuccess() {
        System.out.println("Test for Load AttendanceList - Success");

        List<AttendanceRecord> attendance = hrPersonnelDao.loadAttendanceList();
        assertFalse(attendance.isEmpty());
        System.out.println("Attendance record is not empty");
        for (AttendanceRecord record : attendance) {
            System.out.println(
                    record.getDate() + " (" + record.getTimeIn() 
                            + " - " + record.getTimeOut() + ")");
        }
    }
    
    @Test
    public void testLoadAttendanceListDateRangeSuccess() {
        System.out.println("Test for Load AttendanceList with Date Range - Success");
        LocalDate localDate = LocalDate.of(2024, 6, 10);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<AttendanceRecord> attendance = hrPersonnelDao.loadAttendanceList(dateFrom, dateUntil);
        assertFalse(attendance.isEmpty());
        System.out.println("Attendance record with Date Range is not empty");
        for (AttendanceRecord record : attendance) {
            System.out.println(
                    record.getDate() + " (" + record.getTimeIn() 
                            + " - " + record.getTimeOut() + ")");
        }
    }
    
    @Test
    public void testLoadAttendanceListDateRangeInvalidEmployee() {
        System.out.println("Test for Load AttendanceList with Date Range - Invalid Date");
        LocalDate localDate = LocalDate.of(2002, 1, 10);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2003, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<AttendanceRecord> attendance = hrPersonnelDao.loadAttendanceList(
                dateFrom, dateUntil);
        assertTrue(attendance.isEmpty());
        System.out.println("Attendance record with Date Range is empty for invalid employee");
    }
    
    @Test
    public void testLoadSummaryReportSuccess() throws SQLException {
        System.out.println("Test for Load Summary Report - Success");

        List<Map<String, Object>> reports = hrPersonnelDao.loadSummaryReport(6, 2024);
        assertNotNull(reports, "The report list should not be null");

        System.out.println("Summary report for 2024-06:");
        for (Map<String, Object> row : reports) {
            String fullName = (String) row.get("Employee Full Name");
            double netPay = ((Number) row.get("Net Pay")).doubleValue();

            System.out.println(fullName + " Net Pay: P" + netPay);
        }
    }
    
    @Test
    public void testLoadPayrollListSuccess() {
        System.out.println("Test for Load Payroll List - Success");
        LocalDate localDate = LocalDate.of(2024, 6, 1);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Map<String, Object>> payrollList = hrPersonnelDao.loadPayrollList(dateFrom, dateUntil);
        assertFalse(payrollList.isEmpty());
        System.out.println("Payroll records is existing");
        for (Map<String, Object> payroll : payrollList) {
            System.out.println(
                    payroll.get("payslip_no") + "- " + 
                    payroll.get("employee_id") + ": " + 
                    payroll.get("take_home_pay"));
        }
    }
    
    @Test
    public void testLoadPayrollListInvalidDate() {
        System.out.println("Test for Load Payroll List - Invalid Date");
        LocalDate localDate = LocalDate.of(2024, 6, 3);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 27);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Map<String, Object>> payrollList = hrPersonnelDao.loadPayrollList(dateFrom, dateUntil);
        assertTrue(payrollList.isEmpty());
        System.out.println("No payroll list found for invalid date");
    }
    
    @Test
    public void testLoadLeaveRequestSuccess() {
        System.out.println("Test for Load Leave Request - Success");
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest();
        assertFalse(leaves.isEmpty());
        System.out.println("Leaves request found");
        for (LeaveRequest leave : leaves) {
            Employee employee = leave.getEmployee();
            System.out.println(
                leave.getLeaveID() + "- " + 
                employee.getEmployeeID() + " ( " + 
                leave.getStartDate() + " ) " +
                leave.getEndDate());
        }
    }
    
    @Test
    public void testLoadLeaveRequestSearchSuccess() {
        System.out.println("Test for Load Leave Request Search - Success");
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest("APPROVED");
        assertFalse(leaves.isEmpty());
        System.out.println("Leaves found for approved remarks");
        for (LeaveRequest leave : leaves) {
            Employee employee = leave.getEmployee();
            System.out.println(
                leave.getLeaveID() + "- " + 
                employee.getEmployeeID() + " ( " + 
                leave.getStartDate() + " ) " +
                leave.getEndDate());
        }
    }
    
    @Test
    public void testLoadLeaveRequestSearchInvalidRemarks() {
        System.out.println("Test for Load Leave Request Search - Invalid Remarks");
        List<LeaveRequest> leaves = hrPersonnelDao.loadLeaveRequest("INVALID");
        assertTrue(leaves.isEmpty());
        System.out.println("No leaves found for invalid remarks");
    }
    
    @Test
    public void testLoadPayslipSuccess() {
        System.out.println("Test for Load Payslip - Success");
        LocalDate localDate = LocalDate.of(2024, 6, 1);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Payroll payroll = hrPersonnelDao.loadPayslip(dateFrom, dateUntil, new Employee(10008));
        assertNotNull(payroll);
        System.out.println("Payroll record is not null");
        assertEquals("2024-06-15-1", payroll.getPayrollID());
        assertEquals(74, payroll.getHoursWorked());
        assertEquals(2, payroll.getOvertimeHours());
        assertEquals(133, payroll.getEmployee().getBenefitsHourlyRate());
        assertEquals(10108.0, payroll.getGrossIncome());
        assertEquals(1250.0, payroll.getEmployee().getBenefits().calculateTotalAllowance());
        assertEquals(218.75, payroll.getDeductions().calculateTotalDeductions());
        assertEquals(11139.25, payroll.getNetPay());
    }
    
    @Test
    public void testLoadPayslipInvalidEmployee() {
        System.out.println("Test for Load Payslip - Invalid Employee");
        LocalDate localDate = LocalDate.of(2024, 6, 1);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Payroll payroll = hrPersonnelDao.loadPayslip(dateFrom, dateUntil, new Employee(99999999));
        assertNull(payroll);
        System.out.println("No payroll found for invalid employee");
    }
    
    @Test
    public void testLoadPayslipInvalidDate() {
        System.out.println("Test for Load Payslip - Invalid Date");
        LocalDate localDate = LocalDate.of(2024, 6, 2);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 13);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Payroll payroll = hrPersonnelDao.loadPayslip(dateFrom, dateUntil, new Employee(10008));
        assertNull(payroll);
        System.out.println("No payroll found for invalid date");
    }
    
    @Test
    public void loadDashboardSuccess() {
        System.out.println("Test for Load Dashboard - Success");
        LocalDate localDate = LocalDate.of(1995, 6, 10);
        Date birthday = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        Employee employee = new Employee();
        Address address = new Address();
        address.setStreet("Kalayaan street");
        address.setBarangay("Barangay Uno");
        address.setCity("Calamba City");
        address.setProvince("Laguna");
        address.setZipcode("4027");
        employee.setAddress(address);
        Benefits benefit = new Benefits();
        benefit.setBasicSalary(30000);
        benefit.setGrossSemiMonthlyRate(15000);
        benefit.setHourlyRate(125);
        employee.setBenefits(benefit);
        employee.setLastName("Doe");
        employee.setFirstName("John");
        employee.setPhoneNumber("099-123-456");
        employee.setBirthday(birthday);
        employee.setSssNumber("123456789");
        employee.setTinNumber("22222222");
        employee.setPhilhealthNumber("321-4535-312");
        employee.setPagibigNumber("9999321");
        employee.setStatus("Regular");
        employee.setPosition("Sales & Marketing");
        employee.setImmediateSupervisor(10001);
        boolean result = hrPersonnelDao.addNewEmployee(employee);
        assertTrue(result);
        
        List<Integer> dashboard = hrPersonnelDao.loadDashboard();
        assertTrue(dashboard.get(0) >= 1, "Value should be at least 1");
        assertTrue(dashboard.get(0) >= 1, "Value should be at least 1");
        System.out.println("Total Employees: " + dashboard.get(0));
        System.out.println("New Employees: " + dashboard.get(1));
    }
}
