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

    /**
     * Global lock object used to protect run-level shared state (run root folder and current run report).
     * Synchronize on this object when mutating or reading run-level resources to ensure thread-safety.
     */
    private static final Object RUN_LOCK = new Object();

    /**
     * Sequence generator for scenario numbers within a run. Ensures stable, increasing scenario numbering.
     */
    private static final AtomicInteger SCENARIO_SEQUENCE = new AtomicInteger(0);

    /**
     * Name of the root folder for the current run. Volatile because it can be lazily initialized and read from different threads.
     */
    private static volatile String runRootFolderName;

    /**
     * Filesystem path for the current run root. Volatile for safe publication across threads after initialization.
     */
    private static volatile Path runRootPath;

    /**
     * Shared run-level report containing aggregated scenario results for the current run.
     * Volatile for safe publication across threads.
     */
    private static volatile PerformanceRunReport currentRunReport;

    /**
     * Base directory where run folders are created when creating new performance run reports.
     */
    private static final String DEFAULT_REPORTS_BASE_DIR = "test-output-performance-reports";

    /**
     * Constant used when no threshold breaches are detected in assertions/metrics.
     */
    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";

    /**
     * Engine that evaluates configured performance assertions against scenario results.
     */
    private final PerformanceAssertionEngine assertionEngine = new PerformanceAssertionEngine();

    /**
     * Builder used to generate JMeter DSL test plans from request/profile information.
     */
    private final PerformanceTestPlanBuilder testPlanBuilder = new PerformanceTestPlanBuilder();

    /**
     * Writer responsible for generating per-scenario text summaries.
     */
    private final PerformanceSummaryWriter summaryWriter = new PerformanceSummaryWriter();

    /**
     * Writer responsible for generating the Excel workbook that aggregates the run report.
     */
    private final PerformanceExcelReportWriter excelReportWriter = new PerformanceExcelReportWriter();

    /**
     * Internal token store for bearer-token alias support. ConcurrentHashMap for thread-safe access.
     */
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();

    /**
     * Run a HTTP performance test using default profile and default assertion profile.
     *
     * This is a convenience overload commonly used by tests that do not require custom profiles.
     *
     * @param request PerformanceRequest describing the endpoint and payload to test
     * @return PerformanceExecutionResult containing detailed metrics, assessments and report paths
     */
    public PerformanceExecutionResult runHttpTest(PerformanceRequest request) {
        return runHttpTest(
                request,
                PerformanceConfigurationProperties.getDefaultProfile(),
                PerformanceConfigurationProperties.getDefaultAssertionProfile(),
                false
        );
    }

    /**
     * Run a HTTP performance test with a provided profile and assertion profile.
     *
     * @param request HTTP test request details
     * @param profile load/stress profile parameters (users, ramp, hold, iterations)
     * @param assertionProfile thresholds and assertion configuration
     * @return PerformanceExecutionResult the finalized scenario result
     */
    public PerformanceExecutionResult runHttpTest(PerformanceRequest request,
                                                  PerformanceProfile profile,
                                                  PerformanceAssertionProfile assertionProfile) {
        return runHttpTest(request, profile, assertionProfile, false);
    }

    /**
     * Run a HTTP performance test that is expected to fail (negative/expected failure test),
     * using default profiles.
     *
     * Behavior note: When expectedFailureMode is true, test failures will not throw runtime exceptions
     * and are reported as EXPECTED_FAIL_CONFIRMED vs EXPECTED_FAIL_NOT_TRIGGERED.
     *
     * @param request test request details
     * @return PerformanceExecutionResult the finalized scenario result
     */
    public PerformanceExecutionResult runHttpTestExpectingFailure(PerformanceRequest request) {
        return runHttpTest(
                request,
                PerformanceConfigurationProperties.getDefaultProfile(),
                PerformanceConfigurationProperties.getDefaultAssertionProfile(),
                true
        );
    }

    /**
     * Run a HTTP performance test that is expected to fail (negative/expected failure test)
     * with explicit profile parameters.
     *
     * @param request HTTP test request details
     * @param profile execution profile
     * @param assertionProfile assertion thresholds
     * @return PerformanceExecutionResult scenario result object
     */
    public PerformanceExecutionResult runHttpTestExpectingFailure(PerformanceRequest request,
                                                                  PerformanceProfile profile,
                                                                  PerformanceAssertionProfile assertionProfile) {
        return runHttpTest(request, profile, assertionProfile, true);
    }

    /**
     * Core method that executes a HTTP performance scenario and produces a detailed result.
     *
     * Flow:
     * - validate inputs and initialize run folder if needed
     * - create scenario folder and configure output artifacts (JTL, dashboard, summaries)
     * - build and execute a JMeter DSL test plan (synchronously)
     * - parse produced JTL metrics (CSV or XML)
     * - build an initial raw result and evaluate assertions via assertionEngine
     * - handle assertion failures and exceptions with expected-failure semantics
     * - build final result, write summaries, add to run-level report and produce Excel
     *
     * Important: This method preserves existing behavior where assertion failures
     * can be re-thrown for normal tests and are suppressed or treated differently when
     * expectedFailureMode is true.
     *
     * @param request HTTP test request to execute
     * @param profile execution profile (users, ramp, hold, iterations)
     * @param assertionProfile thresholds used to create business-facing assessments
     * @param expectedFailureMode when true, indicates test is a negative scenario that should produce failures
     * @return PerformanceExecutionResult finalized scenario-level result
     */
    public PerformanceExecutionResult runHttpTest(PerformanceRequest request,
                                                  PerformanceProfile profile,
                                                  PerformanceAssertionProfile assertionProfile,
                                                  boolean expectedFailureMode) {

        // Validate essential inputs before touching filesystem or executing tests
        validateInputs(request, profile, assertionProfile);

        // Lazily initialize the run-level folder and run report (shared between scenarios)
        initializeRunFolderIfNeeded();

        // assign a stable scenario number and build a sanitized folder name to store scenario artifacts
        int scenarioNumber = SCENARIO_SEQUENCE.incrementAndGet();
        String scenarioFolderName = String.format("%02d_%s", scenarioNumber, sanitizeName(request.getRequestName()));
        Path scenarioRootPath = runRootPath.resolve(scenarioFolderName);

        try {
            Files.createDirectories(scenarioRootPath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create scenario report directory: " + scenarioRootPath, e);
        }

        // Standard artifact paths for JMeter run outputs and human-readable summaries
        Path jtlPath = scenarioRootPath.resolve("results.jtl");
        Path dashboardPath = scenarioRootPath.resolve("dashboard");
        Path summaryPath = scenarioRootPath.resolve("summary.txt");
        Path readableSummaryPath = scenarioRootPath.resolve("readable-summary.txt");

        // Capture scenario start time for elapsed calculations
        long scenarioStartTimeMs = System.currentTimeMillis();

        // Initialize metric variables used for building result (defaults in case of failures)
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
            // Build and run the JMeter DSL test plan. The DSL will generate JTL/dashboard/summary files.
            DslTestPlan testPlan = testPlanBuilder.buildHttpTestPlan(
                    request,
                    profile,
                    tokenStore,
                    jtlPath.toString(),
                    dashboardPath.toString(),
                    summaryPath.toString()
            );

            // Execute the test plan synchronously. This will block until test plan completion.
            testPlan.run();

            // After run completion, parse the JTL file to extract sample and timing metrics
            JtlMetrics metrics = parseJtlMetrics(jtlPath);
            totalSamples = metrics.totalSamples;
            totalErrors = metrics.totalErrors;
            errorPercent = metrics.errorPercent;
            minResponseTimeMs = metrics.minResponseTimeMs;
            averageResponseTimeMs = metrics.averageResponseTimeMs;
            p95ResponseTimeMs = metrics.p95ResponseTimeMs;
            maxResponseTimeMs = metrics.maxResponseTimeMs;

            // Measure scenario duration so we can include it in the raw result
            totalScenarioDurationMs = elapsedSince(scenarioStartTimeMs);

            // Build an initial raw result representing the test execution before assertion validation
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
                // Run assertion evaluation. If assertions fail, assertionEngine throws AssertionError.
                assertionEngine.validate(rawResult, assertionProfile);
                executionPassed = true;
                actualFailureDetected = false;
                executionStatus = resolveExecutionStatus(true, expectedFailureMode, false);

            } catch (AssertionError assertionError) {
                // Handle assertion failure: mark scenario failed and capture failure message
                executionPassed = false;
                actualFailureDetected = true;
                failureMessage = safeMessage(assertionError);
                executionStatus = resolveExecutionStatus(false, expectedFailureMode, true);

                // If this was not an expected-failure scenario, finalize and rethrow the assertion so callers (tests) fail fast.
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

                    // Finalize reporting for this scenario before failing the test run
                    finalizeScenarioResult(failedAssertionResult);
                    throw assertionError;
                }
            }

        } catch (AssertionError e) {
            // Propagate assertion errors directly (already handled above for non-expected-fail cases)
            throw e;

        } catch (Exception e) {
            // Non-assertion exceptions (e.g., IO, builder errors, runtime). Capture and handle.
            executionPassed = false;
            actualFailureDetected = true;
            failureMessage = safeMessage(e);
            executionStatus = resolveExecutionStatus(false, expectedFailureMode, true);
            totalScenarioDurationMs = elapsedSince(scenarioStartTimeMs);

            // If not an expected-failure scenario, finalize scenario reporting and rethrow as RuntimeException
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

        // Build the final result after successful execution or when expectedFailureMode is true.
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

        // Final reporting (writes summaries, aggregates to run report, writes Excel)
        finalizeScenarioResult(finalResult);
        return finalResult;
    }

    // ============================================================
    // TOKEN STORAGE
    // ============================================================

    /**
     * Store a bearer token value under a short alias to be used by future requests.
     * Useful for test flows where an authorization token is fetched once and reused.
     *
     * @param alias alias to reference the token
     * @param tokenValue actual bearer token string
     * @throws IllegalArgumentException if alias or tokenValue are null/blank
     */
    public void storeBearerToken(String alias, String tokenValue) {
        if (alias == null || alias.isBlank()) {
            throw new IllegalArgumentException("Token alias cannot be null or blank.");
        }

        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("Token value cannot be null or blank.");
        }

        tokenStore.put(alias, tokenValue);
    }

    /**
     * Retrieve a previously stored bearer token by alias.
     *
     * @param alias token alias
     * @return token value or null if alias is null/blank or not found
     */
    public String getBearerToken(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }
        return tokenStore.get(alias);
    }

    /**
     * Expose the internal token store. Returned map is the live ConcurrentHashMap used by the engine.
     *
     * Tests may inspect tokens stored during multi-step scenarios.
     *
     * @return map of alias -> token
     */
    public Map<String, String> getTokenStore() {
        return tokenStore;
    }

    /**
     * Return the current run-level aggregated PerformanceRunReport instance.
     * May be null if no run has been initialized.
     *
     * @return current run report or null
     */
    public PerformanceRunReport getCurrentRunReport() {
        return currentRunReport;
    }

    // ============================================================
    // RUN / REPORT FINALIZATION
    // ============================================================

    /**
     * Top-level scenario finalization flow. Performs:
     * - per-scenario text summaries
     * - appends scenario to aggregate run summaries and index
     * - adds scenario to run-level in-memory report
     * - writes Excel workbook containing run-level report
     *
     * Any errors thrown here are propagated as RuntimeExceptions to keep behavior explicit.
     *
     * @param result finalized scenario result to be published
     */
    private void finalizeScenarioResult(PerformanceExecutionResult result) {
        writeSummaries(result);
        writeAggregateRunSummary(result);
        addResultToRunReport(result);
        writeExcelRunReport();
    }

    /**
     * Add a scenario result into the run-level report in a thread-safe manner.
     * Initializes the run report if it does not exist yet.
     *
     * @param result scenario result to add
     */
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

    /**
     * Write (or rewrite) the Excel workbook that summarizes the run-level results.
     *
     * Execution is synchronized to avoid concurrent writes to the same workbook.
     * If there are no scenario results, the method returns without writing.
     */
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

    /**
     * Build a detailed PerformanceExecutionResult object including:
     * - source request/profile/assertions
     * - metrics and percentile calculations
     * - human-friendly assessments and recommended actions
     * - artifact paths and execution flags
     *
     * This method centralizes the logic used both for successful runs and failure/capture cases,
     * producing a consistent object for downstream reporting.
     *
     * @param request request under test
     * @param profile execution profile
     * @param assertionProfile assertion thresholds used
     * @param totalScenarioDurationMs elapsed ms for the scenario
     * @param totalSamples number of requests/samples recorded
     * @param totalErrors number of failed samples recorded
     * @param errorPercent failure percentage computed
     * @param minResponseTimeMs minimum response time
     * @param averageResponseTimeMs average response time
     * @param p95ResponseTimeMs 95th percentile response time
     * @param maxResponseTimeMs maximum response time
     * @param dashboardPath path to dashboard folder
     * @param jtlPath path to JTL file
     * @param summaryPath path to machine summary file
     * @param readableSummaryPath path to human-readable summary file
     * @param executionStatus resolved execution status enum
     * @param executionPassed whether assertions passed (true) or not
     * @param expectedFailureMode whether the scenario was a negative/expected-failure test
     * @param actualFailureDetected whether a failure was observed
     * @param failureMessage optional failure message captured from exception/assertion
     * @return fully populated PerformanceExecutionResult instance
     */
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

        // Resolve basic identifiers and types for the result object
        String fullTargetUrl = buildFullUrl(request);
        String authType = resolveAuthType(request);
        String payloadSourceType = resolvePayloadSourceType(request);
        String payloadSourceDetails = resolvePayloadSourceDetails(request);
        String executionMode = resolveExecutionMode(profile);

        // Business-facing summaries and categorizations
        String testPurpose = buildTestPurpose(request, profile);
        String performanceTestType = buildPerformanceTestType(profile, expectedFailureMode);
        String testGoal = buildTestGoal(request, profile, expectedFailureMode);

        // Assessments derived from measured metrics
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

        // Assess threshold breaches vs configured assertionProfile
        String thresholdBreachSummary = buildThresholdBreachSummary(
                errorPercent,
                averageResponseTimeMs,
                p95ResponseTimeMs,
                assertionProfile
        );

        // Risk scoring and recommended action generation
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

        // Construct and return the immutable result object with cleaned/safe values
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

    /**
     * Lazily initialize the run-level folder and the in-memory run report.
     *
     * This method is idempotent and uses double-checked locking on RUN_LOCK to ensure only one
     * caller will create filesystem resources and set up the initial PerformanceRunReport.
     */
    private void initializeRunFolderIfNeeded() {
        if (runRootPath != null && currentRunReport != null) {
            return;
        }

        synchronized (RUN_LOCK) {
            if (runRootPath != null && currentRunReport != null) {
                return;
            }

            // Create a timestamped folder name for grouping all scenario artifacts in this execution run
            runRootFolderName = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd-MMM-yy_HH-mm-ss"));

            runRootPath = Paths.get(DEFAULT_REPORTS_BASE_DIR, runRootFolderName);

            try {
                Files.createDirectories(runRootPath);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create run-level performance report folder: " + runRootPath, e);
            }

            // Prepare the in-memory run report container
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

    /**
     * Write per-scenario summary files (machine-friendly and readable) using the summaryWriter.
     *
     * Exceptions are wrapped and rethrown to fail fast if summary writing fails.
     *
     * @param result scenario execution result
     */
    private void writeSummaries(PerformanceExecutionResult result) {
        try {
            summaryWriter.writeTextSummary(result);
            summaryWriter.writeReadableSummary(result);
        } catch (Exception e) {
            throw new RuntimeException("Unable to write performance summary files.", e);
        }
    }

    /**
     * Append scenario details to aggregate run-level summary files and the run index.
     *
     * This method is tolerant to errors by wrapping IO exceptions, but rethrows them to indicate
     * run-level reporting problems that should be visible to the caller.
     *
     * @param result scenario result to append
     */
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

    /**
     * Append the machine-oriented run summary entry for a scenario.
     *
     * This uses simple text formatting and appends to the aggregate file or creates it if missing.
     *
     * @param aggregateSummaryPath filesystem path for machine run summary
     * @param result scenario result to append
     * @throws IOException on write errors
     */
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

    /**
     * Append a human-readable paragraph-style summary entry for the scenario to the run-level readable summary.
     *
     * @param readableAggregateSummaryPath path to readable run summary file
     * @param result scenario execution result
     * @throws IOException if writing fails
     */
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

    /**
     * Append a compact index entry for the scenario to the run index file. Meant for quick lookup of runs.
     *
     * @param runIndexPath filesystem path for run index
     * @param result scenario execution result
     * @throws IOException on write errors
     */
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

    /**
     * Parse a JTL (JMeter Test Log) file and extract useful metrics needed to build scenario results.
     * Supports both XML JTL (JMeter default) and CSV JTL formats created by some runners.
     *
     * If the file does not exist or content cannot be parsed, returns zeroed metrics.
     *
     * @param jtlPath path to results.jtl produced by the executed test plan
     * @return JtlMetrics aggregated metrics (samples, errors, percent, percentiles)
     */
    private JtlMetrics parseJtlMetrics(Path jtlPath) {
        if (jtlPath == null || !Files.exists(jtlPath)) {
            return new JtlMetrics(0, 0, 0.0, 0, 0, 0, 0);
        }

        try {
            String content = Files.readString(jtlPath, StandardCharsets.UTF_8).trim();

            if (content.isBlank()) {
                return new JtlMetrics(0, 0, 0.0, 0, 0, 0, 0);
            }

            // Detect XML vs CSV content heuristically using the first characters
            if (content.startsWith("<?xml") || content.startsWith("<testResults")) {
                return parseXmlJtl(content);
            }

            return parseCsvJtl(content);

        } catch (Exception e) {
            throw new RuntimeException("Unable to parse JTL metrics from file: " + jtlPath, e);
        }
    }

    /**
     * Parse a CSV-format JTL content string and aggregate metrics.
     *
     * Expected CSV header contains at least "elapsed" and "success" columns.
     * If headers cannot be located, returns zeroed metrics.
     *
     * @param content CSV content representing JTL rows
     * @return JtlMetrics aggregated result
     * @throws IOException if reading from StringReader fails (unlikely)
     */
    private JtlMetrics parseCsvJtl(String content) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new JtlMetrics(0, 0, 0.0, 0, 0, 0, 0);
            }

            String[] headers = headerLine.split(",", -1);
            int elapsedIndex = indexOf(headers, "elapsed");
            int successIndex = indexOf(headers, "success");

            // If required fields are missing, we cannot parse CSV samples safely.
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
                    // row is malformed or truncated; skip
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

    /**
     * Parse an XML-format JTL content string using a regex to extract elapsed time (t attribute)
     * and success flag (s attribute) from <httpSample> or <sample> elements.
     *
     * This method is efficient and tolerant to minor JTL variations because it relies on attribute patterns.
     *
     * @param content XML JTL content
     * @return aggregated JtlMetrics
     */
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

    /**
     * Given totals and a list of elapsed values, compute derived metrics:
     * - error percentage
     * - average response time
     * - p95 percentile
     * - min and max response times
     *
     * This method centralizes the numeric calculations and defensive defaulting for empty data sets.
     *
     * @param totalSamples total sample count
     * @param totalErrors total failure count
     * @param totalElapsed sum of elapsed times in ms
     * @param elapsedValues list of individual sample elapsed times
     * @return JtlMetrics aggregated metrics
     */
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

    /**
     * Calculate the requested percentile (e.g., 95th) from a list of values.
     * Uses a simple nearest-rank selection after sorting the values.
     *
     * Returns 0 if the list is null or empty.
     *
     * @param values list of values to evaluate (will be sorted in place)
     * @param percentile integer percentile (1-100)
     * @return value at the requested percentile (or 0 if not available)
     */
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

    /**
     * Find the zero-based index of a header name within a CSV header array in a case-insensitive manner.
     *
     * @param headers header fields array
     * @param target header to find
     * @return index or -1 if not found
     */
    private int indexOf(String[] headers, String target) {
        for (int i = 0; i < headers.length; i++) {
            if (target.equalsIgnoreCase(headers[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Parse a numeric string into a long, returning 0 on any parsing error.
     *
     * @param value string to parse
     * @return parsed long or 0 if invalid
     */
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

    /**
     * Build a human-friendly test purpose description based on request and profile.
     *
     * @param request request under test
     * @param profile execution profile
     * @return readable test purpose
     */
    private String buildTestPurpose(PerformanceRequest request, PerformanceProfile profile) {
        String method = safeValue(request.getMethod());
        String path = safeValue(request.getPath());

        if (profile.getIterations() > 0) {
            return "Validate " + method + " " + path + " behavior across repeated request iterations.";
        }

        return "Validate " + method + " " + path + " behavior under concurrent user traffic for a timed execution window.";
    }

    /**
     * Create a high-level performance test type label for reporting.
     *
     * @param profile execution profile
     * @param expectedFailureMode negative test flag
     * @return descriptive test type
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
     * Build a test goal description used in reports.
     *
     * @param request request under test
     * @param profile execution profile
     * @param expectedFailureMode negative test flag
     * @return test goal description
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
     * Resolve which authentication mechanism was used in the request for reporting.
     *
     * @param request the performance request
     * @return textual description of auth type
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

    /**
     * Resolve the overall execution status value based on pass/fail flags and expected-failure context.
     *
     * @param executionPassed whether assertions passed
     * @param expectedFailureMode whether test was expecting failures
     * @param actualFailureDetected whether a failure was observed
     * @return PerformanceExecutionStatus corresponding to the situation
     */
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

    /**
     * Build a qualitative assessment string for response times based on average/p95/max.
     *
     * @param avg average response time (ms)
     * @param p95 95th percentile (ms)
     * @param max maximum observed (ms)
     * @return human-friendly response time assessment
     */
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

    /**
     * Build a human-friendly error assessment string for reporting.
     *
     * @param totalErrors total failures observed
     * @param errorPercent percent failures
     * @param expectedFailureMode whether this scenario expected failures
     * @return textual error assessment
     */
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

    /**
     * Build a stability assessment combining error counts and response-time extremes.
     *
     * @param totalSamples total samples recorded
     * @param totalErrors total failures observed
     * @param errorPercent error percentage
     * @param maxResponseTimeMs maximum observed response time
     * @return human-friendly stability assessment
     */
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

    /**
     * Build a first-failure indicator message combining explicit failure messages and metrics.
     *
     * @param totalErrors number of errors
     * @param errorPercent error percentage
     * @param failureMessage optional failure message captured
     * @param expectedFailureMode negative test flag
     * @return textual indicator describing where or how failures manifested
     */
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

    /**
     * Build a concise threshold breach summary listing which assertion thresholds were exceeded.
     *
     * @param errorPercent measured failure percentage
     * @param averageResponseTimeMs measured average
     * @param p95ResponseTimeMs measured p95
     * @param assertionProfile configured assertion thresholds
     * @return semicolon-separated list of breached thresholds or a default "none" text
     */
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

    /**
     * Compute a compact risk score (0-100) from execution status and metric threshold breaches.
     * Higher scores indicate more severe issues.
     *
     * This scoring is intentionally simple and deterministic for consistent reporting.
     *
     * @param executionStatus overall execution status
     * @param errorPercent measured error percent
     * @param averageResponseTimeMs average response time
     * @param p95ResponseTimeMs p95 response time
     * @param maxResponseTimeMs maximum response time
     * @param totalErrors absolute failures count
     * @param assertionProfile configured assertion thresholds (used to award points when exceeded)
     * @return integer risk score limited to 100
     */
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

    /**
     * Map a numeric risk score into a textual risk level.
     *
     * @param riskScore 0-100
     * @return "Critical", "High", "Medium" or "Low"
     */
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

    /**
     * Given execution status, risk score and threshold summary, recommend a next action for stakeholders.
     *
     * @param executionStatus overall execution status
     * @param riskScore computed risk score
     * @param thresholdBreachSummary summary of breached thresholds
     * @param expectedFailureMode whether the scenario is a negative validation
     * @return textual recommended action
     */
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

    /**
     * Create the final conclusion text describing the high-level outcome of the scenario.
     *
     * @param executionPassed whether assertions passed
     * @param expectedFailureMode whether test expected failure
     * @param actualFailureDetected whether a failure was observed
     * @param errorPercent measured error percent
     * @param averageResponseTimeMs average response time
     * @param p95ResponseTimeMs p95 response time
     * @return final conclusion sentence for reporting
     */
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

    /**
     * Build a full URL string for reporting using protocol, host, port and path.
     *
     * @param request performance request
     * @return fully qualified URL string (protocol://host:port/path)
     */
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

    /**
     * Validate that the input objects are not null. Throws IllegalArgumentException if any are missing.
     *
     * @param request request under test
     * @param profile execution profile
     * @param assertionProfile assertion configuration
     */
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

    /**
     * Sanitize a scenario name to produce a filesystem-friendly folder name.
     *
     * Replaces any non-alphanumeric plus dot, underscore, dash characters with a single underscore,
     * trims repeated underscores and returns "unnamed_test" for blank inputs.
     *
     * @param input original name
     * @return sanitized string suitable for folder names
     */
    private String sanitizeName(String input) {
        if (input == null || input.isBlank()) {
            return "unnamed_test";
        }

        return input.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
    }

    /**
     * Return a safe textual value for string fields used in reporting. Replaces null/blank with "N/A".
     *
     * @param value input string
     * @return safe string for display
     */
    private String safeValue(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    /**
     * Calculate elapsed time since a recorded start time in milliseconds. Returns 0 for negative intervals.
     *
     * @param startTimeMs start time in milliseconds
     * @return elapsed milliseconds since startTimeMs
     */
    private long elapsedSince(long startTimeMs) {
        return Math.max(0L, System.currentTimeMillis() - startTimeMs);
    }

    /**
     * Safely extract the message from a Throwable. If no message is available, returns a default line.
     *
     * @param throwable exception or assertion error
     * @return trimmed message or default text
     */
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

    /**
     * Ensure a double value is non-negative and finite; otherwise return zero.
     *
     * @param value input double
     * @return safe non-negative double (or 0.0)
     */
    private double safeNonNegative(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    /**
     * Internal container for aggregated JTL metrics used while parsing JTL files.
     *
     * Instances are produced by parseJtlMetrics and passed to callers for building final results.
     */
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
