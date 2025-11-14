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
    private static boolean isLastScenarioFeature = false;
    private static boolean isFirstScenarioInFeature = true;

    public Hooks() {
    }

    @Before
    public void setUp(Scenario scenario) {
        scenarioThreadLocal.set(scenario);

        if (scenario.getSourceTagNames().contains("@LastScenario")) {
            isLastScenarioFeature = true;
        }

        if (isLastScenarioFeature && !isFirstScenarioInFeature) {
            logger.info("Reusing browser instance for feature with @LastScenario tag.");
        } else {
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
                logger.error("Error setting up the browser for scenario: {}", e.getMessage());
                throw new RuntimeException("Browser setup failed", e);
            }
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
                logger.info("Skipping browser closure for feature with @LastScenario tag.");
                isFirstScenarioInFeature = false;
            } else {
                // ⚠️ Only change here: call the now-static method
                Hooks.closeBrowserResources();
            }
        }
    }

    // ⚠️ Changed: made public and static so it can be reused from step defs
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
                // This closes all pages, tabs, and frames in the context
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