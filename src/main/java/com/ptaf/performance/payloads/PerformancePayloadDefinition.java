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