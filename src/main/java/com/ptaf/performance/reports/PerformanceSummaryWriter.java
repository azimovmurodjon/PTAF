package com.ptaf.performance.reports;

import com.ptaf.performance.models.PerformanceExecutionResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes standardized summary artifacts for completed performance executions.
 *
 * <p>This class is framework-owned and should be invoked by the performance engine
 * after execution completes. It produces a human-readable summary file that can be
 * used for local review, reporting, CI artifact storage, and future integrations.</p>
 *
 * <p>Current supported output:
 * <ul>
 *   <li>TXT summary</li>
 * </ul>
 * </p>
 *
 * <p>Future expansion:
 * <ul>
 *   <li>JSON summary</li>
 *   <li>CSV summary</li>
 *   <li>aggregated historical summaries</li>
 * </ul>
 * </p>
 */
public class PerformanceSummaryWriter {

    /**
     * Writes a standardized text summary for the given execution result.
     *
     * @param executionResult completed performance execution result
     */
    public void writeTextSummary(PerformanceExecutionResult executionResult) {
        validateExecutionResult(executionResult);

        String summaryFilePath = executionResult.getSummaryFilePath();
        if (summaryFilePath == null || summaryFilePath.isBlank()) {
            throw new IllegalArgumentException("Summary file path is missing in PerformanceExecutionResult.");
        }

        Path path = Path.of(summaryFilePath);
        createParentDirectoryIfMissing(path);

        String summaryContent = buildTextSummary(executionResult);

        try {
            Files.writeString(path, summaryContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write performance summary file: " + path, e);
        }
    }

    /**
     * Builds the text summary content in a clean enterprise-readable format.
     *
     * @param executionResult completed execution result
     * @return formatted summary content
     */
    protected String buildTextSummary(PerformanceExecutionResult executionResult) {
        StringBuilder builder = new StringBuilder();

        builder.append("============================================================").append(System.lineSeparator());
        builder.append("                 PERFORMANCE EXECUTION SUMMARY              ").append(System.lineSeparator());
        builder.append("============================================================").append(System.lineSeparator());
        builder.append("Test Name               : ").append(safe(executionResult.getTestName())).append(System.lineSeparator());
        builder.append("Total Samples           : ").append(executionResult.getTotalSamples()).append(System.lineSeparator());
        builder.append("Total Errors            : ").append(executionResult.getTotalErrors()).append(System.lineSeparator());
        builder.append("Error Percent           : ").append(executionResult.getErrorPercent()).append("%").append(System.lineSeparator());
        builder.append("Average Response Time   : ").append(executionResult.getAverageResponseTimeMs()).append(" ms").append(System.lineSeparator());
        builder.append("P95 Response Time       : ").append(executionResult.getP95ResponseTimeMs()).append(" ms").append(System.lineSeparator());
        builder.append("Dashboard Path          : ").append(safe(executionResult.getDashboardPath())).append(System.lineSeparator());
        builder.append("JTL File Path           : ").append(safe(executionResult.getJtlFilePath())).append(System.lineSeparator());
        builder.append("Summary File Path       : ").append(safe(executionResult.getSummaryFilePath())).append(System.lineSeparator());
        builder.append("============================================================").append(System.lineSeparator());

        return builder.toString();
    }

    /**
     * Validates the minimal required execution result fields.
     *
     * @param executionResult execution result to validate
     */
    private void validateExecutionResult(PerformanceExecutionResult executionResult) {
        if (executionResult == null) {
            throw new IllegalArgumentException("PerformanceExecutionResult cannot be null.");
        }

        if (executionResult.getTestName() == null || executionResult.getTestName().isBlank()) {
            throw new IllegalArgumentException("PerformanceExecutionResult test name cannot be null or blank.");
        }
    }

    /**
     * Ensures the parent directory exists before writing a file.
     *
     * @param path target file path
     */
    private void createParentDirectoryIfMissing(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create parent directories for summary file: " + path, e);
        }
    }

    /**
     * Null-safe helper for text output.
     *
     * @param value incoming string
     * @return safe string value
     */
    private String safe(String value) {
        return value == null ? "N/A" : value;
    }
}