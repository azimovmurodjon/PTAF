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

    /**
     * Engine responsible for evaluating performance assertions.
     *
     * <p>
     * The PerformanceAssertionEngine encapsulates logic that validates actual
     * performance results against configured expectations (assertions).
     * Subclasses can use this instance to register assertions, evaluate them
     * after test execution, and report pass/fail status.
     * </p>
     *
     * <p>
     * This field is final and created during construction of the engine to
     * ensure a stable assertion engine instance for the lifetime of the
     * performance engine instance.
     * </p>
     */
    protected final PerformanceAssertionEngine assertionEngine;

    /**
     * Builder used to construct performance test plans.
     *
     * <p>
     * The PerformanceTestPlanBuilder provides a fluent API for assembling the
     * structure of a performance test (scenarios, threads, timings, etc.).
     * Subclasses and test harnesses should use this builder to compose the
     * plan that will be executed by a concrete engine implementation.
     * </p>
     *
     * <p>
     * This instance is owned by the engine and is intended to be reused for
     * the duration of the engine instance (it is final).
     * </p>
     */
    protected final PerformanceTestPlanBuilder testPlanBuilder;

    /**
     * Manager responsible for generating and coordinating performance reports.
     *
     * <p>
     * The PerformanceReportManager handles creation and aggregation of
     * execution reports (such as raw results, metrics, and structured outputs)
     * and provides facilities for storing or publishing those reports.
     * </p>
     *
     * <p>
     * Tests and engine implementations should interact with this manager to
     * record execution artifacts and obtain report references.
     * </p>
     */
    protected final PerformanceReportManager reportManager;

    /**
     * Writer used to produce human-readable performance summaries.
     *
     * <p>
     * The PerformanceSummaryWriter formats and emits summary information
     * (for example, overview of results, key metrics, and assertion outcomes).
     * It is separate from {@link PerformanceReportManager} to allow different
     * formatting/placement strategies for summaries versus raw reports.
     * </p>
     */
    protected final PerformanceSummaryWriter summaryWriter;

    /**
     * Protected constructor to initialize framework-owned dependencies.
     *
     * <p>
     * Subclasses of BasePerformanceEngine should call this constructor (implicit
     * when they extend this class) to obtain fresh, final instances of the
     * core helper components used throughout the engine lifecycle.
     * </p>
     *
     * <p>
     * Note for testers:
     * each engine instance receives its own instances of the assertion engine,
     * test plan builder, report manager, and summary writer. If you need to
     * control or substitute these collaborators (for example, in tests),
     * create a test-specific subclass or use dependency injection at a higher
     * layer — this base class constructs concrete default implementations.
     * </p>
     */
    protected BasePerformanceEngine() {
        // Initialize the assertion engine used to validate performance outcomes.
        this.assertionEngine = new PerformanceAssertionEngine();

        // Initialize the builder that assembles test plans (scenarios, threads, etc.).
        this.testPlanBuilder = new PerformanceTestPlanBuilder();

        // Initialize the manager responsible for producing and organizing reports.
        this.reportManager = new PerformanceReportManager();

        // Initialize the writer that produces concise human-readable summaries.
        this.summaryWriter = new PerformanceSummaryWriter();
    }
}
