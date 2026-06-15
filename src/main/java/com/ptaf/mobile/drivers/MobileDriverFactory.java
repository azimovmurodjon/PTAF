package com.ptaf.mobile.drivers;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Creates Appium sessions for native Android and iOS applications.
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
        setOrientationCapability(options, MobileConfigurationProperties.getOrientation(platform));
        return new IOSDriver(serverUrl, options);
    }

    private static void setOrientationCapability(org.openqa.selenium.MutableCapabilities options, String orientation) {
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
}
