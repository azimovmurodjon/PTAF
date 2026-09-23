package com.ptaf.db.validators;

import com.ptaf.db.handlers.DatabaseHandler;
import com.ptaf.utils.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * DatabaseConnectionValidator validates SQL Server database connectivity.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class provides a controlled and reusable way to confirm that the PTAF
 * database automation layer can successfully connect to the configured database.
 * </p>
 *
 * <p>
 * This is especially useful for enterprise environments where Microsoft SQL Server
 * access is handled through Windows Authentication instead of username/password.
 * </p>
 *
 * <p>
 * This class does not create its own JDBC logic. It uses DatabaseHandler so the
 * validation follows the same connection path used by real DB automation tests.
 * </p>
 *
 * <p>
 * Notes:
 * - This is a stateless utility class. All methods are static and side effects are
 *   limited to logging and using the DatabaseHandler to obtain a connection.
 * - No sensitive configuration values (like passwords) are logged by this class.
 * - Designed for use in test setup/validation steps; if the connection is required
 *   for a test to proceed, use assertDatabaseConnectionSuccessful() to fail fast.
 * </p>
 */
public class DatabaseConnectionValidator {

    /**
     * SLF4J logger used for info and error reporting during validation.
     * All messages that help troubleshoot connection problems are emitted here.
     */
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionValidator.class);

    /**
     * Private constructor prevents object creation because this class is used as
     * a utility validator.
     *
     * <p>
     * Throwing IllegalStateException is a common pattern to clearly indicate the
     * class should not be instantiated.
     * </p>
     */
    private DatabaseConnectionValidator() {
        // Prevent instantiation of this utility class.
        throw new IllegalStateException("Utility class");
    }

    /**
     * Validates that a SQL Server database connection can be opened successfully.
     *
     * <p>
     * Validation steps performed by this method:
     * </p>
     *
     * <ol>
     *     <li>Log a short, sanitized summary of the database configuration (no passwords).</li>
     *     <li>Obtain a JDBC Connection via DatabaseHandler.getConnection().</li>
     *     <li>Verify the returned Connection object is not null and is open.</li>
     *     <li>Retrieve DatabaseMetaData to confirm the connection is usable.</li>
     *     <li>Log database product and driver information for troubleshooting.</li>
     * </ol>
     *
     * <p>
     * Important behavior:
     * - This method never throws checked SQLExceptions to callers; it logs the error
     *   and returns false when problems occur.
     * - It is safe to call from setup code or condition checks where a boolean status is required.
     * </p>
     *
     * @return true when the database connection is successful and metadata can be retrieved; otherwise false.
     */
    public static boolean isDatabaseConnectionSuccessful() {
        try {
            // Start of validation workflow: provide a clear log message so test logs
            // show when DB validation attempts begin.
            logger.info("Starting SQL Server database connection validation.");

            // Log non-sensitive configuration values to aid troubleshooting.
            // This helps testers and administrators confirm which environment is being validated.
            logSanitizedDatabaseConfiguration();

            // Obtain a JDBC Connection using the framework's DatabaseHandler.
            // Using DatabaseHandler ensures the same connection path is used by real tests.
            Connection connection = DatabaseHandler.getConnection();

            // Defensive check: DatabaseHandler may return null if the connection could not be created.
            if (connection == null) {
                logger.error("Database connection validation failed. Connection object is null.");
                return false;
            }

            // Verify the connection is open. Some connection providers may return a closed connection
            // if the pool has been shut down or if the connection attempt failed silently.
            if (connection.isClosed()) {
                logger.error("Database connection validation failed. Connection is closed.");
                return false;
            }

            // Retrieve metadata to ensure the connection is usable for queries and that
            // the JDBC driver is functional. Accessing metadata exercises the connection beyond
            // a simple non-null check.
            DatabaseMetaData metaData = connection.getMetaData();

            // If we've reached this point without exceptions, consider the connection successful.
            logger.info("Database connection validation successful.");
            // Log the database and driver information for traceability in logs.
            logger.info("Connected database product name: {}", metaData.getDatabaseProductName());
            logger.info("Connected database product version: {}", metaData.getDatabaseProductVersion());
            logger.info("Connected JDBC driver name: {}", metaData.getDriverName());
            logger.info("Connected JDBC driver version: {}", metaData.getDriverVersion());

            return true;

        } catch (SQLException e) {
            // SQLExceptions typically indicate connectivity, authentication, or network issues.
            // Provide actionable guidance in the log message to help troubleshooting.
            logger.error(
                    "SQL Server database connection validation failed. " +
                            "Please verify server name, database name, authentication mode, network/VPN access, " +
                            "and Windows Authentication permissions.",
                    e
            );
            return false;

        } catch (Exception e) {
            // Catch-all for unexpected runtime problems (configuration errors, NPEs, etc.).
            // We avoid letting exceptions propagate so callers only need to check the boolean result.
            logger.error("Unexpected error occurred during database connection validation.", e);
            return false;
        }
    }

    /**
     * Validates the database connection and throws an AssertionError when the
     * connection is not successful.
     *
     * <p>
     * This method is useful for Cucumber steps or setup validations where the test
     * should fail immediately if the DB connection cannot be established. The thrown
     * AssertionError will typically be treated as a test failure by test runners.
     * </p>
     *
     * <p>
     * Usage example in a test setup:
     * <pre>
     *     DatabaseConnectionValidator.assertDatabaseConnectionSuccessful();
     * </pre>
     * </p>
     *
     * <p>
     * Note: This method delegates to isDatabaseConnectionSuccessful() and will not
     * attempt additional recovery or retries.
     * </p>
     */
    public static void assertDatabaseConnectionSuccessful() {
        // Use the boolean validation method so all logging and checks remain centralized.
        boolean isSuccessful = isDatabaseConnectionSuccessful();

        // If validation failed, throw an AssertionError to stop test execution and make the failure explicit.
        if (!isSuccessful) {
            throw new AssertionError(
                    "Database connection validation failed. " +
                            "Review SQL Server configuration in config.yml and framework logs for details."
            );
        }
    }

    /**
     * Logs safe database configuration values for troubleshooting.
     *
     * <p>
     * Sensitive values such as passwords are never logged. For Windows Authentication,
     * username and password are not required and should remain empty.
     * </p>
     *
     * <p>
     * This method calls ConfigurationProperties.getValue(...) for each important
     * configuration item and writes a concise summary to the logger at INFO level.
     * Testers should consult these logs to confirm the expected environment and
     * connection parameters without exposing secrets.
     * </p>
     */
    private static void logSanitizedDatabaseConfiguration() {
        // Retrieve commonly configured database properties. These keys correspond to the
        // framework's config.yml naming convention.
        // Note: do not retrieve or log any password-related property here.
        String dbType = ConfigurationProperties.getValue("database.db_type");
        String serverType = ConfigurationProperties.getValue("database.server_type");
        String serverName = ConfigurationProperties.getValue("database.server_name");
        String port = ConfigurationProperties.getValue("database.port");
        String databaseName = ConfigurationProperties.getValue("database.database_name");
        String authentication = ConfigurationProperties.getValue("database.authentication");
        String encrypt = ConfigurationProperties.getValue("database.encrypt");
        String trustServerCertificate = ConfigurationProperties.getValue("database.trust_server_certificate");

        // Output a small, sanitized configuration summary to help diagnose issues quickly.
        logger.info("Database configuration summary:");
        logger.info("database.db_type: {}", dbType);
        logger.info("database.server_type: {}", serverType);
        logger.info("database.server_name: {}", serverName);
        logger.info("database.port: {}", port);
        logger.info("database.database_name: {}", databaseName);
        logger.info("database.authentication: {}", authentication);
        logger.info("database.encrypt: {}", encrypt);
        logger.info("database.trust_server_certificate: {}", trustServerCertificate);
    }
}
