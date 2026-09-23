package com.ptaf.hooks;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
import com.ptaf.softassert.SoftAssertionContext;
import com.ptaf.utils.ConfigurationProperties;
import io.appium.java_client.AppiumDriver;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Mobile-specific Appium lifecycle hooks.
 *
 * <p>This class is intentionally separate from the existing Playwright Hooks class so
 * native mobile execution remains opt-in and backward-compatible.</p>
 *
 * <p>Platform resolution priority:</p>
 * <ol>
 *     <li>Command-line override: {@code -Dmobile.platform=android} or {@code -Dmobile.platform=ios}</li>
 *     <li>Explicit scenario tag: {@code @android} or {@code @ios}</li>
 *     <li>YAML default: {@code mobile.default_platform} in {@code mobile-config.yml}</li>
 * </ol>
 *
 * <p>For one shared cross-platform feature file, use {@code @mobile} or {@code @cross_platform}
 * without {@code @android} or {@code @ios}. Then switch platform only by changing
 * {@code mobile.default_platform} in YAML or by using {@code -Dmobile.platform}.</p>
 */
public class MobileHooks {
    // SLF4J logger for lifecycle events and debugging output related to mobile test execution.
    private static final Logger logger = LoggerFactory.getLogger(MobileHooks.class);

    // The set of Cucumber tags that indicate a scenario should be executed using mobile/Appium.
    // Test writers can tag scenarios with any of these to opt-in to mobile execution.
    private static final Set<String> MOBILE_TAGS = Set.of("@mobile", "@android", "@ios", "@cross_platform", "@appium_browser", "@mobile_browser_real");

    /**
     * Cucumber @Before hook that prepares mobile/Appium resources for scenarios that are marked
     * as mobile tests.
     *
     * <p>Behavior overview:
     * - If the scenario is not tagged for mobile execution, this hook returns immediately.
     * - Records the current scenario in the MobileEvidenceManager for later evidence capture.
     * - Resolves the target mobile platform (Android or iOS) using the resolution priority
     *   described in the class documentation.
     * - Determines whether the scenario should use an Appium-driven browser vs a native app.
     * - Starts the appropriate Appium driver and initiates video recording if enabled.</p>
     *
     * @param scenario the current Cucumber scenario; may be null in some Cucumber runtimes, so checks are defensive
     */
    @Before
    public void setUpMobile(Scenario scenario) {
        // If the scenario is not a mobile scenario, do nothing and let other hooks handle it.
        if (!isMobileScenario(scenario)) return;

        // Save the scenario reference centrally so evidence capture utilities can access details (name, tags).
        MobileEvidenceManager.setCurrentScenario(scenario);

        // Choose which mobile platform to use (Android or iOS).
        MobilePlatform platform = resolvePlatform(scenario);

        // Determine if the scenario should run in an Appium-managed browser context instead of a native app.
        boolean browserScenario = isAppiumBrowserScenario(scenario);

        // Informational log to help testers and CI operators know what's being started.
        logger.info("Starting mobile scenario [{}] on platform [{}] using [{}] mode", scenario.getName(), platform, browserScenario ? "Appium browser" : "native app");

        // Start the appropriate Appium driver based on whether this is a browser or native app scenario.
        AppiumDriver driver = browserScenario ? MobileDriverManager.startBrowserDriver(platform) : MobileDriverManager.startDriver(platform);

        // Optionally begin video recording for the scenario if configured to do so.
        MobileEvidenceManager.startVideoIfEnabled(driver);
    }

    /**
     * Cucumber @After hook that tears down mobile/Appium resources after mobile-marked scenarios.
     *
     * <p>Behavior overview:
     * - If the scenario is not a mobile scenario, this hook returns immediately.
     * - If a driver is available, captures a screenshot (if configured) and stops video recording.
     * - Ensures the driver is always closed and any stored scenario reference is cleared to avoid leaks
     *   even if evidence capture throws an exception.</p>
     *
     * @param scenario the current Cucumber scenario; may be null so checks are defensive
     */
    @After
    public void tearDownMobile(Scenario scenario) {
        // If the scenario is not a mobile scenario, nothing to tear down for Appium.
        if (!isMobileScenario(scenario)) return;

        // ── Soft Assertion Flush (Mobile) ──────────────────────────────────────────────────
        // When soft_assertions.enabled: true, fail the scenario if any mobile steps failed softly.
        // When soft_assertions.enabled: false (default), this block is a no-op.
        if (ConfigurationProperties.isSoftAssertionsEnabled() && SoftAssertionContext.hasFailed()) {
            String summary = SoftAssertionContext.buildSummary();
            SoftAssertionContext.clear();
            if (scenario != null) {
                scenario.log(summary);
            }
            throw new AssertionError(summary);
        }
        SoftAssertionContext.clear();
        // ─────────────────────────────────────────────────────────────────────────────

        try {
            // Only attempt evidence capture if a driver is present (i.e., we started one in setup).
            if (MobileDriverManager.hasDriver()) {
                AppiumDriver driver = MobileDriverManager.getDriver();

                // Capture a final screenshot if the configuration requires it.
                MobileEvidenceManager.captureScenarioScreenshotIfConfigured(driver, scenario);

                // Stop video recording if it was started for this scenario.
                MobileEvidenceManager.stopVideoIfEnabled(driver, scenario);
            }
        } finally {
            // Always ensure driver is closed and the scenario reference is cleared to avoid resource leaks.
            MobileDriverManager.closeDriver();
            MobileEvidenceManager.clearCurrentScenario();
        }
    }

