/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

/**
 *
 * @author Alex Resurreccion
 */
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

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

    private static String setting(String key, String defaultValue) {
        String fromEnvironment = System.getenv(key);
        if (isPresent(fromEnvironment)) {
            return fromEnvironment.trim();
        }
        String fromFile = localProperties().getProperty(key);
        if (isPresent(fromFile)) {
            return fromFile.trim();
        }
        return defaultValue;
    }

    private static Properties localProperties() {
        Properties properties = new Properties();
        if (!Files.isRegularFile(LOCAL_PROPERTIES)) {
            return properties;
        }
        try (InputStream input = Files.newInputStream(LOCAL_PROPERTIES)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read config/db.local.properties. "
                    + "Set MOTORPH_DB_USER and MOTORPH_DB_PASSWORD in the environment, "
                    + "or recreate the file from config/db.properties.example.");
        }
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
