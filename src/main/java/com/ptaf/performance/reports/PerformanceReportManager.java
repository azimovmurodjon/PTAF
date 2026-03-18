package com.ptaf.performance.reports;

import com.ptaf.performance.utils.PerformancePathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Centralized report manager for all performance execution artifacts.
 *
 * <p>This class is framework-owned and ensures that performance-related
 * folders and output files are handled in a consistent enterprise-grade way.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create and validate report/output directories</li>
 *   <li>Provide standardized dashboard paths</li>
 *   <li>Provide standardized raw result file paths</li>
 *   <li>Provide standardized summary file paths</li>
 * </ul>
 * </p>
 *
 * <p>This manager should be used by execution engines and not directly
 * by tester-authored code.</p>
 */
public class PerformanceReportManager {

    /**
     * Ensures that all base performance reporting folders exist before execution.
     */
    public void ensureReportFoldersExist() {
        createDirectoryIfMissing(PerformancePathResolver.getResultsRootPath());
        createDirectoryIfMissing(PerformancePathResolver.getDashboardRootPath());
    }

    /**
     * Builds and creates a unique dashboard folder path for a given test execution.
     *
     * @param testName logical performance test name
     * @return created dashboard folder path
     */
    public Path prepareDashboardPath(String testName) {
        Path dashboardPath = PerformancePathResolver.buildDashboardPath(testName);
        createDirectoryIfMissing(dashboardPath);
        return dashboardPath;
    }

    /**
     * Builds a standardized JTL file path for the test execution.
     *
     * <p>The file itself is not created here. Only the parent folder is ensured.</p>
     *
     * @param testName logical performance test name
     * @return JTL file path
     */
    public Path prepareJtlFilePath(String testName) {
        Path jtlFilePath = PerformancePathResolver.buildJtlFilePath(testName);
        createParentDirectoryIfMissing(jtlFilePath);
        return jtlFilePath;
    }

    /**
     * Builds a standardized summary file path for the test execution.
     *
     * <p>The file itself is not created here. Only the parent folder is ensured.</p>
     *
     * @param testName logical performance test name
     * @return summary file path
     */
    public Path prepareSummaryFilePath(String testName) {
        Path summaryFilePath = PerformancePathResolver.buildSummaryFilePath(testName);
        createParentDirectoryIfMissing(summaryFilePath);
        return summaryFilePath;
    }

    /**
     * Builds a standardized raw results base path for future extensions
     * such as CSV, JSON, or custom export files.
     *
     * @param testName logical performance test name
     * @return raw result base path
     */
    public Path prepareResultFileBasePath(String testName) {
        Path resultBasePath = PerformancePathResolver.buildResultFileBasePath(testName);
        createParentDirectoryIfMissing(resultBasePath);
        return resultBasePath;
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
}