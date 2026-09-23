package com.ptaf.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Utility class to convert Excel (XLSX) sheet data into YAML format.
 *
 * <p>
 * Usage overview:
 * - Call convertExcelToYaml(testcaseId, excelFilePath, yamlFilePath).
 * - If testcaseId is null or equals "ALL" (case-insensitive), all rows from the first sheet
 *   are written to the YAML file as a list of maps.
 * - If testcaseId is provided, the single row whose "testcase_id" cell matches the given id
 *   is located, reordered (testcase_id first) and written as a single YAML document (map).
 * </p>
 *
 * <p>
 * Notes / behaviors important for testers:
 * - The first row of the sheet is treated as the header row and is used as map keys.
 * - Data rows start from row index 1 (second row) up to sheet.getLastRowNum().
 * - Empty or missing cells are represented as empty strings ("").
 * - Numeric cells that are whole numbers are returned as Long (no decimal .0), otherwise as Double.
 * - Date-formatted numeric cells are converted to a String using Date.toString().
 * - Formula cells return the formula expression (cell.getCellFormula()) rather than evaluated result.
 * - The in-memory data snapshot is stored in a static List<Map<String,Object>> named "data".
 * </p>
 *
 * <p>
 * This class depends on Apache POI for Excel parsing and SnakeYAML for YAML output.
 * </p>
 */
public class ExcelToYaml {
    // Static list holding all rows read from the Excel sheet. Each row is a LinkedHashMap
    // to preserve column order as specified by the header row.
    private static List<Map<String, Object>> data = new ArrayList<>();

