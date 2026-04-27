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
     * @param connection active JDBC database connection.
     * @param sql        SQL SELECT statement with optional '?' placeholders.
     * @param params     parameter values to bind into the PreparedStatement.
     * @return list of result rows. Returns an empty list when no data is found.
     * @throws SQLException when SQL execution fails.
     */
    public List<Map<String, Object>> executeQuery(Connection connection, String sql, List<Object> params) throws SQLException {
        validateConnection(connection);
        validateSql(sql);

        List<Object> safeParams = normalizeParams(params);
        List<Map<String, Object>> results = new ArrayList<>();

        logger.info("Executing database SELECT query.");
        logger.debug("SQL query: {}", sql);
        logger.debug("SQL parameter count: {}", safeParams.size());

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            configurePreparedStatement(preparedStatement);
            bindParameters(preparedStatement, safeParams);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                        String columnLabel = metaData.getColumnLabel(columnIndex);
                        Object columnValue = resultSet.getObject(columnIndex);

                        row.put(columnLabel, columnValue);
                    }

                    results.add(row);
                }
            }

            logger.info("Database SELECT query completed successfully. Row count: {}", results.size());
            return results;

        } catch (SQLTimeoutException e) {
            logger.error("Database SELECT query timed out. Review query performance or increase database.query_timeout_seconds.", e);
            throw e;
        } catch (SQLException e) {
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
     * @param connection active JDBC database connection.
     * @param sql        SQL INSERT, UPDATE, or DELETE statement with optional '?' placeholders.
     * @param params     parameter values to bind into the PreparedStatement.
     * @return number of affected rows.
     * @throws SQLException when SQL execution fails.
     */
    public int executeUpdate(Connection connection, String sql, List<Object> params) throws SQLException {
        validateConnection(connection);
        validateSql(sql);

        List<Object> safeParams = normalizeParams(params);

        logger.info("Executing database update statement.");
        logger.debug("SQL update: {}", sql);
        logger.debug("SQL parameter count: {}", safeParams.size());

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            configurePreparedStatement(preparedStatement);
            bindParameters(preparedStatement, safeParams);

            int affectedRows = preparedStatement.executeUpdate();

            logger.info("Database update completed successfully. Affected rows: {}", affectedRows);
            return affectedRows;

        } catch (SQLTimeoutException e) {
            logger.error("Database update statement timed out. Review query performance or increase database.query_timeout_seconds.", e);
            throw e;
        } catch (SQLException e) {
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
     * @param preparedStatement PreparedStatement to configure.
     * @throws SQLException when statement configuration fails.
     */
    private void configurePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
        int queryTimeoutSeconds = getIntConfigValue(
                "database.query_timeout_seconds",
                DEFAULT_QUERY_TIMEOUT_SECONDS
        );

        int fetchSize = getIntConfigValue(
                "database.fetch_size",
                DEFAULT_FETCH_SIZE
        );

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
     * @param preparedStatement PreparedStatement to bind parameters into.
     * @param params            parameter values.
     * @throws SQLException when parameter binding fails.
     */
    private void bindParameters(PreparedStatement preparedStatement, List<Object> params) throws SQLException {
        for (int index = 0; index < params.size(); index++) {
            preparedStatement.setObject(index + 1, params.get(index));
        }
    }

    /**
     * Validates that the active JDBC connection is usable before executing SQL.
     *
     * @param connection JDBC connection.
     * @throws SQLException when connection is null or closed.
     */
    private void validateConnection(Connection connection) throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is null. Verify DatabaseHandler connection creation.");
        }

        if (connection.isClosed()) {
            throw new SQLException("Database connection is closed. Verify database connection lifecycle.");
        }
    }

    /**
     * Validates SQL content before execution.
     *
     * @param sql SQL statement.
     */
    private void validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty.");
        }
    }

    /**
     * Normalizes null parameter lists to an empty list.
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
     * @param key          config.yml key.
     * @param defaultValue default value.
     * @return integer configuration value.
     */
    private int getIntConfigValue(String key, int defaultValue) {
        String value = ConfigurationProperties.getValue(key);

        if (value == null || value.trim().isEmpty()) {
            logger.debug("Configuration key '{}' was not found. Using default value: {}", key, defaultValue);
            return defaultValue;
        }

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