package com.ptaf.performance.models;

/**
 * Standard framework-owned result object returned by the performance engine.
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

    public PerformanceExecutionResult(String testName,
                                      long totalSamples,
                                      long totalErrors,
                                      double errorPercent,
                                      long averageResponseTimeMs,
                                      long p95ResponseTimeMs,
                                      String dashboardPath,
                                      String jtlFilePath,
                                      String summaryFilePath) {
        this.testName = testName;
        this.totalSamples = totalSamples;
        this.totalErrors = totalErrors;
        this.errorPercent = errorPercent;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.p95ResponseTimeMs = p95ResponseTimeMs;
        this.dashboardPath = dashboardPath;
        this.jtlFilePath = jtlFilePath;
        this.summaryFilePath = summaryFilePath;
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
                '}';
    }
}