package com.ptaf.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.HttpCredentials;
import com.microsoft.playwright.options.ViewportSize;
import com.ptaf.ui.mobilebrowser.MobileBrowserExecutionConfig;
import com.ptaf.ui.mobilebrowser.MobileBrowserProfile;
import com.ptaf.ui.mobilebrowser.MobileBrowserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * BrowserFactory is responsible for creating Playwright Browser and BrowserContext instances.
 *
 * <p><strong>Enterprise maximize behavior:</strong> PTAF supports an optional Selenium-like
 * browser maximize mode for headed Chromium/Chrome/Edge execution. When
 * {@code maximize_browser=true} is configured and {@code headless=false}, Chromium is launched
 * with {@code --start-maximized} and the browser context is created without a fixed viewport.
 * This lets the operating system manage the browser window size similarly to Selenium's
 * {@code driver.manage().window().maximize()}.</p>
 *
 * <p>This behavior is intentionally opt-in and does not affect existing tests unless the
 * configuration key is enabled. Mobile browser emulation profiles are also protected from this
 * behavior because they must keep their configured device viewport.</p>
 */
public final class BrowserFactory {

    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);
    private static final String TIMESTAMP = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    private static final String VIDEO_DIR = "test-output/captured-videos/" + TIMESTAMP;
    private static final String MOBILE_BROWSER_VIDEO_DIR = "test-output/mobile-browser-evidence/" + TIMESTAMP + "/videos";

    private BrowserFactory() { throw new IllegalStateException("Utility class"); }

    public enum BrowserTypeEnum { CHROME, FIREFOX, WEBKIT, EDGE }

    private static final ThreadLocal<MobileBrowserProfile> ACTIVE_MOBILE_BROWSER_PROFILE = new ThreadLocal<>();

    public static Browser createBrowser(BrowserTypeEnum browserTypeEnum) {
        ACTIVE_MOBILE_BROWSER_PROFILE.remove();
        Playwright playwright = Playwright.create();
        return switch (browserTypeEnum) {
            case CHROME -> launchChromium(playwright.chromium(), "CHROME", null);
            case FIREFOX -> launchBrowser(playwright.firefox());
            case WEBKIT -> launchBrowser(playwright.webkit());
            case EDGE -> launchChromium(playwright.chromium(), "EDGE", "msedge");
        };
    }

    /** Creates a Playwright browser for a named mobile-browser emulation profile. */
    public static Browser createBrowser(String profileName) {
        MobileBrowserProfile profile = MobileBrowserProfileRepository.findByName(profileName)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported browser or mobile browser profile: " + profileName));
        if (!MobileBrowserExecutionConfig.isEnabled()) {
            throw new IllegalStateException("Mobile browser emulation is disabled in mobile-browser-execution.yml");
        }
        ACTIVE_MOBILE_BROWSER_PROFILE.set(profile);
        Playwright playwright = Playwright.create();
        if (profile.usesWebKit()) return launchBrowser(playwright.webkit());
        if (profile.usesFirefox()) return launchBrowser(playwright.firefox());
        return launchChromium(playwright.chromium(), profile.getName(), null);
    }

    public static boolean isMobileBrowserProfile(String browserName) { return MobileBrowserProfileRepository.isMobileBrowserProfile(browserName); }

    public static boolean hasActiveMobileBrowserProfile() { return ACTIVE_MOBILE_BROWSER_PROFILE.get() != null; }

    private static Browser launchBrowser(BrowserType browserType) {
        boolean headless = getHeadlessMode();
        boolean maximizeBrowser = getMaximizeBrowser();
        logger.info("Launching browser: {} with headless mode: {}", browserType.name().toUpperCase(), headless);
        if (maximizeBrowser) {
            logger.warn("maximize_browser=true is optimized for headed Chromium/Chrome/Edge. Browser [{}] does not support Selenium-style --start-maximized through this factory path. Existing behavior will be preserved.", browserType.name().toUpperCase());
        }
        return browserType.launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }

    private static Browser launchChromium(BrowserType browserType, String browserName, String channel) {
        boolean headless = getHeadlessMode();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();
        boolean maximizeBrowser = getMaximizeBrowser();

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(headless);
        if (channel != null && !channel.trim().isEmpty()) launchOptions.setChannel(channel);

        List<String> launchArgs = new ArrayList<>();
        if (shouldIgnoreHTTPSErrors) {
            launchArgs.add("--ignore-certificate-errors");
            launchArgs.add("--allow-insecure-localhost");
            launchArgs.add("--disable-web-security");
            logger.info("Launching {} with SSL bypass launch arguments enabled because ignoreHTTPSErrors=true.", browserName);
        }

        if (maximizeBrowser && !headless && !hasActiveMobileBrowserProfile()) {
            launchArgs.add("--start-maximized");
            logger.info("Launching {} with Selenium-style maximize enabled using --start-maximized.", browserName);
        } else if (maximizeBrowser && headless) {
            logger.warn("maximize_browser=true was requested, but headless=true. Playwright cannot maximize a real OS browser window in headless mode; existing headless behavior will be preserved.");
        } else if (maximizeBrowser && hasActiveMobileBrowserProfile()) {
            logger.info("maximize_browser=true ignored for mobile browser emulation profile because device viewport must remain profile-controlled.");
        }

        if (!launchArgs.isEmpty()) launchOptions.setArgs(launchArgs);

        logger.info("Launching {} with headless mode: {}, ignoreHTTPSErrors: {}, maximize_browser: {}", browserName, headless, shouldIgnoreHTTPSErrors, maximizeBrowser);
        return browserType.launch(launchOptions);
    }

    public static BrowserContext createContextWithVideo(Browser browser) {
        boolean recordVideo = getVideoCapture();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();
        boolean mobileBrowser = hasActiveMobileBrowserProfile();
        boolean mobileBrowserVideo = mobileBrowser && MobileBrowserExecutionConfig.videoRecordingEnabled();
        boolean maximizeBrowser = getMaximizeBrowser();
        boolean headless = getHeadlessMode();

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions().setIgnoreHTTPSErrors(shouldIgnoreHTTPSErrors);
        applySeleniumStyleMaximizeViewportIfConfigured(contextOptions, maximizeBrowser, headless, mobileBrowser);
        applyMobileBrowserProfileIfAvailable(contextOptions);
        logger.info("Creating UI BrowserContext. ignoreHTTPSErrors={}", shouldIgnoreHTTPSErrors);
        applyHttpCredentialsIfAvailable(contextOptions);

        if (mobileBrowserVideo) {
            logger.info("Mobile browser video capture enabled. Videos will be stored under: {}", MOBILE_BROWSER_VIDEO_DIR);
            contextOptions.setRecordVideoDir(Paths.get(MOBILE_BROWSER_VIDEO_DIR))
                    .setRecordVideoSize(MobileBrowserExecutionConfig.getVideoSizeWidth(), MobileBrowserExecutionConfig.getVideoSizeHeight());
        } else if (recordVideo) {
            logger.info("Video capture enabled. Videos will be stored under: {}", VIDEO_DIR);
            contextOptions.setRecordVideoDir(Paths.get(VIDEO_DIR)).setRecordVideoSize(1280, 720);
        } else {
            logger.info("Video capture disabled.");
        }

        BrowserContext context = browser.newContext(contextOptions);
        logger.info("UI BrowserContext created successfully. ignoreHTTPSErrors={}, videoCapture={}, mobileBrowserProfile={}, maximize_browser={}",
                shouldIgnoreHTTPSErrors, recordVideo || mobileBrowserVideo,
                ACTIVE_MOBILE_BROWSER_PROFILE.get() != null ? ACTIVE_MOBILE_BROWSER_PROFILE.get().getName() : "none",
                maximizeBrowser);
        return context;
    }

    /**
     * Applies Selenium-like maximize context behavior for headed desktop Playwright runs.
     *
     * <p>Playwright normally controls viewport independently from browser window size. Setting
     * viewport to {@code null} allows the context viewport to follow the actual browser window,
     * which is required for true headed maximize behavior.</p>
     */
    private static void applySeleniumStyleMaximizeViewportIfConfigured(Browser.NewContextOptions contextOptions,
                                                                       boolean maximizeBrowser,
                                                                       boolean headless,
                                                                       boolean mobileBrowser) {
        if (!maximizeBrowser) return;
        if (headless) {
            logger.warn("maximize_browser=true requested, but headless=true. No viewport override will be applied.");
            return;
        }
        if (mobileBrowser) {
            logger.info("maximize_browser=true ignored for mobile browser emulation context because profile viewport must remain deterministic.");
            return;
        }
        contextOptions.setViewportSize((ViewportSize) null);
        logger.info("Playwright context viewport disabled so headed browser window can use Selenium-style maximized size.");
    }

    private static void applyMobileBrowserProfileIfAvailable(Browser.NewContextOptions contextOptions) {
        MobileBrowserProfile profile = ACTIVE_MOBILE_BROWSER_PROFILE.get();
        if (profile == null) return;

        int viewportWidth = profile.getViewportWidth();
        int viewportHeight = profile.getViewportHeight();
        int screenWidth = profile.getScreenWidth();
        int screenHeight = profile.getScreenHeight();
        String orientationMode = MobileBrowserExecutionConfig.getOrientationMode();

        if ("portrait".equals(orientationMode) && viewportWidth > viewportHeight) {
            int tmp = viewportWidth; viewportWidth = viewportHeight; viewportHeight = tmp;
            tmp = screenWidth; screenWidth = screenHeight; screenHeight = tmp;
        } else if ("landscape".equals(orientationMode) && viewportHeight > viewportWidth) {
            int tmp = viewportWidth; viewportWidth = viewportHeight; viewportHeight = tmp;
            tmp = screenWidth; screenWidth = screenHeight; screenHeight = tmp;
        }

        contextOptions.setViewportSize(viewportWidth, viewportHeight)
                .setScreenSize(screenWidth, screenHeight)
                .setDeviceScaleFactor(profile.getDeviceScaleFactor())
                .setIsMobile(profile.isMobile())
                .setHasTouch(profile.hasTouch());
        if (isNotBlank(profile.getUserAgent())) contextOptions.setUserAgent(profile.getUserAgent());
        logger.info("Applied mobile browser profile [{}] orientationMode={} viewport={}x{} screen={}x{} scale={} touch={}",
                profile.getName(), orientationMode, viewportWidth, viewportHeight, screenWidth, screenHeight, profile.getDeviceScaleFactor(), profile.hasTouch());
    }

    private static void applyHttpCredentialsIfAvailable(Browser.NewContextOptions contextOptions) {
        String username = System.getProperty("service.username");
        String password = System.getProperty("service.password");
        if (isNotBlank(username) && isNotBlank(password)) {
            contextOptions.setHttpCredentials(new HttpCredentials(username, password));
            logger.info("HTTP authentication credentials applied.");
        } else {
            logger.info("No HTTP credentials found. Proceeding without authentication.");
        }
    }

    private static boolean getHeadlessMode() { return Boolean.parseBoolean(ConfigurationProperties.getHeadlessMode()); }
    private static boolean getVideoCapture() { return Boolean.parseBoolean(ConfigurationProperties.getVideoCapture()); }

    private static boolean getMaximizeBrowser() {
        String value = ConfigurationProperties.getValue("maximize_browser");
        if (!isNotBlank(value)) value = ConfigurationProperties.getValue("maximizeBrowser");
        if (!isNotBlank(value)) return false;
        return Boolean.parseBoolean(value.trim());
    }

    private static boolean getIgnoreHTTPSErrors() {
        String value = ConfigurationProperties.getIgnoreHTTPSErrors();
        if (value == null || value.trim().isEmpty()) {
            logger.warn("ignoreHTTPSErrors is missing or blank. Defaulting to false.");
            return false;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private static boolean isNotBlank(String value) { return value != null && !value.trim().isEmpty(); }
}
