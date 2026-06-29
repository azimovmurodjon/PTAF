package com.ptaf.db.interfaces;

import java.util.List;
import java.util.Map;

/**
 * DatabaseAction defines the contract for all high-level database automation actions.
 *
 * <p>
 * Purpose:
 * This interface is the public API used by higher-level test code (step definitions,
 * validation utilities, and helper libraries) to interact with the database in a
 * consistent, implementation-agnostic way.
 * </p>
 *
 * <p>
 * Key responsibilities for implementations:
 * <ul>
 *   <li>Load SQL statements using a logical query key (for example, keys defined in YAML).</li>
 *   <li>Use PreparedStatement parameter binding for safety and correctness.</li>
 *   <li>Manage JDBC resources (connections, statements, result sets) and ensure proper cleanup.</li>
 *   <li>Translate SQL / JDBC exceptions into runtime exceptions or domain-specific exceptions,
 *       depending on framework conventions.</li>
 *   <li>Provide well-documented semantics for return values so testers know how to write assertions.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Notes for testers and integrators:
 * <ul>
 *   <li>Do not hardcode SQL in tests; reference the YAML logical query keys instead.</li>
 *   <li>Prefer type-safe checks: use getSingleValue for single-column checks, getSingleRecord for single-row checks,
 *       and performQuery for multi-row validation.</li>
 *   <li>Because implementations manage resources and conversions, tests should not attempt to access
 *       JDBC types directly from the returned maps without converting types appropriately.</li>
 * </ul>
 * </p>
 *
 * <p>
 * This design makes the framework easier to maintain, extend, and refactor.
 * For example, the current implementation supports Microsoft SQL Server, but
 * the framework can later add another implementation without changing the
 * higher-level DB test methods.
 * </p>
 *
 * @since 1.0
 */
public interface DatabaseAction {

    /**
     * Executes a SELECT-like query using a logical SQL query key and optional parameters.
     *
     * <p>
     * The implementation is expected to:
     * <ol>
     *   <li>Resolve the provided queryKey to an actual SQL statement (for example, reading from YAML).</li>
     *   <li>Create a PreparedStatement and bind the provided params in order.</li>
     *   <li>Execute the statement and map each row to a Map&lt;String, Object&gt; where the key is the column label
     *       (generally the column name) and the value is the column value converted to a Java object.</li>
     *   <li>Return an empty list when the query returns no rows.</li>
     * </ol>
     * </p>
     *
     * <p>
     * Testers' guidance:
     * <ul>
     *   <li>Use this method when you expect multiple rows or zero-to-many rows.</li>
     *   <li>Returned maps should be treated as read-only snapshots of the row data.</li>
     *   <li>Column name casing depends on the implementation; consider using case-insensitive lookups or
     *       normalizing keys in tests.</li>
     * </ul>
     * </p>
     *
     * @param queryKey logical SQL query key from YAML. This key should map to a SELECT statement
     *                 or any query that returns rows.
     * @param params   optional SQL parameters. These are bound positionally to PreparedStatement placeholders.
     *                 Use the correct ordering that matches the SQL defined for queryKey.
     * @return list of database records. Each map represents one row with column label to value mapping.
     *         Never returns null; implementations should return an empty list for zero rows.
     * @throws RuntimeException implementations may throw runtime exceptions when underlying SQL fails,
     *         resources cannot be acquired, or mapping fails. Callers (tests) should handle or allow such
     *         exceptions to fail the test.
     */
    List<Map<String, Object>> performQuery(String queryKey, Object... params);

