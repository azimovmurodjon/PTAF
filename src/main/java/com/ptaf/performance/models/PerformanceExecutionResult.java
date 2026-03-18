package com.ptaf.performance.models;

/**
 * Standard framework-owned result object returned by the performance engine.
 *
 * <p>This model supports:
 * <ul>
 *   <li>technical execution metrics</li>
 *   <li>human-readable reporting context</li>
 *   <li>expected failure and actual failure tracking</li>
 *   <li>run-level and scenario-level reporting</li>
 *   <li>explicit execution status for Excel/reporting/charting</li>
 *   <li>threshold visibility for business-readable comparisons</li>
 *   <li>risk scoring and recommended actions</li>
 * </ul>
 * </p>
 */
public class PerformanceExecutionResult {

    // ============================================================
    // TEST IDENTITY
    // ============================================================

    private final String testName;
    private final String testPurpose;
    private final String performanceTestType;
    private final String testGoal;

    // ============================================================
    // REQUEST DETAILS
    // ============================================================

    private final String httpMethod;
    private final String targetPath;
    private final String fullTargetUrl;
    private final String contentType;
    private final String acceptType;
    private final String authType;
    private final String payloadSourceType;
    private final String payloadSourceDetails;

    // ============================================================
    // EXECUTION PROFILE
    // ============================================================

    private final int users;
    private final int rampUpSeconds;
    private final int holdSeconds;
    private final int iterations;
    private final String executionMode;

    // ============================================================
    // THRESHOLDS
    // ============================================================

    private final double maxAllowedErrorPercent;
    private final long maxAllowedAverageResponseTimeMs;
    private final long maxAllowedP95ResponseTimeMs;

    // ============================================================
    // TIMING / METRICS
    // ============================================================

    private final long totalScenarioDurationMs;
    private final long totalSamples;
    private final long totalErrors;
    private final double errorPercent;
    private final long minResponseTimeMs;
    private final long averageResponseTimeMs;
    private final long p95ResponseTimeMs;
    private final long maxResponseTimeMs;

    // ============================================================
    // SMART REPORTING FIELDS
    // ============================================================

    private final int riskScore;
    private final String riskLevel;
    private final String thresholdBreachSummary;
    private final String recommendedAction;

    // ============================================================
    // HUMAN-READABLE INTERPRETATION
    // ============================================================

    private final String responseTimeAssessment;
    private final String errorAssessment;
    private final String stabilityAssessment;
    private final String firstFailureIndicator;
    private final String finalConclusion;

    // ============================================================
    // REPORT ARTIFACTS
    // ============================================================

    private final String dashboardPath;
    private final String jtlFilePath;
    private final String summaryFilePath;
    private final String readableSummaryFilePath;

    /**
     * Shared run-level root folder for the entire execution.
     */
    private final String runReportRootPath;

    // ============================================================
    // EXECUTION STATUS
    // ============================================================

    /**
     * High-level reporting status used for summaries, Excel coloring, and charts.
     */
    private final PerformanceExecutionStatus executionStatus;

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

