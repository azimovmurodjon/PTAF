package com.ptaf.db.pages;

import com.ptaf.db.implementation.DatabaseActionImpl;
import com.ptaf.db.interfaces.DatabaseAction;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * DatabaseCommonMethods provides high-level reusable database actions for test automation.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class is the database equivalent of PageCommonMethods / FrameCommonMethods.
 * It provides clean, business-readable methods that can be used by Cucumber step
 * definition classes without exposing low-level JDBC or SQL execution details.
 * </p>
 *
 * <p>
 * Layered DB Automation Flow:
 * </p>
 *
 * <pre>
 * DatabaseSteps
 *      -> DatabaseCommonMethods
 *          -> DatabaseActionImpl
 *              -> DatabaseHandler
 *              -> DatabaseActionPerformer
 *                  -> SQL Server
 * </pre>
 *
 * <p>
 * This class should remain focused on validation and reusable test-facing actions.
 * Connection management, SQL execution, and YAML query resolution are handled by
 * lower framework layers.
 * </p>
 */
public class DatabaseCommonMethods {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCommonMethods.class);

    /**
     * DatabaseAction provides high-level DB operations while hiding implementation details.
     */
    private final DatabaseAction databaseAction;

    /**
     * Creates DatabaseCommonMethods with the default database action implementation.
     */
    public DatabaseCommonMethods() {
        this.databaseAction = new DatabaseActionImpl();
    }

    /**
     * Retrieves multiple records from the database using a logical SQL query key.
     *
     * <p>
     * Example:
     * queryKey = "users.get_user_by_email"
     * </p>
     *
     * @param queryKey SQL query key from db_queries.yml.
     * @param params   optional parameters used for PreparedStatement binding.
     * @return list of database rows. Returns an empty list when no data is found.
     */
    public List<Map<String, Object>> getRecords(String queryKey, Object... params) {
        validateQueryKey(queryKey);

        logger.info("Retrieving database records for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        List<Map<String, Object>> records = databaseAction.performQuery(queryKey, params);

        logger.info("Retrieved {} database record(s) for query key: {}", records.size(), queryKey);
        return records;
    }

    /**
     * Retrieves a single database record using a logical SQL query key.
     *
     * <p>
     * This method should be used when the SQL query is expected to return zero or one row.
     * If more than one row is returned, DatabaseActionImpl will throw an exception to avoid
     * ambiguous test validation.
     * </p>
     *
     * @param queryKey SQL query key from db_queries.yml.
     * @param params   optional parameters used for PreparedStatement binding.
     * @return single database row, or null when no record is found.
     */
    public Map<String, Object> getSingleRecord(String queryKey, Object... params) {
        validateQueryKey(queryKey);

        logger.info("Retrieving single database record for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        return databaseAction.getSingleRecord(queryKey, params);
    }

    /**
     * Retrieves a single database value from the first column of the first row.
     *
     * <p>
     * Common use cases:
     * </p>
     *
     * <ul>
     *     <li>SELECT COUNT(*) FROM table WHERE condition = ?</li>
     *     <li>SELECT Status FROM table WHERE Id = ?</li>
     *     <li>SELECT TOP 1 CreatedDate FROM table ORDER BY CreatedDate DESC</li>
     * </ul>
     *
     * @param queryKey SQL query key from db_queries.yml.
     * @param params   optional parameters used for PreparedStatement binding.
     * @return database value, or null when no value is found.
     */
    public Object getSingleValue(String queryKey, Object... params) {
        validateQueryKey(queryKey);

        logger.info("Retrieving single database value for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        return databaseAction.getSingleValue(queryKey, params);
    }

    /**
     * Verifies that at least one record exists for the given query key and parameters.
     *
     * <p>
     * This method fails the test when no matching record is found.
     * </p>
     *
     * @param queryKey SQL query key from db_queries.yml.
     * @param params   optional parameters used for PreparedStatement binding.
     */
    public void verifyRecordExists(String queryKey, Object... params) {
        validateQueryKey(queryKey);

        logger.info("Verifying database record exists for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        boolean exists = databaseAction.recordExists(queryKey, params);

        Assert.assertTrue(
                "Database verification failed: Expected record was not found for query key: " + queryKey,
                exists
        );

        logger.info("Database verification passed. Record exists for query key: {}", queryKey);
    }

    /**
     * Verifies that no records exist for the given query key and parameters.
     *
     * <p>
     * This method fails the test when a matching record is found.
     * </p>
     *
     * @param queryKey SQL query key from db_queries.yml.
     * @param params   optional parameters used for PreparedStatement binding.
     */
    public void verifyRecordDoesNotExist(String queryKey, Object... params) {
        validateQueryKey(queryKey);

        logger.info("Verifying database record does NOT exist for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        boolean exists = databaseAction.recordExists(queryKey, params);

        Assert.assertFalse(
                "Database verification failed: Record was found but was not expected for query key: " + queryKey,
                exists
        );

        logger.info("Database verification passed. No record exists for query key: {}", queryKey);
    }

    /**
     * Executes an INSERT, UPDATE, or DELETE statement and validates the affected row count.
     *
     * <p>
     * This method is useful when the test needs to confirm that a database operation
     * changed exactly the expected number of rows.
     * </p>
     *
     * @param expectedRowsAffected expected affected row count.
     * @param queryKey             SQL query key from db_queries.yml.
     * @param params               optional parameters used for PreparedStatement binding.
     */
    public void verifyRowsAffected(int expectedRowsAffected, String queryKey, Object... params) {
        validateQueryKey(queryKey);

        logger.info(
                "Executing database update for query key '{}' and expecting {} affected row(s).",
                queryKey,
                expectedRowsAffected
        );
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        int actualRowsAffected = databaseAction.performUpdate(queryKey, params);

        Assert.assertEquals(
                "Database verification failed: Unexpected number of affected rows for query key: " + queryKey,
                expectedRowsAffected,
                actualRowsAffected
        );

        logger.info(
                "Database update verification passed for query key '{}'. Actual affected row(s): {}",
                queryKey,
                actualRowsAffected
        );
    }

    /**
     * Executes an INSERT, UPDATE, or DELETE statement and returns the affected row count.
     *
     * <p>
     * This method gives step definitions more flexibility when the exact row count
     * needs to be validated later or used in custom business logic.
     * </p>
     *
     * @param queryKey SQL query key from db_queries.yml.
     * @param params   optional parameters used for PreparedStatement binding.
     * @return affected row count, or -1 when execution fails.
     */
    public int executeUpdate(String queryKey, Object... params) {
        validateQueryKey(queryKey);

        logger.info("Executing database update for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        int affectedRows = databaseAction.performUpdate(queryKey, params);

        logger.info("Database update completed for query key '{}'. Affected row(s): {}", queryKey, affectedRows);
        return affectedRows;
    }

    /**
     * Validates that query key is present before calling lower framework layers.
     *
     * @param queryKey SQL query key from db_queries.yml.
     */
    private void validateQueryKey(String queryKey) {
        if (queryKey == null || queryKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Database query key cannot be null or empty.");
        }
    }

    /**
     * Returns the number of parameters passed into the DB method.
     *
     * @param params SQL parameters.
     * @return parameter count.
     */
    private int getParameterCount(Object... params) {
        return params == null ? 0 : params.length;
    }
}