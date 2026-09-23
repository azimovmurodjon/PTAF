package com.ptaf.ui_performance.reporting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ptaf.ui_performance.model.UiPerformanceIterationResult;
import com.ptaf.ui_performance.model.UiPerformanceRunResult;
import com.ptaf.ui_performance.model.UiPerformanceStageResult;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Writes standalone browser-load reports with profile, stage, percentile, throughput, and transaction evidence. */
public final class UiPerformanceReportWriter {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_INSTANT;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Path write(UiPerformanceRunResult result,
                      boolean htmlEnabled,
                      boolean pdfEnabled,
                      boolean csvEnabled,
                      boolean jsonEnabled) {
        try {
            List<StepTimingSummary> stepTimings = summarizeStepTimings(result);
            if (htmlEnabled) {
                Files.writeString(result.getReportDirectory().resolve("ui_performance-summary.html"),
                        buildHtml(result, stepTimings), StandardCharsets.UTF_8);
            }
            if (csvEnabled) {
                Files.writeString(result.getReportDirectory().resolve("ui_performance-stages.csv"),
                        buildStageCsv(result), StandardCharsets.UTF_8);
                Files.writeString(result.getReportDirectory().resolve("ui_performance-iterations.csv"),
                        buildIterationCsv(result), StandardCharsets.UTF_8);
                Files.writeString(result.getReportDirectory().resolve("ui_performance-step-timings.csv"),
                        buildStepTimingCsv(stepTimings), StandardCharsets.UTF_8);
            }
            if (jsonEnabled) {
                Files.writeString(result.getReportDirectory().resolve("ui_performance-summary.json"),
                        GSON.toJson(toSafeReportMap(result, stepTimings)), StandardCharsets.UTF_8);
            }
            Files.writeString(result.getReportDirectory().resolve("ui_performance-performance-summary.txt"),
                    buildTechnicalSummary(result, stepTimings), StandardCharsets.UTF_8);
            if (pdfEnabled) {
                writePdf(result, stepTimings, result.getReportDirectory().resolve("ui_performance-summary.pdf"));
            }
            return result.getReportDirectory();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write UI performance reports under: " + result.getReportDirectory(), exception);
        }
    }

