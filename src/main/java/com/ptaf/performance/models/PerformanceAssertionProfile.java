package com.ptaf.performance.models;

/**
 * Defines SLA expectations for a performance run.
 *
 * <p>Reporting-safe goals:
 * <ul>
 *   <li>keep constructor compatibility</li>
 *   <li>normalize invalid threshold values</li>
 *   <li>provide small helper methods for reporting/risk logic</li>
 * </ul>
 * </p>
 *
 * <p>
 * This immutable value object encapsulates three asserted thresholds:
 * <ul>
 *   <li>maxErrorPercent - maximum allowed percentage of errors (double)</li>
 *   <li>maxAverageResponseTimeMs - maximum allowed average response time in milliseconds (long)</li>
 *   <li>maxP95ResponseTimeMs - maximum allowed 95th percentile response time in milliseconds (long)</li>
 * </ul>
 * Any negative, NaN or infinite values provided to the constructor are normalized to 0 (disabled).
 * A threshold value of 0 (or less, after normalization) is treated as "not configured" / disabled.
 * </p>
 *
 * <p>
 * Typical usage:
 * <pre>
 * PerformanceAssertionProfile profile = new PerformanceAssertionProfile(1.0, 200, 500);
 * if (profile.hasAnyBreach(actualErrorPercent, actualAvgMs, actualP95Ms)) {
 *     // handle SLA breach
 * }
 * </pre>
 * </p>
 */
public class PerformanceAssertionProfile {

    /**
     * Maximum allowed error percentage. Normalized in constructor via {@link #sanitizePercent(double)}.
     * A value of 0.0 indicates the error-percentage check is disabled/unconfigured.
     */
    private final double maxErrorPercent;

    /**
     * Maximum allowed average response time in milliseconds. Normalized in constructor via {@link #sanitizeLong(long)}.
     * A value of 0L indicates the average-response-time check is disabled/unconfigured.
     */
    private final long maxAverageResponseTimeMs;

    /**
     * Maximum allowed 95th-percentile response time in milliseconds. Normalized in constructor via {@link #sanitizeLong(long)}.
     * A value of 0L indicates the P95-response-time check is disabled/unconfigured.
     */
    private final long maxP95ResponseTimeMs;

    /**
     * Construct a new PerformanceAssertionProfile.
     *
     * <p>Inputs are sanitized to ensure they are reporting-safe:
     * <ul>
     *   <li>NaN, infinite, or negative percentages are treated as 0.0 (disabled)</li>
     *   <li>Negative response times are clipped to 0L (disabled)</li>
     * </ul>
     * </p>
     *
     * @param maxErrorPercent maximum allowed error percent (as a percentage, e.g. 1.5 means 1.5%)
     * @param maxAverageResponseTimeMs maximum allowed average response time in milliseconds
     * @param maxP95ResponseTimeMs maximum allowed 95th percentile response time in milliseconds
     */
    public PerformanceAssertionProfile(double maxErrorPercent,
                                       long maxAverageResponseTimeMs,
                                       long maxP95ResponseTimeMs) {
        // Normalize incoming values so the internal state is always valid and safe to report.
        this.maxErrorPercent = sanitizePercent(maxErrorPercent);
        this.maxAverageResponseTimeMs = sanitizeLong(maxAverageResponseTimeMs);
        this.maxP95ResponseTimeMs = sanitizeLong(maxP95ResponseTimeMs);
    }

    /**
     * Get the configured maximum error percentage threshold.
     *
     * @return the normalized maximum error percent; 0.0 means the check is disabled/not configured
     */
    public double getMaxErrorPercent() {
        return maxErrorPercent;
    }

    /**
     * Get the configured maximum average response time threshold in milliseconds.
     *
     * @return the normalized maximum average response time (ms); 0L means the check is disabled/not configured
     */
    public long getMaxAverageResponseTimeMs() {
        return maxAverageResponseTimeMs;
    }

    /**
     * Get the configured maximum 95th-percentile response time threshold in milliseconds.
     *
     * @return the normalized maximum P95 response time (ms); 0L means the check is disabled/not configured
     */
    public long getMaxP95ResponseTimeMs() {
        return maxP95ResponseTimeMs;
    }

    /**
     * Determine if any threshold has been configured (i.e., is greater than 0).
     *
     * <p>Thresholds set to 0 are treated as disabled and therefore not considered configured.</p>
     *
     * @return true if at least one of the thresholds is enabled (greater than zero); false otherwise
     */
    public boolean hasConfiguredThresholds() {
        // If any configured threshold is greater than zero, the profile contains at least one active check.
        return maxErrorPercent > 0.0
                || maxAverageResponseTimeMs > 0L
                || maxP95ResponseTimeMs > 0L;
    }

