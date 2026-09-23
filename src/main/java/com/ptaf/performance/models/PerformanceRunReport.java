package com.ptaf.performance.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Run-level performance report model.
 *
 * <p>This object represents one full performance execution run and contains
 * all scenario-level results that belong to the same run.</p>
 *
 * <p>Design goals:
 * - keep reporting calculations centralized
 * - remain backward-compatible for enterprise framework usage
 * - avoid touching execution logic
 * - provide safe aggregated metrics for Excel reporting</p>
 */
public class PerformanceRunReport {

    /**
     * Constant message used by thresholds reporting when there are no breaches.
     * Comparison is case-insensitive after trimming.
     */
    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";

    /**
     * Name of the folder where the run results are stored.
     * This is typically used to identify the run in file-system based reports.
     */
    private final String runFolderName;

    /**
     * Root path (parent folder) for the run. Useful for locating artifacts related to the run.
     */
    private final String runRootPath;

    /**
     * Timestamp string representing when the run was executed. Format is not enforced here;
     * it should be provided by the caller in a human/readable form.
     */
    private final String executionTimestamp;

    /**
     * Internal list that stores scenario-level execution results that belong to this run.
     * Kept private and only exposed as an unmodifiable list to prevent external mutation.
     */
    private final List<PerformanceExecutionResult> scenarioResults = new ArrayList<>();

    /**
     * Construct a run-level report container.
     *
     * @param runFolderName      folder name for this run (non-null recommended)
     * @param runRootPath        root path containing the run folder (non-null recommended)
     * @param executionTimestamp human-readable timestamp for the run execution
     */
    public PerformanceRunReport(String runFolderName,
                                String runRootPath,
                                String executionTimestamp) {
        this.runFolderName = runFolderName;
        this.runRootPath = runRootPath;
        this.executionTimestamp = executionTimestamp;
    }

    /**
     * @return configured run folder name for this report.
     */
    public String getRunFolderName() {
        return runFolderName;
    }

    /**
     * @return configured root path for this run.
     */
    public String getRunRootPath() {
        return runRootPath;
    }

    /**
     * @return execution timestamp associated with this run.
     */
    public String getExecutionTimestamp() {
        return executionTimestamp;
    }

