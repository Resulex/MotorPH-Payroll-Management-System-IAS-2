/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import models.AccountAccess;
import models.Employee;
import models.HRPersonnel;
import models.LeaveRequest;
import utils.DbConnection;
import utils.EmailSender;

/**
 *
 * @author Alex Resurreccion
 */
public class EmailSenderDao {
    private Connection conn;
    private ResultSet rs = null;
    private PreparedStatement preparedStatement = null;

    // Constructor with custom connection
    public EmailSenderDao(Connection conn) {
        this.conn = conn;
    }

    // Constructor with default connection
    public EmailSenderDao() {
        this.conn = DbConnection.getConnection();
    }
    
    public boolean newLeaveNotification(LeaveRequest leaveRequest){
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        String formattedStart = sdf.format(leaveRequest.getStartDate());
        String formattedEnd = sdf.format(leaveRequest.getEndDate());
        Employee employee = leaveRequest.getEmployee();
        EmailSender emailSender = new EmailSender();
        String to = "motorPHHRTeam@mailinator.com"; //Assuming all HR is tagged on this email
        String subject = String.format(
            "New leave request from %s %s for %d days",
            employee.getFirstName(),
            employee.getLastName(),
            leaveRequest.getDuration()
        );

        String message = String.format(
            "Hi HR Team,\n\n"
            + "Employee #%d (%s %s) has submitted a new leave request from %s until %s.\n"
            + "Please log in to the system to approve or decline the request.\n\n"
            + "Thank you,\n\n"
            + "MotorPH Auto Email. Do not reply!",
            employee.getEmployeeID(),
            employee.getFirstName(),
            employee.getLastName(),
            formattedStart,
            formattedEnd
        );
        
        return emailSender.sendEmail(to, subject, message);
    }
    
    public boolean newLeaveUpdateNotification(LeaveRequest leaveRequest){
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        String formattedStart = sdf.format(leaveRequest.getStartDate());
        String formattedEnd = sdf.format(leaveRequest.getEndDate());
        Employee employee = leaveRequest.getEmployee();
        EmailSender emailSender = new EmailSender();
        AccountAccessDao accountAccessDao = new AccountAccessDao();
        AccountAccess employeeAccount = accountAccessDao.getAccountEmail(employee.getEmployeeID());
        String to = employeeAccount.getEmail();
        String subject = String.format(
            "Leave Request #%d has been %s",
            leaveRequest.getLeaveID(),
            leaveRequest.getLeaveStatus()
        );

        String message = String.format(
            "Hi %s,\n\n"
            + "Your leave request from %s until %s has been %s by the HR team.\n\n"
            + "Thank you,\n\n"
            + "MotorPH Auto Email. Do not reply!",
            employee.getFirstName(),
            formattedStart,
            formattedEnd,
            leaveRequest.getLeaveStatus()
        );
        
        return emailSender.sendEmail(to, subject, message);
    }
}