    private String buildHtml(UiPerformanceRunResult result, List<StepTimingSummary> stepTimings) {
        String status = result.getMetrics().failedIterations() == 0 ? "PASS" : "COMPLETE WITH FAILURES";
        StringBuilder stageRows = new StringBuilder();
        for (UiPerformanceStageResult stage : result.getStageResults()) {
            stageRows.append("<tr><td>").append(escape(stage.getStage().name())).append("</td><td>")
                    .append(stage.getStage().virtualUsers()).append("</td><td>")
                    .append(stage.getStage().rampUpSeconds()).append(" s</td><td>")
                    .append(stage.getStage().holdSeconds()).append(" s</td><td>")
                    .append(stage.getStage().iterationsPerUser()).append("</td><td>")
                    .append(stage.getMetrics().totalIterations()).append("</td><td>")
                    .append(stage.getMetrics().passedIterations()).append(" / ").append(stage.getMetrics().failedIterations()).append("</td><td>")
                    .append(format(stage.getMetrics().failureRatePercent())).append("%</td><td>")
                    .append(duration(stage.getMetrics().averageJourneyDurationMs())).append("</td><td>")
                    .append(duration(stage.getMetrics().p90JourneyDurationMs())).append("</td><td>")
                    .append(duration(stage.getMetrics().p95JourneyDurationMs())).append("</td><td>")
                    .append(duration(stage.getMetrics().p99JourneyDurationMs())).append("</td><td>")
                    .append(format(stage.getMetrics().completedJourneysPerSecond())).append("/s</td><td>")
                    .append(duration(stage.getFirstIterationStartSpreadMs())).append("</td></tr>");
        }

        StringBuilder transactionRows = new StringBuilder();
        for (StepTimingSummary summary : stepTimings) {
            transactionRows.append("<tr><td>").append(escape(summary.stepName())).append("</td><td>")
                    .append(summary.samples()).append("</td><td>").append(duration(summary.minimumMs())).append("</td><td>")
                    .append(duration(summary.averageMs())).append("</td><td>").append(duration(summary.medianMs())).append("</td><td>")
                    .append(duration(summary.p90Ms())).append("</td><td>").append(duration(summary.p95Ms())).append("</td><td>")
                    .append(duration(summary.p99Ms())).append("</td><td>").append(duration(summary.maximumMs())).append("</td></tr>");
        }
        if (transactionRows.isEmpty()) {
            transactionRows.append("<tr><td colspan=\"9\">No completed UI transactions were recorded.</td></tr>");
        }

        StringBuilder iterationRows = new StringBuilder();
        for (UiPerformanceIterationResult iteration : result.getIterations()) {
            iterationRows.append("<tr><td>").append(escape(iteration.getStageName())).append("</td><td>")
                    .append(escape(iteration.getUserId())).append("</td><td>").append(iteration.getIteration())
                    .append("</td><td class=\"").append(iteration.isPassed() ? "pass" : "fail").append("\">")
                    .append(iteration.isPassed() ? "Passed" : "Failed").append("</td><td>")
                    .append(duration(iteration.getDurationMs())).append("</td><td>")
                    .append(formatStepDurationsHtml(iteration.getStepDurationsMs())).append("</td><td>")
                    .append(escape(defaultText(iteration.getFailureCategory()))).append("</td><td>")
                    .append(escape(defaultText(iteration.getFailureMessage()))).append("</td></tr>");
        }

        return "<!doctype html><html><head><meta charset=\"UTF-8\"><title>UI Performance Report</title>"
                + "<style>body{font-family:Arial,sans-serif;margin:32px;color:#102a43;background:#f3f6f8}h1,h2{color:#003b70}"
                + ".card{background:#fff;border:1px solid #d6e0e8;border-radius:8px;padding:18px;margin:14px 0;overflow:auto}"
                + ".grid{display:grid;grid-template-columns:repeat(4,minmax(160px,1fr));gap:12px}.metric{background:#e8f4fb;padding:12px;border-radius:6px}"
                + "table{width:100%;border-collapse:collapse;background:#fff}th,td{padding:9px;border:1px solid #d6e0e8;text-align:left;font-size:12px;vertical-align:top}"
                + "th{background:#003b70;color:#fff;white-space:nowrap}.pass{color:#16824c;font-weight:bold}.fail{color:#c8102e;font-weight:bold}.note{color:#526a80;font-size:13px}.timing{white-space:nowrap;line-height:1.55}</style>"
                + "</head><body><h1>UI Performance Automation Report</h1>"
                + "<p class=\"note\">Real-browser " + escape(result.getExecutionPlan().testType().displayName())
                + " performance report. TestNG/Cucumber triggered the run; the dedicated engine supplied the browser-user concurrency.</p>"
                + "<div class=\"card\"><strong>Run:</strong> " + escape(result.getRunId()) + " &nbsp; <strong>Status:</strong> " + status
                + "<br><strong>Journey:</strong> " + escape(result.getJourneyName()) + " &nbsp; <strong>Target host:</strong> " + escape(result.getTargetHost())
                + "<br><strong>Profile:</strong> " + escape(result.getExecutionPlan().profileName()) + " &nbsp; <strong>Type:</strong> " + escape(result.getExecutionPlan().testType().displayName())
                + "<br><strong>Maximum concurrent users:</strong> " + result.getExecutionPlan().maximumVirtualUsers()
                + " &nbsp; <strong>Headless:</strong> " + result.getProfile().headless()
                + " &nbsp; <strong>Isolation:</strong> one browser process per virtual user"
                + "<br><strong>Started:</strong> " + TIMESTAMP.format(result.getStartedAt()) + " &nbsp; <strong>Completed:</strong> " + TIMESTAMP.format(result.getCompletedAt()) + "</div>"
                + metricCards(result)
                + "<div class=\"card\"><h2>Load profile and stage results</h2><table><thead><tr><th>Stage</th><th>Users</th><th>Ramp</th><th>Hold</th><th>Iterations/user</th><th>Samples</th><th>Passed / Failed</th><th>Error</th><th>Average</th><th>P90</th><th>P95</th><th>P99</th><th>Throughput</th><th>First-start spread</th></tr></thead><tbody>"
                + stageRows + "</tbody></table></div>"
                + "<div class=\"card\"><h2>Browser transaction timing summary</h2><p class=\"note\">Durations are shown in readable units; raw milliseconds remain available in the CSV and JSON exports.</p>"
                + "<table><thead><tr><th>Transaction</th><th>Samples</th><th>Min</th><th>Average</th><th>Median</th><th>P90</th><th>P95</th><th>P99</th><th>Max</th></tr></thead><tbody>"
                + transactionRows + "</tbody></table></div>"
                + "<div class=\"card\"><h2>Virtual-user journey evidence</h2><table><thead><tr><th>Stage</th><th>Virtual user</th><th>Iteration</th><th>Status</th><th>Journey duration</th><th>Step timings</th><th>Failure category</th><th>Sanitized diagnostic</th></tr></thead><tbody>"
                + iterationRows + "</tbody></table></div>"
                + "<p class=\"note\">Full URLs, credentials, tokens, input values, cookies, and session data are intentionally excluded.</p></body></html>";
    }

