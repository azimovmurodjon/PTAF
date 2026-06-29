//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.ptaf.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.HttpCredentials;
import com.ptaf.ui.mobilebrowser.MobileBrowserExecutionConfig;
import com.ptaf.ui.mobilebrowser.MobileBrowserProfile;
import com.ptaf.ui.mobilebrowser.MobileBrowserProfileRepository;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class responsible for creating Playwright Browser and BrowserContext instances.
 *
 * <p>This class centralizes browser creation logic including:
 * - launching different browser types (Chromium, Firefox, WebKit, Edge)
 * - applying mobile browser emulation profiles when requested
 * - enforcing configuration-driven options such as headless mode, video capture, and ignoring HTTPS errors
 * - applying HTTP basic authentication credentials to contexts if provided via system properties
 *
 * <p>All methods are static and the class cannot be instantiated.
 *
 * <p>Testers and automation engineers can use this class to obtain Browser and BrowserContext
 * instances consistently across the test suite while respecting environment/configuration settings.
 */
public final class BrowserFactory {
    /**
     * SLF4J logger instance for logging startup and configuration information.
     */
    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);

    /**
     * Timestamp used to create unique folder names for captured artifacts (videos).
     * Format: yyyyMMdd_HHmmss
     */
    private static final String TIMESTAMP = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    /**
     * Base directory for storing UI video recordings captured during tests.
     * Populated in the static initializer to include a timestamp for uniqueness.
     */
    private static final String VIDEO_DIR;

    /**
     * Directory specifically for storing mobile browser evidence video recordings.
     * Populated in the static initializer to include a timestamp for uniqueness.
     */
    private static final String MOBILE_BROWSER_VIDEO_DIR;

    /**
     * ThreadLocal that holds the currently active mobile browser profile, if any.
     *
     * <p>Using ThreadLocal allows tests that run in parallel threads to maintain separate
     * mobile profiles without interfering with each other.
     */
    private static final ThreadLocal<MobileBrowserProfile> ACTIVE_MOBILE_BROWSER_PROFILE = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private BrowserFactory() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Create a Playwright Browser instance for a given BrowserTypeEnum.
     *
     * <p>This method resets any active mobile browser profile for the current thread
     * and then creates a Playwright instance to launch the requested browser type.
     * The implementation uses a switch on the enum ordinal to determine which
     * launch method to call.
     *
     * @param browserTypeEnum the enum representing the desired browser type
     * @return a launched Playwright Browser instance
     * @throws MatchException if an unsupported enum value is provided (should not occur)
     */
    public static Browser createBrowser(BrowserTypeEnum browserTypeEnum) {
        // Ensure no mobile profile remains active for this thread when creating a standard browser
        ACTIVE_MOBILE_BROWSER_PROFILE.remove();
        // Create a Playwright driver in order to create Browser instances
        Playwright playwright = Playwright.create();
        Browser var10000;
        switch (browserTypeEnum.ordinal()) {
            case 0 -> var10000 = launchChromium(playwright.chromium(), "CHROME", (String)null);
            case 1 -> var10000 = launchBrowser(playwright.firefox());
            case 2 -> var10000 = launchBrowser(playwright.webkit());
            case 3 -> var10000 = launchChromium(playwright.chromium(), "EDGE", "msedge");
            default -> throw new MatchException((String)null, (Throwable)null);
        }

        return var10000;
    }

    /**
     * Create a Playwright Browser instance that corresponds to a mobile browser profile.
     *
     * <p>The profileName is looked up in the MobileBrowserProfileRepository. If the
     * profile exists and mobile browser execution is enabled (via configuration),
     * the profile is set on the current thread and the appropriate browser engine
     * (Chromium, WebKit, or Firefox) is launched.
     *
     * @param profileName the name of the mobile browser profile to use
     * @return a launched Playwright Browser instance configured for the chosen profile
     * @throws IllegalArgumentException if the profile name is not found
     * @throws IllegalStateException if mobile browser emulation is disabled in configuration
     */
    public static Browser createBrowser(String profileName) {
        // Retrieve the mobile browser profile by name or fail fast
        MobileBrowserProfile profile = MobileBrowserProfileRepository.findByName(profileName)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported browser or mobile browser profile: " + profileName));
        // Ensure that mobile browser execution is globally enabled in configuration
        if (!MobileBrowserExecutionConfig.isEnabled()) {
            throw new IllegalStateException("Mobile browser emulation is disabled in mobile-browser-execution.yml");
        }

        // Store the chosen profile in the ThreadLocal so subsequent context creation can apply it
        ACTIVE_MOBILE_BROWSER_PROFILE.set(profile);
        Playwright playwright = Playwright.create();
        // Delegate to the appropriate browser engine depending on the profile's engine preference
        if (profile.usesWebKit()) {
            return launchBrowser(playwright.webkit());
        }

        if (profile.usesFirefox()) {
            return launchBrowser(playwright.firefox());
        }

        // Default to Chromium for other profiles; pass profile name as browserName
        return launchChromium(playwright.chromium(), profile.getName(), (String)null);
    }

    /**
     * Check whether a given browserName corresponds to a known mobile browser profile.
     *
     * @param browserName the name to check (can be a browser name or profile name)
     * @return true if the name matches a mobile browser profile, false otherwise
     */
    public static boolean isMobileBrowserProfile(String browserName) {
        return MobileBrowserProfileRepository.isMobileBrowserProfile(browserName);
    }

    /**
     * Returns true if there is an active mobile browser profile set for the current thread.
     *
     * @return true when a ThreadLocal mobile profile exists, false otherwise
     */
    public static boolean hasActiveMobileBrowserProfile() {
        return ACTIVE_MOBILE_BROWSER_PROFILE.get() != null;
    }

    /**
     * Launch a Browser using the provided BrowserType with common launch options.
     *
     * <p>This method reads the configured headless mode and logs the decision before launching.
     *
     * @param browserType the Playwright BrowserType (e.g., firefox(), webkit())
     * @return a launched Browser instance
     */
    private static Browser launchBrowser(BrowserType browserType) {
        // Determine headless mode from configuration
        boolean headless = getHeadlessMode();
        logger.info("Launching browser: {} with headless mode: {}", browserType.name().toUpperCase(), headless);
        // Launch the browser with the resolved headless option
        return browserType.launch((new BrowserType.LaunchOptions()).setHeadless(headless));
    }

    /**
     * Launch Chromium-based browsers with additional Chromium-specific options.
     *
     * <p>This method supports selecting a specific browser 'channel' (for example "msedge"),
     * enabling additional launch arguments to ignore SSL errors when configured, and setting
     * the headless mode according to configuration.
     *
     * @param browserType the Chromium BrowserType instance (playwright.chromium())
     * @param browserName a friendly name for logging (e.g., "CHROME", "EDGE" or a profile name)
     * @param channel optional Chromium channel (e.g., "msedge"). May be null.
     * @return a launched Browser instance
     */
    private static Browser launchChromium(BrowserType browserType, String browserName, String channel) {
        // Resolve configuration-driven flags
        boolean headless = getHeadlessMode();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();
        BrowserType.LaunchOptions launchOptions = (new BrowserType.LaunchOptions()).setHeadless(headless);
        // If a channel is supplied, use it to launch a particular Chromium-based browser
        if (channel != null && !channel.trim().isEmpty()) {
            launchOptions.setChannel(channel);
        }

        // If ignoring HTTPS errors is requested, add Chromium flags to bypass certificate checks
        if (shouldIgnoreHTTPSErrors) {
            launchOptions.setArgs(Arrays.asList("--ignore-certificate-errors", "--allow-insecure-localhost", "--disable-web-security"));
            logger.info("Launching {} with SSL bypass launch arguments enabled because ignoreHTTPSErrors=true.", browserName);
        }

        logger.info("Launching {} with headless mode: {}, ignoreHTTPSErrors: {}", new Object[]{browserName, headless, shouldIgnoreHTTPSErrors});
        return browserType.launch(launchOptions);
    }

    /**
     * Create a new BrowserContext with video recording and mobile profile support as configured.
     *
     * <p>This method:
     * - Reads configuration flags for recording video and ignoring HTTPS errors.
     * - Applies an active mobile browser profile if one is set on the current thread.
     * - Applies HTTP basic auth credentials if provided via system properties.
     * - Configures recording directory and video resolution for general UI or mobile-specific capture.
     *
     * @param browser the Browser instance to create the context for
     * @return a configured BrowserContext ready for use by tests
     */
    public static BrowserContext createContextWithVideo(Browser browser) {
        // Read various configuration flags used for context creation
        boolean recordVideo = getVideoCapture();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();
        boolean mobileBrowser = hasActiveMobileBrowserProfile();
        // For mobile, video capture may be controlled by the mobile-browser-execution.yml config
        boolean mobileBrowserVideo = mobileBrowser && MobileBrowserExecutionConfig.videoRecordingEnabled();
        Browser.NewContextOptions contextOptions = (new Browser.NewContextOptions()).setIgnoreHTTPSErrors(shouldIgnoreHTTPSErrors);
        // Apply mobile profile settings (viewport, device scale, user agent, etc.) if available
        applyMobileBrowserProfileIfAvailable(contextOptions);
        logger.info("Creating UI BrowserContext. ignoreHTTPSErrors={}", shouldIgnoreHTTPSErrors);
        // Apply HTTP credentials if system properties are present
        applyHttpCredentialsIfAvailable(contextOptions);
        // Configure video recording directory and size depending on whether mobile or desktop capture is requested
        if (mobileBrowserVideo) {
            logger.info("Mobile browser video capture enabled. Videos will be stored under: {}", MOBILE_BROWSER_VIDEO_DIR);
            contextOptions.setRecordVideoDir(Paths.get(MOBILE_BROWSER_VIDEO_DIR)).setRecordVideoSize(MobileBrowserExecutionConfig.getVideoSizeWidth(), MobileBrowserExecutionConfig.getVideoSizeHeight());
        } else if (recordVideo) {
            logger.info("Video capture enabled. Videos will be stored under: {}", VIDEO_DIR);
            // Default resolution for desktop recordings
            contextOptions.setRecordVideoDir(Paths.get(VIDEO_DIR)).setRecordVideoSize(1280, 720);
        } else {
            logger.info("Video capture disabled.");
        }

        // Create the context using the assembled options
        BrowserContext context = browser.newContext(contextOptions);
        // Log outcome with different messages when mobile profile is involved to include profile name
        if (mobileBrowser) {
            logger.info("UI BrowserContext created successfully. ignoreHTTPSErrors={}, videoCapture={}, mobileBrowserProfile={}", shouldIgnoreHTTPSErrors, mobileBrowserVideo, ACTIVE_MOBILE_BROWSER_PROFILE.get().getName());
        } else {
            logger.info("UI BrowserContext created successfully. ignoreHTTPSErrors={}, videoCapture={}", shouldIgnoreHTTPSErrors, recordVideo);
        }

        return context;
    }

    /**
     * Apply the currently active mobile browser profile to the provided context options, if present.
     *
     * <p>This configures viewport size, screen size, device scale factor, mobile/touch flags and
     * user agent according to the profile. It also respects an orientation mode setting that can
     * force the profile into portrait or landscape by swapping width/height values.
     *
     * @param contextOptions the Browser.NewContextOptions instance to modify
     */
    private static void applyMobileBrowserProfileIfAvailable(Browser.NewContextOptions contextOptions) {
        MobileBrowserProfile profile = ACTIVE_MOBILE_BROWSER_PROFILE.get();
        // If no profile is set for this thread, nothing to apply
        if (profile == null) {
            return;
        }

        // Extract values from profile
        int viewportWidth = profile.getViewportWidth();
        int viewportHeight = profile.getViewportHeight();
        int screenWidth = profile.getScreenWidth();
        int screenHeight = profile.getScreenHeight();
        String orientationMode = MobileBrowserExecutionConfig.getOrientationMode();
        // If a specific orientation is requested, swap width/height when necessary
        if ("portrait".equals(orientationMode) && viewportWidth > viewportHeight) {
            // Swap viewport width/height to force portrait orientation
            int tmp = viewportWidth;
            viewportWidth = viewportHeight;
            viewportHeight = tmp;
            // Swap screen width/height to match the viewport orientation
            tmp = screenWidth;
            screenWidth = screenHeight;
            screenHeight = tmp;
        } else if ("landscape".equals(orientationMode) && viewportHeight > viewportWidth) {
            // Swap viewport width/height to force landscape orientation
            int tmp = viewportWidth;
            viewportWidth = viewportHeight;
            viewportHeight = tmp;
            // Swap screen width/height to match the viewport orientation
            tmp = screenWidth;
            screenWidth = screenHeight;
            screenHeight = tmp;
        }

        // Apply the computed device metrics and capabilities to the context options
        contextOptions.setViewportSize(viewportWidth, viewportHeight)
                .setScreenSize(screenWidth, screenHeight)
                .setDeviceScaleFactor(profile.getDeviceScaleFactor())
                .setIsMobile(profile.isMobile())
                .setHasTouch(profile.hasTouch());
        // Apply user agent if one is provided in the profile (non-blank)
        if (isNotBlank(profile.getUserAgent())) {
            contextOptions.setUserAgent(profile.getUserAgent());
        }

        logger.info("Applied mobile browser profile [{}] orientationMode={} viewport={}x{} screen={}x{} scale={} touch={}", profile.getName(), orientationMode, viewportWidth, viewportHeight, screenWidth, screenHeight, profile.getDeviceScaleFactor(), profile.hasTouch());
    }

    /**
     * Apply HTTP basic authentication credentials to the context options if username/password system properties are present.
     *
     * <p>System properties used:
     * - service.username
     * - service.password
     *
     * @param contextOptions the Browser.NewContextOptions instance to modify
     */
    private static void applyHttpCredentialsIfAvailable(Browser.NewContextOptions contextOptions) {
        // Read credentials from system properties (commonly set via -Dservice.username=... -Dservice.password=...)
        String username = System.getProperty("service.username");
        String password = System.getProperty("service.password");
        // Only apply credentials when both username and password are non-blank
        if (isNotBlank(username) && isNotBlank(password)) {
            contextOptions.setHttpCredentials(new HttpCredentials(username, password));
            logger.info("HTTP authentication credentials applied.");
        } else {
            logger.info("No HTTP credentials found. Proceeding without authentication.");
        }

    }

    /**
     * Retrieve headless mode from the ConfigurationProperties helper and parse it as boolean.
     *
     * @return true if headless mode is enabled in configuration, false otherwise
     */
    private static boolean getHeadlessMode() {
        String value = ConfigurationProperties.getHeadlessMode();
        return Boolean.parseBoolean(value);
    }

    /**
     * Retrieve video capture flag from the ConfigurationProperties helper and parse it as boolean.
     *
     * @return true if video capture is enabled in configuration, false otherwise
     */
    private static boolean getVideoCapture() {
        String value = ConfigurationProperties.getVideoCapture();
        return Boolean.parseBoolean(value);
    }

    /**
     * Retrieve the ignoreHTTPSErrors flag from ConfigurationProperties, trim it and parse as boolean.
     *
     * <p>If the configuration value is missing or blank, this method logs a warning and defaults to false.
     *
     * @return true if ignoreHTTPSErrors is enabled, false otherwise
     */
    private static boolean getIgnoreHTTPSErrors() {
        String value = ConfigurationProperties.getIgnoreHTTPSErrors();
        if (value != null && !value.trim().isEmpty()) {
            return Boolean.parseBoolean(value.trim());
        } else {
            logger.warn("ignoreHTTPSErrors is missing or blank. Defaulting to false.");
            return false;
        }
    }

    /**
     * Utility method to check that a string is not null and contains non-whitespace characters.
     *
     * @param value the string to test
     * @return true when the value is not null and contains non-whitespace characters
     */
    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // Static initializer for directory constants that rely on the computed TIMESTAMP.
    static {
        VIDEO_DIR = "test-output/captured-videos/" + TIMESTAMP;
        MOBILE_BROWSER_VIDEO_DIR = "test-output/mobile-browser-evidence/" + TIMESTAMP + "/videos";
    }

    /**
     * Enum representing supported top-level browser types used by the factory.
     *
     * <p>Order matters with the ordinal-based switch in createBrowser(BrowserTypeEnum).
     */
    public static enum BrowserTypeEnum {
        CHROME,
        FIREFOX,
        WEBKIT,
        EDGE;
    }
}
