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

public final class BrowserFactory {
    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);
    private static final String TIMESTAMP = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    private static final String VIDEO_DIR;
    private static final String MOBILE_BROWSER_VIDEO_DIR;
    private static final ThreadLocal<MobileBrowserProfile> ACTIVE_MOBILE_BROWSER_PROFILE = new ThreadLocal<>();

    private BrowserFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static Browser createBrowser(BrowserTypeEnum browserTypeEnum) {
        ACTIVE_MOBILE_BROWSER_PROFILE.remove();
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

    public static Browser createBrowser(String profileName) {
        MobileBrowserProfile profile = MobileBrowserProfileRepository.findByName(profileName)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported browser or mobile browser profile: " + profileName));
        if (!MobileBrowserExecutionConfig.isEnabled()) {
            throw new IllegalStateException("Mobile browser emulation is disabled in mobile-browser-execution.yml");
        }

        ACTIVE_MOBILE_BROWSER_PROFILE.set(profile);
        Playwright playwright = Playwright.create();
        if (profile.usesWebKit()) {
            return launchBrowser(playwright.webkit());
        }

        if (profile.usesFirefox()) {
            return launchBrowser(playwright.firefox());
        }

        return launchChromium(playwright.chromium(), profile.getName(), (String)null);
    }

    public static boolean isMobileBrowserProfile(String browserName) {
        return MobileBrowserProfileRepository.isMobileBrowserProfile(browserName);
    }

    public static boolean hasActiveMobileBrowserProfile() {
        return ACTIVE_MOBILE_BROWSER_PROFILE.get() != null;
    }

    private static Browser launchBrowser(BrowserType browserType) {
        boolean headless = getHeadlessMode();
        logger.info("Launching browser: {} with headless mode: {}", browserType.name().toUpperCase(), headless);
        return browserType.launch((new BrowserType.LaunchOptions()).setHeadless(headless));
    }

    private static Browser launchChromium(BrowserType browserType, String browserName, String channel) {
        boolean headless = getHeadlessMode();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();
        BrowserType.LaunchOptions launchOptions = (new BrowserType.LaunchOptions()).setHeadless(headless);
        if (channel != null && !channel.trim().isEmpty()) {
            launchOptions.setChannel(channel);
        }

        if (shouldIgnoreHTTPSErrors) {
            launchOptions.setArgs(Arrays.asList("--ignore-certificate-errors", "--allow-insecure-localhost", "--disable-web-security"));
            logger.info("Launching {} with SSL bypass launch arguments enabled because ignoreHTTPSErrors=true.", browserName);
        }

        logger.info("Launching {} with headless mode: {}, ignoreHTTPSErrors: {}", new Object[]{browserName, headless, shouldIgnoreHTTPSErrors});
        return browserType.launch(launchOptions);
    }

    public static BrowserContext createContextWithVideo(Browser browser) {
        boolean recordVideo = getVideoCapture();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();
        boolean mobileBrowser = hasActiveMobileBrowserProfile();
        boolean mobileBrowserVideo = mobileBrowser && MobileBrowserExecutionConfig.videoRecordingEnabled();
        Browser.NewContextOptions contextOptions = (new Browser.NewContextOptions()).setIgnoreHTTPSErrors(shouldIgnoreHTTPSErrors);
        applyMobileBrowserProfileIfAvailable(contextOptions);
        logger.info("Creating UI BrowserContext. ignoreHTTPSErrors={}", shouldIgnoreHTTPSErrors);
        applyHttpCredentialsIfAvailable(contextOptions);
        if (mobileBrowserVideo) {
            logger.info("Mobile browser video capture enabled. Videos will be stored under: {}", MOBILE_BROWSER_VIDEO_DIR);
            contextOptions.setRecordVideoDir(Paths.get(MOBILE_BROWSER_VIDEO_DIR)).setRecordVideoSize(MobileBrowserExecutionConfig.getVideoSizeWidth(), MobileBrowserExecutionConfig.getVideoSizeHeight());
        } else if (recordVideo) {
            logger.info("Video capture enabled. Videos will be stored under: {}", VIDEO_DIR);
            contextOptions.setRecordVideoDir(Paths.get(VIDEO_DIR)).setRecordVideoSize(1280, 720);
        } else {
            logger.info("Video capture disabled.");
        }

        BrowserContext context = browser.newContext(contextOptions);
        if (mobileBrowser) {
            logger.info("UI BrowserContext created successfully. ignoreHTTPSErrors={}, videoCapture={}, mobileBrowserProfile={}", shouldIgnoreHTTPSErrors, mobileBrowserVideo, ACTIVE_MOBILE_BROWSER_PROFILE.get().getName());
        } else {
            logger.info("UI BrowserContext created successfully. ignoreHTTPSErrors={}, videoCapture={}", shouldIgnoreHTTPSErrors, recordVideo);
        }

        return context;
    }

    private static void applyMobileBrowserProfileIfAvailable(Browser.NewContextOptions contextOptions) {
        MobileBrowserProfile profile = ACTIVE_MOBILE_BROWSER_PROFILE.get();
        if (profile == null) {
            return;
        }

        int viewportWidth = profile.getViewportWidth();
        int viewportHeight = profile.getViewportHeight();
        int screenWidth = profile.getScreenWidth();
        int screenHeight = profile.getScreenHeight();
        String orientationMode = MobileBrowserExecutionConfig.getOrientationMode();
        if ("portrait".equals(orientationMode) && viewportWidth > viewportHeight) {
            int tmp = viewportWidth;
            viewportWidth = viewportHeight;
            viewportHeight = tmp;
            tmp = screenWidth;
            screenWidth = screenHeight;
            screenHeight = tmp;
        } else if ("landscape".equals(orientationMode) && viewportHeight > viewportWidth) {
            int tmp = viewportWidth;
            viewportWidth = viewportHeight;
            viewportHeight = tmp;
            tmp = screenWidth;
            screenWidth = screenHeight;
            screenHeight = tmp;
        }

        contextOptions.setViewportSize(viewportWidth, viewportHeight)
                .setScreenSize(screenWidth, screenHeight)
                .setDeviceScaleFactor(profile.getDeviceScaleFactor())
                .setIsMobile(profile.isMobile())
                .setHasTouch(profile.hasTouch());
        if (isNotBlank(profile.getUserAgent())) {
            contextOptions.setUserAgent(profile.getUserAgent());
        }

        logger.info("Applied mobile browser profile [{}] orientationMode={} viewport={}x{} screen={}x{} scale={} touch={}", profile.getName(), orientationMode, viewportWidth, viewportHeight, screenWidth, screenHeight, profile.getDeviceScaleFactor(), profile.hasTouch());
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

    private static boolean getHeadlessMode() {
        String value = ConfigurationProperties.getHeadlessMode();
        return Boolean.parseBoolean(value);
    }

    private static boolean getVideoCapture() {
        String value = ConfigurationProperties.getVideoCapture();
        return Boolean.parseBoolean(value);
    }

    private static boolean getIgnoreHTTPSErrors() {
        String value = ConfigurationProperties.getIgnoreHTTPSErrors();
        if (value != null && !value.trim().isEmpty()) {
            return Boolean.parseBoolean(value.trim());
        } else {
            logger.warn("ignoreHTTPSErrors is missing or blank. Defaulting to false.");
            return false;
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static {
        VIDEO_DIR = "test-output/captured-videos/" + TIMESTAMP;
        MOBILE_BROWSER_VIDEO_DIR = "test-output/mobile-browser-evidence/" + TIMESTAMP + "/videos";
    }

    public static enum BrowserTypeEnum {
        CHROME,
        FIREFOX,
        WEBKIT,
        EDGE;
    }
}