    private String metricCards(UiPerformanceRunResult result) {
        return "<div class=\"grid\"><div class=\"metric\"><b>Total journeys</b><br>" + result.getMetrics().totalIterations() + "</div>"
                + "<div class=\"metric\"><b>Passed / failed</b><br>" + result.getMetrics().passedIterations() + " / " + result.getMetrics().failedIterations() + "</div>"
                + "<div class=\"metric\"><b>Error rate</b><br>" + format(result.getMetrics().failureRatePercent()) + "%</div>"
                + "<div class=\"metric\"><b>Throughput</b><br>" + format(result.getMetrics().completedJourneysPerSecond()) + " journeys/s</div>"
                + "<div class=\"metric\"><b>Minimum</b><br>" + duration(result.getMetrics().minimumJourneyDurationMs()) + "</div>"
                + "<div class=\"metric\"><b>Average</b><br>" + duration(result.getMetrics().averageJourneyDurationMs()) + "</div>"
                + "<div class=\"metric\"><b>Median</b><br>" + duration(result.getMetrics().medianJourneyDurationMs()) + "</div>"
                + "<div class=\"metric\"><b>P90 / P95 / P99</b><br>" + duration(result.getMetrics().p90JourneyDurationMs()) + " / "
                + duration(result.getMetrics().p95JourneyDurationMs()) + " / " + duration(result.getMetrics().p99JourneyDurationMs()) + "</div>"
                + "<div class=\"metric\"><b>Maximum</b><br>" + duration(result.getMetrics().maximumJourneyDurationMs()) + "</div></div>";
    }

