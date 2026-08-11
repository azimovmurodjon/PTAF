//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.ptaf.hooks;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.ptaf.ui.pages.PageCommonMethods;
import com.ptaf.utils.BrowserFactory;
import com.ptaf.utils.ConfigurationProperties;
import com.ptaf.softassert.SoftAssertionContext;
import com.ptaf.utils.BrowserFactory.BrowserTypeEnum;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central Cucumber Hooks class responsible for lifecycle management of Playwright browser resources used in tests.
 *
 * <p>
 * Responsibilities:
 * - Initialize and teardown Playwright Browser/Context/Page per test scenario unless the scenario is marked as a
 *   performance or mobile-native scenario (those do not use Playwright browser resources).
 * - Support a special feature-level optimization when a feature is tagged with @LastScenario:
 *   The first scenario in such a feature opens a shared browser that is reused for all scenarios in that feature.
 *   If the shared browser becomes unavailable or a scenario fails, remaining scenarios in that feature are intentionally
 *   failed to avoid inconsistent state.
 * - Expose thread-local accessors for Browser, BrowserContext, Page, and Scenario for use by step definitions.
 * - Provide helper methods to count scenarios inside a feature file to determine how many scenarios belong to a feature
 *   (used by @LastScenario tracking).
 * </p>
 *
 * <p>
 * Notes for testers:
 * - Scenarios tagged with performance-related tags (e.g. @performance_testing) or mobile tags (e.g. @mobile)
 *   will not trigger Playwright browser initialization. Teardown routines for those scenarios will not attempt to
 *   close Playwright browser resources.
 * - If you add new special tags that should be treated as performance tags, extend the PERFORMANCE_TAGS set or ensure
 *   they start with "@performance".
 * </p>
 */
public class Hooks {
    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

    // ThreadLocal storage so parallel scenarios (threads) do not interfere with each other.
    private static final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal();
    private static final ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal();
    private static final ThreadLocal<Page> pageThreadLocal = new ThreadLocal();
    private static final ThreadLocal<Scenario> scenarioThreadLocal = new ThreadLocal();
    private static final ThreadLocal<PageCommonMethods> pageCommonMethodsThreadLocal = new ThreadLocal();
    private static final ThreadLocal<String> activeFeatureThreadLocal = new ThreadLocal();

    // Booleans to mark scenario types so teardown knows what to do (skip UI cleanup for performance/mobile).
    private static final ThreadLocal<Boolean> performanceScenarioThreadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> mobileScenarioThreadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Flag set by the "we close all browsers" step to indicate the browser was closed
     * intentionally by the test (not due to a failure). When this flag is true, the
     * @LastScenario teardown logic will NOT mark the feature as failed just because the
     * browser is gone, and setUp() will create a fresh browser for the next scenario
     * instead of throwing "Shared browser was closed or lost".
     */
    private static final ThreadLocal<Boolean> browserClosedIntentionallyThreadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Feature-level tracking maps to support the @LastScenario optimization.
    // lastScenarioFeatureMap: featureKey -> whether feature uses @LastScenario semantics
    // featureScenarioTotalMap: featureKey -> total number of runnable scenarios in that feature
    // featureScenarioExecutedMap: featureKey -> how many executed so far
    // featureFailureMap: featureKey -> whether the feature is considered failed (so remaining scenarios are intentionally failed)
    private static final Map<String, Boolean> lastScenarioFeatureMap = new ConcurrentHashMap();
    private static final Map<String, Integer> featureScenarioTotalMap = new ConcurrentHashMap();
    private static final Map<String, AtomicInteger> featureScenarioExecutedMap = new ConcurrentHashMap();
    private static final Map<String, Boolean> featureFailureMap = new ConcurrentHashMap();

    // Known performance-related tags. If a scenario uses any of these tags it is considered a performance scenario.
    private static final Set<String> PERFORMANCE_TAGS = Set.of("@performance_testing", "@performance_full_regression", "@performance_get", "@performance_post", "@performance_put", "@performance_delete", "@performance_profile", "@performance_inline_json", "@performance_yaml", "@performance_csv", "@performance_excel", "@performance_auth", "@performance_bearer", "@performance_basic_auth", "@performance_negative", "@performance_expected_failure");

