package com.ptaf.ui_performance.metrics;

import com.ptaf.ui_performance.model.UiPerformanceIterationResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calculates browser-journey load metrics from completed real-browser samples. */
public record UiPerformanceMetrics(
        int totalIterations,
        int passedIterations,
        int failedIterations,
        double failureRatePercent,
        long minimumJourneyDurationMs,
        long averageJourneyDurationMs,
        long medianJourneyDurationMs,
        long p90JourneyDurationMs,
        long p95JourneyDurationMs,
        long p99JourneyDurationMs,
        long maximumJourneyDurationMs,
        double completedJourneysPerSecond,
        double completedJourneysPerMinute) {

    /** Produces a performance summary from final journey samples and wall-clock stage/run duration. */
    public static UiPerformanceMetrics from(List<UiPerformanceIterationResult> results, long runDurationMs) {
        if (results == null || results.isEmpty()) {
            return new UiPerformanceMetrics(0, 0, 0, 0.0,
                    0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0, 0.0);
        }
        List<Long> durations = new ArrayList<>();
        int passed = 0;
        for (UiPerformanceIterationResult result : results) {
            durations.add(result.getDurationMs());
            if (result.isPassed()) {
                passed++;
            }
        }
        durations.sort(Comparator.naturalOrder());
        long totalDuration = durations.stream().mapToLong(Long::longValue).sum();
        int total = results.size();
        int failed = total - passed;
        double failureRate = (failed * 100.0) / total;
        double perSecond = runDurationMs <= 0 ? 0.0 : (total * 1_000.0) / runDurationMs;
        return new UiPerformanceMetrics(
                total,
                passed,
                failed,
                failureRate,
                durations.getFirst(),
                Math.round((double) totalDuration / total),
                percentile(durations, 50.0),
                percentile(durations, 90.0),
                percentile(durations, 95.0),
                percentile(durations, 99.0),
                durations.getLast(),
                perSecond,
                perSecond * 60.0);
    }

    /** Uses nearest-rank percentile selection for deterministic cross-report calculations. */
    private static long percentile(List<Long> sortedDurations, double percentile) {
        int rank = (int) Math.ceil((percentile / 100.0) * sortedDurations.size());
        return sortedDurations.get(Math.max(0, rank - 1));
    }
}