    private String buildStageCsv(UiPerformanceRunResult result) {
        StringBuilder csv = new StringBuilder("profile,type,stage,users,ramp_seconds,hold_seconds,iterations_per_user,total_samples,passed,failed,error_percent,min_ms,min_readable,average_ms,average_readable,median_ms,median_readable,p90_ms,p90_readable,p95_ms,p95_readable,p99_ms,p99_readable,max_ms,max_readable,throughput_per_second,first_start_spread_ms,first_start_spread_readable\n");
        for (UiPerformanceStageResult stage : result.getStageResults()) {
            csv.append(csv(result.getExecutionPlan().profileName())).append(',')
                    .append(csv(result.getExecutionPlan().testType().displayName())).append(',')
                    .append(csv(stage.getStage().name())).append(',')
                    .append(stage.getStage().virtualUsers()).append(',')
                    .append(stage.getStage().rampUpSeconds()).append(',')
                    .append(stage.getStage().holdSeconds()).append(',')
                    .append(stage.getStage().iterationsPerUser()).append(',')
                    .append(stage.getMetrics().totalIterations()).append(',')
                    .append(stage.getMetrics().passedIterations()).append(',')
                    .append(stage.getMetrics().failedIterations()).append(',')
                    .append(format(stage.getMetrics().failureRatePercent())).append(',')
                    .append(stage.getMetrics().minimumJourneyDurationMs()).append(',').append(csv(duration(stage.getMetrics().minimumJourneyDurationMs()))).append(',')
                    .append(stage.getMetrics().averageJourneyDurationMs()).append(',').append(csv(duration(stage.getMetrics().averageJourneyDurationMs()))).append(',')
                    .append(stage.getMetrics().medianJourneyDurationMs()).append(',').append(csv(duration(stage.getMetrics().medianJourneyDurationMs()))).append(',')
                    .append(stage.getMetrics().p90JourneyDurationMs()).append(',').append(csv(duration(stage.getMetrics().p90JourneyDurationMs()))).append(',')
                    .append(stage.getMetrics().p95JourneyDurationMs()).append(',').append(csv(duration(stage.getMetrics().p95JourneyDurationMs()))).append(',')
                    .append(stage.getMetrics().p99JourneyDurationMs()).append(',').append(csv(duration(stage.getMetrics().p99JourneyDurationMs()))).append(',')
                    .append(stage.getMetrics().maximumJourneyDurationMs()).append(',').append(csv(duration(stage.getMetrics().maximumJourneyDurationMs()))).append(',')
                    .append(format(stage.getMetrics().completedJourneysPerSecond())).append(',')
                    .append(stage.getFirstIterationStartSpreadMs()).append(',').append(csv(duration(stage.getFirstIterationStartSpreadMs()))).append('\n');
        }
        return csv.toString();
    }

    private String buildIterationCsv(UiPerformanceRunResult result) {
        StringBuilder csv = new StringBuilder("stage,virtual_user,iteration,status,start_epoch_ms,complete_epoch_ms,journey_duration_ms,journey_duration_readable,step_timings_ms,step_timings_readable,failure_category,sanitized_diagnostic,console_error_count\n");
        for (UiPerformanceIterationResult iteration : result.getIterations()) {
            csv.append(csv(iteration.getStageName())).append(',')
                    .append(csv(iteration.getUserId())).append(',')
                    .append(iteration.getIteration()).append(',')
                    .append(iteration.isPassed() ? "PASSED" : "FAILED").append(',')
                    .append(iteration.getStartedAtEpochMs()).append(',')
                    .append(iteration.getCompletedAtEpochMs()).append(',')
                    .append(iteration.getDurationMs()).append(',')
                    .append(csv(duration(iteration.getDurationMs()))).append(',')
                    .append(csv(formatStepDurationsText(iteration.getStepDurationsMs()))).append(',')
                    .append(csv(formatStepDurationsReadableText(iteration.getStepDurationsMs()))).append(',')
                    .append(csv(defaultText(iteration.getFailureCategory()))).append(',')
                    .append(csv(defaultText(iteration.getFailureMessage()))).append(',')
                    .append(iteration.getConsoleErrors().size()).append('\n');
        }
        return csv.toString();
    }

    private String buildStepTimingCsv(List<StepTimingSummary> summaries) {
        StringBuilder csv = new StringBuilder("transaction,sample_count,min_ms,min_readable,average_ms,average_readable,median_ms,median_readable,p90_ms,p90_readable,p95_ms,p95_readable,p99_ms,p99_readable,max_ms,max_readable\n");
        for (StepTimingSummary summary : summaries) {
            csv.append(csv(summary.stepName())).append(',').append(summary.samples()).append(',')
                    .append(summary.minimumMs()).append(',').append(csv(duration(summary.minimumMs()))).append(',')
                    .append(summary.averageMs()).append(',').append(csv(duration(summary.averageMs()))).append(',')
                    .append(summary.medianMs()).append(',').append(csv(duration(summary.medianMs()))).append(',')
                    .append(summary.p90Ms()).append(',').append(csv(duration(summary.p90Ms()))).append(',')
                    .append(summary.p95Ms()).append(',').append(csv(duration(summary.p95Ms()))).append(',')
                    .append(summary.p99Ms()).append(',').append(csv(duration(summary.p99Ms()))).append(',')
                    .append(summary.maximumMs()).append(',').append(csv(duration(summary.maximumMs()))).append('\n');
        }
        return csv.toString();
    }

