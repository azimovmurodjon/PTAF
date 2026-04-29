package com.ptaf.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.HttpCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * BrowserFactory is responsible for creating Playwright Browser and BrowserContext instances.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class centralizes browser creation logic for UI automation execution.
 * Keeping browser and context configuration in one place helps maintain consistency
 * across all projects, teams, and test suites using the PTAF framework.
 * </p>
 *
 * <p>
 * Supported Browsers:
 * Chrome, Firefox, WebKit, and Microsoft Edge are supported through BrowserTypeEnum.
 * </p>
 *
 * <p>
 * HTTPS / SSL Handling:
 * BrowserContext is configured using the framework-level "ignoreHTTPSErrors"
 * value from config.yml. This allows teams to control SSL certificate validation
 * without modifying source code.
 * </p>
 *
 * <p>
 * HTTP Authentication:
 * If the JVM system properties service.username and service.password are provided,
 * they are applied to the BrowserContext using Playwright HttpCredentials.
 * This keeps the main branch authentication behavior intact.
 * </p>
 *
 * <p>
 * Important:
 * The ignoreHTTPSErrors setting must be applied at the BrowserContext level.
 * This is the correct Playwright configuration point for UI automation.
 * </p>
 */
public final class BrowserFactory {

    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);

    /**
     * Timestamp used to keep each execution's video recordings separated.
     * This preserves the main branch behavior where videos are stored by run timestamp.
     */
    private static final String TIMESTAMP =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    /**
     * Centralized directory where Playwright video recordings are stored.
     */
    private static final String VIDEO_DIR = "test-output/captured-videos/" + TIMESTAMP;

    /**
     * BrowserFactory is a utility class and should not be instantiated.
     */
    private BrowserFactory() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Enum representing supported browser types for UI automation execution.
     */
    public enum BrowserTypeEnum {
        CHROME,
        FIREFOX,
        WEBKIT,
        EDGE
    }

    /**
     * Creates and launches a Playwright Browser instance based on the requested browser type.
     *
     * <p>
     * Browser represents the browser process. Test-level configuration such as
     * SSL handling, video recording, viewport, storage state, HTTP credentials,
     * and tracing should be applied on BrowserContext.
     * </p>
     *
     * @param browserTypeEnum Browser type requested by framework configuration or runner.
     * @return launched Playwright Browser instance.
     */
    public static Browser createBrowser(BrowserTypeEnum browserTypeEnum) {
        Playwright playwright = Playwright.create();

        return switch (browserTypeEnum) {
            case CHROME -> launchChromium(playwright.chromium(), "CHROME", null);
            case FIREFOX -> launchBrowser(playwright.firefox());
            case WEBKIT -> launchBrowser(playwright.webkit());
            case EDGE -> launchChromium(playwright.chromium(), "EDGE", "msedge");
        };
    }

    /**
     * Launches Firefox/WebKit browser types.
     *
     * @param browserType Playwright BrowserType instance.
     * @return launched Playwright Browser instance.
     */
    private static Browser launchBrowser(BrowserType browserType) {
        boolean headless = getHeadlessMode();

        logger.info(
                "Launching browser: {} with headless mode: {}",
                browserType.name().toUpperCase(),
                headless
        );

        return browserType.launch(new BrowserType.LaunchOptions()
                .setHeadless(headless));
    }

    /**
     * Launches Chromium-based browsers with enterprise-friendly SSL flags.
     *
     * <p>
     * BrowserContext.setIgnoreHTTPSErrors(true) is the main SSL bypass setting.
     * The Chromium launch args below are added as a defensive enterprise safeguard
     * for some internal QA/UAT environments where Chromium still shows certificate
     * interstitials due to local/corporate certificate handling.
     * </p>
     *
     * @param browserType Playwright Chromium browser type.
     * @param browserName browser display name.
     * @param channel optional browser channel, such as msedge.
     * @return launched Browser instance.
     */
    private static Browser launchChromium(BrowserType browserType, String browserName, String channel) {
        boolean headless = getHeadlessMode();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless);

        if (channel != null && !channel.trim().isEmpty()) {
            launchOptions.setChannel(channel);
        }

        if (shouldIgnoreHTTPSErrors) {
            launchOptions.setArgs(Arrays.asList(
                    "--ignore-certificate-errors",
                    "--allow-insecure-localhost",
                    "--disable-web-security"
            ));

            logger.info(
                    "Launching {} with SSL bypass launch arguments enabled because ignoreHTTPSErrors=true.",
                    browserName
            );
        }

        logger.info(
                "Launching {} with headless mode: {}, ignoreHTTPSErrors: {}",
                browserName,
                headless,
                shouldIgnoreHTTPSErrors
        );

        return browserType.launch(launchOptions);
    }

    /**
     * Creates a BrowserContext with framework-level UI execution settings.
     *
     * <p>
     * Enterprise Configuration Included:
     * </p>
     *
     * <ul>
     *     <li>Optional video recording based on framework configuration.</li>
     *     <li>Configurable HTTPS / SSL certificate handling using ignoreHTTPSErrors.</li>
     *     <li>Optional HTTP authentication using service.username and service.password.</li>
     * </ul>
     *
     * <p>
     * SSL Bypass Purpose:
     * Many QA, DEV, SIT, UAT, and internal enterprise environments use certificates
     * that may not be trusted by the local machine. Enabling ignoreHTTPSErrors allows
     * UI tests to continue without certificate warning pages blocking automation.
     * </p>
     *
     * <p>
     * HTTP Authentication Purpose:
     * Some internal environments require basic HTTP authentication before the
     * application page loads. If service.username and service.password are passed
     * as JVM system properties, they are applied here at the BrowserContext level.
     * </p>
     *
     * <p>
     * Security Note:
     * For production validation, set ignoreHTTPSErrors to false in config.yml to enforce
     * strict SSL certificate validation.
     * </p>
     *
     * @param browser launched Browser instance.
     * @return configured BrowserContext instance.
     */
    public static BrowserContext createContextWithVideo(Browser browser) {
        boolean recordVideo = getVideoCapture();
        boolean shouldIgnoreHTTPSErrors = getIgnoreHTTPSErrors();

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(shouldIgnoreHTTPSErrors);

        logger.info("Creating UI BrowserContext. ignoreHTTPSErrors={}", shouldIgnoreHTTPSErrors);

        applyHttpCredentialsIfAvailable(contextOptions);

        if (recordVideo) {
            logger.info("Video capture enabled. Videos will be stored under: {}", VIDEO_DIR);

            contextOptions.setRecordVideoDir(Paths.get(VIDEO_DIR))
                    .setRecordVideoSize(1280, 720);
        } else {
            logger.info("Video capture disabled.");
        }

        BrowserContext context = browser.newContext(contextOptions);

        logger.info(
                "UI BrowserContext created successfully. ignoreHTTPSErrors={}, videoCapture={}",
                shouldIgnoreHTTPSErrors,
                recordVideo
        );

        return context;
    }

    /**
     * Applies HTTP basic authentication credentials to BrowserContext options
     * when service.username and service.password are available as JVM system properties.
     *
     * <p>
     * This method restores the main branch logic:
     * </p>
     *
     * <pre>
     * -Dservice.username=yourUsername
     * -Dservice.password=yourPassword
     * </pre>
     *
     * @param contextOptions Browser context options that will receive credentials.
     */
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

    /**
     * Reads headless mode dynamically from config.yml.
     *
     * @return true when headless is true.
     */
    private static boolean getHeadlessMode() {
        String value = ConfigurationProperties.getHeadlessMode();
        return Boolean.parseBoolean(value);
    }

    /**
     * Reads video capture dynamically from config.yml.
     *
     * @return true when videoCapture is true.
     */
    private static boolean getVideoCapture() {
        String value = ConfigurationProperties.getVideoCapture();
        return Boolean.parseBoolean(value);
    }

    /**
     * Reads SSL bypass dynamically from config.yml.
     *
     * @return true when ignoreHTTPSErrors is true.
     */
    private static boolean getIgnoreHTTPSErrors() {
        String value = ConfigurationProperties.getIgnoreHTTPSErrors();

        if (value == null || value.trim().isEmpty()) {
            logger.warn("ignoreHTTPSErrors is missing or blank. Defaulting to false.");
            return false;
        }

        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Checks whether a string has real text.
     *
     * @param value input string.
     * @return true when value is not null and not blank.
     */
    private static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}