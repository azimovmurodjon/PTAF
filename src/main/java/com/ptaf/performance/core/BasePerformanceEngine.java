package com.ptaf.performance.core;

import com.ptaf.performance.assertions.PerformanceAssertionEngine;
import com.ptaf.performance.builders.PerformanceTestPlanBuilder;
import com.ptaf.performance.reports.PerformanceReportManager;
import com.ptaf.performance.reports.PerformanceSummaryWriter;

/**
 * Shared architect-owned base for performance engines.
 *
 * <p>This base class centralizes reusable framework-owned dependencies for
 * performance execution layers.</p>
 *
 * <p>Current responsibilities:
 * <ul>
 *   <li>provide shared access to assertion engine</li>
 *   <li>provide shared access to test plan builder</li>
 *   <li>provide shared access to report manager</li>
 *   <li>provide shared access to summary writer</li>
 * </ul>
 * </p>
 *
 * <p>Important:
 * this class does not control run-folder lifecycle, scenario-folder naming,
 * or execution orchestration. That responsibility belongs to concrete engine
 * implementations such as {@link PerformanceEngine}.</p>
 */
public abstract class BasePerformanceEngine {

    protected final PerformanceAssertionEngine assertionEngine;
    protected final PerformanceTestPlanBuilder testPlanBuilder;
    protected final PerformanceReportManager reportManager;
    protected final PerformanceSummaryWriter summaryWriter;

    protected BasePerformanceEngine() {
        this.assertionEngine = new PerformanceAssertionEngine();
        this.testPlanBuilder = new PerformanceTestPlanBuilder();
        this.reportManager = new PerformanceReportManager();
        this.summaryWriter = new PerformanceSummaryWriter();
    }
}