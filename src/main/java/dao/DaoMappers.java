/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import models.Address;
import models.AttendanceRecord;
import models.Benefits;
import models.Deductions;
import models.Employee;
import models.Payroll;
import models.User;

/**
 * Shared result-set mappers and lookup helpers used across the DAO layer.
 *
 * @author Alex Resurreccion
 */
public final class DaoMappers {

    private static final String EMPLOYEE_ID = "employee_id";

    private DaoMappers() {
    }

    /**
     * Maps a single {@code employee_view} row to a new {@link Employee}.
     */
    public static Employee mapEmployee(ResultSet result) throws SQLException {
        Employee emp = new Employee();
        populateUser(result, emp);
        return emp;
    }

    /**
     * Populates the common {@code employee_view} fields onto any {@link User} instance.
     */
    public static void populateUser(ResultSet result, User user) throws SQLException {
        user.setEmployeeID(result.getInt(EMPLOYEE_ID));
        user.setLastName(result.getString("last_name"));
        user.setFirstName(result.getString("first_name"));
        user.setBirthday(result.getDate("birthdate"));
        user.setPhoneNumber(result.getString("phone_number"));
        user.setSssNumber(result.getString("sss_number"));
        user.setPhilhealthNumber(result.getString("philhealth_number"));
        user.setTinNumber(result.getString("tin_number"));
        user.setPagibigNumber(result.getString("pagibig_number"));
        user.setPosition(result.getString("position"));
        user.setDepartment(result.getString("department"));
        user.setDateHired(result.getDate("date_hired"));
        user.setStatus(result.getString("status"));
        user.setImmediateSupervisor(result.getInt("immediate_supervisor"));

        Benefits benefit = new Benefits(
                result.getDouble("basic_salary"),
                result.getDouble("gross_semi_monthly_rate"),
                result.getDouble("hourly_rate"),
                result.getDouble("rice_subsidy"),
                result.getDouble("phone_allowance"),
                result.getDouble("clothing_allowance")
            );
        user.setBenefits(benefit);

        Address address = new Address();
        address.setBarangay(result.getString("barangay"));
        address.setCity(result.getString("city"));
        address.setProvince(result.getString("province"));
        address.setStreet(result.getString("street"));
        address.setZipcode(result.getString("zipcode"));
        user.setAddress(address);
    }

    /**
     * Maps a result set of attendance rows to a list of {@link AttendanceRecord}.
     */
    public static List<AttendanceRecord> mapAttendanceList(ResultSet result) throws SQLException {
        List<AttendanceRecord> attendance = new ArrayList<>();
        while (result.next()) {
            attendance.add(new AttendanceRecord(
                    result.getInt(EMPLOYEE_ID),
                    result.getDate("date"),
                    result.getTime("time_in"),
                    result.getTime("time_out")));
        }
        return attendance;
    }

    /**
     * Maps a single {@code payslip_view} row to a {@link Payroll}.
     */
    public static Payroll mapPayroll(ResultSet result) throws SQLException {
        Payroll pr = new Payroll();
        pr.setGrossIncome(result.getDouble("gross_income"));
        pr.setHoursWorked(result.getDouble("hours_worked"));
        pr.setNetPay(result.getDouble("take_home_pay"));
        pr.setOvertimeHours(result.getDouble("overtime_hours"));
        pr.setPayrollID(result.getString("payslip_no"));
        pr.setWeekPeriodEnd(result.getDate("period_end"));
        pr.setWeekPeriodStart(result.getDate("period_start"));

        Deductions deduction = new Deductions();
        deduction.setPagIbigDeduction(result.getDouble("pagibig_deduction"));
        deduction.setPhilhealthDeduction(result.getDouble("philhealth_deduction"));
        deduction.setSssDeduction(result.getDouble("sss_deduction"));
        deduction.setTaxDeduction(result.getDouble("withholding_deduction"));
        pr.setDeductions(deduction);

        Employee emp = new Employee();
        String fullName = result.getString("employee_name");
        String[] parts = fullName.split(", ", 2);
        emp.setLastName(parts[0].trim());
        emp.setFirstName(parts[1].trim());

        String positionDepartment = result.getString("position_department");
        parts = positionDepartment.split(" / ", 2);
        emp.setEmployeeID(result.getInt(EMPLOYEE_ID));
        emp.setPosition(parts[0].trim());
        emp.setDepartment(parts[1].trim());

        Benefits benefit = new Benefits(
                result.getDouble("basic_salary"),
                result.getDouble("gross_income"),
                result.getDouble("hourly_rate"),
                result.getDouble("rice_subsidy"),
                result.getDouble("phone_allowance"),
                result.getDouble("clothing_allowance")
            );
        emp.setBenefits(benefit);
        pr.setEmployee(emp);
        return pr;
    }

    public static int getStatusId(Connection connection, String statusName) throws SQLException {
        String query = "SELECT status_id FROM status WHERE type = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, statusName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("status_id") : 0;
            }
        }
    }

    public static int getPositionId(Connection connection, String positionName) throws SQLException {
        String query = "SELECT position_id FROM positions WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, positionName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("position_id") : 0;
            }
        }
    }

    public static int getEmployeeAddressId(Connection connection, int employeeId) throws SQLException {
        String query = "SELECT address_id FROM employees WHERE " + EMPLOYEE_ID + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("address_id") : 0;
            }
        }
    }

    public static int getEmployeeSalaryId(Connection connection, int employeeId) throws SQLException {
        String query = "SELECT salary_id FROM employees WHERE " + EMPLOYEE_ID + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("salary_id") : 0;
            }
        }
    }
}
