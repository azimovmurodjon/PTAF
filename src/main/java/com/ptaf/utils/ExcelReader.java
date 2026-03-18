package com.ptaf.utils;

import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExcelReader {

    private static final Logger logger = Logger.getLogger(ExcelReader.class.getName());

    public static String getData(String filePath, String testCaseName, String columnName) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                logger.warning(buildExcelPrettyError(
                        "EXCEL SHEET IS EMPTY",
                        filePath, testCaseName, columnName,
                        "No rows found in the sheet"
                ));
                return null;
            }

            // Read header row
            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                headerMap.put(cell.toString().trim(), cell.getColumnIndex());
            }

            if (!headerMap.containsKey(columnName)) {
                logger.warning(buildExcelPrettyError(
                        "COLUMN NOT FOUND",
                        filePath, testCaseName, columnName,
                        "Column does not exist in header row"
                ));
                return null;
            }

            // Find the row with the matching test case name
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell firstCell = row.getCell(0);
                if (firstCell != null && firstCell.toString().trim().equalsIgnoreCase(testCaseName)) {
                    Integer colIndex = headerMap.get(columnName);
                    if (colIndex != null) {
                        Cell targetCell = row.getCell(colIndex);
                        if (targetCell != null) {
                            return targetCell.toString();
                        } else {
                            logger.warning(buildExcelPrettyError(
                                    "TARGET CELL IS NULL",
                                    filePath, testCaseName, columnName,
                                    "Row exists but cell is empty/null"
                            ));
                            return null;
                        }
                    } else {
                        logger.warning(buildExcelPrettyError(
                                "COLUMN INDEX IS NULL",
                                filePath, testCaseName, columnName,
                                "Header map returned null index"
                        ));
                        return null;
                    }
                }
            }

            logger.warning(buildExcelPrettyError(
                    "TEST CASE NOT FOUND",
                    filePath, testCaseName, columnName,
                    "No row matched the given test case name"
            ));

        } catch (Exception e) {
            logger.log(Level.SEVERE, buildExcelPrettyError(
                    "EXCEL READ FAILURE",
                    filePath, testCaseName, columnName,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            ), e);
        }
        return null;
    }

    // ============================================================
    // Same clean professional formatter used in ExcelWriter
    // ============================================================
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