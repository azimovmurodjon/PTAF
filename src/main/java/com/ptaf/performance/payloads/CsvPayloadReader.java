package com.ptaf.performance.payloads;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Framework-owned CSV payload reader.
 *
 * <p>Resolution order for locating the CSV file:
 * <ol>
 *   <li>classpath resource (as provided)</li>
 *   <li>classpath resource (with leading '/' removed)</li>
 *   <li>direct filesystem path (absolute or relative)</li>
 *   <li>src/test/resources fallback (useful for tests)</li>
 * </ol>
 * </p>
 *
 * <p>Assumptions:
 * - The first row of the CSV is a header row containing column names.
 * - The first column in each subsequent row is a row identifier (used to locate a specific row).
 * - CSV values are simple and do not require full-featured CSV parsing (see splitCsvLine).</p>
 */
public final class CsvPayloadReader {

    /**
     * Private utility constructor to prevent instantiation.
     * This class provides static helper methods only.
     */
    private CsvPayloadReader() {
    }

    /**
     * Retrieve a specific value from a CSV payload.
     *
     * <p>Behavior summary:
     * - Validates inputs (file path/classpath resource, row identifier, and column name).
     * - Opens a reader for the specified resource using the resolution order described in the class Javadoc.
     * - Reads the header line, normalizes header names, and maps header names (lower-cased) to their column indices.
     * - Searches each non-blank data row for a matching row identifier (first column). Matching is case-insensitive
     *   and trims whitespace.
     * - If the requested column index exists for the matched row, returns the normalized cell value.
     * - If the column index is beyond the number of columns for the matched row, returns null.
     * - If the row identifier or column name cannot be found, throws IllegalArgumentException.
     * - Any other failures are wrapped in a RuntimeException to provide context for callers.
     *
     * @param filePathOrClasspathResource path to CSV or classpath resource name (non-null, non-blank)
     * @param rowIdentifier               value in the first column used to identify the desired row (non-null, non-blank)
     * @param columnName                  header name of the column to retrieve (non-null, non-blank)
     * @return the normalized value for the specified row identifier and column, or null if the column exists
     *         but the concrete row does not have that many columns
     * @throws IllegalArgumentException if any input is invalid, or if the requested column/header or row identifier
     *                                  cannot be found in the CSV
     * @throws RuntimeException         if an unexpected I/O or parsing error occurs while reading the CSV
     */
    public static String getData(String filePathOrClasspathResource,
                                 String rowIdentifier,
                                 String columnName) {
        // Validate input parameters early to provide clear error messages to callers/testers.
        if (filePathOrClasspathResource == null || filePathOrClasspathResource.isBlank()) {
            throw new IllegalArgumentException("CSV payload file path cannot be null or blank.");
        }

        if (rowIdentifier == null || rowIdentifier.isBlank()) {
            throw new IllegalArgumentException("CSV row identifier cannot be null or blank.");
        }

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("CSV column name cannot be null or blank.");
        }

        // Use try-with-resources to ensure the BufferedReader is closed automatically.
        try (BufferedReader reader = openReader(filePathOrClasspathResource)) {
            // Read and validate the header line (first line of CSV).
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalStateException("CSV payload file is empty: " + filePathOrClasspathResource);
            }

            // Split the header line into individual header values.
            String[] rawHeaders = splitCsvLine(headerLine);
            // Maintain insertion order for predictable output when listing available headers.
            Map<String, Integer> headerIndexMap = new LinkedHashMap<>();

            // Normalize header names and map them to their column indices.
            for (int i = 0; i < rawHeaders.length; i++) {
                String normalizedHeader = normalizeHeader(rawHeaders[i]);
                // Use lowercase keys to perform case-insensitive header lookup.
                headerIndexMap.put(normalizedHeader.toLowerCase(), i);
            }

            // Normalize the requested column name as well for comparison.
            String normalizedRequestedColumn = normalizeHeader(columnName).toLowerCase();

            // If the requested column name is not present in the header, provide a helpful error message
            // listing the available headers.
            if (!headerIndexMap.containsKey(normalizedRequestedColumn)) {
                String availableHeaders = headerIndexMap.keySet()
                        .stream()
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException(
                        "CSV column not found: " + columnName + ". Available columns: [" + availableHeaders + "]"
                );
            }

            // Iterate through the remaining lines to find the row matching the given row identifier.
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty/blank lines (common in hand-edited CSVs).
                if (line.isBlank()) {
                    continue;
                }

                // Split the data line into values.
                String[] values = splitCsvLine(line);
                if (values.length == 0) {
                    // Defensive: skip rows that do not produce any columns.
                    continue;
                }

