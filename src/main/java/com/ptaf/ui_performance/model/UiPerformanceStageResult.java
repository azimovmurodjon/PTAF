package com.ptaf.ui_performance.model;

import com.ptaf.ui_performance.metrics.UiPerformanceMetrics;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Final metrics and evidence for one configured UI performance stage. */
public final class UiPerformanceStageResult {
    private final UiPerformanceStage stage;
    private final Instant startedAt;
    private final Instant completedAt;
    private final List<UiPerformanceIterationResult> iterations;
    private final UiPerformanceMetrics metrics;

    public UiPerformanceStageResult(UiPerformanceStage stage,
                                    Instant startedAt,
                                    Instant completedAt,
                                    List<UiPerformanceIterationResult> iterations,
                                    UiPerformanceMetrics metrics) {
        this.stage = stage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.iterations = List.copyOf(iterations);
        this.metrics = metrics;
    }

    public UiPerformanceStage getStage() { return stage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public List<UiPerformanceIterationResult> getIterations() { return iterations; }
    public UiPerformanceMetrics getMetrics() { return metrics; }
    public long getDurationMs() { return Math.max(0L, Duration.between(startedAt, completedAt).toMillis()); }

    /** Difference between the earliest and latest first-iteration start for this stage. */
    public long getFirstIterationStartSpreadMs() {
        return iterations.stream()
                .filter(result -> result.getIteration() == 1)
                .mapToLong(UiPerformanceIterationResult::getStartedAtEpochMs)
                .filter(value -> value > 0L)
                .summaryStatistics()
                .getCount() < 2
                ? 0L
                : latestFirstStart() - earliestFirstStart();
    }

    private long earliestFirstStart() {
        return iterations.stream()
                .filter(result -> result.getIteration() == 1 && result.getStartedAtEpochMs() > 0L)
                .mapToLong(UiPerformanceIterationResult::getStartedAtEpochMs)
                .min().orElse(0L);
    }

    private long latestFirstStart() {
        return iterations.stream()
                .filter(result -> result.getIteration() == 1 && result.getStartedAtEpochMs() > 0L)
                .mapToLong(UiPerformanceIterationResult::getStartedAtEpochMs)
                .max().orElse(0L);
    }
}
