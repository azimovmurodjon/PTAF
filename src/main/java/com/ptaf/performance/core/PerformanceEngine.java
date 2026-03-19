package com.ptaf.performance.core;

import com.ptaf.performance.assertions.PerformanceAssertionEngine;
import com.ptaf.performance.builders.PerformanceTestPlanBuilder;
import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceExecutionStatus;
import com.ptaf.performance.models.PerformanceProfile;
import com.ptaf.performance.models.PerformanceRequest;
import com.ptaf.performance.models.PerformanceRunReport;
import com.ptaf.performance.reports.PerformanceExcelReportWriter;
import com.ptaf.performance.reports.PerformanceSummaryWriter;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central architect-controlled execution engine for PTAF performance testing.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>build and run JMeter DSL test plans</li>
 *   <li>parse JTL metrics</li>
 *   <li>evaluate assertion outcomes</li>
 *   <li>produce scenario-level result objects</li>
 *   <li>maintain one shared run-level report</li>
 *   <li>write TXT summaries and one Excel workbook per run</li>
 * </ul>
 * </p>
 *
 * <p>Reporting-safe goals:
 * <ul>
 *   <li>preserve existing execution behavior as much as possible</li>
 *   <li>keep Excel generation deterministic and thread-safe</li>
 *   <li>avoid partial corruption of run-level reporting state</li>
 *   <li>improve business-facing reporting inputs without breaking enterprise reuse</li>
 * </ul>
 * </p>
 */
public class PerformanceEngine {

    private static final Object RUN_LOCK = new Object();
    private static final AtomicInteger SCENARIO_SEQUENCE = new AtomicInteger(0);

    private static volatile String runRootFolderName;
    private static volatile Path runRootPath;
    private static volatile PerformanceRunReport currentRunReport;

    private static final String DEFAULT_REPORTS_BASE_DIR = "test-output-performance-reports";
    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";

    private final PerformanceAssertionEngine assertionEngine = new PerformanceAssertionEngine();
    private final PerformanceTestPlanBuilder testPlanBuilder = new PerformanceTestPlanBuilder();
    private final PerformanceSummaryWriter summaryWriter = new PerformanceSummaryWriter();
    private final PerformanceExcelReportWriter excelReportWriter = new PerformanceExcelReportWriter();

    /**
     * Internal token store for bearer-token alias support.
     */
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    public PerformanceExecutionResult runHttpTest(PerformanceRequest request) {
        return runHttpTest(
                request,
                PerformanceConfigurationProperties.getDefaultProfile(),
                PerformanceConfigurationProperties.getDefaultAssertionProfile(),
                false
        );
    }

    public PerformanceExecutionResult runHttpTest(PerformanceRequest request,
                                                  PerformanceProfile profile,
                                                  PerformanceAssertionProfile assertionProfile) {
        return runHttpTest(request, profile, assertionProfile, false);
    }

    public PerformanceExecutionResult runHttpTestExpectingFailure(PerformanceRequest request) {
        return runHttpTest(
                request,
                PerformanceConfigurationProperties.getDefaultProfile(),
                PerformanceConfigurationProperties.getDefaultAssertionProfile(),
                true
        );
    }

    public PerformanceExecutionResult runHttpTestExpectingFailure(PerformanceRequest request,
                                                                  PerformanceProfile profile,
                                                                  PerformanceAssertionProfile assertionProfile) {
        return runHttpTest(request, profile, assertionProfile, true);
    }