    /**
     * Cucumber @Before hook executed before every scenario.
     *
     * <p>
     * Main responsibilities:
     * - Determine and store the feature key associated with the scenario.
     * - Decide whether the scenario is a performance or mobile-native scenario and skip Playwright setup accordingly.
     * - For regular UI scenarios: initialize Playwright Browser/Context/Page (or reuse if appropriate).
     * - Handle special @LastScenario feature behavior to reuse a single browser across all scenarios in the feature.
     * </p>
     *
     * @param scenario the current Cucumber scenario instance
     */
    @Before
    public void setUp(Scenario scenario) {
        // Store current scenario in thread local for step definitions or other helpers.
        scenarioThreadLocal.set(scenario);

        // Attempt to resolve a stable feature key for tracking. Fall back if resolution fails.
        String featureKey = this.getFeatureKey(scenario);
        if (featureKey == null || featureKey.trim().isEmpty()) {
            featureKey = this.buildFallbackFeatureKey(scenario);
            logger.warn("Unable to resolve feature key for scenario [{}]. Using fallback feature key [{}].", scenario != null ? scenario.getName() : "UNKNOWN", featureKey);
        }

        activeFeatureThreadLocal.set(featureKey);

        // If this is a performance scenario, flag it and skip UI browser initialization.
        if (this.isPerformanceScenario(scenario)) {
            performanceScenarioThreadLocal.set(Boolean.TRUE);
            mobileScenarioThreadLocal.set(Boolean.FALSE);
            logger.info("Performance scenario detected [{}]. Skipping Playwright browser initialization.", scenario != null ? scenario.getName() : "UNKNOWN");
        } else if (this.isMobileScenario(scenario)) {
            // If this is a mobile native scenario (handled by Appium or similar), skip Playwright initialization.
            performanceScenarioThreadLocal.set(Boolean.FALSE);
            mobileScenarioThreadLocal.set(Boolean.TRUE);
            logger.info("Mobile native scenario detected [{}]. Skipping Playwright browser initialization.", scenario != null ? scenario.getName() : "UNKNOWN");
        } else if (this.isNonUiScenario(scenario)) {
            // If this is a non-UI scenario (XML, CSV, API, Database, ZIP) that does not require a browser,
            // reuse the performanceScenarioThreadLocal flag to skip Playwright browser initialization.
            // This prevents an unnecessary browser window from opening during data-only test scenarios.
            performanceScenarioThreadLocal.set(Boolean.TRUE);
            mobileScenarioThreadLocal.set(Boolean.FALSE);
            logger.info("Non-UI scenario detected [{}]. Skipping Playwright browser initialization.", scenario != null ? scenario.getName() : "UNKNOWN");
        } else {
            // Regular UI scenario requires Playwright browser. Reset other flags.
            performanceScenarioThreadLocal.set(Boolean.FALSE);
            mobileScenarioThreadLocal.set(Boolean.FALSE);

            // Check for @LastScenario tag on the scenario source tags. This enables shared browser behavior per feature.
            boolean isLastScenarioTaggedFeature = scenario != null && scenario.getSourceTagNames().contains("@LastScenario");
            lastScenarioFeatureMap.putIfAbsent(featureKey, isLastScenarioTaggedFeature);

            // Setup feature tracking if this feature is marked with @LastScenario.
            if (isLastScenarioTaggedFeature) {
                // Compute total scenarios in feature only once.
                featureScenarioTotalMap.computeIfAbsent(featureKey, (key) -> this.countScenariosInFeatureFile(scenario));
                featureScenarioExecutedMap.computeIfAbsent(featureKey, (key) -> new AtomicInteger(0));
                featureFailureMap.putIfAbsent(featureKey, false);
            }

            // Check if a browser already exists in thread local and appears alive (context and a non-closed page).
            Browser existingBrowser = (Browser)browserThreadLocal.get();
            BrowserContext existingContext = (BrowserContext)contextThreadLocal.get();
            Page existingPage = (Page)pageThreadLocal.get();
            boolean browserAlive = existingBrowser != null && existingContext != null && existingPage != null && !existingPage.isClosed();

            // If @LastScenario behavior is enabled for this feature, decide whether to create or reuse the shared browser.
            if (isLastScenarioTaggedFeature) {
                boolean featureAlreadyFailed = Boolean.TRUE.equals(featureFailureMap.get(featureKey));
                int alreadyExecuted = featureScenarioExecutedMap.containsKey(featureKey) ? ((AtomicInteger)featureScenarioExecutedMap.get(featureKey)).get() : 0;

                // If the feature was already marked failed due to an earlier scenario, stop further execution by throwing.
                if (featureAlreadyFailed) {
                    logger.error("Skipping scenario [{}] because @LastScenario feature [{}] is already marked as failed.", scenario != null ? scenario.getName() : "UNKNOWN", featureKey);
                    throw new RuntimeException("Previous scenario failed in @LastScenario feature. Remaining scenarios are failed intentionally.");
                }

                // If this is the first scenario in the feature and no usable browser is present, create the shared browser.
                if (alreadyExecuted == 0 && !browserAlive) {
                    this.createBrowserStack(scenario);
                    logger.info("Initial shared browser created for @LastScenario feature [{}], scenario [{}]", featureKey, scenario != null ? scenario.getName() : "UNKNOWN");
                    return;
                }

                // If we are beyond the first scenario (alreadyExecuted > 0) then we must have a live browser to continue.
                if (alreadyExecuted > 0) {
                if (browserAlive) {
                        // Reuse the existing shared browser - no new initialization required.
                        logger.info("Reusing shared browser for @LastScenario feature [{}], scenario [{}]", featureKey, scenario != null ? scenario.getName() : "UNKNOWN");
                        browserClosedIntentionallyThreadLocal.set(Boolean.FALSE);
                        return;
                    }

                    // Check whether the browser was closed intentionally by a test step (e.g. "we close all browsers").
                    // If so, open a fresh browser for this scenario instead of treating it as a failure.
                    boolean closedIntentionally = Boolean.TRUE.equals(browserClosedIntentionallyThreadLocal.get());
                    if (closedIntentionally) {
                        logger.info("Browser was closed intentionally by a test step in @LastScenario feature [{}]. Creating a fresh browser for scenario [{}].",
                            featureKey, scenario != null ? scenario.getName() : "UNKNOWN");
                        this.createBrowserStack(scenario);
                        // Clear the flag now that we have acted on it — prevents it from
                        // persisting into subsequent scenarios in the same feature.
                        browserClosedIntentionallyThreadLocal.set(Boolean.FALSE);
                        return;
                    }

                    // Browser disappeared unexpectedly (not intentional) - mark feature failed and fail remaining scenarios.
                    featureFailureMap.put(featureKey, true);
                    logger.error("Shared browser/session is no longer available for @LastScenario feature [{}] before scenario [{}]. Failing remaining scenarios and not reopening browser.", featureKey, scenario != null ? scenario.getName() : "UNKNOWN");
                    throw new RuntimeException("Shared browser was closed or lost during @LastScenario execution. Remaining scenarios are failed intentionally.");
                }
            }

            // Default browser setup for a normal scenario or the initial scenario in a non-shared context.
            this.createBrowserStack(scenario);
            browserClosedIntentionallyThreadLocal.set(Boolean.FALSE);
        }
    }

