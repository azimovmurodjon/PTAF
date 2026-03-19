package com.ptaf.performance.models;

/**
 * Defines the execution load profile for a performance run.
 * This object is architect-controlled and reused by all performance scenarios.
 *
 * <p>Reporting-safe goals:
 * <ul>
 *   <li>keep constructor compatibility</li>
 *   <li>normalize invalid values</li>
 *   <li>provide small helper methods for report interpretation</li>
 * </ul>
 * </p>
 */
public class PerformanceProfile {

    private final int users;
    private final int rampUpSeconds;
    private final int holdSeconds;
    private final int iterations;

    public PerformanceProfile(int users, int rampUpSeconds, int holdSeconds, int iterations) {
        this.users = sanitizeInt(users);
        this.rampUpSeconds = sanitizeInt(rampUpSeconds);
        this.holdSeconds = sanitizeInt(holdSeconds);
        this.iterations = sanitizeInt(iterations);
    }

    public int getUsers() {
        return users;
    }

    public int getRampUpSeconds() {
        return rampUpSeconds;
    }

    public int getHoldSeconds() {
        return holdSeconds;
    }

    public int getIterations() {
        return iterations;
    }

    public boolean isIterationBasedExecution() {
        return iterations > 0;
    }

    public boolean isDurationBasedExecution() {
        return iterations == 0;
    }

    public int getTotalPlannedDurationSeconds() {
        return rampUpSeconds + holdSeconds;
    }

    public boolean isSmokeLikeProfile() {
        return users <= 2;
    }

    public boolean isLoadLikeProfile() {
        return users > 2 && users <= 20;
    }

    public boolean isHighLoadLikeProfile() {
        return users > 20;
    }

    private int sanitizeInt(int value) {
        return Math.max(value, 0);
    }

    @Override
    public String toString() {
        return "PerformanceProfile{" +
                "users=" + users +
                ", rampUpSeconds=" + rampUpSeconds +
                ", holdSeconds=" + holdSeconds +
                ", iterations=" + iterations +
                '}';
    }
}