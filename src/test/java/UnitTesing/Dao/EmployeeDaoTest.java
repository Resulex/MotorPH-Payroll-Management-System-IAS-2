/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing.Dao;

import models.AttendanceRecord;
import models.Employee;
import models.LeaveRequest;
import models.Payroll;
import dao.EmployeeDao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
public class EmployeeDaoTest {
    EmployeeDao employeeDao = null;
    Connection connection;
    
    @BeforeEach
    public void setUp() throws SQLException {
        connection = DbConnection.getConnectionWithTransaction();
        employeeDao = new EmployeeDao(connection);
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null) {
            connection.rollback();  // Roll back everything done in the test
            connection.close();
        }
    }
    
    @Test
    public void testSaveSuccess() {
        System.out.println("Test for Save - Success");
        
        boolean result = employeeDao.save(
                "street #548", "Barangay 123", "Calamba City", "Laguna", 
                "4027", "09981234567", 10008);

        assertTrue(result);
        System.out.println("Employee details has been saved.");
        
        Employee savedEmployee = employeeDao.getEmployeeDetailsByNumber(10008);
        assertEquals("09981234567", savedEmployee.getPhoneNumber());
        assertEquals("4027", savedEmployee.getAddress().getZipcode());
        assertEquals("street #548", savedEmployee.getAddress().getStreet());
        assertEquals("Barangay 123", savedEmployee.getAddress().getBarangay());
        assertEquals("Calamba City", savedEmployee.getAddress().getCity());
        assertEquals("Laguna", savedEmployee.getAddress().getProvince());
        System.out.println("Employee was verified and matched successfully.");
    }
    
    @Test
    public void testSaveInvalidEmployee() {
        System.out.println("Test for getAttendanceRecord - Invalid Employee");
        boolean result = employeeDao.save(
                "Random street", "Barangay 123", "Calamba City", "Laguna", 
                "4027", "09981234567", 9999999);

        assertFalse(result);
        System.out.println("Employee details has not been saved due to invalid employee");
    }
    
    @Test
    public void testCreateLeaveRequestSuccess() {
        System.out.println("Test for Create Leave Request - Success");
        LocalDate localDate = LocalDate.of(2026, 6, 3);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2026, 6, 4);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LeaveRequest newLeave = new LeaveRequest();
        newLeave.setEmployee(new Employee(10008));
        newLeave.setDuration(2);
        newLeave.setStartDate(dateFrom);
        newLeave.setEndDate(dateUntil);
        newLeave.setReason("Personal Leave");
        newLeave.setLeaveType("Vacation Leave");
        
        boolean result = employeeDao.createLeaveRequest(newLeave);

        assertTrue(result);
        System.out.println("Employee has created leave request.");
        
        List<LeaveRequest> leaves = employeeDao.loadLeaveRequests(10008);
        for (LeaveRequest leave : leaves) {
            LocalDate dbStart = ((java.sql.Date) leave.getStartDate()).toLocalDate();
            LocalDate dbEnd = ((java.sql.Date) leave.getEndDate()).toLocalDate();

            if (dbStart.equals(LocalDate.of(2026, 6, 3))
                    && dbEnd.equals(LocalDate.of(2026, 6, 4))) {
                assertNotNull(leave);
                assertEquals(2, leave.getDuration());
                assertEquals("Vacation Leave", leave.getLeaveType());

                System.out.println("Leave request was verified and matched successfully.");
                break;
            }
        }
    }
    
    @Test
    public void testCreateLeaveRequestInvalidLeaveType() {
        System.out.println("Test for Create Leave Request - Invalid Leave Type");
        LocalDate localDate = LocalDate.of(2026, 6, 3);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2026, 6, 4);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LeaveRequest newLeave = new LeaveRequest();
        newLeave.setEmployee(new Employee(10008));
        newLeave.setDuration(2);
        newLeave.setStartDate(dateFrom);
        newLeave.setEndDate(dateUntil);
        newLeave.setReason("Personal Leave");
        newLeave.setLeaveType("Invalid Leave");
        
        boolean result = employeeDao.createLeaveRequest(newLeave);

        assertFalse(result);
        System.out.println("Employee has failed to create leave request due to invalid leave type.");
    }
    
    @Test
    public void testCreateLeaveRequestInvalidEmployee() {
        System.out.println("Test for Create Leave Request - Invalid Employee");
        LocalDate localDate = LocalDate.of(2026, 6, 3);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2026, 6, 4);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        LeaveRequest newLeave = new LeaveRequest();
        newLeave.setEmployee(new Employee(999999999));
        newLeave.setDuration(2);
        newLeave.setStartDate(dateFrom);
        newLeave.setEndDate(dateUntil);
        newLeave.setReason("Personal Leave");
        newLeave.setLeaveType("Vacation Leave");
        
        boolean result = employeeDao.createLeaveRequest(newLeave);

        assertFalse(result);
        System.out.println("Failed to create leave request due to invalid employee.");
    }
    
    @Test
    public void testLoadLeaveRequestSuccess() {
        System.out.println("Test for Load Leave Request - Success");
        List<LeaveRequest> leaves = employeeDao.loadLeaveRequests(10009);
        for (LeaveRequest leave : leaves) {
            LocalDate dbStart = ((java.sql.Date) leave.getStartDate()).toLocalDate();
            LocalDate dbEnd = ((java.sql.Date) leave.getEndDate()).toLocalDate();

            if (dbStart.equals(LocalDate.of(2024, 2, 9))
                    && dbEnd.equals(LocalDate.of(2024, 2, 10))) {
                assertNotNull(leave);
                assertEquals(2, leave.getDuration());
                assertEquals("Vacation Leave", leave.getLeaveType());

                System.out.println("Leave request was verified and matched successfully.");
                break;
            }
        }
    }
    
    @Test
    public void testLoadLeaveRequestInvalidEmployee() {
        System.out.println("Test for Load Leave Request - Invalid Employee");
        List<LeaveRequest> leaves = employeeDao.loadLeaveRequests(99999999);
        assertTrue(leaves.isEmpty());
        System.out.println("No leaves found for invalid employee");
    }
    
    @Test
    public void testLoadLeaveCreditsSuccess() {
        System.out.println("Test for Load Leave Credits - Success");
        List<Map<String, Object>> leaveCredits = employeeDao.loadLeaveCredits(10001);

        // Available credits are computed for the CURRENT year. The seed data has
        // no leave records in 2026, so employee 10001 still has full credits.
        for (Map<String, Object> leave : leaveCredits) {
            if(leave.get("Type").equals("Emergency Leave")){
                assertEquals(5, leave.get("Available"));
            } else if(leave.get("Type").equals("Sick Leave")){
                assertEquals(15, leave.get("Available"));
            } else {
                assertEquals(20, leave.get("Available"));
            }
            
            System.out.println("Unused " + leave.get("Type") + ": " + leave.get("Available"));
        }
    }
    
    @Test
    public void testLoadLeaveCreditsInvalidEmployee() {
        System.out.println("Test for Load Leave Request - Invalid Employee");
        List<Map<String, Object>> leaveCredits = employeeDao.loadLeaveCredits(9999999);
        
        for (Map<String, Object> leave : leaveCredits) {
            if(leave.get("Type").equals("Emergency Leave")){
                assertEquals(5, leave.get("Available"));
            } else if(leave.get("Type").equals("Sick Leave")){
                assertEquals(15, leave.get("Available"));
            } else {
                assertEquals(20, leave.get("Available"));
            }
            System.out.println("Unused " + leave.get("Type") + ": " + leave.get("Available"));
        }
    }
    
    @Test
    public void testLoadAttendanceListSuccess() {
        System.out.println("Test for Load AttendanceList - Success");

        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(10008);
        assertFalse(attendance.isEmpty());
        System.out.println("Attendance record is not empty for employee 10008");
        for (AttendanceRecord record : attendance) {
            System.out.println(
                    record.getDate() + " (" + record.getTimeIn() 
                            + " - " + record.getTimeOut() + ")");
        }
    }
    
    @Test
    public void testLoadAttendanceListInvalidEmployee() {
        System.out.println("Test for Load AttendanceList - Invalid Employee");
        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(999999999);
        assertTrue(attendance.isEmpty());
        System.out.println("Attendance record is empty for invalid employee");
    }
    
    @Test
    public void testLoadAttendanceListDateRangeSuccess() {
        System.out.println("Test for Load AttendanceList with Date Range - Success");
        LocalDate localDate = LocalDate.of(2024, 6, 10);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(dateFrom, dateUntil, 10008);
        assertFalse(attendance.isEmpty());
        System.out.println("Attendance record with Date Range is not empty for employee 10008");
        for (AttendanceRecord record : attendance) {
            System.out.println(
                    record.getDate() + " (" + record.getTimeIn() 
                            + " - " + record.getTimeOut() + ")");
        }
    }
    
    @Test
    public void testLoadAttendanceListDateRangeInvalidEmployee() {
        System.out.println("Test for Load AttendanceList with Date Range - Invalid Employee");
        LocalDate localDate = LocalDate.of(2024, 6, 10);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(
                dateFrom, dateUntil, 999999999);
        assertTrue(attendance.isEmpty());
        System.out.println("Attendance record with Date Range is empty for invalid employee");
    }
    
    @Test
    public void testLoadPayslipSuccess() {
        System.out.println("Test for Load Payslip - Success");
        LocalDate localDate = LocalDate.of(2024, 6, 1);
        Date dateFrom = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        
        localDate = LocalDate.of(2024, 6, 15);
        Date dateUntil = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Payroll payroll = employeeDao.loadPayslip(10008, dateFrom, dateUntil);
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

        Payroll payroll = employeeDao.loadPayslip(99999999, dateFrom, dateUntil);
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

        Payroll payroll = employeeDao.loadPayslip(10008, dateFrom, dateUntil);
        assertNull(payroll);
        System.out.println("No payroll found for invalid date");
    }
    
    @Test
    public void testTimeInSuccess() {
        System.out.println("Test for Time in - Success");
        employeeDao.timeIn(10008);

        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(new Date(), new Date(), 10008);
        assertFalse(attendance.isEmpty());
        System.out.println("Attendance record has been found");
        for (AttendanceRecord record : attendance) {
            System.out.println(
                    record.getDate() + " (" + record.getTimeIn() 
                            + " - " + record.getTimeOut() + ")");
        }
    }
    
    @Test
    public void testTimeInInvalidEmployee() {
        System.out.println("Test for Time in - Invalid Employee");
        employeeDao.timeIn(999999999);

        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(new Date(), new Date(), 999999999);
        assertTrue(attendance.isEmpty());
        System.out.println("Attendance record is empty");
    }
    
    @Test
    public void testTimeOutSuccess() {
        System.out.println("Test for Time out - Success");
        employeeDao.timeIn(10008);

        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(new Date(), new Date(), 10008);
        assertFalse(attendance.isEmpty());
        System.out.println("Attendance record has been found");
        for (AttendanceRecord record : attendance) {
            System.out.println(
                    record.getDate() + " (" + record.getTimeIn() 
                            + " - " + record.getTimeOut() + ")");
        }
    }
    
    @Test
    public void testTimeOutInvalidEmployee() {
        System.out.println("Test for Time out - Invalid Employee");
        employeeDao.timeOut(999999999);

        List<AttendanceRecord> attendance = employeeDao.loadAttendanceList(new Date(), new Date(), 999999999);
        assertTrue(attendance.isEmpty());
        System.out.println("Attendance record is empty");
    }
    
    @Test
    public void testGetEmployeeDetailsByNumberSuccess() {
        System.out.println("Test for Get Employee Details By Number - Success");
        
        Employee employee = employeeDao.getEmployeeDetailsByNumber(10001);
        assertEquals("966-860-270", employee.getPhoneNumber());
        assertEquals("1227", employee.getAddress().getZipcode());
        assertEquals("Valero Carpark Building Valero Street", employee.getAddress().getStreet());
        assertEquals("", employee.getAddress().getBarangay());
        assertEquals("Makati City", employee.getAddress().getCity());
        assertEquals("Metro Manila", employee.getAddress().getProvince());
        System.out.println("Employee was verified and matched successfully.");
    }
    
    @Test
    public void testGetEmployeeDetailsByNumberInvalidEmployee() {
        System.out.println("Test for Get Employee Details By Number  - Invalid Employee");
        
        
        Employee employee = employeeDao.getEmployeeDetailsByNumber(999999999);
        assertNull(employee);
        System.out.println("Employee was verified and matched successfully.");
    }
}