    public PerformanceExecutionResult runHttpTest(PerformanceRequest request,
                                                  PerformanceProfile profile,
                                                  PerformanceAssertionProfile assertionProfile,
                                                  boolean expectedFailureMode) {

        validateInputs(request, profile, assertionProfile);
        initializeRunFolderIfNeeded();

        int scenarioNumber = SCENARIO_SEQUENCE.incrementAndGet();
        String scenarioFolderName = String.format("%02d_%s", scenarioNumber, sanitizeName(request.getRequestName()));
        Path scenarioRootPath = runRootPath.resolve(scenarioFolderName);

        try {
            Files.createDirectories(scenarioRootPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create scenario report directory: " + scenarioRootPath, e);
        }

        Path jtlPath = scenarioRootPath.resolve("results.jtl");
        Path dashboardPath = scenarioRootPath.resolve("dashboard");
        Path summaryPath = scenarioRootPath.resolve("summary.txt");
        Path readableSummaryPath = scenarioRootPath.resolve("readable-summary.txt");

        long scenarioStartTimeMs = System.currentTimeMillis();

        long totalSamples = 0L;
        long totalErrors = 0L;
        double errorPercent = 0.0;
        long minResponseTimeMs = 0L;
        long averageResponseTimeMs = 0L;
        long p95ResponseTimeMs = 0L;
        long maxResponseTimeMs = 0L;
        long totalScenarioDurationMs;

        boolean executionPassed = false;
        boolean actualFailureDetected = false;
        String failureMessage = null;
        PerformanceExecutionStatus executionStatus;

        try {
            DslTestPlan testPlan = testPlanBuilder.buildHttpTestPlan(
                    request,
                    profile,
                    tokenStore,
                    jtlPath.toString(),
                    dashboardPath.toString(),
                    summaryPath.toString()
            );

            testPlan.run();

            JtlMetrics metrics = parseJtlMetrics(jtlPath);
            totalSamples = metrics.totalSamples;
            totalErrors = metrics.totalErrors;
            errorPercent = metrics.errorPercent;
            minResponseTimeMs = metrics.minResponseTimeMs;
            averageResponseTimeMs = metrics.averageResponseTimeMs;
            p95ResponseTimeMs = metrics.p95ResponseTimeMs;
            maxResponseTimeMs = metrics.maxResponseTimeMs;

            totalScenarioDurationMs = elapsedSince(scenarioStartTimeMs);

            PerformanceExecutionResult rawResult = buildFinalResult(
                    request,
                    profile,
                    assertionProfile,
                    totalScenarioDurationMs,
                    totalSamples,
                    totalErrors,
                    errorPercent,
                    minResponseTimeMs,
                    averageResponseTimeMs,
                    p95ResponseTimeMs,
                    maxResponseTimeMs,
                    dashboardPath,
                    jtlPath,
                    summaryPath,
                    readableSummaryPath,
                    resolveExecutionStatus(true, expectedFailureMode, false),
                    true,
                    expectedFailureMode,
                    false,
                    null
            );

            try {
                assertionEngine.validate(rawResult, assertionProfile);
                executionPassed = true;
                actualFailureDetected = false;
                executionStatus = resolveExecutionStatus(true, expectedFailureMode, false);

            } catch (AssertionError assertionError) {
                executionPassed = false;
                actualFailureDetected = true;
                failureMessage = safeMessage(assertionError);
                executionStatus = resolveExecutionStatus(false, expectedFailureMode, true);

                if (!expectedFailureMode) {
                    totalScenarioDurationMs = elapsedSince(scenarioStartTimeMs);

                    PerformanceExecutionResult failedAssertionResult = buildFinalResult(
                            request,
                            profile,
                            assertionProfile,
                            totalScenarioDurationMs,
                            totalSamples,
                            totalErrors,
                            errorPercent,
                            minResponseTimeMs,
                            averageResponseTimeMs,
                            p95ResponseTimeMs,
                            maxResponseTimeMs,
                            dashboardPath,
                            jtlPath,
                            summaryPath,
                            readableSummaryPath,
                            executionStatus,
                            false,
                            false,
                            true,
                            failureMessage
                    );

                    finalizeScenarioResult(failedAssertionResult);
                    throw assertionError;
                }
            }

        } catch (AssertionError e) {
            throw e;

        } catch (Exception e) {
            executionPassed = false;
            actualFailureDetected = true;
            failureMessage = safeMessage(e);
            executionStatus = resolveExecutionStatus(false, expectedFailureMode, true);
            totalScenarioDurationMs = elapsedSince(scenarioStartTimeMs);

            if (!expectedFailureMode) {
                PerformanceExecutionResult failedResult = buildFinalResult(
                        request,
                        profile,
                        assertionProfile,
                        totalScenarioDurationMs,
                        totalSamples,
                        totalErrors,
                        errorPercent,
                        minResponseTimeMs,
                        averageResponseTimeMs,
                        p95ResponseTimeMs,
                        maxResponseTimeMs,
                        dashboardPath,
                        jtlPath,
                        summaryPath,
                        readableSummaryPath,
                        executionStatus,
                        false,
                        false,
                        true,
                        failureMessage
                );

                finalizeScenarioResult(failedResult);
                throw new RuntimeException("Performance execution failed for test: " + request.getRequestName(), e);
            }
        }

        totalScenarioDurationMs = elapsedSince(scenarioStartTimeMs);
        executionStatus = resolveExecutionStatus(executionPassed, expectedFailureMode, actualFailureDetected);

        PerformanceExecutionResult finalResult = buildFinalResult(
                request,
                profile,
                assertionProfile,
                totalScenarioDurationMs,
                totalSamples,
                totalErrors,
                errorPercent,
                minResponseTimeMs,
                averageResponseTimeMs,
                p95ResponseTimeMs,
                maxResponseTimeMs,
                dashboardPath,
                jtlPath,
                summaryPath,
                readableSummaryPath,
                executionStatus,
                executionPassed,
                expectedFailureMode,
                actualFailureDetected,
                failureMessage
        );

        finalizeScenarioResult(finalResult);
        return finalResult;
    }

    // ============================================================
    // TOKEN STORAGE
    // ============================================================

    public void storeBearerToken(String alias, String tokenValue) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Token alias cannot be null or blank.");
        }

        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("Token value cannot be null or blank.");
        }

        tokenStore.put(alias, tokenValue);
    }

    public String getBearerToken(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }
        return tokenStore.get(alias);
    }

    public Map<String, String> getTokenStore() {
        return tokenStore;
    }

    public PerformanceRunReport getCurrentRunReport() {
        return currentRunReport;
    }

    // ============================================================
    // RUN / REPORT FINALIZATION
    // ============================================================

    private void finalizeScenarioResult(PerformanceExecutionResult result) {
        writeSummaries(result);
        writeAggregateRunSummary(result);
        addResultToRunReport(result);
        writeExcelRunReport();
    }

    private void addResultToRunReport(PerformanceExecutionResult result) {
        synchronized (RUN_LOCK) {
            if (currentRunReport == null) {
                currentRunReport = new PerformanceRunReport(
                        runRootFolderName,
                        runRootPath.toString(),
                        runRootFolderName
                );
            }
            currentRunReport.addScenarioResult(result);
        }
    }

    private void writeExcelRunReport() {
        synchronized (RUN_LOCK) {
            if (currentRunReport == null || !currentRunReport.hasScenarioResults()) {
                return;
            }
            excelReportWriter.writeRunReport(currentRunReport);
        }
    }

    // ============================================================
    // RESULT BUILDING
    // ============================================================

    private PerformanceExecutionResult buildFinalResult(PerformanceRequest request,
                                                        PerformanceProfile profile,
                                                        PerformanceAssertionProfile assertionProfile,
                                                        long totalScenarioDurationMs,
                                                        long totalSamples,
                                                        long totalErrors,
                                                        double errorPercent,
                                                        long minResponseTimeMs,
                                                        long averageResponseTimeMs,
                                                        long p95ResponseTimeMs,
                                                        long maxResponseTimeMs,
                                                        Path dashboardPath,
                                                        Path jtlPath,
                                                        Path summaryPath,
                                                        Path readableSummaryPath,
                                                        PerformanceExecutionStatus executionStatus,
                                                        boolean executionPassed,
                                                        boolean expectedFailureMode,
                                                        boolean actualFailureDetected,
                                                        String failureMessage) {

        String fullTargetUrl = buildFullUrl(request);
        String authType = resolveAuthType(request);
        String payloadSourceType = resolvePayloadSourceType(request);
        String payloadSourceDetails = resolvePayloadSourceDetails(request);
        String executionMode = resolveExecutionMode(profile);

        String testPurpose = buildTestPurpose(request, profile);
        String performanceTestType = buildPerformanceTestType(profile, expectedFailureMode);
        String testGoal = buildTestGoal(request, profile, expectedFailureMode);

        String responseTimeAssessment = buildResponseTimeAssessment(
                averageResponseTimeMs,
                p95ResponseTimeMs,
                maxResponseTimeMs
        );

        String errorAssessment = buildErrorAssessment(
                totalErrors,
                errorPercent,
                expectedFailureMode
        );

        String stabilityAssessment = buildStabilityAssessment(
                totalSamples,
                totalErrors,
                errorPercent,
                maxResponseTimeMs
        );

        String firstFailureIndicator = buildFirstFailureIndicator(
                totalErrors,
                errorPercent,
                failureMessage,
                expectedFailureMode
        );

        String thresholdBreachSummary = buildThresholdBreachSummary(
                errorPercent,
                averageResponseTimeMs,
                p95ResponseTimeMs,
                assertionProfile
        );

        int riskScore = calculateRiskScore(
                executionStatus,
                errorPercent,
                averageResponseTimeMs,
                p95ResponseTimeMs,
                maxResponseTimeMs,
                totalErrors,
                assertionProfile
        );

        String riskLevel = resolveRiskLevel(riskScore);

        String recommendedAction = buildRecommendedAction(
                executionStatus,
                riskScore,
                thresholdBreachSummary,
                expectedFailureMode
        );

        String finalConclusion = buildFinalConclusion(
                executionPassed,
                expectedFailureMode,
                actualFailureDetected,
                errorPercent,
                averageResponseTimeMs,
                p95ResponseTimeMs
        );

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
                safeNonNegative(profile.getUsers()),
                safeNonNegative(profile.getRampUpSeconds()),
                safeNonNegative(profile.getHoldSeconds()),
                safeNonNegative(profile.getIterations()),
                executionMode,
                safeNonNegative(assertionProfile.getMaxErrorPercent()),
                safeNonNegative(assertionProfile.getMaxAverageResponseTimeMs()),
                safeNonNegative(assertionProfile.getMaxP95ResponseTimeMs()),
                safeNonNegative(totalScenarioDurationMs),
                safeNonNegative(totalSamples),
                safeNonNegative(totalErrors),
                safeNonNegative(errorPercent),
                safeNonNegative(minResponseTimeMs),
                safeNonNegative(averageResponseTimeMs),
                safeNonNegative(p95ResponseTimeMs),
                safeNonNegative(maxResponseTimeMs),
                safeNonNegative(riskScore),
                riskLevel,
                thresholdBreachSummary,
                recommendedAction,
                responseTimeAssessment,
                errorAssessment,
                stabilityAssessment,
                firstFailureIndicator,
                finalConclusion,
                dashboardPath.toString(),
                jtlPath.toString(),
                summaryPath.toString(),
                readableSummaryPath.toString(),
                runRootPath.toString(),
                executionStatus,
                executionPassed,
                expectedFailureMode,
                actualFailureDetected,
                failureMessage
        );
    }

    // ============================================================
    // RUN ROOT MANAGEMENT
    // ============================================================

    private void initializeRunFolderIfNeeded() {
        if (runRootPath != null && currentRunReport != null) {
            return;
        }

        synchronized (RUN_LOCK) {
            if (runRootPath != null && currentRunReport != null) {
                return;
            }

            runRootFolderName = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd-MMM-yy_HH-mm-ss"));

            runRootPath = Paths.get(DEFAULT_REPORTS_BASE_DIR, runRootFolderName);

            try {
                Files.createDirectories(runRootPath);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create run-level performance report folder: " + runRootPath, e);
            }

            currentRunReport = new PerformanceRunReport(
                    runRootFolderName,
                    runRootPath.toString(),
                    runRootFolderName
            );
        }
    }

    // ============================================================
    // SUMMARY WRITING
    // ============================================================

    private void writeSummaries(PerformanceExecutionResult result) {
        try {
            summaryWriter.writeTextSummary(result);
            summaryWriter.writeReadableSummary(result);
        } catch (Exception e) {
            throw new RuntimeException("Unable to write performance summary files.", e);
        }
    }

    private void writeAggregateRunSummary(PerformanceExecutionResult result) {
        try {
            Path aggregateSummaryPath = runRootPath.resolve("run-summary.txt");
            Path readableAggregateSummaryPath = runRootPath.resolve("run-readable-summary.txt");
            Path runIndexPath = runRootPath.resolve("run-index.txt");

            appendRunSummary(aggregateSummaryPath, result);
            appendReadableRunSummary(readableAggregateSummaryPath, result);
            appendRunIndex(runIndexPath, result);

        } catch (Exception e) {
            throw new RuntimeException("Unable to write aggregate performance run summary.", e);
        }
    }

    private void appendRunSummary(Path aggregateSummaryPath, PerformanceExecutionResult result) throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        sb.append("=======================================================================").append(nl);
        sb.append("Test Name                : ").append(result.getSafeTestName()).append(nl);
        sb.append("Execution Status         : ").append(result.getExecutionStatus()).append(nl);
        sb.append("Risk Score               : ").append(result.getRiskScore()).append(nl);
        sb.append("Risk Level               : ").append(result.getRiskLevel()).append(nl);
        sb.append("Threshold Breach Summary : ").append(result.getThresholdBreachSummary()).append(nl);
        sb.append("Recommended Action       : ").append(result.getSafeRecommendedAction()).append(nl);
        sb.append("Primary Business Concern : ").append(result.getPrimaryBusinessConcern()).append(nl);
        sb.append("Attention Category       : ").append(result.getAttentionCategory()).append(nl);
        sb.append("Test Type                : ").append(result.getPerformanceTestType()).append(nl);
        sb.append("Purpose                  : ").append(result.getTestPurpose()).append(nl);
        sb.append("Goal                     : ").append(result.getTestGoal()).append(nl);
        sb.append("HTTP Method              : ").append(result.getHttpMethod()).append(nl);
        sb.append("Full URL                 : ").append(result.getFullTargetUrl()).append(nl);
        sb.append("Execution Mode           : ").append(result.getExecutionMode()).append(nl);
        sb.append("Users                    : ").append(result.getUsers()).append(nl);
        sb.append("Ramp-Up Seconds          : ").append(result.getRampUpSeconds()).append(nl);
        sb.append("Hold Seconds             : ").append(result.getHoldSeconds()).append(nl);
        sb.append("Iterations               : ").append(result.getIterations()).append(nl);
        sb.append("Max Allowed Error %      : ").append(result.getMaxAllowedErrorPercent()).append(nl);
        sb.append("Max Allowed Avg Resp ms  : ").append(result.getMaxAllowedAverageResponseTimeMs()).append(nl);
        sb.append("Max Allowed P95 Resp ms  : ").append(result.getMaxAllowedP95ResponseTimeMs()).append(nl);
        sb.append("Total Duration ms        : ").append(result.getTotalScenarioDurationMs()).append(nl);
        sb.append("Total Samples            : ").append(result.getTotalSamples()).append(nl);
        sb.append("Total Errors             : ").append(result.getTotalErrors()).append(nl);
        sb.append("Error Percent            : ").append(result.getErrorPercent()).append("%").append(nl);
        sb.append("Min Response Time        : ").append(result.getMinResponseTimeMs()).append(" ms").append(nl);
        sb.append("Average Response Time    : ").append(result.getAverageResponseTimeMs()).append(" ms").append(nl);
        sb.append("P95 Response Time        : ").append(result.getP95ResponseTimeMs()).append(" ms").append(nl);
        sb.append("Max Response Time        : ").append(result.getMaxResponseTimeMs()).append(" ms").append(nl);
        sb.append("Response Assessment      : ").append(result.getSafeResponseTimeAssessment()).append(nl);
        sb.append("Error Assessment         : ").append(result.getSafeErrorAssessment()).append(nl);
        sb.append("Stability Assessment     : ").append(result.getSafeStabilityAssessment()).append(nl);
        sb.append("First Failure Indicator  : ").append(result.getSafeFirstFailureIndicator()).append(nl);
        sb.append("Final Conclusion         : ").append(result.getSafeFinalConclusion()).append(nl);
        sb.append("Execution Passed         : ").append(result.isExecutionPassed()).append(nl);
        sb.append("Expected Failure Mode    : ").append(result.isExpectedFailureMode()).append(nl);
        sb.append("Actual Failure Detected  : ").append(result.isActualFailureDetected()).append(nl);
        sb.append("Failure Message          : ").append(safeValue(result.getSafeFailureMessage())).append(nl);
        sb.append("Dashboard Path           : ").append(result.getDashboardPath()).append(nl);
        sb.append("JTL Path                 : ").append(result.getJtlFilePath()).append(nl);
        sb.append("Scenario Summary         : ").append(result.getSummaryFilePath()).append(nl);
        sb.append("Readable Summary         : ").append(result.getReadableSummaryFilePath()).append(nl);
        sb.append("=======================================================================").append(nl);

        Files.writeString(
                aggregateSummaryPath,
                sb.toString(),
                StandardCharsets.UTF_8,
                Files.exists(aggregateSummaryPath)
                        ? java.nio.file.StandardOpenOption.APPEND
                        : java.nio.file.StandardOpenOption.CREATE
        );
    }

    private void appendReadableRunSummary(Path readableAggregateSummaryPath,
                                          PerformanceExecutionResult result) throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        sb.append("=======================================================================").append(nl);
        sb.append("Test Name: ").append(result.getSafeTestName()).append(nl);
        sb.append("Business Outcome: ").append(result.getBusinessOutcomeLabel()).append(nl);
        sb.append("Execution Status: ").append(result.getExecutionStatus()).append(nl);
        sb.append("Risk Score: ").append(result.getRiskScore()).append(nl);
        sb.append("Risk Level: ").append(result.getRiskLevel()).append(nl);
        sb.append("Primary concern: ").append(result.getPrimaryBusinessConcern()).append(nl);
        sb.append("Thresholds exceeded: ").append(result.getThresholdBreachSummary()).append(nl);
        sb.append("Recommended action: ").append(result.getSafeRecommendedAction()).append(nl);
        sb.append("What was tested: ").append(result.getHttpMethod()).append(" ").append(result.getFullTargetUrl()).append(nl);
        sb.append("Test type: ").append(result.getPerformanceTestType()).append(nl);
        sb.append("Purpose: ").append(result.getTestPurpose()).append(nl);
        sb.append("Goal: ").append(result.getTestGoal()).append(nl);
        sb.append("Users: ").append(result.getUsers()).append(nl);
        sb.append("Ramp-up: ").append(result.getRampUpSeconds()).append(" seconds").append(nl);
        sb.append("Hold time: ").append(result.getHoldSeconds()).append(" seconds").append(nl);
        sb.append("Iterations: ").append(result.getIterations()).append(nl);
        sb.append("Allowed Error %: ").append(result.getMaxAllowedErrorPercent()).append(nl);
        sb.append("Allowed Avg Response ms: ").append(result.getMaxAllowedAverageResponseTimeMs()).append(nl);
        sb.append("Allowed P95 Response ms: ").append(result.getMaxAllowedP95ResponseTimeMs()).append(nl);
        sb.append("Total scenario duration ms: ").append(result.getTotalScenarioDurationMs()).append(nl);
        sb.append("Requests sent: ").append(result.getTotalSamples()).append(nl);
        sb.append("Failures: ").append(result.getTotalErrors()).append(nl);
        sb.append("Failure rate: ").append(result.getErrorPercent()).append("%").append(nl);
        sb.append("Fastest response: ").append(result.getMinResponseTimeMs()).append(" ms").append(nl);
        sb.append("Average response: ").append(result.getAverageResponseTimeMs()).append(" ms").append(nl);
        sb.append("95% of responses were under: ").append(result.getP95ResponseTimeMs()).append(" ms").append(nl);
        sb.append("Slowest response: ").append(result.getMaxResponseTimeMs()).append(" ms").append(nl);
        sb.append("System behavior: ").append(result.getSafeStabilityAssessment()).append(nl);
        sb.append("Where failures started: ").append(result.getSafeFirstFailureIndicator()).append(nl);
        sb.append("Conclusion: ").append(result.getSafeFinalConclusion()).append(nl);
        sb.append("Passed: ").append(result.isExecutionPassed()).append(nl);
        sb.append("Failure message: ").append(safeValue(result.getSafeFailureMessage())).append(nl);
        sb.append("Readable summary file: ").append(result.getReadableSummaryFilePath()).append(nl);
        sb.append("=======================================================================").append(nl);

        Files.writeString(
                readableAggregateSummaryPath,
                sb.toString(),
                StandardCharsets.UTF_8,
                Files.exists(readableAggregateSummaryPath)
                        ? java.nio.file.StandardOpenOption.APPEND
                        : java.nio.file.StandardOpenOption.CREATE
        );
    }

    private void appendRunIndex(Path runIndexPath, PerformanceExecutionResult result) throws IOException {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();

        sb.append("Test Name           : ").append(result.getSafeTestName()).append(nl);
        sb.append("Business Outcome    : ").append(result.getBusinessOutcomeLabel()).append(nl);
        sb.append("Execution Status    : ").append(result.getExecutionStatus()).append(nl);
        sb.append("Risk Score          : ").append(result.getRiskScore()).append(nl);
        sb.append("Risk Level          : ").append(result.getRiskLevel()).append(nl);
        sb.append("Attention Category  : ").append(result.getAttentionCategory()).append(nl);
        sb.append("Threshold Breaches  : ").append(result.getThresholdBreachSummary()).append(nl);
        sb.append("Recommended Action  : ").append(result.getSafeRecommendedAction()).append(nl);
        sb.append("Test Type           : ").append(result.getPerformanceTestType()).append(nl);
        sb.append("Execution Passed    : ").append(result.isExecutionPassed()).append(nl);
        sb.append("Expected Failure    : ").append(result.isExpectedFailureMode()).append(nl);
        sb.append("Actual Failure      : ").append(result.isActualFailureDetected()).append(nl);
        sb.append("Allowed Error %     : ").append(result.getMaxAllowedErrorPercent()).append(nl);
        sb.append("Allowed Avg ms      : ").append(result.getMaxAllowedAverageResponseTimeMs()).append(nl);
        sb.append("Allowed P95 ms      : ").append(result.getMaxAllowedP95ResponseTimeMs()).append(nl);
        sb.append("Total Duration ms   : ").append(result.getTotalScenarioDurationMs()).append(nl);
        sb.append("Error Percent       : ").append(result.getErrorPercent()).append("%").append(nl);
        sb.append("Total Samples       : ").append(result.getTotalSamples()).append(nl);
        sb.append("Total Errors        : ").append(result.getTotalErrors()).append(nl);
        sb.append("Min Response Time   : ").append(result.getMinResponseTimeMs()).append(" ms").append(nl);
        sb.append("Avg Response Time   : ").append(result.getAverageResponseTimeMs()).append(" ms").append(nl);
        sb.append("P95 Response Time   : ").append(result.getP95ResponseTimeMs()).append(" ms").append(nl);
        sb.append("Max Response Time   : ").append(result.getMaxResponseTimeMs()).append(" ms").append(nl);
        sb.append("Dashboard Path      : ").append(result.getDashboardPath()).append(nl);
        sb.append("JTL Path            : ").append(result.getJtlFilePath()).append(nl);
        sb.append("Summary Path        : ").append(result.getSummaryFilePath()).append(nl);
        sb.append("Readable Summary    : ").append(result.getReadableSummaryFilePath()).append(nl);
        sb.append("Failure Message     : ").append(safeValue(result.getSafeFailureMessage())).append(nl);
        sb.append("------------------------------------------------------------").append(nl);

        Files.writeString(
                runIndexPath,
                sb.toString(),
                StandardCharsets.UTF_8,
                Files.exists(runIndexPath)
                        ? java.nio.file.StandardOpenOption.APPEND
                        : java.nio.file.StandardOpenOption.CREATE
        );
    }

    // ============================================================
    // JTL PARSING
    // ============================================================

    private JtlMetrics parseJtlMetrics(Path jtlPath) {
        if (jtlPath == null || !Files.exists(jtlPath)) {
            return new JtlMetrics(0, 0, 0.0, 0, 0, 0, 0);
        }

        try {
            String content = Files.readString(jtlPath, StandardCharsets.UTF_8).trim();

            if (content.isBlank()) {
                return new JtlMetrics(0, 0, 0.0, 0, 0, 0, 0);
            }

            if (content.startsWith("<?xml") || content.startsWith("<testResults")) {
                return parseXmlJtl(content);
            }

            return parseCsvJtl(content);

        } catch (Exception e) {
            throw new RuntimeException("Unable to parse JTL metrics from file: " + jtlPath, e);
        }
    }

    private JtlMetrics parseCsvJtl(String content) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new JtlMetrics(0, 0, 0.0, 0, 0, 0, 0);
            }

            String[] headers = headerLine.split(",", -1);
            int elapsedIndex = indexOf(headers, "elapsed");
            int successIndex = indexOf(headers, "success");

            if (elapsedIndex < 0 || successIndex < 0) {
                return new JtlMetrics(0, 0, 0.0, 0, 0, 0, 0);
            }

            long totalSamples = 0;
            long totalErrors = 0;
            long totalElapsed = 0;
            List<Long> elapsedValues = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length <= Math.max(elapsedIndex, successIndex)) {
                    continue;
                }

                long elapsed = parseLongSafe(parts[elapsedIndex]);
                boolean success = Boolean.parseBoolean(parts[successIndex].trim());

                totalSamples++;
                totalElapsed += elapsed;
                elapsedValues.add(elapsed);

                if (!success) {
                    totalErrors++;
                }
            }

            return buildMetrics(totalSamples, totalErrors, totalElapsed, elapsedValues);
        }
    }

    private JtlMetrics parseXmlJtl(String content) {
        Pattern pattern = Pattern.compile("<(?:httpSample|sample)[^>]*t=\"(\\d+)\"[^>]*s=\"(true|false)\"[^>]*/?>");
        Matcher matcher = pattern.matcher(content);

        long totalSamples = 0;
        long totalErrors = 0;
        long totalElapsed = 0;
        List<Long> elapsedValues = new ArrayList<>();

        while (matcher.find()) {
            long elapsed = parseLongSafe(matcher.group(1));
            boolean success = Boolean.parseBoolean(matcher.group(2));

            totalSamples++;
            totalElapsed += elapsed;
            elapsedValues.add(elapsed);

            if (!success) {
                totalErrors++;
            }
        }

        return buildMetrics(totalSamples, totalErrors, totalElapsed, elapsedValues);
    }

    private JtlMetrics buildMetrics(long totalSamples,
                                    long totalErrors,
                                    long totalElapsed,
                                    List<Long> elapsedValues) {

        double errorPercent = totalSamples == 0 ? 0.0 : ((double) totalErrors * 100.0) / totalSamples;
        long averageResponseTimeMs = totalSamples == 0 ? 0 : Math.round((double) totalElapsed / totalSamples);
        long p95ResponseTimeMs = calculatePercentile(elapsedValues, 95);
        long minResponseTimeMs = elapsedValues.isEmpty() ? 0L : elapsedValues.stream().min(Long::compareTo).orElse(0L);
        long maxResponseTimeMs = elapsedValues.isEmpty() ? 0L : elapsedValues.stream().max(Long::compareTo).orElse(0L);

        return new JtlMetrics(
                totalSamples,
                totalErrors,
                errorPercent,
                minResponseTimeMs,
                averageResponseTimeMs,
                p95ResponseTimeMs,
                maxResponseTimeMs
        );
    }

    private long calculatePercentile(List<Long> values, int percentile) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }

        values.sort(Long::compareTo);
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;

        if (index < 0) {
            index = 0;
        }
        if (index >= values.size()) {
            index = values.size() - 1;
        }

        return values.get(index);
    }

    private int indexOf(String[] headers, String target) {
        for (int i = 0; i < headers.length; i++) {
            if (target.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private long parseLongSafe(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    // ============================================================
    // REPORT INTERPRETATION BUILDERS
    // ============================================================

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

    private PerformanceExecutionStatus resolveExecutionStatus(boolean executionPassed,
                                                              boolean expectedFailureMode,
                                                              boolean actualFailureDetected) {
        if (expectedFailureMode) {
            if (actualFailureDetected) {
                return PerformanceExecutionStatus.EXPECTED_FAIL_CONFIRMED;
            }
            return PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED;
        }

        return executionPassed ? PerformanceExecutionStatus.PASS : PerformanceExecutionStatus.FAIL;
    }

    private String buildResponseTimeAssessment(long avg, long p95, long max) {
        if (avg == 0 && p95 == 0 && max == 0) {
            return "No measurable response-time data was captured.";
        }

        if (p95 <= 1000 && max <= 3000) {
            return "Response times were strong and consistent. Most requests completed quickly without major spikes.";
        }

        if (p95 <= 3000 && max <= 7000) {
            return "Response times were acceptable, though some slower requests appeared under load.";
        }

        if (p95 <= 8000) {
            return "Response times showed noticeable slowdown. Users may feel delay during heavier traffic.";
        }

        return "Response times were poor and indicate clear performance degradation under this execution profile.";
    }

    private String buildErrorAssessment(long totalErrors, double errorPercent, boolean expectedFailureMode) {
        if (expectedFailureMode) {
            if (totalErrors > 0) {
                return "Failures were detected as expected for this negative validation scenario.";
            }
            return "No failures were detected, even though failure behavior was expected.";
        }

        if (totalErrors == 0) {
            return "No request failures were detected.";
        }

        if (errorPercent < 1.0) {
            return "A very small number of failures occurred. Stability is mostly good but should be reviewed.";
        }

        if (errorPercent < 5.0) {
            return "A moderate level of request failure occurred. This may impact user experience and should be investigated.";
        }

        return "Failure rate was high and indicates unstable behavior under this test condition.";
    }

    private String buildStabilityAssessment(long totalSamples,
                                            long totalErrors,
                                            double errorPercent,
                                            long maxResponseTimeMs) {
        if (totalSamples == 0) {
            return "No samples were recorded, so system stability could not be assessed.";
        }

        if (totalErrors == 0 && maxResponseTimeMs <= 3000) {
            return "System remained stable throughout the run with no observed failures and controlled response times.";
        }

        if (errorPercent <= 2.0 && maxResponseTimeMs <= 8000) {
            return "System was mostly stable, but some instability or slower edge-case responses appeared.";
        }

        if (errorPercent <= 5.0) {
            return "System showed visible instability during the run and should be reviewed before higher traffic testing.";
        }

        return "System stability broke down under this execution profile.";
    }

    private String buildFirstFailureIndicator(long totalErrors,
                                              double errorPercent,
                                              String failureMessage,
                                              boolean expectedFailureMode) {
        if (totalErrors == 0 && isBlank(failureMessage)) {
            return expectedFailureMode
                    ? "No failure point was captured, even though failure behavior was expected."
                    : "No failure point detected during execution.";
        }

        if (!isBlank(failureMessage)) {
            return "Framework detected failure with message: " + failureMessage;
        }

        if (errorPercent > 0) {
            return "Failures started once requests began returning unsuccessful responses in the sampled traffic.";
        }

        return "Failure indicator is not available.";
    }

    private String buildThresholdBreachSummary(double errorPercent,
                                               long averageResponseTimeMs,
                                               long p95ResponseTimeMs,
                                               PerformanceAssertionProfile assertionProfile) {
        List<String> breaches = new ArrayList<>();

        if (errorPercent > assertionProfile.getMaxErrorPercent()) {
            breaches.add("Error % exceeded");
        }
        if (averageResponseTimeMs > assertionProfile.getMaxAverageResponseTimeMs()) {
            breaches.add("Average response exceeded");
        }
        if (p95ResponseTimeMs > assertionProfile.getMaxP95ResponseTimeMs()) {
            breaches.add("P95 response exceeded");
        }

        if (breaches.isEmpty()) {
            return NO_THRESHOLD_BREACHES;
        }

        return String.join("; ", breaches);
    }

    private int calculateRiskScore(PerformanceExecutionStatus executionStatus,
                                   double errorPercent,
                                   long averageResponseTimeMs,
                                   long p95ResponseTimeMs,
                                   long maxResponseTimeMs,
                                   long totalErrors,
                                   PerformanceAssertionProfile assertionProfile) {

        int score = 0;

        if (executionStatus == PerformanceExecutionStatus.FAIL) {
            score += 45;
        } else if (executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED) {
            score += 35;
        } else if (executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_CONFIRMED) {
            score += 10;
        }

        if (totalErrors > 0) {
            score += 10;
        }

        if (errorPercent > assertionProfile.getMaxErrorPercent()) {
            score += 20;
        } else if (errorPercent > 0) {
            score += 5;
        }

        if (averageResponseTimeMs > assertionProfile.getMaxAverageResponseTimeMs()) {
            score += 15;
        }

        if (p95ResponseTimeMs > assertionProfile.getMaxP95ResponseTimeMs()) {
            score += 20;
        }

        if (maxResponseTimeMs > assertionProfile.getMaxP95ResponseTimeMs() * 2L) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private String resolveRiskLevel(int riskScore) {
        if (riskScore >= 81) {
            return "Critical";
        }
        if (riskScore >= 51) {
            return "High";
        }
        if (riskScore >= 21) {
            return "Medium";
        }
        return "Low";
    }

    private String buildRecommendedAction(PerformanceExecutionStatus executionStatus,
                                          int riskScore,
                                          String thresholdBreachSummary,
                                          boolean expectedFailureMode) {

        if (expectedFailureMode && executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_CONFIRMED) {
            return "No action required. Expected-failure behavior was confirmed.";
        }

        if (executionStatus == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED) {
            return "Review negative test design or endpoint behavior. Expected failure did not occur.";
        }

        if (riskScore >= 81) {
            return "Immediate investigation required before release. Do not promote without root-cause analysis.";
        }

        if (riskScore >= 51) {
            return "Investigate before release. Performance thresholds were exceeded or instability was detected.";
        }

        if (riskScore >= 21) {
            return "Monitor closely and review " + thresholdBreachSummary;
        }

        return "No immediate action required. Scenario appears healthy within configured thresholds.";
    }

    private String buildFinalConclusion(boolean executionPassed,
                                        boolean expectedFailureMode,
                                        boolean actualFailureDetected,
                                        double errorPercent,
                                        long averageResponseTimeMs,
                                        long p95ResponseTimeMs) {
        if (expectedFailureMode) {
            if (actualFailureDetected) {
                return "Expected-failure scenario behaved correctly. The framework captured and reported the failure condition.";
            }
            return "Expected-failure scenario did not produce the failure behavior that was expected.";
        }

        if (executionPassed) {
            return "Performance execution passed. The endpoint stayed within configured thresholds for failure rate and response time.";
        }

        if (errorPercent > 0 && p95ResponseTimeMs > 0) {
            return "Performance execution failed because the system exceeded allowed stability and/or latency thresholds.";
        }

        if (averageResponseTimeMs > 0) {
            return "Performance execution failed mainly due to response-time threshold violations.";
        }

        return "Performance execution failed. Review failure message and generated artifacts for root cause.";
    }

    private String buildFullUrl(PerformanceRequest request) {
        String protocol = safeValue(request.getProtocol());
        String host = safeValue(request.getHost());
        String path = safeValue(request.getPath());

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return protocol + "://" + host + ":" + request.getPort() + normalizedPath;
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void validateInputs(PerformanceRequest request,
                                PerformanceProfile profile,
                                PerformanceAssertionProfile assertionProfile) {
        if (request == null) {
            throw new IllegalArgumentException("Performance request cannot be null.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("Performance profile cannot be null.");
        }

        if (assertionProfile == null) {
            throw new IllegalArgumentException("Performance assertion profile cannot be null.");
        }
    }

    private String sanitizeName(String input) {
        if (input == null || input.isBlank()) {
            return "unnamed_test";
        }

        return input.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private long elapsedSince(long startTimeMs) {
        return Math.max(0L, System.currentTimeMillis() - startTimeMs);
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "No failure message available.";
        }
        return throwable.getMessage().trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int safeNonNegative(int value) {
        return Math.max(value, 0);
    }

    private long safeNonNegative(long value) {
        return Math.max(value, 0L);
    }

    private double safeNonNegative(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    private static class JtlMetrics {
        private final long totalSamples;
        private final long totalErrors;
        private final double errorPercent;
        private final long minResponseTimeMs;
        private final long averageResponseTimeMs;
        private final long p95ResponseTimeMs;
        private final long maxResponseTimeMs;

        private JtlMetrics(long totalSamples,
                           long totalErrors,
                           double errorPercent,
                           long minResponseTimeMs,
                           long averageResponseTimeMs,
                           long p95ResponseTimeMs,
                           long maxResponseTimeMs) {
            this.totalSamples = totalSamples;
            this.totalErrors = totalErrors;
            this.errorPercent = errorPercent;
            this.minResponseTimeMs = minResponseTimeMs;
            this.averageResponseTimeMs = averageResponseTimeMs;
            this.p95ResponseTimeMs = p95ResponseTimeMs;
            this.maxResponseTimeMs = maxResponseTimeMs;
        }
    }
}