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

    /** Result of a visual comparison. */
    public static class DiffResult {
        public final boolean match;
        public final double diffRatio;     // 0..1 - fraction of differing pixels
        public final String diffImagePath; // where the red-highlight diff was written (if mismatch)
        public DiffResult(boolean match, double diffRatio, String diffImagePath) {
            this.match = match; this.diffRatio = diffRatio; this.diffImagePath = diffImagePath;
        }
    }

    /**
     * Compare two images with RGBA channel tolerance; produce a red-overlay diff if beyond maxDiffRatio.
     *
     * @param channelTolerance 0..255 per channel allowed difference
     * @param maxDiffRatio     allowed fraction of differing pixels (e.g., 0.005 = 0.5%)
     */
    public static DiffResult compare(String actualImgPath, String expectedImgPath,
                                     int channelTolerance, double maxDiffRatio, String outDiffPath) {
        try {
            BufferedImage a = ImageIO.read(new File(actualImgPath));
            BufferedImage b = ImageIO.read(new File(expectedImgPath));
            if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
                return new DiffResult(false, 1.0, null);
            }

            int w = a.getWidth(), h = a.getHeight();
            long pixels = (long) w * h, diffCount = 0;
            BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int ca = a.getRGB(x, y), cb = b.getRGB(x, y);
                    if (!withinTolerance(ca, cb, channelTolerance)) {
                        diff.setRGB(x, y, new Color(255, 0, 0, 180).getRGB());
                        diffCount++;
                    } else {
                        diff.setRGB(x, y, 0x00000000);
                    }
                }
            }

            double ratio = diffCount / (double) pixels;
            boolean match = ratio <= maxDiffRatio;
            String written = null;
            if (!match && outDiffPath != null && !outDiffPath.isBlank()) {
                File out = new File(outDiffPath);
                File parent = out.getParentFile();
                if (parent != null) parent.mkdirs();
                ImageIO.write(diff, "png", out);
                written = out.getAbsolutePath();
            }
            return new DiffResult(match, ratio, written);
        } catch (Exception e) {
            throw new RuntimeException("Image diff failed", e);
        }
    }

    /** Channel-wise tolerance check for RGBA. */
    private static boolean withinTolerance(int a, int b, int tol) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF, aa = (a >>> 24);
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF, ba = (b >>> 24);
        return Math.abs(ar - br) <= tol &&
                Math.abs(ag - bg) <= tol &&
                Math.abs(ab - bb) <= tol &&
                Math.abs(aa - ba) <= tol;
    }
}