package com.ptaf.csv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core CSV parsing and querying engine for the PTAF CSV automation module.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Loading CSV content from a filesystem path or a raw string (e.g., extracted from a UI element).</li>
 *   <li>Parsing the CSV into a list of rows, each represented as a {@code Map<String, String>}
 *       where keys are column header names and values are the cell values.</li>
 *   <li>Querying rows by 1-based row number and column name or 1-based column index.</li>
 *   <li>Counting rows, checking column existence, and iterating all rows.</li>
 * </ul>
 *
 * <p><strong>No external libraries are required.</strong> This class uses only the standard Java SE
 * APIs ({@code java.io}, {@code java.nio.file}) which are bundled with every JDK 11+ installation.</p>
 *
 * <h3>CSV format support:</h3>
 * <ul>
 *   <li>Comma-separated (default) and configurable delimiter (tab, semicolon, pipe, etc.).</li>
 *   <li>Optional header row (first row treated as column names by default).</li>
 *   <li>Quoted fields (values wrapped in double quotes, including fields containing commas or newlines).</li>
 *   <li>Empty fields (two consecutive delimiters produce an empty string value).</li>
 * </ul>
 *
 * <h3>Row numbering:</h3>
 * <p>Row numbers are 1-based and refer to data rows only (not the header row). So row 1 is the
 * first data row, row 2 is the second data row, etc.</p>
 *
 * <h3>Thread safety:</h3>
 * <p>Each instance holds its own parsed data. Instances are created per-scenario via
 * {@link CsvContext} and are not shared across threads.</p>
 */
public class CsvFileHandler {

    private static final Logger logger = LoggerFactory.getLogger(CsvFileHandler.class);

    /** Parsed data rows. Each row is a map of column name → cell value. */
    private List<Map<String, String>> rows = new ArrayList<>();

    /** Column headers in the order they appear in the CSV. */
    private List<String> headers = new ArrayList<>();

    /** The delimiter character used to split fields. Default is comma. */
    private char delimiter = ',';

    /** Whether the first row of the CSV is a header row. Default is true. */
    private boolean hasHeaders = true;

    // ─── Configuration ────────────────────────────────────────────────────────────

    /**
     * Set the delimiter character to use when parsing the CSV.
     *
     * <p>Common values:</p>
     * <ul>
     *   <li>{@code ','} — comma (default, standard CSV)</li>
     *   <li>{@code '\t'} — tab (TSV files)</li>
     *   <li>{@code ';'} — semicolon (common in European locales)</li>
     *   <li>{@code '|'} — pipe (common in data exports)</li>
     * </ul>
     *
     * @param delimiter the delimiter character
     */
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Set whether the first row of the CSV file is a header row.
     *
     * <p>When {@code true} (default), the first row is used as column names and is not
     * included in the data rows. When {@code false}, columns are accessed by index only
     * (1-based) and all rows are treated as data rows.</p>
     *
     * @param hasHeaders {@code true} if the first row contains column headers
     */
    public void setHasHeaders(boolean hasHeaders) {
        this.hasHeaders = hasHeaders;
    }

    // ─── Loading ─────────────────────────────────────────────────────────────────

