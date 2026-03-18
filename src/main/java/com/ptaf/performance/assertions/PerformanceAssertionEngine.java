package com.ptaf.performance.assertions;

import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceExecutionResult;

/**
 * Central SLA validation layer for performance executions.
 */
public class PerformanceAssertionEngine {

    public void validate(PerformanceExecutionResult result, PerformanceAssertionProfile profile) {
        assertErrorPercent(result, profile.getMaxErrorPercent());
        assertAverageResponseTime(result, profile.getMaxAverageResponseTimeMs());
        assertP95ResponseTime(result, profile.getMaxP95ResponseTimeMs());
    }

    public void assertErrorPercent(PerformanceExecutionResult result, double maxAllowedPercent) {
        if (result.getErrorPercent() > maxAllowedPercent) {
            throw new AssertionError(
                    "Performance assertion failed: error percent was " + result.getErrorPercent()
                            + "% but max allowed is " + maxAllowedPercent + "%"
            );
        }
    }

    public void assertAverageResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        if (result.getAverageResponseTimeMs() > maxAllowedMs) {
            throw new AssertionError(
                    "Performance assertion failed: average response time was "
                            + result.getAverageResponseTimeMs()
                            + " ms but max allowed is " + maxAllowedMs + " ms"
            );
        }
    }

    public void assertP95ResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        if (result.getP95ResponseTimeMs() > maxAllowedMs) {
            throw new AssertionError(
                    "Performance assertion failed: p95 response time was "
                            + result.getP95ResponseTimeMs()
                            + " ms but max allowed is " + maxAllowedMs + " ms"
            );
        }
    }
}