package com.ptaf.utils;

import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExcelWriter {

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
        File file = new File(filePath);

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 1. Get or create the header row (Row 0)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                headerRow = sheet.createRow(0);
            }

            // 2. Ensure "Test Case" column exists at index 0
            Cell testCaseHeaderCell = headerRow.getCell(0);
            if (testCaseHeaderCell == null || testCaseHeaderCell.getStringCellValue().trim().isEmpty()) {
                headerRow.createCell(0).setCellValue(TEST_CASE_COLUMN_NAME);
                logger.info("Created missing column: '" + TEST_CASE_COLUMN_NAME + "' at index 0.");
            }
            final int testCaseColIdx = 0;

            // 3. Find or create the target data column
            int dataColumnIdx = -1;
            for (Cell cell : headerRow) {
                if (cell.getStringCellValue().trim().equalsIgnoreCase(columnName)) {
                    dataColumnIdx = cell.getColumnIndex();
                    break;
                }
            }

            if (dataColumnIdx == -1) {
                dataColumnIdx = headerRow.getLastCellNum();
                headerRow.createCell(dataColumnIdx).setCellValue(columnName);
                logger.info("Created new column: " + columnName);
            }

            // 4. Find the target row by test case name
            Row targetRow = null;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row currentRow = sheet.getRow(i);
                if (currentRow != null) {
                    Cell firstCell = currentRow.getCell(testCaseColIdx);
                    if (firstCell != null && firstCell.getCellType() == CellType.STRING &&
                            firstCell.getStringCellValue().trim().equalsIgnoreCase(testCaseName)) {
                        targetRow = currentRow;
                        break;
                    }
                }
            }

            // 5. If row was not found, create it
            if (targetRow == null) {
                int newRowNum = sheet.getLastRowNum() + 1;
                targetRow = sheet.createRow(newRowNum);
                targetRow.createCell(testCaseColIdx).setCellValue(testCaseName);
                logger.info("Test case '" + testCaseName + "' not found. Created a new row at index " + newRowNum + ".");
            }

            // 6. Write the data to the target cell
            Cell targetCell = targetRow.getCell(dataColumnIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            targetCell.setCellValue(valueToWrite);

            // 7. Save the changes back to the file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            logger.info("Data written successfully for Test Case '" + testCaseName + "'.");

        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, buildExcelPrettyError(
                    "EXCEL FILE NOT FOUND",
                    filePath, testCaseName, columnName, valueToWrite,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            ), e);

        } catch (Exception e) {
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
    private static String buildExcelPrettyError(String title,
                                                String filePath,
                                                String testCaseName,
                                                String columnName,
                                                String valueToWrite,
                                                String reason) {

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