    /**
     * Executes an INSERT, UPDATE, or DELETE statement using a logical SQL query key.
     *
     * <p>
     * The implementation is expected to:
     * <ul>
     *   <li>Resolve the queryKey to an update SQL (INSERT/UPDATE/DELETE).</li>
     *   <li>Bind parameters safely through PreparedStatement.</li>
     *   <li>Execute the statement and return the number of affected rows.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Return semantics:
     * <ul>
     *   <li>Return a non-negative integer representing the number of rows affected on success.</li>
     *   <li>Return -1 when execution fails and the implementation chooses to signal failure via the return value.
     *       (Note: some implementations may instead throw a runtime exception; consult your implementation's docs.)</li>
     * </ul>
     * </p>
     *
     * <p>
     * Testers' guidance:
     * <ul>
     *   <li>Use performUpdate for mutating operations and assert the returned affected row count when needed.</li>
     *   <li>If the implementation throws exceptions, let them surface as test failures so the failure cause is visible.</li>
     * </ul>
     * </p>
     *
     * @param queryKey logical SQL query key from YAML. Should map to an update SQL statement.
     * @param params   optional SQL parameters to be bound positionally.
     * @return number of affected rows, or -1 when execution fails (implementation dependent).
     * @throws RuntimeException implementations may throw runtime exceptions on severe errors.
     */
    int performUpdate(String queryKey, Object... params);

    /**
     * Checks whether at least one database record exists for the given query key.
     *
     * <p>
     * This is a convenience helper that can be implemented either by executing the
     * resolved query and checking for any returned rows, or by executing an optimized
     * EXISTS/COUNT query variant provided in YAML.
     * </p>
     *
     * <p>
     * Testers' guidance:
     * <ul>
     *   <li>Use recordExists when you only need a boolean existence check rather than retrieving data.</li>
     *   <li>Prefer this method to reduce test code and to express intent clearly in step definitions.</li>
     * </ul>
     * </p>
     *
     * @param queryKey logical SQL query key from YAML. The corresponding SQL should be suitable for an existence check.
     * @param params   optional SQL parameters to be bound positionally.
     * @return true when one or more records are found; otherwise false. Never returns null.
     * @throws RuntimeException implementations may throw runtime exceptions on SQL or resource errors.
     */
    boolean recordExists(String queryKey, Object... params);

    /**
     * Retrieves a single database record for the given query key.
     *
     * <p>
     * This method is intended for queries that are expected to return at most one row.
     * Implementations may:
     * <ul>
     *   <li>Return the single row as a Map&lt;String, Object&gt; when exactly one row is found.</li>
     *   <li>Return null when no row is found.</li>
     *   <li>Throw an exception or enforce a strict behavior if more than one row is returned,
     *       since that usually indicates an unexpected data state for the test.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Testers' guidance:
     * <ul>
     *   <li>Use this method for unique record validations (for example, retrieving a row by primary key).</li>
     *   <li>Assert against specific column values retrieved from the returned map.</li>
     * </ul>
     * </p>
     *
     * @param queryKey logical SQL query key from YAML. The SQL should be designed to return zero or one row.
     * @param params   optional SQL parameters to be bound positionally.
     * @return single database row as a map of column label to value, or null when no record is found.
     * @throws RuntimeException implementations may throw runtime exceptions when more than one row is returned,
     *         or when underlying SQL / mapping errors occur.
     */
    Map<String, Object> getSingleRecord(String queryKey, Object... params);

    /**
     * Retrieves one value from the first column of the first row returned by a query.
     *
     * <p>
     * This is a common helper for simple validations where only a single value is needed.
     * Typical uses include:
     * <ul>
     *   <li>SELECT COUNT(*) queries</li>
     *   <li>SELECT Status FROM table WHERE Id = ?</li>
     *   <li>SELECT TOP 1 CreatedDate FROM table ORDER BY CreatedDate DESC</li>
     * </ul>
     * </p>
     *
     * <p>
     * Semantics:
     * <ul>
     *   <li>Return the value of the first column from the first row if present.</li>
     *   <li>Return null if no rows are returned or the value itself is SQL NULL.</li>
     *   <li>Type conversion is implementation-dependent; callers should cast the result to the expected type.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Testers' guidance:
     * <ul>
     *   <li>Use getSingleValue for scalar assertions such as counts or status codes.</li>
     *   <li>Be explicit in tests about the expected type (for example, Long for COUNT(*), String for status).</li>
     * </ul>
     * </p>
     *
     * @param queryKey logical SQL query key from YAML. The SQL should return at most one row or the caller will only receive
     *                 the first row's first column value.
     * @param params   optional SQL parameters to be bound positionally.
     * @return first column value from the first row, or null when no value is available.
     * @throws RuntimeException implementations may throw runtime exceptions on SQL or mapping errors.
     */
    Object getSingleValue(String queryKey, Object... params);
}
