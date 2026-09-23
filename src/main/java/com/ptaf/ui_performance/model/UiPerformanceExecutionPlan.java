package com.ptaf.ui_performance.model;

import java.util.List;

/** A named load, stress, spike, or soak schedule selected entirely from UI performance YAML. */
public record UiPerformanceExecutionPlan(
        String profileName,
        UiPerformanceTestType testType,
        List<UiPerformanceStage> stages) {

    public UiPerformanceExecutionPlan {
        profileName = profileName == null ? "" : profileName.trim();
        stages = stages == null ? List.of() : List.copyOf(stages);
    }

    /** Validates every stage and the expected structural shape of the selected profile. */
    public void validate(int maxVirtualUsers) {
        if (profileName.isBlank()) {
            throw new IllegalArgumentException("UI performance active profile name cannot be blank.");
        }
        if (testType == null) {
            throw new IllegalArgumentException("UI performance test type cannot be null.");
        }
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("UI performance profile '" + profileName + "' contains no stages.");
        }
        for (UiPerformanceStage stage : stages) {
            stage.validate(maxVirtualUsers);
        }
        if ((testType == UiPerformanceTestType.STRESS || testType == UiPerformanceTestType.SPIKE) && stages.size() < 2) {
            throw new IllegalArgumentException(testType.displayName() + " UI performance profiles require at least two stages.");
        }
        if (testType == UiPerformanceTestType.SOAK && stages.stream().noneMatch(UiPerformanceStage::isDurationBased)) {
            throw new IllegalArgumentException("Soak UI performance profiles require at least one duration-based stage.");
        }
    }

    public int maximumVirtualUsers() {
        return stages.stream().mapToInt(UiPerformanceStage::virtualUsers).max().orElse(0);
    }

    public int totalPlannedRampSeconds() {
        return stages.stream().mapToInt(UiPerformanceStage::rampUpSeconds).sum();
    }

    public int totalPlannedHoldSeconds() {
        return stages.stream().mapToInt(UiPerformanceStage::holdSeconds).sum();
    }

    public int maximumIterationsPerUser() {
        return stages.stream().mapToInt(UiPerformanceStage::iterationsPerUser).max().orElse(0);
    }
}