    /**
     * Main entry point to convert Excel contents to YAML.
     *
     * @param testcaseId   The testcase id to filter by. If null or "ALL" (case-insensitive),
     *                     all rows will be written. If a specific id is provided, only the first
     *                     row matching a "testcase_id" cell equal to this id will be written.
     * @param excelFilePath Full path to the source Excel (.xlsx) file.
     * @param yamlFilePath  Destination path for the generated YAML file.
     *
     *                     <p>Behavior details for testers:
     *                     - If the specified testcase ID is not found, a message is printed and no YAML file is created.
     *                     - Exceptions during file reading/writing are printed via e.printStackTrace().
     *                     </p>
     */
    public static void convertExcelToYaml(String testcaseId, String excelFilePath, String yamlFilePath) {
        // Use try-with-resources to ensure streams and workbook are closed properly.
        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // Always operate on the first sheet (index 0)
            Sheet sheet = workbook.getSheetAt(0);

            // Read the header row to determine the keys/column names for each cell
            List<String> headers = getHeaders(sheet);

            // Clear any previous data in the static list to avoid mixing runs
            data.clear(); // Clear previous data

            // Populate `data` with rows read from the sheet
            readData(sheet, headers);

            // If caller wants all rows, quote string values (no manual quotes added, but this
            // method is provided to transform/prepare values if needed) and write as a YAML list.
            if (testcaseId == null || testcaseId.equalsIgnoreCase("ALL")) {
                List<Map<String, Object>> quotedData = new ArrayList<>();
                for (Map<String, Object> row : data) {
                    quotedData.add(quoteStringValues(row));
                }
                writeDataToYaml(quotedData, yamlFilePath);
                System.out.println("All data has been written to " + yamlFilePath);
            } else {
                // Caller requested a specific testcase; find the matching row
                Map<String, Object> filteredData = getDataByTestcaseId(testcaseId);
                if (filteredData != null) {
                    // Reorder map so 'testcase_id' appears first, then quote/prepare values and write YAML
                    Map<String, Object> orderedData = reorderMap(filteredData);
                    Map<String, Object> quotedData = quoteStringValues(orderedData);
                    writeDataToYaml(quotedData, yamlFilePath);
                    System.out.println("Filtered data for '" + testcaseId + "' has been written to " + yamlFilePath);
                } else {
                    // Inform the user/tester that the testcase id was not present in the read data
                    System.out.println("Testcase ID '" + testcaseId + "' not found.");
                }
            }

        } catch (IOException e) {
            // Print stack trace for debugging file access or IO problems during test runs
            e.printStackTrace();
        }
    }

    /**
     * Extracts header names from the first row (row index 0) of the provided sheet.
     *
     * @param sheet Excel sheet object (first sheet expected to contain headers in row 0).
     * @return Ordered list of header strings. If header row is missing, an empty list is returned.
     */
    private static List<String> getHeaders(Sheet sheet) {
        List<String> headers = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            // Iterate all cells present in the header row and collect their string values
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
        }
        return headers;
    }

    /**
     * Reads all data rows from the sheet (starting at row index 1) and populates the static 'data' list.
     *
     * Each row is converted into a LinkedHashMap whose keys are the header names passed in.
     * Missing or blank cells are represented as empty strings.
     *
     * @param sheet   Sheet to read rows from.
     * @param headers Ordered list of header names corresponding to columns.
     */
    private static void readData(Sheet sheet, List<String> headers) {
        // Iterate from second row (index 1) to last row in the sheet
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue; // Skip completely blank rows

            // Use LinkedHashMap to maintain column ordering consistent with headers
            Map<String, Object> rowData = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                // If a cell is null (missing) store empty string; otherwise get typed cell value
                rowData.put(headers.get(j), cell == null ? "" : getCellValue(cell));
            }
            // Append parsed row to the static data list
            data.add(rowData);
        }
    }

    /**
     * Converts an Apache POI Cell into an appropriate Java object for YAML serialization.
     *
     * Behaviors:
     * - STRING -> String
     * - NUMERIC -> Date (toString) if date formatted; otherwise Double or Long (no decimal when whole)
     * - BOOLEAN -> Boolean
     * - FORMULA -> String containing the formula expression (no evaluation)
     * - BLANK -> empty String ("")
     * - Other types -> null
     *
     * @param cell Cell to be converted.
     * @return Java representation of the cell value suitable for YAML dumping.
     */
    private static Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // If the numeric cell represents a date, return the date as String.
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double num = cell.getNumericCellValue();
                    // If the numeric value is mathematically an integer, return as Long to avoid ".0"
                    return (num == Math.floor(num)) ? (long) num : num;
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                // Return the formula expression itself rather than an evaluated result
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return null;
        }
    }

    /**
     * Finds and returns the first row map whose "testcase_id" key equals the provided testcaseId.
     *
     * @param testcaseId The testcase identifier to search for.
     * @return Map representing the row if found; null otherwise.
     *
     * Note for testers: the comparison uses equals() and expects the cell value to be exactly the same
     * object type (commonly a String). If your Excel stores numeric IDs, they will be returned as Long/Double
     * and may not match a String testcaseId. Ensure the types align in your Excel input or pass the correct type.
     */
    public static Map<String, Object> getDataByTestcaseId(String testcaseId) {
        for (Map<String, Object> row : data) {
            if (row.containsKey("testcase_id") && testcaseId.equals(row.get("testcase_id"))) {
                return row;
            }
        }
        return null;
    }

    /**
     * Reorders the provided map so that the "testcase_id" entry (if present) comes first,
     * and all other entries follow in their original iteration order.
     *
     * This method returns a new LinkedHashMap preserving insertion order.
     *
     * @param original Original map read from Excel.
     * @return New map with "testcase_id" first (if present), followed by other entries.
     */
    private static Map<String, Object> reorderMap(Map<String, Object> original) {
        Map<String, Object> ordered = new LinkedHashMap<>();
        if (original.containsKey("testcase_id")) {
            ordered.put("testcase_id", original.get("testcase_id"));
        }
        // Add remaining entries in the same order they appeared in the original map
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            if (!entry.getKey().equals("testcase_id")) {
                ordered.put(entry.getKey(), entry.getValue());
            }
        }
        return ordered;
    }

    /**
     * Prepares a new map by processing values. Currently this function leaves values unchanged,
     * but it is provided as a single place to adjust how string values should be represented
     * (e.g., quoting, escaping) before YAML serialization.
     *
     * For the current implementation:
     * - String values remain Strings (no extra manual quotes are added here).
     * - Non-string values are passed through unchanged.
     *
     * @param input Map whose values should be processed.
     * @return New LinkedHashMap with processed values.
     */
    private static Map<String, Object> quoteStringValues(Map<String, Object> input) {
        Map<String, Object> quoted = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                // Keep string values as-is. YAML dumper will handle quoting/escaping as needed.
                quoted.put(entry.getKey(),   value); // No manual quotes
            } else {
                quoted.put(entry.getKey(), value);
            }
        }
        return quoted;
    }

    /**
     * Writes the provided data object to a YAML file using SnakeYAML.
     *
     * @param data         Data to serialize (can be a List of Maps for multiple rows, or a single Map).
     * @param yamlFilePath Destination path for YAML output.
     * @throws IOException If writing the file fails.
     *
     *                     Note: The DumperOptions are configured for block style output and pretty formatting.
     */
    private static void writeDataToYaml(Object data, String yamlFilePath) throws IOException {
        DumperOptions options = new DumperOptions();
        // Use block style for readability in YAML files
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        // Enable nicer formatting
        options.setPrettyFlow(true);

        Yaml yaml = new Yaml(options);
        // Try-with-resources to ensure the writer is closed properly
        try (FileWriter writer = new FileWriter(yamlFilePath)) {
            yaml.dump(data, writer);
        }
    }
}
