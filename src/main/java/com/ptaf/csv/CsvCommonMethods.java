package com.ptaf.csv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * High-level CSV assertion and extraction methods for use by Cucumber step definitions.
 *
 * <p>This class sits between the raw {@link CsvFileHandler} (which handles parsing and querying)
 * and the {@link com.ptaf.stepdefinitions.CsvSteps} class (which maps Gherkin sentences to Java
 * methods). It provides:</p>
 * <ul>
 *   <li>Loading CSV from a file path or a raw string (e.g., extracted from a UI element).</li>
 *   <li>Asserting that a cell value equals or contains an expected value.</li>
 *   <li>Asserting row count and column existence.</li>
 *   <li>Asserting that all rows satisfy a condition on a given column.</li>
 *   <li>Extracting cell values into a named variable store for use in later steps.</li>
 *   <li>Clearing the CSV context after a scenario.</li>
 * </ul>
 *
 * <p>All assertion failures throw an {@link AssertionError} with a clear, descriptive message
 * that includes the row, column, expected value, and actual value found.</p>
 */
public class CsvCommonMethods {

    private static final Logger logger = LoggerFactory.getLogger(CsvCommonMethods.class);

    /**
     * In-scenario variable store for extracted CSV values.
     * Key: variable name (e.g., "TXN_ID"). Value: extracted string value.
     */
    private final Map<String, String> variableStore = new HashMap<>();

    // ─── Loading ─────────────────────────────────────────────────────────────────

    /**
     * Load and parse a CSV file from the filesystem into the current scenario's CSV context.
     *
     * @param filePath path to the CSV file (e.g., "src/test/resources/data/transactions.csv")
     */
    public void loadFromFile(String filePath) {
        CsvFileHandler handler = new CsvFileHandler();
        handler.loadFromFile(filePath);
        CsvContext.set(handler);
        logger.info("PTAF CSV | Loaded CSV file into context: {}", filePath);
    }

    /**
     * Load and parse a CSV file using a custom delimiter.
     *
     * @param filePath  path to the CSV file
     * @param delimiter the delimiter character (e.g., ';' for semicolon-separated files)
     */
    public void loadFromFile(String filePath, char delimiter) {
        CsvFileHandler handler = new CsvFileHandler();
        handler.setDelimiter(delimiter);
        handler.loadFromFile(filePath);
        CsvContext.set(handler);
        logger.info("PTAF CSV | Loaded CSV file with delimiter [{}] into context: {}", delimiter, filePath);
    }

    /**
     * Load and parse CSV content from a raw string into the current scenario's CSV context.
     *
     * @param csvContent raw CSV string to parse
     */
    public void loadFromString(String csvContent) {
        CsvFileHandler handler = new CsvFileHandler();
        handler.loadFromString(csvContent);
        CsvContext.set(handler);
        logger.info("PTAF CSV | Loaded CSV from string content into context.");
    }

    // ─── Assertions — Cell Value ──────────────────────────────────────────────────

