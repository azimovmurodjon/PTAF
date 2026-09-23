package com.ptaf.stepdefinitions;

import com.ptaf.csv.CsvCommonMethods;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.pages.PageCommonMethods;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber step definitions for CSV automation in the PTAF framework.
 *
 * <p>This class provides two categories of CSV steps:</p>
 *
 * <h3>1. File-based CSV steps</h3>
 * <p>Load a CSV file from the filesystem and assert or extract values from it.</p>
 * <pre>
 * Given I load CSV file "src/test/resources/data/transactions.csv"
 * Then CSV row 1 column "Amount" equals "1250.00"
 * Then CSV row count equals 5
 * </pre>
 *
 * <h3>2. UI-embedded CSV steps</h3>
 * <p>Extract CSV content from a visible UI element (e.g., a textarea, exported data area, or
 * pre-formatted text block) and then assert or extract values from it.</p>
 * <pre>
 * Given I load CSV from UI element on page "ReportPage" locator "csvOutput"
 * Then CSV row 1 column "Status" equals "APPROVED"
 * Then all CSV rows have column "Currency" equals "USD"
 * </pre>
 *
 * <h3>Row and column numbering</h3>
 * <ul>
 *   <li>Row numbers are <strong>1-based</strong> and refer to data rows only (not the header row).
 *       Row 1 is the first data row after the header.</li>
 *   <li>Column names are <strong>case-sensitive</strong> and must match the header row exactly.</li>
 *   <li>Column indexes are <strong>1-based</strong> (column 1 = first column).</li>
 * </ul>
 *
 * <h3>Thread safety and lifecycle</h3>
 * <p>The loaded CSV data is stored in a {@link com.ptaf.csv.CsvContext} ThreadLocal and is
 * automatically cleared after each scenario via the {@code @After} hook in this class.</p>
 */
public class CsvSteps {

    private static final Logger logger = LoggerFactory.getLogger(CsvSteps.class);

    /** Shared CSV methods instance — holds the variable store for this scenario. */
    private final CsvCommonMethods csv = new CsvCommonMethods();

    // ─── Loading Steps ────────────────────────────────────────────────────────────

    /**
     * Load and parse a CSV file from the filesystem into the current scenario's CSV context.
     *
     * <p>The path is resolved relative to the project root (where {@code pom.xml} is located).
     * The file must use comma as the delimiter. For other delimiters, use
     * {@link #iLoadCsvFileWithDelimiter(String, String)}.</p>
     *
     * <p>Example:</p>
     * <pre>Given I load CSV file "src/test/resources/data/transactions.csv"</pre>
     *
     * @param filePath path to the CSV file
     */
    @Given("I load CSV file {string}")
    public void iLoadCsvFile(String filePath) {
        csv.loadFromFile(filePath);
    }

