package com.ptaf.utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ScreenshotHandler is a utility class that centralizes logic for capturing
 * and attaching screenshots to Cucumber Scenario reports. It is intended to
 * be used during test teardown to provide visual evidence of the browser
 * state when tests fail (or for any other status you wish to record).
 *
 * <p>Key responsibilities:
 * - Capture full-page screenshots directly from a Playwright Page.
 * - Capture screenshots of specific elements, including elements inside
 *   nested iframe contexts using Playwright's frameLocator API.
 * - Attach the resulting screenshot bytes to the provided Cucumber Scenario
 *   with a descriptive name and appropriate MIME type for embedding in reports.
 *
 * <p>Notes for testers:
 * - The screenshot methods swallow exceptions and log them; they do not throw.
 * - When using frame-based capture, supply iframe locators in order (iFrame,
 *   iFrame_2, iFrame_3). Supply null for unused nested levels. If iFrame is
 *   null, the method will capture the element from the main document (no iframe).
 */
public class ScreenshotHandler {
    private static final Logger logger = LoggerFactory.getLogger(ScreenshotHandler.class);

    /**
     * Handles the teardown process for the given scenario by attempting to
     * capture a full-page screenshot from the provided Playwright Page and
     * attach it to the Cucumber scenario report.
     *
     * <p>Behaviour:
     * - Captures a full-page screenshot (setFullPage(true)).
     * - Attaches the screenshot to the scenario with MIME type "image/png" and a
     *   descriptive name that includes the scenario name and provided status.
     * - Logs success or any error encountered during screenshot capture.
     *
     * <p>Typical usage is to call this method from an After hook to capture the
     * browser state at the end of a scenario.
     *
     * @param scenario The Cucumber Scenario object providing context for logging
     *                 and for attaching artifacts to the scenario report.
     * @param page     The Playwright Page instance used to capture the screenshot.
     *                 Must be non-null and represent the active browser context.
     * @param status   A string representing the status of the scenario (for example
     *                 "failed", "passed", etc.). This is used in the attachment name.
     *
     *                 Note: The method catches and logs exceptions internally and
     *                 will not rethrow them to the caller.
     */
    public static void handleScenarioTeardown(Scenario scenario, Page page, String status) {
        try {
            // Capture a full-page screenshot into a byte array. This captures the
            // entire scrollable page, not just the visible viewport.
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));

            // Attach the screenshot bytes to the Cucumber scenario report. The
            // MIME type "image/png" ensures the report can render the image.
            // The name includes the scenario name and status to make it easier
            // to identify screenshots in test reports.
            scenario.attach(screenshot, "image/png", "Screenshot of the " + status + ": " + scenario.getName());

            // Log successful capture for convenience when reviewing logs.
            logger.info("Screenshot taken for {} scenario: {}", status, scenario.getName());
        } catch (Exception e) {
            // Catch any unexpected errors during capture/attach and log them.
            // The exception is logged with its stack trace to aid debugging,
            // but not propagated further so teardown does not throw.
            logger.error("Error taking screenshot: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles the teardown process for a scenario where the target for the
     * screenshot may be inside one or more nested iframes. This method attempts
     * to locate the specified element and take a screenshot of that element,
     * supporting up to three levels of nested iframe locators.
     *
     * <p>Behavior and parameters:
     * - If {@code iFrame} is null, the method assumes the target element is in the
     *   main document and uses {@code page.locator(targetLocator)}.
     * - If {@code iFrame} is non-null and subsequent iframe parameters are null,
     *   it uses {@code page.frameLocator(iFrame)} to scope to the first iframe.
     * - If {@code iFrame} and {@code iFrame_2} are non-null and {@code iFrame_3}
     *   is null, it scopes to the second (nested) iframe using chained frameLocator calls.
     * - If all three iframe parameters are provided, it scopes into the third nested iframe.
     *
     * <p>Important:
     * - The {@code targetLocator} should be a valid Playwright selector (CSS, text, etc.)
     *   that identifies the element to be screenshotted in the resolved frame context.
     * - The method captures only the element indicated by {@code targetLocator} (element screenshot),
     *   not a full-page screenshot, when using locator.screenshot(...).
     * - Exceptions are caught and logged; this method will not throw.
     *
     * @param scenario      The Cucumber Scenario to attach the screenshot to and to provide naming context.
     * @param page          The Playwright Page instance used to obtain frame locators and target element locators.
     * @param iFrame        The selector or frame locator string for the first iframe level.
     *                      If null, the method operates on the main document context.
     * @param iFrame_2      The selector or frame locator string for the second (nested) iframe level.
     *                      May be null if not applicable.
     * @param iFrame_3      The selector or frame locator string for the third (nested) iframe level.
     *                      May be null if not applicable.
     * @param targetLocator The Playwright selector for the target element to capture (CSS, text, data-test-id, etc.).
     * @param status        A string representing the scenario status (e.g., "failed", "passed") used in the attachment name.
     */
    public static void handleScenarioTeardownLocator(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String status) {
        try {
            byte[] screenshot = null; // Will hold the resulting screenshot bytes if capture succeeds

            // If no iframe locator is provided, take a screenshot of the element
            // located in the main document using the provided targetLocator.
            if (iFrame == null) {
                // element screenshot from main page context
                screenshot = page.locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
            } else if (iFrame != null && iFrame_2 == null && iFrame_3 == null) {
                // Single iframe: scope to the first iframe and locate the target element inside it.
                // Example: page.frameLocator(iFrame).locator(targetLocator).screenshot(...)
                screenshot = page.frameLocator(iFrame).locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
            } else if (iFrame != null && iFrame_2 != null && iFrame_3 == null) {
                // Two-level nested iframe: scope to the first iframe, then to the nested iframe,
                // and then locate the target element inside the nested iframe.
                screenshot = page.frameLocator(iFrame).frameLocator(iFrame_2).locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
            } else if (iFrame != null && iFrame_2 != null && iFrame_3 != null) {
                // Three-level nested iframe: chain three frameLocator calls to reach the deepest iframe,
                // then locate the target element and capture its screenshot.
                screenshot = page.frameLocator(iFrame).frameLocator(iFrame_2).frameLocator(iFrame_3).locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
            }

            // If a screenshot was captured, attach it to the scenario with the
            // provided status and scenario name. Otherwise, log a warning
            // so testers know that no artifact was produced.
            if (screenshot != null) {
                scenario.attach(screenshot, "image/png", "Screenshot of the " + status + ": " + scenario.getName());
                logger.info("Screenshot taken for {} scenario: {}", status, scenario.getName());
            } else {
                // This may indicate that none of the branches were satisfied or an element/frame
                // was not found. The warning helps test authors identify cases where a screenshot
                // was expected but not produced.
                logger.warn("No screenshot captured for scenario: {}", scenario.getName());
            }

        } catch (Exception e) {
            // Log any errors encountered while attempting to capture element screenshots
            // (including issues resolving frames or locating elements). Stack trace is included
            // to assist in root-cause analysis during test failure investigation.
            logger.error("Error taking screenshot: {}", e.getMessage(), e);
        }
    }
}
