package com.ptaf.performance.utils;

import com.ptaf.performance.config.PerformanceConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Resolves standardized output paths for the Performance Engine.
 *
 * <p>This class is architect-controlled and ensures that all reports,
 * dashboards, raw result files, and future performance artifacts are
 * created in a single standardized structure.</p>
 *
 * <p>Advantages:
 * <ul>
 *   <li>Prevents hardcoded folder duplication across classes</li>
 *   <li>Improves maintainability and consistency</li>
 *   <li>Allows future expansion for CSV, JTL, JSON, and summary outputs</li>
 * </ul>
 * </p>
 */
public final class PerformancePathResolver {


    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yy_HH-mm-ss", Locale.ENGLISH);

    private PerformancePathResolver() {
        // Utility class
    }

    /**
     * Returns the base results folder configured for performance execution.
     *
     * @return base results folder path
     */
    public static Path getResultsRootPath() {
        return Paths.get(PerformanceConfigurationProperties.getResultsFolder());
    }

    /**
     * Returns the base dashboard folder configured for performance reporting.
     *
     * @return base dashboard folder path
     */
    public static Path getDashboardRootPath() {
        return Paths.get(PerformanceConfigurationProperties.getDashboardFolder());
    }

    /**
     * Builds a unique timestamp string for folder/file creation.
     *
     * @return formatted execution timestamp
     */
    public static String buildExecutionTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /**
     * Builds a unique dashboard path for a specific test run.
     *
     * @param testName logical test name
     * @return dashboard folder path
     */
    public static Path buildDashboardPath(String testName) {
        return getDashboardRootPath().resolve(buildSafeExecutionName(testName));
    }

    /**
     * Builds a unique raw results file path for a specific test run.
     * This is useful for future JTL or CSV result storage.
     *
     * @param testName logical test name
     * @return results file path without extension
     */
    public static Path buildResultFileBasePath(String testName) {
        return getResultsRootPath().resolve(buildSafeExecutionName(testName));
    }

    /**
     * Builds a unique JTL path for the performance run.
     *
     * @param testName logical test name
     * @return JTL result file path
     */
    public static Path buildJtlFilePath(String testName) {
        return getResultsRootPath().resolve(buildSafeExecutionName(testName) + ".jtl");
    }

    /**
     * Builds a unique summary file path for future aggregated reporting.
     *
     * @param testName logical test name
     * @return summary txt file path
     */
    public static Path buildSummaryFilePath(String testName) {
        return getResultsRootPath().resolve(buildSafeExecutionName(testName) + "_summary.txt");
    }

    /**
     * Creates a safe standardized execution name from the incoming test name.
     *
     * <p>Rules:
     * <ul>
     *   <li>trims spaces</li>
     *   <li>replaces invalid/special characters with underscore</li>
     *   <li>appends timestamp for uniqueness</li>
     * </ul>
     * </p>
     *
     * @param testName logical test name
     * @return sanitized execution name
     */
    public static String buildSafeExecutionName(String testName) {
        String safeName = testName == null || testName.trim().isEmpty()
                ? "performance_test"
                : testName.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");

        return safeName + "_" + buildExecutionTimestamp();
    }
}