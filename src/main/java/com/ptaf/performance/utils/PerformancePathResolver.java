package com.ptaf.performance.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Resolves standardized output paths for the Performance Engine.
 *
 * <p>This version is aligned with the current PTAF reporting structure:
 * <ul>
 *   <li>one run-level root folder per execution</li>
 *   <li>one scenario-level subfolder per scenario</li>
 *   <li>fixed artifact names within each scenario folder</li>
 *   <li>aggregate run-level files inside the run root folder</li>
 * </ul>
 * </p>
 */
public final class PerformancePathResolver {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yy_HH-mm-ss", Locale.ENGLISH);

    private static final String DEFAULT_REPORTS_BASE_DIR = "test-output-performance-reports";

    private PerformancePathResolver() {
        // Utility class
    }

    /**
     * Builds a timestamp string used for the shared run folder.
     *
     * @return formatted execution timestamp
     */
    public static String buildExecutionTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /**
     * Returns the default base reports directory.
     *
     * @return base reports root path
     */
    public static Path getReportsBaseDirectory() {
        return Paths.get(DEFAULT_REPORTS_BASE_DIR);
    }

    /**
     * Builds the shared run root folder path.
     *
     * @param runFolderName generated run folder name
     * @return run root path
     */
    public static Path buildRunRootPath(String runFolderName) {
        validateText(runFolderName, "Run folder name cannot be null or blank.");
        return getReportsBaseDirectory().resolve(runFolderName);
    }

    /**
     * Builds a scenario folder path inside the run root folder.
     *
     * @param runRootPath run root path
     * @param scenarioSequence scenario sequence number
     * @param testName logical test name
     * @return scenario root path
     */
    public static Path buildScenarioRootPath(Path runRootPath,
                                             int scenarioSequence,
                                             String testName) {
        validatePath(runRootPath, "Run root path cannot be null.");

        String safeScenarioName = String.format(
                "%02d_%s",
                scenarioSequence,
                buildSafeScenarioName(testName)
        );

        return runRootPath.resolve(safeScenarioName);
    }

    /**
     * Builds dashboard folder path inside a scenario folder.
     *
     * @param scenarioRootPath scenario root path
     * @return dashboard path
     */
    public static Path buildDashboardPath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        return scenarioRootPath.resolve("dashboard");
    }

    /**
     * Builds JTL file path inside a scenario folder.
     *
     * @param scenarioRootPath scenario root path
     * @return JTL file path
     */
    public static Path buildJtlFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        return scenarioRootPath.resolve("results.jtl");
    }

    /**
     * Builds technical summary file path inside a scenario folder.
     *
     * @param scenarioRootPath scenario root path
     * @return summary file path
     */
    public static Path buildSummaryFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        return scenarioRootPath.resolve("summary.txt");
    }

    /**
     * Builds readable summary file path inside a scenario folder.
     *
     * @param scenarioRootPath scenario root path
     * @return readable summary file path
     */
    public static Path buildReadableSummaryFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        return scenarioRootPath.resolve("readable-summary.txt");
    }

    /**
     * Builds run-level technical summary file path.
     *
     * @param runRootPath run root path
     * @return run summary path
     */
    public static Path buildRunSummaryFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");
        return runRootPath.resolve("run-summary.txt");
    }

    /**
     * Builds run-level readable summary file path.
     *
     * @param runRootPath run root path
     * @return readable run summary path
     */
    public static Path buildReadableRunSummaryFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");
        return runRootPath.resolve("run-readable-summary.txt");
    }

    /**
     * Builds run-level index file path.
     *
     * @param runRootPath run root path
     * @return run index path
     */
    public static Path buildRunIndexFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");
        return runRootPath.resolve("run-index.txt");
    }

    /**
     * Creates a safe scenario folder name from the test name.
     *
     * @param testName logical test name
     * @return sanitized scenario name
     */
    public static String buildSafeScenarioName(String testName) {
        String safeName = testName == null || testName.trim().isEmpty()
                ? "performance_test"
                : testName.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");

        return safeName;
    }

    private static void validatePath(Path path, String message) {
        if (path == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}