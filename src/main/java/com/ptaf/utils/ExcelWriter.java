package com.ptaf.utils;

import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for writing string values into Excel files using Apache POI.
 *
 * <p>
 * This class provides a single public static method to write or overwrite
 * data in a specific cell identified by a test case name (row) and a column name (header).
 * It will create the header row, the "Test Case" identifier column (at index 0),
 * new data columns, and new rows as needed. All operations target the first sheet
 * in the workbook (sheet index 0).
 * </p>
 *
 * <p>Important notes for testers and users:
 * - This utility opens the file with a FileInputStream and writes changes back using a FileOutputStream.
 *   The method uses try-with-resources to ensure streams and workbook are closed automatically.
 * - The "Test Case" column is enforced to be at column index 0. The search for the target row
 *   compares case-insensitively against string cell values in that column.
 * - When adding a new column, the code appends it after the last existing header cell.
 * - When adding a new row, the code creates it at sheet.getLastRowNum() + 1 which places it
 *   after the current last row.
 * - Concurrency: If multiple processes/threads try to modify the same file concurrently,
 *   unexpected results or file access exceptions can occur. Coordinate access to the file externally.
 * </p>
 *
 * <p>This class logs important actions and a formatted error block describing the exact reason
 * for failures when exceptions occur.</p>
 */
public class ExcelWriter {

    // Logger instance used for informational and error messages.
    private static final Logger logger = Logger.getLogger(ExcelWriter.class.getName());
    // Define the standardized name for the test case identifier column
    private static final String TEST_CASE_COLUMN_NAME = "Test Case";

    /**
     * Writes or overwrites data in a specific cell of an Excel file,
     * creating columns and rows as needed.
     *
     * ONLY change from your version:
     * - Logs "exact why it failed" in a clean, readable block (no behavior change)
     *
     * @param filePath Path to the Excel file.
     * @param testCaseName The name of the test case (row identifier).
     * @param columnName The name of the column to write to.
     * @param valueToWrite The string value to write in the cell.
     */
    public static void writeData(String filePath, String testCaseName, String columnName, String valueToWrite) {
        // Represent the target file on disk
        File file = new File(filePath);

        // Use try-with-resources to ensure input stream and workbook are closed automatically.
        // WorkbookFactory.create handles different Excel formats (HSSF/XSSF).
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            // Work on the first sheet only (index 0)
            Sheet sheet = workbook.getSheetAt(0);

            // 1. Get or create the header row (Row 0)
            // The header row is expected to contain column names including the "Test Case" header.
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                // Create header row if the sheet is empty or row 0 does not exist.
                headerRow = sheet.createRow(0);
            }

            // 2. Ensure "Test Case" column exists at index 0
            // This utility enforces a dedicated column at index 0 to store test case identifiers.
            Cell testCaseHeaderCell = headerRow.getCell(0);
            if (testCaseHeaderCell == null || testCaseHeaderCell.getStringCellValue().trim().isEmpty()) {
                // If missing or blank, create and set the header text.
                headerRow.createCell(0).setCellValue(TEST_CASE_COLUMN_NAME);
                logger.info("Created missing column: '" + TEST_CASE_COLUMN_NAME + "' at index 0.");
            }
            final int testCaseColIdx = 0;

            // 3. Find or create the target data column
            // Search header cells (row 0) for a column name that matches the requested columnName (case-insensitive).
            int dataColumnIdx = -1;
            for (Cell cell : headerRow) {
                if (cell.getStringCellValue().trim().equalsIgnoreCase(columnName)) {
                    dataColumnIdx = cell.getColumnIndex();
                    break;
                }
            }

            if (dataColumnIdx == -1) {
                // If the column does not exist, append it after the last header cell.
                // Note: getLastCellNum() returns the index after the last cell (as a short),
                // so it is safe to use as the insertion index when at least one cell exists.
                dataColumnIdx = headerRow.getLastCellNum();
                headerRow.createCell(dataColumnIdx).setCellValue(columnName);
                logger.info("Created new column: " + columnName);
            }

