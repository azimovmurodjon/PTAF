package com.ptaf.performance.models;

/**
 * Defines SLA expectations for a performance run.
 */
public class PerformanceAssertionProfile {

    private final double maxErrorPercent;
    private final long maxAverageResponseTimeMs;
    private final long maxP95ResponseTimeMs;

    public PerformanceAssertionProfile(double maxErrorPercent,
                                       long maxAverageResponseTimeMs,
                                       long maxP95ResponseTimeMs) {
        this.maxErrorPercent = maxErrorPercent;
        this.maxAverageResponseTimeMs = maxAverageResponseTimeMs;
        this.maxP95ResponseTimeMs = maxP95ResponseTimeMs;
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
}