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
     * <p>This method is intended to run a prepared JMeter DSL test plan and produce a
     * {@link PerformanceExecutionResult} object that summarizes the execution. Because
     * this manager is a minimal fallback executor, it does not parse detailed JTL
     * metrics; instead it returns default/placeholder metric values and descriptive
     * messages indicating that richer analysis should be obtained from the main engine.</p>
     *
     * <p>Behavior summary:
     * - Validates inputs.
     * - Executes the provided {@code testPlan} synchronously via {@code testPlan.run()}.
     * - Times the execution and records a summary {@link PerformanceExecutionResult}.
     * - On success: returns a PASS result with placeholder metrics and guidance messages.
     * - On failure: returns a FAIL result with the exception message and failure guidance.</p>
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

        // Ensure required inputs are present before attempting execution.
        validateInputs(request, profile, assertionProfile, testPlan);

        // Capture start time to compute overall scenario duration even if execution fails.
        long startTimeMs = System.currentTimeMillis();

        try {
            // Execute the prepared JMeter DSL test plan synchronously.
            // This may block until the test plan completes.
            testPlan.run();

            // Compute total elapsed time for the scenario, ensuring non-negative result.
            long totalScenarioDurationMs = Math.max(0L, System.currentTimeMillis() - startTimeMs);

            // Collect descriptive metadata about the execution for result reporting.
            String fullTargetUrl = buildFullUrl(request);
            String authType = resolveAuthType(request);
            String payloadSourceType = resolvePayloadSourceType(request);
            String payloadSourceDetails = resolvePayloadSourceDetails(request);
            String executionMode = resolveExecutionMode(profile);

            String testPurpose = buildTestPurpose(request, profile);
            String performanceTestType = buildPerformanceTestType(profile, false);
            String testGoal = buildTestGoal(request, profile, false);

            // Extract assertion thresholds to include in the result object.
            double maxAllowedErrorPercent = assertionProfile.getMaxErrorPercent();
            long maxAllowedAverageResponseTimeMs = assertionProfile.getMaxAverageResponseTimeMs();
            long maxAllowedP95ResponseTimeMs = assertionProfile.getMaxP95ResponseTimeMs();

            // Build and return a PASS result. Note: detailed per-sample metrics are not
            // parsed by this class, so placeholders (zeros/N/A) are used for response-time
            // and error-rate metrics. Messages explain that richer analysis is available
            // from the main PerformanceEngine flow.
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
                    0L,   // totalSamples - not parsed by this fallback manager
                    0L,   // totalErrors - not parsed by this fallback manager
                    0.0,  // errorPercent - not parsed by this fallback manager
                    0L,   // minResponseTimeMs - not parsed by this fallback manager
                    0L,   // averageResponseTimeMs - not parsed by this fallback manager
                    0L,   // p95ResponseTimeMs - not parsed by this fallback manager
                    0L,   // maxResponseTimeMs - not parsed by this fallback manager
                    0,    // riskScore - default low risk for successful execution without parsed metrics
                    "Low", // riskCategory
                    "No configured threshold breaches detected.", // shortSummary
                    "Use PerformanceEngine for full metric parsing and richer report generation.", // mitigation
                    "Execution completed, but detailed response-time metrics were not parsed by PerformanceExecutionManager.", // longSummary
                    "No detailed failure-rate analysis was produced by PerformanceExecutionManager.", // failureRateSummary
                    "Execution completed. Use PerformanceEngine output for full stability interpretation.", // stabilitySummary
                    "No failure point analysis available from PerformanceExecutionManager.", // failurePointAnalysis
                    "Execution completed. For richer interpretation, use the main PerformanceEngine flow.", // recommendations
                    dashboardPath == null ? null : dashboardPath.toString(),
                    jtlFilePath == null ? null : jtlFilePath.toString(),
                    summaryFilePath == null ? null : summaryFilePath.toString(),
                    readableSummaryFilePath == null ? null : readableSummaryFilePath.toString(),
                    runReportRootPath == null ? null : runReportRootPath.toString(),
                    PerformanceExecutionStatus.PASS,
                    true,   // executedSuccessfully
                    false,  // aborted
                    false,  // failed
                    null    // failureMessage
            );

        } catch (Exception e) {
            // On any exception during execution, capture total elapsed time and return a FAIL result.
            long totalScenarioDurationMs = Math.max(0L, System.currentTimeMillis() - startTimeMs);

            // Construct a failure result. As with the success path, detailed metrics are
            // not available; instead include descriptive failure messages and the exception text.
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
                    0L,   // totalSamples - unavailable due to failure
                    0L,   // totalErrors - unavailable due to failure
                    0.0,  // errorPercent - unavailable due to failure
                    0L,   // minResponseTimeMs - unavailable due to failure
                    0L,   // averageResponseTimeMs - unavailable due to failure
                    0L,   // p95ResponseTimeMs - unavailable due to failure
                    0L,   // maxResponseTimeMs - unavailable due to failure
                    85,   // riskScore - assign a high default risk for execution failure
                    "Critical", // riskCategory
                    "Execution manager failed before detailed metrics could be parsed.", // shortSummary
                    "Immediate investigation required before release.", // mitigation
                    "Execution did not complete successfully.", // longSummary
                    "Execution manager failed before detailed failure-rate analysis could be produced.", // failureRateSummary
                    "System stability could not be assessed because execution failed prematurely.", // stabilitySummary
                    "Framework detected failure with message: " + safeValue(e.getMessage()), // failurePointAnalysis
                    "Performance execution manager failed before detailed result generation completed.", // recommendations
                    dashboardPath == null ? null : dashboardPath.toString(),
                    jtlFilePath == null ? null : jtlFilePath.toString(),
                    summaryFilePath == null ? null : summaryFilePath.toString(),
                    readableSummaryFilePath == null ? null : readableSummaryFilePath.toString(),
                    runReportRootPath == null ? null : runReportRootPath.toString(),
                    PerformanceExecutionStatus.FAIL,
                    false,  // executedSuccessfully
                    false,  // aborted
                    true,   // failed
                    e.getMessage() // failureMessage containing exception text for troubleshooting
            );
        }
    }

    /**
     * Validates that the required inputs for execution are provided.
     *
     * <p>This method throws an IllegalArgumentException when any mandatory argument
     * is null. It is used to fail-fast and provide clear error messages for testers
     * and integrators.</p>
     *
     * @param request the performance request (must not be null)
     * @param profile the performance profile (must not be null)
     * @param assertionProfile the assertions profile (must not be null)
     * @param testPlan the prepared DSL test plan (must not be null)
     */
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

    /**
     * Builds a human-readable description of the test purpose based on the request and profile.
     *
     * <p>Produces different text depending on whether the profile is iteration-based or duration-based,
     * helping testers understand why a particular run was executed.</p>
     *
     * @param request the performance request containing method and path
     * @param profile the performance profile containing iterations information
     * @return descriptive purpose string
     */
    private String buildTestPurpose(PerformanceRequest request, PerformanceProfile profile) {
        String method = safeValue(request.getMethod());
        String path = safeValue(request.getPath());

        if (profile.getIterations() > 0) {
            // Iteration-based runs focus on repeated request behavior and consistency.
            return "Validate " + method + " " + path + " behavior across repeated request iterations.";
        }

        // Duration-based runs focus on concurrent users over a timed window.
        return "Validate " + method + " " + path + " behavior under concurrent user traffic for a timed execution window.";
    }

    /**
     * Produces a high-level string categorizing the performance test type.
     *
     * <p>The returned category is influenced by the configured number of users. An optional
     * expectedFailureMode parameter switches the message to a negative test description.</p>
     *
     * @param profile the performance profile with user count
     * @param expectedFailureMode when true, indicates the run is intended to provoke failure behavior
     * @return human-friendly performance test type description
     */
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

    /**
     * Builds a short statement describing the primary goal of the test.
     *
     * <p>Goals differ between iteration-based vs duration-based runs and between normal and
     * expected-failure test modes.</p>
     *
     * @param request the performance request
     * @param profile the performance profile
     * @param expectedFailureMode whether this run intentionally expects a failure
     * @return descriptive test goal string
     */
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

    /**
     * Determines the authentication mechanism used by the request.
     *
     * <p>Checks for bearer token alias then basic auth username, otherwise reports no authentication.</p>
     *
     * @param request the performance request potentially containing auth details
     * @return textual representation of authentication type
     */
    private String resolveAuthType(PerformanceRequest request) {
        if (request.getBearerTokenAlias() != null && !request.getBearerTokenAlias().isBlank()) {
            return "Bearer Token";
        }

        if (request.getBasicAuthUsername() != null && !request.getBasicAuthUsername().isBlank()) {
            return "Basic Authentication";
        }

        return "No Authentication";
    }

    /**
     * Returns a safe textual representation of the payload source type configured in the request.
     *
     * @param request the performance request
     * @return payload source type or "N/A" when not present
     */
    private String resolvePayloadSourceType(PerformanceRequest request) {
        return safeValue(request.getPayloadSourceType());
    }

    /**
     * Returns a safe textual representation of the payload source details configured in the request.
     *
     * @param request the performance request
     * @return payload source details or "N/A" when not present
     */
    private String resolvePayloadSourceDetails(PerformanceRequest request) {
        return safeValue(request.getPayloadSourceDetails());
    }

    /**
     * Returns a textual representation of whether the execution is iteration-based or duration-based.
     *
     * @param profile the performance profile
     * @return "Iteration-Based Execution" when iterations > 0, otherwise "Duration-Based Execution"
     */
    private String resolveExecutionMode(PerformanceProfile profile) {
        if (profile.getIterations() > 0) {
            return "Iteration-Based Execution";
        }
        return "Duration-Based Execution";
    }

    /**
     * Builds the full target URL for reporting purposes from request components.
     *
     * <p>This method concatenates protocol, host, port and path ensuring the path begins with a '/'.</p>
     *
     * @param request the performance request containing protocol, host, port and path
     * @return constructed full URL string (may contain "N/A" parts if values are missing)
     */
    private String buildFullUrl(PerformanceRequest request) {
        String protocol = safeValue(request.getProtocol());
        String host = safeValue(request.getHost());
        String path = safeValue(request.getPath());

        // Ensure the path starts with a leading slash for proper URL formatting.
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return protocol + "://" + host + ":" + request.getPort() + normalizedPath;
    }

    /**
     * Safely returns a displayable string for potentially null or blank values.
     *
     * <p>Used throughout reporting to avoid nulls and present a consistent "N/A" placeholder.</p>
     *
     * @param value the original string value
     * @return original value when non-null and non-blank, otherwise "N/A"
     */
    private String safeValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
