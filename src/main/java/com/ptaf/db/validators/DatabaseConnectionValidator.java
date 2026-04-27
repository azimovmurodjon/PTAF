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
 */
public class DatabaseConnectionValidator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionValidator.class);

    /**
     * Private constructor prevents object creation because this class is used as
     * a utility validator.
     */
    private DatabaseConnectionValidator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Validates that a SQL Server database connection can be opened successfully.
     *
     * <p>
     * This method verifies:
     * </p>
     *
     * <ul>
     *     <li>The framework can create a JDBC connection.</li>
     *     <li>The connection is not closed.</li>
     *     <li>Database metadata can be retrieved.</li>
     * </ul>
     *
     * @return true when the database connection is successful; otherwise false.
     */
    public static boolean isDatabaseConnectionSuccessful() {
        try {
            logger.info("Starting SQL Server database connection validation.");

            logSanitizedDatabaseConfiguration();

            Connection connection = DatabaseHandler.getConnection();

            if (connection == null) {
                logger.error("Database connection validation failed. Connection object is null.");
                return false;
            }

            if (connection.isClosed()) {
                logger.error("Database connection validation failed. Connection is closed.");
                return false;
            }

            DatabaseMetaData metaData = connection.getMetaData();

            logger.info("Database connection validation successful.");
            logger.info("Connected database product name: {}", metaData.getDatabaseProductName());
            logger.info("Connected database product version: {}", metaData.getDatabaseProductVersion());
            logger.info("Connected JDBC driver name: {}", metaData.getDriverName());
            logger.info("Connected JDBC driver version: {}", metaData.getDriverVersion());

            return true;

        } catch (SQLException e) {
            logger.error(
                    "SQL Server database connection validation failed. " +
                            "Please verify server name, database name, authentication mode, network/VPN access, " +
                            "and Windows Authentication permissions.",
                    e
            );
            return false;

        } catch (Exception e) {
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
     * should fail immediately if the DB connection cannot be established.
     * </p>
     */
    public static void assertDatabaseConnectionSuccessful() {
        boolean isSuccessful = isDatabaseConnectionSuccessful();

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
     */
    private static void logSanitizedDatabaseConfiguration() {
        String dbType = ConfigurationProperties.getValue("database.db_type");
        String serverType = ConfigurationProperties.getValue("database.server_type");
        String serverName = ConfigurationProperties.getValue("database.server_name");
        String port = ConfigurationProperties.getValue("database.port");
        String databaseName = ConfigurationProperties.getValue("database.database_name");
        String authentication = ConfigurationProperties.getValue("database.authentication");
        String encrypt = ConfigurationProperties.getValue("database.encrypt");
        String trustServerCertificate = ConfigurationProperties.getValue("database.trust_server_certificate");

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