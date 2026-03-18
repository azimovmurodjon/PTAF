package com.ptaf.performance.core;

import com.ptaf.performance.assertions.PerformanceAssertionEngine;
import com.ptaf.performance.builders.PerformanceTestPlanBuilder;
import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceProfile;
import com.ptaf.performance.models.PerformanceRequest;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
 * <p>Key behavior:
 * <ul>
 *   <li>All scenarios in one JVM execution share one run-level report folder.</li>
 *   <li>Each scenario still has its own artifact subfolder.</li>
 *   <li>Aggregate run summary is written into the shared run folder.</li>
 *   <li>Supports expected-failure mode for negative testing.</li>
 *   <li>Stores bearer tokens internally without requiring a separate auth manager class.</li>
 * </ul>
 * </p>
 */
public class PerformanceEngine {

    private static final Object RUN_LOCK = new Object();
    private static volatile String runRootFolderName;
    private static volatile Path runRootPath;
    private static final AtomicInteger scenarioSequence = new AtomicInteger(0);

    private static final String DEFAULT_REPORTS_BASE_DIR = "test-output/performance-reports";

    private final PerformanceAssertionEngine assertionEngine = new PerformanceAssertionEngine();
    private final PerformanceTestPlanBuilder testPlanBuilder = new PerformanceTestPlanBuilder();

    /**
     * Internal token store used instead of a separate AuthTokenManager class.
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

        if (request == null) {
            throw new IllegalArgumentException("Performance request cannot be null.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("Performance profile cannot be null.");
        }

        if (assertionProfile == null) {
            throw new IllegalArgumentException("Performance assertion profile cannot be null.");
        }

        initializeRunFolderIfNeeded();

        int scenarioNumber = scenarioSequence.incrementAndGet();
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

        long totalSamples = 0L;
        long totalErrors = 0L;
        double errorPercent = 0.0;
        long averageResponseTimeMs = 0L;
        long p95ResponseTimeMs = 0L;
        boolean executionPassed = false;
        boolean actualFailureDetected = false;
        String failureMessage = null;

        try {
            DslTestPlan testPlan = testPlanBuilder.buildHttpTestPlan(
                    request,
                    profile,
                    tokenStore,
                    jtlPath.toString(),
                    dashboardPath.toString(),
                    summaryPath.toString()
            );

            /*
             * Execute the JMeter DSL test plan.
             */
            testPlan.run();

            /*
             * Calculate performance metrics from generated JTL.
             * This avoids compile/runtime dependency on version-specific DSL stats APIs.
             */
            JtlMetrics metrics = parseJtlMetrics(jtlPath);

            totalSamples = metrics.totalSamples;
            totalErrors = metrics.totalErrors;
            errorPercent = metrics.errorPercent;
            averageResponseTimeMs = metrics.averageResponseTimeMs;
            p95ResponseTimeMs = metrics.p95ResponseTimeMs;

            PerformanceExecutionResult rawResult = buildFinalResult(
                    request.getRequestName(),
                    totalSamples,
                    totalErrors,
                    errorPercent,
                    averageResponseTimeMs,
                    p95ResponseTimeMs,
                    dashboardPath,
                    jtlPath,
                    summaryPath,
                    true,
                    expectedFailureMode,
                    false,
                    null
            );