                // The first column is expected to be the row identifier. Normalize it for comparison.
                String currentRowId = normalizeValue(values[0]);
                // Compare identifiers case-insensitively and trim whitespace from the provided identifier.
                if (currentRowId.equalsIgnoreCase(rowIdentifier.trim())) {
                    int columnIndex = headerIndexMap.get(normalizedRequestedColumn);
                    // If the requested column index is beyond the actual number of values, return null.
                    // This covers rows with missing trailing columns.
                    if (columnIndex >= values.length) {
                        return null;
                    }
                    // Return the normalized cell value for the matched row and column.
                    return normalizeValue(values[columnIndex]);
                }
            }

            // If we exhausted the file without finding the requested row identifier, throw a clear error.
            throw new IllegalArgumentException(
                    "CSV row identifier not found. File: " + filePathOrClasspathResource + ", Row: " + rowIdentifier
            );

        } catch (Exception e) {
            // Wrap unexpected exceptions with a RuntimeException that includes context useful for troubleshooting.
            throw new RuntimeException(
                    "Failed to read CSV payload. File: " + filePathOrClasspathResource
                            + ", Row: " + rowIdentifier
                            + ", Column: " + columnName,
                    e
            );
        }
    }

    /**
     * Open a BufferedReader for the given CSV resource using the resolution order described in the class Javadoc.
     *
     * Resolution steps:
     * 1) Attempt to load the resource from the context classloader using the provided path as-is.
     * 2) If not found and the path starts with '/', remove the leading slash and try again.
     * 3) If still not found, attempt to treat the path as a filesystem path and open a reader if the file exists.
     * 4) Finally, attempt the test resources fallback at 'src/test/resources/<path>'.
     *
     * Note: This method throws Exception to allow callers to wrap errors with additional context.
     *
     * @param filePathOrClasspathResource the resource name or filesystem path to locate
     * @return BufferedReader for reading the CSV content
     * @throws Exception if an I/O issue occurs while opening an existing file
     * @throws IllegalStateException if the resource cannot be found in any of the lookup locations
     */
    private static BufferedReader openReader(String filePathOrClasspathResource) throws Exception {
        // First try: treat the input as a classpath resource using the context classloader.
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(filePathOrClasspathResource);

        if (inputStream != null) {
            // Found as-is on the classpath.
            return new BufferedReader(new InputStreamReader(inputStream));
        }

        // Second try: remove leading slash (if any) and try again.
        String normalizedClasspathPath = filePathOrClasspathResource.startsWith("/")
                ? filePathOrClasspathResource.substring(1)
                : filePathOrClasspathResource;

        inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(normalizedClasspathPath);

        if (inputStream != null) {
            return new BufferedReader(new InputStreamReader(inputStream));
        }

        // Third try: treat the provided string as a direct filesystem path.
        Path directPath = Paths.get(filePathOrClasspathResource);
        if (Files.exists(directPath)) {
            return Files.newBufferedReader(directPath);
        }

        // Fourth try: fallback to 'src/test/resources' which is useful for unit/integration tests.
        Path testResourcesPath = Paths.get("src", "test", "resources", normalizedClasspathPath);
        if (Files.exists(testResourcesPath)) {
            return Files.newBufferedReader(testResourcesPath);
        }

        // If none of the locations yielded a resource, throw with helpful diagnostic information.
        throw new IllegalStateException(
                "CSV payload file not found. Checked classpath and filesystem. " +
                        "Provided path: " + filePathOrClasspathResource +
                        ", Filesystem fallback: " + testResourcesPath.toAbsolutePath()
        );
    }

    /**
     * Simple CSV split.
     *
     * <p>Current implementation assumes payload values do not require advanced CSV parsing beyond simple comma splitting.
     * - Splits on commas and preserves empty trailing values using the negative limit parameter.</p>
     *
     * @param line CSV line to split
     * @return array of cell values (may contain empty strings)
     */
    private static String[] splitCsvLine(String line) {
        // Use split with a negative limit to ensure trailing empty values are included.
        return line.split(",", -1);
    }

    /**
     * Normalize header names read from the CSV header row.
     *
     * Normalization steps:
     * - Null headers become an empty string.
     * - Leading/trailing whitespace is trimmed.
     * - Remove Unicode Byte Order Mark (BOM) character '\uFEFF' if present at the start.
     * - If the header is quoted (starts and ends with a double quote), unwrap the quotes and trim the result.
     *
     * @param value raw header string from CSV
     * @return cleaned header string suitable for indexing and comparison
     */
    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        // Remove BOM if present as the first character.
        if (!cleaned.isEmpty() && cleaned.charAt(0) == '\uFEFF') {
            cleaned = cleaned.substring(1);
        }

        // If the header is quoted, remove the surrounding quotes and trim inside content.
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return cleaned;
    }

    /**
     * Normalize individual cell values from the CSV.
     *
     * Normalization steps:
     * - If the input is null, return null.
     * - Trim surrounding whitespace.
     * - If the value is wrapped in double quotes, unwrap them (but preserve inner escaped quotes).
     * - Replace CSV-escaped double quotes ("") with a single double quote character (").
     *
     * @param value raw cell value from CSV
     * @return cleaned cell value with common CSV quoting conventions normalized
     */
    private static String normalizeValue(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        // If the cell is quoted, strip the surrounding quotes (but do not trim inside).
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        // Replace escaped double-quotes ("" -> ") according to basic CSV conventions.
        return cleaned.replace("\"\"", "\"");
    }
}
