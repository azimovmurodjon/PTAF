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

    // Constants used to provide normalized, human-friendly defaults for reporting.
    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";
    private static final String UNKNOWN = "Unknown";
    private static final String NOT_AVAILABLE = "N/A";

    // ============================================================
    // TEST IDENTITY
    // ============================================================

    /**
     * Short, unique display name for the test/scenario.
     */
    private final String testName;

    /**
     * Human-friendly description of why the test exists / what it validates.
     */
    private final String testPurpose;

    /**
     * Type of performance test, e.g. "Load", "Stress", "Soak".
     */
    private final String performanceTestType;

    /**
     * Business-level goal for the test run (SLO, SLA or other objective).
     */
    private final String testGoal;

    // ============================================================
    // REQUEST DETAILS
    // ============================================================

    /**
     * HTTP method used for requests (GET, POST, etc.).
     */
    private final String httpMethod;

    /**
     * Target service path or endpoint (path portion only).
     */
    private final String targetPath;

    /**
     * Full target URL (including protocol and host).
     */
    private final String fullTargetUrl;

    /**
     * Content-Type header value used when sending payloads.
     */
    private final String contentType;

    /**
     * Accept header value used when receiving responses.
     */
    private final String acceptType;

    /**
     * Authentication type used for the request (e.g., Bearer, Basic).
     */
    private final String authType;

    /**
     * Payload source type (e.g., inline, file, generated).
     */
    private final String payloadSourceType;

    /**
     * Details about payload source (file path, generator name, etc.).
     */
    private final String payloadSourceDetails;

    // ============================================================
    // EXECUTION PROFILE
    // ============================================================

    /**
     * Number of virtual users used for the scenario.
     */
    private final int users;

    /**
     * Ramp-up period in seconds for user injection.
     */
    private final int rampUpSeconds;

    /**
     * Duration in seconds to hold the target load.
     */
    private final int holdSeconds;

    /**
     * Number of iterations executed (if applicable).
     */
    private final int iterations;

    /**
     * Execution mode name (e.g., "throughput", "fixed", "iterations").
     */
    private final String executionMode;

    // ============================================================
    // THRESHOLDS
    // ============================================================

    /**
     * Configured maximum allowed error percent (0..100).
     */
    private final double maxAllowedErrorPercent;

    /**
     * Configured max allowed average response time in milliseconds.
     */
    private final long maxAllowedAverageResponseTimeMs;

    /**
     * Configured max allowed 95th percentile response time in milliseconds.
     */
    private final long maxAllowedP95ResponseTimeMs;

    // ============================================================
    // TIMING / METRICS
    // ============================================================

    /**
     * Total elapsed scenario duration in milliseconds.
     */
    private final long totalScenarioDurationMs;

    /**
     * Total successful + failed samples observed.
     */
    private final long totalSamples;

    /**
     * Total observed errors during the scenario execution.
     */
    private final long totalErrors;

    /**
     * Error percentage observed for the scenario.
     */
    private final double errorPercent;

    /**
     * Minimum observed response time (ms).
     */
    private final long minResponseTimeMs;

    /**
     * Average observed response time (ms).
     */
    private final long averageResponseTimeMs;

    /**
     * 95th percentile observed response time (ms).
     */
    private final long p95ResponseTimeMs;

    /**
     * Maximum observed response time (ms).
     */
    private final long maxResponseTimeMs;

    // ============================================================
    // SMART REPORTING FIELDS
    // ============================================================

    /**
     * Numeric risk score aggregated for this scenario (0..100 typical).
     */
    private final int riskScore;

    /**
     * Risk level text normalized to Low/Medium/High/Critical or original.
     */
    private final String riskLevel;

    /**
     * Human-readable summary of any threshold breaches; normalized to a default message when none.
     */
    private final String thresholdBreachSummary;

    /**
     * Recommended action text for business/operators based on results.
     */
    private final String recommendedAction;

    // ============================================================
    // HUMAN-READABLE INTERPRETATION
    // ============================================================

    /**
     * Human-readable assessment of response time characteristics.
     */
    private final String responseTimeAssessment;

    /**
     * Human-readable assessment of error characteristics.
     */
    private final String errorAssessment;

    /**
     * Human-readable assessment of stability (flapping, ramp issues, etc.).
     */
    private final String stabilityAssessment;

    /**
     * Indicator text for the first observed failure (if any).
     */
    private final String firstFailureIndicator;

    /**
     * Final short conclusion text summarizing overall scenario health.
     */
    private final String finalConclusion;

    // ============================================================
    // REPORT ARTIFACTS
    // ============================================================

    /**
     * Path to the generated dashboard artifact for the run.
     */
    private final String dashboardPath;

    /**
     * Path to the raw JTL/test sample file (if generated).
     */
    private final String jtlFilePath;

    /**
     * Path to a machine-readable summary artifact.
     */
    private final String summaryFilePath;

    /**
     * Path to a human-readable summary artifact (for stakeholders).
     */
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

    /**
     * Primary constructor. All inputs are normalized or sanitized to ensure downstream
     * reporting consumers (Excel, dashboards) do not receive nulls or unsafe values.
     *
     * <p>Important: constructor intentionally performs normalization rather than altering
     * input semantics so consumers receive consistent, reporting-friendly values.</p>
     *
     * @param testName scenario short name
     * @param testPurpose scenario purpose/description
     * @param performanceTestType type of test (Load/Stress/etc.)
     * @param testGoal business goal for the test
     * @param httpMethod HTTP method used
     * @param targetPath request path
     * @param fullTargetUrl full request URL
     * @param contentType request content-type
     * @param acceptType request accept header value
     * @param authType authentication type used
     * @param payloadSourceType payload type (file, inline, generator)
     * @param payloadSourceDetails payload details (file path, generator name)
     * @param users number of virtual users
     * @param rampUpSeconds ramp-up seconds for the scenario
     * @param holdSeconds hold duration seconds
     * @param iterations iterations executed
     * @param executionMode execution mode name
     * @param maxAllowedErrorPercent configured max allowed errors percent
     * @param maxAllowedAverageResponseTimeMs configured max average RT (ms)
     * @param maxAllowedP95ResponseTimeMs configured max p95 RT (ms)
     * @param totalScenarioDurationMs observed total scenario duration (ms)
     * @param totalSamples total observed samples
     * @param totalErrors total observed errors
     * @param errorPercent observed error percent
     * @param minResponseTimeMs observed min response time (ms)
     * @param averageResponseTimeMs observed avg response time (ms)
     * @param p95ResponseTimeMs observed p95 response time (ms)
     * @param maxResponseTimeMs observed max response time (ms)
     * @param riskScore aggregated numeric risk score
     * @param riskLevel textual risk level
     * @param thresholdBreachSummary textual summary of threshold breaches
     * @param recommendedAction recommended action for stakeholders
     * @param responseTimeAssessment interpretation of response times
     * @param errorAssessment interpretation of errors
     * @param stabilityAssessment interpretation of stability
     * @param firstFailureIndicator first failure indicator text
     * @param finalConclusion final human-friendly conclusion
     * @param dashboardPath dashboard artifact path
     * @param jtlFilePath raw JTL/sample file path
     * @param summaryFilePath machine-readable summary path
     * @param readableSummaryFilePath human-readable summary path
     * @param runReportRootPath shared run-level root folder
     * @param executionStatus overall execution status enum
     * @param executionPassed true when execution assertions passed
     * @param expectedFailureMode true when run was expected to fail
     * @param actualFailureDetected true when actual failure occurred
     * @param failureMessage captured failure message (if any)
     */
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
        // Normalize all textual fields to be safe for reporting (no nulls, trimmed, single-spaced).
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

        // Sanitize numeric profile values to ensure non-negative integers.
        this.users = sanitizeInt(users);
        this.rampUpSeconds = sanitizeInt(rampUpSeconds);
        this.holdSeconds = sanitizeInt(holdSeconds);
        this.iterations = sanitizeInt(iterations);
        this.executionMode = normalizeText(executionMode);

        // Sanitize threshold numeric values.
        this.maxAllowedErrorPercent = sanitizeDouble(maxAllowedErrorPercent);
        this.maxAllowedAverageResponseTimeMs = sanitizeLong(maxAllowedAverageResponseTimeMs);
        this.maxAllowedP95ResponseTimeMs = sanitizeLong(maxAllowedP95ResponseTimeMs);

        // Sanitize observed metrics to avoid negative or invalid values.
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
        // Provide a default "no breaches" message when none configured/present.
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

        // Execution status values are kept as provided (enum may be null).
        this.executionStatus = executionStatus;
        this.executionPassed = executionPassed;
        this.expectedFailureMode = expectedFailureMode;
        this.actualFailureDetected = actualFailureDetected;
        this.failureMessage = normalizeText(failureMessage);
    }

    // ============================================================
    // GETTERS - TEST IDENTITY
    // ============================================================

    /**
     * @return normalized test name (never null; empty string if not provided)
     */
    public String getTestName() {
        return testName;
    }

    /**
     * @return normalized test purpose/description
     */
    public String getTestPurpose() {
        return testPurpose;
    }

    /**
     * @return performance test type
     */
    public String getPerformanceTestType() {
        return performanceTestType;
    }

    /**
     * @return business test goal
     */
    public String getTestGoal() {
        return testGoal;
    }

    // ============================================================
    // GETTERS - REQUEST DETAILS
    // ============================================================

    /**
     * @return HTTP method used in the scenario
     */
    public String getHttpMethod() {
        return httpMethod;
    }

    /**
     * @return normalized target path (path component)
     */
    public String getTargetPath() {
        return targetPath;
    }

    /**
     * @return normalized full target URL
     */
    public String getFullTargetUrl() {
        return fullTargetUrl;
    }

    /**
     * @return content type used in requests
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return accept type used in requests
     */
    public String getAcceptType() {
        return acceptType;
    }

    /**
     * @return authentication type used
     */
    public String getAuthType() {
        return authType;
    }

    /**
     * @return payload source type
     */
    public String getPayloadSourceType() {
        return payloadSourceType;
    }

    /**
     * @return payload source details
     */
    public String getPayloadSourceDetails() {
        return payloadSourceDetails;
    }

    // ============================================================
    // GETTERS - EXECUTION PROFILE
    // ============================================================

    /**
     * @return configured number of virtual users
     */
    public int getUsers() {
        return users;
    }

    /**
     * @return configured ramp-up seconds
     */
    public int getRampUpSeconds() {
        return rampUpSeconds;
    }

    /**
     * @return configured hold seconds
     */
    public int getHoldSeconds() {
        return holdSeconds;
    }

    /**
     * @return configured iterations
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * @return execution mode name
     */
    public String getExecutionMode() {
        return executionMode;
    }

    // ============================================================
    // GETTERS - THRESHOLDS
    // ============================================================

    /**
     * @return maximum allowed error percent configured for the scenario
     */
    public double getMaxAllowedErrorPercent() {
        return maxAllowedErrorPercent;
    }

    /**
     * @return maximum allowed average response time (ms)
     */
    public long getMaxAllowedAverageResponseTimeMs() {
        return maxAllowedAverageResponseTimeMs;
    }

    /**
     * @return maximum allowed 95th percentile response time (ms)
     */
    public long getMaxAllowedP95ResponseTimeMs() {
        return maxAllowedP95ResponseTimeMs;
    }

    // ============================================================
    // GETTERS - TIMING / METRICS
    // ============================================================

    /**
     * @return total scenario duration in milliseconds
     */
    public long getTotalScenarioDurationMs() {
        return totalScenarioDurationMs;
    }

    /**
     * @return total observed samples
     */
    public long getTotalSamples() {
        return totalSamples;
    }

    /**
     * @return total observed errors
     */
    public long getTotalErrors() {
        return totalErrors;
    }

    /**
     * @return observed error percent
     */
    public double getErrorPercent() {
        return errorPercent;
    }

    /**
     * @return minimum observed response time (ms)
     */
    public long getMinResponseTimeMs() {
        return minResponseTimeMs;
    }

    /**
     * @return average observed response time (ms)
     */
    public long getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    /**
     * @return 95th percentile observed response time (ms)
     */
    public long getP95ResponseTimeMs() {
        return p95ResponseTimeMs;
    }

    /**
     * @return maximum observed response time (ms)
     */
    public long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    // ============================================================
    // GETTERS - SMART REPORTING FIELDS
    // ============================================================

    /**
     * @return numeric risk score
     */
    public int getRiskScore() {
        return riskScore;
    }

    /**
     * @return normalized risk level string
     */
    public String getRiskLevel() {
        return riskLevel;
    }

    /**
     * @return threshold breach summary text (never blank; default when none)
     */
    public String getThresholdBreachSummary() {
        return thresholdBreachSummary;
    }

    /**
     * @return recommended action for stakeholders
     */
    public String getRecommendedAction() {
        return recommendedAction;
    }

    // ============================================================
    // GETTERS - HUMAN INTERPRETATION
    // ============================================================

    /**
     * @return response time assessment text
     */
    public String getResponseTimeAssessment() {
        return responseTimeAssessment;
    }

    /**
     * @return error assessment text
     */
    public String getErrorAssessment() {
        return errorAssessment;
    }

    /**
     * @return stability assessment text
     */
    public String getStabilityAssessment() {
        return stabilityAssessment;
    }

    /**
     * @return first failure indicator text
     */
    public String getFirstFailureIndicator() {
        return firstFailureIndicator;
    }

    /**
     * @return final conclusion text
     */
    public String getFinalConclusion() {
        return finalConclusion;
    }

    // ============================================================
    // GETTERS - REPORT ARTIFACTS
    // ============================================================

    /**
     * @return dashboard file path
     */
    public String getDashboardPath() {
        return dashboardPath;
    }

    /**
     * @return jtl/sample file path
     */
    public String getJtlFilePath() {
        return jtlFilePath;
    }

    /**
     * @return machine-readable summary file path
     */
    public String getSummaryFilePath() {
        return summaryFilePath;
    }

    /**
     * @return human-readable summary file path
     */
    public String getReadableSummaryFilePath() {
        return readableSummaryFilePath;
    }

    /**
     * @return run-level root path for the report artifacts
     */
    public String getRunReportRootPath() {
        return runReportRootPath;
    }

    // ============================================================
    // GETTERS - EXECUTION STATUS
    // ============================================================

    /**
     * @return execution status enum (may be null if not set)
     */
    public PerformanceExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    /**
     * @return true if the run passed assertions
     */
    public boolean isExecutionPassed() {
        return executionPassed;
    }

    /**
     * @return true if this run was deliberately run expecting failure
     */
    public boolean isExpectedFailureMode() {
        return expectedFailureMode;
    }

    /**
     * @return true if an actual failure was detected during execution
     */
    public boolean isActualFailureDetected() {
        return actualFailureDetected;
    }

    /**
     * @return framework-captured failure message (normalized)
     */
    public String getFailureMessage() {
        return failureMessage;
    }

    /**
     * Compatibility helper for older code that may still use this naming.
     *
     * @return error percent (same as getErrorPercent)
     */
    public double getErrorPercentage() {
        return getErrorPercent();
    }

    // ============================================================
    // REPORTING / BUSINESS HELPERS
    // ============================================================

    /**
     * @return true when thresholdBreachSummary contains a real breach message
     */
    public boolean hasThresholdBreach() {
        return thresholdBreachSummary != null
                && !thresholdBreachSummary.isBlank()
                && !NO_THRESHOLD_BREACHES.equalsIgnoreCase(thresholdBreachSummary.trim());
    }

    /**
     * @return true when at least one error was observed or error percentage > 0
     */
    public boolean hasErrors() {
        return totalErrors > 0 || errorPercent > 0.0;
    }

    /**
     * @return true when scenario risk is considered High or Critical.
     *         Also treats riskScore >= 51 as elevated risk for backward compatibility.
     */
    public boolean hasHighOrCriticalRisk() {
        return isHighRisk() || isCriticalRisk() || riskScore >= 51;
    }

    /**
     * @return true if risk level text equals "Low" (case-insensitive)
     */
    public boolean isLowRisk() {
        return "Low".equalsIgnoreCase(riskLevel);
    }

    /**
     * @return true if risk level text equals "Medium" (case-insensitive)
     */
    public boolean isMediumRisk() {
        return "Medium".equalsIgnoreCase(riskLevel);
    }

    /**
     * @return true if risk level text equals "High" (case-insensitive)
     */
    public boolean isHighRisk() {
        return "High".equalsIgnoreCase(riskLevel);
    }

    /**
     * @return true if risk level text equals "Critical" (case-insensitive)
     */
    public boolean isCriticalRisk() {
        return "Critical".equalsIgnoreCase(riskLevel);
    }

    /**
     * Aggregates multiple signals to determine if the scenario requires attention.
     *
     * @return true when the execution status indicates failure, an expected-fail did not trigger,
     *         a threshold breach exists, errors were observed, or risk is elevated.
     */
    public boolean isAttentionNeeded() {
        return executionStatus == PerformanceExecutionStatus.FAIL
                || executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED
                || hasThresholdBreach()
                || hasErrors()
                || hasHighOrCriticalRisk();
    }

    /**
     * Produces a concise business-friendly label describing the outcome.
     *
     * @return one of "Passed", "Failed", "Expected Fail Confirmed", "Expected Fail Not Triggered",
     *         "Skipped", or "Unknown"
     */
    public String getBusinessOutcomeLabel() {
        if (executionStatus == null) {
            return UNKNOWN;
        }

        // Map enum values to human-friendly labels.
        return switch (executionStatus) {
            case PASS -> "Passed";
            case FAIL -> "Failed";
            case EXPECTED_FAIL_CONFIRMED -> "Expected Fail Confirmed";
            case EXPECTED_FAIL_NOT_TRIGGERED -> "Expected Fail Not Triggered";
            case SKIPPED -> "Skipped";
        };
    }

    /**
     * Returns a high-level attention category to help stakeholders triage the result.
     *
     * @return a short category string such as "Execution Failure", "Threshold Breach", or
     *         "No Issue Detected".
     */
    public String getAttentionCategory() {
        if (!isAttentionNeeded()) {
            return "No Issue Detected";
        }

        // Prioritize execution failures over other categories.
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

        // Fallback category when no specific match is found.
        return "Attention Needed";
    }

    /**
     * Provides a primary business-facing concern that summarizes the most critical issue.
     *
     * @return user-friendly primary concern message
     */
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

    /**
     * @return failure message sanitized for safe display (empty string if none)
     */
    public String getSafeFailureMessage() {
        return isBlank(failureMessage) ? "" : failureMessage;
    }

    /**
     * @return test name safe for Excel/CSV display (N/A when not provided)
     */
    public String getSafeTestName() {
        return isBlank(testName) ? NOT_AVAILABLE : testName;
    }

    /**
     * @return target path safe for Excel/CSV (N/A when not provided)
     */
    public String getSafeTargetPath() {
        return isBlank(targetPath) ? NOT_AVAILABLE : targetPath;
    }

    /**
     * @return recommended action safe for Excel/CSV (N/A when not provided)
     */
    public String getSafeRecommendedAction() {
        return isBlank(recommendedAction) ? NOT_AVAILABLE : recommendedAction;
    }

    /**
     * @return final conclusion safe for Excel/CSV (N/A when not provided)
     */
    public String getSafeFinalConclusion() {
        return isBlank(finalConclusion) ? NOT_AVAILABLE : finalConclusion;
    }

    /**
     * @return response time assessment safe for Excel/CSV (N/A when not provided)
     */
    public String getSafeResponseTimeAssessment() {
        return isBlank(responseTimeAssessment) ? NOT_AVAILABLE : responseTimeAssessment;
    }

    /**
     * @return error assessment safe for Excel/CSV (N/A when not provided)
     */
    public String getSafeErrorAssessment() {
        return isBlank(errorAssessment) ? NOT_AVAILABLE : errorAssessment;
    }

    /**
     * @return stability assessment safe for Excel/CSV (N/A when not provided)
     */
    public String getSafeStabilityAssessment() {
        return isBlank(stabilityAssessment) ? NOT_AVAILABLE : stabilityAssessment;
    }

    /**
     * @return first failure indicator safe for Excel/CSV (N/A when not provided)
     */
    public String getSafeFirstFailureIndicator() {
        return isBlank(firstFailureIndicator) ? NOT_AVAILABLE : firstFailureIndicator;
    }

    // ============================================================
    // INTERNAL NORMALIZATION HELPERS
    // ============================================================

    /**
     * Normalizes text fields for reporting:
     * - Converts null to empty string
     * - Trims leading/trailing whitespace
     * - Replaces CR/LF sequences and extra whitespace with single spaces
     *
     * This ensures exported artifacts (CSV/Excel/Dashboards) do not contain embedded newlines
     * or irregular spacing that break formatting.
     *
     * @param value input text
     * @return cleaned text (never null; empty string if input null/blank)
     */
    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        // Replace different newline sequences with a single space and collapse multiple spaces.
        return cleaned
                .replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s{2,}", " ");
    }

    /**
     * Normalizes risk level strings to consistent, capitalized values when recognized.
     *
     * @param value raw risk level input
     * @return normalized risk string ("Low","Medium","High","Critical") or "Unknown" when blank
     */
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

        // If not recognized, return the cleaned input so downstream viewers still see the original.
        return cleaned;
    }

    /**
     * Ensures a threshold summary is never blank by returning a default informational message
     * when no explicit breaches are provided.
     *
     * @param value input summary
     * @return input cleaned summary or default "No configured threshold breaches detected."
     */
    private static String normalizeThresholdBreachSummary(String value) {
        String cleaned = normalizeText(value);
        return cleaned.isEmpty() ? NO_THRESHOLD_BREACHES : cleaned;
    }

    /**
     * Ensures integer values used for counts are non-negative.
     *
     * @param value input int
     * @return sanitized int (zero if negative)
     */
    private static int sanitizeInt(int value) {
        return Math.max(value, 0);
    }

    /**
     * Ensures long values used for durations and counts are non-negative.
     *
     * @param value input long
     * @return sanitized long (zero if negative)
     */
    private static long sanitizeLong(long value) {
        return Math.max(value, 0L);
    }

    /**
     * Ensures doubles used for percentages or other metrics are valid and non-negative.
     * Returns 0.0 for NaN, infinite, or negative inputs to keep reporting consistent.
     *
     * @param value input double value
     * @return sanitized double (0.0 for invalid inputs)
     */
    private static double sanitizeDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            return 0.0;
        }
        return value;
    }

    /**
     * Simple null/blank check used by the "safe" getters above.
     *
     * @param value input string
     * @return true when value is null or blank
     */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Returns a compact, development-friendly string representation of the result object.
     * Useful for quick debug printing in logs or test assertions.
     *
     * @return string containing key fields and metrics
     */
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
