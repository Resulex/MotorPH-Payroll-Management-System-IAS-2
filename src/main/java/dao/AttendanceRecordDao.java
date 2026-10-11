/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import models.AttendanceRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import utils.DbConnection;

/**
 *
 * @author Alex Resurreccion
 */
public class AttendanceRecordDao {
    private Connection conn;
    private ResultSet rs = null;
    private PreparedStatement preparedStatement = null;

    // Constructor with custom connection
    public AttendanceRecordDao(Connection conn) {
        this.conn = conn;
    }

    // Constructor with default connection
    public AttendanceRecordDao() {
        this.conn = DbConnection.getConnection();
    }
    
    public AttendanceRecord getAttendanceRecord(int employeeID, Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = formatter.format(date);

        try {
            // First: Try to retrieve the existing attendance record
            String selectQuery = "SELECT * FROM attendance_record WHERE employee_id = ? AND date = ?";
            preparedStatement = conn.prepareStatement(selectQuery);
            preparedStatement.setInt(1, employeeID);
            preparedStatement.setString(2, formattedDate);

            rs = preparedStatement.executeQuery();

            if (rs.next()) {
                return new AttendanceRecord(
                    rs.getInt("employee_id"),
                    rs.getDate("date"),
                    rs.getTime("time_in"),
                    rs.getTime("time_out")
                );
            } else {
                // No existing record, so insert a new one
                String insertQuery = "INSERT INTO attendance_record (employee_id, date, time_in, time_out) VALUES (?, ?, NULL, NULL)";
                preparedStatement = conn.prepareStatement(insertQuery);
                preparedStatement.setInt(1, employeeID);
                preparedStatement.setString(2, formattedDate);
                preparedStatement.executeUpdate();

                // Retrieve the newly created record
                preparedStatement = conn.prepareStatement(selectQuery);
                preparedStatement.setInt(1, employeeID);
                preparedStatement.setString(2, formattedDate);
                rs = preparedStatement.executeQuery();

                if (rs.next()) {
                    return new AttendanceRecord(
                        rs.getInt("employee_id"),
                        rs.getDate("date"),
                        rs.getTime("time_in"),
                        rs.getTime("time_out")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error while executing SQL query!");
            e.printStackTrace();
        }

        return null;
    }
}

