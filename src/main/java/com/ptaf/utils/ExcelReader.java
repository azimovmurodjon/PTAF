package com.ptaf.utils;

import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for reading data from an Excel file.
 *
 * <p>
 * This class exposes a single public method {@link #getData(String, String, String)}
 * which reads the first sheet of the provided Excel file and searches for a row
 * whose first cell matches the provided test case name (case-insensitive).
 * It then returns the value found in the column specified by columnName from that row.
 * </p>
 *
 * <p>
 * Error cases (sheet empty, column not found, test case not found, IO or parsing errors)
 * are logged using the java.util.logging.Logger and the method returns null in all failure cases.
 * </p>
 *
 * Note for testers:
 * - Provide the exact file path to the Excel file.
 * - The first sheet (index 0) is used.
 * - The first row is expected to be the header row.
 * - The first column in each subsequent row is compared (case-insensitively) against testCaseName.
 * - The cell value is returned as String via Cell#toString(); formatting is not altered.
 */
public class ExcelReader {

    /**
     * Logger instance for logging warnings and errors encountered while reading Excel files.
     */
    private static final Logger logger = Logger.getLogger(ExcelReader.class.getName());

    /**
     * Reads an Excel file and returns the string value of the cell located at the intersection
     * of the row matching testCaseName (compares against the first column of each row) and the
     * column identified by columnName in the header row.
     *
     * <p>
     * Behavior:
     * - Opens the Excel file using a FileInputStream and Apache POI's WorkbookFactory (try-with-resources ensures closing).
     * - Uses the first sheet (sheet index 0).
     * - Expects the first row to be the header row which maps column names to indices.
     * - Iterates subsequent rows to find the one where the first cell equals testCaseName (case-insensitive).
     * - If found, retrieves and returns the target cell's string value via {@code Cell#toString()}.
     * - On any failure (empty sheet, missing column, missing test case, empty target cell, or exception),
     *   the method logs a descriptive, formatted message and returns null.
     * </p>
     *
     * @param filePath     absolute or relative file path to the Excel file to read
     * @param testCaseName the test case name to search for in the first column of each row
     * @param columnName   the name of the column (as present in the header row) whose value should be returned
     * @return the String representation of the target cell if found; null if not found or any error occurs
     */
    public static String getData(String filePath, String testCaseName, String columnName) {
        // Use try-with-resources to ensure both FileInputStream and Workbook are closed automatically
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // Always read the first sheet (sheet index 0)
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // If there are no rows at all, log and return null
            if (!rowIterator.hasNext()) {
                logger.warning(buildExcelPrettyError(
                        "EXCEL SHEET IS EMPTY",
                        filePath, testCaseName, columnName,
                        "No rows found in the sheet"
                ));
                return null;
            }

            // Read the header row and build a map of headerName -> columnIndex
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                // Use cell.toString() and trim whitespace to obtain a normalized header name
                headerMap.put(cell.toString().trim(), cell.getColumnIndex());
            }

            // If the requested column name is not present in the header, log and return null
            if (!headerMap.containsKey(columnName)) {
                logger.warning(buildExcelPrettyError(
                        "COLUMN NOT FOUND",
                        filePath, testCaseName, columnName,
                        "Column does not exist in header row"
                ));
                return null;
            }

            // Iterate over remaining rows to find the row whose first cell matches testCaseName
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell firstCell = row.getCell(0); // compare against the first column of the row
                if (firstCell != null && firstCell.toString().trim().equalsIgnoreCase(testCaseName)) {
                    // Found matching row; obtain the column index for the requested column
                    Integer colIndex = headerMap.get(columnName);
                    if (colIndex != null) {
                        // Get the target cell from the matched row using the column index
                        Cell targetCell = row.getCell(colIndex);
                        if (targetCell != null) {
                            // Return the cell's string representation (as produced by POI's toString)
                            return targetCell.toString();
                        } else {
                            // Target cell is absent or explicitly empty in the found row
                            logger.warning(buildExcelPrettyError(
                                    "TARGET CELL IS NULL",
                                    filePath, testCaseName, columnName,
                                    "Row exists but cell is empty/null"
                            ));
                            return null;
                        }
                    } else {
                        // Defensive check: headerMap should have contained the column, but handle null gracefully
                        logger.warning(buildExcelPrettyError(
                                "COLUMN INDEX IS NULL",
                                filePath, testCaseName, columnName,
                                "Header map returned null index"
                        ));
                        return null;
                    }
                }
            }

            // If iteration completes without finding the test case, log and return null
            logger.warning(buildExcelPrettyError(
                    "TEST CASE NOT FOUND",
                    filePath, testCaseName, columnName,
                    "No row matched the given test case name"
            ));

        } catch (Exception e) {
            // Any exception (IO, format, POI parsing, etc.) is logged with SEVERE level and the stack trace
            logger.log(Level.SEVERE, buildExcelPrettyError(
                    "EXCEL READ FAILURE",
                    filePath, testCaseName, columnName,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            ), e);
        }
        // Default return null on failure
        return null;
    }

    // ============================================================
    // Same clean professional formatter used in ExcelWriter
    // ============================================================

    /**
     * Helper to build a consistently formatted error message for Excel-related issues.
     *
     * <p>
     * This method centralizes the formatting of error logs related to Excel read operations.
     * The output includes a clear title, file path, test case, column and a human-readable reason.
     * </p>
     *
     * @param title        short title categorizing the error (e.g. "COLUMN NOT FOUND")
     * @param filePath     the Excel file path being processed
     * @param testCaseName the test case name searched for in the file
     * @param columnName   the column name requested
     * @param reason       brief explanation of why the error occurred
     * @return fully formatted multi-line string suitable for logging
     */
    private static String buildExcelPrettyError(String title,
                                                String filePath,
                                                String testCaseName,
                                                String columnName,
                                                String reason) {

        StringBuilder sb = new StringBuilder();
        sb.append("\n========== ").append(title).append(" (EXACT WHY) ==========\n");
        sb.append("FilePath  : ").append(filePath).append("\n");
        sb.append("TestCase  : ").append(testCaseName).append("\n");
        sb.append("Column    : ").append(columnName).append("\n");
        sb.append("Reason    : ").append(reason).append("\n");
        sb.append("===========================================================\n");
        return sb.toString();
    }
}
