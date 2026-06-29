package com.ptaf.mobile.evidence;

import com.ptaf.mobile.config.MobileConfigurationProperties;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.screenrecording.CanRecordScreen;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Centralized screenshot and video evidence management for native Appium runs.
 *
 * <p>
 * This utility class provides methods to:
 * - Start and stop native screen recording (via Appium's CanRecordScreen) if enabled in configuration.
 * - Capture screenshots on scenario end (pass/fail/always based on configuration), and also for immediate
 *   assertion failures.
 * - Optionally attach screenshots and videos to the active Cucumber scenario report.
 * - Persist media files to disk under a consistent directory layout.
 * </p>
 *
 * <p>
 * Key behaviors and configuration toggles (driven by MobileConfigurationProperties):
 * - videoRecordingEnabled(): master switch for starting/stopping Appium screen recording.
 * - videoOnFailureOnly(): when true, saved videos will only be persisted for failed scenarios.
 * - attachVideoToReport(): when true, saved videos will also be attached to the Cucumber report.
 * - screenshotOnFailure(): capture a screenshot on scenario failure.
 * - screenshotOnPass(): capture a screenshot on scenario pass (when not failing).
 * - screenshotAfterEachScenario(): capture screenshots after every scenario regardless of pass/fail.
 * - attachScreenshotsToReport(): when true, screenshots are attached to the Cucumber report.
 * </p>
 *
 * <p>
 * Notes for testers and consumers:
 * - All persisted media files are written under the configured evidence output directory,
 *   in a subfolder named by the run id (timestamp) and media type (e.g. "screenshots", "videos").
 * - Screenshots are saved as PNG, videos are saved as MP4.
 * - The class uses a ThreadLocal to maintain the currently active Cucumber Scenario for helper methods
 *   that need to attach to the scenario without an explicit parameter.
 * - This is a utility class and cannot be instantiated.
 * </p>
 *
 * <p>
 * Failure screenshots are intentionally captured even when optional pass/every-scenario
 * screenshots are disabled, so native mobile failures are visible in the same way as UI failures.
 * </p>
 */
public final class MobileEvidenceManager {
    // Logger for informational and warning messages.
    private static final Logger logger = LoggerFactory.getLogger(MobileEvidenceManager.class);

    // RUN_ID is stamped once per JVM run, so all evidence for a single execution lands under the same timestamped folder.
    private static final String RUN_ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    // ThreadLocal to keep track of the active Cucumber Scenario per thread.
    // Useful for helper calls that do not receive the Scenario explicitly.
    private static final ThreadLocal<Scenario> CURRENT_SCENARIO = new ThreadLocal<>();

    // Private constructor to prevent instantiation of this utility class.
    private MobileEvidenceManager() { throw new IllegalStateException("Utility class"); }

    /**
     * Sets the current Cucumber Scenario for the calling thread.
     *
     * @param scenario the active Scenario to associate with the current thread
     */
    public static void setCurrentScenario(Scenario scenario) { CURRENT_SCENARIO.set(scenario); }

    /**
     * Clears the current thread's associated Scenario.
     * Should be invoked when the scenario execution finishes to avoid leaking references.
     */
    public static void clearCurrentScenario() { CURRENT_SCENARIO.remove(); }

    /**
     * Returns the Scenario associated with the current thread, or null if none.
     *
     * @return the current thread's Scenario, or null
     */
    public static Scenario getCurrentScenario() { return CURRENT_SCENARIO.get(); }

    /**
     * Starts native video recording on the provided Appium driver if video recording is enabled in configuration
     * and if the driver supports the CanRecordScreen capability.
     *
     * <p>Any exceptions are logged and do not abort execution.</p>
     *
     * @param driver the active AppiumDriver instance; may implement CanRecordScreen
     */
    public static void startVideoIfEnabled(AppiumDriver driver) {
        // Fast exit if video recording is globally disabled.
        if (!MobileConfigurationProperties.videoRecordingEnabled()) return;

        // Ensure the driver supports screen recording.
        if (!(driver instanceof CanRecordScreen)) {
            logger.warn("The active Appium driver does not support screen recording.");
            return;
        }

        try {
            // Start recording; Appium encapsulates the recording on the server side.
            ((CanRecordScreen) driver).startRecordingScreen();
            logger.info("Native mobile screen recording started.");
        } catch (Exception e) {
            // Any failure to start recording is non-fatal for the test run.
            logger.warn("Unable to start native mobile screen recording: {}", e.getMessage());
        }
    }

    /**
     * Captures a screenshot after a scenario finishes depending on configuration and scenario outcome.
     *
     * <p>
     * Behavior:
     * - If scenario is failed and screenshotOnFailure() is true, always capture and force attach to report.
     * - Otherwise capture when screenshotAfterEachScenario() is true, or when the scenario passed and screenshotOnPass() is true.
     * </p>
     *
     * @param driver   the active AppiumDriver used to fetch the screenshot bytes
     * @param scenario the finished Cucumber Scenario for which to capture/save/attach the screenshot
     */
    public static void captureScenarioScreenshotIfConfigured(AppiumDriver driver, Scenario scenario) {
        if (driver == null || scenario == null) return;

        // If the scenario failed and failure-screenshots are enabled, capture and force attach.
        if (scenario.isFailed() && MobileConfigurationProperties.screenshotOnFailure()) {
            captureScreenshot(driver, scenario, "failure", true);
            return;
        }

        // Decide whether we should capture based on configuration and pass/fail status.
        boolean shouldCapture = MobileConfigurationProperties.screenshotAfterEachScenario()
                || (!scenario.isFailed() && MobileConfigurationProperties.screenshotOnPass());
        if (shouldCapture) captureScreenshot(driver, scenario, scenario.isFailed() ? "failure" : "scenario", false);
    }

    /**
     * Captures a screenshot immediately when an assertion fails.
     *
     * <p>
     * This method is intended to be invoked from assertion helpers to persist a screenshot tied to
     * the current scenario. It writes the screenshot to disk and optionally attaches it to the report.
     * </p>
     *
     * @param driver      the AppiumDriver to capture the screenshot from
     * @param failureName a short name identifying the failing assertion/step
     * @param details     human-readable details about the failure to log to the scenario
     */
    public static void captureAssertionFailureScreenshot(AppiumDriver driver, String failureName, String details) {
        Scenario scenario = CURRENT_SCENARIO.get();
        // Require both a driver and an active scenario to capture and attach.
        if (driver == null || scenario == null) return;
        try {
            // Compose a safe filename based on scenario name and provided failureName.
            String safeName = safeName(scenario.getName() + "_" + failureName);

            // Grab the screenshot bytes from the driver.
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);

            // Persist the screenshot under the "screenshots" directory for assertion failures.
            Path output = outputDir("screenshots").resolve(safeName + ".png");
            Files.write(output, screenshot);
            logger.info("Native mobile assertion failure screenshot saved to [{}]", output.toAbsolutePath());

            // Add readable logs to the scenario for traceability in the report.
            scenario.log("Native mobile assertion failure: " + details);
            scenario.log("Screenshot: " + output.toAbsolutePath());

            // Optionally attach the screenshot bytes to the Cucumber report.
            if (MobileConfigurationProperties.attachScreenshotsToReport()) {
                scenario.attach(screenshot, "image/png", "mobile-assertion-failure-" + safeName);
            }
        } catch (Exception e) {
            // Non-fatal: log and continue.
            logger.warn("Unable to capture immediate native mobile assertion failure screenshot: {}", e.getMessage());
        }
    }

    /**
     * Internal helper that captures a screenshot, writes it to disk, logs to the scenario, and optionally attaches it.
     *
     * @param driver      the AppiumDriver used to obtain the screenshot bytes
     * @param scenario    the Scenario associated with the screenshot
     * @param type        a short suffix indicating screenshot type (e.g. "failure", "scenario")
     * @param forceAttach if true, attach the screenshot to the scenario report regardless of configuration
     */
    private static void captureScreenshot(AppiumDriver driver, Scenario scenario, String type, boolean forceAttach) {
        try {
            // Acquire screenshot bytes from the driver.
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);

            // Produce a filesystem-safe name for the scenario + type.
            String safeScenarioName = safeName(scenario.getName() + "_" + type);

            // Persist under target-output/screenshots to keep all scenario screenshots together.
            Path output = outputDir("target-output/screenshots").resolve(safeScenarioName + ".png");
            Files.write(output, screenshot);
            logger.info("Native mobile screenshot saved to [{}]", output.toAbsolutePath());

            // Log a human-readable entry into the scenario (visible in many Cucumber reports).
            scenario.log("Native mobile screenshot: " + output.toAbsolutePath());

            // Attach bytes to the scenario if forced or configured to do so.
            if (forceAttach || MobileConfigurationProperties.attachScreenshotsToReport()) {
                scenario.attach(screenshot, "image/png", "mobile-screenshot-" + safeScenarioName);
            }
        } catch (Exception e) {
            // Non-fatal: record inability to capture the screenshot.
            logger.warn("Unable to capture native mobile screenshot: {}", e.getMessage());
        }
    }

    /**
     * Explicitly captures a named screenshot and attaches it to the active Cucumber Scenario when possible.
     *
     * <p>
     * This method uses the ThreadLocal current scenario if one is set. It will always write the screenshot to disk,
     * and will attach it to the report only if a scenario is available and the attachScreenshotsToReport flag is enabled.
     * </p>
     *
     * @param driver         the AppiumDriver instance used to capture the screenshot
     * @param screenshotName optional friendly name for the screenshot file; will be sanitized for filesystem use
     */
    public static void captureNamedScreenshot(AppiumDriver driver, String screenshotName) {
        Scenario scenario = getCurrentScenario();
        if (driver == null) return;

        try {
            // Normalize provided name and sanitize illegal characters.
            String baseName = screenshotName == null || screenshotName.trim().isEmpty()
                    ? "mobile_screenshot"
                    : screenshotName;
            String safeName = baseName.trim().replaceAll("[^A-Za-z0-9._-]", "_");

            // Capture and persist the screenshot.
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            Path output = outputDir("target-output/screenshots").resolve(safeName + ".png");
            Files.write(output, screenshot);

            // If a scenario is active, log and optionally attach the screenshot to the report.
            if (scenario != null) {
                scenario.log("Native mobile screenshot: " + output.toAbsolutePath());
                if (MobileConfigurationProperties.attachScreenshotsToReport()) {
                    scenario.attach(screenshot, "image/png", "mobile-screenshot-" + safeName);
                }
            }
        } catch (Exception e) {
            // Non-fatal: just log the inability to capture the named screenshot.
            logger.warn("Unable to capture explicit native mobile screenshot: {}", e.getMessage());
        }
    }


    /**
     * Stops native screen recording (if enabled) on the provided driver and persists/attaches the video
     * depending on configuration and scenario outcome.
     *
     * <p>
     * Behavior:
     * - If videoRecordingEnabled() is false or driver does not support recording, this is a no-op.
     * - The recording data returned by Appium is Base64 encoded; this method decodes and writes it as MP4.
     * - If videoOnFailureOnly() is true and the scenario passed, the video is discarded (not saved/attached).
     * - If attachVideoToReport() is true, the saved video bytes are attached to the scenario report.
     * </p>
     *
     * @param driver   the AppiumDriver which began the recording
     * @param scenario the Scenario associated with the recording; may be null
     */
    public static void stopVideoIfEnabled(AppiumDriver driver, Scenario scenario) {
        // Do nothing if video recording is globally disabled.
        if (!MobileConfigurationProperties.videoRecordingEnabled()) return;

        // Ensure the driver supports stopping a recording.
        if (!(driver instanceof CanRecordScreen)) return;

        try {
            // stopRecordingScreen() typically returns a Base64-encoded string containing the recording bytes.
            String recording = ((CanRecordScreen) driver).stopRecordingScreen();
            if (recording == null || recording.trim().isEmpty()) {
                logger.warn("Native mobile screen recording returned no data.");
                return;
            }

            // Decide whether to persist the recording depending on configuration and scenario status.
            boolean shouldPersist = !MobileConfigurationProperties.videoOnFailureOnly()
                    || (scenario != null && scenario.isFailed());
            if (!shouldPersist) {
                logger.info("Native mobile video discarded because video_on_failure_only=true and scenario passed.");
                return;
            }

            // Decode Base64 payload and write out an MP4 file named after the scenario (or a default).
            byte[] videoBytes = Base64.getDecoder().decode(recording);
            String safeScenarioName = scenario != null ? safeName(scenario.getName()) : "mobile_scenario";
            Path output = outputDir("videos").resolve(safeScenarioName + ".mp4");
            Files.write(output, videoBytes);
            logger.info("Native mobile video saved to [{}]", output.toAbsolutePath());

            // Log to scenario and optionally attach the MP4 bytes to the report.
            if (scenario != null) scenario.log("Native mobile video: " + output.toAbsolutePath());
            if (scenario != null && MobileConfigurationProperties.attachVideoToReport()) {
                scenario.attach(videoBytes, "video/mp4", "mobile-video-" + safeScenarioName);
            }
        } catch (Exception e) {
            // Non-fatal: log the issue but allow tests to continue.
            logger.warn("Unable to stop/save native mobile screen recording: {}", e.getMessage());
        }
    }

    /**
     * Returns the directory path for the given evidence type, creating directories as necessary.
     *
     * <p>Directory structure: {evidenceOutputDirectory}/{RUN_ID}/{type}</p>
     *
     * @param type evidence type subfolder (e.g. "screenshots", "videos", "target-output/screenshots")
     * @return a Path instance pointing to the directory (guaranteed to exist on success)
     * @throws IOException if directories cannot be created or accessed
     */
    private static Path outputDir(String type) throws IOException {
        Path dir = Path.of(MobileConfigurationProperties.getEvidenceOutputDirectory(), RUN_ID, type);
        Files.createDirectories(dir);
        return dir;
    }

    /**
     * Produces a filesystem-safe string derived from the provided value.
     *
     * <p>Replaces any character not in [A-Za-z0-9._-] with an underscore. If the input is null/blank,
     * returns "scenario" as a default name.</p>
     *
     * @param value the input string to sanitize
     * @return sanitized filename-friendly string
     */
    private static String safeName(String value) {
        if (value == null || value.trim().isEmpty()) return "scenario";
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
