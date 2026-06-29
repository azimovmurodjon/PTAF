package com.ptaf.pdf;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * PdfRenderDiff
 *
 * Purpose:
 *  - Pure-Java visual diff between two images (e.g., actual rendered page vs. baseline PNG).
 *  - Supports per-channel tolerance and max diff ratio.
 *
 * Why:
 *  - Some layouts/text flows are easier to validate visually than through text extraction.
 *  - No extra libs required; works anywhere Java AWT/ImageIO is available.
 */
public final class PdfRenderDiff {
    private PdfRenderDiff() {}

    /**
     * Result object returned by compare(...) describing whether images match and details about the diff.
     *
     * Instances are immutable and simple to inspect in tests.
     */
    public static class DiffResult {
        /**
         * True if the images are considered matching (diff ratio <= configured maxDiffRatio).
         */
        public final boolean match;

        /**
         * Fraction of pixels considered different, in the range 0..1 (inclusive).
         * Example: 0.005 = 0.5% of pixels differ.
         */
        public final double diffRatio;     // 0..1 - fraction of differing pixels

        /**
         * If a visual diff image was written (when mismatch and an output path was provided),
         * this contains the absolute path to that written PNG. Null if no diff image was written.
         *
         * The produced diff image highlights differing pixels with a semi-transparent red overlay.
         */
        public final String diffImagePath; // where the red-highlight diff was written (if mismatch)

        /**
         * Construct a DiffResult.
         *
         * @param match whether the images are considered matching
         * @param diffRatio fraction of differing pixels (0..1)
         * @param diffImagePath path to written diff image or null
         */
        public DiffResult(boolean match, double diffRatio, String diffImagePath) {
            this.match = match; this.diffRatio = diffRatio; this.diffImagePath = diffImagePath;
        }
    }

    /**
     * Compare two images with RGBA channel tolerance; produce a red-overlay diff image if the difference
     * ratio exceeds the allowed maxDiffRatio.
     *
     * Behavior and notes:
     * - Images are loaded via ImageIO.read(File). Any exceptions are wrapped in a RuntimeException.
     * - If images differ in dimensions (width/height), the method immediately returns a non-match
     *   with diffRatio==1.0 and no diff image written.
     * - Each pixel is compared per-channel (R,G,B,A). A pixel is considered differing if the absolute
     *   difference of any channel exceeds channelTolerance.
     * - The produced visual diff (when written) places a semi-transparent red pixel at positions that
     *   differ, and fully transparent elsewhere. This makes it easy to overlay or inspect differences.
     *
     * Parameters:
     * @param actualImgPath     path to the actual image produced by rendering
     * @param expectedImgPath   path to the expected/baseline image to compare against
     * @param channelTolerance  allowed difference per RGBA channel, in the range 0..255.
     *                          Use 0 for exact per-channel equality. Larger values allow more variance.
     * @param maxDiffRatio      allowed fraction of differing pixels; if the computed diff ratio is
     *                          greater than this value the images are considered non-matching.
     *                          Example: 0.005 means up to 0.5% of pixels may differ.
     * @param outDiffPath       optional path to write a PNG visual diff when a mismatch occurs.
     *                          If null or blank, no diff will be written even on mismatch.
     *
     * @return DiffResult containing whether images match, the computed diff ratio, and path to
     *         any written diff image (or null).
     *
     * @throws RuntimeException wrapping any IO or image processing exceptions encountered.
     */
    public static DiffResult compare(String actualImgPath, String expectedImgPath,
                                     int channelTolerance, double maxDiffRatio, String outDiffPath) {
        try {
            // Load both images using ImageIO. This supports common formats (PNG, JPEG, etc.).
            BufferedImage a = ImageIO.read(new File(actualImgPath));
            BufferedImage b = ImageIO.read(new File(expectedImgPath));

            // If sizes differ we cannot do a pixel-by-pixel comparison; treat as full mismatch.
            if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
                return new DiffResult(false, 1.0, null);
            }

            int w = a.getWidth(), h = a.getHeight();
            long pixels = (long) w * h;   // total number of pixels (use long to be safe)
            long diffCount = 0;           // count of pixels considered different

            // Create an ARGB image to hold visual diff. We use TYPE_INT_ARGB to support transparency.
            BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            // Iterate every pixel and compare per-channel tolerance.
            // getRGB returns a packed int (ARGB).
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int ca = a.getRGB(x, y), cb = b.getRGB(x, y);

                    // If the pixels are not within tolerance, mark diff image with semi-transparent red.
                    if (!withinTolerance(ca, cb, channelTolerance)) {
                        // 255,0,0 with alpha 180 for a visible translucent red overlay.
                        diff.setRGB(x, y, new Color(255, 0, 0, 180).getRGB());
                        diffCount++;
                    } else {
                        // Transparent pixel where there is no difference.
                        diff.setRGB(x, y, 0x00000000);
                    }
                }
            }

            // Compute ratio of differing pixels.
            double ratio = diffCount / (double) pixels;
            boolean match = ratio <= maxDiffRatio;

            String written = null;
            // If not matching and an output path is provided, write the visual diff as PNG.
            if (!match && outDiffPath != null && !outDiffPath.isBlank()) {
                File out = new File(outDiffPath);
                // Ensure parent directories exist (mkdirs is safe if parent==null it does nothing).
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                ImageIO.write(diff, "png", out);
                written = out.getAbsolutePath();
            }

            return new DiffResult(match, ratio, written);
        } catch (Exception e) {
            // Wrap any checked exceptions so callers don't need to handle many image IO specifics.
            throw new RuntimeException("Image diff failed", e);
        }
    }

    /**
     * Channel-wise tolerance check for two packed ARGB pixels.
     *
     * The packed format expected is the same as BufferedImage.getRGB: highest byte is alpha,
     * then red, green, blue (0xAARRGGBB). Each channel is extracted and compared using absolute
     * difference against the provided tolerance.
     *
     * Note: tol should be in 0..255. A tol of 0 requires exact per-channel equality.
     *
     * @param a   packed ARGB int for first pixel
     * @param b   packed ARGB int for second pixel
     * @param tol allowed absolute difference per channel
     * @return true if all channels (A,R,G,B) differ by at most tol, false otherwise
     */
    private static boolean withinTolerance(int a, int b, int tol) {
        // Extract red, green, blue channels by shifting then masking to 0..255.
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;

        // Extract alpha using unsigned right shift to avoid sign-extension issues.
        // (a >>> 24) yields the high-order byte as an int in 0..255.
        int aa = (a >>> 24);
        int ba = (b >>> 24);

        // Compare each channel independently.
        return Math.abs(ar - br) <= tol &&
                Math.abs(ag - bg) <= tol &&
                Math.abs(ab - bb) <= tol &&
                Math.abs(aa - ba) <= tol;
    }
}
