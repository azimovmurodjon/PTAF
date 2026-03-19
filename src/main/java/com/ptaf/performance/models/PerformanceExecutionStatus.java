package com.ptaf.performance.models;

/**
 * High-level execution status used for reporting, Excel dashboards,
 * summaries, and leadership-readable result interpretation.
 *
 * <p>These values are intentionally simple and business-readable so they can be
 * used consistently across:
 * <ul>
 *   <li>scenario summaries</li>
 *   <li>run-level reports</li>
 *   <li>Excel charts</li>
 *   <li>TXT summaries</li>
 *   <li>readable stakeholder reports</li>
 * </ul>
 * </p>
 *
 * <p>Reporting-safe goals:
 * <ul>
 *   <li>keep enum names stable for backward compatibility</li>
 *   <li>add helper methods only</li>
 *   <li>support cleaner Excel/reporting decisions</li>
 * </ul>
 * </p>
 */
public enum PerformanceExecutionStatus {

    /**
     * Scenario executed successfully and stayed within configured expectations.
     */
    PASS("Passed", "Healthy outcome"),

    /**
     * Scenario failed unexpectedly or exceeded configured validation thresholds.
     */
    FAIL("Failed", "Immediate review needed"),

    /**
     * Scenario was intentionally executed in expected-failure mode
     * and failure was correctly detected.
     */
    EXPECTED_FAIL_CONFIRMED("Expected Fail Confirmed", "Behavior confirmed"),

    /**
     * Scenario was intentionally executed in expected-failure mode
     * but the expected failure did not occur.
     */
    EXPECTED_FAIL_NOT_TRIGGERED("Expected Fail Not Triggered", "Negative validation issue"),

    /**
     * Scenario did not execute.
     */
    SKIPPED("Skipped", "Not executed");

    private final String businessLabel;
    private final String summaryMeaning;

    PerformanceExecutionStatus(String businessLabel, String summaryMeaning) {
        this.businessLabel = businessLabel;
        this.summaryMeaning = summaryMeaning;
    }

    public String getBusinessLabel() {
        return businessLabel;
    }

    public String getSummaryMeaning() {
        return summaryMeaning;
    }

    public boolean isPassLike() {
        return this == PASS || this == EXPECTED_FAIL_CONFIRMED;
    }

    public boolean isFailureLike() {
        return this == FAIL || this == EXPECTED_FAIL_NOT_TRIGGERED;
    }

    public boolean isSkipped() {
        return this == SKIPPED;
    }

    public boolean needsAttention() {
        return isFailureLike() || isSkipped();
    }

    public boolean isExpectedFailureFlow() {
        return this == EXPECTED_FAIL_CONFIRMED || this == EXPECTED_FAIL_NOT_TRIGGERED;
    }

    public static String toBusinessLabel(PerformanceExecutionStatus status) {
        return status == null ? "Unknown" : status.getBusinessLabel();
    }

    public static String toSummaryMeaning(PerformanceExecutionStatus status) {
        return status == null ? "Unknown status" : status.getSummaryMeaning();
    }
}