            // 4. Find the target row by test case name
            // Iterate rows starting from 1 (skip header at row 0) and match cells in column 0
            // using case-insensitive comparison of string cell values.
            Row targetRow = null;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                if (currentRow != null) {
                    Cell firstCell = currentRow.getCell(testCaseColIdx);
                    if (firstCell != null && firstCell.getCellType() == CellType.STRING &&
                            firstCell.getStringCellValue().trim().equalsIgnoreCase(testCaseName)) {
                        // Found the row corresponding to the given testCaseName.
                        targetRow = currentRow;
                        break;
                    }
                }
            }

            // 5. If row was not found, create it
            if (targetRow == null) {
                // Create a new row at index lastRowNum + 1 to append it after existing rows.
                // If the sheet previously had no rows other than header, this will append directly after header.
                int newRowNum = sheet.getLastRowNum() + 1;
                targetRow = sheet.createRow(newRowNum);
                // Set the Test Case identifier in the first column of the new row.
                targetRow.createCell(testCaseColIdx).setCellValue(testCaseName);
                logger.info("Test case '" + testCaseName + "' not found. Created a new row at index " + newRowNum + ".");
            }

            // 6. Write the data to the target cell
            // Use MissingCellPolicy.CREATE_NULL_AS_BLANK to ensure a cell instance exists.
            Cell targetCell = targetRow.getCell(dataColumnIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            targetCell.setCellValue(valueToWrite);

            // 7. Save the changes back to the file
            // Open a FileOutputStream to overwrite the existing file with the modified workbook.
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            logger.info("Data written successfully for Test Case '" + testCaseName + "'.");

        } catch (FileNotFoundException e) {
            // Log a clean, readable block that includes the exact reason and context (file path, test case, etc.)
            logger.log(Level.SEVERE, buildExcelPrettyError(
                    "EXCEL FILE NOT FOUND",
                    filePath, testCaseName, columnName, valueToWrite,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            ), e);

        } catch (Exception e) {
            // Catch-all for other exceptions (IO, POI parsing, etc.) and log detailed context.
            logger.log(Level.SEVERE, buildExcelPrettyError(
                    "EXCEL WRITE FAILURE",
                    filePath, testCaseName, columnName, valueToWrite,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            ), e);
        }
    }

    // ============================================================
    // Clean professional formatter (matches your other logs)
    // ============================================================

    /**
     * Builds a nicely formatted multi-line error block intended for log output.
     *
     * <p>The block includes a short title, file path, test case, column, value,
     * and the textual reason for failure. It is intended to make failures easy
     * to scan in logs and to provide exact diagnostic information without changing
     * application behavior.</p>
     *
     * @param title Short title describing the error context (e.g. "EXCEL WRITE FAILURE").
     * @param filePath Path to the Excel file that was being accessed.
     * @param testCaseName Test case identifier that was being used to locate the row.
     * @param columnName Column name that was being used to locate or create the cell.
     * @param valueToWrite The value attempted to be written.
     * @param reason A brief description of the reason / exception class and message.
     * @return A formatted multi-line string suitable for logging.
     */
    private static String buildExcelPrettyError(String title,
                                                String filePath,
                                                String testCaseName,
                                                String columnName,
                                                String valueToWrite,
                                                String reason) {

        // Use a StringBuilder to assemble the error block with clear labels and separators.
        StringBuilder sb = new StringBuilder();
        sb.append("\n========== ").append(title).append(" (EXACT WHY) ==========\n");
        sb.append("FilePath  : ").append(filePath).append("\n");
        sb.append("TestCase  : ").append(testCaseName).append("\n");
        sb.append("Column    : ").append(columnName).append("\n");
        sb.append("Value     : ").append(valueToWrite).append("\n");
        sb.append("Reason    : ").append(reason).append("\n");
        sb.append("===========================================================\n");
        return sb.toString();
    }
}
