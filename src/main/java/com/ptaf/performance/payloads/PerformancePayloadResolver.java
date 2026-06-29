package com.ptaf.performance.payloads;

import com.ptaf.utils.ExcelReader;
import com.ptaf.utils.YamlReader;

/**
 * Central resolver for performance request payloads.
 *
 * <p>
 * This utility class provides a single entry point to obtain payload bodies used in performance
 * tests. Payloads can originate from different sources: inline, YAML, CSV, or Excel.
 * The resolver delegates retrieval to the appropriate reader based on the provided
 * {@link PerformancePayloadDefinition}.
 * </p>
 *
 * <p>
 * Usage notes for testers:
 * - Call {@link #resolve(PerformancePayloadDefinition)} when you already have a constructed
 *   {@code PerformancePayloadDefinition} instance.
 * - Convenience wrappers are provided for common cases:
 *   {@link #resolveYaml(String)}, {@link #resolveCsv(String, String, String)}, and
 *   {@link #resolveExcel(String, String, String)}.
 * - Validation is performed on incoming parameters and a clear {@link IllegalArgumentException}
 *   will be thrown for invalid or missing inputs.
 * </p>
 */
public final class PerformancePayloadResolver {

    /**
     * Utility class - prevent instantiation.
     */
    private PerformancePayloadResolver() {
    }

    /**
     * Resolve the payload body for the provided {@link PerformancePayloadDefinition}.
     *
     * <p>
     * Behavior:
     * - If {@code definition} is null, returns null (no payload).
     * - If the {@code sourceType} inside the definition is null, an
     *   {@link IllegalArgumentException} is thrown.
     * - Delegates to the appropriate private resolver method depending on the source type.
     * </p>
     *
     * @param definition the payload definition containing source type and related metadata
     * @return the resolved payload as a String, or null if {@code definition} is null
     * @throws IllegalArgumentException if the definition's source type is null or if required
     *                                  metadata for the selected source is missing
     */
    public static String resolve(PerformancePayloadDefinition definition) {
        if (definition == null) {
            return null;
        }

        if (definition.getSourceType() == null) {
            throw new IllegalArgumentException("Payload source type cannot be null.");
        }

        // Dispatch based on configured source type
        return switch (definition.getSourceType()) {
            case INLINE -> resolveInline(definition);
            case YAML -> resolveYaml(definition);
            case CSV -> resolveCsv(definition);
            case EXCEL -> resolveExcel(definition);
        };
    }

    /**
     * Public wrapper for YAML payload resolution by key.
     *
     * <p>
     * Convenience method used by higher-level builders (e.g. PerformanceRequestBuilder).
     * Validates the provided YAML key before resolving it via a {@link PerformancePayloadDefinition}.
     * </p>
     *
     * @param yamlKey the key in the YAML store identifying the payload
     * @return the payload value as a String
     * @throws IllegalArgumentException if {@code yamlKey} is null or blank, or if the YAML value
     *                                  cannot be found during resolution
     */
    public static String resolveYaml(String yamlKey) {
        if (yamlKey == null || yamlKey.isBlank()) {
            throw new IllegalArgumentException("YAML payload key cannot be null or blank.");
        }

        // Build a definition representing a YAML-sourced payload and delegate
        PerformancePayloadDefinition definition = PerformancePayloadDefinition.yaml(yamlKey);
        return resolveYaml(definition);
    }