    /**
     * Determines whether a given scenario is intended to run as a mobile/Appium test.
     *
     * @param scenario the scenario to check; may be null
     * @return true if the scenario contains any of the mobile tags defined in MOBILE_TAGS
     */
    private boolean isMobileScenario(Scenario scenario) {
        // Defensive: ensure scenario is not null, then check tag set intersection with MOBILE_TAGS.
        return scenario != null && scenario.getSourceTagNames().stream().anyMatch(MOBILE_TAGS::contains);
    }

    /**
     * Determines whether the scenario should run in an Appium browser context (web test) rather
     * than a native app context.
     *
     * <p>Decision logic:
     * - If the scenario is null, return false.
     * - If the scenario contains either @appium_browser or @mobile_browser_real, treat it as a browser scenario.
     * - Otherwise, respect a global "browser mode enabled" configuration only when the test has
     *   @mobile_browser_real tag (this allows toggling real mobile browser behavior via config).</p>
     *
     * @param scenario the scenario to evaluate; may be null
     * @return true if the scenario should run as a browser scenario under Appium
     */
    private boolean isAppiumBrowserScenario(Scenario scenario) {
        if (scenario == null) return false;

        // Explicit browser tags take precedence.
        if (scenario.getSourceTagNames().contains("@appium_browser") || scenario.getSourceTagNames().contains("@mobile_browser_real")) return true;

        // If a global configuration enables browser mode, allow @mobile_browser_real to trigger browser behavior.
        return MobileConfigurationProperties.isBrowserModeEnabled() && scenario.getSourceTagNames().contains("@mobile_browser_real");
    }

    /**
     * Resolve which MobilePlatform (ANDROID or IOS) to use for the scenario.
     *
     * <p>Resolution priority (highest to lowest):
     * 1) Command-line system property: -Dmobile.platform=android|ios
     * 2) Scenario tags: @android or @ios (explicit tag on the scenario)
     * 3) YAML configuration default: mobile.default_platform in mobile-config.yml</p>
     *
     * <p>Note: If both @android and @ios are present on the same scenario, an exception is thrown to
     * prevent ambiguous execution.</p>
     *
     * @param scenario the current Cucumber scenario; may be null
     * @return the resolved MobilePlatform
     * @throws IllegalArgumentException if both @android and @ios tags are present on the same scenario
     */
    private MobilePlatform resolvePlatform(Scenario scenario) {
        // 1) Check for a command-line override first.
        String commandLinePlatform = System.getProperty("mobile.platform");
        if (commandLinePlatform != null && !commandLinePlatform.trim().isEmpty()) {
            MobilePlatform platform = MobilePlatform.from(commandLinePlatform);
            logger.info("Resolved native mobile platform from command-line override [-Dmobile.platform={}]: {}", commandLinePlatform, platform);
            return platform;
        }

        // 2) Next, check explicit scenario tags.
        boolean hasAndroidTag = scenario != null && scenario.getSourceTagNames().contains("@android");
        boolean hasIosTag = scenario != null && scenario.getSourceTagNames().contains("@ios");

        // Guard against ambiguous configuration in the feature file.
        if (hasAndroidTag && hasIosTag) {
            throw new IllegalArgumentException("Scenario cannot contain both @android and @ios tags. Use one platform tag, or remove both and use mobile.default_platform.");
        }
        if (hasAndroidTag) {
            logger.info("Resolved native mobile platform from @android tag.");
            return MobilePlatform.ANDROID;
        }
        if (hasIosTag) {
            logger.info("Resolved native mobile platform from @ios tag.");
            return MobilePlatform.IOS;
        }

        // 3) Fall back to the YAML-configured default platform.
        MobilePlatform configuredDefault = MobileConfigurationProperties.getDefaultPlatform();
        logger.info("Resolved native mobile platform from mobile-config.yml mobile.default_platform: {}", configuredDefault);
        return configuredDefault;
    }
}
