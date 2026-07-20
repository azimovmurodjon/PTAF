package com.ptaf.softassert;

import com.ptaf.utils.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-local context that collects soft assertion failures during a scenario.
 *
 * <h3>Purpose</h3>
 * <p>When {@code soft_assertions.enabled: true} in {@code config.yml}, this class acts as
 * the central collector for all step failures within a single scenario. Instead of stopping
 * the scenario on the first failure, each failure is recorded here. At the end of the scenario,
 * the collected failures are flushed — if any exist, the scenario is failed with a full summary.</p>
 *
 * <h3>Thread safety</h3>
 * <p>Each test thread (scenario) has its own isolated {@link SoftAssertionContext} instance
 * stored in a {@link ThreadLocal}. This is safe for parallel execution.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Cleared at the start of each scenario (via {@link #clear()}).</li>
 *   <li>Failures are added during step execution (via {@link #recordFailure(String, String, String)}).</li>
 *   <li>At the end of the scenario, {@link #hasFailed()} is checked and {@link #buildSummary()} is used
 *       to construct the failure message.</li>
 *   <li>Cleared again after the scenario ends.</li>
 * </ol>
 *
 * <h3>When soft assertions are disabled</h3>
 * <p>When {@code soft_assertions.enabled: false}, this class is never populated.
 * All methods are safe to call but have no effect on test behavior.</p>
 */
public final class SoftAssertionContext {

    private static final Logger logger = LoggerFactory.getLogger(SoftAssertionContext.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Per-thread list of soft failures recorded during the current scenario. */
    private static final ThreadLocal<List<SoftFailure>> FAILURES = ThreadLocal.withInitial(ArrayList::new);

    /**
     * Per-thread count of failures that have already been consumed by the reporting listener.
     * The listener compares this against {@link #getFailureCount()} to detect new failures
     * added during the most recently completed step.
     */
    private static final ThreadLocal<Integer> REPORTED_COUNT = ThreadLocal.withInitial(() -> 0);

    /**
     * Returns the number of soft failures that have not yet been reported by the step-level listener.
     * Called by {@code SoftAssertionReportListener} after each step finishes.
     *
     * @return count of unreported failures since the last call to {@link #markFailuresReported()}
     */
    public static int getUnreportedFailureCount() {
        return FAILURES.get().size() - REPORTED_COUNT.get();
    }

    /**
     * Returns the list of soft failures that have not yet been reported by the step-level listener.
     * Called by {@code SoftAssertionReportListener} to get the failures added during the last step.
     *
     * @return unmodifiable list of unreported failures
     */
    public static List<SoftFailure> getUnreportedFailures() {
        int reported = REPORTED_COUNT.get();
        List<SoftFailure> all = FAILURES.get();
        if (reported >= all.size()) return Collections.emptyList();
        return Collections.unmodifiableList(all.subList(reported, all.size()));
    }

    /**
     * Mark all currently recorded failures as reported so the next call to
     * {@link #getUnreportedFailureCount()} returns 0 until new failures are added.
     * Called by {@code SoftAssertionReportListener} after it has processed the failures.
     */
    public static void markFailuresReported() {
        REPORTED_COUNT.set(FAILURES.get().size());
    }

    /** Private constructor — static utility class. */
    private SoftAssertionContext() {
        throw new IllegalStateException("SoftAssertionContext is a static utility class.");
    }

    // ─── Recording ────────────────────────────────────────────────────────────────

    /**
     * Record a soft failure for the current scenario.
     *
     * <p>Called by {@link com.ptaf.ui.pages.PageCommonMethods} and
     * {@link com.ptaf.mobile.pages.MobileCommonMethods} when a step fails in soft assertion mode
     * and the retry window has expired.</p>
     *
     * @param stepDescription a human-readable description of what failed (e.g., "tap on LoginPage.loginButton")
     * @param errorMessage    the error message from the exception
     * @param screenshotPath  path to the failure screenshot (may be null if screenshot capture failed)
     */
    public static void recordFailure(String stepDescription, String errorMessage, String screenshotPath) {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        SoftFailure failure = new SoftFailure(timestamp, stepDescription, errorMessage, screenshotPath);
        FAILURES.get().add(failure);
        logger.warn("PTAF Soft Assert | Failure #{} recorded at {}: [{}] — {}",
            FAILURES.get().size(), timestamp, stepDescription, errorMessage);
    }

    // ─── Querying ─────────────────────────────────────────────────────────────────

    /**
     * Check whether any soft failures have been recorded for the current scenario.
     *
     * @return {@code true} if at least one soft failure has been recorded, {@code false} otherwise
     */
    public static boolean hasFailed() {
        return !FAILURES.get().isEmpty();
    }

    /**
     * Get the number of soft failures recorded for the current scenario.
     *
     * @return the failure count
     */
    public static int getFailureCount() {
        return FAILURES.get().size();
    }

    /**
     * Get an unmodifiable view of all recorded soft failures for the current scenario.
     *
     * @return list of {@link SoftFailure} records
     */
    public static List<SoftFailure> getFailures() {
        return Collections.unmodifiableList(FAILURES.get());
    }

    // ─── Summary ──────────────────────────────────────────────────────────────────

    /**
     * Build a formatted failure summary message for use in the scenario failure assertion.
     *
     * <p>The summary lists all recorded failures with their timestamps, descriptions,
     * and error messages. This message is passed to {@code scenario.log()} and thrown
     * as an {@link AssertionError} at the end of the scenario.</p>
     *
     * @return a multi-line failure summary string
     */
    public static String buildSummary() {
        List<SoftFailure> failures = FAILURES.get();
        if (failures.isEmpty()) return "No soft assertion failures recorded.";

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════════╗\n");
        sb.append("║         PTAF SOFT ASSERTION FAILURES — SCENARIO SUMMARY       ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        sb.append(String.format("  Total failures: %d\n\n", failures.size()));

        for (int i = 0; i < failures.size(); i++) {
            SoftFailure f = failures.get(i);
            sb.append(String.format("  [%d] %s — %s\n", i + 1, f.timestamp, f.stepDescription));
            sb.append(String.format("      Error    : %s\n", f.errorMessage != null ? f.errorMessage : "(no message)"));
            if (f.screenshotPath != null) {
                sb.append(String.format("      Screenshot: %s\n", f.screenshotPath));
            }
            sb.append("\n");
        }

        sb.append("  To investigate: review the screenshots attached to this report.\n");
        sb.append("  To disable soft assertions: set soft_assertions.enabled: false in config.yml.\n");
        return sb.toString();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Clear all recorded soft failures for the current thread.
     *
     * <p>Must be called at the start and end of each scenario to ensure test isolation.
     * Called by the Cucumber {@code @Before} and {@code @After} hooks in
     * {@link com.ptaf.hooks.Hooks} and {@link com.ptaf.hooks.MobileHooks}.</p>
     */
    public static void clear() {
        int count = FAILURES.get().size();
        FAILURES.get().clear();
        REPORTED_COUNT.set(0); // reset reported count so next scenario starts fresh
        if (count > 0) {
            logger.debug("PTAF Soft Assert | Cleared {} failure(s) from context.", count);
        }
    }

    // ─── Inner class ──────────────────────────────────────────────────────────────

    /**
     * Immutable record of a single soft assertion failure.
     */
    public static final class SoftFailure {
        /** Time the failure occurred (HH:mm:ss.SSS format). */
        public final String timestamp;
        /** Human-readable description of the step that failed. */
        public final String stepDescription;
        /** The error message from the exception. */
        public final String errorMessage;
        /** Path to the failure screenshot, or null if not captured. */
        public final String screenshotPath;

        SoftFailure(String timestamp, String stepDescription, String errorMessage, String screenshotPath) {
            this.timestamp = timestamp;
            this.stepDescription = stepDescription;
            this.errorMessage = errorMessage;
            this.screenshotPath = screenshotPath;
        }
    }
}
