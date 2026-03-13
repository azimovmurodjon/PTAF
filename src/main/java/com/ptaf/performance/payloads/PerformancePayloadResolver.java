package com.ptaf.performance.payloads;

import com.ptaf.utils.ExcelReader;
import com.ptaf.utils.YamlReader;

/**
 * Central resolver for performance request payloads.
 */
public final class PerformancePayloadResolver {

    private PerformancePayloadResolver() {
    }

    public static String resolve(PerformancePayloadDefinition definition) {
        if (definition == null) {
            return null;
        }

        if (definition.getSourceType() == null) {
            throw new IllegalArgumentException("Payload source type cannot be null.");
        }

        return switch (definition.getSourceType()) {
            case INLINE -> resolveInline(definition);
            case YAML -> resolveYaml(definition);
            case CSV -> resolveCsv(definition);
            case EXCEL -> resolveExcel(definition);
        };
    }

    private static String resolveInline(PerformancePayloadDefinition definition) {
        return definition.getInlineBody();
    }

    private static String resolveYaml(PerformancePayloadDefinition definition) {
        if (definition.getYamlKey() == null || definition.getYamlKey().isBlank()) {
            throw new IllegalArgumentException("YAML payload key cannot be null or blank.");
        }

        Object value = YamlReader.get(definition.getYamlKey());
        if (value == null) {
            throw new IllegalArgumentException("YAML payload value not found for key: " + definition.getYamlKey());
        }

        return String.valueOf(value);
    }

    private static String resolveCsv(PerformancePayloadDefinition definition) {
        return CsvPayloadReader.getData(
                definition.getFilePath(),
                definition.getRowIdentifier(),
                definition.getColumnName()
        );
    }

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

        return ExcelReader.getData(
                definition.getFilePath(),
                definition.getRowIdentifier(),
                definition.getColumnName()
        );
    }
}