package com.ptaf.mobile.drivers;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-local Appium driver manager to support parallel-safe Cucumber execution.
 */
public final class MobileDriverManager {
    private static final Logger logger = LoggerFactory.getLogger(MobileDriverManager.class);
    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<MobilePlatform> PLATFORM = new ThreadLocal<>();

    private MobileDriverManager() { throw new IllegalStateException("Utility class"); }

    public static AppiumDriver startDriver(MobilePlatform platform) {
        if (!MobileConfigurationProperties.isEnabled()) {
            throw new IllegalStateException("Mobile automation is disabled in mobile-config.yml");
        }
        closeDriver();
        MobilePlatform resolvedPlatform = platform == null ? MobileConfigurationProperties.getDefaultPlatform() : platform;
        AppiumDriver driver = MobileDriverFactory.createDriver(resolvedPlatform);
        DRIVER.set(driver);
        PLATFORM.set(resolvedPlatform);
        return driver;
    }

    public static AppiumDriver getDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("No Appium driver is available for this thread. Start a mobile scenario first.");
        }
        return driver;
    }

    public static boolean hasDriver() { return DRIVER.get() != null; }
    public static MobilePlatform getPlatform() { return PLATFORM.get(); }

    public static void closeDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
                logger.info("Closed Appium session successfully.");
            } catch (Exception e) {
                logger.warn("Unable to close Appium session cleanly: {}", e.getMessage());
            }
        }
        DRIVER.remove();
        PLATFORM.remove();
    }
}
