package com.ptaf.ui.mobilebrowser;

import com.microsoft.playwright.Page;
import io.cucumber.java.Scenario;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class that provides pixel-based visual validation for Playwright mobile-browser profiles.
 *
 * <p>
 * This class captures a full-page screenshot from a Playwright Page, compares it to a stored baseline image,
 * writes artifacts (actual, diff and optionally baseline) to configured output directories, and optionally
 * attaches images to the provided Cucumber Scenario for reporting.
 * </p>
 *
 * <p>
 * The comparison is performed at the pixel level:
 * - Pixels that are identical are faded in the diff image (lower alpha) so differences stand out.
 * - Different pixels are painted red in the diff image.
 * The mismatch percentage is calculated as (mismatched pixels / compared pixels) * 100 and asserted
 * against a configurable threshold.
 * </p>
 *
 * <p>
 * Note for testers:
 * - If visual testing is disabled via MobileBrowserExecutionConfig, compareCurrentPage will be a no-op.
 * - If a baseline is missing and configuration allows, a new baseline will be created from the current screenshot.
 * - Artifacts (baseline, actual, diff) are saved to the configured output directory and can be attached
 *   to the Cucumber Scenario for embedding in reports.
 * </p>
 */
public final class MobileBrowserVisualValidator {
    // SLF4J logger for informational and error messages.
    private static final Logger logger = LoggerFactory.getLogger(MobileBrowserVisualValidator.class);

    // A run identifier used to group artifacts for a particular execution. Generated once per JVM run.
    private static final String RUN_ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    // Private constructor to prevent instantiation - this is a static utility class.
    private MobileBrowserVisualValidator() { throw new IllegalStateException("Utility class"); }

    /**
     * Capture the current page screenshot, compare it to a baseline image, and handle artifacts and assertions.
     *
     * <p>
     * Steps performed:
     * 1. Check whether visual testing is enabled; if not, log and return.
     * 2. Validate the provided Playwright Page is available.
     * 3. Capture a full-page screenshot as a PNG byte array.
     * 4. Determine baseline and output paths using a sanitized name for file-system safety.
     * 5. Save the actual screenshot to the output directory.
     * 6. If baseline is missing:
     *    - If configured to create baselines, create directories, write baseline, attach it to the scenario and return.
     *    - Otherwise fail with an AssertionError.
     * 7. Read baseline and actual into BufferedImage and run a pixel-by-pixel diff.
     * 8. Write a diff image to the output directory.
     * 9. Optionally attach baseline, actual and diff images to the Cucumber scenario.
     * 10. Log the mismatch percentage and assert it does not exceed the configured threshold.
     * </p>
     *
     * @param page               Playwright Page instance to capture. Must be non-null and open.
     * @param scenario           Cucumber Scenario to attach artifacts to; may be null (attachments skipped).
     * @param baselineName       Logical name of the baseline image (used to locate baseline file). May contain unsafe chars.
     * @param browserProfileName Profile name used to separate baselines per mobile browser configuration; may be null.
     *
     * @throws IllegalStateException If the provided Playwright page is null or closed.
     * @throws AssertionError If the mismatch percent is above the configured threshold, or baseline is missing and auto-create is disabled.
     * @throws RuntimeException For any unexpected I/O or image processing errors encountered during validation.
     */
    public static void compareCurrentPage(Page page, Scenario scenario, String baselineName, String browserProfileName) {
        // If visual validation is globally disabled, log and return immediately.
        if (!MobileBrowserExecutionConfig.visualEnabled()) {
            logger.info("Mobile browser visual testing disabled in configuration.");
            return;
        }

        // Ensure we have a valid Playwright page to capture. Throw early to surface misconfiguration.
        if (page == null || page.isClosed()) throw new IllegalStateException("No active Playwright page is available for visual validation.");

        // Sanitize baseline and profile names so they are safe to use as file names.
        String safeBaseline = safeName(baselineName);
        String safeProfile = safeName(browserProfileName == null ? "unknown_profile" : browserProfileName);

        try {
            // Capture a full-page PNG screenshot from Playwright.
            byte[] actualBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));