    /**
     * Cucumber @After hook executed after every scenario.
     *
     * <p>
     * Handles cleanup and feature-level bookkeeping:
     * - For performance/mobile scenarios: only cleans thread-local flags; does not touch Playwright resources.
     * - For @LastScenario feature: keeps the browser open until the last scenario in that feature completes (tracked via maps).
     *   If a scenario fails or the shared browser goes away during the feature execution, remaining scenarios are marked as failed.
     * - For regular scenarios: fully closes browser resources after each scenario.
     * </p>
     *
     * @param scenario the current Cucumber scenario instance
     */
    @After
    public void tearDown(Scenario scenario) {
        // ── Soft Assertion Flush ──────────────────────────────────────────────────────
        // When soft_assertions.enabled: true, check if any steps failed softly during this scenario.
        // IMPORTANT: We do NOT throw here immediately. Instead, we capture the summary and throw
        // AFTER the @LastScenario browser management logic has run. This prevents the AssertionError
        // from being thrown before the @LastScenario counter is incremented and the browser-keep-alive
        // decision is made — which was causing the browser to close prematurely on soft failures.
        String softAssertionSummary = null;
        if (ConfigurationProperties.isSoftAssertionsEnabled() && SoftAssertionContext.hasFailed()) {
            softAssertionSummary = SoftAssertionContext.buildSummary();
            if (scenario != null) {
                scenario.log(softAssertionSummary); // attach the summary to the Cucumber/Extent report
            }
        }
        // Always clear the soft assertion context at end of scenario to ensure test isolation.
        SoftAssertionContext.clear();
        // ─────────────────────────────────────────────────────────────────────────────

        // Resolve the feature key in a safe way: try threadlocal first, then recover from the scenario if missing.
        String featureKey = this.getSafeFeatureKeyForTearDown(scenario);
        boolean var22 = false;

        label443: {
            try {
                var22 = true;
                // If neither performance nor mobile flags are set, and the scenario passed, attempt to finalize scenario via PageCommonMethods.
                if (!Boolean.TRUE.equals(performanceScenarioThreadLocal.get()) && !Boolean.TRUE.equals(mobileScenarioThreadLocal.get())) {
                    if (scenario.getStatus() == Status.PASSED) {
                        PageCommonMethods pageCommonMethods = (PageCommonMethods)pageCommonMethodsThreadLocal.get();
                        if (pageCommonMethods != null) {
                            // Allow PageCommonMethods to perform any finalization like flushing logs/screenshots.
                            pageCommonMethods.finalizeScenario();
                            var22 = false;
                        } else {
                            var22 = false;
                        }
                    } else {
                        var22 = false;
                    }
                } else {
                    var22 = false;
                }
                break label443;
            } catch (Exception e) {
                // Ensure exceptions during finalization do not prevent cleanup. Log details for debugging.
                logger.error("Error during scenario teardown: {}", e.getMessage(), e);
                var22 = false;
            } finally {
                // The finally block below duplicates logic to ensure proper cleanup ordering in all exceptional paths.
                if (var22) {
                    if (Boolean.TRUE.equals(performanceScenarioThreadLocal.get())) {
                        this.clearPerformanceScenarioStateOnly();
                        logger.info("Performance scenario teardown completed without UI browser cleanup requirement.");
                        return;
                    }

                    if (Boolean.TRUE.equals(mobileScenarioThreadLocal.get())) {
                        this.clearMobileScenarioStateOnly();
                        logger.info("Mobile native scenario teardown completed without UI browser cleanup requirement.");
                        return;
                    }

                    boolean isLastScenarioFeature = featureKey != null && Boolean.TRUE.equals(lastScenarioFeatureMap.get(featureKey));
                    if (isLastScenarioFeature) {
                        // For @LastScenario features we must update execution counters and decide whether to close shared browser.
                        Browser browser = (Browser)browserThreadLocal.get();
                        BrowserContext context = (BrowserContext)contextThreadLocal.get();
                        Page page = (Page)pageThreadLocal.get();
                        boolean browserAlive = browser != null && context != null && page != null && !page.isClosed();

                        // When soft assertions are enabled and browser is still alive, do NOT mark the feature as failed
                        // just because of soft assertion failures. The browser should stay open for the next scenario.
                        boolean isSoftAssertionOnlyFailure = ConfigurationProperties.isSoftAssertionsEnabled()
                            && softAssertionSummary != null
                            && browserAlive;

                        // If this scenario failed mark the feature as failed so remaining scenarios are intentionally failed.
                        // Exception: soft assertion failures with a live browser should NOT close the shared browser.
                        if (scenario.getStatus() == Status.FAILED && !isSoftAssertionOnlyFailure) {
                            featureFailureMap.put(featureKey, true);
                            logger.error("Scenario [{}] failed in @LastScenario feature [{}]. Remaining scenarios will fail immediately.", scenario.getName(), featureKey);
                        } else if (scenario.getStatus() == Status.FAILED && isSoftAssertionOnlyFailure) {
                            logger.warn("Scenario [{}] had soft assertion failures in @LastScenario feature [{}]. Browser is still alive — continuing to next scenario.", scenario.getName(), featureKey);
                        }

                        if (!browserAlive && !Boolean.TRUE.equals(browserClosedIntentionallyThreadLocal.get())) {
                            // Browser became unavailable unexpectedly (not due to an intentional close step).
                            featureFailureMap.put(featureKey, true);
                            logger.error("Shared browser/session became unavailable in @LastScenario feature [{}] after scenario [{}]. Remaining scenarios will fail immediately.", featureKey, scenario.getName());
                        } else if (!browserAlive) {
                            // Browser was closed intentionally by a test step — do NOT mark the feature as failed.
                            logger.info("Browser was closed intentionally in @LastScenario feature [{}] after scenario [{}]. Feature is NOT marked as failed.", featureKey, scenario.getName());
                        }

                        // Increment executed counter and log progress. Close resources only after last scenario.
                        AtomicInteger executedCounter = (AtomicInteger)featureScenarioExecutedMap.get(featureKey);
                        int executed = executedCounter != null ? executedCounter.incrementAndGet() : 1;
                        int total = (Integer)featureScenarioTotalMap.getOrDefault(featureKey, 1);
                        logger.info("Feature [{}] progress: {}/{}", new Object[]{featureKey, executed, total});
                        if (executed >= total) {
                            // Last scenario reached: close shared browser resources and cleanup feature trackers.
                            logger.info("Last scenario reached for feature [{}]. Closing browser resources.", featureKey);
                            closeBrowserResources();
                            clearFeatureTracking(featureKey);
                        } else {
                            // Keep browser alive for next scenario in the same feature.
                            logger.info("Keeping browser state unchanged for next scenario in @LastScenario feature [{}].", featureKey);
                        }
                    } else {
                        // Not a @LastScenario feature: normal cleanup. featureKey may be null in rare cases.
                        if (featureKey == null) {
                            logger.warn("Feature key was not available during teardown for scenario [{}]. Proceeding with normal browser cleanup to avoid NullPointerException.", scenario != null ? scenario.getName() : "UNKNOWN");
                        }

                        closeBrowserResources();
                    }

                }
            }

            // The code below repeats the same behavior as the finally block above in a non-exception-driven path.
            if (Boolean.TRUE.equals(performanceScenarioThreadLocal.get())) {
                this.clearPerformanceScenarioStateOnly();
                logger.info("Performance scenario teardown completed without UI browser cleanup requirement.");
                return;
            }

            if (Boolean.TRUE.equals(mobileScenarioThreadLocal.get())) {
                this.clearMobileScenarioStateOnly();
                logger.info("Mobile native scenario teardown completed without UI browser cleanup requirement.");
                return;
            }

            boolean isLastScenarioFeature = featureKey != null && Boolean.TRUE.equals(lastScenarioFeatureMap.get(featureKey));
            if (isLastScenarioFeature) {
                Browser browser = (Browser)browserThreadLocal.get();
                BrowserContext context = (BrowserContext)contextThreadLocal.get();
                Page page = (Page)pageThreadLocal.get();
                boolean browserAlive = browser != null && context != null && page != null && !page.isClosed();
                // When soft assertions are enabled and the browser is still alive, do NOT mark the
                // @LastScenario feature as failed just because of soft assertion failures.
                // A soft assertion failure means the scenario had issues but the browser is still
                // running and the next scenario should continue. Only mark failed if browser is dead.
                boolean isSoftAssertionOnlyFailure = ConfigurationProperties.isSoftAssertionsEnabled()
                    && softAssertionSummary != null
                    && browserAlive;

                if (scenario.getStatus() == Status.FAILED && !isSoftAssertionOnlyFailure) {
                    featureFailureMap.put(featureKey, true);
                    logger.error("Scenario [{}] failed in @LastScenario feature [{}]. Remaining scenarios will fail immediately.", scenario.getName(), featureKey);
                } else if (scenario.getStatus() == Status.FAILED && isSoftAssertionOnlyFailure) {
                    logger.warn("Scenario [{}] had soft assertion failures in @LastScenario feature [{}]. Browser is still alive — continuing to next scenario.", scenario.getName(), featureKey);
                }

                if (!browserAlive && !Boolean.TRUE.equals(browserClosedIntentionallyThreadLocal.get())) {
                    featureFailureMap.put(featureKey, true);
                    logger.error("Shared browser/session became unavailable in @LastScenario feature [{}] after scenario [{}]. Remaining scenarios will fail immediately.", featureKey, scenario.getName());
                } else if (!browserAlive) {
                    logger.info("Browser was closed intentionally in @LastScenario feature [{}] after scenario [{}]. Feature is NOT marked as failed.", featureKey, scenario.getName());
                }

                AtomicInteger executedCounter = (AtomicInteger)featureScenarioExecutedMap.get(featureKey);
                int executed = executedCounter != null ? executedCounter.incrementAndGet() : 1;
                int total = (Integer)featureScenarioTotalMap.getOrDefault(featureKey, 1);
                logger.info("Feature [{}] progress: {}/{}", new Object[]{featureKey, executed, total});
                if (executed >= total) {
                    logger.info("Last scenario reached for feature [{}]. Closing browser resources.", featureKey);
                    closeBrowserResources();
                    clearFeatureTracking(featureKey);
                } else {
                    logger.info("Keeping browser state unchanged for next scenario in @LastScenario feature [{}].", featureKey);
                }

                // Throw soft assertion summary AFTER browser management is complete.
                if (softAssertionSummary != null) {
                    throw new AssertionError("PTAF Soft Assertion Failures detected — see failed steps above for details. Total failures: " + com.ptaf.softassert.SoftAssertionContext.getFailureCount() + ". (set soft_assertions.enabled: false in config.yml to disable)");
                }
                return;
            } else {
                if (featureKey == null) {
                    logger.warn("Feature key was not available during teardown for scenario [{}]. Proceeding with normal browser cleanup to avoid NullPointerException.", scenario != null ? scenario.getName() : "UNKNOWN");
                }

                closeBrowserResources();
                // Throw soft assertion summary AFTER browser resources are closed.
                if (softAssertionSummary != null) {
                    throw new AssertionError("PTAF Soft Assertion Failures detected — see failed steps above for details. Total failures: " + com.ptaf.softassert.SoftAssertionContext.getFailureCount() + ". (set soft_assertions.enabled: false in config.yml to disable)");
                }
                return;
            }
        }

        // The following block handles the common code paths if execution reaches here for any reason.

        if (Boolean.TRUE.equals(performanceScenarioThreadLocal.get())) {
            this.clearPerformanceScenarioStateOnly();
            logger.info("Performance scenario teardown completed without UI browser cleanup requirement.");
        } else if (Boolean.TRUE.equals(mobileScenarioThreadLocal.get())) {
            this.clearMobileScenarioStateOnly();
            logger.info("Mobile native scenario teardown completed without UI browser cleanup requirement.");
        } else {
            boolean isLastScenarioFeature = featureKey != null && Boolean.TRUE.equals(lastScenarioFeatureMap.get(featureKey));
            if (isLastScenarioFeature) {
                Browser browser = (Browser)browserThreadLocal.get();
                BrowserContext context = (BrowserContext)contextThreadLocal.get();
                Page page = (Page)pageThreadLocal.get();
                boolean browserAlive = browser != null && context != null && page != null && !page.isClosed();

                boolean isSoftAssertionOnlyFailure = ConfigurationProperties.isSoftAssertionsEnabled()
                    && softAssertionSummary != null
                    && browserAlive;

                if (scenario.getStatus() == Status.FAILED && !isSoftAssertionOnlyFailure) {
                    featureFailureMap.put(featureKey, true);
                    logger.error("Scenario [{}] failed in @LastScenario feature [{}]. Remaining scenarios will fail immediately.", scenario.getName(), featureKey);
                } else if (scenario.getStatus() == Status.FAILED && isSoftAssertionOnlyFailure) {
                    logger.warn("Scenario [{}] had soft assertion failures in @LastScenario feature [{}]. Browser is still alive — continuing to next scenario.", scenario.getName(), featureKey);
                }

                if (!browserAlive && !Boolean.TRUE.equals(browserClosedIntentionallyThreadLocal.get())) {
                    featureFailureMap.put(featureKey, true);
                    logger.error("Shared browser/session became unavailable in @LastScenario feature [{}] after scenario [{}]. Remaining scenarios will fail immediately.", featureKey, scenario.getName());
                } else if (!browserAlive) {
                    logger.info("Browser was closed intentionally in @LastScenario feature [{}] after scenario [{}]. Feature is NOT marked as failed.", featureKey, scenario.getName());
                }

                AtomicInteger executedCounter = (AtomicInteger)featureScenarioExecutedMap.get(featureKey);
                int executed = executedCounter != null ? executedCounter.incrementAndGet() : 1;
                int total = (Integer)featureScenarioTotalMap.getOrDefault(featureKey, 1);
                logger.info("Feature [{}] progress: {}/{}", new Object[]{featureKey, executed, total});
                if (executed >= total) {
                    logger.info("Last scenario reached for feature [{}]. Closing browser resources.", featureKey);
                    closeBrowserResources();
                    clearFeatureTracking(featureKey);
                } else {
                    logger.info("Keeping browser state unchanged for next scenario in @LastScenario feature [{}].", featureKey);
                }
            } else {
                if (featureKey == null) {
                    logger.warn("Feature key was not available during teardown for scenario [{}]. Proceeding with normal browser cleanup to avoid NullPointerException.", scenario != null ? scenario.getName() : "UNKNOWN");
                }

                closeBrowserResources();
            }

        }
        // Throw soft assertion summary at the very end, after all browser management is complete.
        if (softAssertionSummary != null) {
            throw new AssertionError("PTAF Soft Assertion Failures detected — see failed steps above for details. Total failures: " + com.ptaf.softassert.SoftAssertionContext.getFailureCount() + ". (set soft_assertions.enabled: false in config.yml to disable)");
        }
    }

