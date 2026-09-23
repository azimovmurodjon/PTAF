package com.ptaf.performance.payloads;

/**
 * Immutable payload definition that describes how to obtain request body content
 * from different supported sources.
 *
 * <p>This class encapsulates all possible metadata required to resolve a payload
 * from one of several sources:
 * <ul>
 *     <li>INLINE - raw body provided directly as a string</li>
 *     <li>YAML - a value referenced by a key in a YAML resource</li>
 *     <li>CSV - a value located in a CSV file identified by file path, row and column</li>
 *     <li>EXCEL - a value located in an Excel file (optionally with sheet) identified by file path, row and column</li>
 * </ul>
 *
 * <p>The class is immutable: all fields are final and set only via the constructor.
 * Use the provided static factory methods to create instances for common use cases.
 *
 * <p>Note for testers:
 * <ul>
 *     <li>Choose the factory method that matches the source of your payload.</li>
 *     <li>Fields that are not applicable to a given source will be null.</li>
 *     <li>The semantics of rowIdentifier (for CSV/Excel) depend on the payload resolver:
 *         it may be a row number, a row key, or any identifier understood by the resolver.</li>
 * </ul>
 *
 * @see PayloadSourceType
 */
public final class PerformancePayloadDefinition {

    /**
     * The type of source from which the payload will be resolved.
     * This indicates which of the other fields should be consulted.
     */
    private final PayloadSourceType sourceType;

    /**
     * Inline request body content. Used when sourceType == INLINE.
     * May be null for other source types.
     */
    private final String inlineBody;

    /**
     * Key to lookup a payload value inside a YAML document or resource.
     * Used when sourceType == YAML. May be null otherwise.
     */
    private final String yamlKey;

    /**
     * File system path (or classpath/resource path depending on resolver) to the data file
     * used for CSV or Excel payload resolution. May be null for INLINE and YAML.
     */
    private final String filePath;

    /**
     * Excel sheet name to be consulted when resolving payloads from Excel files.
     * May be null for CSV or when the default sheet is used.
     */
    private final String sheetName;

    /**
     * Row identifier used for locating the target row in CSV/Excel files.
     * The exact interpretation is resolver-dependent (e.g., numeric index or key-based).
     */
    private final String rowIdentifier;

    /**
     * Column name (or header) used for locating the target cell in CSV/Excel files.
     */
    private final String columnName;

    /**
     * Primary constructor that initializes all fields. Intended to be used by the
     * static factory methods; kept public to allow manual construction if needed.
     *
     * <p>All parameters are accepted as provided and assigned directly. The consumer
     * (or the payload resolver) is responsible for interpreting nulls correctly.
     *
     * @param sourceType    the payload source type (never modified after construction)
     * @param inlineBody    the inline payload content; relevant when sourceType == INLINE
     * @param yamlKey       the lookup key for YAML payloads; relevant when sourceType == YAML
     * @param filePath      the path to an external file (CSV/Excel); relevant for CSV and EXCEL
     * @param sheetName     the sheet name inside an Excel file; may be null to indicate default sheet
     * @param rowIdentifier the identifier for the row in CSV/Excel (index or key as understood by resolver)
     * @param columnName    the name/header of the column in CSV/Excel containing the payload value
     */
    public PerformancePayloadDefinition(PayloadSourceType sourceType,
                                        String inlineBody,
                                        String yamlKey,
                                        String filePath,
                                        String sheetName,
                                        String rowIdentifier,
                                        String columnName) {
        this.sourceType = sourceType;
        this.inlineBody = inlineBody;
        this.yamlKey = yamlKey;
        this.filePath = filePath;
        this.sheetName = sheetName;
        this.rowIdentifier = rowIdentifier;
        this.columnName = columnName;
    }

