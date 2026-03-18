package com.ptaf.performance.assertions;

import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceExecutionResult;

/**
 * Central SLA validation layer for performance executions.
 *
 * <p>This engine validates the final execution result against the configured
 * performance assertion profile and throws readable assertion messages when
 * thresholds are exceeded.</p>
 */
public class PerformanceAssertionEngine {

    public void validate(PerformanceExecutionResult result, PerformanceAssertionProfile profile) {
        if (result == null) {
            throw new IllegalArgumentException("PerformanceExecutionResult cannot be null.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("PerformanceAssertionProfile cannot be null.");
        }

        assertErrorPercent(result, profile.getMaxErrorPercent());
        assertAverageResponseTime(result, profile.getMaxAverageResponseTimeMs());
        assertP95ResponseTime(result, profile.getMaxP95ResponseTimeMs());
    }

    public void assertErrorPercent(PerformanceExecutionResult result, double maxAllowedPercent) {
        if (result.getErrorPercent() > maxAllowedPercent) {
            throw new AssertionError(
                    "Performance validation failed because the error rate was too high. "
                            + "Actual error percent: " + result.getErrorPercent() + "%, "
                            + "allowed maximum: " + maxAllowedPercent + "%. "
                            + "Total failed requests: " + result.getTotalErrors() + " out of "
                            + result.getTotalSamples() + " total requests."
            );
        }
    }

    public void assertAverageResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        if (result.getAverageResponseTimeMs() > maxAllowedMs) {
            throw new AssertionError(
                    "Performance validation failed because the average response time was slower than allowed. "
                            + "Actual average response time: " + result.getAverageResponseTimeMs() + " ms, "
                            + "allowed maximum: " + maxAllowedMs + " ms. "
                            + "This means the system responded slower than the configured average-time threshold."
            );
        }
    }

    public void assertP95ResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        if (result.getP95ResponseTimeMs() > maxAllowedMs) {
            throw new AssertionError(
                    "Performance validation failed because the p95 response time was slower than allowed. "
                            + "Actual p95 response time: " + result.getP95ResponseTimeMs() + " ms, "
                            + "allowed maximum: " + maxAllowedMs + " ms. "
                            + "This means at least 95% of requests were not completed within the expected threshold."
            );
        }
    }
}