    /**
     * Load and parse a CSV file using a custom delimiter character.
     *
     * <p>Use this step when the file uses a delimiter other than comma, such as semicolon
     * (common in European locales), tab (TSV files), or pipe.</p>
     *
     * <p>Examples:</p>
     * <pre>
     * Given I load CSV file "data/export.csv" with delimiter ";"
     * Given I load CSV file "data/export.tsv" with delimiter "\t"
     * Given I load CSV file "data/export.txt" with delimiter "|"
     * </pre>
     *
     * @param filePath  path to the CSV file
     * @param delimiter the delimiter string (first character is used)
     */
    @Given("I load CSV file {string} with delimiter {string}")
    public void iLoadCsvFileWithDelimiter(String filePath, String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            throw new IllegalArgumentException("Delimiter cannot be empty.");
        }
        char delimChar = delimiter.equals("\\t") ? '\t' : delimiter.charAt(0);
        csv.loadFromFile(filePath, delimChar);
    }

    /**
     * Extract CSV content from a visible UI element and load it into the CSV context.
     *
     * <p>This step finds the UI element identified by the given page and locator keys,
     * extracts its text content, and parses it as CSV. The element must contain valid CSV data.</p>
     *
     * <p>Typical use cases:</p>
     * <ul>
     *   <li>A {@code <textarea>} showing exported CSV data</li>
     *   <li>A {@code <pre>} block displaying formatted CSV output</li>
     *   <li>A read-only input field containing CSV content</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>Given I load CSV from UI element on page "ReportPage" locator "csvOutput"</pre>
     *
     * @param page    the logical page name matching the YAML element file (e.g., "ReportPage")
     * @param locator the element key matching the YAML element file (e.g., "csvOutput")
     */
    @Given("I load CSV from UI element on page {string} locator {string}")
    public void iLoadCsvFromUiElement(String page, String locator) {
        com.microsoft.playwright.Page playwrightPage = Hooks.getPage();
        PageCommonMethods pcm = new PageCommonMethods(playwrightPage);
        // Use gettext() which resolves the locator via YAML and returns the element's inner text.
        String rawContent = pcm.gettext(playwrightPage, page, locator);
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new RuntimeException(
                "PTAF CSV | UI element on page [" + page + "] locator [" + locator + "] " +
                "contains no text content. Ensure the element is visible and contains CSV data."
            );
        }
        csv.loadFromString(rawContent);
        logger.info("PTAF CSV | Loaded CSV from UI element [{}.{}]", page, locator);
    }

    // ─── Assertion Steps — Cell Value by Column Name ──────────────────────────────

    /**
     * Assert that the value of a cell (identified by row number and column name) equals the expected value.
     *
     * <p>Example:</p>
     * <pre>Then CSV row 1 column "Amount" equals "1250.00"</pre>
     *
     * @param rowNumber  1-based row number
     * @param columnName the column header name (case-sensitive)
     * @param expected   the exact expected value
     */
    @Then("CSV row {int} column {string} equals {string}")
    public void csvRowColumnEquals(int rowNumber, String columnName, String expected) {
        csv.assertValueEquals(rowNumber, columnName, expected);
    }

    /**
     * Assert that the value of a cell contains the expected substring.
     *
     * <p>Example:</p>
     * <pre>Then CSV row 2 column "Description" contains "payment"</pre>
     *
     * @param rowNumber  1-based row number
     * @param columnName the column header name
     * @param expected   the substring that must be present
     */
    @Then("CSV row {int} column {string} contains {string}")
    public void csvRowColumnContains(int rowNumber, String columnName, String expected) {
        csv.assertValueContains(rowNumber, columnName, expected);
    }

    /**
     * Assert that the value of a cell does NOT equal the given value.
     *
     * <p>Example:</p>
     * <pre>Then CSV row 1 column "Status" does not equal "FAILED"</pre>
     *
     * @param rowNumber  1-based row number
     * @param columnName the column header name
     * @param expected   the value that must NOT be present
     */
    @Then("CSV row {int} column {string} does not equal {string}")
    public void csvRowColumnNotEquals(int rowNumber, String columnName, String expected) {
        csv.assertValueNotEquals(rowNumber, columnName, expected);
    }

    // ─── Assertion Steps — Cell Value by Column Index ─────────────────────────────

    /**
     * Assert that the value of a cell (identified by row number and 1-based column index) equals
     * the expected value.
     *
     * <p>Use this when the CSV has no headers or when you prefer index-based access.</p>
     *
     * <p>Example:</p>
     * <pre>Then CSV row 1 column index 3 equals "1250.00"</pre>
     *
     * @param rowNumber   1-based row number
     * @param columnIndex 1-based column index
     * @param expected    the exact expected value
     */
    @Then("CSV row {int} column index {int} equals {string}")
    public void csvRowColumnIndexEquals(int rowNumber, int columnIndex, String expected) {
        csv.assertValueEqualsByIndex(rowNumber, columnIndex, expected);
    }

    // ─── Assertion Steps — Structure ─────────────────────────────────────────────

    /**
     * Assert that the CSV contains exactly the expected number of data rows (excluding header).
     *
     * <p>Example:</p>
     * <pre>Then CSV row count equals 5</pre>
     *
     * @param expectedCount the exact expected row count
     */
    @Then("CSV row count equals {int}")
    public void csvRowCountEquals(int expectedCount) {
        csv.assertRowCount(expectedCount);
    }

    /**
     * Assert that the CSV contains at least the expected number of data rows.
     *
     * <p>Example:</p>
     * <pre>Then CSV row count is at least 3</pre>
     *
     * @param minimumCount the minimum expected row count
     */
    @Then("CSV row count is at least {int}")
    public void csvRowCountAtLeast(int minimumCount) {
        csv.assertRowCountAtLeast(minimumCount);
    }

    /**
     * Assert that a column with the given name exists in the CSV headers.
     *
     * <p>Example:</p>
     * <pre>Then CSV column "Amount" exists</pre>
     *
     * @param columnName the column header name to check (case-sensitive)
     */
    @Then("CSV column {string} exists")
    public void csvColumnExists(String columnName) {
        csv.assertColumnExists(columnName);
    }

    /**
     * Assert that a column with the given name does NOT exist in the CSV headers.
     *
     * <p>Example:</p>
     * <pre>Then CSV column "InternalCode" does not exist</pre>
     *
     * @param columnName the column header name to check
     */
    @Then("CSV column {string} does not exist")
    public void csvColumnNotExists(String columnName) {
        csv.assertColumnNotExists(columnName);
    }

    // ─── Assertion Steps — All Rows ───────────────────────────────────────────────

    /**
     * Assert that every data row has the same value in the specified column.
     *
     * <p>Example:</p>
     * <pre>Then all CSV rows have column "Status" equals "APPROVED"</pre>
     *
     * @param columnName the column to check in every row
     * @param expected   the value that every row must have in that column
     */
    @Then("all CSV rows have column {string} equals {string}")
    public void allCsvRowsHaveColumnEquals(String columnName, String expected) {
        csv.assertAllRowsValueEquals(columnName, expected);
    }

    // ─── Assertion Steps — Stored Values ─────────────────────────────────────────

    /**
     * Assert that the value of a cell equals a previously stored variable value.
     *
     * <p>Example:</p>
     * <pre>
     * When I extract CSV row 1 column "TransactionId" and store as "TXN_ID"
     * Then CSV row 2 column "RelatedId" equals stored value "TXN_ID"
     * </pre>
     *
     * @param rowNumber    1-based row number
     * @param columnName   the column header name
     * @param variableName name of the previously stored variable
     */
    @Then("CSV row {int} column {string} equals stored value {string}")
    public void csvRowColumnEqualsStoredValue(int rowNumber, String columnName, String variableName) {
        String expected = csv.getStoredValue(variableName);
        csv.assertValueEquals(rowNumber, columnName, expected);
    }

    // ─── Extraction Steps ─────────────────────────────────────────────────────────

    /**
     * Extract the value of a specific cell and store it under a named variable for use in later steps.
     *
     * <p>Example:</p>
     * <pre>When I extract CSV row 1 column "TransactionId" and store as "TXN_ID"</pre>
     *
     * @param rowNumber    1-based row number
     * @param columnName   the column header name
     * @param variableName name to store the extracted value under
     */
    @When("I extract CSV row {int} column {string} and store as {string}")
    public void iExtractCsvRowColumnAndStoreAs(int rowNumber, String columnName, String variableName) {
        csv.extractAndStore(rowNumber, columnName, variableName);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Cucumber {@code @After} hook that clears the CSV context and variable store after each scenario.
     * This ensures test isolation — CSV data loaded in one scenario does not bleed into the next.
     */
    @After
    public void clearCsvContext() {
        csv.clear();
    }
}