    /**
     * Create a payload definition that uses an inline body string.
     *
     * <p>Only inlineBody and sourceType are populated; all other fields will be null.
     *
     * @param inlineBody the raw request body to be used directly
     * @return a PerformancePayloadDefinition configured for inline payloads
     */
    public static PerformancePayloadDefinition inline(String inlineBody) {
        return new PerformancePayloadDefinition(
                PayloadSourceType.INLINE,
                inlineBody,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Create a payload definition that resolves payload from a YAML resource using a key.
     *
     * <p>Only yamlKey and sourceType are populated; other fields will be null.
     *
     * @param yamlKey the key used to lookup the payload value in a YAML document
     * @return a PerformancePayloadDefinition configured for YAML payloads
     */
    public static PerformancePayloadDefinition yaml(String yamlKey) {
        return new PerformancePayloadDefinition(
                PayloadSourceType.YAML,
                null,
                yamlKey,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Create a payload definition that resolves payload from a CSV file.
     *
     * <p>filePath identifies the CSV file, rowIdentifier locates the row, and columnName
     * identifies the column/header within that row.
     *
     * @param filePath      path to the CSV file containing payload data
     * @param rowIdentifier identifier for the desired row (resolver-specific semantics)
     * @param columnName    column name or header that contains the payload
     * @return a PerformancePayloadDefinition configured for CSV payloads
     */
    public static PerformancePayloadDefinition csv(String filePath,
                                                   String rowIdentifier,
                                                   String columnName) {
        return new PerformancePayloadDefinition(
                PayloadSourceType.CSV,
                null,
                null,
                filePath,
                null,
                rowIdentifier,
                columnName
        );
    }

    /**
     * Create a payload definition that resolves payload from an Excel file.
     *
     * <p>This overload does not specify a sheet name; the resolver should use a default
     * (e.g., the first sheet) or otherwise handle a null sheetName.
     *
     * @param filePath      path to the Excel file containing payload data
     * @param rowIdentifier identifier for the desired row (resolver-specific semantics)
     * @param columnName    column name or header that contains the payload
     * @return a PerformancePayloadDefinition configured for Excel payloads (default sheet)
     */
    public static PerformancePayloadDefinition excel(String filePath,
                                                     String rowIdentifier,
                                                     String columnName) {
        return new PerformancePayloadDefinition(
                PayloadSourceType.EXCEL,
                null,
                null,
                filePath,
                null,
                rowIdentifier,
                columnName
        );
    }

    /**
     * Create a payload definition that resolves payload from a specific sheet in an Excel file.
     *
     * @param filePath      path to the Excel file containing payload data
     * @param sheetName     explicit sheet name within the Excel workbook to consult
     * @param rowIdentifier identifier for the desired row (resolver-specific semantics)
     * @param columnName    column name or header that contains the payload
     * @return a PerformancePayloadDefinition configured for Excel payloads with an explicit sheet
     */
    public static PerformancePayloadDefinition excel(String filePath,
                                                     String sheetName,
                                                     String rowIdentifier,
                                                     String columnName) {
        return new PerformancePayloadDefinition(
                PayloadSourceType.EXCEL,
                null,
                null,
                filePath,
                sheetName,
                rowIdentifier,
                columnName
        );
    }

    /**
     * Get the configured payload source type.
     *
     * @return the PayloadSourceType indicating which source to use
     */
    public PayloadSourceType getSourceType() {
        return sourceType;
    }

    /**
     * Get the inline body content. May be null if sourceType is not INLINE.
     *
     * @return inline payload body or null
     */
    public String getInlineBody() {
        return inlineBody;
    }

    /**
     * Get the YAML lookup key. May be null if sourceType is not YAML.
     *
     * @return yamlKey or null
     */
    public String getYamlKey() {
        return yamlKey;
    }

    /**
     * Get the file path for CSV/Excel sources. May be null for INLINE or YAML.
     *
     * @return filePath or null
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Get the Excel sheet name. May be null if not specified (resolver should handle default).
     *
     * @return sheetName or null
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * Get the row identifier used for CSV/Excel lookup. Interpretation is resolver-dependent.
     *
     * @return rowIdentifier or null
     */
    public String getRowIdentifier() {
        return rowIdentifier;
    }

    /**
     * Get the column name/header used for CSV/Excel lookup.
     *
     * @return columnName or null
     */
    public String getColumnName() {
        return columnName;
    }
}
