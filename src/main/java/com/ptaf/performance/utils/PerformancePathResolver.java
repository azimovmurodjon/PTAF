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

    /**
     * Timestamp formatter used to create a human readable, sortable run folder name.
     *
     * Format example: "29-Jun-26_14-05-30"
     *
     * Note: LocalDateTime.now() is used (system default timezone) when creating timestamps.
     */
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yy_HH-mm-ss", Locale.ENGLISH);

    /**
     * Default base directory where all performance reports are stored.
     *
     * The directory is relative to the current working directory. Tests and build
     * systems should be aware of this location when collecting artifacts.
     */
    private static final String DEFAULT_REPORTS_BASE_DIR = "test-output-performance-reports";

    /**
     * Private constructor for utility class.
     *
     * This class only provides static helpers and should not be instantiated.
     */
    private PerformancePathResolver() {
        // Utility class
    }

    /**
     * Builds a timestamp string used for the shared run folder.
     *
     * <p>The returned value is suitable to be used as part of a directory name
     * that represents a single execution (run) of the performance tests.</p>
     *
     * @return formatted execution timestamp using the TIMESTAMP_FORMAT and the system clock
     */
    public static String buildExecutionTimestamp() {
        // Capture the current date/time and format it with the configured pattern.
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    /**
     * Returns the default base reports directory as a Path.
     *
     * <p>This is the root folder under which per-run folders are created. The
     * path is relative to the JVM working directory unless an absolute path
     * is supplied elsewhere in the environment.</p>
     *
     * @return base reports root path
     */
    public static Path getReportsBaseDirectory() {
        // Convert the configured base directory name into a Path object.
        return Paths.get(DEFAULT_REPORTS_BASE_DIR);
    }

    /**
     * Builds the shared run root folder path by resolving the given run folder
     * name against the base reports directory.
     *
     * <p>Example result: test-output-performance-reports/29-Jun-26_14-05-30</p>
     *
     * @param runFolderName generated run folder name (must not be null or blank)
     * @return run root path
     * @throws IllegalArgumentException if runFolderName is null/blank
     */
    public static Path buildRunRootPath(String runFolderName) {
        // Validate the provided folder name to avoid invalid or empty run folders.
        validateText(runFolderName, "Run folder name cannot be null or blank.");
        // Resolve the run folder name against the base reports directory.
        return getReportsBaseDirectory().resolve(runFolderName);
    }

    /**
     * Builds a scenario folder path inside the run root folder.
     *
     * <p>The scenario folder name is constructed as a two-digit sequence prefix
     * followed by an underscore and a sanitized test name. Example: "01_performance_test".</p>
     *
     * @param runRootPath run root path (must not be null)
     * @param scenarioSequence scenario sequence number (used for ordering)
     * @param testName logical test name (will be sanitized to a filesystem-safe name)
     * @return scenario root path
     * @throws IllegalArgumentException if runRootPath is null
     */
    public static Path buildScenarioRootPath(Path runRootPath,
                                             int scenarioSequence,
                                             String testName) {
        // Ensure the run root path is provided.
        validatePath(runRootPath, "Run root path cannot be null.");

        // Build a safe scenario name with a two-digit sequence prefix.
        String safeScenarioName = String.format(
                "%02d_%s",
                scenarioSequence,
                buildSafeScenarioName(testName)
        );

        // Resolve the scenario folder inside the run root.
        return runRootPath.resolve(safeScenarioName);
    }

    /**
     * Builds dashboard folder path inside a scenario folder.
     *
     * <p>Dashboard contents (e.g. HTML dashboards or aggregated graphs) are expected
     * to be placed inside this subfolder.</p>
     *
     * @param scenarioRootPath scenario root path (must not be null)
     * @return dashboard path
     * @throws IllegalArgumentException if scenarioRootPath is null
     */
    public static Path buildDashboardPath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        // Use a fixed subfolder name "dashboard" for dashboard artifacts.
        return scenarioRootPath.resolve("dashboard");
    }

    /**
     * Builds JTL file path inside a scenario folder.
     *
     * <p>JTL files contain the raw results produced by the load generator and are
     * named "results.jtl" within each scenario directory.</p>
     *
     * @param scenarioRootPath scenario root path (must not be null)
     * @return JTL file path
     * @throws IllegalArgumentException if scenarioRootPath is null
     */
    public static Path buildJtlFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        // Use a fixed filename "results.jtl" for raw JMeter-like results.
        return scenarioRootPath.resolve("results.jtl");
    }

    /**
     * Builds technical summary file path inside a scenario folder.
     *
     * <p>The technical summary is a compact machine-oriented summary file named "summary.txt".</p>
     *
     * @param scenarioRootPath scenario root path (must not be null)
     * @return summary file path
     * @throws IllegalArgumentException if scenarioRootPath is null
     */
    public static Path buildSummaryFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        return scenarioRootPath.resolve("summary.txt");
    }

    /**
     * Builds readable summary file path inside a scenario folder.
     *
     * <p>The readable summary is intended for human consumption and is named "readable-summary.txt".</p>
     *
     * @param scenarioRootPath scenario root path (must not be null)
     * @return readable summary file path
     * @throws IllegalArgumentException if scenarioRootPath is null
     */
    public static Path buildReadableSummaryFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        return scenarioRootPath.resolve("readable-summary.txt");
    }

    /**
     * Builds run-level technical summary file path.
     *
     * <p>This file aggregates technical summaries across scenarios and is named "run-summary.txt".</p>
     *
     * @param runRootPath run root path (must not be null)
     * @return run summary path
     * @throws IllegalArgumentException if runRootPath is null
     */
    public static Path buildRunSummaryFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");
        return runRootPath.resolve("run-summary.txt");
    }

    /**
     * Builds run-level readable summary file path.
     *
     * <p>A human readable aggregation across scenarios, named "run-readable-summary.txt".</p>
     *
     * @param runRootPath run root path (must not be null)
     * @return readable run summary path
     * @throws IllegalArgumentException if runRootPath is null
     */
    public static Path buildReadableRunSummaryFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");
        return runRootPath.resolve("run-readable-summary.txt");
    }

    /**
     * Builds run-level index file path.
     *
     * <p>The run index is a simple listing of run artifacts and is named "run-index.txt".</p>
     *
     * @param runRootPath run root path (must not be null)
     * @return run index path
     * @throws IllegalArgumentException if runRootPath is null
     */
    public static Path buildRunIndexFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");
        return runRootPath.resolve("run-index.txt");
    }

    /**
     * Creates a safe scenario folder name from the test name.
     *
     * <p>Sanitization rules:
     * <ul>
     *   <li>If testName is null or empty -> returns "performance_test"</li>
     *   <li>Trims whitespace from ends</li>
     *   <li>Replaces any character that is not a letter, digit, dot, underscore or hyphen with an underscore</li>
     *   <li>Collapses multiple consecutive underscores into a single underscore</li>
     * </ul>
     * </p>
     *
     * <p>This ensures the resulting folder name is safe for most file systems and easy to read.</p>
     *
     * @param testName logical test name (may be null)
     * @return sanitized scenario name suitable for use as a directory name
     */
    public static String buildSafeScenarioName(String testName) {
        // If the provided test name is null or blank, use a sensible default.
        String safeName = testName == null || testName.trim().isEmpty()
                ? "performance_test"
                : testName.trim()
                // Replace disallowed characters (anything other than letters, digits, dot, underscore or hyphen)
                // with an underscore to avoid filesystem issues.
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                // Collapse multiple underscores into a single underscore for readability.
                .replaceAll("_+", "_");

        return safeName;
    }

    /**
     * Validates that a Path parameter is not null.
     *
     * @param path Path to validate
     * @param message message used for the IllegalArgumentException if validation fails
     * @throws IllegalArgumentException if path is null
     */
    private static void validatePath(Path path, String message) {
        if (path == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that a text parameter is not null or blank.
     *
     * @param value text value to validate
     * @param message message used for the IllegalArgumentException if validation fails
     * @throws IllegalArgumentException if value is null or blank
     */
    private static void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }
}
