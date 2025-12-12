package com.ptaf.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.IntStream;

/**
 * PdfUtils
 *
 * Purpose:
 *  - Low-level PDF utilities built on Apache PDFBox:
 *      * Extract text (entire document or by page)
 *      * Handle passwords
 *      * Count pages
 *      * Render a page to PNG (for visual diffs or OCR staging)
 *  - Normalize whitespace/ligatures for resilient text assertions.
 *
 * Why:
 *  - Keeps validation code simple by offering a single, reliable source for reading PDFs.
 *  - Rendering allows image-based comparisons and OCR fallback for scanned PDFs.
 */
public final class PdfUtils {
    private PdfUtils() {}

    /* ==================== TEXT ==================== */

    /** Read all text from a PDF (whitespace-normalized). */
    public static String readAllText(String pdfPath) {
        return normalize(extract(pdfPath, null, 1, Integer.MAX_VALUE));
    }

    /** Read all text from a password-protected PDF (whitespace-normalized). */
    public static String readAllText(String pdfPath, String password) {
        return normalize(extract(pdfPath, password, 1, Integer.MAX_VALUE));
    }

    /** Read text from a single page (1-based), normalized. */
    public static String readPageText(String pdfPath, int pageNumber) {
        return normalize(extract(pdfPath, null, pageNumber, pageNumber));
    }

    /** Read text from a single page (1-based) of a password-protected PDF, normalized. */
    public static String readPageText(String pdfPath, int pageNumber, String password) {
        return normalize(extract(pdfPath, password, pageNumber, pageNumber));
    }

    /** Read text across a page range, normalized, returned as a list of per-page strings. */
    public static List<String> readPages(String pdfPath, int from, int to) {
        int start = Math.max(1, from);
        int end = Math.max(start, to);
        return IntStream.rangeClosed(start, end)
                .mapToObj(p -> readPageText(pdfPath, p))
                .toList();
    }

    /* ==================== STRUCTURE ==================== */

    /** Total page count for a non-password PDF. */
    public static int pageCount(String pdfPath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read page count for " + pdfPath, e);
        }
    }

    /** Total page count for a password-protected PDF. */
    public static int pageCount(String pdfPath, String password) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read page count (pwd) for " + pdfPath, e);
        }
    }

    /** Quick magic header check (not a full validation) to ensure the file starts like a PDF. */
    public static boolean isPdfFile(String pdfPath) {
        try {
            byte[] first = Files.readAllBytes(new File(pdfPath).toPath());
            String head = new String(first, 0, Math.min(first.length, 5), StandardCharsets.US_ASCII);
            return head.startsWith("%PDF-");
        } catch (Exception e) {
            return false;
        }
    }

    /* ==================== RENDER (PNG) ==================== */

    /**
     * Render a page (1-based) to a PNG image.
     * @param dpi Suggested 150–200 for readable text, 300 for high fidelity.
     * @return Absolute path to the written image file.
     */
    public static String renderPageToPng(String pdfPath, int pageNumber, float dpi, String outFilePath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            return render(doc, pageNumber, dpi, outFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to render page " + pageNumber + " from " + pdfPath, e);
        }
    }

    /** Password variant of renderPageToPng. */
    public static String renderPageToPng(String pdfPath, String password, int pageNumber, float dpi, String outFilePath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return render(doc, pageNumber, dpi, outFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to render page " + pageNumber + " (pwd) from " + pdfPath, e);
        }
    }

    /* ==================== INTERNAL ==================== */

    private static String render(PDDocument doc, int pageNumber, float dpi, String outFilePath) throws Exception {
        int count = doc.getNumberOfPages();
        if (pageNumber < 1 || pageNumber > count) {
            throw new IllegalArgumentException("Page " + pageNumber + " out of range 1.." + count);
        }
        PDFRenderer renderer = new PDFRenderer(doc);
        BufferedImage img = renderer.renderImageWithDPI(pageNumber - 1, dpi);
        File out = new File(outFilePath);
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        ImageIO.write(img, "png", out);
        return out.getAbsolutePath();
    }

    private static String extract(String pdfPath, String password, int startPageInclusive, int endPageInclusive) {
        try (PDDocument doc = (password == null || password.isEmpty())
                ? PDDocument.load(new File(pdfPath))
                : PDDocument.load(new File(pdfPath), password)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(startPageInclusive);
            stripper.setEndPage(endPageInclusive == Integer.MAX_VALUE ? doc.getNumberOfPages() : endPageInclusive);
            return stripper.getText(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from " + pdfPath, e);
        }
    }

    /**
     * Normalize string for robust comparisons across renderers/encodings/ligatures.
     * - Replaces non-breaking spaces.
     * - Collapses all whitespace (including zero-width chars).
     */
    public static String normalize(String text) {
        if (text == null) return "";
        String t = text.replace('\u00A0', ' ');
        t = t.replaceAll("[\\s\\u200B\\u200C\\u200D\\uFEFF]+", " ").trim();
        return t;
    }
}