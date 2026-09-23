package com.ptaf.mobile.drivers;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.MutableCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory responsible for creating configured AppiumDriver instances for native apps and
 * browser sessions on Android and iOS.
 *
 * <p>
 * This class is configuration-driven: all session capabilities and behavior are read from
 * MobileConfigurationProperties which is typically backed by YAML configuration files
 * (for example mobile-config.yml and mobile-browser-config.yml). The same code path can
 * be used to run simulator/simulator apps (.app), real-device apps (.ipa), Android APKs,
 * or mobile browser sessions (Chrome, Safari) without code changes.
 * </p>
 *
 * <p>
 * Important tester notes:
 * - Optional capabilities are only applied when present in configuration (blank values are ignored).
 * - There are separate creation paths for native app sessions and browser sessions to avoid
 *   accidental capability crossover (e.g. sending appPackage to a browser session).
 * - Browser sessions include best-effort "clean start" logic to clear cookies/data and
 *   optionally terminate/activate the browser app depending on platform and configuration.
 * - The factory will set an implicit wait after session creation if configured.
 * </p>
 */
public final class MobileDriverFactory {
    private static final Logger logger = LoggerFactory.getLogger(MobileDriverFactory.class);

    private MobileDriverFactory() { throw new IllegalStateException("Utility class"); }

    /**
     * Create and return an AppiumDriver for a native mobile application (not browser).
     *
     * <p>
     * The requested platform may be null; in that case the default platform from
     * MobileConfigurationProperties is used. This method:
     * - Builds platform-specific options (UiAutomator2Options for Android, XCUITestOptions for iOS)
     * - Applies required and optional capabilities from configuration
     * - Connects to the Appium server provided by configuration
     * - Optionally sets an implicit wait and attempts to apply runtime device orientation
     * </p>
     *
     * @param platform The target MobilePlatform (ANDROID or IOS). If null, default is used.
     * @return A started AppiumDriver connected to the configured Appium server.
     * @throws IllegalArgumentException when the configured Appium server URL is malformed.
     */
    public static AppiumDriver createDriver(MobilePlatform platform) {
        // Use default platform when caller didn't provide one
        MobilePlatform targetPlatform = platform == null ? MobileConfigurationProperties.getDefaultPlatform() : platform;
        try {
            URL serverUrl = new URL(MobileConfigurationProperties.getAppiumServerUrl());
            // Log the planned native session capability summary for diagnostics
            logNativeSessionPlan(targetPlatform);
            // Create the correct driver type based on platform
            AppiumDriver driver = targetPlatform.isAndroid() ? createAndroidDriver(serverUrl) : createIosDriver(serverUrl);

            // Apply global implicit wait if configured (> 0 seconds)
            int implicitWait = MobileConfigurationProperties.getImplicitWaitSeconds();
            if (implicitWait > 0) {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            }

            // Try to apply orientation at runtime if driver supports the mobile command
            applyRuntimeOrientationIfSupported(driver, targetPlatform);

            logger.info("Started Appium {} session with id [{}]", targetPlatform, driver.getSessionId());
            return driver;
        } catch (MalformedURLException e) {
            // Bubble up as IllegalArgumentException to indicate config problem to the caller/tester
            throw new IllegalArgumentException("Invalid Appium server URL: " + MobileConfigurationProperties.getAppiumServerUrl(), e);
        }
    }

    /**
     * Build and return a configured AndroidDriver for native app automation.
     *
     * <p>
     * All capability values come from MobileConfigurationProperties for MobilePlatform.ANDROID.
     * The method sets required defaults and only applies optional capabilities if they are non-blank.
     * </p>
     *
     * @param serverUrl Appium server URL to connect to.
     * @return a new AndroidDriver instance configured per YAML.
     */
    private static AndroidDriver createAndroidDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.ANDROID;

