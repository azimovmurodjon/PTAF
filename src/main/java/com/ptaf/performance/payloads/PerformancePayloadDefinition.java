package com.ptaf.performance.payloads;

/**
 * Immutable payload definition used to resolve request body content
 * from different supported sources.
 */
public final class PerformancePayloadDefinition {

    private final PayloadSourceType sourceType;
    private final String inlineBody;
    private final String yamlKey;
    private final String filePath;
    private final String sheetName;
    private final String rowIdentifier;
    private final String columnName;

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

    public PayloadSourceType getSourceType() {
        return sourceType;
    }

    public String getInlineBody() {
        return inlineBody;
    }

    public String getYamlKey() {
        return yamlKey;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getRowIdentifier() {
        return rowIdentifier;
    }

    public String getColumnName() {
        return columnName;
    }
}