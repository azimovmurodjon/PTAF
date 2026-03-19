package com.ptaf.performance.assertions;

import com.ptaf.performance.models.PerformanceAssertionProfile;
import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.reports.PerformanceExcelFormatHelper;

/**
 * Central SLA validation layer for performance executions.
 *
 * <p>This engine validates the final execution result against the configured
 * performance assertion profile and throws readable assertion messages when
 * thresholds are exceeded.</p>
 *
 * <p>Reporting-safe goals:
 * <ul>
 *   <li>keep validation behavior simple and stable</li>
 *   <li>improve assertion readability for Excel/TXT summaries</li>
 *   <li>avoid changing execution contract</li>
 * </ul>
 * </p>
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
        double actualErrorPercent = safeDouble(result.getErrorPercent());

        if (actualErrorPercent > safeDouble(maxAllowedPercent)) {
            throw new AssertionError(
                    "Performance validation failed because the error rate exceeded the configured threshold. "
                            + "Scenario: " + result.getSafeTestName() + ". "
                            + "Actual error percent: " + PerformanceExcelFormatHelper.formatPercent(actualErrorPercent) + ", "
                            + "allowed maximum: " + PerformanceExcelFormatHelper.formatPercent(maxAllowedPercent) + ". "
                            + "Total failed requests: " + PerformanceExcelFormatHelper.formatInteger(result.getTotalErrors()) + " out of "
                            + PerformanceExcelFormatHelper.formatInteger(result.getTotalSamples()) + " total requests."
            );
        }
    }

    public void assertAverageResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        long actualAverageMs = safeLong(result.getAverageResponseTimeMs());

        if (actualAverageMs > safeLong(maxAllowedMs)) {
            throw new AssertionError(
                    "Performance validation failed because the average response time exceeded the configured threshold. "
                            + "Scenario: " + result.getSafeTestName() + ". "
                            + "Actual average response time: " + PerformanceExcelFormatHelper.formatMillisecondsDetailed(actualAverageMs) + ", "
                            + "allowed maximum: " + PerformanceExcelFormatHelper.formatMillisecondsDetailed(maxAllowedMs) + ". "
                            + "This indicates the system responded slower than the configured average-time expectation."
            );
        }
    }

    public void assertP95ResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        long actualP95Ms = safeLong(result.getP95ResponseTimeMs());

        if (actualP95Ms > safeLong(maxAllowedMs)) {
            throw new AssertionError(
                    "Performance validation failed because the P95 response time exceeded the configured threshold. "
                            + "Scenario: " + result.getSafeTestName() + ". "
                            + "Actual P95 response time: " + PerformanceExcelFormatHelper.formatMillisecondsDetailed(actualP95Ms) + ", "
                            + "allowed maximum: " + PerformanceExcelFormatHelper.formatMillisecondsDetailed(maxAllowedMs) + ". "
                            + "This means at least 95% of requests were not completed within the expected response-time target."
            );
        }
    }

    private double safeDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    private long safeLong(long value) {
        return Math.max(value, 0L);
    }
}