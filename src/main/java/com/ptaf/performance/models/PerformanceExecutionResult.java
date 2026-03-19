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
 *
 * <p>Reporting-safe design goals:
 * <ul>
 *   <li>keep constructor compatibility</li>
 *   <li>do not change execution behavior</li>
 *   <li>normalize null/blank values for Excel/reporting usage</li>
 *   <li>provide reusable helper methods for business-facing reporting</li>
 * </ul>
 * </p>
 */
public class PerformanceExecutionResult {

    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";
    private static final String UNKNOWN = "Unknown";
    private static final String NOT_AVAILABLE = "N/A";

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
        this.testName = normalizeText(testName);
        this.testPurpose = normalizeText(testPurpose);
        this.performanceTestType = normalizeText(performanceTestType);
        this.testGoal = normalizeText(testGoal);

        this.httpMethod = normalizeText(httpMethod);
        this.targetPath = normalizeText(targetPath);
        this.fullTargetUrl = normalizeText(fullTargetUrl);
        this.contentType = normalizeText(contentType);
        this.acceptType = normalizeText(acceptType);
        this.authType = normalizeText(authType);
        this.payloadSourceType = normalizeText(payloadSourceType);
        this.payloadSourceDetails = normalizeText(payloadSourceDetails);

        this.users = sanitizeInt(users);
        this.rampUpSeconds = sanitizeInt(rampUpSeconds);
        this.holdSeconds = sanitizeInt(holdSeconds);
        this.iterations = sanitizeInt(iterations);
        this.executionMode = normalizeText(executionMode);

        this.maxAllowedErrorPercent = sanitizeDouble(maxAllowedErrorPercent);
        this.maxAllowedAverageResponseTimeMs = sanitizeLong(maxAllowedAverageResponseTimeMs);
        this.maxAllowedP95ResponseTimeMs = sanitizeLong(maxAllowedP95ResponseTimeMs);

        this.totalScenarioDurationMs = sanitizeLong(totalScenarioDurationMs);
        this.totalSamples = sanitizeLong(totalSamples);
        this.totalErrors = sanitizeLong(totalErrors);
        this.errorPercent = sanitizeDouble(errorPercent);
        this.minResponseTimeMs = sanitizeLong(minResponseTimeMs);
        this.averageResponseTimeMs = sanitizeLong(averageResponseTimeMs);
        this.p95ResponseTimeMs = sanitizeLong(p95ResponseTimeMs);
        this.maxResponseTimeMs = sanitizeLong(maxResponseTimeMs);

        this.riskScore = sanitizeInt(riskScore);
        this.riskLevel = normalizeRiskLevel(riskLevel);
        this.thresholdBreachSummary = normalizeThresholdBreachSummary(thresholdBreachSummary);
        this.recommendedAction = normalizeText(recommendedAction);

        this.responseTimeAssessment = normalizeText(responseTimeAssessment);
        this.errorAssessment = normalizeText(errorAssessment);
        this.stabilityAssessment = normalizeText(stabilityAssessment);
        this.firstFailureIndicator = normalizeText(firstFailureIndicator);
        this.finalConclusion = normalizeText(finalConclusion);

        this.dashboardPath = normalizeText(dashboardPath);
        this.jtlFilePath = normalizeText(jtlFilePath);
        this.summaryFilePath = normalizeText(summaryFilePath);
        this.readableSummaryFilePath = normalizeText(readableSummaryFilePath);
        this.runReportRootPath = normalizeText(runReportRootPath);

        this.executionStatus = executionStatus;
        this.executionPassed = executionPassed;
        this.expectedFailureMode = expectedFailureMode;
        this.actualFailureDetected = actualFailureDetected;
        this.failureMessage = normalizeText(failureMessage);
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

    // ============================================================
    // REPORTING / BUSINESS HELPERS
    // ============================================================

    public boolean hasThresholdBreach() {
        return thresholdBreachSummary != null
                && !thresholdBreachSummary.isBlank()
                && !NO_THRESHOLD_BREACHES.equalsIgnoreCase(thresholdBreachSummary.trim());
    }

    public boolean hasErrors() {
        return totalErrors > 0 || errorPercent > 0.0;
    }

    public boolean hasHighOrCriticalRisk() {
        return isHighRisk() || isCriticalRisk() || riskScore >= 51;
    }

    public boolean isLowRisk() {
        return "Low".equalsIgnoreCase(riskLevel);
    }

    public boolean isMediumRisk() {
        return "Medium".equalsIgnoreCase(riskLevel);
    }

