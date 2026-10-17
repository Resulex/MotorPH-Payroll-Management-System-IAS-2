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

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(
                    AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword());
            System.out.println("Database connected.");
            return conn;
        } catch (SQLException e) {
            throw connectionFailure(e);
        }
    }

    public static Connection getConnectionWithTransaction() {
        try {
            Connection conn = DriverManager.getConnection(
                    AppConfig.dbUrl(), AppConfig.dbUser(), AppConfig.dbPassword());
            conn.setAutoCommit(false);
            System.out.println("Database connected with transaction.");
            return conn;
        } catch (SQLException e) {
            throw connectionFailure(e);
        }
    }

    private static IllegalStateException connectionFailure(SQLException cause) {
        return new IllegalStateException(
                "Could not connect to the payroll database. Check that MySQL is running and "
                + "that db.url, db.user and db.password are configured (environment variables "
                + "MOTORPH_DB_URL, MOTORPH_DB_USER, MOTORPH_DB_PASSWORD, or a local "
                + AppConfig.CONFIG_FILE_NAME + " file).", cause);
    }
}
