/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Resolves configuration values that must not be hard-coded in the source tree,
 * such as database and e-mail credentials.
 *
 * <p>Resolution order for every key (the first non-blank value wins):</p>
 * <ol>
 *   <li>Java system property with the same name, e.g. {@code -Ddb.password=...}</li>
 *   <li>Environment variable listed for the key, e.g. {@code MOTORPH_DB_PASSWORD}</li>
 *   <li>{@code config.properties}, or the legacy {@code config/db.local.properties}
 *       used by the R-04 audit logging, loaded from {@code -Dmotorph.config=<path>}
 *       or from the working directory; the file is git-ignored and is never
 *       packaged into the build output</li>
 * </ol>
 *
 * <p>Property files may use either the canonical key ({@code db.user}) or the
 * environment-variable name ({@code MOTORPH_DB_USER}) used by older setups.</p>
 *
 * <p>Secrets are never logged and never included in exception messages.</p>
 *
 * @author Alex Resurreccion
 */
public final class AppConfig {

    /** System property that points at an alternative configuration file. */
    public static final String CONFIG_FILE_PROPERTY = "motorph.config";

    /** File name searched in the working directory when no explicit path is given. */
    public static final String CONFIG_FILE_NAME = "config.properties";

    /** Legacy local file used by the R-04 audit-logging change. */
    private static final String LEGACY_CONFIG_FILE_NAME = "config/db.local.properties";

    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/payrollsystem_db_aoop";
    private static final String DEFAULT_MAIL_HOST = "smtp.gmail.com";
    private static final String DEFAULT_MAIL_PORT = "587";

    private static final Properties FILE_VALUES = loadFileValues();

    private AppConfig() {
    }

    private static Properties loadFileValues() {
        Properties values = new Properties();
        String explicitPath = System.getProperty(CONFIG_FILE_PROPERTY);
        boolean explicit = explicitPath != null && !explicitPath.isBlank();
        Path path = explicit ? Path.of(explicitPath) : findLocalConfigFile();

        if (path == null) {
            // No local file is a valid setup: environment variables may be used instead.
            return values;
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(
                    "Configuration file not found: " + path.toAbsolutePath());
        }

        try (InputStream in = Files.newInputStream(path)) {
            values.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to read configuration file: " + path.toAbsolutePath(), e);
        }
        return values;
    }

    private static Path findLocalConfigFile() {
        Path current = Path.of(CONFIG_FILE_NAME);
        if (Files.isRegularFile(current)) {
            return current;
        }
        Path legacy = Path.of(LEGACY_CONFIG_FILE_NAME);
        return Files.isRegularFile(legacy) ? legacy : null;
    }

    /**
     * Returns the configured value for {@code key}, or {@code null} when it is not
     * set anywhere.
     *
     * @param key                 property name used in the config file and as a
     *                            system property
     * @param environmentVariables environment variables to try, in order
     * @return the resolved value or {@code null}
     */
    public static String get(String key, String... environmentVariables) {
        String value = System.getProperty(key);
        if (isBlank(value)) {
            for (String variable : environmentVariables) {
                value = System.getenv(variable);
                if (!isBlank(value)) {
                    break;
                }
            }
        }
        if (isBlank(value)) {
            // Older setups store MOTORPH_DB_* directly as keys in the file.
            for (String variable : environmentVariables) {
                value = FILE_VALUES.getProperty(variable);
                if (!isBlank(value)) {
                    break;
                }
            }
        }
        if (isBlank(value)) {
            value = FILE_VALUES.getProperty(key);
        }
        return isBlank(value) ? null : value.trim();
    }

    /**
     * Returns the configured value for {@code key}, failing fast when it is missing.
     *
     * @param key                 property name used in the config file and as a
     *                            system property
     * @param environmentVariables environment variables to try, in order
     * @return the resolved value, never {@code null}
     * @throws IllegalStateException when the value is not configured
     */
    public static String require(String key, String... environmentVariables) {
        String value = get(key, environmentVariables);
        if (value == null) {
            String hint = environmentVariables.length == 0
                    ? ""
                    : "Set the environment variable "
                            + String.join(" or ", environmentVariables) + ", or ";
            throw new IllegalStateException(
                    "Missing required configuration value '" + key + "'. " + hint
                            + "add it to " + CONFIG_FILE_NAME
                            + " (copy config.properties.example to get started).");
        }
        return value;
    }

    /** JDBC URL of the payroll database. */
    public static String dbUrl() {
        String value = get("db.url", "MOTORPH_DB_URL", "MYSQL_URL");
        return value == null ? DEFAULT_DB_URL : value;
    }

    /** Database user. */
    public static String dbUser() {
        return require("db.user", "MOTORPH_DB_USER", "MYSQL_USER");
    }

    /** Database password. */
    public static String dbPassword() {
        return require("db.password", "MOTORPH_DB_PASSWORD", "MYSQL_PASS");
    }

    /** Sender address used for outgoing e-mail, or {@code null} when not configured. */
    public static String mailFrom() {
        return get("mail.from", "MOTORPH_MAIL_USERNAME", "MAIL_FROM");
    }

    /** Password (app password) used for outgoing e-mail, or {@code null} when not configured. */
    public static String mailPassword() {
        return get("mail.password", "MOTORPH_MAIL_PASSWORD", "MAIL_PASSWORD");
    }

    /** SMTP host for outgoing e-mail. */
    public static String mailHost() {
        return getOrDefault("mail.host", DEFAULT_MAIL_HOST);
    }

    /** SMTP port for outgoing e-mail. */
    public static String mailPort() {
        return getOrDefault("mail.port", DEFAULT_MAIL_PORT);
    }

    /** Whether both the sender address and its password have been configured. */
    public static boolean isMailConfigured() {
        return mailFrom() != null && mailPassword() != null;
    }

    /**
     * Returns the configured value for {@code key}, or {@code defaultValue} when it
     * is not set anywhere.
     */
    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