    /**
     * Add a scenario-level execution result to this run report.
     *
     * <p>Note: The method validates the provided result to be non-null and will throw
     * IllegalArgumentException if null is passed. This protects downstream aggregate
     * calculations that assume presence of valid objects.</p>
     *
     * @param result PerformanceExecutionResult instance to add (must not be null)
     * @throws IllegalArgumentException if result is null
     */
    public void addScenarioResult(PerformanceExecutionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("PerformanceExecutionResult cannot be null.");
        }
        scenarioResults.add(result);
    }

    /**
     * Get an unmodifiable view of scenario results collected for this run.
     * Testers can iterate the returned list but cannot modify the internal state.
     *
     * @return unmodifiable list of PerformanceExecutionResult objects
     */
    public List<PerformanceExecutionResult> getScenarioResults() {
        return Collections.unmodifiableList(scenarioResults);
    }

    /**
     * @return total number of scenarios recorded for this run.
     */
    public int getTotalScenarios() {
        return scenarioResults.size();
    }

    /**
     * @return true if at least one scenario result has been recorded.
     */
    public boolean hasScenarioResults() {
        return !scenarioResults.isEmpty();
    }

    /**
     * Count scenarios with PASS status.
     *
     * @return number of passed scenarios
     */
    public long getPassedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.PASS)
                .count();
    }

    /**
     * Count scenarios with FAIL status.
     *
     * @return number of failed scenarios
     */
    public long getFailedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.FAIL)
                .count();
    }

    /**
     * Count scenarios that were expected to fail and indeed failed (confirmed expected failures).
     *
     * @return number of expected-failures that were confirmed
     */
    public long getExpectedFailConfirmedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_CONFIRMED)
                .count();
    }

    /**
     * Count scenarios that were marked as expected-fail but did not trigger the expected failure.
     *
     * @return number of expected-failures that were not triggered
     */
    public long getExpectedFailNotTriggeredScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED)
                .count();
    }

    /**
     * Count scenarios that were skipped during execution.
     *
     * @return number of skipped scenarios
     */
    public long getSkippedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.SKIPPED)
                .count();
    }

    /**
     * Compute pass rate as a percentage.
     *
     * <p>Returns 0.0 when there are no scenarios to avoid divide-by-zero. The result
     * is computed as: passed_count * 100.0 / total_count</p>
     *
     * @return pass rate percentage (0.0..100.0)
     */
    public double getPassRatePercent() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }
        return (getPassedScenarios() * 100.0) / scenarioResults.size();
    }

    /**
     * Compute fail rate as a percentage.
     *
     * <p>Returns 0.0 when there are no scenarios. The result is computed as:
     * failed_count * 100.0 / total_count</p>
     *
     * @return fail rate percentage (0.0..100.0)
     */
    public double getFailRatePercent() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }
        return (getFailedScenarios() * 100.0) / scenarioResults.size();
    }

    /**
     * Compute average of scenario-level error percentages.
     *
     * <p>Each scenario contributes its own errorPercent value. The method returns 0.0 if
     * there are no scenarios.</p>
     *
     * @return average error percentage across scenarios
     */
    public double getAverageErrorPercent() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }

        double total = scenarioResults.stream()
                .mapToDouble(PerformanceExecutionResult::getErrorPercent)
                .sum();

        return total / scenarioResults.size();
    }

    /**
     * Compute average risk score across scenarios.
     *
     * <p>Risk scores are expected to be integer values in the scenario results.
     * Returns 0.0 when there are no scenarios.</p>
     *
     * @return average risk score (as double)
     */
    public double getAverageRiskScore() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }

        double total = scenarioResults.stream()
                .mapToInt(PerformanceExecutionResult::getRiskScore)
                .sum();

        return total / scenarioResults.size();
    }

    /**
     * Total of all scenario durations in milliseconds.
     *
     * @return sum of getTotalScenarioDurationMs() for all scenarios
     */
    public long getTotalScenarioDurationMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalScenarioDurationMs)
                .sum();
    }

    /**
     * Average scenario duration in milliseconds, rounded to nearest long.
     *
     * @return rounded average duration or 0L when no scenarios exist
     */
    public long getAverageScenarioDurationMs() {
        if (scenarioResults.isEmpty()) {
            return 0L;
        }

        return Math.round((double) getTotalScenarioDurationMs() / scenarioResults.size());
    }

    /**
     * Sum of total request samples across all scenarios.
     *
     * @return total sample count
     */
    public long getTotalSamples() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalSamples)
                .sum();
    }

    /**
     * Sum of total errors across all scenarios.
     *
     * @return total error count
     */
    public long getTotalErrors() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalErrors)
                .sum();
    }

    /**
     * The slowest (maximum) 95th-percentile response time across all scenarios.
     *
     * @return max P95 response time in ms, or 0L if none present
     */
    public long getSlowestP95ResponseTimeMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getP95ResponseTimeMs)
                .max()
                .orElse(0L);
    }

    /**
     * The slowest (maximum) average response time across all scenarios.
     *
     * @return max average response time in ms, or 0L if none present
     */
    public long getSlowestAverageResponseTimeMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getAverageResponseTimeMs)
                .max()
                .orElse(0L);
    }

    /**
     * Highest risk score value observed across all scenarios.
     *
     * @return maximum risk score integer, or 0 if no scenarios present
     */
    public int getHighestRiskScore() {
        return scenarioResults.stream()
                .mapToInt(PerformanceExecutionResult::getRiskScore)
                .max()
                .orElse(0);
    }

    /**
     * Longest scenario duration observed in the run.
     *
     * @return maximum scenario duration in ms, or 0L if none present
     */
    public long getHighestScenarioDurationMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalScenarioDurationMs)
                .max()
                .orElse(0L);
    }

    /**
     * Shortest scenario duration observed in the run.
     *
     * @return minimum scenario duration in ms, or 0L if none present
     */
    public long getShortestScenarioDurationMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalScenarioDurationMs)
                .min()
                .orElse(0L);
    }

    /**
     * Highest total error count for a single scenario in the run.
     *
     * @return maximum totalErrors across scenarios, or 0L if none present
     */
    public long getHighestTotalErrors() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalErrors)
                .max()
                .orElse(0L);
    }

    /**
     * Highest error percentage observed across all scenarios.
     *
     * @return maximum error percent (double) or 0.0 if none present
     */
    public double getHighestErrorPercent() {
        return scenarioResults.stream()
                .mapToDouble(PerformanceExecutionResult::getErrorPercent)
                .max()
                .orElse(0.0);
    }

    /**
     * Count scenarios where configured thresholds were breached.
     *
     * @return number of scenarios with threshold breaches
     */
    public long getThresholdBreachScenarioCount() {
        return scenarioResults.stream()
                .filter(this::hasThresholdBreach)
                .count();
    }

    /**
     * Count scenarios that have any errors (either totalErrors > 0 or errorPercent > 0.0).
     *
     * @return number of error scenarios
     */
    public long getErrorScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> result.getTotalErrors() > 0 || result.getErrorPercent() > 0.0)
                .count();
    }

    /**
     * Count scenarios whose risk is considered High or Critical (by label or by numeric score).
     *
     * @return number of high-or-critical risk scenarios
     */
    public long getHighOrCriticalRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(this::isHighOrCriticalRisk)
                .count();
    }

    /**
     * Count scenarios explicitly marked with risk level "Critical" (case-insensitive).
     *
     * @return number of critical risk scenarios
     */
    public long getCriticalRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "Critical"))
                .count();
    }

    /**
     * Count scenarios explicitly marked with risk level "High" (case-insensitive).
     *
     * @return number of high risk scenarios
     */
    public long getHighRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "High"))
                .count();
    }

    /**
     * Count scenarios explicitly marked with risk level "Medium" (case-insensitive).
     *
     * @return number of medium risk scenarios
     */
    public long getMediumRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "Medium"))
                .count();
    }

    /**
     * Count scenarios explicitly marked with risk level "Low" (case-insensitive).
     *
     * @return number of low risk scenarios
     */
    public long getLowRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "Low"))
                .count();
    }

    /**
     * Count scenarios that do not require attention. This is the inverse of isAttentionNeeded(...)
     * and includes scenarios that passed and have no threshold breaches, no errors and acceptable risk levels.
     *
     * @return number of scenarios with no detected issues
     */
    public long getNoIssueScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> !isAttentionNeeded(result))
                .count();
    }

    /**
     * Retrieve the scenario which has the slowest P95 response time.
     *
     * @return PerformanceExecutionResult with the largest p95 value, or null if none present
     */
    public PerformanceExecutionResult getSlowestP95Scenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getP95ResponseTimeMs))
                .orElse(null);
    }

    /**
     * Retrieve the scenario which has the slowest average response time.
     *
     * @return PerformanceExecutionResult with the largest average response time, or null if none
     */
    public PerformanceExecutionResult getSlowestAverageResponseScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getAverageResponseTimeMs))
                .orElse(null);
    }

    /**
     * Retrieve the scenario with the highest error percent.
     *
     * @return scenario with maximum errorPercent, or null if none present
     */
    public PerformanceExecutionResult getHighestErrorScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingDouble(PerformanceExecutionResult::getErrorPercent))
                .orElse(null);
    }

    /**
     * Retrieve the scenario with the highest numeric risk score.
     *
     * @return scenario with maximum riskScore, or null if none present
     */
    public PerformanceExecutionResult getHighestRiskScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore))
                .orElse(null);
    }

    /**
     * Retrieve the scenario that ran the longest.
     *
     * @return scenario with maximum totalScenarioDurationMs, or null if none present
     */
    public PerformanceExecutionResult getLongestDurationScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getTotalScenarioDurationMs))
                .orElse(null);
    }

    /**
     * Retrieve the scenario that completed the fastest (shortest total duration).
     *
     * @return scenario with minimum totalScenarioDurationMs, or null if none present
     */
    public PerformanceExecutionResult getShortestDurationScenario() {
        return scenarioResults.stream()
                .min(Comparator.comparingLong(PerformanceExecutionResult::getTotalScenarioDurationMs))
                .orElse(null);
    }

    /**
     * Retrieve the scenario with the highest total errors.
     *
     * @return scenario with maximum totalErrors, or null if none present
     */
    public PerformanceExecutionResult getHighestTotalErrorsScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getTotalErrors))
                .orElse(null);
    }

    /**
     * Produce a human-friendly overall conclusion for the run based on collected metrics.
     *
     * <p>The logic follows a prioritized set of checks:
     * - If no scenarios recorded, state that explicitly.
     * - If no fails and no untriggered expected failures and no skipped, provide successful messages
     *   with additional checks for expected failures, risk, threshold breaches, and errors.
     * - If all failed, indicate all failed.
     * - If all skipped, indicate skipped.
     * - Otherwise inspect highest risk, threshold breaches, and errors to produce advisory messages.
     * - If none of the above, return a generic mixed-result advisory.</p>
     *
     * <p>This method is intended to provide a concise summary for quick human consumption in reports.</p>
     *
     * @return textual overall conclusion for the run
     */
    public String getOverallConclusion() {
        if (scenarioResults.isEmpty()) {
            return "No performance scenarios were recorded in this run.";
        }

        long passCount = getPassedScenarios();
        long failCount = getFailedScenarios();
        long expectedConfirmedCount = getExpectedFailConfirmedScenarios();
        long expectedNotTriggeredCount = getExpectedFailNotTriggeredScenarios();
        long skippedCount = getSkippedScenarios();
        int highestRiskScore = getHighestRiskScore();
        long thresholdBreaches = getThresholdBreachScenarioCount();
        long errorScenarios = getErrorScenarioCount();

        if (failCount == 0 && expectedNotTriggeredCount == 0 && skippedCount == 0) {
            if (expectedConfirmedCount > 0) {
                return "Run completed successfully. Positive scenarios passed, and expected-failure scenarios behaved as designed.";
            }
            if (highestRiskScore >= 81) {
                return "All scenarios completed, but one or more scenarios still show critical risk and should be reviewed.";
            }
            if (highestRiskScore >= 51) {
                return "All scenarios completed, but some scenarios still show elevated risk and should be reviewed.";
            }
            if (thresholdBreaches > 0 || errorScenarios > 0) {
                return "All scenarios completed, but threshold or error indicators were detected and should be reviewed.";
            }
            return "All performance scenarios passed in this run.";
        }

        if (failCount == scenarioResults.size()) {
            return "All performance scenarios failed in this run.";
        }

        if (passCount == 0 && failCount == 0 && skippedCount == scenarioResults.size()) {
            return "All performance scenarios were skipped in this run.";
        }

        if (highestRiskScore >= 81) {
            return "This run contains critical-risk scenarios and should be investigated before release.";
        }

        if (highestRiskScore >= 51) {
            return "This run contains high-risk scenarios that should be investigated before release.";
        }

        if (thresholdBreaches > 0) {
            return "This run contains threshold breaches that should be reviewed before release.";
        }

        if (errorScenarios > 0) {
            return "This run contains request errors that should be reviewed before release.";
        }

        return "This run contains a mix of passed, failed, expected-failure, or skipped scenarios. Review the scenario summary and charts for detailed risk areas.";
    }

    /**
     * Determine whether the given scenario result contains a threshold breach.
     *
     * <p>Checks threshold breach summary for a non-null, non-blank value and ensures
     * it does not match the standard NO_THRESHOLD_BREACHES message (case-insensitive).</p>
     *
     * @param result scenario result to inspect
     * @return true when a threshold breach is present, false otherwise
     */
    private boolean hasThresholdBreach(PerformanceExecutionResult result) {
        String summary = result.getThresholdBreachSummary();
        return summary != null
                && !summary.isBlank()
                && !NO_THRESHOLD_BREACHES.equalsIgnoreCase(summary.trim());
    }

    /**
     * Determine if a scenario should be considered high or critical risk.
     *
     * <p>A scenario is considered high-or-critical if:
     * - Its risk level label equals "High" or "Critical" (case-insensitive), OR
     * - Its numeric riskScore is >= 51 (threshold used as heuristic).</p>
     *
     * @param result scenario to evaluate
     * @return true when scenario is high or critical risk
     */
    private boolean isHighOrCriticalRisk(PerformanceExecutionResult result) {
        return equalsIgnoreCase(result.getRiskLevel(), "High")
                || equalsIgnoreCase(result.getRiskLevel(), "Critical")
                || result.getRiskScore() >= 51;
    }

    /**
     * Determines if a scenario needs attention (i.e., should be investigated).
     *
     * <p>Attention is needed when any of the following are true:
     * - The scenario failed
     * - It was expected to fail but did not trigger the expected failure
     * - A threshold breach is present
     * - The scenario recorded any errors
     * - The scenario qualifies as high-or-critical risk</p>
     *
     * @param result scenario to inspect
     * @return true if the scenario requires attention, false otherwise
     */
    private boolean isAttentionNeeded(PerformanceExecutionResult result) {
        return result.getExecutionStatus() == PerformanceExecutionStatus.FAIL
                || result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED
                || hasThresholdBreach(result)
                || result.getTotalErrors() > 0
                || isHighOrCriticalRisk(result);
    }

    /**
     * Safe case-insensitive comparison between a potentially-null string value and a target.
     *
     * @param value  input string (may be null)
     * @param target target to compare against (non-null expected)
     * @return true when value is non-null and equals target ignoring case
     */
    private boolean equalsIgnoreCase(String value, String target) {
        return value != null && value.equalsIgnoreCase(target);
    }

    /**
     * Debug-friendly summary of the run report object and key aggregated metrics.
     *
     * @return single-line string representing the report summary
     */
    @Override
    public String toString() {
        return "PerformanceRunReport{" +
                "runFolderName='" + runFolderName + '\'' +
                ", runRootPath='" + runRootPath + '\'' +
                ", executionTimestamp='" + executionTimestamp + '\'' +
                ", totalScenarios=" + getTotalScenarios() +
                ", passedScenarios=" + getPassedScenarios() +
                ", failedScenarios=" + getFailedScenarios() +
                ", expectedFailConfirmedScenarios=" + getExpectedFailConfirmedScenarios() +
                ", expectedFailNotTriggeredScenarios=" + getExpectedFailNotTriggeredScenarios() +
                ", skippedScenarios=" + getSkippedScenarios() +
                ", averageRiskScore=" + getAverageRiskScore() +
                ", highestRiskScore=" + getHighestRiskScore() +
                ", totalScenarioDurationMs=" + getTotalScenarioDurationMs() +
                ", shortestScenarioDurationMs=" + getShortestScenarioDurationMs() +
                ", totalSamples=" + getTotalSamples() +
                ", totalErrors=" + getTotalErrors() +
                '}';
    }
}