    /**
     * Assert that the value of a specific cell (identified by row number and column name) equals
     * the expected value exactly.
     *
     * @param rowNumber  1-based row number (1 = first data row after header)
     * @param columnName the column header name (case-sensitive)
     * @param expected   the exact expected value
     * @throws AssertionError if the actual value does not equal the expected value
     */
    public void assertValueEquals(int rowNumber, String columnName, String expected) {
        String actual = handler().getValue(rowNumber, columnName);
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — cell value does not match.\n" +
                "  Row      : " + rowNumber + "\n" +
                "  Column   : " + columnName + "\n" +
                "  Expected : [" + expected + "]\n" +
                "  Actual   : [" + actual + "]"
            );
        }
        logger.info("PTAF CSV | assertValueEquals PASSED — row={} col={} value=[{}]", rowNumber, columnName, actual);
    }

    /**
     * Assert that the value of a specific cell (identified by row number and column index) equals
     * the expected value exactly.
     *
     * @param rowNumber   1-based row number
     * @param columnIndex 1-based column index
     * @param expected    the exact expected value
     * @throws AssertionError if the actual value does not equal the expected value
     */
    public void assertValueEqualsByIndex(int rowNumber, int columnIndex, String expected) {
        String actual = handler().getValueByIndex(rowNumber, columnIndex);
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — cell value does not match.\n" +
                "  Row          : " + rowNumber + "\n" +
                "  Column Index : " + columnIndex + "\n" +
                "  Expected     : [" + expected + "]\n" +
                "  Actual       : [" + actual + "]"
            );
        }
        logger.info("PTAF CSV | assertValueEqualsByIndex PASSED — row={} colIdx={} value=[{}]", rowNumber, columnIndex, actual);
    }

    /**
     * Assert that the value of a specific cell contains the expected substring.
     *
     * @param rowNumber  1-based row number
     * @param columnName the column header name
     * @param expected   the substring that must be present in the actual value
     * @throws AssertionError if the actual value does not contain the expected substring
     */
    public void assertValueContains(int rowNumber, String columnName, String expected) {
        String actual = handler().getValue(rowNumber, columnName);
        if (!actual.contains(expected)) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — cell value does not contain expected substring.\n" +
                "  Row                 : " + rowNumber + "\n" +
                "  Column              : " + columnName + "\n" +
                "  Expected to contain : [" + expected + "]\n" +
                "  Actual              : [" + actual + "]"
            );
        }
        logger.info("PTAF CSV | assertValueContains PASSED — row={} col={} contains=[{}]", rowNumber, columnName, expected);
    }

    /**
     * Assert that the value of a specific cell does NOT equal the given value.
     *
     * @param rowNumber  1-based row number
     * @param columnName the column header name
     * @param expected   the value that must NOT be present
     * @throws AssertionError if the actual value equals the expected value
     */
    public void assertValueNotEquals(int rowNumber, String columnName, String expected) {
        String actual = handler().getValue(rowNumber, columnName);
        if (expected.equals(actual)) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — cell value should not equal [" + expected + "] but it does.\n" +
                "  Row    : " + rowNumber + "\n" +
                "  Column : " + columnName
            );
        }
        logger.info("PTAF CSV | assertValueNotEquals PASSED — row={} col={} value=[{}]", rowNumber, columnName, actual);
    }

    // ─── Assertions — Structure ───────────────────────────────────────────────────

    /**
     * Assert that the CSV contains exactly the expected number of data rows (excluding header).
     *
     * @param expectedCount the exact expected row count
     * @throws AssertionError if the actual row count does not match
     */
    public void assertRowCount(int expectedCount) {
        int actual = handler().getRowCount();
        if (actual != expectedCount) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — row count does not match.\n" +
                "  Expected : " + expectedCount + "\n" +
                "  Actual   : " + actual
            );
        }
        logger.info("PTAF CSV | assertRowCount PASSED — count=[{}]", actual);
    }

    /**
     * Assert that the CSV contains at least the expected number of data rows.
     *
     * @param minimumCount the minimum expected row count
     * @throws AssertionError if the actual row count is less than the minimum
     */
    public void assertRowCountAtLeast(int minimumCount) {
        int actual = handler().getRowCount();
        if (actual < minimumCount) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — row count is less than expected minimum.\n" +
                "  Minimum expected : " + minimumCount + "\n" +
                "  Actual           : " + actual
            );
        }
        logger.info("PTAF CSV | assertRowCountAtLeast PASSED — actual=[{}] >= minimum=[{}]", actual, minimumCount);
    }

    /**
     * Assert that a column with the given name exists in the CSV headers.
     *
     * @param columnName the column header name to check (case-sensitive)
     * @throws AssertionError if the column does not exist
     */
    public void assertColumnExists(String columnName) {
        if (!handler().columnExists(columnName)) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — column [" + columnName + "] does not exist.\n" +
                "  Available columns : " + handler().getHeaders() + "\n" +
                "  Column names are case-sensitive."
            );
        }
        logger.info("PTAF CSV | assertColumnExists PASSED — column=[{}]", columnName);
    }

    /**
     * Assert that a column with the given name does NOT exist in the CSV headers.
     *
     * @param columnName the column header name to check
     * @throws AssertionError if the column exists
     */
    public void assertColumnNotExists(String columnName) {
        if (handler().columnExists(columnName)) {
            throw new AssertionError(
                "PTAF CSV | Assertion failed — column [" + columnName + "] should not exist but it does."
            );
        }
        logger.info("PTAF CSV | assertColumnNotExists PASSED — column=[{}]", columnName);
    }

    // ─── Assertions — All Rows ────────────────────────────────────────────────────

    /**
     * Assert that every data row has the same value in the specified column.
     *
     * <p>Useful for verifying that a batch export or report only contains records with
     * a specific status, type, or category.</p>
     *
     * <p>Example:</p>
     * <pre>Then all CSV rows have column "Status" equals "APPROVED"</pre>
     *
     * @param columnName the column to check in every row
     * @param expected   the value that every row must have in that column
     * @throws AssertionError if any row has a different value
     */
    public void assertAllRowsValueEquals(String columnName, String expected) {
        List<Map<String, String>> allRows = handler().getAllRows();
        for (int i = 0; i < allRows.size(); i++) {
            Map<String, String> row = allRows.get(i);
            if (!row.containsKey(columnName)) {
                throw new RuntimeException(
                    "PTAF CSV | Column [" + columnName + "] not found. Available: " + handler().getHeaders()
                );
            }
            String actual = row.getOrDefault(columnName, "");
            if (!expected.equals(actual)) {
                throw new AssertionError(
                    "PTAF CSV | Assertion failed — not all rows have [" + columnName + "] = [" + expected + "].\n" +
                    "  Row " + (i + 1) + " has value : [" + actual + "]"
                );
            }
        }
        logger.info("PTAF CSV | assertAllRowsValueEquals PASSED — all {} rows have [{}]=[{}]", allRows.size(), columnName, expected);
    }

    // ─── Extraction ──────────────────────────────────────────────────────────────

    /**
     * Extract the value of a specific cell and store it under a named variable for later use.
     *
     * <p>Example:</p>
     * <pre>When I extract CSV row 1 column "TransactionId" and store as "TXN_ID"
     * Then CSV row 2 column "RelatedId" equals stored value "TXN_ID"</pre>
     *
     * @param rowNumber    1-based row number
     * @param columnName   the column header name
     * @param variableName name to store the extracted value under
     */
    public void extractAndStore(int rowNumber, String columnName, String variableName) {
        String value = handler().getValue(rowNumber, columnName);
        variableStore.put(variableName, value);
        logger.info("PTAF CSV | Extracted [{}] from row={} col={} and stored as [{}]", value, rowNumber, columnName, variableName);
    }

    /**
     * Retrieve a previously stored variable value by name.
     *
     * @param variableName name of the variable to retrieve
     * @return the stored value
     * @throws RuntimeException if no variable with the given name has been stored
     */
    public String getStoredValue(String variableName) {
        if (!variableStore.containsKey(variableName)) {
            throw new RuntimeException(
                "PTAF CSV | Variable [" + variableName + "] has not been stored. " +
                "Use a step like: When I extract CSV row 1 column \"...\" and store as \"" + variableName + "\" first."
            );
        }
        return variableStore.get(variableName);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Clear the CSV context and variable store for the current scenario.
     * Call this in a Cucumber {@code @After} hook to ensure test isolation.
     */
    public void clear() {
        CsvContext.clear();
        variableStore.clear();
        logger.debug("PTAF CSV | Context and variable store cleared.");
    }

    // ─── Internal ─────────────────────────────────────────────────────────────────

    private CsvFileHandler handler() {
        CsvFileHandler h = CsvContext.get();
        if (h == null) {
            throw new IllegalStateException(
                "PTAF CSV | No CSV data is loaded. " +
                "Add a step like: Given I load CSV file \"path/to/file.csv\" " +
                "or: Given I load CSV from UI element on page \"X\" locator \"Y\" " +
                "before any CSV assertion or extraction steps."
            );
        }
        return h;
    }
}
