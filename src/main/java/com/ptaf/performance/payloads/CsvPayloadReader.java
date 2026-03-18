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
 * <p>Resolution order:
 * <ul>
 *   <li>classpath resource</li>
 *   <li>direct filesystem path</li>
 *   <li>src/test/resources fallback</li>
 * </ul>
 * </p>
 *
 * <p>Assumptions:
 * first row is header,
 * first column is row identifier.</p>
 */
public final class CsvPayloadReader {

    private CsvPayloadReader() {
    }

    public static String getData(String filePathOrClasspathResource,
                                 String rowIdentifier,
                                 String columnName) {
        if (filePathOrClasspathResource == null || filePathOrClasspathResource.isBlank()) {
            throw new IllegalArgumentException("CSV payload file path cannot be null or blank.");
        }

        if (rowIdentifier == null || rowIdentifier.isBlank()) {
            throw new IllegalArgumentException("CSV row identifier cannot be null or blank.");
        }

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("CSV column name cannot be null or blank.");
        }

        try (BufferedReader reader = openReader(filePathOrClasspathResource)) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalStateException("CSV payload file is empty: " + filePathOrClasspathResource);
            }

            String[] rawHeaders = splitCsvLine(headerLine);
            Map<String, Integer> headerIndexMap = new LinkedHashMap<>();

            for (int i = 0; i < rawHeaders.length; i++) {
                String normalizedHeader = normalizeHeader(rawHeaders[i]);
                headerIndexMap.put(normalizedHeader.toLowerCase(), i);
            }

            String normalizedRequestedColumn = normalizeHeader(columnName).toLowerCase();

            if (!headerIndexMap.containsKey(normalizedRequestedColumn)) {
                String availableHeaders = headerIndexMap.keySet()
                        .stream()
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException(
                        "CSV column not found: " + columnName + ". Available columns: [" + availableHeaders + "]"
                );
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] values = splitCsvLine(line);
                if (values.length == 0) {
                    continue;
                }

                String currentRowId = normalizeValue(values[0]);
                if (currentRowId.equalsIgnoreCase(rowIdentifier.trim())) {
                    int columnIndex = headerIndexMap.get(normalizedRequestedColumn);
                    if (columnIndex >= values.length) {
                        return null;
                    }
                    return normalizeValue(values[columnIndex]);
                }
            }

            throw new IllegalArgumentException(
                    "CSV row identifier not found. File: " + filePathOrClasspathResource + ", Row: " + rowIdentifier
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to read CSV payload. File: " + filePathOrClasspathResource
                            + ", Row: " + rowIdentifier
                            + ", Column: " + columnName,
                    e
            );
        }
    }

    private static BufferedReader openReader(String filePathOrClasspathResource) throws Exception {
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(filePathOrClasspathResource);

        if (inputStream != null) {
            return new BufferedReader(new InputStreamReader(inputStream));
        }

        String normalizedClasspathPath = filePathOrClasspathResource.startsWith("/")
                ? filePathOrClasspathResource.substring(1)
                : filePathOrClasspathResource;

        inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(normalizedClasspathPath);

        if (inputStream != null) {
            return new BufferedReader(new InputStreamReader(inputStream));
        }

        Path directPath = Paths.get(filePathOrClasspathResource);
        if (Files.exists(directPath)) {
            return Files.newBufferedReader(directPath);
        }

        Path testResourcesPath = Paths.get("src", "test", "resources", normalizedClasspathPath);
        if (Files.exists(testResourcesPath)) {
            return Files.newBufferedReader(testResourcesPath);
        }

        throw new IllegalStateException(
                "CSV payload file not found. Checked classpath and filesystem. " +
                        "Provided path: " + filePathOrClasspathResource +
                        ", Filesystem fallback: " + testResourcesPath.toAbsolutePath()
        );
    }

    /**
     * Simple CSV split.
     *
     * <p>Current implementation assumes payload values do not require
     * advanced CSV parsing beyond simple comma splitting.</p>
     */
    private static String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private static String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        if (!cleaned.isEmpty() && cleaned.charAt(0) == '\uFEFF') {
            cleaned = cleaned.substring(1);
        }

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return cleaned;
    }

    private static String normalizeValue(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        if (cleaned.startsWith("\"") && cleaned.endsWith("\"") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned.replace("\"\"", "\"");
    }
}