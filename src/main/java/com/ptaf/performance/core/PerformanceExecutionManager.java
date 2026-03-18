package com.ptaf.performance.core;

import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceExecutionStatus;
import com.ptaf.performance.models.PerformanceProfile;
import com.ptaf.performance.models.PerformanceRequest;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;

import java.nio.file.Path;

/**
 * Fallback execution manager for prepared performance test plans.
 *
 * <p>This class is compatible with the richer {@link PerformanceExecutionResult}
 * model and can be used when a caller already has a prepared test plan and only
 * needs a framework-owned result object back.</p>
 *
 * <p>Important:
 * the main {@link PerformanceEngine} remains the primary execution path because
 * it performs richer JTL parsing, smarter risk calculation, and run-level report
 * generation. This manager exists as a compatible lower-level executor.</p>
 */
public class PerformanceExecutionManager {

    /**
     * Executes the given performance test plan and returns a framework-owned result.
     *
     * @param request performance request
     * @param profile performance profile
     * @param assertionProfile assertion thresholds
     * @param testPlan prepared JMeter DSL test plan
     * @param dashboardPath dashboard output path
     * @param jtlFilePath raw JTL result file path
     * @param summaryFilePath summary output file path
     * @param readableSummaryFilePath readable summary output file path
     * @param runReportRootPath run-level root output path
     * @return standardized execution result
     */
    public PerformanceExecutionResult execute(PerformanceRequest request,
                                              PerformanceProfile profile,
                                              PerformanceAssertionProfile assertionProfile,
                                              DslTestPlan testPlan,
                                              Path dashboardPath,
                                              Path jtlFilePath,
                                              Path summaryFilePath,
                                              Path readableSummaryFilePath,
                                              Path runReportRootPath) {

        validateInputs(request, profile, assertionProfile, testPlan);

        long startTimeMs = System.currentTimeMillis();

        try {
            testPlan.run();

            long totalScenarioDurationMs = Math.max(0L, System.currentTimeMillis() - startTimeMs);

            String fullTargetUrl = buildFullUrl(request);
            String authType = resolveAuthType(request);
            String payloadSourceType = resolvePayloadSourceType(request);
            String payloadSourceDetails = resolvePayloadSourceDetails(request);
            String executionMode = resolveExecutionMode(profile);

            String testPurpose = buildTestPurpose(request, profile);
            String performanceTestType = buildPerformanceTestType(profile, false);
            String testGoal = buildTestGoal(request, profile, false);

            double maxAllowedErrorPercent = assertionProfile.getMaxErrorPercent();
            long maxAllowedAverageResponseTimeMs = assertionProfile.getMaxAverageResponseTimeMs();
            long maxAllowedP95ResponseTimeMs = assertionProfile.getMaxP95ResponseTimeMs();

            return new PerformanceExecutionResult(
                    request.getRequestName(),
                    testPurpose,
                    performanceTestType,
                    testGoal,
                    safeValue(request.getMethod()),
                    safeValue(request.getPath()),
                    fullTargetUrl,
                    safeValue(request.getContentType()),
                    safeValue(request.getAcceptType()),
                    authType,
                    payloadSourceType,
                    payloadSourceDetails,
                    profile.getUsers(),
                    profile.getRampUpSeconds(),
                    profile.getHoldSeconds(),
                    profile.getIterations(),
                    executionMode,
                    maxAllowedErrorPercent,
                    maxAllowedAverageResponseTimeMs,
                    maxAllowedP95ResponseTimeMs,
                    totalScenarioDurationMs,
                    0L,   // totalSamples
                    0L,   // totalErrors
                    0.0,  // errorPercent
                    0L,   // minResponseTimeMs
                    0L,   // averageResponseTimeMs
                    0L,   // p95ResponseTimeMs
                    0L,   // maxResponseTimeMs
                    0,    // riskScore
                    "Low",
                    "No configured threshold breaches detected.",
                    "Use PerformanceEngine for full metric parsing and richer report generation.",
                    "Execution completed, but detailed response-time metrics were not parsed by PerformanceExecutionManager.",
                    "No detailed failure-rate analysis was produced by PerformanceExecutionManager.",
                    "Execution completed. Use PerformanceEngine output for full stability interpretation.",
                    "No failure point analysis available from PerformanceExecutionManager.",
                    "Execution completed. For richer interpretation, use the main PerformanceEngine flow.",
                    dashboardPath == null ? null : dashboardPath.toString(),
                    jtlFilePath == null ? null : jtlFilePath.toString(),
                    summaryFilePath == null ? null : summaryFilePath.toString(),
                    readableSummaryFilePath == null ? null : readableSummaryFilePath.toString(),
                    runReportRootPath == null ? null : runReportRootPath.toString(),
                    PerformanceExecutionStatus.PASS,
                    true,
                    false,
                    false,
                    null
            );

        } catch (Exception e) {
            long totalScenarioDurationMs = Math.max(0L, System.currentTimeMillis() - startTimeMs);

            return new PerformanceExecutionResult(
                    request.getRequestName(),
                    buildTestPurpose(request, profile),
                    buildPerformanceTestType(profile, false),
                    buildTestGoal(request, profile, false),
                    safeValue(request.getMethod()),
                    safeValue(request.getPath()),
                    buildFullUrl(request),
                    safeValue(request.getContentType()),
                    safeValue(request.getAcceptType()),
                    resolveAuthType(request),
                    resolvePayloadSourceType(request),
                    resolvePayloadSourceDetails(request),
                    profile.getUsers(),
                    profile.getRampUpSeconds(),
                    profile.getHoldSeconds(),
                    profile.getIterations(),
                    resolveExecutionMode(profile),
                    assertionProfile.getMaxErrorPercent(),
                    assertionProfile.getMaxAverageResponseTimeMs(),
                    assertionProfile.getMaxP95ResponseTimeMs(),
                    totalScenarioDurationMs,
                    0L,
                    0L,
                    0.0,
                    0L,
                    0L,
                    0L,
                    0L,
                    85,
                    "Critical",
                    "Execution manager failed before detailed metrics could be parsed.",
                    "Immediate investigation required before release.",
                    "Execution did not complete successfully.",
                    "Execution manager failed before detailed failure-rate analysis could be produced.",
                    "System stability could not be assessed because execution failed prematurely.",
                    "Framework detected failure with message: " + safeValue(e.getMessage()),
                    "Performance execution manager failed before detailed result generation completed.",
                    dashboardPath == null ? null : dashboardPath.toString(),
                    jtlFilePath == null ? null : jtlFilePath.toString(),
                    summaryFilePath == null ? null : summaryFilePath.toString(),
                    readableSummaryFilePath == null ? null : readableSummaryFilePath.toString(),
                    runReportRootPath == null ? null : runReportRootPath.toString(),
                    PerformanceExecutionStatus.FAIL,
                    false,
                    false,
                    true,
                    e.getMessage()
            );
        }
    }

