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
 * <p>Failure screenshots are intentionally captured even when optional pass/every-scenario
 * screenshots are disabled, so native mobile failures are visible in the same way as UI failures.</p>
 */
public final class MobileEvidenceManager {
    private static final Logger logger = LoggerFactory.getLogger(MobileEvidenceManager.class);
    private static final String RUN_ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    private static final ThreadLocal<Scenario> CURRENT_SCENARIO = new ThreadLocal<>();

    private MobileEvidenceManager() { throw new IllegalStateException("Utility class"); }

    public static void setCurrentScenario(Scenario scenario) { CURRENT_SCENARIO.set(scenario); }
    public static void clearCurrentScenario() { CURRENT_SCENARIO.remove(); }
    public static Scenario getCurrentScenario() { return CURRENT_SCENARIO.get(); }

    public static void startVideoIfEnabled(AppiumDriver driver) {
        if (!MobileConfigurationProperties.videoRecordingEnabled()) return;
        if (!(driver instanceof CanRecordScreen)) {
            logger.warn("The active Appium driver does not support screen recording.");
            return;
        }
        try {
            ((CanRecordScreen) driver).startRecordingScreen();
            logger.info("Native mobile screen recording started.");
        } catch (Exception e) {
            logger.warn("Unable to start native mobile screen recording: {}", e.getMessage());
        }
    }

    public static void captureScenarioScreenshotIfConfigured(AppiumDriver driver, Scenario scenario) {
        if (driver == null || scenario == null) return;

        if (scenario.isFailed() && MobileConfigurationProperties.screenshotOnFailure()) {
            captureScreenshot(driver, scenario, "failure", true);
            return;
        }

        boolean shouldCapture = MobileConfigurationProperties.screenshotAfterEachScenario()
                || (!scenario.isFailed() && MobileConfigurationProperties.screenshotOnPass());
        if (shouldCapture) captureScreenshot(driver, scenario, scenario.isFailed() ? "failure" : "scenario", false);
    }

    /** Captures screenshot immediately when a framework assertion fails. */
    public static void captureAssertionFailureScreenshot(AppiumDriver driver, String failureName, String details) {
        Scenario scenario = CURRENT_SCENARIO.get();
        if (driver == null || scenario == null) return;
        try {
            String safeName = safeName(scenario.getName() + "_" + failureName);
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            Path output = outputDir("screenshots").resolve(safeName + ".png");
            Files.write(output, screenshot);
            logger.info("Native mobile assertion failure screenshot saved to [{}]", output.toAbsolutePath());
            scenario.log("Native mobile assertion failure: " + details);
            scenario.log("Screenshot: " + output.toAbsolutePath());
            if (MobileConfigurationProperties.attachScreenshotsToReport()) {
                scenario.attach(screenshot, "image/png", "mobile-assertion-failure-" + safeName);
            }
        } catch (Exception e) {
            logger.warn("Unable to capture immediate native mobile assertion failure screenshot: {}", e.getMessage());
        }
    }

    private static void captureScreenshot(AppiumDriver driver, Scenario scenario, String type, boolean forceAttach) {
        try {
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            String safeScenarioName = safeName(scenario.getName() + "_" + type);
            Path output = outputDir("target-output/screenshots").resolve(safeScenarioName + ".png");
            Files.write(output, screenshot);
            logger.info("Native mobile screenshot saved to [{}]", output.toAbsolutePath());
            scenario.log("Native mobile screenshot: " + output.toAbsolutePath());
            if (forceAttach || MobileConfigurationProperties.attachScreenshotsToReport()) {
                scenario.attach(screenshot, "image/png", "mobile-screenshot-" + safeScenarioName);
            }
        } catch (Exception e) {
            logger.warn("Unable to capture native mobile screenshot: {}", e.getMessage());
        }
    }

    /** Captures an explicitly requested screenshot and attaches it to the active Cucumber report. */
    public static void captureNamedScreenshot(AppiumDriver driver, String screenshotName) {
        Scenario scenario = getCurrentScenario();
        if (driver == null) return;

        try {
            String baseName = screenshotName == null || screenshotName.trim().isEmpty()
                    ? "mobile_screenshot"
                    : screenshotName;
            String safeName = baseName.trim().replaceAll("[^A-Za-z0-9._-]", "_");

            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            Path output = outputDir("target-output/screenshots").resolve(safeName + ".png");
            Files.write(output, screenshot);

            if (scenario != null) {
                scenario.log("Native mobile screenshot: " + output.toAbsolutePath());
                if (MobileConfigurationProperties.attachScreenshotsToReport()) {
                    scenario.attach(screenshot, "image/png", "mobile-screenshot-" + safeName);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to capture explicit native mobile screenshot: {}", e.getMessage());
        }
    }


    public static void stopVideoIfEnabled(AppiumDriver driver, Scenario scenario) {
        if (!MobileConfigurationProperties.videoRecordingEnabled()) return;
        if (!(driver instanceof CanRecordScreen)) return;
        try {
            String recording = ((CanRecordScreen) driver).stopRecordingScreen();
            if (recording == null || recording.trim().isEmpty()) {
                logger.warn("Native mobile screen recording returned no data.");
                return;
            }
            boolean shouldPersist = !MobileConfigurationProperties.videoOnFailureOnly()
                    || (scenario != null && scenario.isFailed());
            if (!shouldPersist) {
                logger.info("Native mobile video discarded because video_on_failure_only=true and scenario passed.");
                return;
            }
            byte[] videoBytes = Base64.getDecoder().decode(recording);
            String safeScenarioName = scenario != null ? safeName(scenario.getName()) : "mobile_scenario";
            Path output = outputDir("videos").resolve(safeScenarioName + ".mp4");
            Files.write(output, videoBytes);
            logger.info("Native mobile video saved to [{}]", output.toAbsolutePath());
            if (scenario != null) scenario.log("Native mobile video: " + output.toAbsolutePath());
            if (scenario != null && MobileConfigurationProperties.attachVideoToReport()) {
                scenario.attach(videoBytes, "video/mp4", "mobile-video-" + safeScenarioName);
            }
        } catch (Exception e) {
            logger.warn("Unable to stop/save native mobile screen recording: {}", e.getMessage());
        }
    }

    private static Path outputDir(String type) throws IOException {
        Path dir = Path.of(MobileConfigurationProperties.getEvidenceOutputDirectory(), RUN_ID, type);
        Files.createDirectories(dir);
        return dir;
    }

    private static String safeName(String value) {
        if (value == null || value.trim().isEmpty()) return "scenario";
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
