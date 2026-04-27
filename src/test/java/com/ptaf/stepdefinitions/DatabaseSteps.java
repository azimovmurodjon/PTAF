package com.ptaf.stepdefinitions;

import com.ptaf.db.pages.DatabaseCommonMethods;
import com.ptaf.db.validators.DatabaseConnectionValidator;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

/**
 * DatabaseSteps contains Cucumber step definitions for database validation and data setup.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class provides business-readable Gherkin steps for database automation.
 * It does not manage JDBC connections, build SQL Server connection URLs, or execute
 * SQL directly. Those responsibilities are delegated to the database framework layers:
 * </p>
 *
 * <pre>
 * DatabaseSteps
 *      -> DatabaseCommonMethods
 *          -> DatabaseActionImpl
 *              -> DatabaseHandler
 *              -> DatabaseActionPerformer
 *                  -> Microsoft SQL Server
 * </pre>
 *
 * <p>
 * SQL statements should remain externalized in db_queries.yml and referenced by
 * logical query keys from feature files. This keeps feature files readable and avoids
 * hardcoding SQL inside step definitions.
 * </p>
 */
public class DatabaseSteps {

    /**
     * High-level reusable database methods used by the step definitions.
     */
    private final DatabaseCommonMethods dbMethods;

    /**
     * Creates DatabaseSteps with the default DB common methods implementation.
     */
    public DatabaseSteps() {
        this.dbMethods = new DatabaseCommonMethods();
    }

    /**
     * Validates that the framework can successfully connect to the configured SQL Server database.
     *
     * <p>
     * Example:
     * Given I validate the database connection is successful
     * </p>
     *
     * <p>
     * This step is useful as a smoke/health-check step before executing deeper DB validations.
     * It uses the same DatabaseHandler connection path as the real DB tests, so it validates
     * the real framework connection configuration from config.yml.
     * </p>
     */
    @Given("I validate the database connection is successful")
    public void i_validate_the_database_connection_is_successful() {
        DatabaseConnectionValidator.assertDatabaseConnectionSuccessful();
    }

    /**
     * Verifies that the database does not contain a matching record before test execution.
     *
     * <p>
     * Example:
     * Given the database does not contain a record for query "users.get_user_by_email" with parameters "test@test.com"
     * </p>
     *
     * @param queryKey logical SQL query key from db_queries.yml.
     * @param params   comma-separated SQL parameters from the feature file.
     */
    @Given("the database does not contain a record for query {string} with parameters {string}")
    public void the_database_does_not_contain_a_record_for_query(String queryKey, String params) {
        dbMethods.verifyRecordDoesNotExist(queryKey, parseParameters(params));
    }

    /**
     * Verifies that the database contains at least one matching record.
     *
     * <p>
     * Example:
     * Then I verify the database contains a record for query "users.get_user_by_email" with parameters "test@test.com"
     * </p>
     *
     * @param queryKey logical SQL query key from db_queries.yml.
     * @param params   comma-separated SQL parameters from the feature file.
     */
    @Then("I verify the database contains a record for query {string} with parameters {string}")
    public void i_verify_the_database_contains_a_record_for_query_with_parameters(String queryKey, String params) {
        dbMethods.verifyRecordExists(queryKey, parseParameters(params));
    }

    /**
     * Verifies that the database does not contain any matching record.
     *
     * @param queryKey logical SQL query key from db_queries.yml.
     * @param params   comma-separated SQL parameters from the feature file.
     */
    @Then("I verify the database does not contain a record for query {string} with parameters {string}")
    public void i_verify_the_database_does_not_contain_a_record_for_query_with_parameters(String queryKey, String params) {
        dbMethods.verifyRecordDoesNotExist(queryKey, parseParameters(params));
    }

