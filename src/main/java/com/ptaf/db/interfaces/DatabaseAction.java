package com.ptaf.db.interfaces;

import java.util.List;
import java.util.Map;

/**
 * DatabaseAction defines the contract for all high-level database automation actions.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This interface provides a clean abstraction layer between database test-facing
 * classes and the actual database implementation. Step definitions and common
 * methods should depend on this interface rather than directly depending on JDBC
 * or SQL Server implementation details.
 * </p>
 *
 * <p>
 * This design makes the framework easier to maintain, extend, and refactor.
 * For example, the current implementation supports Microsoft SQL Server, but
 * the framework can later add another implementation without changing the
 * higher-level DB test methods.
 * </p>
 */
public interface DatabaseAction {

    /**
     * Executes a SELECT query using a logical SQL query key.
     *
     * <p>
     * The query key should point to a SQL statement stored in the framework YAML files.
     * Parameters are passed as varargs and safely bound through PreparedStatement
     * in the implementation layer.
     * </p>
     *
     * @param queryKey logical SQL query key from YAML.
     * @param params   optional SQL parameters.
     * @return list of database records. Each map represents one row.
     */
    List<Map<String, Object>> performQuery(String queryKey, Object... params);

    /**
     * Executes an INSERT, UPDATE, or DELETE statement using a logical SQL query key.
     *
     * <p>
     * This method is intended for SQL statements that modify data and return
     * an affected row count.
     * </p>
     *
     * @param queryKey logical SQL query key from YAML.
     * @param params   optional SQL parameters.
     * @return number of affected rows, or -1 when execution fails.
     */
    int performUpdate(String queryKey, Object... params);

    /**
     * Checks whether at least one database record exists for the given query key.
     *
     * @param queryKey logical SQL query key from YAML.
     * @param params   optional SQL parameters.
     * @return true when one or more records are found; otherwise false.
     */
    boolean recordExists(String queryKey, Object... params);

    /**
     * Retrieves a single database record for the given query key.
     *
     * <p>
     * This method should be used when the SQL query is expected to return zero
     * or one row. The implementation can throw an exception if more than one row
     * is returned because that means the validation result is ambiguous.
     * </p>
     *
     * @param queryKey logical SQL query key from YAML.
     * @param params   optional SQL parameters.
     * @return single database row, or null when no record is found.
     */
    Map<String, Object> getSingleRecord(String queryKey, Object... params);

    /**
     * Retrieves one value from the first column of the first row returned by a query.
     *
     * <p>
     * This is useful for simple validation queries such as:
     * </p>
     *
     * <ul>
     *     <li>SELECT COUNT(*) FROM table WHERE condition = ?</li>
     *     <li>SELECT Status FROM table WHERE Id = ?</li>
     *     <li>SELECT TOP 1 CreatedDate FROM table ORDER BY CreatedDate DESC</li>
     * </ul>
     *
     * @param queryKey logical SQL query key from YAML.
     * @param params   optional SQL parameters.
     * @return first column value from the first row, or null when no value is available.
     */
    Object getSingleValue(String queryKey, Object... params);
}