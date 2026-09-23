package com.ptaf.ui_performance.model;

/**
 * One sequential stage in a real-browser UI performance profile.
 *
 * <p>Within a stage, every requested user owns one Playwright instance and browser process. A zero
 * ramp starts all prepared users together. A positive ramp distributes first actions across the
 * ramp interval. A positive iteration count makes the stage iteration-based; zero iterations makes
 * it duration-based and requires a positive hold duration.</p>
 */
public record UiPerformanceStage(
        String name,
        int virtualUsers,
        int rampUpSeconds,
        int holdSeconds,
        int iterationsPerUser) {

    public UiPerformanceStage {
        name = name == null ? "" : name.trim();
    }

    /** Validates this stage against the tester-controlled safety maximum. */
    public void validate(int maxVirtualUsers) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Every UI performance stage requires a non-blank name.");
        }
        if (virtualUsers < 1) {
            throw new IllegalArgumentException("UI performance stage '" + name + "' must request at least one user.");
        }
        if (virtualUsers > maxVirtualUsers) {
            throw new IllegalArgumentException(
                    "UI performance stage '" + name + "' requests " + virtualUsers
                            + " users, exceeding ui_performance.safety.max_virtual_users=" + maxVirtualUsers + ".");
        }
        if (rampUpSeconds < 0 || holdSeconds < 0 || iterationsPerUser < 0) {
            throw new IllegalArgumentException("UI performance stage timing and iteration values cannot be negative: " + name);
        }
        if (iterationsPerUser == 0 && holdSeconds < 1) {
            throw new IllegalArgumentException(
                    "Duration-based UI performance stage '" + name + "' requires hold_seconds >= 1 when iterations_per_user is 0.");
        }
    }

    public boolean isDurationBased() {
        return iterationsPerUser == 0;
    }

    public boolean isIterationBased() {
        return iterationsPerUser > 0;
    }

    public int plannedDurationSeconds() {
        return rampUpSeconds + holdSeconds;
    }
}