            try {
                assertionEngine.validate(rawResult, assertionProfile);
                executionPassed = true;
                actualFailureDetected = false;

            } catch (AssertionError assertionError) {
                executionPassed = false;
                actualFailureDetected = true;
                failureMessage = assertionError.getMessage();

                if (!expectedFailureMode) {
                    PerformanceExecutionResult finalResult = buildFinalResult(
                            request.getRequestName(),
                            totalSamples,
                            totalErrors,
                            errorPercent,
                            averageResponseTimeMs,
                            p95ResponseTimeMs,
                            dashboardPath,
                            jtlPath,
                            summaryPath,
                            executionPassed,
                            expectedFailureMode,
                            actualFailureDetected,
                            failureMessage
                    );

                    writeScenarioSummary(finalResult);
                    writeAggregateRunSummary(finalResult);
                    throw assertionError;
                }
            }

        } catch (AssertionError e) {
            throw e;

        } catch (Exception e) {
            executionPassed = false;
            actualFailureDetected = true;
            failureMessage = e.getMessage();

            if (!expectedFailureMode) {
                PerformanceExecutionResult failedResult = buildFinalResult(
                        request.getRequestName(),
                        totalSamples,
                        totalErrors,
                        errorPercent,
                        averageResponseTimeMs,
                        p95ResponseTimeMs,
                        dashboardPath,
                        jtlPath,
                        summaryPath,
                        false,
                        false,
                        true,
                        failureMessage
                );

                writeScenarioSummary(failedResult);
                writeAggregateRunSummary(failedResult);

                throw new RuntimeException("Performance execution failed for test: " + request.getRequestName(), e);
            }
        }

        PerformanceExecutionResult finalResult = buildFinalResult(
                request.getRequestName(),
                totalSamples,
                totalErrors,
                errorPercent,
                averageResponseTimeMs,
                p95ResponseTimeMs,
                dashboardPath,
                jtlPath,
                summaryPath,
                executionPassed,
                expectedFailureMode,
                actualFailureDetected,
                failureMessage
        );

        writeScenarioSummary(finalResult);
        writeAggregateRunSummary(finalResult);
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

    // ============================================================
    // RESULT BUILDING
    // ============================================================

    private PerformanceExecutionResult buildFinalResult(String testName,
                                                        long totalSamples,
                                                        long totalErrors,
                                                        double errorPercent,
                                                        long averageResponseTimeMs,
                                                        long p95ResponseTimeMs,
                                                        Path dashboardPath,
                                                        Path jtlPath,
                                                        Path summaryPath,
                                                        boolean executionPassed,
                                                        boolean expectedFailureMode,
                                                        boolean actualFailureDetected,
                                                        String failureMessage) {

        return new PerformanceExecutionResult(
                testName,
                totalSamples,
                totalErrors,
                errorPercent,
                averageResponseTimeMs,
                p95ResponseTimeMs,
                dashboardPath.toString(),
                jtlPath.toString(),
                summaryPath.toString(),
                runRootPath.toString(),
                executionPassed,
                expectedFailureMode,
                actualFailureDetected,
                failureMessage
        );
    }

    // ============================================================
    // RUN-LEVEL REPORT FOLDER
    // ============================================================

    private void initializeRunFolderIfNeeded() {
        if (runRootPath != null) {
            return;
        }

        synchronized (RUN_LOCK) {
            if (runRootPath != null) {
                return;
            }

            String baseReportsDir = DEFAULT_REPORTS_BASE_DIR;

            runRootFolderName = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd-MMM-yy_HH-mm-ss"));

            runRootPath = Paths.get(baseReportsDir, runRootFolderName);

            try {
                Files.createDirectories(runRootPath);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create run-level performance report folder: " + runRootPath, e);
            }
        }
    }

    // ============================================================
    // SUMMARY WRITING
    // ============================================================

    private void writeScenarioSummary(PerformanceExecutionResult result) {
        try {
            Path summaryPath = Paths.get(result.getSummaryFilePath());

            StringBuilder sb = new StringBuilder();
            sb.append("============================================================").append(System.lineSeparator());
            sb.append("Test Name           : ").append(result.getTestName()).append(System.lineSeparator());
            sb.append("Execution Passed    : ").append(result.isExecutionPassed()).append(System.lineSeparator());
            sb.append("Expected Failure    : ").append(result.isExpectedFailureMode()).append(System.lineSeparator());
            sb.append("Actual Failure      : ").append(result.isActualFailureDetected()).append(System.lineSeparator());
            sb.append("Total Samples       : ").append(result.getTotalSamples()).append(System.lineSeparator());
            sb.append("Total Errors        : ").append(result.getTotalErrors()).append(System.lineSeparator());
            sb.append("Error Percent       : ").append(result.getErrorPercent()).append(System.lineSeparator());
            sb.append("Average Response    : ").append(result.getAverageResponseTimeMs()).append(" ms").append(System.lineSeparator());
            sb.append("P95 Response        : ").append(result.getP95ResponseTimeMs()).append(" ms").append(System.lineSeparator());
            sb.append("Dashboard Path      : ").append(result.getDashboardPath()).append(System.lineSeparator());
            sb.append("JTL Path            : ").append(result.getJtlFilePath()).append(System.lineSeparator());
            sb.append("Run Root Path       : ").append(result.getRunReportRootPath()).append(System.lineSeparator());

            if (result.getFailureMessage() != null && !result.getFailureMessage().isBlank()) {
                sb.append("Failure Message     : ").append(result.getFailureMessage()).append(System.lineSeparator());
            }

            sb.append("============================================================").append(System.lineSeparator());

            Files.writeString(
                    summaryPath,
                    sb.toString(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

        } catch (Exception e) {
            throw new RuntimeException("Unable to write scenario summary file.", e);
        }
    }

    private void writeAggregateRunSummary(PerformanceExecutionResult result) {
        try {
            Path aggregateSummaryPath = runRootPath.resolve("run-summary.txt");
            Path runIndexPath = runRootPath.resolve("run-index.txt");

            appendRunSummary(aggregateSummaryPath, result);
            appendRunIndex(runIndexPath, result);

        } catch (Exception e) {
            throw new RuntimeException("Unable to write aggregate performance run summary.", e);
        }
    }

    private void appendRunSummary(Path aggregateSummaryPath, PerformanceExecutionResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================").append(System.lineSeparator());
        sb.append("Test Name           : ").append(result.getTestName()).append(System.lineSeparator());
        sb.append("Execution Passed    : ").append(result.isExecutionPassed()).append(System.lineSeparator());
        sb.append("Expected Failure    : ").append(result.isExpectedFailureMode()).append(System.lineSeparator());
        sb.append("Actual Failure      : ").append(result.isActualFailureDetected()).append(System.lineSeparator());
        sb.append("Total Samples       : ").append(result.getTotalSamples()).append(System.lineSeparator());
        sb.append("Total Errors        : ").append(result.getTotalErrors()).append(System.lineSeparator());
        sb.append("Error Percent       : ").append(result.getErrorPercent()).append(System.lineSeparator());
        sb.append("Average Response    : ").append(result.getAverageResponseTimeMs()).append(" ms").append(System.lineSeparator());
        sb.append("P95 Response        : ").append(result.getP95ResponseTimeMs()).append(" ms").append(System.lineSeparator());
        sb.append("Dashboard Path      : ").append(result.getDashboardPath()).append(System.lineSeparator());
        sb.append("JTL Path            : ").append(result.getJtlFilePath()).append(System.lineSeparator());
        sb.append("Scenario Summary    : ").append(result.getSummaryFilePath()).append(System.lineSeparator());

        if (result.getFailureMessage() != null && !result.getFailureMessage().isBlank()) {
            sb.append("Failure Message     : ").append(result.getFailureMessage()).append(System.lineSeparator());
        }

        sb.append("============================================================").append(System.lineSeparator());

        Files.writeString(
                aggregateSummaryPath,
                sb.toString(),
                StandardCharsets.UTF_8,
                Files.exists(aggregateSummaryPath)
                        ? StandardOpenOption.APPEND
                        : StandardOpenOption.CREATE
        );
    }

    private void appendRunIndex(Path runIndexPath, PerformanceExecutionResult result) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Test Name           : ").append(result.getTestName()).append(System.lineSeparator());
        sb.append("Execution Passed    : ").append(result.isExecutionPassed()).append(System.lineSeparator());
        sb.append("Expected Failure    : ").append(result.isExpectedFailureMode()).append(System.lineSeparator());
        sb.append("Actual Failure      : ").append(result.isActualFailureDetected()).append(System.lineSeparator());
        sb.append("Error Percent       : ").append(result.getErrorPercent()).append(System.lineSeparator());
        sb.append("Total Samples       : ").append(result.getTotalSamples()).append(System.lineSeparator());
        sb.append("Total Errors        : ").append(result.getTotalErrors()).append(System.lineSeparator());
        sb.append("Avg Response Time   : ").append(result.getAverageResponseTimeMs()).append(" ms").append(System.lineSeparator());
        sb.append("P95 Response Time   : ").append(result.getP95ResponseTimeMs()).append(" ms").append(System.lineSeparator());
        sb.append("Dashboard Path      : ").append(result.getDashboardPath()).append(System.lineSeparator());
        sb.append("JTL Path            : ").append(result.getJtlFilePath()).append(System.lineSeparator());
        sb.append("Summary Path        : ").append(result.getSummaryFilePath()).append(System.lineSeparator());

        if (result.getFailureMessage() != null && !result.getFailureMessage().isBlank()) {
            sb.append("Failure Message     : ").append(result.getFailureMessage()).append(System.lineSeparator());
        }

        sb.append("------------------------------------------------------------").append(System.lineSeparator());

        Files.writeString(
                runIndexPath,
                sb.toString(),
                StandardCharsets.UTF_8,
                Files.exists(runIndexPath)
                        ? StandardOpenOption.APPEND
                        : StandardOpenOption.CREATE
        );
    }

    // ============================================================
    // JTL PARSING
    // ============================================================

    private JtlMetrics parseJtlMetrics(Path jtlPath) {
        if (jtlPath == null || !Files.exists(jtlPath)) {
            return new JtlMetrics(0, 0, 0.0, 0, 0);
        }

        try {
            String content = Files.readString(jtlPath, StandardCharsets.UTF_8).trim();

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
                return new JtlMetrics(0, 0, 0.0, 0, 0);
            }

            String[] headers = headerLine.split(",", -1);
            int elapsedIndex = indexOf(headers, "elapsed");
            int successIndex = indexOf(headers, "success");

            if (elapsedIndex < 0 || successIndex < 0) {
                return new JtlMetrics(0, 0, 0.0, 0, 0);
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

        return new JtlMetrics(
                totalSamples,
                totalErrors,
                errorPercent,
                averageResponseTimeMs,
                p95ResponseTimeMs
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
    // HELPERS
    // ============================================================

    private String sanitizeName(String input) {
        if (input == null || input.isBlank()) {
            return "unnamed_test";
        }

        return input.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
    }

    private static class JtlMetrics {
        private final long totalSamples;
        private final long totalErrors;
        private final double errorPercent;
        private final long averageResponseTimeMs;
        private final long p95ResponseTimeMs;

        private JtlMetrics(long totalSamples,
                           long totalErrors,
                           double errorPercent,
                           long averageResponseTimeMs,
                           long p95ResponseTimeMs) {
            this.totalSamples = totalSamples;
            this.totalErrors = totalErrors;
            this.errorPercent = errorPercent;
            this.averageResponseTimeMs = averageResponseTimeMs;
            this.p95ResponseTimeMs = p95ResponseTimeMs;
        }
    }
}