    /**
     * Check whether the actual error percentage breaches the configured maximum.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If the configured maximum is 0.0 (disabled), this method always returns false.</li>
     *   <li>The actual value is normalized via {@link #safeDouble(double)} to protect against NaN/Infinite/negative values.</li>
     * </ul>
     * </p>
     *
     * @param actualErrorPercent the observed error percentage to evaluate
     * @return true if the actual (normalized) error percent is greater than the configured maximum and the check is enabled
     */
    public boolean isErrorPercentBreached(double actualErrorPercent) {
        // Only evaluate when a positive threshold is configured. Normalize the actual value before comparing.
        return maxErrorPercent > 0.0 && safeDouble(actualErrorPercent) > maxErrorPercent;
    }

    /**
     * Check whether the actual average response time breaches the configured maximum.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If the configured maximum is 0L (disabled), this method always returns false.</li>
     *   <li>The actual value is normalized via {@link #safeLong(long)} to prevent negative values.</li>
     * </ul>
     * </p>
     *
     * @param actualAverageResponseTimeMs the observed average response time in milliseconds
     * @return true if the actual (normalized) average response time is greater than the configured maximum and the check is enabled
     */
    public boolean isAverageResponseBreached(long actualAverageResponseTimeMs) {
        // Only evaluate when a positive threshold is configured. Normalize the actual value before comparing.
        return maxAverageResponseTimeMs > 0L && safeLong(actualAverageResponseTimeMs) > maxAverageResponseTimeMs;
    }

    /**
     * Check whether the actual 95th-percentile response time breaches the configured maximum.
     *
     * <p>Behavior:
     * <ul>
     *   <li>If the configured maximum is 0L (disabled), this method always returns false.</li>
     *   <li>The actual value is normalized via {@link #safeLong(long)} to prevent negative values.</li>
     * </ul>
     * </p>
     *
     * @param actualP95ResponseTimeMs the observed P95 response time in milliseconds
     * @return true if the actual (normalized) P95 response time is greater than the configured maximum and the check is enabled
     */
    public boolean isP95ResponseBreached(long actualP95ResponseTimeMs) {
        // Only evaluate when a positive threshold is configured. Normalize the actual value before comparing.
        return maxP95ResponseTimeMs > 0L && safeLong(actualP95ResponseTimeMs) > maxP95ResponseTimeMs;
    }

    /**
     * Check if any of the configured thresholds are breached.
     *
     * <p>This is a convenience method that checks error percent, average response and P95 response in turn.
     * Each individual check is responsible for normalizing the actual values and for honoring "disabled" thresholds.</p>
     *
     * @param actualErrorPercent observed error percentage
     * @param actualAverageResponseTimeMs observed average response time in ms
     * @param actualP95ResponseTimeMs observed 95th percentile response time in ms
     * @return true if any configured threshold is breached; false otherwise
     */
    public boolean hasAnyBreach(double actualErrorPercent,
                                long actualAverageResponseTimeMs,
                                long actualP95ResponseTimeMs) {
        // Short-circuit: return true as soon as any single breach is detected.
        return isErrorPercentBreached(actualErrorPercent)
                || isAverageResponseBreached(actualAverageResponseTimeMs)
                || isP95ResponseBreached(actualP95ResponseTimeMs);
    }

    /**
     * Normalize a percentage input to a safe reporting value.
     *
     * <p>Rules:
     * <ul>
     *   <li>NaN or infinite values become 0.0</li>
     *   <li>Negative values become 0.0</li>
     *   <li>Valid non-negative finite values are returned unchanged</li>
     * </ul>
     * </p>
     *
     * @param value the percentage value to sanitize
     * @return a safe, non-negative, finite percentage (0.0 if invalid)
     */
    private double sanitizePercent(double value) {
        // Guard against invalid floating point values that could break reporting or comparisons.
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    /**
     * Normalize a long input to ensure non-negative values.
     *
     * @param value the long value to sanitize
     * @return the original value if >= 0, otherwise 0L
     */
    private long sanitizeLong(long value) {
        // Clip negative values to zero so that thresholds are never negative internally.
        return Math.max(value, 0L);
    }

    /**
     * Safely normalize an actual double measurement before comparison.
     *
     * <p>This mirrors sanitizePercent but is used for incoming actual values rather than configured thresholds.
     * It ensures comparisons treat invalid measurements as zero rather than throwing or propagating NaN/Infinity.</p>
     *
     * @param value actual measured double value
     * @return a safe, non-negative, finite double (0.0 if invalid)
     */
    private double safeDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            // Treat invalid actual measurements as zero (no breach) instead of propagating errors.
            return 0.0;
        }
        return value;
    }

    /**
     * Safely normalize an actual long measurement before comparison.
     *
     * @param value actual measured long value
     * @return the value if non-negative, otherwise 0L
     */
    private long safeLong(long value) {
        // Treat negative measurements as zero to avoid false-positive breaches from invalid data.
        return Math.max(value, 0L);
    }

    /**
     * Produce a compact string representation useful for logging and debugging.
     *
     * @return a string containing the configured thresholds
     */
    @Override
    public String toString() {
        return "PerformanceAssertionProfile{" +
                "maxErrorPercent=" + maxErrorPercent +
                ", maxAverageResponseTimeMs=" + maxAverageResponseTimeMs +
                ", maxP95ResponseTimeMs=" + maxP95ResponseTimeMs +
                '}';
    }
}
