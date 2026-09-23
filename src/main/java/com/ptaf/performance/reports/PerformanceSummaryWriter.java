package com.ptaf.performance.reports;

import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceExecutionStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes standardized summary artifacts for completed performance executions.
 *
 * <p>This writer produces:
 * <ul>
 *   <li>technical summary for QA / engineering</li>
 *   <li>readable summary for leadership / non-technical stakeholders</li>
 * </ul>
 * </p>
 *
 * <p>Reporting-safe goals:
 * <ul>
 *   <li>keep artifact generation stable</li>
 *   <li>align wording with Excel business reporting</li>
 *   <li>improve readability without changing execution logic</li>
 * </ul>
 * </p>
 *
 * <p>Usage notes:
 * <ul>
 *   <li>Call writeTextSummary to produce a compact, technical summary file intended for engineering/QA.</li>
 *   <li>Call writeReadableSummary to produce a more narrative summary intended for stakeholders.</li>
 *   <li>Both methods expect the provided PerformanceExecutionResult to contain valid file paths and a non-empty test name.</li>
 * </ul>
 * </p>
 */
public class PerformanceSummaryWriter {

    /**
     * Write a technical text summary to the file path specified on the provided executionResult.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Validates that executionResult and its essential fields are present.</li>
     *   <li>Verifies that a summary file path is configured on executionResult.</li>
     *   <li>Ensures parent directories exist for the target file.</li>
     *   <li>Builds the summary content and writes it using UTF-8 encoding.</li>
     * </ol>
     *
     * @param executionResult the performance execution data to summarize; must not be null and must contain a valid test name
     * @throws IllegalArgumentException if executionResult is null, missing a test name, or missing a summary file path
     * @throws RuntimeException if the file cannot be written or parent directories cannot be created
     */
    public void writeTextSummary(PerformanceExecutionResult executionResult) {
        // Ensure the provided execution result is valid before proceeding.
        validateExecutionResult(executionResult);

        // Retrieve the configured file path where the technical summary should be written.
        String summaryFilePath = executionResult.getSummaryFilePath();
        if (summaryFilePath == null || summaryFilePath.isBlank()) {
            // Fail-fast if no summary path is configured; callers/tests will see a clear exception.
            throw new IllegalArgumentException("Summary file path is missing in PerformanceExecutionResult.");
        }

        // Resolve the path and make sure directories exist.
        Path path = Path.of(summaryFilePath);
        createParentDirectoryIfMissing(path);

        // Construct the textual summary content.
        String summaryContent = buildTextSummary(executionResult);

        // Write the summary content to disk using UTF-8 encoding.
        try {
            Files.writeString(path, summaryContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Wrap checked IO exceptions into a runtime exception with a helpful message for testers/operators.
            throw new RuntimeException("Failed to write performance summary file: " + path, e);
        }
    }

    /**
     * Write a readable (narrative) summary to the file path specified on the provided executionResult.
     *
     * <p>This method mirrors the behavior of writeTextSummary but produces a different, stakeholder-focused
     * layout and wording intended for leadership and non-technical audiences.</p>
     *
     * @param executionResult the performance execution data to summarize; must not be null and must contain a valid test name
     * @throws IllegalArgumentException if executionResult is null, missing a test name, or missing a readable summary file path
     * @throws RuntimeException if the file cannot be written or parent directories cannot be created
     */
    public void writeReadableSummary(PerformanceExecutionResult executionResult) {
        // Validate basic integrity of the execution result object.
        validateExecutionResult(executionResult);

        // Obtain the configured path for the readable summary output.
        String readableSummaryFilePath = executionResult.getReadableSummaryFilePath();
        if (readableSummaryFilePath == null || readableSummaryFilePath.isBlank()) {
            // Clear error if caller did not configure an output path.
            throw new IllegalArgumentException("Readable summary file path is missing in PerformanceExecutionResult.");
        }

        // Ensure the directory structure exists and write the readable summary content.
        Path path = Path.of(readableSummaryFilePath);
        createParentDirectoryIfMissing(path);

        String summaryContent = buildReadableSummary(executionResult);

        try {
            Files.writeString(path, summaryContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Propagate a runtime exception with context to help diagnose failing test runs.
            throw new RuntimeException("Failed to write readable performance summary file: " + path, e);
        }
    }

    /**
     * Build the technical (compact) summary as a single string.
     *
     * <p>The output contains discrete numbered sections:
     * <ol>
     *   <li>Test overview</li>
     *   <li>Request information</li>
     *   <li>Load profile</li>
     *   <li>Configured thresholds</li>
     *   <li>Execution metrics</li>
     *   <li>Business / risk status</li>
     *   <li>Interpretation</li>
     *   <li>Generated artifacts (file paths)</li>
     * </ol>
     * </p>
     *
     * <p>Number formatting and null-safety are delegated to PerformanceExcelFormatHelper and the safe(...) helper.</p>
     *
     * @param executionResult populated execution result with metrics and metadata
     * @return full content for the technical summary file
     */
    protected String buildTextSummary(PerformanceExecutionResult executionResult) {
        String nl = System.lineSeparator(); // Use platform line separator for portability.
        StringBuilder builder = new StringBuilder();

        // Header block
        builder.append("=======================================================================").append(nl);
        builder.append("                    PERFORMANCE EXECUTION SUMMARY").append(nl);
        builder.append("=======================================================================").append(nl);

        // 1. Test overview
        builder.append("1. TEST OVERVIEW").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Test Name                    : ").append(executionResult.getSafeTestName()).append(nl);
        builder.append("Test Purpose                 : ").append(safe(executionResult.getTestPurpose())).append(nl);
        builder.append("Performance Test Type        : ").append(safe(executionResult.getPerformanceTestType())).append(nl);
        builder.append("Test Goal                    : ").append(safe(executionResult.getTestGoal())).append(nl);
        builder.append(nl);

        // 2. Request information: technical request details used by the scenario.
        builder.append("2. REQUEST INFORMATION").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("HTTP Method                  : ").append(safe(executionResult.getHttpMethod())).append(nl);
        builder.append("Target Path                  : ").append(executionResult.getSafeTargetPath()).append(nl);
        builder.append("Full Target URL              : ").append(safe(executionResult.getFullTargetUrl())).append(nl);
        builder.append("Content Type                 : ").append(safe(executionResult.getContentType())).append(nl);
        builder.append("Accept Type                  : ").append(safe(executionResult.getAcceptType())).append(nl);
        builder.append("Authentication Type          : ").append(safe(executionResult.getAuthType())).append(nl);
        builder.append("Payload Source Type          : ").append(safe(executionResult.getPayloadSourceType())).append(nl);
        builder.append("Payload Source Details       : ").append(safe(executionResult.getPayloadSourceDetails())).append(nl);
        builder.append(nl);

        // 3. Load profile: how load was applied during the test.
        builder.append("3. LOAD PROFILE").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Users                        : ").append(executionResult.getUsers()).append(nl);
        builder.append("Ramp-Up Seconds              : ").append(executionResult.getRampUpSeconds()).append(nl);
        builder.append("Hold Seconds                 : ").append(executionResult.getHoldSeconds()).append(nl);
        builder.append("Iterations                   : ").append(executionResult.getIterations()).append(nl);
        builder.append("Execution Mode               : ").append(safe(executionResult.getExecutionMode())).append(nl);
        builder.append(nl);

        // 4. Configured thresholds: expected performance limits for pass/fail logic.
        builder.append("4. CONFIGURED THRESHOLDS").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Max Allowed Error Percent    : ")
                .append(PerformanceExcelFormatHelper.formatPercent(executionResult.getMaxAllowedErrorPercent()))
                .append(nl);
        builder.append("Max Allowed Avg Response     : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMaxAllowedAverageResponseTimeMs()))
                .append(nl);
        builder.append("Max Allowed P95 Response     : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMaxAllowedP95ResponseTimeMs()))
                .append(nl);
        builder.append("Threshold Breach Summary     : ").append(safe(executionResult.getThresholdBreachSummary())).append(nl);
        builder.append(nl);

        // 5. Execution metrics: measured results from the run.
        builder.append("5. EXECUTION METRICS").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Total Scenario Duration      : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getTotalScenarioDurationMs()))
                .append(nl);
        builder.append("Total Samples                : ")
                .append(PerformanceExcelFormatHelper.formatInteger(executionResult.getTotalSamples()))
                .append(nl);
        builder.append("Total Errors                 : ")
                .append(PerformanceExcelFormatHelper.formatInteger(executionResult.getTotalErrors()))
                .append(nl);
        builder.append("Error Percent                : ")
                .append(PerformanceExcelFormatHelper.formatPercent(executionResult.getErrorPercent()))
                .append(nl);
        builder.append("Minimum Response Time        : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMinResponseTimeMs()))
                .append(nl);
        builder.append("Average Response Time        : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getAverageResponseTimeMs()))
                .append(nl);
        builder.append("P95 Response Time            : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getP95ResponseTimeMs()))
                .append(nl);
        builder.append("Maximum Response Time        : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMaxResponseTimeMs()))
                .append(nl);
        builder.append(nl);

        // 6. Business / risk status: interpretation of outcome in business terms.
        builder.append("6. BUSINESS / RISK STATUS").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Business Outcome             : ").append(executionResult.getBusinessOutcomeLabel()).append(nl);
        builder.append("Status Meaning               : ").append(getStatusMeaning(executionResult.getExecutionStatus())).append(nl);
        builder.append("Attention Category           : ").append(executionResult.getAttentionCategory()).append(nl);
        builder.append("Primary Business Concern     : ").append(executionResult.getPrimaryBusinessConcern()).append(nl);
        builder.append("Execution Status             : ")
                .append(safe(statusName(executionResult.getExecutionStatus())))
                .append(nl);
        builder.append("Risk Score                   : ")
                .append(PerformanceExcelFormatHelper.formatScore(executionResult.getRiskScore()))
                .append(nl);
        builder.append("Risk Level                   : ").append(safe(executionResult.getRiskLevel())).append(nl);
        builder.append("Recommended Action           : ").append(executionResult.getSafeRecommendedAction()).append(nl);
        builder.append("Execution Passed             : ").append(executionResult.isExecutionPassed()).append(nl);
        builder.append("Expected Failure Mode        : ").append(executionResult.isExpectedFailureMode()).append(nl);
        builder.append("Actual Failure Detected      : ").append(executionResult.isActualFailureDetected()).append(nl);
        builder.append("Failure Message              : ").append(safe(executionResult.getSafeFailureMessage())).append(nl);
        builder.append(nl);

        // 7. Interpretation: more detailed assessments produced by the analysis logic.
        builder.append("7. INTERPRETATION").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Response Assessment          : ").append(executionResult.getSafeResponseTimeAssessment()).append(nl);
        builder.append("Error Assessment             : ").append(executionResult.getSafeErrorAssessment()).append(nl);
        builder.append("Stability Assessment         : ").append(executionResult.getSafeStabilityAssessment()).append(nl);
        builder.append("First Failure Indicator      : ").append(executionResult.getSafeFirstFailureIndicator()).append(nl);
        builder.append("Final Conclusion             : ").append(executionResult.getSafeFinalConclusion()).append(nl);
        builder.append(nl);

        // 8. Generated artifacts: file paths useful to locate additional artifacts produced by the run.
        builder.append("8. GENERATED ARTIFACTS").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Dashboard Path               : ").append(safe(executionResult.getDashboardPath())).append(nl);
        builder.append("JTL File Path                : ").append(safe(executionResult.getJtlFilePath())).append(nl);
        builder.append("Summary File Path            : ").append(safe(executionResult.getSummaryFilePath())).append(nl);
        builder.append("Readable Summary Path        : ").append(safe(executionResult.getReadableSummaryFilePath())).append(nl);
        builder.append("Run Report Root Path         : ").append(safe(executionResult.getRunReportRootPath())).append(nl);
        builder.append("=======================================================================").append(nl);

        return builder.toString();
    }

    /**
     * Build the readable (narrative) summary as a single string.
     *
     * <p>The output is designed to be easier to read for stakeholders and follows a question-and-answer flow:
     * "What was tested?", "How was it tested?", "What were the expectations?", "What happened?",
     * "How risky is the result?", "How did the system behave?", "Final result", "Where to find report files".</p>
     *
     * <p>Formatting functions are reused from PerformanceExcelFormatHelper to maintain consistency with Excel reports.</p>
     *
     * @param executionResult the performance execution data to present
     * @return full content for the readable summary file
     */
    protected String buildReadableSummary(PerformanceExecutionResult executionResult) {
        String nl = System.lineSeparator(); // Platform-specific line break
        StringBuilder builder = new StringBuilder();

        // Header
        builder.append("=======================================================================").append(nl);
        builder.append("                 READABLE PERFORMANCE TEST SUMMARY").append(nl);
        builder.append("=======================================================================").append(nl);
        builder.append(nl);

        // "What was tested?" section provides high-level test metadata.
        builder.append("What was tested?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Test Name: ").append(executionResult.getSafeTestName()).append(nl);
        builder.append("Purpose: ").append(safe(executionResult.getTestPurpose())).append(nl);
        builder.append("Test Type: ").append(safe(executionResult.getPerformanceTestType())).append(nl);
        builder.append("Goal: ").append(safe(executionResult.getTestGoal())).append(nl);
        builder.append("Target: ").append(safe(executionResult.getHttpMethod()))
                .append(" ")
                .append(safe(executionResult.getFullTargetUrl()))
                .append(nl);
        builder.append(nl);

        // "How was it tested?" section describes the load profile and duration.
        builder.append("How was it tested?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Users: ").append(PerformanceExcelFormatHelper.formatInteger(executionResult.getUsers())).append(nl);
        builder.append("Ramp-up: ").append(PerformanceExcelFormatHelper.formatInteger(executionResult.getRampUpSeconds())).append(" seconds").append(nl);
        builder.append("Hold time: ").append(PerformanceExcelFormatHelper.formatInteger(executionResult.getHoldSeconds())).append(" seconds").append(nl);
        builder.append("Iterations: ").append(PerformanceExcelFormatHelper.formatInteger(executionResult.getIterations())).append(nl);
        builder.append("Execution mode: ").append(safe(executionResult.getExecutionMode())).append(nl);
        builder.append("Total scenario duration: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getTotalScenarioDurationMs()))
                .append(nl);
        builder.append(nl);

        // "What were the expectations?" section lists configured thresholds and allowed limits.
        builder.append("What were the expectations?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Allowed failure rate: ")
                .append(PerformanceExcelFormatHelper.formatPercent(executionResult.getMaxAllowedErrorPercent()))
                .append(nl);
        builder.append("Allowed average response: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMaxAllowedAverageResponseTimeMs()))
                .append(nl);
        builder.append("Allowed p95 response: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMaxAllowedP95ResponseTimeMs()))
                .append(nl);
        builder.append("Threshold breaches: ").append(safe(executionResult.getThresholdBreachSummary())).append(nl);
        builder.append(nl);

        // "What happened?" section summarizes measured results.
        builder.append("What happened?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Total requests sent: ")
                .append(PerformanceExcelFormatHelper.formatInteger(executionResult.getTotalSamples()))
                .append(nl);
        builder.append("Failed requests: ")
                .append(PerformanceExcelFormatHelper.formatInteger(executionResult.getTotalErrors()))
                .append(nl);
        builder.append("Failure rate: ")
                .append(PerformanceExcelFormatHelper.formatPercent(executionResult.getErrorPercent()))
                .append(nl);
        builder.append("Fastest response: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMinResponseTimeMs()))
                .append(nl);
        builder.append("Average response: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getAverageResponseTimeMs()))
                .append(nl);
        builder.append("95% of responses were at or below: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getP95ResponseTimeMs()))
                .append(nl);
        builder.append("Slowest response: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getMaxResponseTimeMs()))
                .append(nl);
        builder.append(nl);

        // Risk analysis section: translate technical outcomes into business terms.
        builder.append("How risky is the result?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Business outcome: ").append(executionResult.getBusinessOutcomeLabel()).append(nl);
        builder.append("Status meaning: ").append(getStatusMeaning(executionResult.getExecutionStatus())).append(nl);
        builder.append("Attention category: ").append(executionResult.getAttentionCategory()).append(nl);
        builder.append("Primary concern: ").append(executionResult.getPrimaryBusinessConcern()).append(nl);
        builder.append("Execution status: ")
                .append(safe(statusName(executionResult.getExecutionStatus())))
                .append(nl);
        builder.append("Risk score: ")
                .append(PerformanceExcelFormatHelper.formatScore(executionResult.getRiskScore()))
                .append(nl);
        builder.append("Risk level: ").append(safe(executionResult.getRiskLevel())).append(nl);
        builder.append("Recommended action: ").append(executionResult.getSafeRecommendedAction()).append(nl);
        builder.append(nl);

        // System behavior section: assessments produced by the execution analysis.
        builder.append("How did the system behave?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Response time assessment: ").append(executionResult.getSafeResponseTimeAssessment()).append(nl);
        builder.append("Error assessment: ").append(executionResult.getSafeErrorAssessment()).append(nl);
        builder.append("Stability assessment: ").append(executionResult.getSafeStabilityAssessment()).append(nl);
        builder.append("Where failures started: ").append(executionResult.getSafeFirstFailureIndicator()).append(nl);
        builder.append(nl);

        // Final result section: pass/fail and failure details.
        builder.append("Final result").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Execution passed: ").append(executionResult.isExecutionPassed()).append(nl);
        builder.append("Expected failure mode: ").append(executionResult.isExpectedFailureMode()).append(nl);
        builder.append("Actual failure detected: ").append(executionResult.isActualFailureDetected()).append(nl);
        builder.append("Failure message: ").append(safe(executionResult.getSafeFailureMessage())).append(nl);
        builder.append("Conclusion: ").append(executionResult.getSafeFinalConclusion()).append(nl);
        builder.append(nl);

        // Artifact locations: where to find supporting files produced by the run.
        builder.append("Where to find report files").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Dashboard: ").append(safe(executionResult.getDashboardPath())).append(nl);
        builder.append("Raw JTL: ").append(safe(executionResult.getJtlFilePath())).append(nl);
        builder.append("Technical summary: ").append(safe(executionResult.getSummaryFilePath())).append(nl);
        builder.append("Readable summary: ").append(safe(executionResult.getReadableSummaryFilePath())).append(nl);
        builder.append("Run folder: ").append(safe(executionResult.getRunReportRootPath())).append(nl);
        builder.append("=======================================================================").append(nl);

        return builder.toString();
    }

    /**
     * Validate that the provided PerformanceExecutionResult contains minimum required information.
     *
     * <p>Currently the checks performed are:
     * <ul>
     *   <li>executionResult is not null</li>
     *   <li>testName is not null or blank</li>
     * </ul>
     * </p>
     *
     * @param executionResult the object to validate
     * @throws IllegalArgumentException when validation fails
     */
    private void validateExecutionResult(PerformanceExecutionResult executionResult) {
        if (executionResult == null) {
            // Immediately fail if the caller provided no result object.
            throw new IllegalArgumentException("PerformanceExecutionResult cannot be null.");
        }

        if (executionResult.getTestName() == null || executionResult.getTestName().isBlank()) {
            // Enforce the presence of a test name because summaries rely on this identifier.
            throw new IllegalArgumentException("PerformanceExecutionResult test name cannot be null or blank.");
        }
    }

    /**
     * Ensure parent directories exist for the provided path. If the parent is null (path at file system root),
     * no action is taken.
     *
     * @param path the file path for which parent directories should exist
     * @throws RuntimeException if directories cannot be created due to an IO error
     */
    private void createParentDirectoryIfMissing(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                // Create directories if they do not already exist. This is idempotent.
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            // Surface a clear runtime exception to indicate directory creation failure.
            throw new RuntimeException("Failed to create parent directories for summary file: " + path, e);
        }
    }

    /**
     * Safe text helper that delegates to PerformanceExcelFormatHelper.safeText.
     *
     * <p>This centralizes null/blank handling for textual fields appearing in summaries.</p>
     *
     * @param value input text that may be null
     * @return safe, non-null textual representation
     */
    private String safe(String value) {
        return PerformanceExcelFormatHelper.safeText(value);
    }

    /**
     * Returns a stable name for the provided execution status.
     *
     * @param status the execution status, may be null
     * @return the enum name or "UNKNOWN" when status is null
     */
    private String statusName(PerformanceExecutionStatus status) {
        return status == null ? "UNKNOWN" : status.name();
    }

    /**
     * Translate an execution status into a human-friendly summary meaning.
     *
     * <p>The actual mapping logic resides on PerformanceExecutionStatus.toSummaryMeaning(...).</p>
     *
     * @param status the execution status to translate
     * @return a short textual meaning appropriate for summaries
     */
    private String getStatusMeaning(PerformanceExecutionStatus status) {
        return PerformanceExecutionStatus.toSummaryMeaning(status);
    }
}
