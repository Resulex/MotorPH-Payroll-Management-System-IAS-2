/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UnitTesing;

import models.Employee;
import models.HRPersonnel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import models.LeaveRequest;

/**
 *
 * @author Alex Resurreccion
 */
public class LeaveRequestTest {
    
    @Test
    public void testLeaveRequestSettersAndGetters() {
        Employee employee = new Employee();
        HRPersonnel hrPersonnel = new HRPersonnel();
        Date startDate = new Date();
        Date endDate = new Date();
        Date dateCreated = new Date();
        int leaveID = 101;
        String leaveType = "Sick Leave";
        int duration = 3;
        String leaveStatus = "Pending";
        String reason = "Flu";
        
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployee(employee);
        leaveRequest.setApprovedBy(hrPersonnel);
        leaveRequest.setStartDate(startDate);
        leaveRequest.setEndDate(endDate);
        leaveRequest.setDateCreated(dateCreated);
        leaveRequest.setLeaveID(leaveID);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setDuration(duration);
        leaveRequest.setLeaveStatus(leaveStatus);
        leaveRequest.setReason(reason);
        
        assertEquals(employee, leaveRequest.getEmployee());
        assertEquals(hrPersonnel, leaveRequest.getApprovedBy());
        assertEquals(startDate, leaveRequest.getStartDate());
        assertEquals(endDate, leaveRequest.getEndDate());
        assertEquals(dateCreated, leaveRequest.getDateCreated());
        assertEquals(leaveID, leaveRequest.getLeaveID());
        assertEquals(leaveType, leaveRequest.getLeaveType());
        assertEquals(duration, leaveRequest.getDuration());
        assertEquals(leaveStatus, leaveRequest.getLeaveStatus());
        assertEquals(reason, leaveRequest.getReason());
    }

    @Test
    public void testParameterizedConstructor() {
        Employee employee = new Employee();
        HRPersonnel hrPersonnel = new HRPersonnel();
        Date startDate = new Date();
        Date endDate = new Date();
        LeaveRequest leaveRequest = new LeaveRequest(employee, 202, "Vacation Leave",
                startDate, endDate, 2, "PENDING", hrPersonnel);

        assertEquals(employee, leaveRequest.getEmployee());
        assertEquals(202, leaveRequest.getLeaveID());
        assertEquals("Vacation Leave", leaveRequest.getLeaveType());
        assertEquals(startDate, leaveRequest.getStartDate());
        assertEquals(endDate, leaveRequest.getEndDate());
        assertEquals(2, leaveRequest.getDuration());
        assertEquals("PENDING", leaveRequest.getLeaveStatus());
        assertEquals(hrPersonnel, leaveRequest.getApprovedBy());
        assertNull(leaveRequest.getReason());
    }
}