    public PerformanceExecutionResult(
            String testName,
            String testPurpose,
            String performanceTestType,
            String testGoal,
            String httpMethod,
            String targetPath,
            String fullTargetUrl,
            String contentType,
            String acceptType,
            String authType,
            String payloadSourceType,
            String payloadSourceDetails,
            int users,
            int rampUpSeconds,
            int holdSeconds,
            int iterations,
            String executionMode,
            double maxAllowedErrorPercent,
            long maxAllowedAverageResponseTimeMs,
            long maxAllowedP95ResponseTimeMs,
            long totalScenarioDurationMs,
            long totalSamples,
            long totalErrors,
            double errorPercent,
            long minResponseTimeMs,
            long averageResponseTimeMs,
            long p95ResponseTimeMs,
            long maxResponseTimeMs,
            int riskScore,
            String riskLevel,
            String thresholdBreachSummary,
            String recommendedAction,
            String responseTimeAssessment,
            String errorAssessment,
            String stabilityAssessment,
            String firstFailureIndicator,
            String finalConclusion,
            String dashboardPath,
            String jtlFilePath,
            String summaryFilePath,
            String readableSummaryFilePath,
            String runReportRootPath,
            PerformanceExecutionStatus executionStatus,
            boolean executionPassed,
            boolean expectedFailureMode,
            boolean actualFailureDetected,
            String failureMessage
    ) {
        this.testName = testName;
        this.testPurpose = testPurpose;
        this.performanceTestType = performanceTestType;
        this.testGoal = testGoal;
        this.httpMethod = httpMethod;
        this.targetPath = targetPath;
        this.fullTargetUrl = fullTargetUrl;
        this.contentType = contentType;
        this.acceptType = acceptType;
        this.authType = authType;
        this.payloadSourceType = payloadSourceType;
        this.payloadSourceDetails = payloadSourceDetails;
        this.users = users;
        this.rampUpSeconds = rampUpSeconds;
        this.holdSeconds = holdSeconds;
        this.iterations = iterations;
        this.executionMode = executionMode;
        this.maxAllowedErrorPercent = maxAllowedErrorPercent;
        this.maxAllowedAverageResponseTimeMs = maxAllowedAverageResponseTimeMs;
        this.maxAllowedP95ResponseTimeMs = maxAllowedP95ResponseTimeMs;
        this.totalScenarioDurationMs = totalScenarioDurationMs;
        this.totalSamples = totalSamples;
        this.totalErrors = totalErrors;
        this.errorPercent = errorPercent;
        this.minResponseTimeMs = minResponseTimeMs;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.p95ResponseTimeMs = p95ResponseTimeMs;
        this.maxResponseTimeMs = maxResponseTimeMs;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.thresholdBreachSummary = thresholdBreachSummary;
        this.recommendedAction = recommendedAction;
        this.responseTimeAssessment = responseTimeAssessment;
        this.errorAssessment = errorAssessment;
        this.stabilityAssessment = stabilityAssessment;
        this.firstFailureIndicator = firstFailureIndicator;
        this.finalConclusion = finalConclusion;
        this.dashboardPath = dashboardPath;
        this.jtlFilePath = jtlFilePath;
        this.summaryFilePath = summaryFilePath;
        this.readableSummaryFilePath = readableSummaryFilePath;
        this.runReportRootPath = runReportRootPath;
        this.executionStatus = executionStatus;
        this.executionPassed = executionPassed;
        this.expectedFailureMode = expectedFailureMode;
        this.actualFailureDetected = actualFailureDetected;
        this.failureMessage = failureMessage;
    }

    // ============================================================
    // GETTERS - TEST IDENTITY
    // ============================================================

    public String getTestName() {
        return testName;
    }

    public String getTestPurpose() {
        return testPurpose;
    }

    public String getPerformanceTestType() {
        return performanceTestType;
    }

    public String getTestGoal() {
        return testGoal;
    }

    // ============================================================
    // GETTERS - REQUEST DETAILS
    // ============================================================

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getFullTargetUrl() {
        return fullTargetUrl;
    }

    public String getContentType() {
        return contentType;
    }

    public String getAcceptType() {
        return acceptType;
    }

    public String getAuthType() {
        return authType;
    }

    public String getPayloadSourceType() {
        return payloadSourceType;
    }

    public String getPayloadSourceDetails() {
        return payloadSourceDetails;
    }

    // ============================================================
    // GETTERS - EXECUTION PROFILE
    // ============================================================

    public int getUsers() {
        return users;
    }

    public int getRampUpSeconds() {
        return rampUpSeconds;
    }

    public int getHoldSeconds() {
        return holdSeconds;
    }