    /**
     * Public wrapper for CSV payload resolution.
     *
     * <p>
     * Convenience method used by higher-level builders (e.g. PerformanceRequestBuilder).
     * Validates file path, row identifier, and column name before constructing a
     * {@link PerformancePayloadDefinition} and resolving it.
     * </p>
     *
     * @param filePath      path to the CSV file
     * @param rowIdentifier identifier used to locate the correct row inside the CSV
     * @param columnName    name of the column to retrieve the value from
     * @return the requested CSV cell value as a String
     * @throws IllegalArgumentException if any parameter is null or blank
     */
    public static String resolveCsv(String filePath, String rowIdentifier, String columnName) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("CSV payload file path cannot be null or blank.");
        }

        if (rowIdentifier == null || rowIdentifier.isBlank()) {
            throw new IllegalArgumentException("CSV row identifier cannot be null or blank.");
        }

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("CSV column name cannot be null or blank.");
        }

        // Build CSV definition and delegate to the CSV resolver
        PerformancePayloadDefinition definition =
                PerformancePayloadDefinition.csv(filePath, rowIdentifier, columnName);

        return resolveCsv(definition);
    }

    /**
     * Public wrapper for Excel payload resolution.
     *
     * <p>
     * Convenience method used by higher-level builders (e.g. PerformanceRequestBuilder).
     * Validates file path, row identifier, and column name before constructing a
     * {@link PerformancePayloadDefinition} and resolving it.
     * </p>
     *
     * @param filePath      path to the Excel file
     * @param rowIdentifier identifier used to locate the correct row inside the sheet
     * @param columnName    name of the column to retrieve the value from
     * @return the requested Excel cell value as a String
     * @throws IllegalArgumentException if any parameter is null or blank
     */
    public static String resolveExcel(String filePath, String rowIdentifier, String columnName) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("Excel payload file path cannot be null or blank.");
        }

        if (rowIdentifier == null || rowIdentifier.isBlank()) {
            throw new IllegalArgumentException("Excel row identifier cannot be null or blank.");
        }

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException("Excel column name cannot be null or blank.");
        }

        // Build Excel definition and delegate to the Excel resolver
        PerformancePayloadDefinition definition =
                PerformancePayloadDefinition.excel(filePath, rowIdentifier, columnName);

        return resolveExcel(definition);
    }

    /**
     * Resolve an inline payload body.
     *
     * <p>
     * For inline payloads the definition already contains the payload body and no further
     * lookup is required.
     * </p>
     *
     * @param definition the payload definition containing the inline body
     * @return the inline payload string (may be null if the definition's inline body is null)
     */
    private static String resolveInline(PerformancePayloadDefinition definition) {
        // Directly return the provided inline body
        return definition.getInlineBody();
    }

    /**
     * Resolve a YAML payload from the YAML store.
     *
     * <p>
     * This method validates the presence of a YAML key in the definition, reads the value using
     * {@link YamlReader#get(String)}, and returns the string representation of the stored value.
     * A descriptive {@link IllegalArgumentException} is thrown if the key is missing or the value
     * cannot be found.
     * </p>
     *
     * @param definition the payload definition containing the YAML key
     * @return the payload value as a String
     * @throws IllegalArgumentException if the YAML key is null/blank or the key cannot be resolved
     */
    private static String resolveYaml(PerformancePayloadDefinition definition) {
        if (definition.getYamlKey() == null || definition.getYamlKey().isBlank()) {
            throw new IllegalArgumentException("YAML payload key cannot be null or blank.");
        }

        // Retrieve the object value from the YAML reader; could be String, number, map, etc.
        Object value = YamlReader.get(definition.getYamlKey());
        if (value == null) {
            // Provide a clear message to help testers locate missing YAML keys
            throw new IllegalArgumentException("YAML payload value not found for key: " + definition.getYamlKey());
        }

        // Convert any returned object to its string representation
        return String.valueOf(value);
    }

    /**
     * Resolve a CSV payload cell.
     *
     * <p>
     * Delegates to {@link CsvPayloadReader#getData(String, String, String)} which encapsulates
     * CSV parsing and lookup logic.
     * </p>
     *
     * @param definition the payload definition containing file path, row identifier and column name
     * @return the CSV cell value as a String
     */
    private static String resolveCsv(PerformancePayloadDefinition definition) {
        // Delegate to the CSV reader utility - it is responsible for validation and lookup
        return CsvPayloadReader.getData(
                definition.getFilePath(),
                definition.getRowIdentifier(),
                definition.getColumnName()
        );
    }

    /**
     * Resolve an Excel payload cell.
     *
     * <p>
     * Validates required fields on the definition and then delegates to {@link ExcelReader#getData}
     * to read the Excel file and return the requested cell value.
     * </p>
     *
     * @param definition the payload definition containing file path, row identifier and column name
     * @return the Excel cell value as a String
     * @throws IllegalArgumentException if any required metadata in the definition is null or blank
     */
    private static String resolveExcel(PerformancePayloadDefinition definition) {
        if (definition.getFilePath() == null || definition.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Excel payload file path cannot be null or blank.");
        }

        if (definition.getRowIdentifier() == null || definition.getRowIdentifier().isBlank()) {
            throw new IllegalArgumentException("Excel row identifier cannot be null or blank.");
        }

        if (definition.getColumnName() == null || definition.getColumnName().isBlank()) {
            throw new IllegalArgumentException("Excel column name cannot be null or blank.");
        }

        // Delegate to ExcelReader which handles file I/O and cell lookup
        return ExcelReader.getData(
                definition.getFilePath(),
                definition.getRowIdentifier(),
                definition.getColumnName()
        );
    }
}