    /**
     * Create and initialize Playwright Browser, Context and Page for the current thread/scenario.
     *
     * <p>
     * Uses configuration to choose browser type. If the configured profile indicates a mobile browser,
     * the BrowserFactory is asked to create the appropriate mobile browser. A BrowserContext with video recording
     * is created and a new Page is opened. Default timeout values are applied to the Page based on configuration.
     * </p>
     *
     * @param scenario the current scenario (used for logging context)
     */
    private void createBrowserStack(Scenario scenario) {
        try {
            String browserName = ConfigurationProperties.getBrowser();
            Browser browser;
            // If the browser profile is a "mobile browser profile", let BrowserFactory create it accordingly.
            if (BrowserFactory.isMobileBrowserProfile(browserName)) {
                browser = BrowserFactory.createBrowser(browserName);
                logger.info("Mobile browser profile [{}] selected for scenario [{}].", browserName, scenario != null ? scenario.getName() : "UNKNOWN");
            } else {
                // Map configured browser name to BrowserTypeEnum and create browser via BrowserFactory.
                BrowserFactory.BrowserTypeEnum browserTypeEnum;
                switch (browserName.toUpperCase()) {
                    case "CHROME" -> browserTypeEnum = BrowserTypeEnum.CHROME;
                    case "FIREFOX" -> browserTypeEnum = BrowserTypeEnum.FIREFOX;
                    case "WEBKIT" -> browserTypeEnum = BrowserTypeEnum.WEBKIT;
                    case "EDGE" -> browserTypeEnum = BrowserTypeEnum.EDGE;
                    default -> throw new IllegalArgumentException("Unsupported browser type: " + browserName);
                }

                browser = BrowserFactory.createBrowser(browserTypeEnum);
            }
            // Persist browser into thread local storage for access from step definitions.
            browserThreadLocal.set(browser);

            // Create a context configured for video recording (handled inside BrowserFactory).
            BrowserContext context = BrowserFactory.createContextWithVideo(browser);
            contextThreadLocal.set(context);

            // ── Auto-maximize popup windows ──────────────────────────────────────────────
            // When maximize_browser=true, any new page (popup) opened by the browser
            // (e.g. window.open() or target="_blank" links) is automatically maximized via
            // JavaScript window.resizeTo(). This mirrors the behaviour of the initial window
            // which is maximized by the --start-maximized Chromium launch flag.
            // The listener is registered once per context so it covers all popups for the
            // entire scenario without any extra step definitions.
            boolean maximizeBrowserEnabled = Boolean.parseBoolean(
                ConfigurationProperties.getValue("maximize_browser"));
            boolean headlessMode = Boolean.parseBoolean(
                ConfigurationProperties.getHeadlessMode());
            if (maximizeBrowserEnabled && !headlessMode) {
                context.onPage(newPage -> {
                    try {
                        // Only resize top-level browser windows (popups opened by the app).
                        // The onPage event also fires for pages created by frame navigation
                        // (e.g. when a step definition switches to an iframe context). Running
                        // window.resizeTo() on an iframe page causes the frame context to reset
                        // and breaks subsequent actions on that frame.
                        // A top-level page has no parent frame; an iframe page does.
                        boolean isTopLevel = newPage.mainFrame().parentFrame() == null;
                        if (!isTopLevel) {
                            logger.debug("Skipping auto-maximize for non-top-level page [{}].", newPage.url());
                            return;
                        }
                        // Wait for the popup to reach at least DOMContentLoaded so the
                        // window object is available before we try to resize it.
                        newPage.waitForLoadState(
                            com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED,
                            new Page.WaitForLoadStateOptions().setTimeout(10_000));
                        // Resize the popup window to fill the screen.
                        newPage.evaluate("window.moveTo(0, 0); window.resizeTo(screen.width, screen.height);");
                        logger.info("Auto-maximized popup window [{}] because maximize_browser=true.",
                            newPage.url());
                    } catch (Exception popupEx) {
                        // Non-fatal: log but do not fail the scenario if the popup
                        // cannot be resized (e.g. the page closed before load completed).
                        logger.warn("Could not auto-maximize popup window: {}", popupEx.getMessage());
                    }
                });
                logger.info("Registered auto-maximize listener for popup windows on BrowserContext.");
            }
            // ────────────────────────────────────────────────────────────────────────────

            // Open a fresh page in the context and store it.
            Page page = context.newPage();
            pageThreadLocal.set(page);

            // Apply configured timeouts to the Page so all waits use consistent values.
            long runtimeTimeoutMillis = getConfiguredRuntimeTimeoutMillis();
            page.setDefaultTimeout((double)runtimeTimeoutMillis);
            page.setDefaultNavigationTimeout((double)runtimeTimeoutMillis);

            // Wrap Page with helper methods and make available to steps.
            PageCommonMethods pageCommonMethods = new PageCommonMethods(page);
            pageCommonMethodsThreadLocal.set(pageCommonMethods);
            logger.info("Browser setup completed for scenario: {} with runtime timeout: {} ms", scenario != null ? scenario.getName() : "UNKNOWN", runtimeTimeoutMillis);
        } catch (Exception e) {
            // If browser creation fails, bubble up a runtime exception so the scenario fails fast with a clear error.
            logger.error("Error setting up the browser for scenario: {}", scenario != null ? scenario.getName() : "UNKNOWN", e);
            throw new RuntimeException("Browser setup failed", e);
        }
    }

