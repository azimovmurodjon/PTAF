package com.ptaf.performance.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Run-level performance report model.
 *
 * <p>This object represents one full performance execution run and contains
 * all scenario-level results that belong to the same run.</p>
 */
public class PerformanceRunReport {

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

    public PerformanceExecutionResult getSlowestP95Scenario() {
        return scenarioResults.stream()
                .max((a, b) -> Long.compare(a.getP95ResponseTimeMs(), b.getP95ResponseTimeMs()))
                .orElse(null);
    }

    public PerformanceExecutionResult getHighestErrorScenario() {
        return scenarioResults.stream()
                .max((a, b) -> Double.compare(a.getErrorPercent(), b.getErrorPercent()))
                .orElse(null);
    }

    public PerformanceExecutionResult getHighestRiskScenario() {
        return scenarioResults.stream()
                .max((a, b) -> Integer.compare(a.getRiskScore(), b.getRiskScore()))
                .orElse(null);
    }

    public PerformanceExecutionResult getLongestDurationScenario() {
        return scenarioResults.stream()
                .max((a, b) -> Long.compare(a.getTotalScenarioDurationMs(), b.getTotalScenarioDurationMs()))
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

        if (failCount == 0 && expectedNotTriggeredCount == 0 && skippedCount == 0) {
            if (expectedConfirmedCount > 0) {
                return "Run completed successfully. Positive scenarios passed, and expected-failure scenarios behaved as designed.";
            }
            if (highestRiskScore >= 51) {
                return "All scenarios completed, but some scenarios still show elevated risk and should be reviewed.";
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

        return "This run contains a mix of passed, failed, expected-failure, or skipped scenarios. Review the scenario summary and charts for detailed risk areas.";
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
                '}';
    }
}