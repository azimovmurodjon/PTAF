package com.ptaf.ui.mobilebrowser;

import com.microsoft.playwright.Page;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Evidence capture manager for Playwright mobile-browser emulation.
 *
 * <p>
 * This utility class centralizes the logic for deciding when to capture screenshots
 * from a Playwright Page instance during Cucumber scenario execution and for persisting
 * those screenshots to the filesystem. It also optionally attaches screenshots to the
 * Cucumber report when configured to do so.
 * </p>
 *
 * <p>Usage notes for testers:</p>
 * <ul>
 *     <li>Behavior is driven by MobileBrowserExecutionConfig:
 *         screenshotAfterEachScenario(), screenshotOnFailure(), screenshotOnPass(), attachScreenshotsToReport(),
 *         and getEvidenceOutputDirectory().</li>
 *     <li>Only pages associated with mobile-browser profiles are considered. The check is performed
 *         via MobileBrowserProfileRepository.isMobileBrowserProfile(browserName).</li>
 *     <li>Screenshots are written under: {evidenceOutputDirectory}/{RUN_ID}/screenshots/{safeScenarioName}.png</li>
 *     <li>RUN_ID is generated once at class load and is based on the current date/time (format yyyyMMdd_HHmmss).</li>
 * </ul>
 */
public final class MobileBrowserEvidenceManager {
    // Logger for informational and warning messages related to screenshot capture and file operations.
    private static final Logger logger = LoggerFactory.getLogger(MobileBrowserEvidenceManager.class);

    // A run-specific identifier used to group evidence artifacts created during a single test run.
    // Formatted as yyyyMMdd_HHmmss (e.g., 20250629_143501).
    private static final String RUN_ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is a static utility and should not be instantiated. An explicit exception
     * is thrown if instantiation is attempted to make misuse obvious during test development.</p>
     */
    private MobileBrowserEvidenceManager() { throw new IllegalStateException("Utility class"); }

    /**
     * Capture a screenshot for the given Cucumber scenario if the current configuration and state demand it.
     *
     * <p>The method performs a number of defensive checks before attempting to capture a screenshot:
     * <ul>
     *     <li>If the provided Page is null or already closed, nothing is done.</li>
     *     <li>If the Scenario is null, nothing is done.</li>
     *     <li>If the provided browserName does not correspond to a configured mobile-browser profile,
     *         nothing is done. This avoids capturing screenshots for non-mobile browser runs.</li>
     * </ul>
     * </p>
     *
     * <p>When a capture is warranted, the method:
     * <ol>
     *     <li>Takes a full-page screenshot using Playwright's Page.screenshot with fullPage=true.</li>
     *     <li>Creates a file path under the configured evidence output directory, including RUN_ID and a
     *         screenshots subfolder.</li>
     *     <li>Sanitizes the scenario name to create a filesystem-safe filename.</li>
     *     <li>Writes the screenshot bytes to disk and logs the location.</li>
     *     <li>Optionally attaches the screenshot to the Cucumber scenario report if configured to do so.</li>
     * </ol>
     * </p>
     *
     * @param page the Playwright Page from which to capture the screenshot; may be null
     * @param scenario the Cucumber Scenario instance associated with the current test; may be null
     * @param browserName the name of the browser/profile used for the test run; used to verify mobile profile
     */
    public static void captureScenarioScreenshotIfConfigured(Page page, Scenario scenario, String browserName) {
        // Defensive preconditions: ensure we have a valid page and scenario, and that the browser profile
        // corresponds to a mobile browser. If any of these checks fail, we skip capturing.
        if (page == null || page.isClosed() || scenario == null || !MobileBrowserProfileRepository.isMobileBrowserProfile(browserName)) {
            return;
        }

        // Determine whether a screenshot should be captured based on configured policies:
        // - Always capture after each scenario if configured
        // - Capture on failure if configured and the scenario failed
        // - Capture on pass if configured and the scenario passed
        boolean shouldCapture = MobileBrowserExecutionConfig.screenshotAfterEachScenario()
                || (scenario.isFailed() && MobileBrowserExecutionConfig.screenshotOnFailure())
                || (!scenario.isFailed() && MobileBrowserExecutionConfig.screenshotOnPass());
        if (!shouldCapture) {
            // If none of the configured conditions are met, skip capture.
            return;
        }

        try {
            // Request a full-page screenshot from Playwright. The returned byte[] represents a PNG image.
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));

            // Sanitize the scenario name to create a filesystem-friendly filename component.
            String safeScenarioName = safeName(scenario.getName());

            // Build the target output path: {evidenceDirectory}/{RUN_ID}/screenshots/{safeScenarioName}.png
            Path output = Path.of(MobileBrowserExecutionConfig.getEvidenceOutputDirectory(), RUN_ID, "screenshots", safeScenarioName + ".png");

            // Ensure the parent directories exist before writing the file.
            Files.createDirectories(output.getParent());

            // Persist the screenshot bytes to disk.
            Files.write(output, screenshot);

            // Log the absolute path so testers and automation pipelines can locate the artifact.
            logger.info("Mobile browser screenshot saved to [{}]", output.toAbsolutePath());

            // Optionally attach the screenshot to the Cucumber scenario report (inline/embedded).
            if (MobileBrowserExecutionConfig.attachScreenshotsToReport()) {
                // The attach call expects MIME type and a name; use a consistent prefix to make attachments searchable.
                scenario.attach(screenshot, "image/png", "mobile-browser-screenshot-" + safeScenarioName);
            }
        } catch (Exception e) {
            // Catch-all to avoid letting screenshot failures break test execution; log a warning for troubleshooting.
            logger.warn("Unable to capture mobile browser screenshot: {}", e.getMessage());
        }
    }

    /**
     * Produce a filesystem-safe name derived from the provided value.
     *
     * <p>The method trims whitespace, substitutes any characters that are not letters, digits,
     * period (.), underscore (_) or dash (-) with underscores, and returns a default name
     * "scenario" when the input is null or empty after trimming.</p>
     *
     * @param value the original name (e.g., Cucumber scenario name)
     * @return a sanitized name suitable for use as a filename component
     */
    private static String safeName(String value) {
        if (value == null || value.trim().isEmpty()) return "scenario";
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
