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
 *
 * <p>
 * This enum encapsulates both a human-friendly business label and a short
 * "summary meaning" that can be used directly in automated reports, dashboards,
 * and exported artifacts. Helper methods provide common boolean checks that
 * are useful for test assertions, reporting filters, and summary computations.
 * </p>
 */
public enum PerformanceExecutionStatus {

    /**
     * Scenario executed successfully and stayed within configured expectations.
     *
     * <p>Represents a normal, healthy outcome where all validations passed.</p>
     */
    PASS("Passed", "Healthy outcome"),

    /**
     * Scenario failed unexpectedly or exceeded configured validation thresholds.
     *
     * <p>Use this to indicate an actual test failure requiring investigation.</p>
     */
    FAIL("Failed", "Immediate review needed"),

    /**
     * Scenario was intentionally executed in expected-failure mode
     * and failure was correctly detected.
     *
     * <p>This is considered a pass-like outcome for workflows that explicitly
     * assert that an error/failure should occur.</p>
     */
    EXPECTED_FAIL_CONFIRMED("Expected Fail Confirmed", "Behavior confirmed"),

    /**
     * Scenario was intentionally executed in expected-failure mode
     * but the expected failure did not occur.
     *
     * <p>This is considered a failure-like outcome because the negative
     * validation did not trigger as expected.</p>
     */
    EXPECTED_FAIL_NOT_TRIGGERED("Expected Fail Not Triggered", "Negative validation issue"),

    /**
     * Scenario did not execute.
     *
     * <p>Used when tests are explicitly skipped or not applicable in the run.
     * This often requires attention when unexpected.</p>
     */
    SKIPPED("Skipped", "Not executed");

    /**
     * Business-facing label to display in reports and dashboards.
     *
     * <p>Intended for short, human-readable strings that are stable across
     * reporting formats (Excel, TXT, HTML).</p>
     */
    private final String businessLabel;

    /**
     * Short explanation of the meaning for high-level summaries.
     *
     * <p>This field is optimized for quick stakeholder reading and explanatory
     * summaries in exportable artifacts.</p>
     */
    private final String summaryMeaning;

    /**
     * Constructor for enum values.
     *
     * @param businessLabel   short, human-friendly label to show in reports
     * @param summaryMeaning  concise explanation used in summaries and tooltips
     */
    PerformanceExecutionStatus(String businessLabel, String summaryMeaning) {
        this.businessLabel = businessLabel;
        this.summaryMeaning = summaryMeaning;
    }

    /**
     * Returns the stable business label for this status.
     *
     * <p>Example: PASS -> "Passed"</p>
     *
     * @return human-readable business label
     */
    public String getBusinessLabel() {
        // simple accessor for external reporting and UI display
        return businessLabel;
    }

    /**
     * Returns a short explanation of what this status means in summaries.
     *
     * <p>Example: FAIL -> "Immediate review needed"</p>
     *
     * @return concise summary meaning string
     */
    public String getSummaryMeaning() {
        // simple accessor used for tooltips, CSV/TXT exports, or summary rows
        return summaryMeaning;
    }

    /**
     * Returns true when the status should be treated as a passing outcome
     * for high-level reports and stakeholder summaries.
     *
     * <p>Includes explicit PASS and EXPECTED_FAIL_CONFIRMED (negative test that
     * behaved as expected).</p>
     *
     * @return true if the status is considered pass-like; false otherwise
     */
    public boolean isPassLike() {
        // PASS is a direct success; EXPECTED_FAIL_CONFIRMED is a successful
        // outcome in negative/expected-failure flows.
        return this == PASS || this == EXPECTED_FAIL_CONFIRMED;
    }

    /**
     * Returns true when the status should be treated as a failing outcome
     * for high-level reports.
     *
     * <p>Includes explicit FAIL and EXPECTED_FAIL_NOT_TRIGGERED (expected
     * failure that did not occur).</p>
     *
     * @return true if the status is considered failure-like; false otherwise
     */
    public boolean isFailureLike() {
        // FAIL indicates an assertion/validation failure; EXPECTED_FAIL_NOT_TRIGGERED
        // indicates a negative test that unexpectedly passed.
        return this == FAIL || this == EXPECTED_FAIL_NOT_TRIGGERED;
    }

    /**
     * Returns true when the scenario was skipped (i.e., not executed).
     *
     * @return true for SKIPPED; false otherwise
     */
    public boolean isSkipped() {
        // direct check for skipped state
        return this == SKIPPED;
    }

    /**
     * Convenience method to indicate whether this status requires attention
     * from engineers, testers, or stakeholders.
     *
     * <p>Currently defined as any failure-like status or a skipped scenario.
     * This makes it easy to filter dashboards for outcomes that need follow-up.</p>
     *
     * @return true if the status needs investigation or follow-up; false otherwise
     */
    public boolean needsAttention() {
        // Combine failure-like and skipped into a single "needs attention" predicate
        return isFailureLike() || isSkipped();
    }

    /**
     * Returns true if this status is part of the expected-failure workflow.
     *
     * <p>Both EXPECTED_FAIL_CONFIRMED and EXPECTED_FAIL_NOT_TRIGGERED are
     * considered part of the expected-failure flow.</p>
     *
     * @return true for expected-failure flow statuses; false otherwise
     */
    public boolean isExpectedFailureFlow() {
        // Used to group statuses that originate from negative/expected-failure tests
        return this == EXPECTED_FAIL_CONFIRMED || this == EXPECTED_FAIL_NOT_TRIGGERED;
    }

    /**
     * Gets the business label for a potentially-null status.
     *
     * <p>Safe accessor useful in reporting code that may receive null values:
     * returns "Unknown" when status is null.</p>
     *
     * @param status the status to convert to a business label; may be null
     * @return the business label or "Unknown" if status is null
     */
    public static String toBusinessLabel(PerformanceExecutionStatus status) {
        // Null-safe conversion for external report builders
        return status == null ? "Unknown" : status.getBusinessLabel();
    }

    /**
     * Gets the summary meaning for a potentially-null status.
     *
     * <p>Safe accessor useful in reporting code that may receive null values:
     * returns "Unknown status" when status is null.</p>
     *
     * @param status the status to convert to a summary meaning; may be null
     * @return the summary meaning or "Unknown status" if status is null
     */
    public static String toSummaryMeaning(PerformanceExecutionStatus status) {
        // Null-safe conversion for summary exports or tooltips
        return status == null ? "Unknown status" : status.getSummaryMeaning();
    }
}