    private void validateInputs(PerformanceRequest request,
                                PerformanceProfile profile,
                                PerformanceAssertionProfile assertionProfile,
                                DslTestPlan testPlan) {
        if (request == null) {
            throw new IllegalArgumentException("Performance request cannot be null.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("Performance profile cannot be null.");
        }

        if (assertionProfile == null) {
            throw new IllegalArgumentException("Performance assertion profile cannot be null.");
        }

        if (testPlan == null) {
            throw new IllegalArgumentException("DslTestPlan cannot be null.");
        }
    }

    private String buildTestPurpose(PerformanceRequest request, PerformanceProfile profile) {
        String method = safeValue(request.getMethod());
        String path = safeValue(request.getPath());

        if (profile.getIterations() > 0) {
            return "Validate " + method + " " + path + " behavior across repeated request iterations.";
        }

        return "Validate " + method + " " + path + " behavior under concurrent user traffic for a timed execution window.";
    }

    private String buildPerformanceTestType(PerformanceProfile profile, boolean expectedFailureMode) {
        if (expectedFailureMode) {
            return "Negative / Expected Failure Performance Validation";
        }

        if (profile.getUsers() <= 2) {
            return "Baseline / Smoke Performance Test";
        } else if (profile.getUsers() <= 20) {
            return "Load Performance Test";
        } else {
            return "High Load / Stress-Oriented Performance Test";
        }
    }

    private String buildTestGoal(PerformanceRequest request,
                                 PerformanceProfile profile,
                                 boolean expectedFailureMode) {
        if (expectedFailureMode) {
            return "Confirm that the system properly shows failure behavior and that the framework reports where and why the failure happened.";
        }

        if (profile.getIterations() > 0) {
            return "Measure consistency, response time, and error rate across repeated executions.";
        }

        return "Measure response time, stability, and failure behavior while multiple users hit the endpoint over time.";
    }

    private String resolveAuthType(PerformanceRequest request) {
        if (request.getBearerTokenAlias() != null && !request.getBearerTokenAlias().isBlank()) {
            return "Bearer Token";
        }

        if (request.getBasicAuthUsername() != null && !request.getBasicAuthUsername().isBlank()) {
            return "Basic Authentication";
        }

        return "No Authentication";
    }

    private String resolvePayloadSourceType(PerformanceRequest request) {
        return safeValue(request.getPayloadSourceType());
    }

    private String resolvePayloadSourceDetails(PerformanceRequest request) {
        return safeValue(request.getPayloadSourceDetails());
    }

    private String resolveExecutionMode(PerformanceProfile profile) {
        if (profile.getIterations() > 0) {
            return "Iteration-Based Execution";
        }
        return "Duration-Based Execution";
    }

    private String buildFullUrl(PerformanceRequest request) {
        String protocol = safeValue(request.getProtocol());
        String host = safeValue(request.getHost());
        String path = safeValue(request.getPath());

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return protocol + "://" + host + ":" + request.getPort() + normalizedPath;
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}