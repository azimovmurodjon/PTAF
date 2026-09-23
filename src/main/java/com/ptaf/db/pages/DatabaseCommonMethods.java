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

    // SLF4J logger used to record informational and debug messages for test execution.
    // Tests and CI logs can use these messages to diagnose failures or confirm expected behavior.
    private static final Logger logger = LoggerFactory.getLogger(DatabaseCommonMethods.class);

    /**
     * Provides abstracted database actions for executing queries and updates.
     *
     * <p>
     * This field is intentionally typed to the DatabaseAction interface so that tests
     * remain decoupled from the concrete implementation. The default implementation
     * (DatabaseActionImpl) is provided in the constructor, but the interface allows
     * for easier mocking or substitution in future enhancements.
     * </p>
     */
    private final DatabaseAction databaseAction;

    /**
     * Default constructor that initializes the DatabaseCommonMethods class with the
     * framework's default DatabaseAction implementation.
     *
     * <p>
     * Note for testers:
     * - No DB connection is opened here. Connection handling is performed by the lower-level
     *   DatabaseActionImpl when queries/updates are executed.
     * - If you need to provide a mocked DatabaseAction for unit tests, consider adding
     *   an overloaded constructor in the future. For now, integration tests will use the real implementation.
     * </p>
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
        // Ensure a valid, non-empty query key is provided to prevent accidental calls with invalid input.
        validateQueryKey(queryKey);

        // High-level log to indicate operation intent in the test flow.
        logger.info("Retrieving database records for query key: {}", queryKey);

        // Debug log to show how many parameters were passed; helpful when diagnosing SQL binding issues.
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        // Delegate actual query execution to the DatabaseAction implementation.
        // This returns a list of rows where each row is a map keyed by column name.
        List<Map<String, Object>> records = databaseAction.performQuery(queryKey, params);

        // Informational log that includes the number of returned records to assist in test logs.
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
        // Guard clause to prevent calling into lower layers with an invalid key.
        validateQueryKey(queryKey);

        logger.info("Retrieving single database record for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        // The DatabaseAction implementation is expected to enforce single-row semantics:
        // - Return the single row as a Map when present.
        // - Return null when no row is found.
        // - Throw an exception when more than one row is returned (to avoid ambiguous test assertions).
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
        // Verify the query key is valid before delegating.
        validateQueryKey(queryKey);

        logger.info("Retrieving single database value for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        // Delegates to implementation that returns the first column of the first row,
        // or null when the result set is empty. Useful for scalar queries such as counts or single attributes.
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
        // Ensure the caller supplied a valid query key.
        validateQueryKey(queryKey);

        logger.info("Verifying database record exists for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        // The recordExists call should return true when at least one row matches the predicate.
        boolean exists = databaseAction.recordExists(queryKey, params);

        // Fail the test with a clear message if no matching record is found.
        Assert.assertTrue(
                "Database verification failed: Expected record was not found for query key: " + queryKey,
                exists
        );

        // Confirmation log for successful verification.
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
        // Validate the input to avoid misleading test results caused by a bad query key.
        validateQueryKey(queryKey);

        logger.info("Verifying database record does NOT exist for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        // Reuse the same record existence check; invert the assertion below.
        boolean exists = databaseAction.recordExists(queryKey, params);

        // Fail the test if a record was unexpectedly found.
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
        // Prevent attempting an update with an invalid query key.
        validateQueryKey(queryKey);

        logger.info(
                "Executing database update for query key '{}' and expecting {} affected row(s).",
                queryKey,
                expectedRowsAffected
        );
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        // Execute the update and capture the number of rows changed.
        int actualRowsAffected = databaseAction.performUpdate(queryKey, params);

        // Assert that the returned affected-row count matches the expected value.
        // This helps ensure that the tested operation had the intended effect.
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
        // Validate the query key prior to executing the update.
        validateQueryKey(queryKey);

        logger.info("Executing database update for query key: {}", queryKey);
        logger.debug("Query key '{}' parameter count: {}", queryKey, getParameterCount(params));

        // Delegate update execution and return the number of rows affected so the caller can perform custom checks.
        int affectedRows = databaseAction.performUpdate(queryKey, params);

        logger.info("Database update completed for query key '{}'. Affected row(s): {}", queryKey, affectedRows);
        return affectedRows;
    }

    /**
     * Validates that query key is present before calling lower framework layers.
     *
     * <p>
     * Important for testers:
     * - Passing a null or empty queryKey indicates a test bug (missing input) and will throw an IllegalArgumentException.
     * - This early validation prevents confusing errors from bubbling up from lower-level DB utilities.
     * </p>
     *
     * @param queryKey SQL query key from db_queries.yml.
     */
    private void validateQueryKey(String queryKey) {
        if (queryKey == null || queryKey.trim().isEmpty()) {
            // Throw an unchecked exception to fail fast when test code uses an invalid key.
            throw new IllegalArgumentException("Database query key cannot be null or empty.");
        }
    }

    /**
     * Returns the number of parameters passed into the DB method.
     *
     * <p>
     * This helper is used primarily for improved logging so that test logs can show
     * how many parameters were supplied to a query or update.
     * </p>
     *
     * @param params SQL parameters.
     * @return parameter count.
     */
    private int getParameterCount(Object... params) {
        return params == null ? 0 : params.length;
    }
}
