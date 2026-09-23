package com.ptaf.ui_performance.reporting;

import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceExecutionStatus;
import com.ptaf.performance.models.PerformanceRunReport;
import com.ptaf.performance.reports.PerformanceExcelReportWriter;
import com.ptaf.performance.reports.PerformanceSummaryWriter;
import com.ptaf.ui_performance.metrics.UiPerformanceMetrics;
import com.ptaf.ui_performance.model.UiPerformanceIterationResult;
import com.ptaf.ui_performance.model.UiPerformanceRunResult;
import com.ptaf.ui_performance.model.UiPerformanceStage;
import com.ptaf.ui_performance.model.UiPerformanceStageResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapts real-browser UI performance stage results into the framework's existing performance report model.
 *
 * <p>This is a reporting adapter only. It does not route browser execution through JMeter and does not
 * misrepresent network samples as browser journeys. Each stage becomes one performance scenario in the
 * existing Excel workbook and summary writers. A JTL-compatible raw CSV is created from browser journey
 * samples so established report links remain useful.</p>
 */
public final class UiPerformanceExistingReporterAdapter {
    private final PerformanceSummaryWriter summaryWriter = new PerformanceSummaryWriter();
    private final PerformanceExcelReportWriter excelReportWriter = new PerformanceExcelReportWriter();

    /** Writes existing Performance reporter artifacts into the same isolated UI performance run folder. */
    public Path write(UiPerformanceRunResult uiResult) {
        PerformanceRunReport runReport = new PerformanceRunReport(
                uiResult.getRunId(),
                uiResult.getReportDirectory().toString(),
                uiResult.getStartedAt().toString());

        for (UiPerformanceStageResult stageResult : uiResult.getStageResults()) {
            PerformanceExecutionResult adapted = adaptStage(uiResult, stageResult);
            summaryWriter.writeTextSummary(adapted);
            summaryWriter.writeReadableSummary(adapted);
            runReport.addScenarioResult(adapted);
        }

        Path workbook = excelReportWriter.writeRunReport(runReport);
        writeRunIndex(uiResult, runReport, workbook);
        return workbook;
    }

    private PerformanceExecutionResult adaptStage(UiPerformanceRunResult uiResult,
                                                  UiPerformanceStageResult stageResult) {
        UiPerformanceStage stage = stageResult.getStage();
        UiPerformanceMetrics metrics = stageResult.getMetrics();
        Path stageDirectory = uiResult.getReportDirectory().resolve(
                "performance-reporter").resolve(UiPerformanceReportManager.sanitize(stage.name()));
        Path summary = stageDirectory.resolve("summary.txt");
        Path readableSummary = stageDirectory.resolve("readable-summary.txt");
        Path rawJtl = stageDirectory.resolve("ui-browser-results.jtl");
        Path dashboard = uiResult.getReportDirectory().resolve("ui_performance-summary.html");
        createDirectories(stageDirectory);
        writeRawBrowserJtl(stageResult, rawJtl);

        List<String> breaches = thresholdBreaches(uiResult, metrics);
        boolean passed = breaches.isEmpty();
        int riskScore = calculateRiskScore(uiResult, metrics);
        String riskLevel = riskLevel(riskScore);
        String failureMessage = firstFailureMessage(stageResult);
        String thresholdSummary = passed
                ? "No configured threshold breaches detected."
                : String.join("; ", breaches);
        String testType = "UI " + uiResult.getExecutionPlan().testType().displayName();
        String profileDescription = uiResult.getExecutionPlan().profileName() + " / stage " + stage.name();

        return new PerformanceExecutionResult(
                uiResult.getJourneyName() + " - " + stage.name(),
                "Measure the configured browser journey under concurrent real-browser user load.",
                testType,
                "Validate browser journey response time, throughput, error rate, and stability under " + profileDescription + ".",
                "BROWSER",
                "Configured UI journey",
                uiResult.getTargetHost(),
                "Browser interaction",
                "Rendered application",
                "Configured test account",
                "UI locator actions",
                "Values resolved from isolated UI performance YAML, locator YAML, and CSV data.",
                stage.virtualUsers(),
                stage.rampUpSeconds(),
                stage.holdSeconds(),
                stage.iterationsPerUser(),
                profileDescription + " / " + (stage.isDurationBased() ? "duration-based" : "iteration-based"),
                uiResult.getProfile().maximumFailureRatePercent(),
                uiResult.getProfile().maximumAverageJourneyDurationMs(),
                uiResult.getProfile().maximumP95JourneyDurationMs(),
                stageResult.getDurationMs(),
                metrics.totalIterations(),
                metrics.failedIterations(),
                metrics.failureRatePercent(),
                metrics.minimumJourneyDurationMs(),
                metrics.averageJourneyDurationMs(),
                metrics.p95JourneyDurationMs(),
                metrics.maximumJourneyDurationMs(),
                riskScore,
                riskLevel,
                thresholdSummary,
                passed
                        ? "No immediate action required; compare this stage with the baseline and monitor trend changes."
                        : "Review failed browser journeys and timing regressions before increasing concurrent users.",
                "Average " + duration(metrics.averageJourneyDurationMs()) + "; P95 " + duration(metrics.p95JourneyDurationMs())
                        + "; maximum " + duration(metrics.maximumJourneyDurationMs()) + ".",
                metrics.failedIterations() == 0
                        ? "No failed browser journeys were recorded."
                        : metrics.failedIterations() + " browser journey sample(s) failed ("
                        + format(metrics.failureRatePercent()) + "%).",
                "Completed " + metrics.totalIterations() + " browser journeys at "
                        + format(metrics.completedJourneysPerMinute()) + " journeys/min. First-start spread: "
                        + duration(stageResult.getFirstIterationStartSpreadMs()) + ".",
                firstFailureIndicator(stageResult),
                passed
                        ? "UI performance stage passed configured thresholds."
                        : "UI performance stage completed but breached configured thresholds.",
                dashboard.toString(),
                rawJtl.toString(),
                summary.toString(),
                readableSummary.toString(),
                uiResult.getReportDirectory().toString(),
                passed ? PerformanceExecutionStatus.PASS : PerformanceExecutionStatus.FAIL,
                passed,
                false,
                !passed,
                failureMessage);
    }

