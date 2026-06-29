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
 *
 * <p>Usage notes:
 * <ul>
 *   <li>Values are initialized from framework defaults obtained via PerformanceConfigurationProperties.getDefaultProfile().</li>
 *   <li>Fluent 'withX' methods allow overriding defaults; validation is performed when build() is called.</li>
 *   <li>build() returns an immutable PerformanceProfile containing the configured values.</li>
 * </ul>
 * </p>
 */
public class PerformanceProfileBuilder {

    /**
     * Number of virtual users (concurrent actors) to simulate.
     * <p>
     * Must be > 0. Initialized from framework default profile in the no-arg constructor.
     * </p>
     */
    private int users;

    /**
     * Time in seconds over which users are ramped up to the target 'users' value.
     * <p>
     * Must be >= 0. A value of 0 indicates immediate start with all users active.
     * Initialized from framework default profile in the no-arg constructor.
     * </p>
     */
    private int rampUpSeconds;

    /**
     * Duration in seconds to hold the load when using duration-based execution mode.
     * <p>
     * Only used when iterations == 0 and holdSeconds > 0 (duration mode).
     * Must be >= 0. Initialized from framework default profile in the no-arg constructor.
     * </p>
     */
    private int holdSeconds;

    /**
     * Number of iterations each virtual user should perform in iteration-based execution mode.
     * <p>
     * When iterations > 0 the builder operates in iteration mode.
     * When iterations == 0 and holdSeconds > 0 the builder operates in duration mode.
     * Must be >= 0. Initialized from framework default profile in the no-arg constructor.
     * </p>
     */
    private int iterations;

    /**
     * Initializes the builder using framework default configuration values.
     *
     * <p>The default profile is retrieved from PerformanceConfigurationProperties.getDefaultProfile()
     * and its values are copied into this builder instance so callers can override selectively
     * using the fluent withX methods.</p>
     */
    public PerformanceProfileBuilder() {
        PerformanceProfile defaultProfile = PerformanceConfigurationProperties.getDefaultProfile();
        // Copy default values into builder fields so they can be overridden via fluent API.
        this.users = defaultProfile.getUsers();
        this.rampUpSeconds = defaultProfile.getRampUpSeconds();
        this.holdSeconds = defaultProfile.getHoldSeconds();
        this.iterations = defaultProfile.getIterations();
    }

    /**
     * Sets the number of virtual users to simulate.
     *
     * <p>Note: This setter does not perform validation immediately. Validation occurs when build()
     * is called to allow composing multiple changes in a fluent manner.</p>
     *
     * @param users number of concurrent users to simulate; expected to be > 0
     * @return the same builder instance for fluent chaining
     */
    public PerformanceProfileBuilder withUsers(int users) {
        // Assign requested users count; validation deferred until build().
        this.users = users;
        return this;
    }

    /**
     * Sets the ramp-up time in seconds.
     *
     * <p>Zero indicates that all users become active immediately. Negative values are invalid and
     * will be rejected during build() validation.</p>
     *
     * @param rampUpSeconds seconds to ramp up users; expected to be >= 0
     * @return the same builder instance for fluent chaining
     */
    public PerformanceProfileBuilder withRampUpSeconds(int rampUpSeconds) {
        // Assign requested ramp up duration; validation deferred until build().
        this.rampUpSeconds = rampUpSeconds;
        return this;
    }

    /**
     * Sets the hold (steady-state) time in seconds for duration mode.
     *
     * <p>This value is only meaningful when iterations == 0 (duration mode).
     * Negative values are invalid and will be rejected during build() validation.</p>
     *
     * @param holdSeconds seconds to hold the load in duration mode; expected to be >= 0
     * @return the same builder instance for fluent chaining
     */
    public PerformanceProfileBuilder withHoldSeconds(int holdSeconds) {
        // Assign requested hold duration; validation deferred until build().
        this.holdSeconds = holdSeconds;
        return this;
    }