    private String buildTechnicalSummary(UiPerformanceRunResult result, List<StepTimingSummary> summaries) {
        StringBuilder text = new StringBuilder();
        text.append("UI PERFORMANCE AUTOMATION REPORT\n")
                .append("Run: ").append(result.getRunId()).append('\n')
                .append("Journey: ").append(result.getJourneyName()).append('\n')
                .append("Target host: ").append(result.getTargetHost()).append('\n')
                .append("Profile: ").append(result.getExecutionPlan().profileName()).append('\n')
                .append("Type: ").append(result.getExecutionPlan().testType().displayName()).append('\n')
                .append("Stages: ").append(result.getStageResults().size()).append('\n')
                .append("Maximum concurrent users: ").append(result.getExecutionPlan().maximumVirtualUsers()).append('\n')
                .append("Headless: ").append(result.getProfile().headless()).append('\n')
                .append("Browser isolation: one process per virtual user\n")
                .append("Total journeys: ").append(result.getMetrics().totalIterations()).append('\n')
                .append("Passed / failed: ").append(result.getMetrics().passedIterations()).append(" / ").append(result.getMetrics().failedIterations()).append('\n')
                .append("Error rate: ").append(format(result.getMetrics().failureRatePercent())).append("%\n")
                .append("Average journey: ").append(duration(result.getMetrics().averageJourneyDurationMs())).append('\n')
                .append("P90 / P95 / P99: ").append(duration(result.getMetrics().p90JourneyDurationMs())).append(" / ")
                .append(duration(result.getMetrics().p95JourneyDurationMs())).append(" / ").append(duration(result.getMetrics().p99JourneyDurationMs())).append('\n')
                .append("Throughput: ").append(format(result.getMetrics().completedJourneysPerSecond())).append(" journeys/s\n\n")
                .append("STAGE RESULTS\n")
                .append("Stage | Users | Ramp | Hold | Iterations/User | Samples | Error % | Avg | P95 | P99 | Throughput/s | First-start spread\n");
        for (UiPerformanceStageResult stage : result.getStageResults()) {
            text.append(stage.getStage().name()).append(" | ").append(stage.getStage().virtualUsers()).append(" | ")
                    .append(stage.getStage().rampUpSeconds()).append(" | ").append(stage.getStage().holdSeconds()).append(" | ")
                    .append(stage.getStage().iterationsPerUser()).append(" | ").append(stage.getMetrics().totalIterations()).append(" | ")
                    .append(format(stage.getMetrics().failureRatePercent())).append(" | ")
                    .append(duration(stage.getMetrics().averageJourneyDurationMs())).append(" | ")
                    .append(duration(stage.getMetrics().p95JourneyDurationMs())).append(" | ")
                    .append(duration(stage.getMetrics().p99JourneyDurationMs())).append(" | ")
                    .append(format(stage.getMetrics().completedJourneysPerSecond())).append(" | ")
                    .append(duration(stage.getFirstIterationStartSpreadMs())).append('\n');
        }
        text.append("\nTRANSACTION TIMING SUMMARY\n")
                .append("Transaction | Samples | Min | Average | Median | P90 | P95 | P99 | Max\n");
        for (StepTimingSummary summary : summaries) {
            text.append(summary.stepName()).append(" | ").append(summary.samples()).append(" | ")
                    .append(duration(summary.minimumMs())).append(" | ").append(duration(summary.averageMs())).append(" | ")
                    .append(duration(summary.medianMs())).append(" | ").append(duration(summary.p90Ms())).append(" | ")
                    .append(duration(summary.p95Ms())).append(" | ").append(duration(summary.p99Ms())).append(" | ")
                    .append(duration(summary.maximumMs())).append('\n');
        }
        text.append("\nNo credentials, tokens, input values, cookies, sessions, or full target URLs are included.\n");
        return text.toString();
    }