    /**
     * Executes an INSERT statement and verifies that one row was inserted.
     *
     * @param queryKey logical SQL query key from db_queries.yml.
     * @param params   comma-separated SQL parameters from the feature file.
     */
    @When("I insert a new record using query {string} with parameters {string}")
    public void i_insert_a_new_record_using_query_with_parameters(String queryKey, String params) {
        dbMethods.verifyRowsAffected(1, queryKey, parseParameters(params));
    }

    /**
     * Executes an UPDATE statement and verifies that one row was updated.
     *
     * @param queryKey logical SQL query key from db_queries.yml.
     * @param params   comma-separated SQL parameters from the feature file.
     */
    @When("I update a record using query {string} with parameters {string}")
    public void i_update_a_record_using_query_with_parameters(String queryKey, String params) {
        dbMethods.verifyRowsAffected(1, queryKey, parseParameters(params));
    }

    /**
     * Executes a DELETE statement and verifies the expected number of affected rows.
     *
     * @param expectedRows expected deleted row count.
     * @param queryKey     logical SQL query key from db_queries.yml.
     * @param params       comma-separated SQL parameters from the feature file.
     */
    @When("I delete {int} record\\(s) using query {string} with parameters {string}")
    public void i_delete_records_using_query_with_parameters(int expectedRows, String queryKey, String params) {
        dbMethods.verifyRowsAffected(expectedRows, queryKey, parseParameters(params));
    }

    /**
     * Verifies that a single database value matches the expected value.
     *
     * <p>
     * This step is useful for SQL Server validation queries such as:
     * SELECT COUNT(*) FROM TableName WHERE Id = ?
     * SELECT Status FROM TableName WHERE ClientId = ?
     * </p>
     *
     * <p>
     * Example:
     * Then I verify single database value for query "clients.get_status_by_id" with parameters "1001" equals "ACTIVE"
     * </p>
     *
     * @param queryKey      logical SQL query key from db_queries.yml.
     * @param params        comma-separated SQL parameters from the feature file.
     * @param expectedValue expected database value.
     */
    @Then("I verify single database value for query {string} with parameters {string} equals {string}")
    public void i_verify_single_database_value_for_query_with_parameters_equals(
            String queryKey,
            String params,
            String expectedValue
    ) {
        Object actualValue = dbMethods.getSingleValue(queryKey, parseParameters(params));

        org.junit.Assert.assertEquals(
                "Database value verification failed for query key: " + queryKey,
                normalizeExpectedValue(expectedValue),
                actualValue == null ? null : String.valueOf(actualValue)
        );
    }

    /**
     * Executes an update statement and verifies the expected affected row count.
     *
     * <p>
     * This step gives more flexibility than insert/update/delete specific steps because
     * the expected row count can be controlled directly from the feature file.
     * </p>
     *
     * <p>
     * Example:
     * When I execute database update query "users.update_status" with parameters "ACTIVE, 1001" then 1 row(s) should be affected
     * </p>
     *
     * @param queryKey      logical SQL query key from db_queries.yml.
     * @param params        comma-separated SQL parameters from the feature file.
     * @param expectedRows  expected affected row count.
     */
    @When("I execute database update query {string} with parameters {string} then {int} row\\(s) should be affected")
    public void i_execute_database_update_query_with_parameters_then_rows_should_be_affected(
            String queryKey,
            String params,
            int expectedRows
    ) {
        dbMethods.verifyRowsAffected(expectedRows, queryKey, parseParameters(params));
    }

