package com.ptaf.db.implementation;

import com.ptaf.db.handlers.DatabaseHandler;
import com.ptaf.db.interfaces.DatabaseAction;
import com.ptaf.db.performer.DatabaseActionPerformer;
import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DatabaseActionImpl provides the concrete implementation of DatabaseAction.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class acts as the orchestration layer for database automation. It does not
 * directly manage SQL Server JDBC details and does not directly execute SQL.
 * Instead, it coordinates the following responsibilities:
 * </p>
 *
 * <ul>
 *     <li>Reads SQL statements from YAML using logical query keys.</li>
 *     <li>Obtains an active database connection from DatabaseHandler.</li>
 *     <li>Delegates SQL execution to DatabaseActionPerformer.</li>
 *     <li>Provides reusable high-level methods for test and step definition layers.</li>
 * </ul>
 *
 * <p>
 * This design keeps DB automation reusable, maintainable, and aligned with the
 * same framework style used for UI, API, PDF, and Performance modules.
 * </p>
 */
public class DatabaseActionImpl implements DatabaseAction {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseActionImpl.class);

    /**
     * Low-level performer responsible for executing prepared SQL statements.
     */
    private final DatabaseActionPerformer dbPerformer;

    /**
     * Creates DatabaseActionImpl with the default SQL performer.
     */
    public DatabaseActionImpl() {
        this.dbPerformer = new DatabaseActionPerformer();
    }

    /**
     * Executes a SELECT query identified by a logical YAML query key.
     *
     * <p>
     * Example query key:
     * users.get_user_by_email
     * </p>
     *
     * <p>
     * Example YAML:
     * users:
     *   get_user_by_email: "SELECT * FROM Users WHERE Email = ?"
     * </p>
     *
     * @param queryKey logical SQL key from YAML.
     * @param params   optional parameters to bind into the SQL PreparedStatement.
     * @return list of database records. Returns an empty list when execution fails or no rows are found.
     */
    @Override
    public List<Map<String, Object>> performQuery(String queryKey, Object... params) {
        String sql = getSqlFromYaml(queryKey);
        List<Object> queryParameters = normalizeParameters(params);

        logger.info("Performing database SELECT query for key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, queryParameters.size());

        try {
            Connection connection = DatabaseHandler.getConnection();
            List<Map<String, Object>> results = dbPerformer.executeQuery(connection, sql, queryParameters);

            logger.info("Database SELECT query completed for key '{}'. Row count: {}", queryKey, results.size());
            return results;

        } catch (SQLException e) {
            /*
             * Returning an empty list preserves existing framework behavior and prevents
             * uncontrolled runtime failures in calling methods. The failure is still logged
             * with full details for troubleshooting.
             */
            logger.error("Failed to execute database SELECT query for key '{}'.", queryKey, e);
            return Collections.emptyList();
        }
    }

    /**
     * Executes an INSERT, UPDATE, or DELETE statement identified by a logical YAML query key.
     *
     * @param queryKey logical SQL key from YAML.
     * @param params   optional parameters to bind into the SQL PreparedStatement.
     * @return affected row count, or -1 when execution fails.
     */
    @Override
    public int performUpdate(String queryKey, Object... params) {
        String sql = getSqlFromYaml(queryKey);
        List<Object> queryParameters = normalizeParameters(params);

        logger.info("Performing database update statement for key: {}", queryKey);
        logger.debug("Update key '{}' parameter count: {}", queryKey, queryParameters.size());

        try {
            Connection connection = DatabaseHandler.getConnection();
            int affectedRows = dbPerformer.executeUpdate(connection, sql, queryParameters);

            logger.info("Database update completed for key '{}'. Affected rows: {}", queryKey, affectedRows);
            return affectedRows;

        } catch (SQLException e) {
            /*
             * Returning -1 preserves the existing contract used by DatabaseCommonMethods
             * when update execution fails.
             */
            logger.error("Failed to execute database update statement for key '{}'.", queryKey, e);
            return -1;
        }
    }

    /**
     * Verifies whether at least one record exists for the given SELECT query.
     *
     * @param queryKey logical SQL key from YAML.
     * @param params   optional SQL parameters.
     * @return true when one or more records are returned; otherwise false.
     */
    @Override
    public boolean recordExists(String queryKey, Object... params) {
        List<Map<String, Object>> results = performQuery(queryKey, params);
        boolean exists = !results.isEmpty();

        logger.info("Database record existence check for key '{}'. Exists: {}", queryKey, exists);
        return exists;
    }

    /**
     * Retrieves a single database record for the given query key.
     *
     * <p>
     * This method is intended for queries that should return either zero or one row.
     * If more than one row is returned, the method throws an exception because the
     * query result is ambiguous.
     * </p>
     *
     * @param queryKey logical SQL key from YAML.
     * @param params   optional SQL parameters.
     * @return single database row, or null when no record is found.
     */
    @Override
    public Map<String, Object> getSingleRecord(String queryKey, Object... params) {
        List<Map<String, Object>> results = performQuery(queryKey, params);

        if (results.isEmpty()) {
            logger.warn("Database query for key '{}' returned no records.", queryKey);
            return null;
        }

        if (results.size() > 1) {
            String message = "Expected a single database record for query key '"
                    + queryKey
                    + "', but found "
                    + results.size()
                    + " records.";

            logger.error(message);
            throw new IllegalStateException(message);
        }

        logger.info("Single database record retrieved successfully for key '{}'.", queryKey);
        return results.get(0);
    }

    /**
     * Retrieves a single value from the first column of the first row returned by a query.
     *
     * <p>
     * This method is useful for validation queries such as:
     * </p>
     *
     * <ul>
     *     <li>SELECT COUNT(*) FROM table_name WHERE condition = ?</li>
     *     <li>SELECT Status FROM table_name WHERE Id = ?</li>
     *     <li>SELECT TOP 1 CreatedDate FROM table_name ORDER BY CreatedDate DESC</li>
     * </ul>
     *
     * @param queryKey logical SQL key from YAML.
     * @param params   optional SQL parameters.
     * @return first column value from the first row, or null when no value is available.
     */
    @Override
    public Object getSingleValue(String queryKey, Object... params) {
        Map<String, Object> record = getSingleRecord(queryKey, params);

        if (record == null || record.isEmpty()) {
            logger.warn("No single value found for database query key '{}'.", queryKey);
            return null;
        }

        if (record.size() > 1) {
            logger.warn(
                    "Database query key '{}' returned multiple columns. Returning the first column value only.",
                    queryKey
            );
        }

        Object value = record.values().iterator().next();

        logger.info("Single database value retrieved successfully for key '{}'.", queryKey);
        return value;
    }

    /**
     * Reads SQL text from YAML using the provided logical query key.
     *
     * <p>
     * This keeps SQL externalized from Java code and allows teams to update queries
     * without changing framework classes.
     * </p>
     *
     * @param queryKey logical SQL key.
     * @return SQL statement from YAML.
     */
    private String getSqlFromYaml(String queryKey) {
        if (queryKey == null || queryKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Database query key cannot be null or empty.");
        }

        Object rawSql = YamlReader.get(queryKey);

        if (rawSql == null) {
            String message = "Database query key '" + queryKey + "' was not found in YAML files.";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        String sql = String.valueOf(rawSql);

        if (sql.trim().isEmpty()) {
            String message = "Database query key '" + queryKey + "' was found, but the SQL value is empty.";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        return sql;
    }

    /**
     * Converts varargs parameters into a safe List for PreparedStatement binding.
     *
     * @param params incoming varargs parameters.
     * @return non-null list of SQL parameters.
     */
    private List<Object> normalizeParameters(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyList();
        }

        return Arrays.asList(params);
    }
}