//package com.ptaf.utils;
//
//import com.microsoft.playwright.Locator;
//import com.microsoft.playwright.Page;
//import io.cucumber.java.Scenario;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * ScenarioUtil is a utility class that provides methods for managing
// * scenarios during test execution, particularly focusing on teardown processes.
// * This class offers functionality to capture and attach screenshots
// * to scenario reports in cases of test failures or specific conditions.
// */
//public class ScenarioUtil {
//    private static final Logger logger = LoggerFactory.getLogger(ScenarioUtil.class);
//
//    /**
//     * Handles the teardown process for the given scenario.
//     * This method attempts to capture a full-page screenshot
//     * and attaches it to the scenario report, allowing for better
//     * debugging in the event of test failures.
//     *
//     * @param scenario The Cucumber Scenario object providing context for logging
//     *                 and information about the test case being executed.
//     * @param page     The Playwright Page instance from which to capture the screenshot.
//     * @param status   A string representing the status of the scenario (e.g., "failed", "passed"),
//     *                 used for tagging the screenshot.
//     */
//    public static void handleScenarioTeardown(Scenario scenario, Page page, String status) {
//        try {
//            // Capture a full-page screenshot and store it in a byte array
//            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
//
//            // Attach the screenshot to the scenario report, with a descriptive name
//            scenario.attach(screenshot, "image/png", "Screenshot of the " + status + ": " + scenario.getName());
//            logger.info("Screenshot taken for {} scenario: {}", status, scenario.getName()); // Log info about the captured screenshot
//        } catch (Exception e) {
//            // Log any errors that occur during the screenshot capturing process for further analysis
//            logger.error("Error taking screenshot: {}", e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Handles the teardown process for a scenario that involves iframes.
//     * Depending on the presence of iframes, this method captures screenshots
//     * from different contexts (full-page or specific iframe elements) and
//     * attaches them to the scenario report.
//     *
//     * @param scenario   The current Cucumber Scenario being executed,
//     *                   which provides context for logging and report attachments.
//     * @param page       The Playwright Page instance to interact with,
//     *                   allowing us to access iframes and elements.
//     * @param iFrame     The identifier for the first iframe, which will be targeted for screenshots.
//     * @param iFrame_2   The identifier for the second iframe, which allows for nested targeting of screenshots.
//     * @param iFrame_3   The identifier for the third iframe, enabling further nested targeting.
//     * @param targetLocator The selector for the specific element within the iframe from which to capture the screenshot.
//     * @param status     A string that represents the status of the scenario (e.g., "failed", "passed").
//     */
//    public static void handleScenarioTeardownLocator(Scenario scenario, Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator, String status) {
//        try {
//            byte[] screenshot = null; // Variable to hold the screenshot byte array
//
//            // Check if no iframes are provided; in this case, capture a full-page screenshot
//            if (iFrame == null) {
//                screenshot = page.locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
//            } else if (iFrame != null && iFrame_2 == null && iFrame_3 == null) {
//                // Capture screenshot from the first specified iframe
//                screenshot = page.frameLocator(iFrame).locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
//            } else if (iFrame != null && iFrame_2 != null && iFrame_3 == null) {
//                // Capture screenshot from an element located within the second nested iframe
//                screenshot = page.frameLocator(iFrame).frameLocator(iFrame_2).locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
//            } else if (iFrame != null && iFrame_2 != null && iFrame_3 != null) {
//                // Capture screenshot from an element in the third nested iframe
//                screenshot = page.frameLocator(iFrame).frameLocator(iFrame_2).frameLocator(iFrame_3).locator(targetLocator).screenshot(new Locator.ScreenshotOptions());
//            }
//
//            // Verify that a screenshot was captured successfully before attaching it
//            if (screenshot != null) {
//                scenario.attach(screenshot, "image/png", "Screenshot of the " + status + ": " + scenario.getName());
//                logger.info("Screenshot taken for {} scenario: {}", status, scenario.getName()); // Log the successful capture of the screenshot
//            } else {
//                // Log a warning if no screenshot was captured, indicating potential issues in the process
//                logger.warn("No screenshot captured for scenario: {}", scenario.getName());
//            }
//
//        } catch (Exception e) {
//            // Log any exceptions that occur during the process of capturing screenshots
//            logger.error("Error taking screenshot: {}", e.getMessage(), e);
//        }
//    }
//}