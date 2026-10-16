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
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/payrollsystem_db_aoop";
    private static final String USER_KEY = "MOTORPH_DB_USER";
    private static final String PASSWORD_KEY = "MOTORPH_DB_PASSWORD";
    private static final String URL_KEY = "MOTORPH_DB_URL";
    private static final Path LOCAL_PROPERTIES = Path.of("config", "db.local.properties");
    private static Connection conn = null;

    public static Connection getConnection() {
        return openConnection(false);
    }
    
    public static Connection getConnectionWithTransaction(){
        return openConnection(true);
    }

    private static Connection openConnection(boolean transactional) {
        String url = setting(URL_KEY, DEFAULT_URL);
        String user = setting(USER_KEY, null);
        String password = setting(PASSWORD_KEY, null);
        if (!isPresent(user) || !isPresent(password)) {
            throw new IllegalStateException(
                    "Missing MySQL credentials. Set MOTORPH_DB_USER and MOTORPH_DB_PASSWORD "
                    + "as environment variables, or create config/db.local.properties from "
                    + "config/db.properties.example.");
        }

        try {
            conn = DriverManager.getConnection(url, user, password);
            if (transactional) {
                conn.setAutoCommit(false);
                System.out.println("Database connected with transaction.");
            } else {
                System.out.println("Database connected.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
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
