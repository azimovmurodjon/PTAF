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

    private static final String NO_THRESHOLD_BREACHES = "No configured threshold breaches detected.";

    private final String runFolderName;
    private final String runRootPath;
    private final String executionTimestamp;
    private final List<PerformanceExecutionResult> scenarioResults = new ArrayList<>();

    public PerformanceRunReport(String runFolderName,
                                String runRootPath,
                                String executionTimestamp) {
        this.runFolderName = runFolderName;
        this.runRootPath = runRootPath;
        this.executionTimestamp = executionTimestamp;
    }

    public String getRunFolderName() {
        return runFolderName;
    }

    public String getRunRootPath() {
        return runRootPath;
    }

    public String getExecutionTimestamp() {
        return executionTimestamp;
    }

    public void addScenarioResult(PerformanceExecutionResult result) {
        if (result == null) {
            throw new IllegalArgumentException("PerformanceExecutionResult cannot be null.");
        }
        scenarioResults.add(result);
    }

    public List<PerformanceExecutionResult> getScenarioResults() {
        return Collections.unmodifiableList(scenarioResults);
    }

    public int getTotalScenarios() {
        return scenarioResults.size();
    }

    public boolean hasScenarioResults() {
        return !scenarioResults.isEmpty();
    }

    public long getPassedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.PASS)
                .count();
    }

    public long getFailedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.FAIL)
                .count();
    }

    public long getExpectedFailConfirmedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_CONFIRMED)
                .count();
    }

    public long getExpectedFailNotTriggeredScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED)
                .count();
    }

    public long getSkippedScenarios() {
        return scenarioResults.stream()
                .filter(result -> result.getExecutionStatus() == PerformanceExecutionStatus.SKIPPED)
                .count();
    }

    public double getPassRatePercent() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }
        return (getPassedScenarios() * 100.0) / scenarioResults.size();
    }

    public double getFailRatePercent() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }
        return (getFailedScenarios() * 100.0) / scenarioResults.size();
    }

    public double getAverageErrorPercent() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }

        double total = scenarioResults.stream()
                .mapToDouble(PerformanceExecutionResult::getErrorPercent)
                .sum();

        return total / scenarioResults.size();
    }

    public double getAverageRiskScore() {
        if (scenarioResults.isEmpty()) {
            return 0.0;
        }

        double total = scenarioResults.stream()
                .mapToInt(PerformanceExecutionResult::getRiskScore)
                .sum();

        return total / scenarioResults.size();
    }

    public long getTotalScenarioDurationMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalScenarioDurationMs)
                .sum();
    }

    public long getAverageScenarioDurationMs() {
        if (scenarioResults.isEmpty()) {
            return 0L;
        }

        return Math.round((double) getTotalScenarioDurationMs() / scenarioResults.size());
    }

    public long getTotalSamples() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalSamples)
                .sum();
    }

    public long getTotalErrors() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalErrors)
                .sum();
    }

    public long getSlowestP95ResponseTimeMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getP95ResponseTimeMs)
                .max()
                .orElse(0L);
    }

    public long getSlowestAverageResponseTimeMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getAverageResponseTimeMs)
                .max()
                .orElse(0L);
    }

    public int getHighestRiskScore() {
        return scenarioResults.stream()
                .mapToInt(PerformanceExecutionResult::getRiskScore)
                .max()
                .orElse(0);
    }

    public long getHighestScenarioDurationMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalScenarioDurationMs)
                .max()
                .orElse(0L);
    }

    public long getShortestScenarioDurationMs() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalScenarioDurationMs)
                .min()
                .orElse(0L);
    }

    public long getHighestTotalErrors() {
        return scenarioResults.stream()
                .mapToLong(PerformanceExecutionResult::getTotalErrors)
                .max()
                .orElse(0L);
    }

    public double getHighestErrorPercent() {
        return scenarioResults.stream()
                .mapToDouble(PerformanceExecutionResult::getErrorPercent)
                .max()
                .orElse(0.0);
    }

    public long getThresholdBreachScenarioCount() {
        return scenarioResults.stream()
                .filter(this::hasThresholdBreach)
                .count();
    }

    public long getErrorScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> result.getTotalErrors() > 0 || result.getErrorPercent() > 0.0)
                .count();
    }

    public long getHighOrCriticalRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(this::isHighOrCriticalRisk)
                .count();
    }

    public long getCriticalRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "Critical"))
                .count();
    }

    public long getHighRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "High"))
                .count();
    }

    public long getMediumRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "Medium"))
                .count();
    }

    public long getLowRiskScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> equalsIgnoreCase(result.getRiskLevel(), "Low"))
                .count();
    }

    public long getNoIssueScenarioCount() {
        return scenarioResults.stream()
                .filter(result -> !isAttentionNeeded(result))
                .count();
    }

    public PerformanceExecutionResult getSlowestP95Scenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getP95ResponseTimeMs))
                .orElse(null);
    }

    public PerformanceExecutionResult getSlowestAverageResponseScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getAverageResponseTimeMs))
                .orElse(null);
    }

    public PerformanceExecutionResult getHighestErrorScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingDouble(PerformanceExecutionResult::getErrorPercent))
                .orElse(null);
    }

    public PerformanceExecutionResult getHighestRiskScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingInt(PerformanceExecutionResult::getRiskScore))
                .orElse(null);
    }

    public PerformanceExecutionResult getLongestDurationScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getTotalScenarioDurationMs))
                .orElse(null);
    }

    public PerformanceExecutionResult getShortestDurationScenario() {
        return scenarioResults.stream()
                .min(Comparator.comparingLong(PerformanceExecutionResult::getTotalScenarioDurationMs))
                .orElse(null);
    }

    public PerformanceExecutionResult getHighestTotalErrorsScenario() {
        return scenarioResults.stream()
                .max(Comparator.comparingLong(PerformanceExecutionResult::getTotalErrors))
                .orElse(null);
    }

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

    private boolean hasThresholdBreach(PerformanceExecutionResult result) {
        String summary = result.getThresholdBreachSummary();
        return summary != null
                && !summary.isBlank()
                && !NO_THRESHOLD_BREACHES.equalsIgnoreCase(summary.trim());
    }

    private boolean isHighOrCriticalRisk(PerformanceExecutionResult result) {
        return equalsIgnoreCase(result.getRiskLevel(), "High")
                || equalsIgnoreCase(result.getRiskLevel(), "Critical")
                || result.getRiskScore() >= 51;
    }

    private boolean isAttentionNeeded(PerformanceExecutionResult result) {
        return result.getExecutionStatus() == PerformanceExecutionStatus.FAIL
                || result.getExecutionStatus() == PerformanceExecutionStatus.EXPECTED_FAIL_NOT_TRIGGERED
                || hasThresholdBreach(result)
                || result.getTotalErrors() > 0
                || isHighOrCriticalRisk(result);
    }

    private boolean equalsIgnoreCase(String value, String target) {
        return value != null && value.equalsIgnoreCase(target);
    }

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