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

    private static Connection conn = null;

    public static Connection getConnection() {
        return openConnection(false);
    }

    public static Connection getConnectionWithTransaction() {
        return openConnection(true);
    }

    private static Connection openConnection(boolean transactional) {
        // AppConfig is the single source for credentials. It also reads the
        // MOTORPH_DB_* names and config/db.local.properties used by R-04.
        String url = AppConfig.dbUrl();
        String user = AppConfig.dbUser();
        String password = AppConfig.dbPassword();

        try {
            conn = DriverManager.getConnection(url, user, password);
            if (transactional) {
                conn.setAutoCommit(false);
                System.out.println("Database connected with transaction.");
            } else {
                System.out.println("Database connected.");
            }
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
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
