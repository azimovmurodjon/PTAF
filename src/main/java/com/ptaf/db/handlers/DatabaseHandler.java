package com.ptaf.db.handlers;

import com.ptaf.utils.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseHandler manages database connection lifecycle for PTAF database automation.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class centralizes database connection creation, reuse, and cleanup.
 * It uses ThreadLocal to ensure each parallel test execution thread receives
 * its own isolated database connection.
 * </p>
 *
 * <p>
 * Supported Connection Model:
 * This implementation supports Microsoft SQL Server using either:
 * </p>
 *
 * <ul>
 *     <li>Windows Integrated Authentication</li>
 *     <li>SQL Server username/password authentication</li>
 * </ul>
 *
 * <p>
 * For enterprise environments where users already have database access through
 * their Windows domain account, the framework can connect using Windows Authentication
 * without storing or passing a username or password.
 * </p>
 *
 * <p>
 * Configuration is controlled from config.yml under the "database" section.
 * </p>
 */
public class DatabaseHandler {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHandler.class);

    /**
     * ThreadLocal connection storage provides safe database execution for parallel scenarios.
     */
    private static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    /**
     * Private constructor prevents object creation because this is a utility/lifecycle class.
     */
    private DatabaseHandler() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns the active database connection for the current execution thread.
     *
     * <p>
     * If no connection exists, or if the previous connection was closed,
     * a new connection is created using the database configuration from config.yml.
     * </p>
     *
     * @return active JDBC Connection for the current thread.
     * @throws SQLException when database connection cannot be created.
     */
    public static Connection getConnection() throws SQLException {
        Connection currentConnection = connectionThreadLocal.get();

        if (currentConnection == null || currentConnection.isClosed()) {
            logger.info("No active database connection found for current thread. Creating new connection.");
            connectionThreadLocal.set(createConnection());
        }

        return connectionThreadLocal.get();
    }

    /**
     * Closes the database connection for the current thread and removes it from ThreadLocal.
     *
     * <p>
     * This method should be called from framework teardown, such as an @After hook,
     * to prevent memory leaks and avoid stale database sessions.
     * </p>
     */
    public static void closeConnection() {
        try {
            Connection connection = connectionThreadLocal.get();

            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed successfully for current thread.");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection for current thread.", e);
        } finally {
            connectionThreadLocal.remove();
        }
    }

    /**
     * Creates a new database connection based on config.yml settings.
     *
     * <p>
     * For your enterprise SQL Server setup, Windows Authentication is supported
     * using integratedSecurity=true in the JDBC connection URL.
     * </p>
     *
     * @return new JDBC Connection.
     * @throws SQLException when connection creation fails.
     */
    private static Connection createConnection() throws SQLException {
        String dbType = getConfigValue("database.db_type", "sqlserver");
        String authentication = getConfigValue("database.authentication", "windows");

        if (!"sqlserver".equalsIgnoreCase(dbType)) {
            throw new IllegalArgumentException(
                    "Unsupported database.db_type: " + dbType + ". Current enterprise setup supports sqlserver."
            );
        }

        String connectionUrl = buildSqlServerConnectionUrl(authentication);

        try {
            logger.info("Attempting SQL Server connection using authentication mode: {}", authentication);
            logger.info("SQL Server JDBC URL prepared successfully. Sensitive information is not logged.");

            if ("windows".equalsIgnoreCase(authentication)) {
                /*
                 * Windows Integrated Authentication:
                 * No username or password is required here.
                 *
                 * The connection uses the logged-in Windows/domain user account
                 * that is running the automation framework.
                 */
                return DriverManager.getConnection(connectionUrl);
            }

            if ("sqlserver".equalsIgnoreCase(authentication)) {
                /*
                 * SQL Server Authentication:
                 * Username is read from config.yml.
                 * Password is read securely from the environment variable configured in config.yml.
                 */
                String username = ConfigurationProperties.getValue("database.username");
                String passwordEnvVariable = ConfigurationProperties.getValue("database.password_env_variable");

                if (isBlank(username)) {
                    throw new IllegalArgumentException(
                            "database.username is required when database.authentication is set to sqlserver."
                    );
                }

                if (isBlank(passwordEnvVariable)) {
                    throw new IllegalArgumentException(
                            "database.password_env_variable is required when database.authentication is set to sqlserver."
                    );
                }

                String password = System.getenv(passwordEnvVariable);

                if (isBlank(password)) {
                    throw new IllegalArgumentException(
                            "Database password environment variable '" + passwordEnvVariable + "' is not set or is empty."
                    );
                }

                return DriverManager.getConnection(connectionUrl, username, password);
            }

            throw new IllegalArgumentException(
                    "Unsupported database.authentication value: " + authentication
                            + ". Supported values are: windows, sqlserver."
            );

        } catch (IllegalArgumentException e) {
            logger.error("Database configuration error.", e);
            throw e;
        }
    }

    /**
     * Builds a Microsoft SQL Server JDBC connection URL from config.yml values.
     *
     * <p>
     * Example generated URL:
     * jdbc:sqlserver://VSDISSQLQA01:1433;databaseName=YourDB;
     * encrypt=true;trustServerCertificate=true;integratedSecurity=true;
     * loginTimeout=30;applicationName=PTAF Automation Framework;
     * </p>
     *
     * @param authentication configured authentication mode.
     * @return SQL Server JDBC connection URL.
     */
    private static String buildSqlServerConnectionUrl(String authentication) {
        String serverName = ConfigurationProperties.getValue("database.server_name");
        String port = getConfigValue("database.port", "1433");
        String databaseName = ConfigurationProperties.getValue("database.database_name");
        String encrypt = getConfigValue("database.encrypt", "true");
        String trustServerCertificate = getConfigValue("database.trust_server_certificate", "true");
        String loginTimeout = getConfigValue("database.login_timeout_seconds", "30");
        String applicationName = getConfigValue("database.application_name", "PTAF Automation Framework");

        if (isBlank(serverName)) {
            throw new IllegalArgumentException("database.server_name is required in config.yml.");
        }

        if (isBlank(databaseName)) {
            throw new IllegalArgumentException("database.database_name is required in config.yml.");
        }

        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("jdbc:sqlserver://")
                .append(serverName.trim());

        if (!isBlank(port)) {
            urlBuilder.append(":").append(port.trim());
        }

        urlBuilder.append(";databaseName=").append(databaseName.trim())
                .append(";encrypt=").append(encrypt.trim())
                .append(";trustServerCertificate=").append(trustServerCertificate.trim())
                .append(";loginTimeout=").append(loginTimeout.trim())
                .append(";applicationName=").append(applicationName.trim());

        if ("windows".equalsIgnoreCase(authentication)) {
            /*
             * integratedSecurity=true tells the SQL Server JDBC driver to use
             * Windows Authentication instead of username/password credentials.
             */
            urlBuilder.append(";integratedSecurity=true");
        }

        urlBuilder.append(";");

        return urlBuilder.toString();
    }

    /**
     * Safely reads a configuration value and applies a default if the value is missing.
     *
     * @param key          config.yml key.
     * @param defaultValue fallback value.
     * @return configured value or default value.
     */
    private static String getConfigValue(String key, String defaultValue) {
        String value = ConfigurationProperties.getValue(key);
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * Checks whether a String value is null, empty, or only whitespace.
     *
     * @param value String value to validate.
     * @return true when value is null, empty, or blank.
     */
    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}