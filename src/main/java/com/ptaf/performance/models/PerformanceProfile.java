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
 *
 * <p>
 * A PerformanceProfile is an immutable value object that captures the intended
 * load shape for a performance test run:
 * <ul>
 *   <li>users - number of concurrent virtual users</li>
 *   <li>rampUpSeconds - seconds to ramp from 0 to 'users'</li>
 *   <li>holdSeconds - seconds to hold the target load (after ramp-up)</li>
 *   <li>iterations - if >0 the run will be iteration-based; if 0 the run is duration-based</li>
 * </ul>
 * </p>
 *
 * <p>
 * The class also provides convenience predicates used by reporting and test orchestration:
 * <ul>
 *   <li>isSmokeLikeProfile - very small number of users (<=2)</li>
 *   <li>isLoadLikeProfile - moderate load (3-20 users)</li>
 *   <li>isHighLoadLikeProfile - high load (>20 users)</li>
 * </ul>
 * These thresholds are intentionally simple and intended for coarse-grained classification
 * in dashboards and test selectors.
 * </p>
 */
public class PerformanceProfile {

    /**
     * Target number of concurrent virtual users for this profile.
     * Guaranteed to be non-negative (sanitized by constructor).
     */
    private final int users;

    /**
     * Duration in seconds to ramp from 0 users to {@link #users}.
     * Guaranteed to be non-negative (sanitized by constructor).
     */
    private final int rampUpSeconds;

    /**
     * Duration in seconds to hold the target number of users after ramp-up.
     * Guaranteed to be non-negative (sanitized by constructor).
     *
     * Note: For iteration-based runs this value may be ignored by execution engines;
     * however keeping it explicit helps reporting and comparisons between profiles.
     */
    private final int holdSeconds;

    /**
     * Number of iterations per user to execute. A value of zero means the profile
     * is duration-based (use {@link #isDurationBasedExecution()}), whereas a positive
     * value indicates iteration-based execution.
     *
     * Guaranteed to be non-negative (sanitized by constructor).
     */
    private final int iterations;

    /**
     * Create a new PerformanceProfile instance.
     *
     * <p>All integer inputs are normalized to be non-negative. Negative inputs are
     * converted to zero to avoid propagation of invalid values into execution logic
     * and reports.</p>
     *
     * @param users         number of concurrent virtual users (negative values are normalized to zero)
     * @param rampUpSeconds ramp-up time in seconds (negative values are normalized to zero)
     * @param holdSeconds   hold time in seconds after ramp-up (negative values are normalized to zero)
     * @param iterations    number of iterations per user; zero means duration-based execution (negative values are normalized to zero)
     */
    public PerformanceProfile(int users, int rampUpSeconds, int holdSeconds, int iterations) {
        // sanitize values to ensure the object always holds non-negative integers
        this.users = sanitizeInt(users);
        this.rampUpSeconds = sanitizeInt(rampUpSeconds);
        this.holdSeconds = sanitizeInt(holdSeconds);
        this.iterations = sanitizeInt(iterations);
    }

    /**
     * Get the configured number of users for this profile.
     *
     * @return non-negative number of virtual users
     */
    public int getUsers() {
        return users;
    }

    /**
     * Get the configured ramp-up duration in seconds.
     *
     * @return non-negative ramp-up duration in seconds
     */
    public int getRampUpSeconds() {
        return rampUpSeconds;
    }

    /**
     * Get the configured hold duration in seconds (time to keep the target load after ramp-up).
     *
     * @return non-negative hold duration in seconds
     */
    public int getHoldSeconds() {
        return holdSeconds;
    }

    /**
     * Get the configured number of iterations per user.
     *
     * @return non-negative iterations count; zero indicates duration-based execution
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Determine if this profile represents an iteration-based execution.
     *
     * <p>By convention, iteration-based execution is represented by a positive
     * {@link #iterations} value. This is commonly used when each user performs a
     * fixed number of transactions regardless of time.</p>
     *
     * @return true if iterations &gt; 0, false otherwise
     */
    public boolean isIterationBasedExecution() {
        return iterations > 0;
    }

    /**
     * Determine if this profile represents a duration-based execution.
     *
     * <p>By convention, a duration-based execution uses {@link #holdSeconds} (and
     * possibly {@link #rampUpSeconds}) to control the run length. A zero {@link #iterations}
     * value indicates duration-based execution.</p>
     *
     * @return true if iterations == 0, false otherwise
     */
    public boolean isDurationBasedExecution() {
        return iterations == 0;
    }

    /**
     * Get the total planned duration in seconds for the non-iteration execution flow.
     *
     * <p>This is a convenience that sums ramp-up and hold durations. For duration-based
     * runs this is the expected wall-clock time from start to finish (excluding any
     * setup/teardown or measurement windows).</p>
     *
     * @return sum of rampUpSeconds and holdSeconds (both non-negative)
     */
    public int getTotalPlannedDurationSeconds() {
        return rampUpSeconds + holdSeconds;
    }

    /**
     * Heuristic classifier that marks very small profiles as "smoke-like".
     *
     * <p>Useful for quick sanity checks or CI gating where only a tiny number of
     * virtual users are required. The current threshold is users <= 2.</p>
     *
     * @return true if users <= 2
     */
    public boolean isSmokeLikeProfile() {
        return users <= 2;
    }

    /**
     * Heuristic classifier for moderate load profiles.
     *
     * <p>Profiles with 3 to 20 users are considered "load-like" by the simple
     * thresholds used here. This is intended for classification and not as a
     * strict definition of load testing.</p>
     *
     * @return true if users is between 3 and 20 inclusive
     */
    public boolean isLoadLikeProfile() {
        return users > 2 && users <= 20;
    }

    /**
     * Heuristic classifier for high-load profiles.
     *
     * <p>Profiles with more than 20 users are treated as high load for the purposes
     * of selection, reporting, or routing tests to larger environments.</p>
     *
     * @return true if users &gt; 20
     */
    public boolean isHighLoadLikeProfile() {
        return users > 20;
    }

    /**
     * Normalize integer inputs so that the profile never contains negative values.
     *
     * <p>This method is intentionally simple: negative values are clamped to zero.
     * Keeping inputs non-negative reduces the need for defensive checks elsewhere
     * in the codebase and makes reports more predictable.</p>
     *
     * @param value input integer that may be negative
     * @return the original value if non-negative; otherwise zero
     */
    private int sanitizeInt(int value) {
        return Math.max(value, 0);
    }

    /**
     * Render a concise string representation of the profile used for logging and diagnostics.
     *
     * @return human-readable description containing all configured fields
     */
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