    /**
     * Verifies returned column values from the first database record using a Cucumber DataTable.
     *
     * <p>
     * Example:
     * Then I verify database record for query "clients.get_client_by_id" with parameters "1001" contains:
     *   | Status | ACTIVE |
     *   | Type   | NEW    |
     * </p>
     *
     * @param queryKey  logical SQL query key from db_queries.yml.
     * @param params    comma-separated SQL parameters from the feature file.
     * @param dataTable expected column/value pairs.
     */
    @Then("I verify database record for query {string} with parameters {string} contains:")
    public void i_verify_database_record_for_query_with_parameters_contains(
            String queryKey,
            String params,
            DataTable dataTable
    ) {
        Map<String, Object> record = dbMethods.getSingleRecord(queryKey, parseParameters(params));

        org.junit.Assert.assertNotNull(
                "Database verification failed: No record found for query key: " + queryKey,
                record
        );

        Map<String, String> expectedColumnValues = dataTable.asMap(String.class, String.class);

        for (Map.Entry<String, String> expectedEntry : expectedColumnValues.entrySet()) {
            String columnName = expectedEntry.getKey();
            String expectedValue = normalizeExpectedValue(expectedEntry.getValue());

            org.junit.Assert.assertTrue(
                    "Database verification failed: Column '" + columnName + "' was not found for query key: " + queryKey,
                    record.containsKey(columnName)
            );

            Object actualValue = record.get(columnName);

            org.junit.Assert.assertEquals(
                    "Database verification failed for column '" + columnName + "' and query key: " + queryKey,
                    expectedValue,
                    actualValue == null ? null : String.valueOf(actualValue)
            );
        }
    }

    /**
     * Parses comma-separated feature file parameters into strongly typed Java objects.
     *
     * <p>
     * Supported examples:
     * </p>
     *
     * <ul>
     *     <li>"1001" -> Integer</li>
     *     <li>"29.99" -> BigDecimal</li>
     *     <li>"true" -> Boolean</li>
     *     <li>"false" -> Boolean</li>
     *     <li>"null" -> null</li>
     *     <li>"ACTIVE" -> String</li>
     * </ul>
     *
     * <p>
     * Keeping parameter parsing here allows feature files to stay clean while still
     * supporting SQL Server PreparedStatement binding with common data types.
     * </p>
     *
     * @param paramString comma-separated parameter string from the feature file.
     * @return array of typed SQL parameter objects.
     */
    private Object[] parseParameters(String paramString) {
        if (paramString == null || paramString.trim().isEmpty()) {
            return new Object[0];
        }

        return Arrays.stream(paramString.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::convertParameter)
                .toArray();
    }

    /**
     * Converts a single parameter value into the most appropriate Java type.
     *
     * @param value raw parameter value from the feature file.
     * @return converted parameter object.
     */
    private Object convertParameter(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        if (trimmedValue.equalsIgnoreCase("null")) {
            return null;
        }

        if (trimmedValue.equalsIgnoreCase("true") || trimmedValue.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(trimmedValue);
        }

        if (trimmedValue.matches("^-?\\d+$")) {
            try {
                return Integer.parseInt(trimmedValue);
            } catch (NumberFormatException ignored) {
                return Long.parseLong(trimmedValue);
            }
        }

        if (trimmedValue.matches("^-?\\d+\\.\\d+$")) {
            return new BigDecimal(trimmedValue);
        }

        return removeOptionalQuotes(trimmedValue);
    }

    /**
     * Removes optional wrapping single or double quotes from a parameter value.
     *
     * <p>
     * This allows feature files to pass values like:
     * "ACTIVE"
     * 'ACTIVE'
     * </p>
     *
     * @param value raw parameter value.
     * @return value without wrapping quotes.
     */
    private String removeOptionalQuotes(String value) {
        if (value.length() >= 2) {
            boolean hasDoubleQuotes = value.startsWith("\"") && value.endsWith("\"");
            boolean hasSingleQuotes = value.startsWith("'") && value.endsWith("'");

            if (hasDoubleQuotes || hasSingleQuotes) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }

    /**
     * Normalizes expected values used in assertions.
     *
     * @param expectedValue expected value from the feature file.
     * @return normalized expected value.
     */
    private String normalizeExpectedValue(String expectedValue) {
        if (expectedValue == null) {
            return null;
        }

        String value = expectedValue.trim();

        if (value.equalsIgnoreCase("null")) {
            return null;
        }

        return removeOptionalQuotes(value);
    }
}