    public int getIterations() {
        return iterations;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    // ============================================================
    // GETTERS - THRESHOLDS
    // ============================================================

    public double getMaxAllowedErrorPercent() {
        return maxAllowedErrorPercent;
    }

    public long getMaxAllowedAverageResponseTimeMs() {
        return maxAllowedAverageResponseTimeMs;
    }

    public long getMaxAllowedP95ResponseTimeMs() {
        return maxAllowedP95ResponseTimeMs;
    }

    // ============================================================
    // GETTERS - TIMING / METRICS
    // ============================================================

    public long getTotalScenarioDurationMs() {
        return totalScenarioDurationMs;
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

    public long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }

    public long getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public long getP95ResponseTimeMs() {
        return p95ResponseTimeMs;
    }

    public long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    // ============================================================
    // GETTERS - SMART REPORTING FIELDS
    // ============================================================

    public int getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getThresholdBreachSummary() {
        return thresholdBreachSummary;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    // ============================================================
    // GETTERS - HUMAN INTERPRETATION
    // ============================================================

    public String getResponseTimeAssessment() {
        return responseTimeAssessment;
    }

    public String getErrorAssessment() {
        return errorAssessment;
    }

    public String getStabilityAssessment() {
        return stabilityAssessment;
    }

    public String getFirstFailureIndicator() {
        return firstFailureIndicator;
    }

    public String getFinalConclusion() {
        return finalConclusion;
    }

    // ============================================================
    // GETTERS - REPORT ARTIFACTS
    // ============================================================

    public String getDashboardPath() {
        return dashboardPath;
    }

    public String getJtlFilePath() {
        return jtlFilePath;
    }

    public String getSummaryFilePath() {
        return summaryFilePath;
    }

    public String getReadableSummaryFilePath() {
        return readableSummaryFilePath;
    }

    public String getRunReportRootPath() {
        return runReportRootPath;
    }

    // ============================================================
    // GETTERS - EXECUTION STATUS
    // ============================================================

    public PerformanceExecutionStatus getExecutionStatus() {
        return executionStatus;
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
                ", testPurpose='" + testPurpose + '\'' +
                ", performanceTestType='" + performanceTestType + '\'' +
                ", testGoal='" + testGoal + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", targetPath='" + targetPath + '\'' +
                ", fullTargetUrl='" + fullTargetUrl + '\'' +
                ", contentType='" + contentType + '\'' +
                ", acceptType='" + acceptType + '\'' +
                ", authType='" + authType + '\'' +
                ", payloadSourceType='" + payloadSourceType + '\'' +
                ", payloadSourceDetails='" + payloadSourceDetails + '\'' +
                ", users=" + users +
                ", rampUpSeconds=" + rampUpSeconds +
                ", holdSeconds=" + holdSeconds +
                ", iterations=" + iterations +
                ", executionMode='" + executionMode + '\'' +
                ", maxAllowedErrorPercent=" + maxAllowedErrorPercent +
                ", maxAllowedAverageResponseTimeMs=" + maxAllowedAverageResponseTimeMs +
                ", maxAllowedP95ResponseTimeMs=" + maxAllowedP95ResponseTimeMs +
                ", totalScenarioDurationMs=" + totalScenarioDurationMs +
                ", totalSamples=" + totalSamples +
                ", totalErrors=" + totalErrors +
                ", errorPercent=" + errorPercent +
                ", minResponseTimeMs=" + minResponseTimeMs +
                ", averageResponseTimeMs=" + averageResponseTimeMs +
                ", p95ResponseTimeMs=" + p95ResponseTimeMs +
                ", maxResponseTimeMs=" + maxResponseTimeMs +
                ", riskScore=" + riskScore +
                ", riskLevel='" + riskLevel + '\'' +
                ", thresholdBreachSummary='" + thresholdBreachSummary + '\'' +
                ", recommendedAction='" + recommendedAction + '\'' +
                ", responseTimeAssessment='" + responseTimeAssessment + '\'' +
                ", errorAssessment='" + errorAssessment + '\'' +
                ", stabilityAssessment='" + stabilityAssessment + '\'' +
                ", firstFailureIndicator='" + firstFailureIndicator + '\'' +
                ", finalConclusion='" + finalConclusion + '\'' +
                ", dashboardPath='" + dashboardPath + '\'' +
                ", jtlFilePath='" + jtlFilePath + '\'' +
                ", summaryFilePath='" + summaryFilePath + '\'' +
                ", readableSummaryFilePath='" + readableSummaryFilePath + '\'' +
                ", runReportRootPath='" + runReportRootPath + '\'' +
                ", executionStatus=" + executionStatus +
                ", executionPassed=" + executionPassed +
                ", expectedFailureMode=" + expectedFailureMode +
                ", actualFailureDetected=" + actualFailureDetected +
                ", failureMessage='" + failureMessage + '\'' +
                '}';
    }
}