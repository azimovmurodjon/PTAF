package com.ptaf.db.performer;

import com.ptaf.utils.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseActionPerformer contains the low-level database execution logic.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class is responsible only for executing SQL statements against an active
 * JDBC Connection. It does not create or close database connections. Connection
 * lifecycle is managed by DatabaseHandler.
 * </p>
 *
 * <p>
 * SQL Server Compatibility:
 * This class is designed to work with Microsoft SQL Server through JDBC and uses
 * PreparedStatement for all query and update execution.
 * </p>
 *
 * <p>
 * Security Standard:
 * PreparedStatement is used to safely bind all dynamic parameters. This helps
 * reduce SQL injection risk and keeps query execution consistent across teams.
 * </p>
 *
 * <p>
 * Configuration:
 * SQL execution behavior can be controlled from config.yml under:
 * </p>
 *
 * <pre>
 * database:
 *   query_timeout_seconds: "60"
 *   fetch_size: "500"
 * </pre>
 */
public class DatabaseActionPerformer {

    // SLF4J logger used to emit informational, debug and error logs related to DB operations.
    private static final Logger logger = LoggerFactory.getLogger(DatabaseActionPerformer.class);

    /**
     * Default SQL timeout in seconds when config.yml does not provide a value.
     */
    private static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 60;

    /**
     * Default fetch size when config.yml does not provide a value.
     */
    private static final int DEFAULT_FETCH_SIZE = 500;

    /**
     * Public constructor kept for compatibility with DatabaseActionImpl.
     *
     * <p>
     * This class does not maintain any instance state, so the constructor is
     * intentionally empty. Kept public to allow straightforward instantiation
     * by framework classes and unit tests.
     * </p>
     */
    public DatabaseActionPerformer() {
        // Constructor intentionally kept public because DatabaseActionImpl creates an instance of this performer.
    }

