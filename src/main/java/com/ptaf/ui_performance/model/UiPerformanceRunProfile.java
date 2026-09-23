package com.ptaf.ui_performance.model;

/**
 * Immutable runtime, browser, safety, and threshold settings for one UI performance run.
 * The selected load shape is held by {@link UiPerformanceExecutionPlan}.
 */
public record UiPerformanceRunProfile(
        UiPerformanceExecutionPlan executionPlan,
        long synchronizedStartTimeoutMs,
        long betweenIterationsMs,
        long actionTimeoutMs,
        long navigationTimeoutMs,
        boolean headless,
        boolean ignoreHttpsErrors,
        String browserIsolation,
        String userAgent,
        boolean captureFailureScreenshots,
        boolean captureConsoleErrors,
        int maxVirtualUsers,
        double maximumFailureRatePercent,
        long maximumAverageJourneyDurationMs,
        long maximumP95JourneyDurationMs) {

    /** Validates every config-controlled runtime setting before a browser is launched. */
    public void validate() {
        if (maxVirtualUsers < 1) {
            throw new IllegalArgumentException("ui_performance.safety.max_virtual_users must be at least 1.");
        }
        if (executionPlan == null) {
            throw new IllegalArgumentException("UI performance execution plan cannot be null.");
        }
        executionPlan.validate(maxVirtualUsers);
        if (synchronizedStartTimeoutMs < 1 || betweenIterationsMs < 0
                || actionTimeoutMs < 1 || navigationTimeoutMs < 1) {
            throw new IllegalArgumentException(
                    "UI performance timeouts must be positive; between_iterations_ms may be zero.");
        }
        if (!"process".equalsIgnoreCase(browserIsolation)) {
            throw new IllegalArgumentException(
                    "ui_performance.browser.isolation must be 'process'. Playwright Java objects are not thread safe, so real concurrent users require one Playwright/browser process per worker.");
        }
        if (userAgent == null || userAgent.isBlank()) {
            throw new IllegalArgumentException("ui_performance.browser.user_agent cannot be blank.");
        }
        if (maximumFailureRatePercent < 0 || maximumFailureRatePercent > 100) {
            throw new IllegalArgumentException("ui_performance.thresholds.maximum_failure_rate_percent must be between 0 and 100.");
        }
        if (maximumAverageJourneyDurationMs < 1 || maximumP95JourneyDurationMs < 1) {
            throw new IllegalArgumentException("UI performance average and P95 journey thresholds must be at least 1 ms.");
        }
    }
}
