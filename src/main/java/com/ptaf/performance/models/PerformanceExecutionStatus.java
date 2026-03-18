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
 */
public enum PerformanceExecutionStatus {

    /**
     * Scenario executed successfully and stayed within configured expectations.
     */
    PASS,

    /**
     * Scenario failed unexpectedly or exceeded configured validation thresholds.
     */
    FAIL,

    /**
     * Scenario was intentionally executed in expected-failure mode
     * and failure was correctly detected.
     */
    EXPECTED_FAIL_CONFIRMED,

    /**
     * Scenario was intentionally executed in expected-failure mode
     * but the expected failure did not occur.
     */
    EXPECTED_FAIL_NOT_TRIGGERED,

    /**
     * Scenario did not execute.
     */
    SKIPPED
}