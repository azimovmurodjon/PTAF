package com.ptaf.performance.reports;

import com.ptaf.performance.models.PerformanceExecutionResult;

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
 */
public class PerformanceSummaryWriter {

    public void writeTextSummary(PerformanceExecutionResult executionResult) {
        validateExecutionResult(executionResult);

        String summaryFilePath = executionResult.getSummaryFilePath();
        if (summaryFilePath == null || summaryFilePath.isBlank()) {
            throw new IllegalArgumentException("Summary file path is missing in PerformanceExecutionResult.");
        }

        Path path = Path.of(summaryFilePath);
        createParentDirectoryIfMissing(path);

        String summaryContent = buildTextSummary(executionResult);

        try {
            Files.writeString(path, summaryContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write performance summary file: " + path, e);
        }
    }

    public void writeReadableSummary(PerformanceExecutionResult executionResult) {
        validateExecutionResult(executionResult);

        String readableSummaryFilePath = executionResult.getReadableSummaryFilePath();
        if (readableSummaryFilePath == null || readableSummaryFilePath.isBlank()) {
            throw new IllegalArgumentException("Readable summary file path is missing in PerformanceExecutionResult.");
        }

        Path path = Path.of(readableSummaryFilePath);
        createParentDirectoryIfMissing(path);

        String summaryContent = buildReadableSummary(executionResult);

        try {
            Files.writeString(path, summaryContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write readable performance summary file: " + path, e);
        }
    }

    protected String buildTextSummary(PerformanceExecutionResult executionResult) {
        String nl = System.lineSeparator();
        StringBuilder builder = new StringBuilder();

        builder.append("=======================================================================").append(nl);
        builder.append("                    PERFORMANCE EXECUTION SUMMARY").append(nl);
        builder.append("=======================================================================").append(nl);

        builder.append("1. TEST OVERVIEW").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Test Name                    : ").append(safe(executionResult.getTestName())).append(nl);
        builder.append("Test Purpose                 : ").append(safe(executionResult.getTestPurpose())).append(nl);
        builder.append("Performance Test Type        : ").append(safe(executionResult.getPerformanceTestType())).append(nl);
        builder.append("Test Goal                    : ").append(safe(executionResult.getTestGoal())).append(nl);
        builder.append(nl);

        builder.append("2. REQUEST INFORMATION").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("HTTP Method                  : ").append(safe(executionResult.getHttpMethod())).append(nl);
        builder.append("Target Path                  : ").append(safe(executionResult.getTargetPath())).append(nl);
        builder.append("Full Target URL              : ").append(safe(executionResult.getFullTargetUrl())).append(nl);
        builder.append("Content Type                 : ").append(safe(executionResult.getContentType())).append(nl);
        builder.append("Accept Type                  : ").append(safe(executionResult.getAcceptType())).append(nl);
        builder.append("Authentication Type          : ").append(safe(executionResult.getAuthType())).append(nl);
        builder.append("Payload Source Type          : ").append(safe(executionResult.getPayloadSourceType())).append(nl);
        builder.append("Payload Source Details       : ").append(safe(executionResult.getPayloadSourceDetails())).append(nl);
        builder.append(nl);

        builder.append("3. LOAD PROFILE").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Users                        : ").append(executionResult.getUsers()).append(nl);
        builder.append("Ramp-Up Seconds              : ").append(executionResult.getRampUpSeconds()).append(nl);
        builder.append("Hold Seconds                 : ").append(executionResult.getHoldSeconds()).append(nl);
        builder.append("Iterations                   : ").append(executionResult.getIterations()).append(nl);
        builder.append("Execution Mode               : ").append(safe(executionResult.getExecutionMode())).append(nl);
        builder.append(nl);

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

        builder.append("5. EXECUTION METRICS").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Total Scenario Duration      : ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getTotalScenarioDurationMs()))
                .append(nl);
        builder.append("Total Samples                : ").append(executionResult.getTotalSamples()).append(nl);
        builder.append("Total Errors                 : ").append(executionResult.getTotalErrors()).append(nl);
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

        builder.append("6. RISK AND STATUS").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Execution Status             : ")
                .append(safe(executionResult.getExecutionStatus() == null ? null : executionResult.getExecutionStatus().name()))
                .append(nl);
        builder.append("Risk Score                   : ").append(executionResult.getRiskScore()).append(nl);
        builder.append("Risk Level                   : ").append(safe(executionResult.getRiskLevel())).append(nl);
        builder.append("Recommended Action           : ").append(safe(executionResult.getRecommendedAction())).append(nl);
        builder.append("Execution Passed             : ").append(executionResult.isExecutionPassed()).append(nl);
        builder.append("Expected Failure Mode        : ").append(executionResult.isExpectedFailureMode()).append(nl);
        builder.append("Actual Failure Detected      : ").append(executionResult.isActualFailureDetected()).append(nl);
        builder.append("Failure Message              : ").append(safe(executionResult.getFailureMessage())).append(nl);
        builder.append(nl);

        builder.append("7. INTERPRETATION").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Response Assessment          : ").append(safe(executionResult.getResponseTimeAssessment())).append(nl);
        builder.append("Error Assessment             : ").append(safe(executionResult.getErrorAssessment())).append(nl);
        builder.append("Stability Assessment         : ").append(safe(executionResult.getStabilityAssessment())).append(nl);
        builder.append("First Failure Indicator      : ").append(safe(executionResult.getFirstFailureIndicator())).append(nl);
        builder.append("Final Conclusion             : ").append(safe(executionResult.getFinalConclusion())).append(nl);
        builder.append(nl);

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

    protected String buildReadableSummary(PerformanceExecutionResult executionResult) {
        String nl = System.lineSeparator();
        StringBuilder builder = new StringBuilder();

        builder.append("=======================================================================").append(nl);
        builder.append("                 READABLE PERFORMANCE TEST SUMMARY").append(nl);
        builder.append("=======================================================================").append(nl);
        builder.append(nl);

        builder.append("What was tested?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Test Name: ").append(safe(executionResult.getTestName())).append(nl);
        builder.append("Purpose: ").append(safe(executionResult.getTestPurpose())).append(nl);
        builder.append("Test Type: ").append(safe(executionResult.getPerformanceTestType())).append(nl);
        builder.append("Goal: ").append(safe(executionResult.getTestGoal())).append(nl);
        builder.append("Target: ").append(safe(executionResult.getHttpMethod()))
                .append(" ")
                .append(safe(executionResult.getFullTargetUrl()))
                .append(nl);
        builder.append(nl);

        builder.append("How was it tested?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Users: ").append(executionResult.getUsers()).append(nl);
        builder.append("Ramp-up: ").append(executionResult.getRampUpSeconds()).append(" seconds").append(nl);
        builder.append("Hold time: ").append(executionResult.getHoldSeconds()).append(" seconds").append(nl);
        builder.append("Iterations: ").append(executionResult.getIterations()).append(nl);
        builder.append("Execution mode: ").append(safe(executionResult.getExecutionMode())).append(nl);
        builder.append("Total scenario duration: ")
                .append(PerformanceExcelFormatHelper.formatMillisecondsDetailed(executionResult.getTotalScenarioDurationMs()))
                .append(nl);
        builder.append(nl);

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

        builder.append("What happened?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Total requests sent: ").append(executionResult.getTotalSamples()).append(nl);
        builder.append("Failed requests: ").append(executionResult.getTotalErrors()).append(nl);
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

        builder.append("How risky is the result?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Execution status: ")
                .append(safe(executionResult.getExecutionStatus() == null ? null : executionResult.getExecutionStatus().name()))
                .append(nl);
        builder.append("Risk score: ").append(executionResult.getRiskScore()).append(nl);
        builder.append("Risk level: ").append(safe(executionResult.getRiskLevel())).append(nl);
        builder.append("Recommended action: ").append(safe(executionResult.getRecommendedAction())).append(nl);
        builder.append(nl);

        builder.append("How did the system behave?").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Response time assessment: ").append(safe(executionResult.getResponseTimeAssessment())).append(nl);
        builder.append("Error assessment: ").append(safe(executionResult.getErrorAssessment())).append(nl);
        builder.append("Stability assessment: ").append(safe(executionResult.getStabilityAssessment())).append(nl);
        builder.append("Where failures started: ").append(safe(executionResult.getFirstFailureIndicator())).append(nl);
        builder.append(nl);

        builder.append("Final result").append(nl);
        builder.append("-----------------------------------------------------------------------").append(nl);
        builder.append("Execution passed: ").append(executionResult.isExecutionPassed()).append(nl);
        builder.append("Expected failure mode: ").append(executionResult.isExpectedFailureMode()).append(nl);
        builder.append("Actual failure detected: ").append(executionResult.isActualFailureDetected()).append(nl);
        builder.append("Failure message: ").append(safe(executionResult.getFailureMessage())).append(nl);
        builder.append("Conclusion: ").append(safe(executionResult.getFinalConclusion())).append(nl);
        builder.append(nl);

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

    private void validateExecutionResult(PerformanceExecutionResult executionResult) {
        if (executionResult == null) {
            throw new IllegalArgumentException("PerformanceExecutionResult cannot be null.");
        }

        if (executionResult.getTestName() == null || executionResult.getTestName().isBlank()) {
            throw new IllegalArgumentException("PerformanceExecutionResult test name cannot be null or blank.");
        }
    }

    private void createParentDirectoryIfMissing(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create parent directories for summary file: " + path, e);
        }
    }

    private String safe(String value) {
        return PerformanceExcelFormatHelper.safeText(value);
    }
}