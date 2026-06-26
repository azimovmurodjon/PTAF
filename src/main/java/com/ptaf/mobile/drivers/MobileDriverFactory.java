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
 * Creates Appium sessions for native Android and iOS applications.
 *
 * <p>The factory is intentionally configuration-driven so the same PTAF framework can run
 * simulator .app files, real-device .ipa files, Android APKs, and future project apps
 * without code changes. Optional capabilities are applied only when present in YAML.</p>
 */
public final class MobileDriverFactory {
    private static final Logger logger = LoggerFactory.getLogger(MobileDriverFactory.class);

    private MobileDriverFactory() { throw new IllegalStateException("Utility class"); }

    public static AppiumDriver createDriver(MobilePlatform platform) {
        MobilePlatform targetPlatform = platform == null ? MobileConfigurationProperties.getDefaultPlatform() : platform;
        try {
            URL serverUrl = new URL(MobileConfigurationProperties.getAppiumServerUrl());
            logNativeSessionPlan(targetPlatform);
            AppiumDriver driver = targetPlatform.isAndroid() ? createAndroidDriver(serverUrl) : createIosDriver(serverUrl);
            int implicitWait = MobileConfigurationProperties.getImplicitWaitSeconds();
            if (implicitWait > 0) {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            }
            applyRuntimeOrientationIfSupported(driver, targetPlatform);
            logger.info("Started Appium {} session with id [{}]", targetPlatform, driver.getSessionId());
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Appium server URL: " + MobileConfigurationProperties.getAppiumServerUrl(), e);
        }
    }

