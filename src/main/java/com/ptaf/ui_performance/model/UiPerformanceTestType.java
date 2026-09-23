package com.ptaf.ui_performance.model;

import java.util.Locale;

/** Supported real-browser UI performance workload shapes. */
public enum UiPerformanceTestType {
    LOAD("Load"),
    STRESS("Stress"),
    SPIKE("Spike"),
    SOAK("Soak");

    private final String displayName;

    UiPerformanceTestType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Parses a profile type without accepting ambiguous or unsupported values. */
    public static UiPerformanceTestType fromConfig(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UI performance profile type cannot be blank.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "UI performance profile type must be one of: load, stress, spike, soak. Received: " + value,
                    exception);
        }
    }
}
