package com.ptaf.hooks;

import com.google.gson.JsonObject;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.CDPSession;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Video;
import com.microsoft.playwright.options.LoadState;
import com.ptaf.ui.pages.PageCommonMethods;
import com.ptaf.utils.BrowserFactory;
import com.ptaf.utils.BrowserFactory.BrowserTypeEnum;
import com.ptaf.utils.ConfigurationProperties;
import com.ptaf.utils.FeatureArtifactNameResolver;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Hooks {

    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

    private static final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Scenario> scenarioThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<PageCommonMethods> pageCommonMethodsThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<String> activeFeatureThreadLocal = new ThreadLocal<>();
    /** Tracks scenarios that deliberately run without a Playwright browser stack. */
    private static final ThreadLocal<Boolean> browserlessScenarioThreadLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Feature has @LastScenario tag.
     */
    private static final Map<String, Boolean> lastScenarioFeatureMap = new ConcurrentHashMap<>();

    /**
     * Total runnable scenarios in feature.
     */
    private static final Map<String, Integer> featureScenarioTotalMap = new ConcurrentHashMap<>();

    /**
     * How many scenarios already completed @After.
     */
    private static final Map<String, AtomicInteger> featureScenarioExecutedMap = new ConcurrentHashMap<>();

    /**
     * Once true, all next scenarios in that feature must fail immediately.
     */
    private static final Map<String, Boolean> featureFailureMap = new ConcurrentHashMap<>();

    /**
     * Records an explicit close step separately from an unexpected browser failure so the next
     * @LastScenario scenario can create a fresh browser, context, and page.
     */
    private static final Map<String, Boolean> intentionalBrowserCloseFeatureMap = new ConcurrentHashMap<>();
    private static final String INTENTIONAL_CLOSE_PROPERTY_PREFIX = "com.ptaf.lastScenario.intentionalClose.";

    /**
     * Performance-related tags that should never initialize UI browser stack.
     */
    private static final Set<String> PERFORMANCE_TAGS = Set.of(
            "@performance_testing",
            "@performance_full_regression",
            "@performance_get",
            "@performance_post",
            "@performance_put",
            "@performance_delete",
            "@performance_profile",
            "@performance_inline_json",
            "@performance_yaml",
            "@performance_csv",
            "@performance_excel",
            "@performance_auth",
            "@performance_bearer",
            "@performance_basic_auth",
            "@performance_negative",
            "@performance_expected_failure"
    );

    /** Tags that start an Appium native-app or real mobile-browser session, not Playwright. */
    private static final Set<String> APPIUM_MOBILE_TAGS = Set.of(
            "@mobile",
            "@android",
            "@ios",
            "@cross_platform",
            "@appium_browser",
            "@mobile_browser_real"
    );

    /** Tags that identify file, database, or document scenarios that do not require a UI. */
    private static final Set<String> NON_UI_DATA_TAGS = Set.of(
            "@xml_file",
            "@csv_file",
            "@db",
            "@database",
            "@zip",
            "@pdf"
    );

    public Hooks() {
    }

    @Before
    public void setUp(Scenario scenario) {
        scenarioThreadLocal.set(scenario);

        String featureKey = getFeatureKey(scenario);

        if (featureKey == null || featureKey.trim().isEmpty()) {
            featureKey = buildFallbackFeatureKey(scenario);
            logger.warn(
                    "Unable to resolve feature key for scenario [{}]. Using fallback feature key [{}].",
                    scenario != null ? scenario.getName() : "UNKNOWN",
                    featureKey
            );
        }

        activeFeatureThreadLocal.set(featureKey);

        String browserlessScenarioType = getBrowserlessScenarioType(scenario);
        if (browserlessScenarioType != null) {
            browserlessScenarioThreadLocal.set(Boolean.TRUE);
            logger.info(
                    "Non-UI {} scenario detected [{}]. Skipping Playwright browser initialization.",
                    browserlessScenarioType,
                    scenario != null ? scenario.getName() : "UNKNOWN"
            );
            return;
        }

        browserlessScenarioThreadLocal.set(Boolean.FALSE);

        boolean isLastScenarioTaggedFeature =
                scenario != null && scenario.getSourceTagNames().contains("@LastScenario");

        lastScenarioFeatureMap.putIfAbsent(featureKey, isLastScenarioTaggedFeature);

        if (isLastScenarioTaggedFeature) {
            featureScenarioTotalMap.computeIfAbsent(featureKey, key -> countScenariosInFeatureFile(scenario));
            featureScenarioExecutedMap.computeIfAbsent(featureKey, key -> new AtomicInteger(0));
            featureFailureMap.putIfAbsent(featureKey, false);
        }

        Browser existingBrowser = browserThreadLocal.get();
        BrowserContext existingContext = contextThreadLocal.get();
        Page existingPage = pageThreadLocal.get();

        boolean browserAlive =
                existingBrowser != null &&
                        existingContext != null &&
                        existingPage != null &&
                        !existingPage.isClosed();

        if (isLastScenarioTaggedFeature) {
            boolean featureAlreadyFailed = Boolean.TRUE.equals(featureFailureMap.get(featureKey));
            boolean browserClosedIntentionally = isBrowserClosedIntentionally(featureKey);
            int alreadyExecuted = featureScenarioExecutedMap.containsKey(featureKey)
                    ? featureScenarioExecutedMap.get(featureKey).get()
                    : 0;

            if (featureAlreadyFailed && !browserClosedIntentionally) {
                logger.error(
                        "Skipping scenario [{}] because @LastScenario feature [{}] is already marked as failed.",
                        scenario != null ? scenario.getName() : "UNKNOWN",
                        featureKey
                );
                throw new RuntimeException(
                        "Previous scenario failed in @LastScenario feature. Remaining scenarios are failed intentionally."
                );
            }

            if (alreadyExecuted == 0 && !browserAlive) {
                createBrowserStack(scenario);
                logger.info(
                        "Initial shared browser created for @LastScenario feature [{}], scenario [{}]",
                        featureKey,
                        scenario != null ? scenario.getName() : "UNKNOWN"
                );
                return;
            }

            if (alreadyExecuted > 0) {
                if (browserAlive) {
                    logger.info(
                            "Reusing shared browser for @LastScenario feature [{}], scenario [{}]",
                            featureKey,
                            scenario != null ? scenario.getName() : "UNKNOWN"
                    );
                    return;
                } else if (browserClosedIntentionally) {
                    logger.info(
                            "Browser was closed intentionally in @LastScenario feature [{}]. Creating a fresh browser for scenario [{}].",
                            featureKey,
                            scenario != null ? scenario.getName() : "UNKNOWN"
                    );
                    featureFailureMap.put(featureKey, false);
                    createBrowserStack(scenario);
                    clearIntentionalBrowserCloseMarker(featureKey);
                    return;
                } else {
                    featureFailureMap.put(featureKey, true);
                    logger.error(
                            "Shared browser/session is no longer available for @LastScenario feature [{}] before scenario [{}]. " +
                                    "Failing remaining scenarios and not reopening browser.",
                            featureKey,
                            scenario != null ? scenario.getName() : "UNKNOWN"
                    );
                    throw new RuntimeException(
                            "Shared browser was closed or lost during @LastScenario execution. Remaining scenarios are failed intentionally."
                    );
                }
            }
        }

        createBrowserStack(scenario);
    }

    @After
    public void tearDown(Scenario scenario) {
        String featureKey = getSafeFeatureKeyForTearDown(scenario);

        try {
            if (!Boolean.TRUE.equals(browserlessScenarioThreadLocal.get()) && scenario.getStatus() == Status.PASSED) {
                PageCommonMethods pageCommonMethods = pageCommonMethodsThreadLocal.get();
                if (pageCommonMethods != null) {
                    pageCommonMethods.finalizeScenario();
                }
            }
        } catch (Exception e) {
            logger.error("Error during scenario teardown: {}", e.getMessage(), e);
        } finally {
            if (Boolean.TRUE.equals(browserlessScenarioThreadLocal.get())) {
                clearBrowserlessScenarioStateOnly();
                logger.info("Non-UI scenario teardown completed without UI browser cleanup requirement.");
                return;
            }

            boolean isLastScenarioFeature =
                    featureKey != null && Boolean.TRUE.equals(lastScenarioFeatureMap.get(featureKey));

            if (isLastScenarioFeature) {
                boolean browserClosedIntentionally = isBrowserClosedIntentionally(featureKey);
                Browser browser = browserThreadLocal.get();
                BrowserContext context = contextThreadLocal.get();
                Page page = pageThreadLocal.get();

                boolean browserAlive =
                        browser != null &&
                                context != null &&
                                page != null &&
                                !page.isClosed();

                if (scenario.getStatus() == Status.FAILED) {
                    featureFailureMap.put(featureKey, true);
                    logger.error(
                            "Scenario [{}] failed in @LastScenario feature [{}]. Remaining scenarios will fail immediately.",
                            scenario.getName(),
                            featureKey
                    );
                }

                if (!browserAlive && !browserClosedIntentionally) {
                    featureFailureMap.put(featureKey, true);
                    logger.error(
                            "Shared browser/session became unavailable in @LastScenario feature [{}] after scenario [{}]. " +
                                    "Remaining scenarios will fail immediately.",
                            featureKey,
                            scenario.getName()
                    );
                } else if (!browserAlive) {
                    logger.info(
                            "Browser was closed intentionally in @LastScenario feature [{}]. The next scenario will create a fresh browser stack.",
                            featureKey
                    );
                }

                AtomicInteger executedCounter = featureScenarioExecutedMap.get(featureKey);
                int executed = executedCounter != null ? executedCounter.incrementAndGet() : 1;
                int total = featureScenarioTotalMap.getOrDefault(featureKey, 1);

                logger.info("Feature [{}] progress: {}/{}", featureKey, executed, total);

                if (executed >= total) {
                    logger.info("Last scenario reached for feature [{}]. Closing browser resources.", featureKey);
                    closeBrowserResources();
                    clearFeatureTracking(featureKey);
                } else {
                    logger.info("Keeping browser state unchanged for next scenario in @LastScenario feature [{}].", featureKey);
                }
            } else {
                if (featureKey == null) {
                    logger.warn(
                            "Feature key was not available during teardown for scenario [{}]. " +
                                    "Proceeding with normal browser cleanup to avoid NullPointerException.",
                            scenario != null ? scenario.getName() : "UNKNOWN"
                    );
                }

                closeBrowserResources();
            }
        }
    }

    private void createBrowserStack(Scenario scenario) {
        try {
            String browserName = ConfigurationProperties.getBrowser();
            BrowserTypeEnum browserTypeEnum;

            switch (browserName.toUpperCase()) {
                case "CHROME":
                    browserTypeEnum = BrowserTypeEnum.CHROME;
                    break;
                case "FIREFOX":
                    browserTypeEnum = BrowserTypeEnum.FIREFOX;
                    break;
                case "WEBKIT":
                    browserTypeEnum = BrowserTypeEnum.WEBKIT;
                    break;
                case "EDGE":
                    browserTypeEnum = BrowserTypeEnum.EDGE;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported browser type: " + browserName);
            }

            Browser browser = BrowserFactory.createBrowser(browserTypeEnum);
            browserThreadLocal.set(browser);

            BrowserContext context = BrowserFactory.createContextWithVideo(browser);
            contextThreadLocal.set(context);

            Page page = context.newPage();
            pageThreadLocal.set(page);

            long runtimeTimeoutMillis = getConfiguredRuntimeTimeoutMillis();

            page.setDefaultTimeout(runtimeTimeoutMillis);
            page.setDefaultNavigationTimeout(runtimeTimeoutMillis);

            PageCommonMethods pageCommonMethods = new PageCommonMethods(page);
            pageCommonMethodsThreadLocal.set(pageCommonMethods);

            logger.info(
                    "Browser setup completed for scenario: {} with runtime timeout: {} ms",
                    scenario != null ? scenario.getName() : "UNKNOWN",
                    runtimeTimeoutMillis
            );

        } catch (Exception e) {
            logger.error(
                    "Error setting up the browser for scenario: {}",
                    scenario != null ? scenario.getName() : "UNKNOWN",
                    e
            );
            throw new RuntimeException("Browser setup failed", e);
        }
    }

    /**
     * Reads runtimeWait from YAML/config.
     * Value is treated as direct seconds.
     *
     * Example:
     * runtimeWait = 5 -> 5000 ms
     */
    private static long getConfiguredRuntimeTimeoutMillis() {
        try {
            String runtimeValue = ConfigurationProperties.getValue("runtimeWait");

            if (runtimeValue == null || runtimeValue.trim().isEmpty()) {
                logger.warn("runtimeWait is not configured. Defaulting to 30 seconds.");
                return 30000L;
            }

            long seconds = Long.parseLong(runtimeValue.trim());

            if (seconds <= 0) {
                logger.warn("runtimeWait must be greater than 0. Defaulting to 30 seconds.");
                return 30000L;
            }

            long timeoutMillis = seconds * 1000L;
            logger.info("Configured runtimeWait: {} second(s) = {} ms", seconds, timeoutMillis);
            return timeoutMillis;

        } catch (Exception e) {
            logger.warn(
                    "Unable to parse runtimeWait from configuration. Defaulting to 30 seconds. Reason: {}",
                    e.getMessage()
            );
            return 30000L;
        }
    }

    /**
     * Waits until current page is loaded.
     * It waits only up to configured timeout.
     * If page loads earlier, execution continues immediately.
     */
    public static void waitForCurrentPageToLoad() {
        Page page = getPage();
        long timeout = getConfiguredRuntimeTimeoutMillis();

        try {
            page.waitForLoadState(
                    LoadState.DOMCONTENTLOADED,
                    new Page.WaitForLoadStateOptions().setTimeout(timeout)
            );

            page.waitForLoadState(
                    LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(timeout)
            );

            logger.info("Page reached load state successfully within {} ms", timeout);
        } catch (Exception e) {
            logger.error("Page did not load within configured timeout: {} ms", timeout, e);
            throw new RuntimeException("Page did not load within configured timeout: " + timeout + " ms", e);
        }
    }

    /**
     * Sets the active page for the current thread.
     * This is important when popup/new tab page is opened and must become the new working page.
     */
    public static void setPage(Page page) {
        if (page == null || page.isClosed()) {
            throw new IllegalArgumentException("The page is null or closed.");
        }

        pageThreadLocal.set(page);

        try {
            long runtimeTimeoutMillis = getConfiguredRuntimeTimeoutMillis();
            page.setDefaultTimeout(runtimeTimeoutMillis);
            page.setDefaultNavigationTimeout(runtimeTimeoutMillis);
        } catch (Exception e) {
            logger.warn("Unable to apply timeout settings to switched page. Reason: {}", e.getMessage());
        }

        pageCommonMethodsThreadLocal.set(new PageCommonMethods(page));
        logger.info("Active page has been switched successfully.");
    }

    public static void closeBrowserResources() {
        // This public method is the framework's explicit browser-cleanup API. When a test calls
        // it while an @LastScenario feature is active, the close is deliberate even if the
        // consuming project does not invoke PageCommonSteps' marker method first. Recording the
        // intent here guarantees that the next scenario is allowed to create a new page.
        String activeFeatureKey = activeFeatureThreadLocal.get();
        if (activeFeatureKey != null && Boolean.TRUE.equals(lastScenarioFeatureMap.get(activeFeatureKey))) {
            markBrowserClosedIntentionally();
        }

        // Video paths become available only once their pages/context are closed. Capture the
        // Video handles before cleanup, then rename the finalized .webm files afterwards.
        Scenario artifactScenario = scenarioThreadLocal.get();
        List<Video> recordedVideos = new ArrayList<>();
        try {
            BrowserContext context = contextThreadLocal.get();

            if (context != null) {
                for (Page page : context.pages()) {
                    try {
                        rememberRecordedVideo(page, recordedVideos);
                        if (page != null && !page.isClosed()) {
                            page.close();
                        }
                    } catch (Exception pageCloseEx) {
                        logger.error("Error closing page: {}", pageCloseEx.getMessage(), pageCloseEx);
                    }
                }

                context.close();
            }
        } catch (Exception ex) {
            logger.error("Error closing the browser context: {}", ex.getMessage(), ex);
        } finally {
            pageThreadLocal.remove();
            contextThreadLocal.remove();
        }

        renameRecordedVideos(recordedVideos, artifactScenario);

        try {
            Browser browser = browserThreadLocal.get();
            if (browser != null) {
                browser.close();
                logger.info("Browser closed.");
            }
        } catch (Exception ex) {
            logger.error("Error closing the browser: {}", ex.getMessage(), ex);
        } finally {
            browserThreadLocal.remove();
        }

        pageCommonMethodsThreadLocal.remove();
        scenarioThreadLocal.remove();
        activeFeatureThreadLocal.remove();
        browserlessScenarioThreadLocal.remove();
    }

    /**
     * Stores a Playwright video handle before a page closes. A video handle is null when browser
     * recording is disabled, so this method is a no-op for normal non-recording runs.
     */
    private static void rememberRecordedVideo(Page page, List<Video> recordedVideos) {
        if (page == null || recordedVideos == null) {
            return;
        }
        try {
            Video video = page.video();
            if (video != null && !recordedVideos.contains(video)) {
                recordedVideos.add(video);
            }
        } catch (Exception exception) {
            logger.debug("Unable to obtain Playwright video handle before page close: {}", exception.getMessage());
        }
    }

    /**
     * Renames finalized Playwright recordings from Playwright's anonymous .webm name to the
     * declared Feature title plus timestamp. Failure to rename evidence is non-fatal and does
     * not affect browser teardown or scenario status.
     */
    private static void renameRecordedVideos(List<Video> recordedVideos, Scenario scenario) {
        if (recordedVideos == null || recordedVideos.isEmpty()) {
            return;
        }

        for (Video video : recordedVideos) {
            try {
                Path source = video.path();
                if (source == null || !Files.exists(source)) {
                    logger.debug("Recorded Playwright video file was not available for renaming.");
                    continue;
                }
                Path featureVideoDirectory = FeatureArtifactNameResolver.createFeatureDirectory(
                        source.getParent(), scenario);
                Path target = FeatureArtifactNameResolver.buildArtifactPath(
                        featureVideoDirectory, scenario, source.getFileName().toString());
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Playwright video renamed using Feature title: {}", target.toAbsolutePath());
            } catch (Exception exception) {
                logger.warn("Unable to rename Playwright video using Feature title: {}", exception.getMessage());
            }
        }
    }

    private static void clearFeatureTracking(String featureKey) {
        if (featureKey == null || featureKey.trim().isEmpty()) {
            logger.warn("Skipping feature tracking cleanup because feature key is null or empty.");
            return;
        }

        lastScenarioFeatureMap.remove(featureKey);
        featureScenarioTotalMap.remove(featureKey);
        featureScenarioExecutedMap.remove(featureKey);
        featureFailureMap.remove(featureKey);
        clearIntentionalBrowserCloseMarker(featureKey);
    }

    /** Marks the current @LastScenario feature's close operation as deliberate. */
    public static void markBrowserClosedIntentionally() {
        String featureKey = activeFeatureThreadLocal.get();
        if (featureKey == null || featureKey.trim().isEmpty()) {
            logger.warn("Unable to mark browser close as intentional because no active feature key is available.");
            return;
        }
        intentionalBrowserCloseFeatureMap.put(featureKey, Boolean.TRUE);
        System.setProperty(intentionalClosePropertyName(featureKey), Boolean.TRUE.toString());
        logger.info("Browser close was marked as intentional for @LastScenario feature [{}].", featureKey);
    }

    /** Returns whether the current feature explicitly requested browser cleanup. */
    private static boolean isBrowserClosedIntentionally(String featureKey) {
        return featureKey != null
                && (Boolean.TRUE.equals(intentionalBrowserCloseFeatureMap.get(featureKey))
                || Boolean.parseBoolean(System.getProperty(intentionalClosePropertyName(featureKey), Boolean.FALSE.toString())));
    }

    /**
     * Builds the same marker name for a feature whether the consumer reports it as a file URI,
     * classpath URI, or a dependency-resource URI.
     */
    private static String intentionalClosePropertyName(String featureKey) {
        String normalized = featureKey.replace('\\', '/').toLowerCase(Locale.ROOT);
        int lastSeparator = normalized.lastIndexOf('/');
        String featureName = lastSeparator >= 0 ? normalized.substring(lastSeparator + 1) : normalized;
        return INTENTIONAL_CLOSE_PROPERTY_PREFIX + featureName;
    }

    /** Clears local and JVM-visible close intent after it is consumed or the feature ends. */
    private static void clearIntentionalBrowserCloseMarker(String featureKey) {
        if (featureKey == null || featureKey.trim().isEmpty()) {
            return;
        }
        intentionalBrowserCloseFeatureMap.remove(featureKey);
        System.clearProperty(intentionalClosePropertyName(featureKey));
    }

    private void clearBrowserlessScenarioStateOnly() {
        scenarioThreadLocal.remove();
        activeFeatureThreadLocal.remove();
        browserlessScenarioThreadLocal.remove();
    }

    /**
     * Determines whether a scenario must run without Playwright and returns a concise reason for
     * lifecycle logging. Playwright mobile-browser emulation ({@code @mobile_browser}) is
     * intentionally excluded because it is a UI test and still requires a browser.
     *
     * @param scenario active Cucumber scenario
     * @return non-UI scenario type, or {@code null} when Playwright is required
     */
    private String getBrowserlessScenarioType(Scenario scenario) {
        if (isPerformanceScenario(scenario)) {
            return "performance";
        }
        if (isApiScenario(scenario)) {
            return "API";
        }
        if (isAppiumMobileScenario(scenario)) {
            return "Appium mobile";
        }
        if (isFileOrDatabaseScenario(scenario)) {
            return "file/database";
        }
        return null;
    }

    private boolean isPerformanceScenario(Scenario scenario) {
        if (scenario == null) {
            return false;
        }

        for (String tag : scenario.getSourceTagNames()) {
            if (PERFORMANCE_TAGS.contains(tag)) {
                return true;
            }

            if (tag != null && tag.startsWith("@performance")) {
                return true;
            }
        }

        String featureKey = getFeatureKey(scenario);
        return featureKey != null && featureKey.toLowerCase().contains("performance");
    }

    /**
     * Identifies API-only scenarios before browser initialization. API scenarios are detected
     * explicitly by {@code @api}, {@code @api_*}, or {@code @api-*} tags. To preserve support
     * for existing API features without a tag, feature files located under an {@code api}
     * directory or whose file name contains {@code api} are also treated as API-only.
     *
     * <p>This method only controls Playwright setup/teardown. It does not alter API step
     * definitions, API requests, reporting, or any UI/mobile scenario behavior.</p>
     *
     * @param scenario active Cucumber scenario
     * @return {@code true} when the scenario does not require a Playwright browser
     */
    private boolean isApiScenario(Scenario scenario) {
        if (scenario == null) {
            return false;
        }

        for (String tag : scenario.getSourceTagNames()) {
            if (tag == null) {
                continue;
            }
            String normalizedTag = tag.trim().toLowerCase(Locale.ROOT);
            if ("@api".equals(normalizedTag)
                    || normalizedTag.startsWith("@api_")
                    || normalizedTag.startsWith("@api-")) {
                return true;
            }
        }

        String featureKey = getFeatureKey(scenario);
        if (featureKey == null || featureKey.trim().isEmpty()) {
            return false;
        }

        String normalizedPath = featureKey.toLowerCase(Locale.ROOT).replace('\\', '/');
        int lastSlash = normalizedPath.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? normalizedPath.substring(lastSlash + 1) : normalizedPath;
        return normalizedPath.contains("/api/") || fileName.contains("api");
    }

    /**
     * Detects native mobile and real mobile-browser scenarios, both of which use Appium through
     * {@link MobileHooks} and must never start a second Playwright desktop browser.
     */
    private boolean isAppiumMobileScenario(Scenario scenario) {
        if (scenario == null) {
            return false;
        }

        for (String tag : scenario.getSourceTagNames()) {
            if (tag != null && APPIUM_MOBILE_TAGS.contains(tag.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        String featureKey = getFeatureKey(scenario);
        if (featureKey == null) {
            return false;
        }
        String normalizedPath = featureKey.toLowerCase(Locale.ROOT).replace('\\', '/');
        return normalizedPath.contains("/features/mobile/");
    }

    /**
     * Detects explicitly file-only XML/CSV work and database, ZIP, and PDF scenarios. UI-embedded
     * XML/CSV scenarios are deliberately excluded so their existing browser automation remains
     * unchanged.
     */
    private boolean isFileOrDatabaseScenario(Scenario scenario) {
        if (scenario == null) {
            return false;
        }

        java.util.Collection<String> tags = scenario.getSourceTagNames();
        boolean uiEmbeddedFileScenario = tags.stream()
                .filter(tag -> tag != null)
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .anyMatch(tag -> "@xml_ui".equals(tag) || "@csv_ui".equals(tag));
        if (uiEmbeddedFileScenario) {
            return false;
        }

        for (String tag : tags) {
            if (tag != null && NON_UI_DATA_TAGS.contains(tag.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        String featureKey = getFeatureKey(scenario);
        if (featureKey == null) {
            return false;
        }
        String normalizedPath = featureKey.toLowerCase(Locale.ROOT).replace('\\', '/');
        return normalizedPath.contains("/features/db/")
                || normalizedPath.contains("/features/pdf/")
                || normalizedPath.contains("/features/zip/");
    }

    private String getSafeFeatureKeyForTearDown(Scenario scenario) {
        String featureKey = activeFeatureThreadLocal.get();

        if (featureKey != null && !featureKey.trim().isEmpty()) {
            return featureKey;
        }

        try {
            featureKey = getFeatureKey(scenario);

            if (featureKey != null && !featureKey.trim().isEmpty()) {
                activeFeatureThreadLocal.set(featureKey);
                logger.warn(
                        "Feature key was missing from ThreadLocal during teardown. Recovered feature key [{}] from scenario [{}].",
                        featureKey,
                        scenario != null ? scenario.getName() : "UNKNOWN"
                );
                return featureKey;
            }
        } catch (Exception e) {
            logger.warn(
                    "Unable to recover feature key during teardown for scenario [{}]. Reason: {}",
                    scenario != null ? scenario.getName() : "UNKNOWN",
                    e.getMessage()
            );
        }

        return null;
    }

    private String getFeatureKey(Scenario scenario) {
        if (scenario == null) {
            return null;
        }

        try {
            URI uri = scenario.getUri();
            if (uri != null && uri.toString() != null && !uri.toString().trim().isEmpty()) {
                return uri.toString();
            }
        } catch (Exception ignored) {
        }

        try {
            String id = scenario.getId();

            if (id == null || id.trim().isEmpty()) {
                return null;
            }

            int colonIndex = id.lastIndexOf(':');
            return colonIndex > 0 ? id.substring(0, colonIndex) : id;
        } catch (Exception ignored) {
        }

        return null;
    }

    private String buildFallbackFeatureKey(Scenario scenario) {
        String scenarioName = scenario != null && scenario.getName() != null
                ? scenario.getName().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "UNKNOWN_SCENARIO";

        return "UNKNOWN_FEATURE_" + Thread.currentThread().getId() + "_" + scenarioName;
    }

    private int countScenariosInFeatureFile(Scenario scenario) {
        try {
            URI uri = scenario.getUri();
            if (uri == null) {
                logger.warn("Scenario URI is null. Defaulting scenario count to 1.");
                return 1;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFeatureStream(uri)))) {
                String line;
                int total = 0;

                boolean inScenarioOutline = false;
                boolean inExamples = false;
                boolean headerSkipped = false;
                int outlineExampleRows = 0;

                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();

                    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("@")) {
                        continue;
                    }

                    if (trimmed.startsWith("Scenario Outline:") || trimmed.startsWith("Scenario Template:")) {
                        if (inScenarioOutline) {
                            total += Math.max(outlineExampleRows, 1);
                        }
                        inScenarioOutline = true;
                        inExamples = false;
                        headerSkipped = false;
                        outlineExampleRows = 0;
                        continue;
                    }

                    if (trimmed.startsWith("Scenario:")) {
                        if (inScenarioOutline) {
                            total += Math.max(outlineExampleRows, 1);
                            inScenarioOutline = false;
                            inExamples = false;
                            headerSkipped = false;
                            outlineExampleRows = 0;
                        }
                        total++;
                        continue;
                    }

                    if (inScenarioOutline && trimmed.startsWith("Examples:")) {
                        inExamples = true;
                        headerSkipped = false;
                        continue;
                    }

                    if (inScenarioOutline && inExamples && trimmed.startsWith("|")) {
                        if (!headerSkipped) {
                            headerSkipped = true;
                        } else {
                            outlineExampleRows++;
                        }
                    }
                }

                if (inScenarioOutline) {
                    total += Math.max(outlineExampleRows, 1);
                }

                logger.info("Detected [{}] runnable scenarios in feature [{}]", total, uri);
                return total > 0 ? total : 1;
            }
        } catch (Exception e) {
            logger.warn(
                    "Unable to count scenarios in feature file. Defaulting scenario count to 1. Reason: {}",
                    e.getMessage()
            );
            return 1;
        }
    }

    private InputStream openFeatureStream(URI uri) throws Exception {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Files.newInputStream(Paths.get(uri));
        }

        String path = uri.toString();

        if (path.startsWith("classpath:")) {
            path = path.replace("classpath:", "");
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(path);

        if (inputStream == null) {
            throw new IllegalStateException("Unable to load feature file from URI: " + uri);
        }

        return inputStream;
    }

    /**
     * Retains compatibility with the framework's explicit maximize action without changing
     * browser lifecycle behavior. Unsupported browsers simply return false.
     */
    public static boolean maximizeBrowserWindow(Page page) {
        if (page == null || page.isClosed()) {
            return false;
        }
        CDPSession cdpSession = null;
        try {
            cdpSession = page.context().newCDPSession(page);
            JsonObject windowInfo = cdpSession.send("Browser.getWindowForTarget");
            if (!windowInfo.has("windowId")) {
                return false;
            }
            JsonObject bounds = new JsonObject();
            bounds.addProperty("windowState", "maximized");
            JsonObject parameters = new JsonObject();
            parameters.addProperty("windowId", windowInfo.get("windowId").getAsInt());
            parameters.add("bounds", bounds);
            cdpSession.send("Browser.setWindowBounds", parameters);
            return true;
        } catch (Exception exception) {
            logger.debug("Native browser maximization is unavailable: {}", exception.getMessage());
            return false;
        } finally {
            if (cdpSession != null) {
                try {
                    cdpSession.detach();
                } catch (Exception ignored) {
                    // The browser can close while a window command is in progress.
                }
            }
        }
    }

    public static Page getPage() {
        Page page = pageThreadLocal.get();
        if (page != null && !page.isClosed()) {
            return page;
        }
        throw new IllegalStateException("The page is closed or not initialized.");
    }

    public static Browser getBrowser() {
        Browser browser = browserThreadLocal.get();
        if (browser == null) {
            throw new IllegalStateException("The browser is not initialized.");
        }
        return browser;
    }

    public static Scenario getCurrentScenario() {
        return scenarioThreadLocal.get();
    }

    public static void setCurrentScenario(Scenario scenario) {
        scenarioThreadLocal.set(scenario);
    }

    public static BrowserContext getContext() {
        BrowserContext context = contextThreadLocal.get();
        if (context == null) {
            throw new IllegalStateException("The browser context is not initialized.");
        }
        return context;
    }
}
