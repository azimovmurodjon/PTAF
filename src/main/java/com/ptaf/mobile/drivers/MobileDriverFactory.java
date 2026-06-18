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
        setIfPresent(options::setAppActivity, MobileConfigurationProperties.getCapability(platform, "app_activity", ""));
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
