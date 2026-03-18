package com.ptaf.performance.builders;

import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.models.PerformanceProfile;

/**
 * Architect-controlled builder for creating performance execution profiles.
 *
 * <p>This builder supports two execution modes:
 * <ul>
 *   <li>Iteration mode: iterations > 0</li>
 *   <li>Duration mode: iterations == 0 and holdSeconds > 0</li>
 * </ul>
 * </p>
 *
 * <p>Testers should not manually define low-level execution logic.
 * This builder is intended to be used through framework services and step definitions.</p>
 */
public class PerformanceProfileBuilder {

    private int users;
    private int rampUpSeconds;
    private int holdSeconds;
    private int iterations;

    /**
     * Initializes the builder using framework default configuration values.
     */
    public PerformanceProfileBuilder() {
        PerformanceProfile defaultProfile = PerformanceConfigurationProperties.getDefaultProfile();
        this.users = defaultProfile.getUsers();
        this.rampUpSeconds = defaultProfile.getRampUpSeconds();
        this.holdSeconds = defaultProfile.getHoldSeconds();
        this.iterations = defaultProfile.getIterations();
    }

    public PerformanceProfileBuilder withUsers(int users) {
        this.users = users;
        return this;
    }

    public PerformanceProfileBuilder withRampUpSeconds(int rampUpSeconds) {
        this.rampUpSeconds = rampUpSeconds;
        return this;
    }

    public PerformanceProfileBuilder withHoldSeconds(int holdSeconds) {
        this.holdSeconds = holdSeconds;
        return this;
    }

    public PerformanceProfileBuilder withIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    /**
     * Creates a validated performance profile.
     *
     * @return validated profile
     */
    public PerformanceProfile build() {
        validate();
        return new PerformanceProfile(users, rampUpSeconds, holdSeconds, iterations);
    }

    /**
     * Validates profile values according to supported execution modes.
     */
    private void validate() {
        if (users <= 0) {
            throw new IllegalArgumentException("Performance profile validation failed: users must be greater than 0.");
        }

        if (rampUpSeconds < 0) {
            throw new IllegalArgumentException("Performance profile validation failed: rampUpSeconds cannot be negative.");
        }

        if (holdSeconds < 0) {
            throw new IllegalArgumentException("Performance profile validation failed: holdSeconds cannot be negative.");
        }

        if (iterations < 0) {
            throw new IllegalArgumentException("Performance profile validation failed: iterations cannot be negative.");
        }

        boolean iterationMode = iterations > 0;
        boolean durationMode = iterations == 0 && holdSeconds > 0;

        if (!iterationMode && !durationMode) {
            throw new IllegalArgumentException(
                    "Performance profile validation failed: invalid execution mode. " +
                            "Use either iterations > 0 for iteration mode, or iterations == 0 with holdSeconds > 0 for duration mode."
            );
        }
    }

    public static PerformanceProfileBuilder fromDefaults() {
        return new PerformanceProfileBuilder();
    }

    public static PerformanceProfileBuilder fromProfile(PerformanceProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Performance profile cannot be null.");
        }

        return new PerformanceProfileBuilder()
                .withUsers(profile.getUsers())
                .withRampUpSeconds(profile.getRampUpSeconds())
                .withHoldSeconds(profile.getHoldSeconds())
                .withIterations(profile.getIterations());
    }
}