    /**
     * Executes a SELECT query and returns the results as a list of maps.
     *
     * <p>
     * Each map represents one database row. Column labels are used as keys.
     * LinkedHashMap is used instead of HashMap to preserve the column order returned
     * by SQL Server. This makes logs, debugging, and report output easier to read.
     * </p>
     *
     * <p>
     * Behavior notes for testers:
     * - If params is null, this method treats it as an empty parameter list.
     * - The method will not close the provided Connection; it only closes the
     *   PreparedStatement and ResultSet it creates.
     * - Returns an empty list (not null) if the query returns zero rows.
     * </p>
     *
     * @param connection active JDBC database connection. Must be non-null and open.
     * @param sql        SQL SELECT statement with optional '?' placeholders.
     * @param params     parameter values to bind into the PreparedStatement. May be null.
     * @return list of result rows. Returns an empty list when no data is found.
     * @throws SQLException when SQL execution fails.
     */
    public List<Map<String, Object>> executeQuery(Connection connection, String sql, List<Object> params) throws SQLException {
        // Ensure the connection is valid before attempting to prepare a statement.
        validateConnection(connection);

        // Ensure the SQL content is valid (non-null and non-empty).
        validateSql(sql);

        // Convert a potentially null params list into an empty list to simplify binding logic.
        List<Object> safeParams = normalizeParams(params);

        // Prepare the return container. Using ArrayList as callers may expect random access.
        List<Map<String, Object>> results = new ArrayList<>();

        logger.info("Executing database SELECT query.");
        logger.debug("SQL query: {}", sql);
        logger.debug("SQL parameter count: {}", safeParams.size());

        // Use try-with-resources to ensure PreparedStatement and ResultSet are closed promptly.
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            // Apply framework-configured statement options such as timeout and fetch size.
            configurePreparedStatement(preparedStatement);

            // Bind all provided parameters into the PreparedStatement using JDBC positional binding.
            bindParameters(preparedStatement, safeParams);

            // Execute the query and iterate the ResultSet. ResultSet is also auto-closed by try-with-resources.
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                // Number of columns returned by the query; used to iterate columns per row.
                int columnCount = metaData.getColumnCount();

                // Iterate all rows returned by the query.
                while (resultSet.next()) {
                    // Preserve column order as returned by the DB by using a LinkedHashMap.
                    Map<String, Object> row = new LinkedHashMap<>();

                    // JDBC columns are 1-based: loop from 1 to columnCount inclusive.
                    for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                        // Use column label (alias if provided) rather than column name for better compatibility.
                        String columnLabel = metaData.getColumnLabel(columnIndex);
                        // Retrieve the column value as an Object. Caller test code should cast as needed.
                        Object columnValue = resultSet.getObject(columnIndex);

                        row.put(columnLabel, columnValue);
                    }

                    // Add the fully populated row map to the result list.
                    results.add(row);
                }
            }

            logger.info("Database SELECT query completed successfully. Row count: {}", results.size());
            return results;

        } catch (SQLTimeoutException e) {
            // Specific handling/logging for query timeout events to help triage slow queries.
            logger.error("Database SELECT query timed out. Review query performance or increase database.query_timeout_seconds.", e);
            throw e;
        } catch (SQLException e) {
            // General SQL exception logging; rethrow so higher-level code can decide on retry/rollback.
            logger.error("Database SELECT query failed.", e);
            throw e;
        }
    }

    /**
     * Executes an INSERT, UPDATE, or DELETE SQL statement.
     *
     * <p>
     * This method should be used for database operations that modify data.
     * It returns the number of affected rows so higher-level framework classes
     * can validate expected database behavior.
     * </p>
     *
     * <p>
     * Behavior notes for testers:
     * - If params is null, this method treats it as an empty parameter list.
     * - The method will not commit or rollback the Connection; transaction control
     *   is expected to be handled by the caller or DatabaseHandler.
     * </p>
     *
     * @param connection active JDBC database connection.
     * @param sql        SQL INSERT, UPDATE, or DELETE statement with optional '?' placeholders.
     * @param params     parameter values to bind into the PreparedStatement. May be null.
     * @return number of affected rows.
     * @throws SQLException when SQL execution fails.
     */
    public int executeUpdate(Connection connection, String sql, List<Object> params) throws SQLException {
        // Validate that the connection is usable and the SQL is not blank.
        validateConnection(connection);
        validateSql(sql);

        // Normalize null parameter list to avoid NPE when binding.
        List<Object> safeParams = normalizeParams(params);

        logger.info("Executing database update statement.");
        logger.debug("SQL update: {}", sql);
        logger.debug("SQL parameter count: {}", safeParams.size());

        // Use try-with-resources so PreparedStatement is closed automatically.
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            // Apply configured query timeout and fetch size to the statement.
            configurePreparedStatement(preparedStatement);

            // Bind any provided parameters into the statement before execution.
            bindParameters(preparedStatement, safeParams);

            // Execute the update and capture the number of rows affected.
            int affectedRows = preparedStatement.executeUpdate();

            logger.info("Database update completed successfully. Affected rows: {}", affectedRows);
            return affectedRows;

        } catch (SQLTimeoutException e) {
            // Timeout-specific logging to help determine if config adjustments are needed.
            logger.error("Database update statement timed out. Review query performance or increase database.query_timeout_seconds.", e);
            throw e;
        } catch (SQLException e) {
            // General SQL exception handling: log and rethrow for upstream processing.
            logger.error("Database update statement failed.", e);
            throw e;
        }
    }

    /**
     * Applies framework-level execution settings to PreparedStatement.
     *
     * <p>
     * queryTimeout prevents long-running SQL statements from hanging automation.
     * fetchSize helps control how many rows the JDBC driver fetches in batches.
     * </p>
     *
     * <p>
     * Notes for testers:
     * - Changes to database.query_timeout_seconds or database.fetch_size in config.yml
     *   will affect subsequent statements executed by this performer.
     * </p>
     *
     * @param preparedStatement PreparedStatement to configure.
     * @throws SQLException when statement configuration fails.
     */
    private void configurePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
        // Read configured query timeout (in seconds) or use default when missing/invalid.
        int queryTimeoutSeconds = getIntConfigValue(
                "database.query_timeout_seconds",
                DEFAULT_QUERY_TIMEOUT_SECONDS
        );

        // Read configured fetch size or use default when missing/invalid.
        int fetchSize = getIntConfigValue(
                "database.fetch_size",
                DEFAULT_FETCH_SIZE
        );

        // Apply settings to the PreparedStatement instance. These calls may throw SQLException.
        preparedStatement.setQueryTimeout(queryTimeoutSeconds);
        preparedStatement.setFetchSize(fetchSize);

        logger.debug("PreparedStatement configured with queryTimeoutSeconds={} and fetchSize={}",
                queryTimeoutSeconds,
                fetchSize);
    }

    /**
     * Safely binds parameters into the PreparedStatement.
     *
     * <p>
     * JDBC parameter indexes start from 1. The method accepts a List of Objects
     * so the framework can support String, Integer, Boolean, Date, Timestamp,
     * BigDecimal, and other JDBC-supported types.
     * </p>
     *
     * <p>
     * Important: This method uses PreparedStatement.setObject which allows the
     * JDBC driver to infer the proper SQL type. Tests should ensure parameter
     * ordering matches the SQL placeholders.
     * </p>
     *
     * @param preparedStatement PreparedStatement to bind parameters into.
     * @param params            parameter values.
     * @throws SQLException when parameter binding fails.
     */
    private void bindParameters(PreparedStatement preparedStatement, List<Object> params) throws SQLException {
        // Loop over parameters and bind each to its 1-based index in the PreparedStatement.
        for (int index = 0; index < params.size(); index++) {
            // JDBC parameter indexes are 1-based, so add 1 to the loop index.
            preparedStatement.setObject(index + 1, params.get(index));
        }
    }

    /**
     * Validates that the active JDBC connection is usable before executing SQL.
     *
     * <p>
     * This method does not attempt to re-open or create a connection; it only
     * verifies the provided Connection reference is non-null and open.
     * </p>
     *
     * @param connection JDBC connection.
     * @throws SQLException when connection is null or closed.
     */
    private void validateConnection(Connection connection) throws SQLException {
        if (connection == null) {
            // Fail fast with a clear message so testers can link the error to connection management.
            throw new SQLException("Database connection is null. Verify DatabaseHandler connection creation.");
        }

        if (connection.isClosed()) {
            // Closed connections should be rebounded/renewed by DatabaseHandler; surface a clear message.
            throw new SQLException("Database connection is closed. Verify database connection lifecycle.");
        }
    }

    /**
     * Validates SQL content before execution.
     *
     * @param sql SQL statement.
     * @throws IllegalArgumentException if sql is null or empty.
     */
    private void validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            // Use IllegalArgumentException as SQL content validation is a caller responsibility.
            throw new IllegalArgumentException("SQL statement cannot be null or empty.");
        }
    }

    /**
     * Normalizes null parameter lists to an empty list.
     *
     * <p>
     * This helper avoids repeated null checks in bindParameters and simplifies
     * call sites that may pass null when no parameters are required.
     * </p>
     *
     * @param params original parameter list.
     * @return safe non-null parameter list.
     */
    private List<Object> normalizeParams(List<Object> params) {
        return params == null ? new ArrayList<>() : params;
    }

    /**
     * Reads an integer value from config.yml and applies a default when missing or invalid.
     *
     * <p>
     * The method logs helpful debug or warning messages when configuration keys are absent
     * or contain non-integer values. This aids testers and operators when investigating
     * why defaults were used.
     * </p>
     *
     * @param key          config.yml key.
     * @param defaultValue default value.
     * @return integer configuration value.
     */
    private int getIntConfigValue(String key, int defaultValue) {
        // Read string value from a central configuration helper.
        String value = ConfigurationProperties.getValue(key);

        // If value is not present, log and return the provided default.
        if (value == null || value.trim().isEmpty()) {
            logger.debug("Configuration key '{}' was not found. Using default value: {}", key, defaultValue);
            return defaultValue;
        }

        // Attempt to parse the configured value as an integer; fall back to default on failure.
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Configuration key '{}' has invalid integer value '{}'. Using default value: {}",
                    key,
                    value,
                    defaultValue);
            return defaultValue;
        }
    }
}
