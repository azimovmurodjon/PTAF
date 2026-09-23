package com.ptaf.ui_performance.model;

import com.ptaf.ui_performance.metrics.UiPerformanceMetrics;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Aggregates a complete load, stress, spike, or soak UI performance run. */
public final class UiPerformanceRunResult {
    private final String runId;
    private final String journeyName;
    private final String targetHost;
    private final Instant startedAt;
    private final Instant completedAt;
    private final UiPerformanceRunProfile profile;
    private final List<UiPerformanceStageResult> stageResults;
    private final List<UiPerformanceIterationResult> iterations;
    private final UiPerformanceMetrics metrics;
    private final Path reportDirectory;

    public UiPerformanceRunResult(String runId,
                                  String journeyName,
                                  String targetHost,
                                  Instant startedAt,
                                  Instant completedAt,
                                  UiPerformanceRunProfile profile,
                                  List<UiPerformanceStageResult> stageResults,
                                  List<UiPerformanceIterationResult> iterations,
                                  UiPerformanceMetrics metrics,
                                  Path reportDirectory) {
        this.runId = runId;
        this.journeyName = journeyName;
        this.targetHost = targetHost;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.profile = profile;
        this.stageResults = List.copyOf(stageResults);
        this.iterations = List.copyOf(iterations);
        this.metrics = metrics;
        this.reportDirectory = reportDirectory;
    }

    /** Backward-compatible constructor for report contract tests representing one synthetic stage. */
    public UiPerformanceRunResult(String runId,
                                  String journeyName,
                                  String targetHost,
                                  Instant startedAt,
                                  Instant completedAt,
                                  UiPerformanceRunProfile profile,
                                  List<UiPerformanceIterationResult> iterations,
                                  UiPerformanceMetrics metrics,
                                  Path reportDirectory) {
        this(runId, journeyName, targetHost, startedAt, completedAt, profile,
                List.of(new UiPerformanceStageResult(
                        profile.executionPlan().stages().getFirst(), startedAt, completedAt, iterations, metrics)),
                iterations, metrics, reportDirectory);
    }

    public String getRunId() { return runId; }
    public String getJourneyName() { return journeyName; }
    public String getTargetHost() { return targetHost; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public UiPerformanceRunProfile getProfile() { return profile; }
    public UiPerformanceExecutionPlan getExecutionPlan() { return profile.executionPlan(); }
    public List<UiPerformanceStageResult> getStageResults() { return stageResults; }
    public List<UiPerformanceIterationResult> getIterations() { return iterations; }
    public UiPerformanceMetrics getMetrics() { return metrics; }
    public Path getReportDirectory() { return reportDirectory; }
}
