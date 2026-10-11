/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Time;
import java.util.Date;
import models.AttendanceRecord;

/**
 *
 * @author Alex Resurreccion
 */
public class AttendanceRecordTest {   private AttendanceRecord attendanceRecord;
    private Date testDate;
    private Time timeIn;
    private Time timeOut;

    @BeforeEach
    void setUp() {
        // Set up the initial date and times for the attendance record
        testDate = new Date(System.currentTimeMillis());  // Current date
        timeIn = Time.valueOf("08:00:00");  // Time when the employee clocks in
        timeOut = Time.valueOf("17:00:00");  // Time when the employee clocks out

        // Create an AttendanceRecord for an employee (ID: 1001) for the test date
        attendanceRecord = new AttendanceRecord(1001, testDate, timeIn, timeOut);
    }

    @Test
    void testGetAndSetEmployeeId() {
        // Verify the employee ID getter and setter
        assertEquals(1001, attendanceRecord.getEmployeeId());
    }

    @Test
    void testGetAndSetDate() {
        Date newDate = new Date(System.currentTimeMillis());  // New test date
        attendanceRecord.setDate(newDate);
        // Check if the date is properly set
        assertEquals(newDate, attendanceRecord.getDate());
    }

    @Test
    void testGetAndSetTimeIn() {
        Time newTimeIn = Time.valueOf("09:00:00");  // New time for clock-in
        attendanceRecord.setTimeIn(newTimeIn);
        // Check if the timeIn is properly set
        assertEquals(newTimeIn, attendanceRecord.getTimeIn());
    }

    @Test
    void testCalculateWorkHours() {
        // Test that the calculation of work hours is correct (9 hours in this case)
        assertEquals(9.0, attendanceRecord.calculateWorkHours(), 0.01);
    }
    
    @Test
    void testIsTimeSet() {
    assertTrue(attendanceRecord.isTimeSet());
    }

    @Test
    void testGetTimeOut() {
        assertEquals(timeOut, attendanceRecord.getTimeOut());
    }

    @Test
    void testTwoArgConstructor() {
        AttendanceRecord attendance = new AttendanceRecord(1002, testDate);
        assertEquals(1002, attendance.getEmployeeId());
        assertEquals(testDate, attendance.getDate());
        assertNull(attendance.getTimeIn());
        assertNull(attendance.getTimeOut());
        assertFalse(attendance.isTimeSet());
    }

}