            // Compose file paths for baseline and outputs. Baseline location is configured externally.
            Path baselinePath = Path.of(MobileBrowserExecutionConfig.getVisualBaselineDirectory(), safeProfile, safeBaseline + ".png");
            Path outputDir = Path.of(MobileBrowserExecutionConfig.getVisualOutputDirectory(), RUN_ID, safeProfile);

            // Ensure output directories exist before writing artifacts.
            Files.createDirectories(outputDir);

            // Write the actual screenshot to the output directory for inspection.
            Path actualPath = outputDir.resolve(safeBaseline + "-actual.png");
            Files.write(actualPath, actualBytes);

            // If a baseline does not exist, optionally create one if configured; otherwise fail.
            if (!Files.exists(baselinePath)) {
                if (MobileBrowserExecutionConfig.createBaselineIfMissing()) {
                    // Create parent directories for baseline path (if necessary) and write the baseline file.
                    Files.createDirectories(baselinePath.getParent());
                    Files.write(baselinePath, actualBytes);

                    // Attach the newly created baseline to the scenario for traceability.
                    attach(scenario, actualBytes, "image/png", "visual-baseline-created-" + safeBaseline);
                    logger.info("Created new mobile browser visual baseline [{}]", baselinePath.toAbsolutePath());
                    return;
                }
                // Baseline missing and auto-creation disabled => fail the test explicitly.
                throw new AssertionError("Mobile browser visual baseline does not exist: " + baselinePath.toAbsolutePath());
            }

            // Read baseline and actual into BufferedImage for pixel comparisons.
            BufferedImage baseline = ImageIO.read(baselinePath.toFile());
            BufferedImage actual = ImageIO.read(new ByteArrayInputStream(actualBytes));

            // Compute the visual diff (diff image, percent mismatch, compared pixel count).
            VisualDiff diff = diff(baseline, actual);

            // Persist diff image to the output directory for inspection.
            Path diffPath = outputDir.resolve(safeBaseline + "-diff.png");
            byte[] diffBytes = toPng(diff.diffImage());
            Files.write(diffPath, diffBytes);

            // Optionally attach baseline, actual and diff images to the Cucumber Scenario.
            if (MobileBrowserExecutionConfig.attachVisualArtifactsToReport()) {
                attach(scenario, Files.readAllBytes(baselinePath), "image/png", "visual-baseline-" + safeBaseline);
                attach(scenario, actualBytes, "image/png", "visual-actual-" + safeBaseline);
                attach(scenario, diffBytes, "image/png", "visual-diff-" + safeBaseline);
            }

