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
 */
public class PerformanceAssertionProfile {

    private final double maxErrorPercent;
    private final long maxAverageResponseTimeMs;
    private final long maxP95ResponseTimeMs;

    public PerformanceAssertionProfile(double maxErrorPercent,
                                       long maxAverageResponseTimeMs,
                                       long maxP95ResponseTimeMs) {
        this.maxErrorPercent = sanitizePercent(maxErrorPercent);
        this.maxAverageResponseTimeMs = sanitizeLong(maxAverageResponseTimeMs);
        this.maxP95ResponseTimeMs = sanitizeLong(maxP95ResponseTimeMs);
    }

    public double getMaxErrorPercent() {
        return maxErrorPercent;
    }

    public long getMaxAverageResponseTimeMs() {
        return maxAverageResponseTimeMs;
    }

    public long getMaxP95ResponseTimeMs() {
        return maxP95ResponseTimeMs;
    }

    public boolean hasConfiguredThresholds() {
        return maxErrorPercent > 0.0
                || maxAverageResponseTimeMs > 0L
                || maxP95ResponseTimeMs > 0L;
    }

    public boolean isErrorPercentBreached(double actualErrorPercent) {
        return maxErrorPercent > 0.0 && safeDouble(actualErrorPercent) > maxErrorPercent;
    }

    public boolean isAverageResponseBreached(long actualAverageResponseTimeMs) {
        return maxAverageResponseTimeMs > 0L && safeLong(actualAverageResponseTimeMs) > maxAverageResponseTimeMs;
    }

    public boolean isP95ResponseBreached(long actualP95ResponseTimeMs) {
        return maxP95ResponseTimeMs > 0L && safeLong(actualP95ResponseTimeMs) > maxP95ResponseTimeMs;
    }

    public boolean hasAnyBreach(double actualErrorPercent,
                                long actualAverageResponseTimeMs,
                                long actualP95ResponseTimeMs) {
        return isErrorPercentBreached(actualErrorPercent)
                || isAverageResponseBreached(actualAverageResponseTimeMs)
                || isP95ResponseBreached(actualP95ResponseTimeMs);
    }

    private double sanitizePercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0) {
            return 0.0;
        }
        return value;
    }

    private long sanitizeLong(long value) {
        return Math.max(value, 0L);
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

    @Override
    public String toString() {
        return "PerformanceAssertionProfile{" +
                "maxErrorPercent=" + maxErrorPercent +
                ", maxAverageResponseTimeMs=" + maxAverageResponseTimeMs +
                ", maxP95ResponseTimeMs=" + maxP95ResponseTimeMs +
                '}';
    }
}