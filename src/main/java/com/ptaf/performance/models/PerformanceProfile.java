package com.ptaf.performance.models;

/**
 * Defines the execution load profile for a performance run.
 * This object is architect-controlled and reused by all performance scenarios.
 */
public class PerformanceProfile {

    private final int users;
    private final int rampUpSeconds;
    private final int holdSeconds;
    private final int iterations;

    public PerformanceProfile(int users, int rampUpSeconds, int holdSeconds, int iterations) {
        this.users = users;
        this.rampUpSeconds = rampUpSeconds;
        this.holdSeconds = holdSeconds;
        this.iterations = iterations;
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