    public boolean isHighRisk() {
        return "High".equalsIgnoreCase(riskLevel);
    }

    public boolean isCriticalRisk() {
        return "Critical".equalsIgnoreCase(riskLevel);
    }

    public boolean isAttentionNeeded() {
        return executionStatus == PerformanceExecutionStatus.FAIL
                || executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED
                || hasThresholdBreach()
                || hasErrors()
                || hasHighOrCriticalRisk();
    }

    public String getBusinessOutcomeLabel() {
        if (executionStatus == null) {
            return UNKNOWN;
        }

        return switch (executionStatus) {
            case PASS -> "Passed";
            case FAIL -> "Failed";
            case EXPECTED_FAIL_CONFIRMED -> "Expected Fail Confirmed";
            case EXPECTED_FAIL_NOT_TRIGGERED -> "Expected Fail Not Triggered";
            case SKIPPED -> "Skipped";
        };
    }

    public String getAttentionCategory() {
        if (!isAttentionNeeded()) {
            return "No Issue Detected";
        }

        if (executionStatus == PerformanceExecutionStatus.FAIL) {
            return "Execution Failure";
        }

        if (executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED) {
            return "Expected Failure Not Triggered";
        }

        if (hasThresholdBreach()) {
            return "Threshold Breach";
        }

        if (hasErrors()) {
            return "Errors Present";
        }

        if (hasHighOrCriticalRisk()) {
            return "High / Critical Risk";
        }

        return "Attention Needed";
    }

    public String getPrimaryBusinessConcern() {
        if (executionStatus == PerformanceExecutionStatus.FAIL) {
            return "Scenario failed unexpectedly.";
        }

        if (executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED) {
            return "Expected failure did not trigger.";
        }

        if (hasThresholdBreach()) {
            return thresholdBreachSummary;
        }

        if (hasErrors()) {
            return "Request errors were detected.";
        }

        if (hasHighOrCriticalRisk()) {
            return "Elevated scenario risk detected.";
        }

        return "No major business concern detected.";
    }

    public String getSafeFailureMessage() {
        return isBlank(failureMessage) ? "" : failureMessage;
    }

    public String getSafeTestName() {
        return isBlank(testName) ? NOT_AVAILABLE : testName;
    }

    public String getSafeTargetPath() {
        return isBlank(targetPath) ? NOT_AVAILABLE : targetPath;
    }

    public String getSafeRecommendedAction() {
        return isBlank(recommendedAction) ? NOT_AVAILABLE : recommendedAction;
    }

    public String getSafeFinalConclusion() {
        return isBlank(finalConclusion) ? NOT_AVAILABLE : finalConclusion;
    }

    public String getSafeResponseTimeAssessment() {
        return isBlank(responseTimeAssessment) ? NOT_AVAILABLE : responseTimeAssessment;
    }

    public String getSafeErrorAssessment() {
        return isBlank(errorAssessment) ? NOT_AVAILABLE : errorAssessment;
    }

    public String getSafeStabilityAssessment() {
        return isBlank(stabilityAssessment) ? NOT_AVAILABLE : stabilityAssessment;
    }

    public String getSafeFirstFailureIndicator() {
        return isBlank(firstFailureIndicator) ? NOT_AVAILABLE : firstFailureIndicator;
    }

    // ============================================================
    // INTERNAL NORMALIZATION HELPERS
    // ============================================================

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        return cleaned
                .replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s{2,}", " ");
    }

    private static String normalizeRiskLevel(String value) {
        String cleaned = normalizeText(value);

        if (cleaned.isEmpty()) {
            return UNKNOWN;
        }

        if ("low".equalsIgnoreCase(cleaned)) {
            return "Low";
        }
        if ("medium".equalsIgnoreCase(cleaned)) {
            return "Medium";
        }
        if ("high".equalsIgnoreCase(cleaned)) {
            return "High";
        }
        if ("critical".equalsIgnoreCase(cleaned)) {
            return "Critical";
        }

        return cleaned;
    }

    private static String normalizeThresholdBreachSummary(String value) {
        String cleaned = normalizeText(value);
        return cleaned.isEmpty() ? NO_THRESHOLD_BREACHES : cleaned;
    }

    private static int sanitizeInt(int value) {
        return Math.max(value, 0);
    }

    private static long sanitizeLong(long value) {
        return Math.max(value, 0L);
    }

    private static double sanitizeDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            return 0.0;
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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