    private Map<String, Object> toSafeReportMap(UiPerformanceRunResult result, List<StepTimingSummary> stepTimings) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("run_id", result.getRunId());
        root.put("journey", result.getJourneyName());
        root.put("target_host", result.getTargetHost());
        root.put("started_at", TIMESTAMP.format(result.getStartedAt()));
        root.put("completed_at", TIMESTAMP.format(result.getCompletedAt()));
        root.put("profile", result.getExecutionPlan().profileName());
        root.put("test_type", result.getExecutionPlan().testType().displayName());
        root.put("maximum_concurrent_users", result.getExecutionPlan().maximumVirtualUsers());
        root.put("headless", result.getProfile().headless());
        root.put("browser_isolation", result.getProfile().browserIsolation());
        root.put("metrics", result.getMetrics());
        root.put("metrics_readable", readableMetrics(result.getMetrics()));
        List<Map<String, Object>> stages = new ArrayList<>();
        for (UiPerformanceStageResult stage : result.getStageResults()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", stage.getStage().name());
            row.put("users", stage.getStage().virtualUsers());
            row.put("ramp_up_seconds", stage.getStage().rampUpSeconds());
            row.put("hold_seconds", stage.getStage().holdSeconds());
            row.put("iterations_per_user", stage.getStage().iterationsPerUser());
            row.put("first_iteration_start_spread_ms", stage.getFirstIterationStartSpreadMs());
            row.put("first_iteration_start_spread_readable", duration(stage.getFirstIterationStartSpreadMs()));
            row.put("metrics", stage.getMetrics());
            row.put("metrics_readable", readableMetrics(stage.getMetrics()));
            stages.add(row);
        }
        root.put("stages", stages);
        root.put("transaction_timing_summary", stepTimings.stream().map(StepTimingSummary::toMap).toList());
        List<Map<String, Object>> iterations = new ArrayList<>();
        for (UiPerformanceIterationResult iteration : result.getIterations()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stage", iteration.getStageName());
            row.put("virtual_user", iteration.getUserId());
            row.put("iteration", iteration.getIteration());
            row.put("status", iteration.isPassed() ? "PASSED" : "FAILED");
            row.put("start_epoch_ms", iteration.getStartedAtEpochMs());
            row.put("complete_epoch_ms", iteration.getCompletedAtEpochMs());
            row.put("journey_duration_ms", iteration.getDurationMs());
            row.put("journey_duration_readable", duration(iteration.getDurationMs()));
            row.put("step_durations_ms", iteration.getStepDurationsMs());
            row.put("step_durations_readable", readableStepDurations(iteration.getStepDurationsMs()));
            row.put("failure_category", iteration.getFailureCategory());
            row.put("sanitized_diagnostic", iteration.getFailureMessage());
            row.put("failure_screenshot", iteration.getFailureScreenshotPath());
            row.put("console_error_count", iteration.getConsoleErrors().size());
            iterations.add(row);
        }
        root.put("iterations", iterations);
        return root;
    }

    private List<StepTimingSummary> summarizeStepTimings(UiPerformanceRunResult result) {
        Map<String, List<Long>> durationsByStep = new LinkedHashMap<>();
        for (UiPerformanceIterationResult iteration : result.getIterations()) {
            for (Map.Entry<String, Long> entry : iteration.getStepDurationsMs().entrySet()) {
                durationsByStep.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(entry.getValue());
            }
        }
        List<StepTimingSummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : durationsByStep.entrySet()) {
            List<Long> durations = new ArrayList<>(entry.getValue());
            durations.sort(Comparator.naturalOrder());
            long total = durations.stream().mapToLong(Long::longValue).sum();
            summaries.add(new StepTimingSummary(entry.getKey(), durations.size(), durations.getFirst(),
                    Math.round((double) total / durations.size()), percentile(durations, 50),
                    percentile(durations, 90), percentile(durations, 95), percentile(durations, 99), durations.getLast()));
        }
        return summaries;
    }

    private void writePdf(UiPerformanceRunResult result, List<StepTimingSummary> stepTimings, Path pdfPath) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage summaryPage = new PDPage(PDRectangle.LETTER);
            document.addPage(summaryPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, summaryPage)) {
                float y = 740;
                y = writeLine(stream, y, "UI Performance Automation Report", 16, true);
                y -= 8;
                y = writeLine(stream, y, "Run: " + result.getRunId(), 10, false);
                y = writeLine(stream, y, "Journey: " + result.getJourneyName(), 10, false);
                y = writeLine(stream, y, "Profile: " + result.getExecutionPlan().profileName() + " | Type: " + result.getExecutionPlan().testType().displayName(), 10, false);
                y = writeLine(stream, y, "Maximum users: " + result.getExecutionPlan().maximumVirtualUsers() + " | Headless: " + result.getProfile().headless() + " | Process isolation", 10, false);
                y = writeLine(stream, y, "Samples: " + result.getMetrics().totalIterations() + " | Passed: " + result.getMetrics().passedIterations() + " | Failed: " + result.getMetrics().failedIterations() + " | Error: " + format(result.getMetrics().failureRatePercent()) + "%", 10, false);
                y = writeLine(stream, y, "Average: " + duration(result.getMetrics().averageJourneyDurationMs()) + " | P90: " + duration(result.getMetrics().p90JourneyDurationMs()) + " | P95: " + duration(result.getMetrics().p95JourneyDurationMs()) + " | P99: " + duration(result.getMetrics().p99JourneyDurationMs()), 10, false);
                y = writeLine(stream, y, "Throughput: " + format(result.getMetrics().completedJourneysPerSecond()) + " journeys/s", 10, false);
                y -= 12;
                y = writeLine(stream, y, "Stage Results", 13, true);
                for (UiPerformanceStageResult stage : result.getStageResults()) {
                    if (y < 70) break;
                    y = writeLine(stream, y, stage.getStage().name() + " | users " + stage.getStage().virtualUsers()
                            + " | samples " + stage.getMetrics().totalIterations() + " | error " + format(stage.getMetrics().failureRatePercent())
                            + "% | avg " + duration(stage.getMetrics().averageJourneyDurationMs()) + " | p95 " + duration(stage.getMetrics().p95JourneyDurationMs())
                            + " | throughput " + format(stage.getMetrics().completedJourneysPerSecond()) + "/s", 9, false);
                }
            }

            PDPage transactionsPage = new PDPage(PDRectangle.LETTER);
            document.addPage(transactionsPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, transactionsPage)) {
                float y = 740;
                y = writeLine(stream, y, "Browser Transaction Timing Summary", 16, true);
                y -= 8;
                y = writeLine(stream, y, "Transaction | Samples | Min | Average | Median | P90 | P95 | P99 | Max", 9, true);
                for (StepTimingSummary summary : stepTimings) {
                    if (y < 60) break;
                    y = writeLine(stream, y, summary.stepName() + " | " + summary.samples() + " | "
                            + duration(summary.minimumMs()) + " | " + duration(summary.averageMs()) + " | " + duration(summary.medianMs()) + " | "
                            + duration(summary.p90Ms()) + " | " + duration(summary.p95Ms()) + " | " + duration(summary.p99Ms()) + " | " + duration(summary.maximumMs()), 9, false);
                }
            }
            document.save(pdfPath.toFile());
        }
    }

    private static long percentile(List<Long> sortedDurations, double percentile) {
        int rank = (int) Math.ceil((percentile / 100.0) * sortedDurations.size());
        return sortedDurations.get(Math.max(0, rank - 1));
    }

    private float writeLine(PDPageContentStream stream, float y, String text, int size, boolean bold) throws IOException {
        stream.beginText();
        stream.setFont(bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA, size);
        stream.newLineAtOffset(50, y);
        stream.showText(pdfSafe(text.length() > 115 ? text.substring(0, 112) + "..." : text));
        stream.endText();
        return y - (size + 6);
    }

    private static String formatStepDurationsHtml(Map<String, Long> stepDurations) {
        if (stepDurations.isEmpty()) return "-";
        StringBuilder output = new StringBuilder("<span class=\"timing\">");
        for (Map.Entry<String, Long> entry : stepDurations.entrySet()) {
            output.append(escape(entry.getKey())).append(": ").append(duration(entry.getValue())).append("<br>");
        }
        return output.append("</span>").toString();
    }

    private static String formatStepDurationsText(Map<String, Long> stepDurations) {
        if (stepDurations.isEmpty()) return "-";
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, Long> entry : stepDurations.entrySet()) {
            if (!output.isEmpty()) output.append("; ");
            output.append(entry.getKey()).append(": ").append(entry.getValue()).append(" ms");
        }
        return output.toString();
    }

    private static String formatStepDurationsReadableText(Map<String, Long> stepDurations) {
        if (stepDurations.isEmpty()) return "-";
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, Long> entry : stepDurations.entrySet()) {
            if (!output.isEmpty()) output.append("; ");
            output.append(entry.getKey()).append(": ").append(duration(entry.getValue()));
        }
        return output.toString();
    }

    private static Map<String, String> readableStepDurations(Map<String, Long> stepDurations) {
        Map<String, String> readable = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : stepDurations.entrySet()) {
            readable.put(entry.getKey(), duration(entry.getValue()));
        }
        return readable;
    }

    private static Map<String, String> readableMetrics(com.ptaf.ui_performance.metrics.UiPerformanceMetrics metrics) {
        Map<String, String> readable = new LinkedHashMap<>();
        readable.put("minimum_journey_duration", duration(metrics.minimumJourneyDurationMs()));
        readable.put("average_journey_duration", duration(metrics.averageJourneyDurationMs()));
        readable.put("median_journey_duration", duration(metrics.medianJourneyDurationMs()));
        readable.put("p90_journey_duration", duration(metrics.p90JourneyDurationMs()));
        readable.put("p95_journey_duration", duration(metrics.p95JourneyDurationMs()));
        readable.put("p99_journey_duration", duration(metrics.p99JourneyDurationMs()));
        readable.put("maximum_journey_duration", duration(metrics.maximumJourneyDurationMs()));
        return readable;
    }

    private static String duration(long milliseconds) { return UiPerformanceDurationFormatter.format(milliseconds); }

    private static String defaultText(String text) { return text == null || text.isBlank() ? "-" : text; }
    private static String format(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static String pdfSafe(String value) { return value.replaceAll("[^\\x20-\\x7E]", "?"); }

    private record StepTimingSummary(String stepName,
                                     int samples,
                                     long minimumMs,
                                     long averageMs,
                                     long medianMs,
                                     long p90Ms,
                                     long p95Ms,
                                     long p99Ms,
                                     long maximumMs) {
        private Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("transaction", stepName);
            row.put("sample_count", samples);
            row.put("min_ms", minimumMs);
            row.put("min_readable", duration(minimumMs));
            row.put("average_ms", averageMs);
            row.put("average_readable", duration(averageMs));
            row.put("median_ms", medianMs);
            row.put("median_readable", duration(medianMs));
            row.put("p90_ms", p90Ms);
            row.put("p90_readable", duration(p90Ms));
            row.put("p95_ms", p95Ms);
            row.put("p95_readable", duration(p95Ms));
            row.put("p99_ms", p99Ms);
            row.put("p99_readable", duration(p99Ms));
            row.put("max_ms", maximumMs);
            row.put("max_readable", duration(maximumMs));
            return row;
        }
    }
}