    /**
     * Read runtimeWait configuration key and convert to milliseconds, falling back to 30 seconds on errors.
     *
     * @return configured runtime timeout in milliseconds
     */
    private static long getConfiguredRuntimeTimeoutMillis() {
        try {
            String runtimeValue = ConfigurationProperties.getValue("runtimeWait");
            if (runtimeValue != null && !runtimeValue.trim().isEmpty()) {
                long seconds = Long.parseLong(runtimeValue.trim());
                if (seconds <= 0L) {
                    // runtimeWait: 0 means "delegate to time_to_wait_in_seconds".
                    // Use time_to_wait_in_seconds as the page default timeout so there is
                    // no separate Playwright default that stacks on top of the element wait.
                    String timeToWait = ConfigurationProperties.getValue("time_to_wait_in_seconds");
                    if (timeToWait != null && !timeToWait.trim().isEmpty()) {
                        try {
                            long ttw = Long.parseLong(timeToWait.trim());
                            if (ttw > 0) {
                                logger.info("runtimeWait=0: using time_to_wait_in_seconds={} s = {} ms as page default timeout.",
                                    ttw, ttw * 1000L);
                                return ttw * 1000L;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    // Both are 0/missing — use a safe minimum of 30 s.
                    logger.warn("runtimeWait=0 and time_to_wait_in_seconds is not set. Defaulting to 30 seconds.");
                    return 30000L;
                } else {
                    long timeoutMillis = seconds * 1000L;
                    logger.info("Configured runtimeWait: {} second(s) = {} ms", seconds, timeoutMillis);
                    return timeoutMillis;
                }
            } else {
                logger.warn("runtimeWait is not configured. Defaulting to 30 seconds.");
                return 30000L;
            }
        } catch (Exception e) {
            // Return the default on parsing errors, but log the cause for diagnostics.
            logger.warn("Unable to parse runtimeWait from configuration. Defaulting to 30 seconds. Reason: {}", e.getMessage());
            return 30000L;
        }
    }

    /**
     * Wait until the current page reaches DOMContentLoaded and LOAD states using configured timeout.
     *
     * @throws RuntimeException if page does not reach required states within configured timeout
     */
    public static void waitForCurrentPageToLoad() {
        Page page = getPage();
        long timeout = getConfiguredRuntimeTimeoutMillis();

        try {
            // Wait for DOMContentLoaded and full LOAD state sequentially.
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, (new Page.WaitForLoadStateOptions()).setTimeout((double)timeout));
            page.waitForLoadState(LoadState.LOAD, (new Page.WaitForLoadStateOptions()).setTimeout((double)timeout));
            logger.info("Page reached load state successfully within {} ms", timeout);
        } catch (Exception e) {
            // Wrap and rethrow to make failure clear to test frameworks.
            logger.error("Page did not load within configured timeout: {} ms", timeout, e);
            throw new RuntimeException("Page did not load within configured timeout: " + timeout + " ms", e);
        }
    }

    /**
     * Switch the thread-local active Page to the provided Page instance. Re-applies configured timeouts and helper wrappers.
     *
     * @param page the Playwright Page to set as active
     * @throws IllegalArgumentException if the page is null or already closed
     */
    public static void setPage(Page page) {
        if (page != null && !page.isClosed()) {
            pageThreadLocal.set(page);

            try {
                long runtimeTimeoutMillis = getConfiguredRuntimeTimeoutMillis();
                page.setDefaultTimeout((double)runtimeTimeoutMillis);
                page.setDefaultNavigationTimeout((double)runtimeTimeoutMillis);
            } catch (Exception e) {
                // Do not fail the scenario if timeouts cannot be re-applied; log for diagnostics.
                logger.warn("Unable to apply timeout settings to switched page. Reason: {}", e.getMessage());
            }

            // Create new PageCommonMethods wrapper for the switched page.
            pageCommonMethodsThreadLocal.set(new PageCommonMethods(page));
            logger.info("Active page has been switched successfully.");
        } else {
            throw new IllegalArgumentException("The page is null or closed.");
        }
    }

    /**
     * Safely close Playwright pages, context and browser registered in this thread, and remove associated thread-local references.
     *
     * <p>
     * - Closes each page inside the context (if any) and then the context.
     * - Closes the browser.
     * - Removes thread-local references so next scenario starts with a fresh state.
     * </p>
     */
    public static void closeBrowserResources() {
        try {
            BrowserContext context = (BrowserContext)contextThreadLocal.get();
            if (context != null) {
                // Close every page in the context first; log any exceptions per-page but proceed with the rest.
                for(Page page : context.pages()) {
                    try {
                        if (page != null && !page.isClosed()) {
                            page.close();
                        }
                    } catch (Exception pageCloseEx) {
                        // TargetClosedError is expected when the browser/page was already closed
                        // (e.g. after a soft assertion scenario or a @LastScenario failure).
                        // Log as debug to avoid misleading ERROR messages in the report.
                        if (isTargetClosedError(pageCloseEx)) {
                            logger.debug("Page already closed (expected during teardown): {}", pageCloseEx.getMessage());
                        } else {
                            logger.error("Error closing page: {}", pageCloseEx.getMessage(), pageCloseEx);
                        }
                    }
                }

                // Close the context (this releases resources associated with the context).
                context.close();
            }
        } catch (Exception ex) {
            // TargetClosedError is expected when the browser was already closed — log as debug, not error.
            if (isTargetClosedError(ex)) {
                logger.debug("Browser context already closed (expected during teardown): {}", ex.getMessage());
            } else {
                logger.error("Error closing the browser context: {}", ex.getMessage(), ex);
            }
        } finally {
            // Remove page and context thread-local references regardless of errors above to avoid leaks.
            pageThreadLocal.remove();
            contextThreadLocal.remove();
        }

        try {
            Browser browser = (Browser)browserThreadLocal.get();
            if (browser != null) {
                browser.close();
                logger.info("Browser closed.");
            }
        } catch (Exception ex) {
            if (isTargetClosedError(ex)) {
                logger.debug("Browser already closed (expected during teardown): {}", ex.getMessage());
            } else {
                logger.error("Error closing the browser: {}", ex.getMessage(), ex);
            }
        } finally {
            // Ensure the browser thread-local reference is removed.
            browserThreadLocal.remove();
        }

        // Remove other thread-local state to fully reset the thread.
        pageCommonMethodsThreadLocal.remove();
        scenarioThreadLocal.remove();
        activeFeatureThreadLocal.remove();
        performanceScenarioThreadLocal.remove();
        mobileScenarioThreadLocal.remove();
        // NOTE: browserClosedIntentionallyThreadLocal is intentionally NOT cleared here.
        // It must remain set so that tearDown() can read it AFTER closeBrowserResources() returns.
        // The flag is cleared in setUp() at the start of the next scenario (after the @LastScenario
        // decision is made), and also in the intentional-close branch of setUp() after a fresh
        // browser is created. This prevents the flag from being cleared before tearDown reads it.
    }

    /**
     * Remove all tracking entries related to a feature key used by @LastScenario handling.
     *
     * @param featureKey the feature key to clean up
     */
    private static void clearFeatureTracking(String featureKey) {
        if (featureKey != null && !featureKey.trim().isEmpty()) {
            lastScenarioFeatureMap.remove(featureKey);
            featureScenarioTotalMap.remove(featureKey);
            featureScenarioExecutedMap.remove(featureKey);
            featureFailureMap.remove(featureKey);
        } else {
            logger.warn("Skipping feature tracking cleanup because feature key is null or empty.");
        }
    }

    /**
     * Clear only the thread-local state used for performance scenarios (no Playwright resources to close).
     */
    private void clearPerformanceScenarioStateOnly() {
        scenarioThreadLocal.remove();
        activeFeatureThreadLocal.remove();
        performanceScenarioThreadLocal.remove();
    }

    /**
     * Clear only the thread-local state used for mobile-native scenarios (no Playwright resources to close).
     */
    private void clearMobileScenarioStateOnly() {
        scenarioThreadLocal.remove();
        activeFeatureThreadLocal.remove();
        mobileScenarioThreadLocal.remove();
    }

    /**
     * Determine if a scenario is a mobile-native test by inspecting its source tags.
     *
     * @param scenario the scenario to inspect
     * @return true if the scenario is considered mobile-native
     */
    private boolean isMobileScenario(Scenario scenario) {
        if (scenario == null) {
            return false;
        } else {
            // Typical mobile tags: @mobile, @appium_browser, @cross_platform (case-insensitive)
            for(String tag : scenario.getSourceTagNames()) {
                if (tag != null && (tag.equalsIgnoreCase("@mobile") || tag.equalsIgnoreCase("@appium_browser") || tag.equalsIgnoreCase("@cross_platform"))) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Determine if a scenario is a performance scenario by checking tags and the feature key heuristically.
     *
     * @param scenario the scenario to inspect
     * @return true if the scenario is considered a performance test
     */
    private boolean isPerformanceScenario(Scenario scenario) {
        if (scenario == null) {
            return false;
        } else {
            for(String tag : scenario.getSourceTagNames()) {
                // If tag is in predefined performance set, treat as performance.
                if (PERFORMANCE_TAGS.contains(tag)) {
                    return true;
                }

                // Support any tag starting with @performance as a fallback.
                if (tag != null && tag.startsWith("@performance")) {
                    return true;
                }
            }

            // As an additional heuristic, if the feature file path contains "performance", consider it performance.
            String featureKey = this.getFeatureKey(scenario);
            return featureKey != null && featureKey.toLowerCase().contains("performance");
        }
    }

    /**
     * Determine if a scenario is a non-UI scenario that does not require a Playwright browser.
     *
     * <p>Non-UI scenarios include XML validation, CSV validation, API testing, database testing,
     * ZIP file processing, and PDF validation — any scenario that operates purely on data or
     * backend services without needing a browser window.</p>
     *
     * <p>Detection is based on scenario tags. If a scenario has ANY of the following tags it is
     * treated as non-UI and the browser is not started:</p>
     * <ul>
     *   <li>{@code @xml_validation}, {@code @xml_example}, {@code @xml} — XML automation scenarios</li>
     *   <li>{@code @csv_validation}, {@code @csv_example}, {@code @csv} — CSV automation scenarios</li>
     *   <li>{@code @api}, {@code @api_test} — API testing scenarios</li>
     *   <li>{@code @database}, {@code @db}, {@code @db_test} — Database testing scenarios</li>
     *   <li>{@code @zip}, {@code @zip_validation} — ZIP file processing scenarios</li>
     *   <li>{@code @pdf}, {@code @pdf_validation} — PDF validation scenarios</li>
     * </ul>
     *
     * <p>Additionally, if the feature file path contains keywords such as "xml", "csv", "api",
     * "database", "db", "zip", or "pdf" (and does NOT contain "ui" or "browser"),
     * it is also treated as non-UI.</p>
     *
     * <p><strong>Important:</strong> If a scenario mixes UI steps with XML/CSV steps (e.g., loading
     * XML from a UI element), it must NOT have any of the above non-UI tags. The browser will only
     * be skipped when the scenario is tagged exclusively as a non-UI scenario.</p>
     *
     * @param scenario the scenario to inspect
     * @return {@code true} if the scenario is a non-UI data-only scenario, {@code false} otherwise
     */
    private boolean isNonUiScenario(Scenario scenario) {
        if (scenario == null) {
            return false;
        }

        // Check scenario tags for known non-UI tags
        for (String tag : scenario.getSourceTagNames()) {
            if (tag == null) continue;
            String t = tag.toLowerCase();
            if (t.equals("@xml_validation") || t.equals("@xml_example") || t.equals("@xml")
                || t.equals("@csv_validation") || t.equals("@csv_example") || t.equals("@csv")
                || t.equals("@api") || t.equals("@api_test")
                || t.equals("@database") || t.equals("@db") || t.equals("@db_test")
                || t.equals("@zip") || t.equals("@zip_validation")
                || t.equals("@pdf") || t.equals("@pdf_validation")
                || t.startsWith("@xml_") || t.startsWith("@csv_")
                || t.startsWith("@api_") || t.startsWith("@db_")
                || t.startsWith("@database_") || t.startsWith("@zip_")
                || t.startsWith("@pdf_")) {
                return true;
            }
        }

        // Heuristic: if the feature file path contains non-UI keywords and no UI keywords, treat as non-UI
        String featureKey = this.getFeatureKey(scenario);
        if (featureKey != null) {
            String fk = featureKey.toLowerCase();
            boolean hasNonUiKeyword = fk.contains("/xml/") || fk.contains("/csv/")
                || fk.contains("/api/") || fk.contains("/database/")
                || fk.contains("/db/") || fk.contains("/zip/")
                || fk.contains("/pdf/");
            boolean hasUiKeyword = fk.contains("/ui/") || fk.contains("browser")
                || fk.contains("playwright");
            if (hasNonUiKeyword && !hasUiKeyword) {
                return true;
            }
        }

        return false;
    }

    /**
     * Safely retrieve the feature key for teardown. If the thread-local is missing, attempt to recover from scenario.
     *
     * @param scenario the current scenario
     * @return resolved feature key or null if unable to recover
     */
    private String getSafeFeatureKeyForTearDown(Scenario scenario) {
        String featureKey = (String)activeFeatureThreadLocal.get();
        if (featureKey != null && !featureKey.trim().isEmpty()) {
            return featureKey;
        } else {
            try {
                featureKey = this.getFeatureKey(scenario);
                if (featureKey != null && !featureKey.trim().isEmpty()) {
                    // Recover and log that we had to reconstruct the feature key during teardown.
                    activeFeatureThreadLocal.set(featureKey);
                    logger.warn("Feature key was missing from ThreadLocal during teardown. Recovered feature key [{}] from scenario [{}].", featureKey, scenario != null ? scenario.getName() : "UNKNOWN");
                    return featureKey;
                }
            } catch (Exception e) {
                logger.warn("Unable to recover feature key during teardown for scenario [{}]. Reason: {}", scenario != null ? scenario.getName() : "UNKNOWN", e.getMessage());
            }

            return null;
        }
    }

    /**
     * Attempt to extract a unique feature key for a scenario. Prefers scenario.getUri(); falls back to parsing scenario.getId().
     *
     * @param scenario the scenario to examine
     * @return a string representing the feature key (URI or id prefix) or null if not resolvable
     */
    private String getFeatureKey(Scenario scenario) {
        if (scenario == null) {
            return null;
        } else {
            try {
                URI uri = scenario.getUri();
                if (uri != null && uri.toString() != null && !uri.toString().trim().isEmpty()) {
                    return uri.toString();
                }
            } catch (Exception var5) {
                // ignore and try id-based fallback
            }

            try {
                String id = scenario.getId();
                if (id != null && !id.trim().isEmpty()) {
                    // The id may contain a colon and line number; strip trailing colon part to produce a stable key.
                    int colonIndex = id.lastIndexOf(58);
                    return colonIndex > 0 ? id.substring(0, colonIndex) : id;
                } else {
                    return null;
                }
            } catch (Exception var4) {
                return null;
            }
        }
    }

    /**
     * Build a deterministic fallback feature key when URI/ID cannot be determined.
     *
     * @param scenario the scenario used to build a friendly fallback key
     * @return constructed fallback feature key
     */
    private String buildFallbackFeatureKey(Scenario scenario) {
        // Use scenario name sanitized to be safe in logs and file-system-like contexts.
        String scenarioName = scenario != null && scenario.getName() != null ? scenario.getName().replaceAll("[^a-zA-Z0-9_-]", "_") : "UNKNOWN_SCENARIO";
        long var10000 = Thread.currentThread().getId();
        return "UNKNOWN_FEATURE_" + var10000 + "_" + scenarioName;
    }

    /**
     * Parse the feature file content referenced by the scenario URI and estimate the number of runnable scenarios.
     *
     * <p>
     * This method understands:
     * - "Scenario:" lines count as 1 scenario each.
     * - "Scenario Outline:" or "Scenario Template:" combined with "Examples:" will count one scenario per example row.
     * - Example table headers (first row) are skipped; blank lines, comments (#) and tag lines (@) are ignored.
     * - If parsing fails or yields zero, defaults to 1 so tests still run.
     * </p>
     *
     * @param scenario the scenario whose feature file to inspect
     * @return number of runnable scenarios discovered (minimum 1)
     */
    private int countScenariosInFeatureFile(Scenario scenario) {
        try {
            URI uri = scenario.getUri();
            if (uri == null) {
                logger.warn("Scenario URI is null. Defaulting scenario count to 1.");
                return 1;
            } else {
                // Open the feature file (supports file: and classpath: URIs).
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.openFeatureStream(uri)))) {
                    int total = 0;
                    boolean inScenarioOutline = false;
                    boolean inExamples = false;
                    boolean headerSkipped = false;
                    int outlineExampleRows = 0;

                    String line;
                    while((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        // Skip blank lines, comments and tag lines.
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("@")) {
                            // Start of a Scenario Outline or Scenario Template indicates entering outline mode.
                            if (!trimmed.startsWith("Scenario Outline:") && !trimmed.startsWith("Scenario Template:")) {
                                if (trimmed.startsWith("Scenario:")) {
                                    // If previously inside an outline, finalize counting of the outline before counting this standalone scenario.
                                    if (inScenarioOutline) {
                                        total += Math.max(outlineExampleRows, 1);
                                        inScenarioOutline = false;
                                        inExamples = false;
                                        headerSkipped = false;
                                        outlineExampleRows = 0;
                                    }

                                    ++total; // Standard scenario counts as 1
                                } else if (inScenarioOutline && trimmed.startsWith("Examples:")) {
                                    // Enter examples table section for the currently open outline.
                                    inExamples = true;
                                    headerSkipped = false;
                                } else if (inScenarioOutline && inExamples && trimmed.startsWith("|")) {
                                    // Lines that start with '|' inside Examples: are table rows. Skip the header line, count the rest.
                                    if (!headerSkipped) {
                                        headerSkipped = true;
                                    } else {
                                        ++outlineExampleRows;
                                    }
                                }
                            } else {
                                // Enter a scenario outline/template block. Finalize any previous outline counting first.
                                if (inScenarioOutline) {
                                    total += Math.max(outlineExampleRows, 1);
                                }

                                inScenarioOutline = true;
                                inExamples = false;
                                headerSkipped = false;
                                outlineExampleRows = 0;
                            }
                        }
                    }

                    // If we ended while inside an outline, account for the examples parsed so far.
                    if (inScenarioOutline) {
                        total += Math.max(outlineExampleRows, 1);
                    }

                    logger.info("Detected [{}] runnable scenarios in feature [{}]", total, uri);
                    return total > 0 ? total : 1;
                }
            }
        } catch (Exception e) {
            // Fail safe: if anything goes wrong, treat feature as having a single scenario.
            logger.warn("Unable to count scenarios in feature file. Defaulting scenario count to 1. Reason: {}", e.getMessage());
            return 1;
        }
    }

    /**
     * Open an InputStream for the feature file referenced by the provided URI.
     *
     * <p>
     * Supports:
     * - file: URIs: read directly from file system
     * - classpath: prefixed URIs: load via context class loader
     * - other URIs resolved via class loader resource path
     * </p>
     *
     * @param uri the feature file URI
     * @return InputStream that must be closed by caller
     * @throws Exception if the resource cannot be opened
     */
    private InputStream openFeatureStream(URI uri) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            // Local file - open directly with NIO
            return Files.newInputStream(Paths.get(uri));
        } else {
            // Normalize path for classpath resource loading
            String path = uri.toString();
            if (path.startsWith("classpath:")) {
                path = path.replace("classpath:", "");
            }

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            if (inputStream == null) {
                throw new IllegalStateException("Unable to load feature file from URI: " + String.valueOf(uri));
            } else {
                return inputStream;
            }
        }
    }

