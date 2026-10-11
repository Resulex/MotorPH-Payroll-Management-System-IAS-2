/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

/**
 *
 * @author Alex Resurreccion
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/payrollsystem_db_aoop";
    private static final String USER = "root";
    private static final String PASS = "Alex123!!";
    private static Connection conn = null;

    public static Connection getConnection() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("Database connected.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
    
    public static Connection getConnectionWithTransaction(){
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(false);
            System.out.println("Database connected with transaction.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
}
