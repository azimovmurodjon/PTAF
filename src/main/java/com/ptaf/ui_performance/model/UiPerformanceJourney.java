package com.ptaf.ui_performance.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Browser journey executed independently by each virtual user.
 *
 * <p>Journeys are assembled by the UI performance-only Cucumber glue. They cannot call normal UI
 * step definitions, normal hooks, or normal page lifecycle classes.</p>
 */
public final class UiPerformanceJourney {
    private final String name;
    private final String baseUrl;
    private final List<UiPerformanceStep> steps = new ArrayList<>();

    public UiPerformanceJourney(String name, String baseUrl) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("UI performance journey name cannot be blank.");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("UI performance base URL cannot be blank.");
        }
        this.name = name.trim();
        this.baseUrl = baseUrl.trim();
    }

    public String getName() {
        return name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void addStep(UiPerformanceStep step) {
        if (step == null) {
            throw new IllegalArgumentException("UI performance journey step cannot be null.");
        }
        steps.add(step);
    }

    public List<UiPerformanceStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    /** Ensures a configured run has a safe target and an executable journey. */
    public void validateForExecution() {
        if (steps.isEmpty()) {
            throw new IllegalStateException("UI performance journey contains no steps: " + name);
        }
        String normalized = baseUrl.toLowerCase();
        if (normalized.contains("replace-with") || normalized.contains("example.com") || normalized.contains("yourapp")) {
            throw new IllegalStateException("UI performance base URL still contains a sample placeholder. Configure an approved non-production target before execution.");
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            throw new IllegalArgumentException("UI performance base URL must begin with http:// or https://.");
        }
    }
}
