package com.ptaf.performance.core;

import com.ptaf.performance.models.PerformanceExecutionResult;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

import java.nio.file.Path;

/**
 * Responsible for executing prepared performance test plans and transforming
 * low-level execution statistics into framework-owned result objects.
 *
 * <p>This class hides JMeter execution details from the rest of the framework
 * and provides a standardized output model for assertions, reporting, and
 * future integrations.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Execute JMeter DSL test plans</li>
 *   <li>Convert execution statistics into PerformanceExecutionResult</li>
 *   <li>Preserve standardized artifact paths for dashboard, JTL, and summary</li>
 * </ul>
 * </p>
 */
public class PerformanceExecutionManager {

    /**
     * Executes the given performance test plan and converts the result into
     * a framework-owned execution result.
     *
     * @param testName logical test name
     * @param testPlan prepared JMeter DSL test plan
     * @param dashboardPath dashboard output path
     * @param jtlFilePath raw JTL result file path
     * @param summaryFilePath summary output file path
     * @return standardized execution result
     */
    public PerformanceExecutionResult execute(String testName,
                                              DslTestPlan testPlan,
                                              Path dashboardPath,
                                              Path jtlFilePath,
                                              Path summaryFilePath) {
        try {
            TestPlanStats stats = testPlan.run();

            long totalSamples = stats.overall().samplesCount();
            long totalErrors = stats.overall().errorsCount();

            double errorPercent = totalSamples == 0
                    ? 0.0
                    : ((double) totalErrors / totalSamples) * 100.0;

            long averageResponseTimeMs = Math.round(stats.overall().sampleTime().mean().toMillis());
            long p95ResponseTimeMs = Math.round(stats.overall().sampleTime().perc95().toMillis());

            return new PerformanceExecutionResult(
                    testName,
                    totalSamples,
                    totalErrors,
                    errorPercent,
                    averageResponseTimeMs,
                    p95ResponseTimeMs,
                    dashboardPath == null ? null : dashboardPath.toString(),
                    jtlFilePath == null ? null : jtlFilePath.toString(),
                    summaryFilePath == null ? null : summaryFilePath.toString()
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Performance execution failed for test: " + testName,
                    e
            );
        }
    }
}