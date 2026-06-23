package com.ptaf.hooks;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
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
    private static final Logger logger = LoggerFactory.getLogger(MobileHooks.class);
    private static final Set<String> MOBILE_TAGS = Set.of("@mobile", "@android", "@ios", "@cross_platform", "@appium_browser", "@mobile_browser_real");

    @Before
    public void setUpMobile(Scenario scenario) {
        if (!isMobileScenario(scenario)) return;
        MobileEvidenceManager.setCurrentScenario(scenario);
        MobilePlatform platform = resolvePlatform(scenario);
        boolean browserScenario = isAppiumBrowserScenario(scenario);
        logger.info("Starting mobile scenario [{}] on platform [{}] using [{}] mode", scenario.getName(), platform, browserScenario ? "Appium browser" : "native app");
        AppiumDriver driver = browserScenario ? MobileDriverManager.startBrowserDriver(platform) : MobileDriverManager.startDriver(platform);
        MobileEvidenceManager.startVideoIfEnabled(driver);
    }

    @After
    public void tearDownMobile(Scenario scenario) {
        if (!isMobileScenario(scenario)) return;
        try {
            if (MobileDriverManager.hasDriver()) {
                AppiumDriver driver = MobileDriverManager.getDriver();
                MobileEvidenceManager.captureScenarioScreenshotIfConfigured(driver, scenario);
                MobileEvidenceManager.stopVideoIfEnabled(driver, scenario);
            }
        } finally {
            MobileDriverManager.closeDriver();
            MobileEvidenceManager.clearCurrentScenario();
        }
    }

    private boolean isMobileScenario(Scenario scenario) {
        return scenario != null && scenario.getSourceTagNames().stream().anyMatch(MOBILE_TAGS::contains);
    }

    private boolean isAppiumBrowserScenario(Scenario scenario) {
        if (scenario == null) return false;
        if (scenario.getSourceTagNames().contains("@appium_browser") || scenario.getSourceTagNames().contains("@mobile_browser_real")) return true;
        return MobileConfigurationProperties.isBrowserModeEnabled() && scenario.getSourceTagNames().contains("@mobile_browser_real");
    }

    private MobilePlatform resolvePlatform(Scenario scenario) {
        String commandLinePlatform = System.getProperty("mobile.platform");
        if (commandLinePlatform != null && !commandLinePlatform.trim().isEmpty()) {
            MobilePlatform platform = MobilePlatform.from(commandLinePlatform);
            logger.info("Resolved native mobile platform from command-line override [-Dmobile.platform={}]: {}", commandLinePlatform, platform);
            return platform;
        }

        boolean hasAndroidTag = scenario != null && scenario.getSourceTagNames().contains("@android");
        boolean hasIosTag = scenario != null && scenario.getSourceTagNames().contains("@ios");
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

        MobilePlatform configuredDefault = MobileConfigurationProperties.getDefaultPlatform();
        logger.info("Resolved native mobile platform from mobile-config.yml mobile.default_platform: {}", configuredDefault);
        return configuredDefault;
    }
}