    /**
     * Load and parse a CSV file from the filesystem.
     *
     * <p>The path is resolved relative to the project root (the directory from which Maven runs).
     * Both absolute paths and relative paths are supported.</p>
     *
     * <p>Example usage in a feature file:</p>
     * <pre>Given I load CSV file "src/test/resources/data/transactions.csv"</pre>
     *
     * @param filePath path to the CSV file (absolute or relative to project root)
     * @throws RuntimeException if the file does not exist, cannot be read, or cannot be parsed
     */
    public void loadFromFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV file path cannot be null or blank.");
        }
        Path path = Path.of(filePath.trim());
        if (!Files.exists(path)) {
            throw new RuntimeException(
                "PTAF CSV | File not found: [" + filePath + "]. " +
                "Check the path is correct and the file exists. " +
                "Paths are relative to the project root (where pom.xml is located)."
            );
        }
        try {
            logger.info("PTAF CSV | Loading CSV file: {}", filePath);
            String content = Files.readString(path, StandardCharsets.UTF_8);
            parse(content);
            logger.info("PTAF CSV | CSV file loaded: {} rows, {} columns.", rows.size(), headers.size());
        } catch (IOException e) {
            throw new RuntimeException(
                "PTAF CSV | Failed to read CSV file [" + filePath + "]: " + e.getMessage(), e
            );
        }
    }

    /**
     * Load and parse CSV content from a raw string.
     *
     * <p>This is used when CSV content has been extracted from a UI element (e.g., a textarea
     * showing exported data, a table rendered as plain text, or a file preview area).</p>
     *
     * <p>Example usage in a feature file:</p>
     * <pre>Given I load CSV from UI element on page "ReportPage" locator "csvOutput"</pre>
     *
     * @param csvContent raw CSV string to parse
     * @throws RuntimeException if the string is blank or cannot be parsed
     */
    public void loadFromString(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV content string cannot be null or blank.");
        }
        try {
            logger.info("PTAF CSV | Parsing CSV from string content ({} characters).", csvContent.length());
            parse(csvContent.trim());
            logger.info("PTAF CSV | CSV string parsed: {} rows, {} columns.", rows.size(), headers.size());
        } catch (Exception e) {
            throw new RuntimeException(
                "PTAF CSV | Failed to parse CSV string content. Root cause: " + e.getMessage(), e
            );
        }
    }

    // ─── Querying ─────────────────────────────────────────────────────────────────

    /**
     * Get the value of a specific cell identified by 1-based row number and column name.
     *
     * <p>Row 1 is the first data row (after the header, if present). Column names are
     * case-sensitive and must match the header row exactly.</p>
     *
     * @param rowNumber  1-based row number (1 = first data row)
     * @param columnName the column header name
     * @return the cell value, or an empty string if the cell is empty
     * @throws RuntimeException if the document has not been loaded, the row number is out of range,
     *                          or the column name does not exist
     */
    public String getValue(int rowNumber, String columnName) {
        ensureLoaded("getValue");
        validateRowNumber(rowNumber);
        Map<String, String> row = rows.get(rowNumber - 1);
        if (!row.containsKey(columnName)) {
            throw new RuntimeException(
                "PTAF CSV | Column [" + columnName + "] not found in row " + rowNumber + ". " +
                "Available columns: " + headers + ". " +
                "Column names are case-sensitive."
            );
        }
        String value = row.getOrDefault(columnName, "");
        logger.debug("PTAF CSV | getValue(row={}, col={}) = [{}]", rowNumber, columnName, value);
        return value;
    }

    /**
     * Get the value of a specific cell identified by 1-based row number and 1-based column index.
     *
     * <p>This is useful when the CSV has no headers or when the column position is known.
     * Column index 1 is the first column, column index 2 is the second, etc.</p>
     *
     * @param rowNumber   1-based row number
     * @param columnIndex 1-based column index
     * @return the cell value, or an empty string if the cell is empty
     * @throws RuntimeException if the row or column index is out of range
     */
    public String getValueByIndex(int rowNumber, int columnIndex) {
        ensureLoaded("getValueByIndex");
        validateRowNumber(rowNumber);
        if (columnIndex < 1 || columnIndex > headers.size()) {
            throw new RuntimeException(
                "PTAF CSV | Column index [" + columnIndex + "] is out of range. " +
                "The CSV has " + headers.size() + " columns (1-based index)."
            );
        }
        String columnName = headers.get(columnIndex - 1);
        return getValue(rowNumber, columnName);
    }

    /**
     * Get the total number of data rows in the CSV (excluding the header row).
     *
     * @return the number of data rows
     */
    public int getRowCount() {
        ensureLoaded("getRowCount");
        return rows.size();
    }

    /**
     * Check whether a column with the given name exists in the CSV headers.
     *
     * @param columnName the column header name to check (case-sensitive)
     * @return {@code true} if the column exists, {@code false} otherwise
     */
    public boolean columnExists(String columnName) {
        ensureLoaded("columnExists");
        return headers.contains(columnName);
    }

    /**
     * Get all column header names in the order they appear in the CSV.
     *
     * @return an unmodifiable list of column names
     */
    public List<String> getHeaders() {
        ensureLoaded("getHeaders");
        return Collections.unmodifiableList(headers);
    }

    /**
     * Get all data rows as a list of maps. Each map represents one row with column name → value pairs.
     *
     * @return an unmodifiable list of row maps
     */
    public List<Map<String, String>> getAllRows() {
        ensureLoaded("getAllRows");
        return Collections.unmodifiableList(rows);
    }

    /**
     * Get a single data row as a map of column name → value pairs.
     *
     * @param rowNumber 1-based row number
     * @return the row as a map
     * @throws RuntimeException if the row number is out of range
     */
    public Map<String, String> getRow(int rowNumber) {
        ensureLoaded("getRow");
        validateRowNumber(rowNumber);
        return Collections.unmodifiableMap(rows.get(rowNumber - 1));
    }

    // ─── Internal parsing ─────────────────────────────────────────────────────────

    /**
     * Parse the raw CSV content into the {@link #rows} and {@link #headers} fields.
     * Handles quoted fields, empty fields, and configurable delimiters.
     */
    private void parse(String content) throws IOException {
        rows = new ArrayList<>();
        headers = new ArrayList<>();
        List<List<String>> allRows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    allRows.add(parseLine(line));
                }
            }
        }
        if (allRows.isEmpty()) {
            logger.warn("PTAF CSV | The CSV content is empty — no rows were parsed.");
            return;
        }
        if (hasHeaders) {
            // First row is the header
            headers = new ArrayList<>(allRows.get(0));
            for (int i = 1; i < allRows.size(); i++) {
                rows.add(rowToMap(allRows.get(i), headers));
            }
        } else {
            // No headers — generate synthetic column names: Column1, Column2, ...
            int colCount = allRows.get(0).size();
            for (int c = 1; c <= colCount; c++) {
                headers.add("Column" + c);
            }
            for (List<String> rawRow : allRows) {
                rows.add(rowToMap(rawRow, headers));
            }
        }
    }

    /**
     * Parse a single CSV line into a list of field values.
     * Handles quoted fields that may contain the delimiter character or newlines.
     */
    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped double quote inside quoted field
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /** Convert a list of field values to a map using the provided headers as keys. */
    private Map<String, String> rowToMap(List<String> fields, List<String> columnHeaders) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < columnHeaders.size(); i++) {
            String value = i < fields.size() ? fields.get(i) : "";
            map.put(columnHeaders.get(i), value);
        }
        return map;
    }

    /** Throw a clear error if no CSV has been loaded yet. */
    private void ensureLoaded(String operation) {
        if (rows == null || (rows.isEmpty() && headers.isEmpty())) {
            throw new IllegalStateException(
                "PTAF CSV | Cannot perform [" + operation + "] — no CSV data is loaded. " +
                "Add a step like: Given I load CSV file \"path/to/file.csv\" " +
                "or: Given I load CSV from UI element on page \"X\" locator \"Y\" " +
                "before any CSV assertion or extraction steps."
            );
        }
    }

    /** Validate that a 1-based row number is within the valid range. */
    private void validateRowNumber(int rowNumber) {
        if (rowNumber < 1 || rowNumber > rows.size()) {
            throw new RuntimeException(
                "PTAF CSV | Row number [" + rowNumber + "] is out of range. " +
                "The CSV has " + rows.size() + " data row(s). " +
                "Row numbers are 1-based (row 1 = first data row after the header)."
            );
        }
    }
}
