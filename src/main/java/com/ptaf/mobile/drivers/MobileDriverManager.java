package com.ptaf.mobile.drivers;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import com.ptaf.mobile.config.MobilePlatform;
import io.appium.java_client.AppiumDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-local Appium driver manager to support parallel-safe Cucumber execution.
 *
 * <p>
 * This utility class centralizes the lifecycle of AppiumDriver instances per-thread using ThreadLocal
 * storage. Tests (or Cucumber scenarios) can call {@link #startDriver(MobilePlatform)} or
 * {@link #startBrowserDriver(MobilePlatform)} at the beginning of a scenario to obtain a dedicated
 * Appium session for the current thread, and must rely on {@link #closeDriver()} to clean up the
 * session at the end.
 * </p>
 *
 * <p>
 * Design notes:
 * - A single AppiumDriver instance is stored per-thread in {@code DRIVER}.
 * - The platform used to create the driver is stored per-thread in {@code PLATFORM}.
 * - A boolean per-thread in {@code BROWSER_SESSION} indicates whether the created session
 *   is a mobile browser session (true) or a native app session (false).
 * - All methods are static; this class is a utility and cannot be instantiated.
 * </p>
 */
public final class MobileDriverManager {
    // Logger used to report lifecycle events and failures when closing sessions.
    private static final Logger logger = LoggerFactory.getLogger(MobileDriverManager.class);

    // Thread-local storage for the AppiumDriver instance. Each thread (test/scenario) will
    // have its own driver reference so parallel execution does not share sessions.
    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    // Thread-local storage for the resolved mobile platform used to create the driver.
    // This helps tests assert or adapt behavior based on the platform under test.
    private static final ThreadLocal<MobilePlatform> PLATFORM = new ThreadLocal<>();

    // Thread-local flag to indicate whether the current session is a browser session
    // (true) or an application session (false). Default is false.
    private static final ThreadLocal<Boolean> BROWSER_SESSION = ThreadLocal.withInitial(() -> false);

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * <p>Throws an IllegalStateException when called to make misuse obvious.</p>
     */
    private MobileDriverManager() { throw new IllegalStateException("Utility class"); }

    /**
     * Start a new Appium driver for a native mobile application on the specified platform.
     *
     * <p>
     * The method performs the following steps:
     * - Validates that mobile automation is enabled in configuration.
     * - Closes any existing driver for the current thread (to ensure a clean start).
     * - Resolves the desired platform: if the supplied {@code platform} is null, the default
     *   platform from configuration is used.
     * - Creates a platform-specific native AppiumDriver via {@code MobileDriverFactory.createDriver}.
     * - Stores the created driver and platform in thread-local storage.
     * </p>
     *
     * @param platform the desired mobile platform to start a driver for. If null, the configured
     *                 default platform will be used.
     * @return the newly created AppiumDriver instance associated with the current thread.
     * @throws IllegalStateException if mobile automation is disabled in the configuration.
     */
    public static AppiumDriver startDriver(MobilePlatform platform) {
        if (!MobileConfigurationProperties.isEnabled()) {
            throw new IllegalStateException("Mobile automation is disabled in mobile-config.yml");
        }
        // Ensure any previous driver tied to this thread is terminated before starting a new one.
        closeDriver();
        // If the caller didn't specify a platform, fall back to the configured default platform.
        MobilePlatform resolvedPlatform = platform == null ? MobileConfigurationProperties.getDefaultPlatform() : platform;
        // Create a native app AppiumDriver for the resolved platform.
        AppiumDriver driver = MobileDriverFactory.createDriver(resolvedPlatform);
        // Store the driver and resolved platform in thread-local storage for future access.
        DRIVER.set(driver);
        PLATFORM.set(resolvedPlatform);
        return driver;
    }

    /**
     * Start a new Appium driver configured to work with a mobile browser on the specified platform.
     *
     * <p>
     * This method mirrors {@link #startDriver(MobilePlatform)} but specifically creates a browser-capable
     * driver via {@code MobileDriverFactory.createBrowserDriver} and sets the browser-session flag.
     * </p>
     *
     * @param platform the desired mobile platform to start a browser driver for. If null, the configured
     *                 default platform will be used.
     * @return the newly created AppiumDriver instance configured for browser automation.
     * @throws IllegalStateException if mobile automation is disabled in the configuration.
     */
    public static AppiumDriver startBrowserDriver(MobilePlatform platform) {
        if (!MobileConfigurationProperties.isEnabled()) {
            throw new IllegalStateException("Mobile automation is disabled in mobile-config.yml");
        }
        // Ensure a clean state before starting a new browser session.
        closeDriver();
        // Resolve platform using provided value or default configuration.
        MobilePlatform resolvedPlatform = platform == null ? MobileConfigurationProperties.getDefaultPlatform() : platform;
        // Create a browser-capable AppiumDriver for the platform.
        AppiumDriver driver = MobileDriverFactory.createBrowserDriver(resolvedPlatform);
        // Store driver and platform in thread-local storage.
        DRIVER.set(driver);
        PLATFORM.set(resolvedPlatform);
        // Mark this thread's session as a browser session.
        BROWSER_SESSION.set(true);
        return driver;
    }

    /**
     * Returns whether the current thread's session was created as a browser session.
     *
     * @return true if the current session is a browser session; false otherwise.
     */
    public static boolean isBrowserSession() { return Boolean.TRUE.equals(BROWSER_SESSION.get()); }

    /**
     * Retrieve the Appium driver associated with the current thread.
     *
     * @return the thread-local AppiumDriver instance.
     * @throws IllegalStateException if no driver has been started for the current thread.
     */
    public static AppiumDriver getDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            // Provide a clear message to callers (test code / step definitions) indicating
            // that they must start a mobile scenario before attempting to access the driver.
            throw new IllegalStateException("No Appium driver is available for this thread. Start a mobile scenario first.");
        }
        return driver;
    }

    /**
     * Convenience check to determine if a driver exists for the current thread.
     *
     * @return true if a driver is present; false otherwise.
     */
    public static boolean hasDriver() { return DRIVER.get() != null; }

    /**
     * Retrieve the MobilePlatform associated with the current thread's driver.
     *
     * @return the thread-local MobilePlatform, or null if no platform has been set.
     */
    public static MobilePlatform getPlatform() { return PLATFORM.get(); }

    /**
     * Close and clean up the Appium driver associated with the current thread.
     *
     * <p>
     * This method will attempt to gracefully quit the Appium session by calling {@code driver.quit()}.
     * Any exception encountered during quit will be logged as a warning but will not be rethrown to
     * avoid masking teardown activities. After attempting to quit, all thread-local references are
     * removed to prevent memory leaks when threads are reused by test frameworks.
     * </p>
     */
    public static void closeDriver() {
        AppiumDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                // Attempt to close the Appium session cleanly.
                driver.quit();
                logger.info("Closed Appium session successfully.");
            } catch (Exception e) {
                // Log a warning if the session could not be closed cleanly, but continue cleanup.
                logger.warn("Unable to close Appium session cleanly: {}", e.getMessage());
            }
        }
        // Remove thread-local references to avoid leaking objects across thread reuse.
        DRIVER.remove();
        PLATFORM.remove();
        BROWSER_SESSION.remove();
    }
}