    /**
     * Get the active Playwright Page instance for the current thread.
     *
     * <p>Returns {@code null} instead of throwing when the page is closed or not initialized.
     * This is intentional for {@code @LastScenario} feature support: when a scenario in a
     * shared-browser feature fails, the page may be closed before subsequent scenarios run.
     * Step definitions that call this method must check for null before using the page.</p>
     *
     * <p>Previously this method threw {@code IllegalStateException} when the page was closed,
     * which caused all subsequent scenarios in a {@code @LastScenario} feature to fail with
     * a misleading "The page is closed or not initialized" error instead of the real failure.
     * Returning null allows the step definition to produce a cleaner, more informative error.</p>
     *
     * @return the active Page, or {@code null} if the page is closed or not initialized
     */
    public static Page getPage() {
        Page page = (Page)pageThreadLocal.get();
        if (page != null && !page.isClosed()) {
            return page;
        }
        // Return null instead of throwing — callers must handle null gracefully.
        // This prevents cascading IllegalStateException errors in @LastScenario features.
        return null;
    }

    /**
     * Get the active Playwright Browser instance for the current thread.
     *
     * @return the active Browser
     * @throws IllegalStateException if the browser is not initialized
     */
    public static Browser getBrowser() {
        Browser browser = (Browser)browserThreadLocal.get();
        if (browser == null) {
            throw new IllegalStateException("The browser is not initialized.");
        } else {
            return browser;
        }
    }

