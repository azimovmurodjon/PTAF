package com.ptaf.performance.reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Centralized report manager for all performance execution artifacts.
 *
 * <p>This class is framework-owned and supports the current PTAF reporting model:
 * <ul>
 *   <li>one shared run-level root folder per execution</li>
 *   <li>one scenario-level subfolder per performance scenario</li>
 *   <li>technical and readable summaries inside each scenario folder</li>
 *   <li>aggregate run-level files inside the run root folder</li>
 * </ul>
 * </p>
 *
 * <p>This manager should be used by execution engines and not directly
 * by tester-authored code.</p>
 */
public class PerformanceReportManager {

    /**
     * Ensures the run root folder exists.
     *
     * @param runRootPath shared run-level folder
     */
    public void ensureRunRootExists(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");
        createDirectoryIfMissing(runRootPath);
    }

    /**
     * Ensures the scenario root folder exists.
     *
     * @param scenarioRootPath scenario-level folder
     */
    public void ensureScenarioRootExists(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");
        createDirectoryIfMissing(scenarioRootPath);
    }

    /**
     * Returns dashboard folder path inside a scenario folder and ensures it exists.
     *
     * @param scenarioRootPath scenario-level folder
     * @return dashboard folder path
     */
    public Path prepareDashboardPath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        Path dashboardPath = scenarioRootPath.resolve("dashboard");
        createDirectoryIfMissing(dashboardPath);
        return dashboardPath;
    }

    /**
     * Returns JTL file path inside a scenario folder.
     *
     * @param scenarioRootPath scenario-level folder
     * @return JTL file path
     */
    public Path prepareJtlFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        Path jtlFilePath = scenarioRootPath.resolve("results.jtl");
        createParentDirectoryIfMissing(jtlFilePath);
        return jtlFilePath;
    }

    /**
     * Returns technical summary file path inside a scenario folder.
     *
     * @param scenarioRootPath scenario-level folder
     * @return technical summary file path
     */
    public Path prepareSummaryFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        Path summaryFilePath = scenarioRootPath.resolve("summary.txt");
        createParentDirectoryIfMissing(summaryFilePath);
        return summaryFilePath;
    }

    /**
     * Returns readable summary file path inside a scenario folder.
     *
     * @param scenarioRootPath scenario-level folder
     * @return readable summary file path
     */
    public Path prepareReadableSummaryFilePath(Path scenarioRootPath) {
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        Path readableSummaryFilePath = scenarioRootPath.resolve("readable-summary.txt");
        createParentDirectoryIfMissing(readableSummaryFilePath);
        return readableSummaryFilePath;
    }

    /**
     * Returns run-level technical aggregate summary path.
     *
     * @param runRootPath shared run-level folder
     * @return run summary file path
     */
    public Path prepareRunSummaryFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");

        Path runSummaryFilePath = runRootPath.resolve("run-summary.txt");
        createParentDirectoryIfMissing(runSummaryFilePath);
        return runSummaryFilePath;
    }

    /**
     * Returns run-level readable aggregate summary path.
     *
     * @param runRootPath shared run-level folder
     * @return readable run summary file path
     */
    public Path prepareReadableRunSummaryFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");

        Path readableRunSummaryFilePath = runRootPath.resolve("run-readable-summary.txt");
        createParentDirectoryIfMissing(readableRunSummaryFilePath);
        return readableRunSummaryFilePath;
    }

    /**
     * Returns run-level index file path.
     *
     * @param runRootPath shared run-level folder
     * @return run index file path
     */
    public Path prepareRunIndexFilePath(Path runRootPath) {
        validatePath(runRootPath, "Run root path cannot be null.");

        Path runIndexFilePath = runRootPath.resolve("run-index.txt");
        createParentDirectoryIfMissing(runIndexFilePath);
        return runIndexFilePath;
    }

    /**
     * Creates the directory if it does not already exist.
     *
     * @param directoryPath directory path to create
     */
    private void createDirectoryIfMissing(Path directoryPath) {
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to create performance directory: " + directoryPath,
                    e
            );
        }
    }

    /**
     * Creates the parent directory of a file path if it does not already exist.
     *
     * @param filePath target file path
     */
    private void createParentDirectoryIfMissing(Path filePath) {
        Path parent = filePath.getParent();
        if (parent != null) {
            createDirectoryIfMissing(parent);
        }
    }

    /**
     * Validates incoming path.
     *
     * @param path path to validate
     * @param message exception message
     */
    private void validatePath(Path path, String message) {
        if (path == null) {
            throw new IllegalArgumentException(message);
        }
    }
}