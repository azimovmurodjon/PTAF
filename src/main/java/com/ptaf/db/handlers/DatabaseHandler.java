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
     *
     * <p>
     * Using ThreadLocal ensures that each execution thread (for example, each test thread)
     * receives its own JDBC Connection instance. This prevents sharing a single Connection
     * across threads which could cause concurrency issues and unpredictable behavior.
     * </p>
     */
    private static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();

    /**
     * Private constructor prevents object creation because this is a utility/lifecycle class.
     *
     * <p>
     * The class exposes only static helpers to manage connection lifecycle. Instantiating
     * this class is not intended and is prevented to make usage obvious and safe.
     * </p>
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
     * <p>
     * Callers (for example test steps) should use this method to obtain the JDBC Connection
     * and must not close the connection directly. Framework teardown should call closeConnection()
     * to properly dispose of the connection associated with the current thread.
     * </p>
     *
     * @return active JDBC Connection for the current thread.
     * @throws SQLException when database connection cannot be created.
     */
    public static Connection getConnection() throws SQLException {
        // Retrieve the current thread's Connection, if previously created.
        Connection currentConnection = connectionThreadLocal.get();

        try {
            // If no Connection exists or it was closed, create and store a new one for this thread.
            if (currentConnection == null || currentConnection.isClosed()) {
                logger.info("No active database connection found for current thread. Creating new connection.");
                connectionThreadLocal.set(createConnection());
            }
        } catch (SQLException e) {
            // Re-throw after logging to ensure callers know the failure reason.
            logger.error("Error checking or creating database connection for current thread.", e);
            throw e;
        }

        // Return the ThreadLocal-stored connection. This is guaranteed non-null and open here.
        return connectionThreadLocal.get();
    }

    /**
     * Closes the database connection for the current thread and removes it from ThreadLocal.
     *
     * <p>
     * This method should be called from framework teardown, such as an @After hook,
     * to prevent memory leaks and avoid stale database sessions.
     * </p>
     *
     * <p>
     * Implementation notes for testers:
     * - Do not call Connection.close() directly on the object returned by getConnection().
     *   Use this helper so the Framework can also remove the reference from ThreadLocal.
     * - This method is safe to call multiple times; it will only attempt to close when
     *   a connection exists and is open.
     * </p>
     */
    public static void closeConnection() {
        try {
            // Fetch the connection associated with the current thread.
            Connection connection = connectionThreadLocal.get();

            if (connection != null && !connection.isClosed()) {
                // Close the connection and log success.
                connection.close();
                logger.info("Database connection closed successfully for current thread.");
            }
        } catch (SQLException e) {
            // Log errors during close but do not rethrow because teardown should continue.
            logger.error("Failed to close database connection for current thread.", e);
        } finally {
            // Always remove the reference from ThreadLocal to avoid potential memory leaks.
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
     * <p>
     * Supported configuration keys (in config.yml):
     * - database.db_type (defaults to "sqlserver")
     * - database.authentication (defaults to "windows") -> "windows" or "sqlserver"
     * - For sqlserver auth:
     *   - database.username
     *   - database.password_env_variable (the name of an environment variable that holds the password)
     * </p>
     *
     * @return new JDBC Connection.
     * @throws SQLException when connection creation fails.
     */
    private static Connection createConnection() throws SQLException {
        // Read basic connection configuration with sensible defaults.
        String dbType = getConfigValue("database.db_type", "sqlserver");
        String authentication = getConfigValue("database.authentication", "windows");

        // Only SQL Server is supported in this implementation.
        if (!"sqlserver".equalsIgnoreCase(dbType)) {
            throw new IllegalArgumentException(
                    "Unsupported database.db_type: " + dbType + ". Current enterprise setup supports sqlserver."
            );
        }

        // Build the JDBC URL based on configuration and requested authentication mode.
        String connectionUrl = buildSqlServerConnectionUrl(authentication);

        try {
            logger.info("Attempting SQL Server connection using authentication mode: {}", authentication);
            // Avoid logging the URL with sensitive details; the prepared URL is noted but not printed.
            logger.info("SQL Server JDBC URL prepared successfully. Sensitive information is not logged.");

            if ("windows".equalsIgnoreCase(authentication)) {
                /*
                 * Windows Integrated Authentication:
                 * No username or password is required here.
                 *
                 * The connection uses the logged-in Windows/domain user account
                 * that is running the automation framework.
                 *
                 * Note for testers: Ensure the OS user running tests has database access.
                 */
                return DriverManager.getConnection(connectionUrl);
            }

            if ("sqlserver".equalsIgnoreCase(authentication)) {
                /*
                 * SQL Server Authentication:
                 * Username is read from config.yml.
                 * Password is read securely from the environment variable configured in config.yml.
                 *
                 * Security note: passwords are not stored in config.yml; instead, the framework
                 * reads the name of an environment variable which must contain the password.
                 */
                String username = ConfigurationProperties.getValue("database.username");
                String passwordEnvVariable = ConfigurationProperties.getValue("database.password_env_variable");

                // Validate required properties for SQL Server authentication mode.
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

                // Read the actual password from the OS environment.
                String password = System.getenv(passwordEnvVariable);

                if (isBlank(password)) {
                    throw new IllegalArgumentException(
                            "Database password environment variable '" + passwordEnvVariable + "' is not set or is empty."
                    );
                }

                // Create a connection using username and password.
                return DriverManager.getConnection(connectionUrl, username, password);
            }

            // If the configured authentication type is unknown, raise an error.
            throw new IllegalArgumentException(
                    "Unsupported database.authentication value: " + authentication
                            + ". Supported values are: windows, sqlserver."
            );

        } catch (IllegalArgumentException e) {
            // Log configuration issues clearly to assist testers in diagnosing misconfiguration.
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
     * <p>
     * Important notes for testers and integrators:
     * - server_name and database.database_name are required and validated.
     * - Default values are applied for port, encrypt, trustServerCertificate, login timeout and application name.
     * - When authentication is "windows", integratedSecurity=true is appended so the JDBC driver uses the Windows token.
     * </p>
     *
     * @param authentication configured authentication mode.
     * @return SQL Server JDBC connection URL.
     */
    private static String buildSqlServerConnectionUrl(String authentication) {
        // Read server, port and database-related configuration values; apply defaults where appropriate.
        String serverName = ConfigurationProperties.getValue("database.server_name");
        String port = getConfigValue("database.port", "1433");
        String databaseName = ConfigurationProperties.getValue("database.database_name");
        String encrypt = getConfigValue("database.encrypt", "true");
        String trustServerCertificate = getConfigValue("database.trust_server_certificate", "true");
        String loginTimeout = getConfigValue("database.login_timeout_seconds", "30");
        String applicationName = getConfigValue("database.application_name", "PTAF Automation Framework");

        // Validate required parameters and provide clear error messages for testers.
        if (isBlank(serverName)) {
            throw new IllegalArgumentException("database.server_name is required in config.yml.");
        }

        if (isBlank(databaseName)) {
            throw new IllegalArgumentException("database.database_name is required in config.yml.");
        }

        // Build the JDBC URL in a safe manner using StringBuilder.
        StringBuilder urlBuilder = new StringBuilder();

        urlBuilder.append("jdbc:sqlserver://")
                .append(serverName.trim());

        // Only append port if provided (default 1433 is used if absent).
        if (!isBlank(port)) {
            urlBuilder.append(":").append(port.trim());
        }

        // Append required and optional connection parameters.
        urlBuilder.append(";databaseName=").append(databaseName.trim())
                .append(";encrypt=").append(encrypt.trim())
                .append(";trustServerCertificate=").append(trustServerCertificate.trim())
                .append(";loginTimeout=").append(loginTimeout.trim())
                .append(";applicationName=").append(applicationName.trim());

        if ("windows".equalsIgnoreCase(authentication)) {
            /*
             * integratedSecurity=true tells the SQL Server JDBC driver to use
             * Windows Authentication instead of username/password credentials.
             *
             * Note: On some platforms, the appropriate SQL Server JDBC driver DLL or native
             * library must be available on the host for integrated security to function.
             * Testers should verify the test runner environment has the required setup.
             */
            urlBuilder.append(";integratedSecurity=true");
        }

        // Terminate the URL with a semicolon (some drivers accept it; keep consistent with examples).
        urlBuilder.append(";");

        return urlBuilder.toString();
    }

    /**
     * Safely reads a configuration value and applies a default if the value is missing.
     *
     * <p>
     * This helper centralizes default behavior so callers do not need to repeatedly
     * check for null/empty configuration values.
     * </p>
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
