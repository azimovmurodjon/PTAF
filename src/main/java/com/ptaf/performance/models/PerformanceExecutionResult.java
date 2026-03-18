package com.ptaf.performance.models;

/**
 * Standard framework-owned result object returned by the performance engine.
 *
 * <p>This model supports both positive and expected-failure executions.</p>
 */
public class PerformanceExecutionResult {

    private final String testName;
    private final long totalSamples;
    private final long totalErrors;
    private final double errorPercent;
    private final long averageResponseTimeMs;
    private final long p95ResponseTimeMs;
    private final String dashboardPath;
    private final String jtlFilePath;
    private final String summaryFilePath;

    /**
     * Shared run-level root folder for the entire execution.
     */
    private final String runReportRootPath;

    /**
     * True when assertions passed for the scenario execution.
     */
    private final boolean executionPassed;

    /**
     * True when scenario was intentionally executed in expected-failure mode.
     */
    private final boolean expectedFailureMode;

    /**
     * True when the execution actually failed assertions or execution validation.
     */
    private final boolean actualFailureDetected;

    /**
     * Framework-captured error/failure message if available.
     */
    private final String failureMessage;

    public PerformanceExecutionResult(String testName,
                                      long totalSamples,
                                      long totalErrors,
                                      double errorPercent,
                                      long averageResponseTimeMs,
                                      long p95ResponseTimeMs,
                                      String dashboardPath,
                                      String jtlFilePath,
                                      String summaryFilePath,
                                      String runReportRootPath,
                                      boolean executionPassed,
                                      boolean expectedFailureMode,
                                      boolean actualFailureDetected,
                                      String failureMessage) {
        this.testName = testName;
        this.totalSamples = totalSamples;
        this.totalErrors = totalErrors;
        this.errorPercent = errorPercent;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.p95ResponseTimeMs = p95ResponseTimeMs;
        this.dashboardPath = dashboardPath;
        this.jtlFilePath = jtlFilePath;
        this.summaryFilePath = summaryFilePath;
        this.runReportRootPath = runReportRootPath;
        this.executionPassed = executionPassed;
        this.expectedFailureMode = expectedFailureMode;
        this.actualFailureDetected = actualFailureDetected;
        this.failureMessage = failureMessage;
    }

    public String getTestName() {
        return testName;
    }

    public long getTotalSamples() {
        return totalSamples;
    }

    public long getTotalErrors() {
        return totalErrors;
    }

    public double getErrorPercent() {
        return errorPercent;
    }

    public long getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public long getP95ResponseTimeMs() {
        return p95ResponseTimeMs;
    }

    public String getDashboardPath() {
        return dashboardPath;
    }

    public String getJtlFilePath() {
        return jtlFilePath;
    }

    public String getSummaryFilePath() {
        return summaryFilePath;
    }

    public String getRunReportRootPath() {
        return runReportRootPath;
    }

    public boolean isExecutionPassed() {
        return executionPassed;
    }

    public boolean isExpectedFailureMode() {
        return expectedFailureMode;
    }

    public boolean isActualFailureDetected() {
        return actualFailureDetected;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * Compatibility helper for older code that may still use this naming.
     */
    public double getErrorPercentage() {
        return getErrorPercent();
    }

    @Override
    public String toString() {
        return "PerformanceExecutionResult{" +
                "testName='" + testName + '\'' +
                ", totalSamples=" + totalSamples +
                ", totalErrors=" + totalErrors +
                ", errorPercent=" + errorPercent +
                ", averageResponseTimeMs=" + averageResponseTimeMs +
                ", p95ResponseTimeMs=" + p95ResponseTimeMs +
                ", dashboardPath='" + dashboardPath + '\'' +
                ", jtlFilePath='" + jtlFilePath + '\'' +
                ", summaryFilePath='" + summaryFilePath + '\'' +
                ", runReportRootPath='" + runReportRootPath + '\'' +
                ", executionPassed=" + executionPassed +
                ", expectedFailureMode=" + expectedFailureMode +
                ", actualFailureDetected=" + actualFailureDetected +
                ", failureMessage='" + failureMessage + '\'' +
                '}';
    }
}