package com.ptaf.ui.mobilebrowser;

import com.microsoft.playwright.Page;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Evidence capture manager for Playwright mobile-browser emulation. */
public final class MobileBrowserEvidenceManager {
    private static final Logger logger = LoggerFactory.getLogger(MobileBrowserEvidenceManager.class);
    private static final String RUN_ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    private MobileBrowserEvidenceManager() { throw new IllegalStateException("Utility class"); }

    public static void captureScenarioScreenshotIfConfigured(Page page, Scenario scenario, String browserName) {
        if (page == null || page.isClosed() || scenario == null || !MobileBrowserProfileRepository.isMobileBrowserProfile(browserName)) {
            return;
        }
        boolean shouldCapture = MobileBrowserExecutionConfig.screenshotAfterEachScenario()
                || (scenario.isFailed() && MobileBrowserExecutionConfig.screenshotOnFailure())
                || (!scenario.isFailed() && MobileBrowserExecutionConfig.screenshotOnPass());
        if (!shouldCapture) {
            return;
        }
        try {
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            String safeScenarioName = safeName(scenario.getName());
            Path output = Path.of(MobileBrowserExecutionConfig.getEvidenceOutputDirectory(), RUN_ID, "screenshots", safeScenarioName + ".png");
            Files.createDirectories(output.getParent());
            Files.write(output, screenshot);
            logger.info("Mobile browser screenshot saved to [{}]", output.toAbsolutePath());
            if (MobileBrowserExecutionConfig.attachScreenshotsToReport()) {
                scenario.attach(screenshot, "image/png", "mobile-browser-screenshot-" + safeScenarioName);
            }
        } catch (Exception e) {
            logger.warn("Unable to capture mobile browser screenshot: {}", e.getMessage());
        }
    }

    private static String safeName(String value) {
        if (value == null || value.trim().isEmpty()) return "scenario";
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