    private List<String> thresholdBreaches(UiPerformanceRunResult result, UiPerformanceMetrics metrics) {
        List<String> breaches = new ArrayList<>();
        if (metrics.failureRatePercent() > result.getProfile().maximumFailureRatePercent()) {
            breaches.add("Failure rate " + format(metrics.failureRatePercent()) + "% exceeds "
                    + format(result.getProfile().maximumFailureRatePercent()) + "%");
        }
        if (metrics.averageJourneyDurationMs() > result.getProfile().maximumAverageJourneyDurationMs()) {
            breaches.add("Average browser journey " + duration(metrics.averageJourneyDurationMs()) + " exceeds "
                    + duration(result.getProfile().maximumAverageJourneyDurationMs()));
        }
        if (metrics.p95JourneyDurationMs() > result.getProfile().maximumP95JourneyDurationMs()) {
            breaches.add("P95 browser journey " + duration(metrics.p95JourneyDurationMs()) + " exceeds "
                    + duration(result.getProfile().maximumP95JourneyDurationMs()));
        }
        return breaches;
    }

    private int calculateRiskScore(UiPerformanceRunResult result, UiPerformanceMetrics metrics) {
        int score = 0;
        if (metrics.failureRatePercent() > 0) score += 20;
        if (metrics.failureRatePercent() > result.getProfile().maximumFailureRatePercent()) score += 35;
        if (metrics.averageJourneyDurationMs() > result.getProfile().maximumAverageJourneyDurationMs()) score += 20;
        if (metrics.p95JourneyDurationMs() > result.getProfile().maximumP95JourneyDurationMs()) score += 25;
        return Math.min(100, score);
    }

    private String riskLevel(int score) {
        if (score >= 75) return "Critical";
        if (score >= 50) return "High";
        if (score >= 25) return "Medium";
        return "Low";
    }

    private String firstFailureIndicator(UiPerformanceStageResult stageResult) {
        return stageResult.getIterations().stream()
                .filter(result -> !result.isPassed())
                .findFirst()
                .map(result -> "Stage " + stageResult.getStage().name() + ", virtual user "
                        + result.getUserId() + ", iteration " + result.getIteration()
                        + ", category " + safe(result.getFailureCategory()))
                .orElse("No browser journey failure detected.");
    }

    private String firstFailureMessage(UiPerformanceStageResult stageResult) {
        return stageResult.getIterations().stream()
                .filter(result -> !result.isPassed())
                .findFirst()
                .map(UiPerformanceIterationResult::getFailureMessage)
                .map(UiPerformanceSensitiveTextSanitizer::sanitize)
                .orElse("");
    }

    private void writeRawBrowserJtl(UiPerformanceStageResult stageResult, Path path) {
        StringBuilder csv = new StringBuilder(
                "timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage\n");
        int index = 0;
        for (UiPerformanceIterationResult result : stageResult.getIterations()) {
            csv.append(result.getStartedAtEpochMs()).append(',')
                    .append(result.getDurationMs()).append(',')
                    .append(csv(stageResult.getStage().name())).append(',')
                    .append(result.isPassed() ? "200" : "500").append(',')
                    .append(csv(result.isPassed() ? "Browser journey passed" : safe(result.getFailureCategory()))).append(',')
                    .append(csv("UI-VU-" + result.getUserId() + "-" + (++index))).append(',')
                    .append("ui-browser,")
                    .append(result.isPassed()).append(',')
                    .append(csv(result.isPassed() ? "" : safe(result.getFailureMessage()))).append('\n');
        }
        try {
            Files.writeString(path, csv.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write UI performance raw browser sample file: " + path, exception);
        }
    }

    private void writeRunIndex(UiPerformanceRunResult uiResult,
                               PerformanceRunReport runReport,
                               Path workbook) {
        String content = "UI PERFORMANCE / EXISTING PERFORMANCE REPORTER INTEGRATION\n"
                + "Run: " + uiResult.getRunId() + "\n"
                + "Profile: " + uiResult.getExecutionPlan().profileName() + "\n"
                + "Type: " + uiResult.getExecutionPlan().testType().displayName() + "\n"
                + "Stages: " + runReport.getTotalScenarios() + "\n"
                + "Browser journey samples: " + uiResult.getMetrics().totalIterations() + "\n"
                + "Performance Excel workbook: " + workbook + "\n"
                + "Browser dashboard: " + uiResult.getReportDirectory().resolve("ui_performance-summary.html") + "\n"
                + "Note: each Performance reporter scenario represents one real-browser load stage, not one HTTP endpoint.\n";
        try {
            Files.writeString(uiResult.getReportDirectory().resolve("performance-reporter-index.txt"),
                    content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write UI performance reporter index.", exception);
        }
    }

    private void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create UI performance reporter directory: " + path, exception);
        }
    }

    private static String csv(String value) {
        return "\"" + safe(value).replace("\"", "\"\"") + "\"";
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.replaceAll("[\\r\\n]+", " ").trim();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String duration(long milliseconds) {
        return UiPerformanceDurationFormatter.format(milliseconds);
    }
}
