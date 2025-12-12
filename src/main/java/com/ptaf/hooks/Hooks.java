//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.ptaf.hooks;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.ptaf.ui.pages.PageCommonMethods;
import com.ptaf.utils.BrowserFactory;
import com.ptaf.utils.ConfigurationProperties;
import com.ptaf.utils.BrowserFactory.BrowserTypeEnum;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hooks {
    private static final ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> contextThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Page> pageThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Scenario> scenarioThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<PageCommonMethods> pageCommonMethodsThreadLocal = new ThreadLocal<>();
    private static final Logger logger = LoggerFactory.getLogger(Hooks.class);

    // Reuse flags
    private static boolean isLastScenarioFeature = false;
    private static boolean isFirstScenarioInFeature = true;

    public Hooks() {
    }

    @Before
    public void setUp(Scenario scenario) {
        scenarioThreadLocal.set(scenario);

        // If this scenario has @LastScenario tag – mark it
        if (scenario.getSourceTagNames().contains("@LastScenario")) {
            isLastScenarioFeature = true;
        }

        // Check if we can safely reuse existing browser (for LastScenario feature logic)
        Browser existingBrowser = browserThreadLocal.get();
        BrowserContext existingContext = contextThreadLocal.get();
        Page existingPage = pageThreadLocal.get();

        boolean canReuse =
                isLastScenarioFeature &&
                        !isFirstScenarioInFeature &&
                        existingBrowser != null &&
                        existingContext != null &&
                        existingPage != null &&
                        !existingPage.isClosed();

        if (canReuse) {
            logger.info("Reusing browser instance for feature with @LastScenario tag. Scenario: {}", scenario.getName());
            return;
        }

        // Otherwise create a brand new browser/context/page
        try {
            String browserName = ConfigurationProperties.getBrowser();
            BrowserFactory.BrowserTypeEnum browserTypeEnum;

            switch (browserName.toUpperCase()) {
                case "CHROME" -> browserTypeEnum = BrowserTypeEnum.CHROME;
                case "FIREFOX" -> browserTypeEnum = BrowserTypeEnum.FIREFOX;
                case "WEBKIT" -> browserTypeEnum = BrowserTypeEnum.WEBKIT;
                case "EDGE" -> browserTypeEnum = BrowserTypeEnum.EDGE;
                default -> throw new IllegalArgumentException("Unsupported browser type: " + browserName);
            }

            Browser browser = BrowserFactory.createBrowser(browserTypeEnum);
            browserThreadLocal.set(browser);

            BrowserContext context = BrowserFactory.createContextWithVideo(browser);
            contextThreadLocal.set(context);

            Page page = context.newPage();
            pageThreadLocal.set(page);

            PageCommonMethods pageCommonMethods = new PageCommonMethods(page);
            pageCommonMethodsThreadLocal.set(pageCommonMethods);

            logger.info("Browser setup completed for scenario: {}", scenario.getName());
        } catch (Exception e) {
            logger.error("Error setting up the browser for scenario: {}", e.getMessage(), e);
            throw new RuntimeException("Browser setup failed", e);
        }
    }

    @After
    public void tearDown(Scenario scenario) {
        try {
            if (scenario.getStatus() == Status.PASSED) {
                PageCommonMethods pageCommonMethods = pageCommonMethodsThreadLocal.get();
                if (pageCommonMethods != null) {
                    pageCommonMethods.finalizeScenario();
                }
            }
        } catch (Exception e) {
            logger.error("Error during scenario teardown: {}", e.getMessage(), e);
        } finally {
            if (isLastScenarioFeature) {
                // Keep current behavior: for @LastScenario feature, do not auto-close here
                logger.info("Skipping browser closure for feature with @LastScenario tag.");
                isFirstScenarioInFeature = false;
            } else {
                // For normal runs, close and reset everything
                Hooks.closeBrowserResources();
            }
        }
    }

    /**
     * Global "kill switch" – can be called from:
     *  - @After (normal scenarios)
     *  - Step definition: "Then we close all browsers"
     *
     * It:
     *  - Closes Page, Context, Browser
     *  - Clears ThreadLocals
     *  - Resets LastScenario flags so the next scenario will start a fresh browser
     */
    public static void closeBrowserResources() {
        Exception e;

        try {
            Page page = pageThreadLocal.get();
            if (page != null && !page.isClosed()) {
                page.close();
            }
        } catch (Exception ex) {
            e = ex;
            logger.error("Error closing the page: {}", e.getMessage(), e);
        } finally {
            pageThreadLocal.remove();
        }

        try {
            BrowserContext context = contextThreadLocal.get();
            if (context != null) {
                // This closes all tabs and frames inside this context
                context.close();
            }
        } catch (Exception ex) {
            e = ex;
            logger.error("Error closing the browser context: {}", e.getMessage(), e);
        } finally {
            contextThreadLocal.remove();
        }

        try {
            Browser browser = browserThreadLocal.get();
            if (browser != null) {
                browser.close();
                logger.info("Browser closed.");
            }
        } catch (Exception ex) {
            e = ex;
            logger.error("Error closing the browser: {}", e.getMessage(), e);
        } finally {
            browserThreadLocal.remove();
        }

        // 🔁 IMPORTANT:
        // After a full manual close we want the next scenario (including one with @LastScenario)
        // to behave as a fresh start.
        isLastScenarioFeature = false;
        isFirstScenarioInFeature = true;
    }

    public static Page getPage() {
        Page page = pageThreadLocal.get();
        if (page != null && !page.isClosed()) {
            return page;
        } else {
            throw new IllegalStateException("The page is closed or not initialized.");
        }
    }

    public static Browser getBrowser() {
        Browser browser = browserThreadLocal.get();
        if (browser == null) {
            throw new IllegalStateException("The browser is not initialized.");
        } else {
            return browser;
        }
    }

    public static Scenario getCurrentScenario() {
        return scenarioThreadLocal.get();
    }

    public static void setCurrentScenario(Scenario scenario) {
        scenarioThreadLocal.set(scenario);
    }

    public static BrowserContext getContext() {
        BrowserContext context = contextThreadLocal.get();
        if (context == null) {
            throw new IllegalStateException("The browser context is not initialized.");
        } else {
            return context;
        }
    }
}