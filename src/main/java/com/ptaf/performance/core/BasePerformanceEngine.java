package com.ptaf.performance.core;

import com.ptaf.performance.assertions.PerformanceAssertionEngine;
import com.ptaf.performance.builders.PerformanceTestPlanBuilder;
import com.ptaf.performance.reports.PerformanceReportManager;
import com.ptaf.performance.reports.PerformanceSummaryWriter;

/**
 * Shared architect-owned base for all performance engines.
 */
public abstract class BasePerformanceEngine {

    protected final PerformanceTestPlanBuilder testPlanBuilder = new PerformanceTestPlanBuilder();
    protected final PerformanceExecutionManager executionManager = new PerformanceExecutionManager();
    protected final PerformanceAssertionEngine assertionEngine = new PerformanceAssertionEngine();
    protected final PerformanceReportManager reportManager = new PerformanceReportManager();
    protected final PerformanceSummaryWriter summaryWriter = new PerformanceSummaryWriter();
}