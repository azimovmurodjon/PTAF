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
 *
 * <p>Usage notes for testers:
 * <ul>
 *   <li>Call validate(...) with the execution result and the assertion profile
 *       produced/configured for the scenario under test.</li>
 *   <li>When an assertion fails an {@link AssertionError} is thrown with a
 *       human-readable message suitable for inclusion in reports or logs.</li>
 *   <li>Numeric inputs that are invalid (NaN, infinite, negative for doubles)
 *       are sanitized before comparison to avoid spurious failures.</li>
 * </ul>
 * </p>
 */
public class PerformanceAssertionEngine {

    /**
     * Validate the given performance execution result against the provided
     * assertion profile. This method performs a sequence of checks:
     * <ol>
     *     <li>Error percentage check</li>
     *     <li>Average response time check</li>
     *     <li>P95 response time check</li>
     * </ol>
     *
     * <p>If any configured threshold is exceeded, an {@link AssertionError} is
     * thrown describing the failure in human-readable form. Caller code (tests,
     * CI) should treat an AssertionError as a test failure.</p>
     *
     * @param result  the execution result to validate; must not be null
     * @param profile the assertion profile containing threshold values; must not be null
     * @throws IllegalArgumentException if either {@code result} or {@code profile} is null
     * @throws AssertionError if any configured SLA threshold is breached
     */
    public void validate(PerformanceExecutionResult result, PerformanceAssertionProfile profile) {
        // Basic null checks to provide a clearer failure mode if caller passed invalid arguments.
        if (result == null) {
            throw new IllegalArgumentException("PerformanceExecutionResult cannot be null.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("PerformanceAssertionProfile cannot be null.");
        }

        // Validate against configured maximums in the profile. Each method throws
        // an AssertionError with a clear message on failure.
        assertErrorPercent(result, profile.getMaxErrorPercent());
        assertAverageResponseTime(result, profile.getMaxAverageResponseTimeMs());
        assertP95ResponseTime(result, profile.getMaxP95ResponseTimeMs());
    }

    /**
     * Assert that the error percent of the execution result does not exceed the
     * provided maximum allowed percent.
     *
     * <p>Numeric safety:
     * <ul>
     *   <li>Any NaN, infinite or negative measured values are treated as 0.0 for comparison purposes.</li>
     *   <li>The configured maximum is also passed through the same safety check to avoid spurious failures.</li>
     * </ul>
     * </p>
     *
     * @param result           the execution result containing error statistics; must not be null
     * @param maxAllowedPercent the maximum allowed error rate (percentage). A typical value is between 0.0 and 100.0.
     * @throws AssertionError when the actual error percent is greater than the allowed percent
     */
    public void assertErrorPercent(PerformanceExecutionResult result, double maxAllowedPercent) {
        // Retrieve and sanitize the actual error rate from the result object.
        double actualErrorPercent = safeDouble(result.getErrorPercent());

        // Compare sanitized actual value with sanitized configured maximum.
        if (actualErrorPercent > safeDouble(maxAllowedPercent)) {
            // Build a clear, report-friendly failure message. Use the helper to format numbers
            // consistently for Excel/TXT reporting consumers.
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

    /**
     * Assert that the average response time of the execution result does not
     * exceed the provided maximum allowed milliseconds.
     *
     * @param result      the execution result containing timing metrics; must not be null
     * @param maxAllowedMs the maximum allowed average response time in milliseconds (non-negative)
     * @throws AssertionError when the actual average response time is greater than the allowed maximum
     */
    public void assertAverageResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        // Guarded retrieval: ensure value is non-negative before comparing.
        long actualAverageMs = safeLong(result.getAverageResponseTimeMs());

        // If the observed average is larger than the configured SLA threshold, fail with detail.
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

    /**
     * Assert that the 95th percentile (P95) response time of the execution
     * result does not exceed the provided maximum allowed milliseconds.
     *
     * <p>The P95 metric is commonly used to express a near-worst-case user
     * experience (95% of requests are faster than this value).</p>
     *
     * @param result      the execution result containing percentile timings; must not be null
     * @param maxAllowedMs the maximum allowed P95 response time in milliseconds (non-negative)
     * @throws AssertionError when the actual P95 response time is greater than the allowed maximum
     */
    public void assertP95ResponseTime(PerformanceExecutionResult result, long maxAllowedMs) {
        // Ensure non-negative P95 value for safe comparison.
        long actualP95Ms = safeLong(result.getP95ResponseTimeMs());

        // Fail if P95 exceeds configured threshold. Message explains implication to testers/readers.
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

    /**
     * Safe-normalize a double input used for percentage comparisons. This method
     * converts invalid values to a safe comparison baseline (0.0).
     *
     * <p>Invalid values handled:
     * <ul>
     *   <li>Double.NaN</li>
     *   <li>Infinite values (Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)</li>
     *   <li>Negative values (not meaningful for percent metrics)</li>
     * </ul>
     * </p>
     *
     * @param value raw double value to sanitize
     * @return a non-negative finite double (0.0 if input is invalid)
     */
    private double safeDouble(double value) {
        // Defensive handling of edge cases to avoid throwing exceptions during comparisons
        // and to ensure consistent behavior when test tooling reports strange numeric values.
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    /**
     * Safe-normalize a long input used for time/size comparisons. Negative values
     * are clamped to zero since negative durations/sizes are not meaningful here.
     *
     * @param value raw long value to sanitize
     * @return the original value if non-negative, otherwise 0L
     */
    private long safeLong(long value) {
        // Clamp negative values to zero for predictable comparisons.
        return Math.max(value, 0L);
    }
}