    /**
     * Sets the number of iterations per user for iteration mode.
     *
     * <p>When iterations > 0 the builder will produce an iteration-based profile.
     * A value of 0 indicates that iteration mode is not active; use holdSeconds > 0
     * instead to enable duration mode. Negative values are invalid and will be rejected
     * during build() validation.</p>
     *
     * @param iterations number of iterations per user; expected to be >= 0
     * @return the same builder instance for fluent chaining
     */
    public PerformanceProfileBuilder withIterations(int iterations) {
        // Assign requested iterations count; validation deferred until build().
        this.iterations = iterations;
        return this;
    }

    /**
     * Creates a validated PerformanceProfile instance from the configured values.
     *
     * <p>This method performs validation to ensure that the profile is coherent and
     * matches one of the supported execution modes (iteration or duration).</p>
     *
     * @return a new PerformanceProfile populated with the configured values
     * @throws IllegalArgumentException if validation fails (e.g. invalid values or unsupported mode)
     */
    public PerformanceProfile build() {
        // Ensure configured values are valid before creating immutable profile object.
        validate();
        return new PerformanceProfile(users, rampUpSeconds, holdSeconds, iterations);
    }

    /**
     * Validates profile values according to supported execution modes.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>users must be > 0</li>
     *   <li>rampUpSeconds, holdSeconds, iterations must be >= 0</li>
     *   <li>Execution mode must be either:
     *     <ul>
     *       <li>Iteration mode: iterations > 0 (holdSeconds may be 0 or any non-negative value)</li>
     *       <li>Duration mode: iterations == 0 and holdSeconds > 0</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <p>Throws descriptive IllegalArgumentException messages to aid troubleshooting in tests
     * and step definitions.</p>
     */
    private void validate() {
        // Users must be a positive count of concurrent actors.
        if (users <= 0) {
            throw new IllegalArgumentException("Performance profile validation failed: users must be greater than 0.");
        }

        // Ramp-up cannot be negative; zero is allowed (start all users immediately).
        if (rampUpSeconds < 0) {
            throw new IllegalArgumentException("Performance profile validation failed: rampUpSeconds cannot be negative.");
        }

        // Hold seconds cannot be negative; zero is allowed when using iteration mode.
        if (holdSeconds < 0) {
            throw new IllegalArgumentException("Performance profile validation failed: holdSeconds cannot be negative.");
        }

        // Iteration count cannot be negative.
        if (iterations < 0) {
            throw new IllegalArgumentException("Performance profile validation failed: iterations cannot be negative.");
        }

        // Determine which execution mode (if any) the configuration specifies.
        boolean iterationMode = iterations > 0;
        boolean durationMode = iterations == 0 && holdSeconds > 0;

        // If neither mode is satisfied, the profile is invalid.
        if (!iterationMode && !durationMode) {
            throw new IllegalArgumentException(
                    "Performance profile validation failed: invalid execution mode. " +
                            "Use either iterations > 0 for iteration mode, or iterations == 0 with holdSeconds > 0 for duration mode."
            );
        }
    }

    /**
     * Convenience factory returning a new builder initialized from framework defaults.
     *
     * @return a new PerformanceProfileBuilder populated with default profile values
     */
    public static PerformanceProfileBuilder fromDefaults() {
        // Delegate to the no-arg constructor which applies framework defaults.
        return new PerformanceProfileBuilder();
    }

    /**
     * Creates a builder pre-populated from an existing PerformanceProfile instance.
     *
     * <p>This is useful for tests or step definitions that want to start from a known profile
     * and make incremental adjustments via the fluent API.</p>
     *
     * @param profile existing profile to copy values from; must not be null
     * @return a new builder initialized with the provided profile's values
     * @throws IllegalArgumentException if profile is null
     */
    public static PerformanceProfileBuilder fromProfile(PerformanceProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Performance profile cannot be null.");
        }

        // Create a new builder and copy all values from the supplied profile to allow further adjustments.
        return new PerformanceProfileBuilder()
                .withUsers(profile.getUsers())
                .withRampUpSeconds(profile.getRampUpSeconds())
                .withHoldSeconds(profile.getHoldSeconds())
                .withIterations(profile.getIterations());
    }
}
