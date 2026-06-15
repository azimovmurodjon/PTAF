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
 * Pixel-based visual validation for Playwright mobile-browser profiles.
 */
public final class MobileBrowserVisualValidator {
    private static final Logger logger = LoggerFactory.getLogger(MobileBrowserVisualValidator.class);
    private static final String RUN_ID = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

    private MobileBrowserVisualValidator() { throw new IllegalStateException("Utility class"); }

    public static void compareCurrentPage(Page page, Scenario scenario, String baselineName, String browserProfileName) {
        if (!MobileBrowserExecutionConfig.visualEnabled()) {
            logger.info("Mobile browser visual testing disabled in configuration.");
            return;
        }
        if (page == null || page.isClosed()) throw new IllegalStateException("No active Playwright page is available for visual validation.");
        String safeBaseline = safeName(baselineName);
        String safeProfile = safeName(browserProfileName == null ? "unknown_profile" : browserProfileName);
        try {
            byte[] actualBytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            Path baselinePath = Path.of(MobileBrowserExecutionConfig.getVisualBaselineDirectory(), safeProfile, safeBaseline + ".png");
            Path outputDir = Path.of(MobileBrowserExecutionConfig.getVisualOutputDirectory(), RUN_ID, safeProfile);
            Files.createDirectories(outputDir);
            Path actualPath = outputDir.resolve(safeBaseline + "-actual.png");
            Files.write(actualPath, actualBytes);

            if (!Files.exists(baselinePath)) {
                if (MobileBrowserExecutionConfig.createBaselineIfMissing()) {
                    Files.createDirectories(baselinePath.getParent());
                    Files.write(baselinePath, actualBytes);
                    attach(scenario, actualBytes, "image/png", "visual-baseline-created-" + safeBaseline);
                    logger.info("Created new mobile browser visual baseline [{}]", baselinePath.toAbsolutePath());
                    return;
                }
                throw new AssertionError("Mobile browser visual baseline does not exist: " + baselinePath.toAbsolutePath());
            }

            BufferedImage baseline = ImageIO.read(baselinePath.toFile());
            BufferedImage actual = ImageIO.read(new ByteArrayInputStream(actualBytes));
            VisualDiff diff = diff(baseline, actual);
            Path diffPath = outputDir.resolve(safeBaseline + "-diff.png");
            byte[] diffBytes = toPng(diff.diffImage());
            Files.write(diffPath, diffBytes);

            if (MobileBrowserExecutionConfig.attachVisualArtifactsToReport()) {
                attach(scenario, Files.readAllBytes(baselinePath), "image/png", "visual-baseline-" + safeBaseline);
                attach(scenario, actualBytes, "image/png", "visual-actual-" + safeBaseline);
                attach(scenario, diffBytes, "image/png", "visual-diff-" + safeBaseline);
            }

            double threshold = MobileBrowserExecutionConfig.getVisualMismatchThresholdPercent();
            logger.info("Mobile browser visual comparison [{}] mismatch={}%, threshold={}%, pixels={}", safeBaseline, diff.mismatchPercent(), threshold, diff.comparedPixels());
            Assert.assertTrue("Mobile browser visual mismatch for baseline [" + baselineName + "] on profile [" + browserProfileName + "] was " + diff.mismatchPercent() + "% which is above threshold " + threshold + "%. Diff: " + diffPath.toAbsolutePath(), diff.mismatchPercent() <= threshold);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform mobile browser visual validation for baseline: " + baselineName, e);
        }
    }

    private static VisualDiff diff(BufferedImage baseline, BufferedImage actual) {
        int width = Math.max(baseline.getWidth(), actual.getWidth());
        int height = Math.max(baseline.getHeight(), actual.getHeight());
        BufferedImage diff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        long compared = (long) width * height;
        long mismatches = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int b = pixelOrTransparent(baseline, x, y);
                int a = pixelOrTransparent(actual, x, y);
                if (b == a) {
                    diff.setRGB(x, y, fade(a));
                } else {
                    mismatches++;
                    diff.setRGB(x, y, Color.RED.getRGB());
                }
            }
        }
        double percent = compared == 0 ? 0.0 : (mismatches * 100.0 / compared);
        return new VisualDiff(diff, percent, compared);
    }

    private static int pixelOrTransparent(BufferedImage img, int x, int y) { return x < img.getWidth() && y < img.getHeight() ? img.getRGB(x, y) : 0x00000000; }
    private static int fade(int rgb) { Color c = new Color(rgb, true); return new Color(c.getRed(), c.getGreen(), c.getBlue(), 70).getRGB(); }
    private static byte[] toPng(BufferedImage img) throws Exception { ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(img, "png", out); return out.toByteArray(); }
    private static void attach(Scenario scenario, byte[] bytes, String mimeType, String name) { if (scenario != null && bytes != null) scenario.attach(bytes, mimeType, name); }
    private static String safeName(String value) { return value == null || value.trim().isEmpty() ? "baseline" : value.trim().replaceAll("[^A-Za-z0-9._-]", "_"); }
    private record VisualDiff(BufferedImage diffImage, double mismatchPercent, long comparedPixels) { }
}
