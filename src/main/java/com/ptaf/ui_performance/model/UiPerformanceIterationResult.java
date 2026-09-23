package com.ptaf.ui_performance.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One completed browser journey for one virtual user in one configured performance stage.
 *
 * <p>The model records timing and sanitized evidence only. It never carries form values, passwords,
 * tokens, cookies, or session data.</p>
 */
public final class UiPerformanceIterationResult {
    private final String stageName;
    private final String userId;
    private final int iteration;
    private final boolean passed;
    private final long startedAtEpochMs;
    private final long completedAtEpochMs;
    private final long durationMs;
    private final Map<String, Long> stepDurationsMs;
    private final String failureCategory;
    private final String failureMessage;
    private final String failureScreenshotPath;
    private final List<String> consoleErrors;

    public UiPerformanceIterationResult(String stageName,
                                        String userId,
                                        int iteration,
                                        boolean passed,
                                        long startedAtEpochMs,
                                        long completedAtEpochMs,
                                        long durationMs,
                                        Map<String, Long> stepDurationsMs,
                                        String failureCategory,
                                        String failureMessage,
                                        String failureScreenshotPath,
                                        List<String> consoleErrors) {
        this.stageName = stageName == null ? "" : stageName.trim();
        this.userId = userId;
        this.iteration = iteration;
        this.passed = passed;
        this.startedAtEpochMs = Math.max(0L, startedAtEpochMs);
        this.completedAtEpochMs = Math.max(0L, completedAtEpochMs);
        this.durationMs = Math.max(0L, durationMs);
        this.stepDurationsMs = Collections.unmodifiableMap(new LinkedHashMap<>(stepDurationsMs));
        this.failureCategory = failureCategory;
        this.failureMessage = failureMessage;
        this.failureScreenshotPath = failureScreenshotPath;
        this.consoleErrors = List.copyOf(consoleErrors);
    }

    /** Backward-compatible constructor used by existing offline tests and report adapters. */
    public UiPerformanceIterationResult(String userId,
                                        int iteration,
                                        boolean passed,
                                        long durationMs,
                                        Map<String, Long> stepDurationsMs,
                                        String failureCategory,
                                        String failureMessage,
                                        String failureScreenshotPath,
                                        List<String> consoleErrors) {
        this("default", userId, iteration, passed, 0L, 0L, durationMs, stepDurationsMs,
                failureCategory, failureMessage, failureScreenshotPath, consoleErrors);
    }

    public String getStageName() { return stageName; }
    public String getUserId() { return userId; }
    public int getIteration() { return iteration; }
    public boolean isPassed() { return passed; }
    public long getStartedAtEpochMs() { return startedAtEpochMs; }
    public long getCompletedAtEpochMs() { return completedAtEpochMs; }
    public long getDurationMs() { return durationMs; }
    public Map<String, Long> getStepDurationsMs() { return stepDurationsMs; }
    public String getFailureCategory() { return failureCategory; }
    public String getFailureMessage() { return failureMessage; }
    public String getFailureScreenshotPath() { return failureScreenshotPath; }
    public List<String> getConsoleErrors() { return consoleErrors; }
}