    private static AndroidDriver createAndroidDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.ANDROID;
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(MobileConfigurationProperties.getCapability(platform, "platform_name", "Android"))
                .setAutomationName(MobileConfigurationProperties.getCapability(platform, "automation_name", "UiAutomator2"))
                .setDeviceName(MobileConfigurationProperties.getCapability(platform, "device_name", "Android Emulator"))
                .setNoReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "no_reset", false))
                .setFullReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "full_reset", false))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));

        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getCapability(platform, "platform_version", ""));
        setIfPresent(options::setApp, MobileConfigurationProperties.getCapability(platform, "app", ""));
        setIfPresent(options::setAppPackage, MobileConfigurationProperties.getCapability(platform, "app_package", ""));
        setIfPresent(options::setAppActivity, firstNonBlank(MobileConfigurationProperties.getCapability(platform, "app_activity", ""), MobileConfigurationProperties.getCapability(platform, "appActivity", "")));
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getCapability(platform, "udid", ""));
        setCapabilityIfPresent(options, "systemPort", MobileConfigurationProperties.getCapability(platform, "system_port", ""));
        setCapabilityIfPresent(options, "adbExecTimeout", MobileConfigurationProperties.getCapability(platform, "adb_exec_timeout", ""));
        setCapabilityIfPresent(options, "appWaitActivity", MobileConfigurationProperties.getCapability(platform, "app_wait_activity", ""));
        setCapabilityIfPresent(options, "appWaitPackage", MobileConfigurationProperties.getCapability(platform, "app_wait_package", ""));
        setBooleanCapabilityIfPresent(options, "autoGrantPermissions", platform, "auto_grant_permissions");
        setOrientationCapability(options, MobileConfigurationProperties.getOrientation(platform));
        return new AndroidDriver(serverUrl, options);
    }

    private static IOSDriver createIosDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.IOS;
        XCUITestOptions options = new XCUITestOptions()
                .setPlatformName(MobileConfigurationProperties.getCapability(platform, "platform_name", "iOS"))
                .setAutomationName(MobileConfigurationProperties.getCapability(platform, "automation_name", "XCUITest"))
                .setDeviceName(MobileConfigurationProperties.getCapability(platform, "device_name", "iPhone Simulator"))
                .setNoReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "no_reset", false))
                .setFullReset(MobileConfigurationProperties.getCapabilityBoolean(platform, "full_reset", false))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));

        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getCapability(platform, "platform_version", ""));
        setIfPresent(options::setApp, MobileConfigurationProperties.getCapability(platform, "app", ""));
        setIfPresent(options::setBundleId, MobileConfigurationProperties.getCapability(platform, "bundle_id", ""));

        // Device selection. Required for real-device .ipa execution and useful when multiple simulators share a name.
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getCapability(platform, "udid", ""));

        // Real-device WebDriverAgent signing capabilities. These are optional and ignored when blank.
        setCapabilityIfPresent(options, "xcodeOrgId", MobileConfigurationProperties.getCapability(platform, "xcode_org_id", ""));
        setCapabilityIfPresent(options, "xcodeSigningId", MobileConfigurationProperties.getCapability(platform, "xcode_signing_id", ""));
        setCapabilityIfPresent(options, "updatedWDABundleId", MobileConfigurationProperties.getCapability(platform, "updated_wda_bundle_id", ""));

        // Enterprise XCUITest stability and diagnostics options.
        setCapabilityIfPresent(options, "wdaLocalPort", MobileConfigurationProperties.getCapability(platform, "wda_local_port", ""));
        setCapabilityIfPresent(options, "wdaStartupRetries", MobileConfigurationProperties.getCapability(platform, "wda_startup_retries", ""));
        setCapabilityIfPresent(options, "wdaStartupRetryInterval", MobileConfigurationProperties.getCapability(platform, "wda_startup_retry_interval", ""));
        setCapabilityIfPresent(options, "wdaLaunchTimeout", MobileConfigurationProperties.getCapability(platform, "wda_launch_timeout", ""));
        setCapabilityIfPresent(options, "wdaConnectionTimeout", MobileConfigurationProperties.getCapability(platform, "wda_connection_timeout", ""));
        setCapabilityIfPresent(options, "waitForIdleTimeout", MobileConfigurationProperties.getCapability(platform, "wait_for_idle_timeout", ""));
        setCapabilityIfPresent(options, "appLaunchStateTimeoutSec", MobileConfigurationProperties.getCapability(platform, "app_launch_state_timeout_sec", ""));

        setBooleanCapabilityIfPresent(options, "useNewWDA", platform, "use_new_wda");
        setBooleanCapabilityIfPresent(options, "showXcodeLog", platform, "show_xcode_log");
        setBooleanCapabilityIfPresent(options, "autoAcceptAlerts", platform, "auto_accept_alerts");
        setBooleanCapabilityIfPresent(options, "autoDismissAlerts", platform, "auto_dismiss_alerts");
        setBooleanCapabilityIfPresent(options, "includeSafariInWebviews", platform, "include_safari_in_webviews");
        setBooleanCapabilityIfPresent(options, "connectHardwareKeyboard", platform, "connect_hardware_keyboard");
        setBooleanCapabilityIfPresent(options, "enforceAppInstall", platform, "enforce_app_install");

        setOrientationCapability(options, MobileConfigurationProperties.getOrientation(platform));
        logIosArtifactGuidance(platform);
        return new IOSDriver(serverUrl, options);
    }


    /**
     * Creates an Appium browser session on a real emulator/simulator browser.
     *
     * <p>This is intentionally separate from native app creation. Native app sessions use
     * {@code app/appPackage/appActivity/bundleId}; browser sessions use {@code browserName}.
     * Keeping these paths separate prevents app-focused capabilities from accidentally
     * breaking Chrome/Safari browser automation.</p>
     */
    public static AppiumDriver createBrowserDriver(MobilePlatform platform) {
        MobilePlatform targetPlatform = platform == null ? MobileConfigurationProperties.getDefaultPlatform() : platform;
        try {
            URL serverUrl = new URL(MobileConfigurationProperties.getAppiumServerUrl());
            logBrowserSessionPlan(targetPlatform);
            AppiumDriver driver = targetPlatform.isAndroid() ? createAndroidBrowserDriver(serverUrl) : createIosBrowserDriver(serverUrl);
            int implicitWait = MobileConfigurationProperties.getImplicitWaitSeconds();
            if (implicitWait > 0) {
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
            }
            // For Android browser sessions, clean-start operations like terminateApp/activateApp
            // must be carefully managed to avoid invalidating the WebDriver session.
            // The clean-start logic is now conditionally applied within the method.
            prepareCleanBrowserStartIfConfigured(driver, targetPlatform);
            openInitialBrowserUrlIfConfigured(driver, targetPlatform);
            logger.info("Started Appium {} browser session with id [{}]", targetPlatform, driver.getSessionId());
            return driver;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid Appium server URL: " + MobileConfigurationProperties.getAppiumServerUrl(), e);
        }
    }

    private static AndroidDriver createAndroidBrowserDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.ANDROID;
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(MobileConfigurationProperties.getBrowserCapability(platform, "platform_name", "Android"))
                .setAutomationName(MobileConfigurationProperties.getBrowserCapability(platform, "automation_name", "UiAutomator2"))
                .setDeviceName(MobileConfigurationProperties.getBrowserCapability(platform, "device_name", "Android Emulator"))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));
        options.setCapability("browserName", MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Chrome"));
        options.setCapability("noReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "no_reset", false));
        options.setCapability("fullReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "full_reset", false));
        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getBrowserCapability(platform, "platform_version", ""));
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getBrowserCapability(platform, "udid", ""));
        setCapabilityIfPresent(options, "chromedriverExecutable", MobileConfigurationProperties.getBrowserCapability(platform, "chromedriver_executable", ""));
        setCapabilityIfPresent(options, "chromedriverChromeMappingFile", MobileConfigurationProperties.getBrowserCapability(platform, "chromedriver_mapping_file", ""));
        setBooleanBrowserCapabilityIfPresent(options, "chromedriverAutodownload", platform, "chromedriver_autodownload");
        setBooleanBrowserCapabilityIfPresent(options, "autoGrantPermissions", platform, "auto_grant_permissions");
        setOrientationCapability(options, MobileConfigurationProperties.getBrowserCapability(platform, "orientation", MobileConfigurationProperties.getOrientation(platform)));
        return new AndroidDriver(serverUrl, options);
    }

    private static IOSDriver createIosBrowserDriver(URL serverUrl) {
        MobilePlatform platform = MobilePlatform.IOS;
        XCUITestOptions options = new XCUITestOptions()
                .setPlatformName(MobileConfigurationProperties.getBrowserCapability(platform, "platform_name", "iOS"))
                .setAutomationName(MobileConfigurationProperties.getBrowserCapability(platform, "automation_name", "XCUITest"))
                .setDeviceName(MobileConfigurationProperties.getBrowserCapability(platform, "device_name", "iPhone Simulator"))
                .setNewCommandTimeout(Duration.ofSeconds(MobileConfigurationProperties.getNewCommandTimeoutSeconds()));
        options.setCapability("browserName", MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Safari"));
        options.setCapability("noReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "no_reset", false));
        options.setCapability("fullReset", MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "full_reset", false));
        setIfPresent(options::setPlatformVersion, MobileConfigurationProperties.getBrowserCapability(platform, "platform_version", ""));
        setCapabilityIfPresent(options, "udid", MobileConfigurationProperties.getBrowserCapability(platform, "udid", ""));
        setCapabilityIfPresent(options, "safariInitialUrl", MobileConfigurationProperties.getBrowserCapability(platform, "initial_url", ""));
        setBooleanBrowserCapabilityIfPresent(options, "autoAcceptAlerts", platform, "auto_accept_alerts");
        setBooleanBrowserCapabilityIfPresent(options, "autoDismissAlerts", platform, "auto_dismiss_alerts");
        setBooleanBrowserCapabilityIfPresent(options, "includeSafariInWebviews", platform, "include_safari_in_webviews");
        setBooleanBrowserCapabilityIfPresent(options, "connectHardwareKeyboard", platform, "connect_hardware_keyboard");
        setBooleanBrowserCapabilityIfPresent(options, "safariAllowPopups", platform, "safari_allow_popups");
        setBooleanBrowserCapabilityIfPresent(options, "safariIgnoreFraudWarning", platform, "safari_ignore_fraud_warning");
        setOrientationCapability(options, MobileConfigurationProperties.getBrowserCapability(platform, "orientation", MobileConfigurationProperties.getOrientation(platform)));
        return new IOSDriver(serverUrl, options);
    }

    /**
     * Best-effort clean-start preparation for Appium real mobile browser sessions.
     *
     * <p>This method is intentionally used only by createBrowserDriver. Native app automation
     * is not affected. Browser cleanup is best-effort because iOS Safari and Android Chrome
     * expose different reset controls through Appium. The framework always logs what was
     * attempted and never hides cleanup failures that may explain later browser instability.</p>
     */
    private static void prepareCleanBrowserStartIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "clean_start_enabled", true)) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | disabled by mobile-browser-config.yml");
            return;
        }

        // Safeguard: For mobile browsers (Android Chrome, iOS Safari), do not terminate/activate if it would invalidate the session.
        boolean isAndroidChrome = platform.isAndroid() && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Chrome").equalsIgnoreCase("Chrome");
        boolean isIosSafari = platform == com.ptaf.mobile.config.MobilePlatform.IOS && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "Safari").equalsIgnoreCase("Safari");
        boolean isNativeBrowser = isAndroidChrome || isIosSafari;

        logger.info("PTAF APPIUM REAL BROWSER CLEAN START | platform={} | clearCookies={} | resetAppData={} | closeTabs={}",
                platform,
                MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "clear_cookies", true),
                MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "reset_app_data", false),
                MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "close_existing_tabs", true));

        if (isNativeBrowser) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Native browser session detected ({}). Skipping app termination/activation to prevent session invalidation.", platform);
        } else {
            terminateBrowserAppIfConfigured(driver, platform);
        }

        clearBrowserCookiesIfConfigured(driver, platform);
        clearAndroidBrowserAppDataIfConfigured(driver, platform);

        if (!isNativeBrowser) {
            activateBrowserAppIfConfigured(driver, platform);
        }
    }

    private static void clearBrowserCookiesIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "clear_cookies", true)) return;
        try {
            driver.manage().deleteAllCookies();
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Selenium cookies cleared for {}.", platform);
        } catch (Exception e) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Cookie cleanup was not available yet for {}. Details: {}", platform, e.getMessage());
        }
    }

    private static void terminateBrowserAppIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        // Safeguard: Do not terminate native browsers if it would invalidate the session.
        if (platform.isAndroid() && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Chrome")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping terminateBrowserApp for Android Chrome to prevent session invalidation.");
            return;
        }
        if (platform == com.ptaf.mobile.config.MobilePlatform.IOS && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Safari")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping terminateBrowserApp for iOS Safari to prevent session invalidation.");
            return;
        }
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "terminate_before_start", true)) return;
        String bundleOrPackage = platform.isAndroid()
                ? MobileConfigurationProperties.getBrowserCapability(platform, "browser_package", "com.android.chrome")
                : MobileConfigurationProperties.getBrowserCapability(platform, "browser_bundle_id", "com.apple.mobilesafari");
        try {
            driver.executeScript("mobile: terminateApp", Map.of(platform.isAndroid() ? "appId" : "bundleId", bundleOrPackage));
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Terminated browser app [{}].", bundleOrPackage);
        } catch (Exception e) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Browser termination was skipped/not supported for [{}]. Details: {}", bundleOrPackage, e.getMessage());
        }
    }

    private static void activateBrowserAppIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        // Safeguard: Do not activate native browsers if it would invalidate the session.
        if (platform.isAndroid() && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Chrome")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping activateBrowserApp for Android Chrome to prevent session invalidation.");
            return;
        }
        if (platform == com.ptaf.mobile.config.MobilePlatform.IOS && MobileConfigurationProperties.getBrowserCapability(platform, "browser_name", "").equalsIgnoreCase("Safari")) {
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Skipping activateBrowserApp for iOS Safari to prevent session invalidation.");
            return;
        }
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "activate_after_cleanup", true)) return;
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

    private static void clearAndroidBrowserAppDataIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        if (!platform.isAndroid()) return;
        if (!MobileConfigurationProperties.getBrowserCapabilityBoolean(platform, "reset_app_data", false)) return;
        String browserPackage = MobileConfigurationProperties.getBrowserCapability(platform, "browser_package", "com.android.chrome");
        try {
            Map<String, Object> args = new HashMap<>();
            args.put("command", "pm");
            args.put("args", java.util.List.of("clear", browserPackage));
            driver.executeScript("mobile: shell", args);
            logger.info("PTAF APPIUM REAL BROWSER CLEAN START | Cleared Android browser app data for [{}].", browserPackage);
        } catch (Exception e) {
            logger.warn("PTAF APPIUM REAL BROWSER CLEAN START | Could not clear Android browser app data for [{}]. Start Appium with --relaxed-security or set reset_app_data=false. Details: {}", browserPackage, e.getMessage());
        }
    }

    private static void setBooleanBrowserCapabilityIfPresent(MutableCapabilities options, String capabilityName, MobilePlatform platform, String yamlKey) {
        String raw = MobileConfigurationProperties.getBrowserCapability(platform, yamlKey, "");
        if (raw != null && !raw.trim().isEmpty()) {
            options.setCapability(capabilityName, Boolean.parseBoolean(raw.trim()));
        }
    }

    private static void setOrientationCapability(MutableCapabilities options, String orientation) {
        if (isSupportedOrientation(orientation)) {
            options.setCapability("orientation", orientation.toUpperCase());
        }
    }

    private static void applyRuntimeOrientationIfSupported(AppiumDriver driver, MobilePlatform platform) {
        String orientation = MobileConfigurationProperties.getOrientation(platform);
        if (!isSupportedOrientation(orientation)) {
            return;
        }
        try {
            driver.executeScript("mobile: setDeviceOrientation", java.util.Map.of("orientation", orientation.toUpperCase()));
            logger.info("Applied runtime mobile orientation [{}].", orientation.toUpperCase());
        } catch (Exception e) {
            logger.info("Runtime orientation command was not supported by this driver/session. Initial orientation capability was still sent. Details: {}", e.getMessage());
        }
    }

    private static boolean isSupportedOrientation(String orientation) {
        return "PORTRAIT".equalsIgnoreCase(orientation) || "LANDSCAPE".equalsIgnoreCase(orientation);
    }

    private static void setIfPresent(java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }

    private static void setCapabilityIfPresent(MutableCapabilities options, String capabilityName, String value) {
        if (value != null && !value.trim().isEmpty()) {
            options.setCapability(capabilityName, parseScalar(value.trim()));
        }
    }

    private static void setBooleanCapabilityIfPresent(MutableCapabilities options, String capabilityName, MobilePlatform platform, String yamlKey) {
        String raw = MobileConfigurationProperties.getCapability(platform, yamlKey, "");
        if (raw != null && !raw.trim().isEmpty()) {
            options.setCapability(capabilityName, Boolean.parseBoolean(raw.trim()));
        }
    }


    private static String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.trim().isEmpty() ? primary : fallback;
    }

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

    private static void openInitialBrowserUrlIfConfigured(AppiumDriver driver, MobilePlatform platform) {
        String initialUrl = MobileConfigurationProperties.getBrowserCapability(platform, "initial_url", "");
        if (initialUrl == null || initialUrl.trim().isEmpty()) return;
        try {
            logger.info("PTAF APPIUM REAL BROWSER NAVIGATION | Opening initial URL [{}]", initialUrl.trim());
            driver.get(initialUrl.trim());
        } catch (Exception e) {
            logger.warn("PTAF APPIUM REAL BROWSER NAVIGATION WARNING | Unable to open initial URL [{}]. The feature step can retry navigation. Root cause: {}", initialUrl, e.getMessage());
        }
    }

    private static Object parseScalar(String value) {
        if (value.matches("^-?\\d+$")) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                return Long.parseLong(value);
            }
        }
        if (value.matches("^-?\\d+\\.\\d+$")) {
            return Double.parseDouble(value);
        }
        return value;
    }

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