            // Retrieve configured threshold and assert the mismatch percentage is acceptable.
            double threshold = MobileBrowserExecutionConfig.getVisualMismatchThresholdPercent();
            logger.info("Mobile browser visual comparison [{}] mismatch={}%, threshold={}%, pixels={}", safeBaseline, diff.mismatchPercent(), threshold, diff.comparedPixels());
            Assert.assertTrue("Mobile browser visual mismatch for baseline [" + baselineName + "] on profile [" + browserProfileName + "] was " + diff.mismatchPercent() + "% which is above threshold " + threshold + "%. Diff: " + diffPath.toAbsolutePath(), diff.mismatchPercent() <= threshold);
        } catch (AssertionError e) {
            // Propagate assertion errors (failures due to threshold/baseline missing when not allowed).
            throw e;
        } catch (Exception e) {
            // Wrap any unexpected exception with context about which baseline failed.
            throw new RuntimeException("Unable to perform mobile browser visual validation for baseline: " + baselineName, e);
        }
    }

    /**
     * Produce a pixel-level visual diff between a baseline and an actual image.
     *
     * <p>
     * The diff image will have the dimensions of the larger of the two inputs (baseline, actual).
     * - Pixels that are identical are written to the diff image as a faded (semi-transparent) version of the pixel.
     * - Pixels that differ are painted RED in the diff image.
     * The method returns a VisualDiff containing the diff image, the mismatch percentage and the number of compared pixels.
     * </p>
     *
     * @param baseline The baseline image to compare against; must not be null.
     * @param actual   The actual (current) image to compare; must not be null.
     * @return VisualDiff with the generated diff image, mismatch percentage and number of compared pixels.
     */
    private static VisualDiff diff(BufferedImage baseline, BufferedImage actual) {
        // Use the maximum width/height so images of different dimensions are compared over the full canvas.
        int width = Math.max(baseline.getWidth(), actual.getWidth());
        int height = Math.max(baseline.getHeight(), actual.getHeight());

        // Create an ARGB diff image so we can draw faded colors (alpha) and opaque red pixels.
        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Total number of pixels considered in the comparison.
        long compared = (long) width * height;
        long mismatches = 0;

        // Iterate over every pixel coordinate in the canvas and compare.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Retrieve pixel value from each image, or transparent if out of bounds.
                int b = pixelOrTransparent(baseline, x, y);
                int a = pixelOrTransparent(actual, x, y);

                if (b == a) {
                    // If identical, paint a faded version of the pixel into the diff image to provide context.
                    diff.setRGB(x, y, fade(a));
                } else {
                    // Mark mismatched pixel count and paint it RED for easy visual spotting.
                    mismatches++;
                    diff.setRGB(x, y, Color.RED.getRGB());
                }
            }
        }

        // Compute mismatch percentage defensively (avoid division by zero).
        double percent = compared == 0 ? 0.0 : (mismatches * 100.0 / compared);
        return new VisualDiff(diff, percent, compared);
    }

    /**
     * Return the pixel color at (x,y) from img, or fully transparent ARGB (0x00000000) if coordinates are outside image bounds.
     *
     * @param img The image to read from.
     * @param x   X coordinate.
     * @param y   Y coordinate.
     * @return ARGB integer representing the pixel color or transparent if outside the image.
     */
    private static int pixelOrTransparent(BufferedImage img, int x, int y) { return x < img.getWidth() && y < img.getHeight() ? img.getRGB(x, y) : 0x00000000; }

    /**
     * Create a faded (semi-transparent) version of the provided ARGB color.
     *
     * <p>
     * This is used to render matching pixels more subtly in the diff image so mismatches stand out.
     * The alpha is reduced to 70 for a faint overlay effect.
     * </p>
     *
     * @param rgb ARGB color integer.
     * @return ARGB color integer with reduced alpha.
     */
    private static int fade(int rgb) { Color c = new Color(rgb, true); return new Color(c.getRed(), c.getGreen(), c.getBlue(), 70).getRGB(); }

    /**
     * Convert a BufferedImage to a PNG-formatted byte array.
     *
     * @param img BufferedImage to encode.
     * @return PNG bytes.
     * @throws Exception If the image cannot be written (propagated to caller).
     */
    private static byte[] toPng(BufferedImage img) throws Exception { ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(img, "png", out); return out.toByteArray(); }

    /**
     * Attach binary data to a Cucumber Scenario if both scenario and bytes are present.
     *
     * @param scenario Cucumber Scenario (may be null).
     * @param bytes    Data to attach (may be null).
     * @param mimeType MIME type for the attachment.
     * @param name     Attachment name used in the report.
     */
    private static void attach(Scenario scenario, byte[] bytes, String mimeType, String name) { if (scenario != null && bytes != null) scenario.attach(bytes, mimeType, name); }

    /**
     * Produce a file-system safe name from an arbitrary input string.
     *
     * <p>
     * Rules:
     * - Null or empty input becomes the literal "baseline".
     * - Trims surrounding whitespace.
     * - Replaces any character not in [A-Za-z0-9._-] with an underscore.
     * </p>
     *
     * @param value Input name that may contain unsafe characters.
     * @return Sanitized name safe to use in file paths.
     */
    private static String safeName(String value) { return value == null || value.trim().isEmpty() ? "baseline" : value.trim().replaceAll("[^A-Za-z0-9._-]", "_"); }

    /**
     * Immutable container holding the result of a visual diff operation.
     *
     * @param diffImage      The generated diff BufferedImage (pixels faded or marked red for differences).
     * @param mismatchPercent Percentage of pixels that differ between baseline and actual.
     * @param comparedPixels Total number of pixels compared.
     */
    private record VisualDiff(BufferedImage diffImage, double mismatchPercent, long comparedPixels) { }
}