        // Base UiAutomator2 options with defaults if missing from config
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(MobileConfigurationProperties.getCapability(platform, "platform_name", "Android"))
                .setAutomationName(MobileConfigurationProperties.getCapability(platform, "automation_name", "UiAutomator2"))
                .setDeviceName(MobileConfigurationProperties.getCapability(platform, "device_name", "Android Emulator"))
                .setNoReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "no_reset", false))
                .setFullReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "full_reset", false))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));

        // Optional string capabilities: apply only when non-blank
        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getCapability(platform, "platform_version", ""));
        setIfPresent(options::setApp, MobileConfigurationProperties.getCapability(platform, "app", ""));
        setIfPresent(options::setAppPackage, MobileConfigurationProperties.getCapability(platform, "app_package", ""));
        // app_activity can be supplied under two YAML keys for backward compatibility
        setIfPresent(options::setAppActivity, firstNonBlank(MobileConfigurationProperties.getCapability(platform, "app_activity", ""), MobileConfigurationProperties.getCapability(platform, "appActivity", "")));

        // Generic capability names that are not first-class setters on UiAutomator2Options
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getCapability(platform, "udid", ""));
        setCapabilityIfPresent(options, "systemPort", MobileConfigurationProperties.getCapability(platform, "system_port", ""));
        setCapabilityIfPresent(options, "adbExecTimeout", MobileConfigurationProperties.getCapability(platform, "adb_exec_timeout", ""));
        setCapabilityIfPresent(options, "appWaitActivity", MobileConfigurationProperties.getCapability(platform, "app_wait_activity", ""));
        setCapabilityIfPresent(options, "appWaitPackage", MobileConfigurationProperties.getCapability(platform, "app_wait_package", ""));

        // Optional boolean capabilities read via YAML boolean parser helper
        setBooleanCapabilityIfPresent(options, "autoGrantPermissions", platform, "auto_grant_permissions");

        // Orientation capability may be provided as part of mobile config; normalize and apply if valid
        setOrientationCapability(options, MobileConfigurationProperties.getOrientation(platform));

        // Create and return the driver instance connected to the server URL
        return new AndroidDriver(serverUrl, options);
    }

    /**
     * Build and return a configured IOSDriver for native app automation.
     *
     * <p>
     * This method applies a number of optional iOS-specific capabilities (XCUITest/WDA related).
     * Capabilities are only set if non-blank in configuration. Additional guidance about .ipa/.app
     * files and UDID presence is logged for testers.
     * </p>
     *
     * @param serverUrl Appium server URL to connect to.
     * @return a new IOSDriver instance configured per YAML.
     */
    private static IOSDriver createIosDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.IOS;

        // Base XCUITest options with sensible defaults when not provided
        XCUITestOptions options = new XCUITestOptions()
                .setPlatformName(MobileConfigurationProperties.getCapability(platform, "platform_name", "iOS"))
                .setAutomationName(MobileConfigurationProperties.getCapability(platform, "automation_name", "XCUITest"))
                .setDeviceName(MobileConfigurationProperties.getCapability(platform, "device_name", "iPhone Simulator"))
                .setNoReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "no_reset", false))
                .setFullReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "full_reset", false))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));

        // Optional standard capabilities
        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getCapability(platform, "platform_version", ""));
        setIfPresent(options::setApp, MobileConfigurationProperties.getCapability(platform, "app", ""));
        setIfPresent(options::setBundleId, MobileConfigurationProperties.getCapability(platform, "bundle_id", ""));

        // Device selection: UDID required for real-device .ipa execution
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getCapability(platform, "udid", ""));

        // WebDriverAgent / Xcode signing related optional capabilities for real devices
        setCapabilityIfPresent(options, "xcodeOrgId", MobileConfigurationProperties.getCapability(platform, "xcode_org_id", ""));
        setCapabilityIfPresent(options, "xcodeSigningId", MobileConfigurationProperties.getCapability(platform, "xcode_signing_id", ""));
        setCapabilityIfPresent(options, "updatedWDABundleId", MobileConfigurationProperties.getCapability(platform, "updated_wda_bundle_id", ""));

        // Enterprise/debugging/diagnostics related options for XCUITest and WDA
        setCapabilityIfPresent(options, "wdaLocalPort", MobileConfigurationProperties.getCapability(platform, "wda_local_port", ""));
        setCapabilityIfPresent(options, "wdaStartupRetries", MobileConfigurationProperties.getCapability(platform, "wda_startup_retries", ""));
        setCapabilityIfPresent(options, "wdaStartupRetryInterval", MobileConfigurationProperties.getCapability(platform, "wda_startup_retry_interval", ""));
        setCapabilityIfPresent(options, "wdaLaunchTimeout", MobileConfigurationProperties.getCapability(platform, "wda_launch_timeout", ""));
        setCapabilityIfPresent(options, "wdaConnectionTimeout", MobileConfigurationProperties.getCapability(platform, "wda_connection_timeout", ""));
        setCapabilityIfPresent(options, "waitForIdleTimeout", MobileConfigurationProperties.getCapability(platform, "wait_for_idle_timeout", ""));
        setCapabilityIfPresent(options, "appLaunchStateTimeoutSec", MobileConfigurationProperties.getCapability(platform, "app_launch_state_timeout_sec", ""));

        // Optional booleans commonly used for iOS automation flows
        setBooleanCapabilityIfPresent(options, "useNewWDA", platform, "use_new_wda");
        setBooleanCapabilityIfPresent(options, "showXcodeLog", platform, "show_xcode_log");
        setBooleanCapabilityIfPresent(options, "autoAcceptAlerts", platform, "auto_accept_alerts");
        setBooleanCapabilityIfPresent(options, "autoDismissAlerts", platform, "auto_dismiss_alerts");
        setBooleanCapabilityIfPresent(options, "includeSafariInWebviews", platform, "include_safari_in_webviews");
        setBooleanCapabilityIfPresent(options, "connectHardwareKeyboard", platform, "connect_hardware_keyboard");
        setBooleanCapabilityIfPresent(options, "enforceAppInstall", platform, "enforce_app_install");

        // Orientation capability if set in config
        setOrientationCapability(options, MobileConfigurationProperties.getOrientation(platform));

        // Provide guidance when testers supply .ipa without UDID, or .app with UDID
        logIosArtifactGuidance(platform);

        // Return the created iOS driver
        return new IOSDriver(serverUrl, options);
    }


    /**
     * Create an AppiumDriver for browser automation on mobile (Chrome on Android or Safari on iOS).
     *
     * <p>
     * Browser sessions are kept separate from native app sessions to avoid accidentally sending
     * app-focused capabilities to browser sessions. The method:
     * - Builds browser-specific options (UiAutomator2Options or XCUITestOptions)
     * - Connects to Appium server
     * - Applies implicit wait when configured
     * - Optionally performs best-effort browser cleanup/start tasks (clear cookies, clear app data,
     *   terminate/activate the browser app) depending on configuration and platform safeguards
     * - Optionally opens an initial URL if configured
     * </p>
     *
     * @param platform Target mobile platform (ANDROID or IOS). If null, default is used.
     * @return A started AppiumDriver representing the browser session.
     * @throws IllegalArgumentException when the configured Appium server URL is malformed.
     */
    public static AppiumDriver createBrowserDriver(MobilePlatform platform) {
        MobilePlatform targetPlatform = platform == null ? MobileConfigurationProperties.getDefaultPlatform() : platform;
        try {
            URL serverUrl = new URL(MobileConfigurationProperties.getAppiumServerUrl());
            // Log the planned browser session details for diagnostics
            logBrowserSessionPlan(targetPlatform);

            // Create browser driver according to platform
            AppiumDriver driver = targetPlatform.isAndroid() ? createAndroidBrowserDriver(serverUrl) : createIosBrowserDriver(serverUrl);

            // Apply global implicit wait if configured
            int implicitWait = MobileConfigurationProperties.getImplicitWaitSeconds();
            if (implicitWait > 0) {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            }

            // Clean-start is best-effort and platform-aware. Android Chrome and iOS Safari may
            // require special handling to avoid invalidating the WebDriver session.
            prepareCleanBrowserStartIfConfigured(driver, targetPlatform);

            // Open initial URL if present in browser config
            openInitialBrowserUrlIfConfigured(driver, targetPlatform);

            logger.info("Started Appium {} browser session with id [{}]", targetPlatform, driver.getSessionId());
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Appium server URL: " + MobileConfigurationProperties.getAppiumServerUrl(), e);
        }
    }

    /**
     * Build and return AndroidDriver configured for mobile browser automation.
     *
     * <p>
     * This method sets browserName to Chrome by default and applies browser-specific
     * capabilities from mobile-browser-config.yml (through MobileConfigurationProperties).
     * It also handles ChromeDriver related settings such as chromedriverExecutable and mapping file.
     * </p>
     *
     * @param serverUrl Appium server URL to connect to.
     * @return a configured AndroidDriver for browser automation.
     */
    private static AndroidDriver createAndroidBrowserDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.ANDROID;

        // Basic options for Android browser automation
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(MobileConfigurationProperties.getBrowserCapability(platform, "platform_name", "Android"))
                .setAutomationName(MobileConfigurationProperties.getBrowserCapability(platform, "automation_name", "UiAutomator2"))
                .setDeviceName(MobileConfigurationProperties.getBrowserCapability(platform, "device_name", "Android Emulator"))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));

        // Browser-specific capabilities (browserName and reset behavior)
        options.setCapability("browserName", MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Chrome"));
        options.setCapability("noReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "no_reset", false));
        options.setCapability("fullReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "full_reset", false));

        // Optional capabilities
        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getBrowserCapability(platform, "platform_version", ""));
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getBrowserCapability(platform, "udid", ""));
        setCapabilityIfPresent(options, "chromedriverExecutable", MobileConfigurationProperties.getBrowserCapability(platform, "chromedriver_executable", ""));
        setCapabilityIfPresent(options, "chromedriverChromeMappingFile", MobileConfigurationProperties.getBrowserCapability(platform, "chromedriver_mapping_file", ""));

        // Optional boolean browser capabilities
        setBooleanBrowserCapabilityIfPresent(options, "chromedriverAutodownload", platform, "chromedriver_autodownload");
        setBooleanBrowserCapabilityIfPresent(options, "autoGrantPermissions", platform, "auto_grant_permissions");

        // Orientation for browser sessions; default falls back to MobileConfigurationProperties.getOrientation(platform)
        setOrientationCapability(options, MobileConfigurationProperties.getBrowserCapability(platform, "orientation", MobileConfigurationProperties.getOrientation(platform)));

        return new AndroidDriver(serverUrl, options);
    }

    /**
     * Build and return IOSDriver configured for mobile browser automation (Safari).
     *
     * <p>
     * Sets browserName to Safari by default and applies optional capabilities such as
     * safariInitialUrl and Safari-specific toggles. Only non-blank configuration keys are applied.
     * </p>
     *
     * @param serverUrl Appium server URL to connect to.
     * @return a configured IOSDriver for browser automation.
     */
    private static IOSDriver createIosBrowserDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.IOS;

        // Base XCUITest options for Safari automation
        XCUITestOptions options = new XCUITestOptions()
                .setPlatformName(MobileConfigurationProperties.getBrowserCapability(platform, "platform_name", "iOS"))
                .setAutomationName(MobileConfigurationProperties.getBrowserCapability(platform, "automation_name", "XCUITest"))
                .setDeviceName(MobileConfigurationProperties.getBrowserCapability(platform, "device_name", "iPhone Simulator"))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));

        // Safari specific capabilities
        options.setCapability("browserName", MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Safari"));
        options.setCapability("noReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "no_reset", false));
        options.setCapability("fullReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "full_reset", false));

        // Optional settings
        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getBrowserCapability(platform, "platform_version", ""));
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getBrowserCapability(platform, "udid", ""));
        setCapabilityIfPresent(options, "safariInitialUrl", MobileConfigurationProperties.getBrowserCapability(platform, "initial_url", ""));

        // Safari toggles and options (booleans)
        setBooleanBrowserCapabilityIfPresent(options, "autoAcceptAlerts", platform, "auto_accept_alerts");
        setBooleanBrowserCapabilityIfPresent(options, "autoDismissAlerts", platform, "auto_dismiss_alerts");
        setBooleanBrowserCapabilityIfPresent(options, "includeSafariInWebviews", platform, "include_safari_in_webviews");
        setBooleanBrowserCapabilityIfPresent(options, "connectHardwareKeyboard", platform, "connect_hardware_keyboard");
        setBooleanBrowserCapabilityIfPresent(options, "safariAllowPopups", platform, "safari_allow_popups");
        setBooleanBrowserCapabilityIfPresent(options, "safariIgnoreFraudWarning", platform, "safari_ignore_fraud_warning");

        // Orientation for Safari sessions
        setOrientationCapability(options, MobileConfigurationProperties.getBrowserCapability(platform, "orientation", MobileConfigurationProperties.getOrientation(platform)));

        return new IOSDriver(serverUrl, options);
    }

    /**
     * Best-effort cleaning and preparation for mobile browser sessions.
     *
     * <p>
     * This method reads browser-cleanup configuration and attempts the following (when enabled):
     * - Terminate the browser app before start (if not a native browser session that would invalidate the session)
     * - Clear Selenium cookies via driver.manage().deleteAllCookies()
     * - Clear Android browser application data via an ADB shell pm clear command (requires --relaxed-security)
     * - Activate/bring the browser app to foreground after cleanup
     *
     * The behavior is platform-aware: for Android Chrome and iOS Safari the terminate/activate steps
     * are skipped to avoid invalidating the Appium WebDriver session (these browsers have special session semantics).
     * All attempts are logged; failures are tolerated (best-effort), and exceptions are logged for tester diagnostics.
     * </p>
     *
     * @param driver   active AppiumDriver instance for the session
     * @param platform target mobile platform
     */
    private static void prepareCleanBrowserStartIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        // If clean start is disabled in browser config then skip all cleanup steps
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "clean_start_enabled", true)) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | disabled by mobile-browser-config.yml");
            return;
        }

        // Determine if we have a "native" browser session (Android Chrome or iOS Safari).
        // For those native browser sessions, terminating/activating the app may invalidate the WebDriver session.
        boolean isAndroidChrome = platform.isAndroid() && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Chrome").equalsIgnoreCase("Chrome");
        boolean isIosSafari = platform == com.ptaf.mobile.config.MobilePlatform.IOS && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Safari").equalsIgnoreCase("Safari");
        boolean isNativeBrowser = isAndroidChrome || isIosSafari;

        // Log the chosen clean-start options so testers know what will be attempted
        logger.info("PTAF APPIUM REAL BROWSER CLEAN START | platform={} | clearCookies={} | resetAppData={} | closeTabs={}",
                platform,
                MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "clear_cookies", true),
                MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "reset_app_data", false),
                MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "close_existing_tabs", true));

        // If it's a native browser session, skip terminating/activating the app to avoid invalidation
        if (isNativeBrowser) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Native browser session detected ({}). Skipping app termination/activation to prevent session invalidation.", platform);
        } else {
            // Non-native browser flows can terminate the app before starting
            terminateBrowserAppIfConfigured(driver, platform);
        }

        // Try to clear Selenium cookies (works for web context)
        clearBrowserCookiesIfConfigured(driver, platform);

        // Android-specific application data clearing via adb shell pm clear (best-effort)
        clearAndroidBrowserAppDataIfConfigured(driver, platform);

        // For non-native browser flows, re-activate the browser app after cleanup
        if (!isNativeBrowser) {
            activateBrowserAppIfConfigured(driver, platform);
        }
    }

    /**
     * Attempt to delete all Selenium cookies if configured.
     *
     * <p>Failures are logged and ignored as this is best-effort cleanup for browser tests.</p>
     */
    private static void clearBrowserCookiesIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "clear_cookies", true)) return;
        try {
            driver.manage().deleteAllCookies();
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Selenium cookies cleared for {}.", platform);
        } catch (Exception e) {
            // Cookie deletion may not be supported immediately or in certain contexts; log for diagnostics.
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Cookie cleanup was not available yet for {}. Details: {}", platform, e.getMessage());
        }
    }

    /**
     * Terminate the browser application if configured to do so and safe to perform.
     *
     * <p>
     * For Android Chrome and iOS Safari, terminating the browser may invalidate the created
     * WebDriver session, so termination is skipped and logged. For other browser packages/bundleIds,
     * this method executes the Appium "mobile: terminateApp" endpoint with either appId (Android) or bundleId (iOS).
     * </p>
     */
    private static void terminateBrowserAppIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        // Safeguards: skip termination for platform-native browsers that would invalidate the session
        if (platform.isAndroid() && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Chrome")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping terminateBrowserApp for Android Chrome to prevent session invalidation.");
            return;
        }
        if (platform == com.ptaf.mobile.config.MobilePlatform.IOS && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Safari")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping terminateBrowserApp for iOS Safari to prevent session invalidation.");
            return;
        }

        // Only terminate if the YAML key terminate_before_start is true
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "terminate_before_start", true)) return;

        // Determine the package/bundle id to terminate
        String bundleOrPackage = platform.isAndroid()
                ? MobileConfigurationProperties.getBrowserCapability(platform, "browser_package", "com.android.chrome")
                : MobileConfigurationProperties.getBrowserCapability(platform, "browser_bundle_id", "com.apple.mobilesafari");
        try {
            // Use Appium executeScript "mobile: terminateApp" which accepts appId for Android or bundleId for iOS
            driver.executeScript("mobile: terminateApp", Map.of(platform.isAndroid() ? "appId" : "bundleId", bundleOrPackage));
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Terminated browser app [{}].", bundleOrPackage);
        } catch (Exception e) {
            // Termination may not be supported or may fail; log details for troubleshooting
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Browser termination was skipped/not supported for [{}]. Details: {}", bundleOrPackage, e.getMessage());
        }
    }

    /**
     * Activate / bring the browser application to foreground if configured.
     *
     * <p>
     * Similar to termination, activation for Android Chrome or iOS Safari is skipped to avoid
     * invalidating the WebDriver session. For other browsers this calls the Appium
     * "mobile: activateApp" endpoint with the package or bundle id.
     * </p>
     */
    private static void activateBrowserAppIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        // Safeguard: Do not activate native browsers (would invalidate session)
        if (platform.isAndroid() && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Chrome")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping activateBrowserApp for Android Chrome to prevent session invalidation.");
            return;
        }
        if (platform == com.ptaf.mobile.config.MobilePlatform.IOS && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Safari")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping activateBrowserApp for iOS Safari to prevent session invalidation.");
            return;
        }

        // Only activate when the YAML setting is enabled
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "activate_after_cleanup", true)) return;

        // Determine package/bundle id
        String bundleOrPackage = platform.isAndroid()
                ? MobileConfigurationProperties.getBrowserCapability(platform, "browser_package", "com.android.chrome")
                : MobileConfigurationProperties.getBrowserCapability(platform, "browser_bundle_id", "com.apple.mobilesafari");
        try {
            driver.executeScript("mobile: activateApp", Map.of(platform.isAndroid() ? "appId" : "bundleId", bundleOrPackage));
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Activated browser app [{}].", bundleOrPackage);
        } catch (Exception e) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Browser activation was skipped/not supported for [{}]. Details: {}", bundleOrPackage, e.getMessage());
        }
    }

    /**
     * Clear Android browser application data using an adb shell command if configured.
     *
     * <p>
     * This method runs a mobile shell command equivalent to "pm clear <package>" via Appium.
     * It requires the Appium server to be started with --relaxed-security to allow shell execution
     * from the client in many environments. Failures are logged and the test continues (best-effort).
     * </p>
     */
    private static void clearAndroidBrowserAppDataIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        // Only relevant for Android
        if (!platform.isAndroid()) return;
        // Only proceed if reset_app_data is set to true
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "reset_app_data", false)) return;

        String browserPackage = MobileConfigurationProperties.getBrowserCapability(platform, "browser_package", "com.android.chrome");
        try {
            // Build the arguments for "pm clear <package>"
            Map<String, Object> args = new HashMap<>();
            args.put("command", "pm");
            args.put("args", java.util.List.of("clear", browserPackage));
            // Ask Appium to run the shell command on device
            driver.executeScript("mobile: shell", args);
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Cleared Android browser app data for [{}].", browserPackage);
        } catch (Exception e) {
            // Common cause: Appium server not started with --relaxed-security or command not permitted
            logger.warn("PTAF APPIUM REAL BROWSER CLEAN START | Could not clear Android browser app data for [{}]. Start Appium with --relaxed-security or set reset_app_data=false. Details: {}", browserPackage, e.getMessage());
        }
    }

    /**
     * Utility: parse boolean browser capability from YAML and set it on MutableCapabilities if present.
     *
     * <p>
     * The YAML value is read as a string and parsed with Boolean.parseBoolean after trimming.
     * Empty or missing values are ignored so defaults remain in effect.
     * </p>
     */
    private static void setBooleanBrowserCapabilityIfPresent(MutableCapabilities options, String capabilityName, MobilePlatform platform, String yamlKey) {
        String raw = MobileConfigurationProperties.getBrowserCapability(platform, yamlKey, "");
        if (raw != null && !raw.trim().isEmpty()) {
            options.setCapability(capabilityName, Boolean.parseBoolean(raw.trim()));
        }
    }

    /**
     * Normalize and set orientation capability on the capabilities bag if orientation is supported.
     *
     * <p>
     * Supported values (case-insensitive): "PORTRAIT" or "LANDSCAPE". The value is normalized
     * to uppercase before being set as a capability.
     * </p>
     */
    private static void setOrientationCapability(MutableCapabilities options, String orientation) {
        if (isSupportedOrientation(orientation)) {
            options.setCapability("orientation", orientation.toUpperCase());
        }
    }

    /**
     * Attempt to apply device orientation at runtime via Appium's "mobile: setDeviceOrientation".
     *
     * <p>
     * Some drivers/sessions may not support this mobile command. The method logs success or
     * expected failures; the initial orientation capability was still sent during session creation.
     * </p>
     */
    private static void applyRuntimeOrientationIfSupported(AppiumDriver driver, MobilePlatform platform) {
        String orientation = MobileConfigurationProperties.getOrientation(platform);
        if (!isSupportedOrientation(orientation)) {
            return;
        }
        try {
            driver.executeScript("mobile: setDeviceOrientation", java.util.Map.of("orientation", orientation.toUpperCase()));
            logger.info("Applied runtime mobile orientation [{}].", orientation.toUpperCase());
        } catch (Exception e) {
            // Log that runtime orientation command isn't supported; capability may still have been set during creation
            logger.info("Runtime orientation command was not supported by this driver/session. Initial orientation capability was still sent. Details: {}", e.getMessage());
        }
    }

    /**
     * Return true when the orientation string is a supported orientation.
     */
    private static boolean isSupportedOrientation(String orientation) {
        return "PORTRAIT".equalsIgnoreCase(orientation) || "LANDSCAPE".equalsIgnoreCase(orientation);
    }

    /**
     * Helper to call a setter only when the provided value is non-blank.
     *
     * <p>
     * This avoids setting empty strings as capabilities and keeps the capability payload clean.
     * </p>
     */
    private static void setIfPresent(java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }

    /**
     * Helper to set a named capability on MutableCapabilities when the value is non-blank.
     *
     * <p>
     * This method will attempt to parse scalar strings like integers and doubles to their numeric
     * types before setting the capability, so YAML values that represent numbers are typed
     * appropriately for the driver.
     * </p>
     */
    private static void setCapabilityIfPresent(MutableCapabilities options, String capabilityName, String value) {
        if (value != null && !value.trim().isEmpty()) {
            options.setCapability(capabilityName, parseScalar(value.trim()));
        }
    }

    /**
     * Helper to set a capability boolean read from generic capability YAML for native sessions.
     *
     * <p>
     * The YAML key is read as a string, trimmed and then parsed with Boolean.parseBoolean. Empty
     * or missing values are ignored.
     * </p>
     */
    private static void setBooleanCapabilityIfPresent(MutableCapabilities options, String capabilityName, MobilePlatform platform, String yamlKey) {
        String raw = MobileConfigurationProperties.getCapability(platform, yamlKey, "");
        if (raw != null && !raw.trim().isEmpty()) {
            options.setCapability(capabilityName, Boolean.parseBoolean(raw.trim()));
        }
    }


    /**
     * Return the first non-blank string between primary and fallback.
     *
     * <p>
     * Used to support alternate YAML keys for the same logical setting (backwards-compatible).
     * </p>
     */
    private static String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.trim().isEmpty() ? primary : fallback;
    }

    /**
     * Log a concise summary of the proposed native session configuration.
     *
     * <p>
     * Logged information includes device name, UDID, app/bundle identifiers and server URL.
     * Useful for debugging mis-configured runs or confirming which device/app will be used.
     * </p>
     */
    private static void logNativeSessionPlan(MobilePlatform platform) {
        if (platform.isAndroid()) {
            logger.info("PTAF MOBILE NATIVE SESSION | platform=ANDROID | device={} | app={} | appPackage={} | appActivity={} | server={}",
                    MobileConfigurationProperties.getCapability(platform, "device_name", "Android Emulator"),
                    MobileConfigurationProperties.getCapability(platform, "app", ""),
                    MobileConfigurationProperties.getCapability(platform, "app_package", ""),
                    firstNonBlank(MobileConfigurationProperties.getCapability(platform, "app_activity", ""), MobileConfigurationProperties.getCapability(platform, "appActivity", "")),
                    MobileConfigurationProperties.getAppiumServerUrl());
        } else {
            logger.info("PTAF MOBILE NATIVE SESSION | platform=IOS | device={} | udid={} | app={} | bundleId={} | autoAcceptAlerts={} | autoDismissAlerts={} | server={}",
                    MobileConfigurationProperties.getCapability(platform, "device_name", "iPhone Simulator"),
                    MobileConfigurationProperties.getCapability(platform, "udid", ""),
                    MobileConfigurationProperties.getCapability(platform, "app", ""),
                    MobileConfigurationProperties.getCapability(platform, "bundle_id", ""),
                    MobileConfigurationProperties.getCapability(platform, "auto_accept_alerts", ""),
                    MobileConfigurationProperties.getCapability(platform, "auto_dismiss_alerts", ""),
                    MobileConfigurationProperties.getAppiumServerUrl());
        }
    }

    /**
     * Log a concise summary of the planned browser session configuration for diagnostics.
     *
     * <p>
     * Also logs an Android-specific diagnostic note about ChromeDriver when appropriate.
     * </p>
     */
    private static void logBrowserSessionPlan(MobilePlatform platform) {
        logger.info("PTAF APPIUM REAL BROWSER SESSION | platform={} | browserName={} | device={} | udid={} | initialUrl={} | server={}",
                platform,
                MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", platform.isAndroid() ? "Chrome" : "Safari"),
                MobileConfigurationProperties.getBrowserCapability(platform, "device_name", platform.isAndroid() ? "Android Emulator" : "iPhone Simulator"),
                MobileConfigurationProperties.getBrowserCapability(platform, "udid", ""),
                MobileConfigurationProperties.getBrowserCapability(platform, "initial_url", ""),
                MobileConfigurationProperties.getAppiumServerUrl());
        if (platform.isAndroid()) {
            logger.info("PTAF APPIUM REAL BROWSER DIAGNOSTIC | Android Chrome requires a compatible ChromeDriver. If session creation fails, start Appium with --relaxed-security or set chromedriver_executable in mobile-browser-config.yml.");
        }
    }

    /**
     * Open the configured initial browser URL after session start, if provided.
     *
     * <p>
     * Navigation is attempted with driver.get(initialUrl). Failures are logged as warnings
     * because test steps can retry navigation if needed.
     * </p>
     */
    private static void openInitialBrowserUrlIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        String initialUrl = MobileConfigurationProperties.getBrowserCapability(platform, "initial_url", "");
        if (initialUrl == null || initialUrl.trim().isEmpty()) return;
        try {
            logger.info("PTAF APPIUM REAL BROWSER NAVIGATION | Opening initial URL [{}]", initialUrl.trim());
            driver.get(initialUrl.trim());
        } catch (Exception e) {
            // Navigation may fail depending on timing, network, or driver support; log and let test flows decide next steps.
            logger.warn("PTAF APPIUM REAL BROWSER NAVIGATION WARNING | Unable to open initial URL [{}]. The feature step can retry navigation. Root cause: {}", initialUrl, e.getMessage());
        }
    }

    /**
     * Parse a scalar string into Integer/Long/Double where appropriate, otherwise return the original string.
     *
     * <p>
     * This method supports simple numeric detection so capability strings that represent numbers
     * are set as numeric types on the capabilities object rather than strings. Examples:
     * "42" -> Integer 42, "9000000000" -> Long, "3.14" -> Double.
     * </p>
     */
    private static Object parseScalar(String value) {
        if (value.matches("^-?\\d+$")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // If it doesn't fit into Integer, fallback to Long
                return Long.parseLong(value);
            }
        }
        if (value.matches("^-?\\d+\\.\\d+$")) {
            return Double.parseDouble(value);
        }
        // Not a plain number; return the original string
        return value;
    }

    /**
     * Provide guidance about iOS artifact selection to testers.
     *
     * <p>
     * - If an .ipa is used without a UDID configured, warn because .ipa requires a real device.
     * - If a .app is used with a UDID present, log that the .app will run against the provided UDID.
     * This helps avoid common misconfigurations when choosing artifacts for simulators vs real devices.
     * </p>
     */
    private static void logIosArtifactGuidance(MobilePlatform platform) {
        String app = MobileConfigurationProperties.getCapability(platform, "app", "");
        String udid = MobileConfigurationProperties.getCapability(platform, "udid", "");
        if (app != null && app.toLowerCase().endsWith(".ipa") && (udid == null || udid.trim().isEmpty())) {
            logger.warn("iOS app [{}] is an IPA but no ios.udid is configured. A normal IPA requires a real iOS device. For simulator execution use a simulator-built .app.", app);
        }
        if (app != null && app.toLowerCase().endsWith(".app") && udid != null && !udid.trim().isEmpty()) {
            logger.info("iOS app [{}] will run against the configured device/simulator UDID [{}].", app, udid.trim());
        }
    }
}
