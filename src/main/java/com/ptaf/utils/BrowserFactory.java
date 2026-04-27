package com.ptaf.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

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
 * Important:
 * The ignoreHTTPSErrors setting must be applied at the BrowserContext level.
 * This is the correct Playwright configuration point for UI automation.
 * </p>
 */
public class BrowserFactory {

    private static final Logger logger = LoggerFactory.getLogger(BrowserFactory.class);

    /**
     * Reads browser headless execution mode from framework configuration.
     * Expected config.yml values: "true" or "false".
     */
    private static final String headlessMode = ConfigurationProperties.getHeadlessMode();

    /**
     * Reads video capture setting from framework configuration.
     * Expected config.yml values: "true" or "false".
     */
    private static final String videoCapture = ConfigurationProperties.getVideoCapture();

    /**
     * Reads HTTPS / SSL certificate error handling setting from framework configuration.
     * Expected config.yml values: "true" or "false".
     */
    private static final String ignoreHTTPSErrors = ConfigurationProperties.getIgnoreHTTPSErrors();

    /**
     * Centralized directory where Playwright video recordings are stored.
     */
    private static final String VIDEO_DIR = "test-output/captured-videos";

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
     * SSL handling, video recording, viewport, storage state, and tracing should
     * be applied on BrowserContext.
     * </p>
     *
     * @param browserTypeEnum Browser type requested by framework configuration or runner.
     * @return launched Playwright Browser instance.
     */
    public static Browser createBrowser(BrowserTypeEnum browserTypeEnum) {
        Playwright playwright = Playwright.create();

        return switch (browserTypeEnum) {
            case CHROME -> {
                BrowserType browserType = playwright.chromium();
                yield launchBrowser(browserType);
            }
            case FIREFOX -> {
                BrowserType browserType = playwright.firefox();
                yield launchBrowser(browserType);
            }
            case WEBKIT -> {
                BrowserType browserType = playwright.webkit();
                yield launchBrowser(browserType);
            }
            case EDGE -> {
                boolean headless = Boolean.parseBoolean(headlessMode);

                logger.info("Launching Microsoft Edge with headless mode: {}", headless);

                yield playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setChannel("msedge")
                        .setHeadless(headless));
            }
        };
    }

    /**
     * Launches the specified Playwright browser type with framework-level launch options.
     *
     * @param browserType Playwright BrowserType instance.
     * @return launched Playwright Browser instance.
     */
    private static Browser launchBrowser(BrowserType browserType) {
        boolean headless = Boolean.parseBoolean(headlessMode);

        logger.info("Launching browser: {} with headless mode: {}",
                browserType.name().toUpperCase(),
                headless);

        return browserType.launch(new BrowserType.LaunchOptions()
                .setHeadless(headless));
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
     * Security Note:
     * For production validation, set ignoreHTTPSErrors to false in config.yml to enforce
     * strict SSL certificate validation.
     * </p>
     *
     * @param browser launched Browser instance.
     * @return configured BrowserContext instance.
     */
    public static BrowserContext createContextWithVideo(Browser browser) {
        boolean recordVideo = Boolean.parseBoolean(videoCapture);
        boolean shouldIgnoreHTTPSErrors = Boolean.parseBoolean(ignoreHTTPSErrors);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setIgnoreHTTPSErrors(shouldIgnoreHTTPSErrors);

        logger.info("UI ignoreHTTPSErrors value from config.yml: {}", shouldIgnoreHTTPSErrors);

        if (recordVideo) {
            logger.info("Video capture enabled. Videos will be stored under: {}", VIDEO_DIR);

            contextOptions.setRecordVideoDir(Paths.get(VIDEO_DIR))
                    .setRecordVideoSize(1280, 720);
        } else {
            logger.info("Video capture disabled.");
        }

        return browser.newContext(contextOptions);
    }
}