    /**
     * Accessor for the currently running Scenario stored in thread-local storage.
     *
     * @return current Cucumber Scenario or null if not set
     */
    public static Scenario getCurrentScenario() {
        return (Scenario)scenarioThreadLocal.get();
    }

    /**
     * Store the Scenario in thread-local storage. Useful for custom test helpers that need scenario context.
     *
     * @param scenario the scenario to set as current
     */
    public static void setCurrentScenario(Scenario scenario) {
        scenarioThreadLocal.set(scenario);
    }

    /**
     * Get the BrowserContext for the current thread.
     *
     * @return current BrowserContext
     * @throws IllegalStateException if context is not initialized
     */
    public static BrowserContext getContext() {
        BrowserContext context = (BrowserContext)contextThreadLocal.get();
        if (context == null) {
            throw new IllegalStateException("The browser context is not initialized.");
        } else {
            return context;
        }
    }

    /**
     * Mark that the browser was closed intentionally by a test step (not due to a failure).
     *
     * <p>Call this method from the "we close all browsers" step definition <em>before</em>
     * calling {@link #closeBrowserResources()}. This prevents the {@code @LastScenario}
     * teardown logic from treating the missing browser as a failure, and allows
     * {@link #setUp(Scenario)} to open a fresh browser for the next scenario.</p>
     */
    public static void markBrowserClosedIntentionally() {
        browserClosedIntentionallyThreadLocal.set(Boolean.TRUE);
        logger.info("Browser will be closed intentionally by test step — @LastScenario feature will NOT be marked as failed.");
    }

    /**
     * Check whether an exception is a Playwright TargetClosedError.
     *
     * <p>TargetClosedError is thrown by Playwright when an operation is attempted on a browser,
     * context, or page that has already been closed. This is expected during teardown after
     * soft assertion scenarios or @LastScenario failures where the browser may already be closed.
     * These errors should be logged at DEBUG level, not ERROR level, to avoid misleading reports.</p>
     *
     * @param ex the exception to check
     * @return {@code true} if the exception is a TargetClosedError or caused by one
     */
    private static boolean isTargetClosedError(Exception ex) {
        if (ex == null) return false;
        // Check the exception class name (avoids importing the Playwright internal class)
        String className = ex.getClass().getName();
        if (className.contains("TargetClosedError")) return true;
        // Check the message for the characteristic string
        String message = ex.getMessage();
        if (message != null && message.contains("Target page, context or browser has been closed")) return true;
        // Check the cause chain
        Throwable cause = ex.getCause();
        if (cause instanceof Exception) return isTargetClosedError((Exception) cause);
        return false;
    }
}
