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
 * <p>Responsibilities:
 * <ul>
 *   <li>Create required directories for run-level and scenario-level artifacts.</li>
 *   <li>Provide canonical Paths for known file names used by the reporting subsystems
 *       (JTL, summaries, dashboard, run index).</li>
 *   <li>Ensure parent directories of file paths exist before the files themselves are
 *       created by other parts of the system.</li>
 * </ul>
 * </p>
 *
 * <p>Behavior notes:
 * <ul>
 *   <li>Methods validate incoming Path parameters and will throw IllegalArgumentException
 *       if a null path is passed.</li>
 *   <li>Directory creation uses Files.createDirectories and will wrap IOExceptions in a
 *       RuntimeException to surface configuration / filesystem issues to the caller.</li>
 *   <li>This class is effectively stateless and can be used concurrently by multiple
 *       threads in typical execution engines.</li>
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
     * <p>Calling this method will create the directory pointed to by {@code runRootPath}
     * including any necessary parent directories. If the directory already exists, no
     * changes are made. This method does not create or modify any files within the
     * run root folder; it only guarantees the directory is present.</p>
     *
     * @param runRootPath shared run-level folder; must not be null
     * @throws IllegalArgumentException if {@code runRootPath} is null
     * @throws RuntimeException if the directory cannot be created due to an IO error
     */
    public void ensureRunRootExists(Path runRootPath) {
        // Validate input early and provide a clear error message for callers/testers.
        validatePath(runRootPath, "Run root path cannot be null.");

        // Create the directory and any missing parents if necessary.
        createDirectoryIfMissing(runRootPath);
    }

    /**
     * Ensures the scenario root folder exists.
     *
     * <p>Each performance scenario should have its own folder. This method guarantees
     * that the given scenario root directory exists on the filesystem. No files inside
     * the scenario folder are touched.</p>
     *
     * @param scenarioRootPath scenario-level folder; must not be null
     * @throws IllegalArgumentException if {@code scenarioRootPath} is null
     * @throws RuntimeException if the directory cannot be created due to an IO error
     */
    public void ensureScenarioRootExists(Path scenarioRootPath) {
        // Validate the provided scenario path.
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        // Ensure the scenario directory exists (recursively creating parents as needed).
        createDirectoryIfMissing(scenarioRootPath);
    }

    /**
     * Returns dashboard folder path inside a scenario folder and ensures it exists.
     *
     * <p>The dashboard folder is a dedicated subdirectory named "dashboard" located
     * under the provided scenario folder. This method creates that subdirectory if it
     * does not already exist and then returns the Path to it.</p>
     *
     * @param scenarioRootPath scenario-level folder; must not be null
     * @return dashboard folder path (existing or newly created)
     * @throws IllegalArgumentException if {@code scenarioRootPath} is null
     * @throws RuntimeException if the dashboard directory cannot be created due to an IO error
     */
    public Path prepareDashboardPath(Path scenarioRootPath) {
        // Validate the input path before any filesystem operations.
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        // Create a Path representing the dashboard subfolder under the scenario root.
        Path dashboardPath = scenarioRootPath.resolve("dashboard");
        // Ensure the dashboard directory exists (create it if missing).
        createDirectoryIfMissing(dashboardPath);
        // Return the (now guaranteed to exist) dashboard path to the caller.
        return dashboardPath;
    }

    /**
     * Returns JTL file path inside a scenario folder.
     *
     * <p>The JTL file (typically used for raw JMeter output) is referenced as
     * "results.jtl" inside the scenario folder. This method ensures the parent directory
     * exists but does not create the file itself.</p>
     *
     * @param scenarioRootPath scenario-level folder; must not be null
     * @return JTL file path (parent directory is guaranteed to exist)
     * @throws IllegalArgumentException if {@code scenarioRootPath} is null
     * @throws RuntimeException if required directories cannot be created due to an IO error
     */
    public Path prepareJtlFilePath(Path scenarioRootPath) {
        // Validate provided scenario path.
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        // Compose the expected JTL file path within the scenario directory.
        Path jtlFilePath = scenarioRootPath.resolve("results.jtl");
        // Ensure the parent directory of the JTL file exists (scenarioRootPath).
        createParentDirectoryIfMissing(jtlFilePath);
        return jtlFilePath;
    }

    /**
     * Returns technical summary file path inside a scenario folder.
     *
     * <p>The technical summary ("summary.txt") is intended for machine-readable or
     * detailed output. This method ensures the parent directory exists but does not
     * create or write the summary file.</p>
     *
     * @param scenarioRootPath scenario-level folder; must not be null
     * @return technical summary file path (parent directory is guaranteed to exist)
     * @throws IllegalArgumentException if {@code scenarioRootPath} is null
     * @throws RuntimeException if required directories cannot be created due to an IO error
     */
    public Path prepareSummaryFilePath(Path scenarioRootPath) {
        // Validate the incoming path.
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        // Compute the technical summary file path inside the scenario folder.
        Path summaryFilePath = scenarioRootPath.resolve("summary.txt");
        // Ensure the parent directory exists before callers attempt to create/write the file.
        createParentDirectoryIfMissing(summaryFilePath);
        return summaryFilePath;
    }

    /**
     * Returns readable summary file path inside a scenario folder.
     *
     * <p>The readable summary ("readable-summary.txt") is intended for human consumption.
     * Only the parent directory is guaranteed to exist after calling this method.</p>
     *
     * @param scenarioRootPath scenario-level folder; must not be null
     * @return readable summary file path (parent directory is guaranteed to exist)
     * @throws IllegalArgumentException if {@code scenarioRootPath} is null
     * @throws RuntimeException if required directories cannot be created due to an IO error
     */
    public Path prepareReadableSummaryFilePath(Path scenarioRootPath) {
        // Ensure the input is not null to avoid NPEs later on.
        validatePath(scenarioRootPath, "Scenario root path cannot be null.");

        // Compose the readable summary filename under the scenario folder.
        Path readableSummaryFilePath = scenarioRootPath.resolve("readable-summary.txt");
        // Create the parent directory of the file if necessary.
        createParentDirectoryIfMissing(readableSummaryFilePath);
        return readableSummaryFilePath;
    }

    /**
     * Returns run-level technical aggregate summary path.
     *
     * <p>This file aggregates per-scenario technical summaries into a run-level file
     * named "run-summary.txt". The method ensures that the run root directory exists
     * (the parent of this file) so callers may create or append to the returned path.</p>
     *
     * @param runRootPath shared run-level folder; must not be null
     * @return run summary file path (parent directory is guaranteed to exist)
     * @throws IllegalArgumentException if {@code runRootPath} is null
     * @throws RuntimeException if the parent directory cannot be created due to an IO error
     */
    public Path prepareRunSummaryFilePath(Path runRootPath) {
        // Validate the provided run root path.
        validatePath(runRootPath, "Run root path cannot be null.");

        // Resolve the canonical run summary file name inside the run root folder.
        Path runSummaryFilePath = runRootPath.resolve("run-summary.txt");
        // Ensure the parent folder exists for file operations.
        createParentDirectoryIfMissing(runSummaryFilePath);
        return runSummaryFilePath;
    }

    /**
     * Returns run-level readable aggregate summary path.
     *
     * <p>This file aggregates per-scenario readable summaries into a run-level, human-friendly
     * file named "run-readable-summary.txt". The method will ensure the run-level folder
     * exists before returning the path.</p>
     *
     * @param runRootPath shared run-level folder; must not be null
     * @return readable run summary file path (parent directory is guaranteed to exist)
     * @throws IllegalArgumentException if {@code runRootPath} is null
     * @throws RuntimeException if the parent directory cannot be created due to an IO error
     */
    public Path prepareReadableRunSummaryFilePath(Path runRootPath) {
        // Validate input first.
        validatePath(runRootPath, "Run root path cannot be null.");

        // Compute the path for the readable run-level summary file.
        Path readableRunSummaryFilePath = runRootPath.resolve("run-readable-summary.txt");
        // Make sure the parent directory exists before any file write attempts.
        createParentDirectoryIfMissing(readableRunSummaryFilePath);
        return readableRunSummaryFilePath;
    }

    /**
     * Returns run-level index file path.
     *
     * <p>The run index ("run-index.txt") can be used to store metadata or a list of
     * scenario results for the execution. This method guarantees that the directory
     * containing the file exists.</p>
     *
     * @param runRootPath shared run-level folder; must not be null
     * @return run index file path (parent directory is guaranteed to exist)
     * @throws IllegalArgumentException if {@code runRootPath} is null
     * @throws RuntimeException if the parent directory cannot be created due to an IO error
     */
    public Path prepareRunIndexFilePath(Path runRootPath) {
        // Validate the run root path to avoid unexpected NPEs later.
        validatePath(runRootPath, "Run root path cannot be null.");

        // Resolve and return the run index path, ensuring the parent directory exists.
        Path runIndexFilePath = runRootPath.resolve("run-index.txt");
        createParentDirectoryIfMissing(runIndexFilePath);
        return runIndexFilePath;
    }

    /**
     * Creates the directory if it does not already exist.
     *
     * <p>Delegates to {@link Files#createDirectories(Path)}, which will create the
     * directory and all non-existent parent directories. If the directory already
     * exists, the call has no effect. Any IOException is wrapped in a RuntimeException
     * to make callers' error handling straightforward in the execution engine.</p>
     *
     * @param directoryPath directory path to create; must not be null
     * @throws RuntimeException wrapping any IOException encountered when creating directories
     */
    private void createDirectoryIfMissing(Path directoryPath) {
        try {
            // Files.createDirectories is resilient: it creates all non-existent parents
            // and does nothing if the directory already exists.
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            // Surface filesystem problems with a descriptive runtime exception so the
            // execution engine or tests can fail fast and inspect the cause.
            throw new RuntimeException(
                    "Failed to create performance directory: " + directoryPath,
                    e
            );
        }
    }

    /**
     * Creates the parent directory of a file path if it does not already exist.
     *
     * <p>This is useful when callers intend to create or write a file and want to
     * ensure the directory exists first. If {@code filePath} has no parent (for example,
     * it is a root path or a relative single-name path), this method does nothing.</p>
     *
     * @param filePath target file path; may be null-safe insofar as callers pass non-null
     *                 (validation should have been applied by the public API)
     */
    private void createParentDirectoryIfMissing(Path filePath) {
        // Determine the parent directory for the provided file path.
        Path parent = filePath.getParent();
        // Only attempt directory creation if a parent exists; otherwise do nothing.
        if (parent != null) {
            createDirectoryIfMissing(parent);
        }
    }

    /**
     * Validates incoming path.
     *
     * <p>Performs a simple null-check and throws an IllegalArgumentException with the
     * provided message when the check fails. This centralizes the null validation so
     * public methods can provide consistent and meaningful error messages to callers
     * (useful for tests and diagnostics).</p>
     *
     * @param path path to validate
     * @param message exception message used when throwing IllegalArgumentException
     * @throws IllegalArgumentException when {@code path} is null
     */
    private void validatePath(Path path, String message) {
        if (path == null) {
            // Prefer IllegalArgumentException for incorrect arguments rather than NPEs
            // to make the contract clearer to the caller.
            throw new IllegalArgumentException(message);
        }
    }
}
