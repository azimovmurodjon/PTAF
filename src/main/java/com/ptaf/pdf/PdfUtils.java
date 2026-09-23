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
 * Utility helpers for working with PDF documents using Apache PDFBox.
 *
 * <p>
 * This final utility class centralizes small, commonly-used operations for reading and
 * rendering PDFs:
 * <ul>
 *   <li>Extract text (entire document, single page or page ranges)</li>
 *   <li>Support for password-protected PDFs</li>
 *   <li>Count pages</li>
 *   <li>Render a single page to PNG (useful for visual diffs or OCR staging)</li>
 *   <li>Normalize whitespace and ligatures for robust string comparisons across renderers</li>
 * </ul>
 * </p>
 *
 * <p>
 * Note for testers:
 * - Methods return normalized text by default (see normalize) to reduce false negatives
 *   in assertions caused by different whitespace, zero-width characters or ligatures.
 * - Page numbers are 1-based throughout (matching human expectations and most viewers).
 * - Rendering produces a PNG written to disk and returns its absolute path.
 * </p>
 *
 * <p>Requires Apache PDFBox on the classpath.</p>
 */
public final class PdfUtils {
    // Prevent instantiation: utility class only.
    private PdfUtils() {}

    /* ==================== TEXT ==================== */

    /**
     * Read all text from the PDF at the given path and apply normalization.
     *
     * <p>Normalization collapses excessive whitespace and replaces non-breaking spaces
     * so that text comparisons are more resilient across different renderers / encodings.</p>
     *
     * @param pdfPath path to the PDF file on disk
     * @return normalized extracted text; never null (returns empty string for null/empty source)
     * @throws RuntimeException wrapping any IO or PDFBox exceptions encountered while loading or extracting
     */
    public static String readAllText(String pdfPath) {
        return normalize(extract(pdfPath, null, 1, Integer.MAX_VALUE));
    }

    /**
     * Read all text from a password-protected PDF and apply normalization.
     *
     * @param pdfPath path to the PDF file
     * @param password password for opening the PDF; supply null or empty to attempt without a password
     * @return normalized extracted text
     * @throws RuntimeException if the file cannot be opened (wrong password or IO errors)
     */
    public static String readAllText(String pdfPath, String password) {
        return normalize(extract(pdfPath, password, 1, Integer.MAX_VALUE));
    }

    /**
     * Read and normalize text from a single page in the PDF.
     *
     * <p>Page numbering is 1-based. If the page does not exist the underlying extractor
     * will throw an exception which is wrapped as a RuntimeException.</p>
     *
     * @param pdfPath path to the PDF file
     * @param pageNumber 1-based page number to extract
     * @return normalized text for the requested page
     */
    public static String readPageText(String pdfPath, int pageNumber) {
        return normalize(extract(pdfPath, null, pageNumber, pageNumber));
    }

    /**
     * Read and normalize text from a single page in a password-protected PDF.
     *
     * @param pdfPath path to the PDF file
     * @param pageNumber 1-based page number to extract
     * @param password password for opening the PDF
     * @return normalized text for the requested page
     */
    public static String readPageText(String pdfPath, int pageNumber, String password) {
        return normalize(extract(pdfPath, password, pageNumber, pageNumber));
    }

    /**
     * Read text across a closed page range and return a List of normalized strings,
     * one string per page in the requested range.
     *
     * <p>Inputs are sanitized so that 'from' is at least 1 and 'to' is at least 'from'.</p>
     *
     * @param pdfPath path to the PDF file
     * @param from starting 1-based page number (inclusive)
     * @param to ending 1-based page number (inclusive)
     * @return list of normalized page texts in ascending page order
     */
    public static List<String> readPages(String pdfPath, int from, int to) {
        int start = Math.max(1, from);
        int end = Math.max(start, to);
        /*
         * Use a stream to map each 1-based page number to its text.
         * This invokes readPageText per page which will open/read the document each time.
         * For short ranges this is fine; for large ranges consider a single-extraction method.
         */
        return IntStream.rangeClosed(start, end)
                .mapToObj(p -> readPageText(pdfPath, p))
                .toList();
    }

    /* ==================== STRUCTURE ==================== */

    /**
     * Return the total number of pages in a non-password protected PDF.
     *
     * <p>Uses try-with-resources to ensure PDDocument is closed promptly.</p>
     *
     * @param pdfPath path to the PDF file
     * @return number of pages in the document
     * @throws RuntimeException if the file cannot be loaded or parsed
     */
    public static int pageCount(String pdfPath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read page count for " + pdfPath, e);
        }
    }

    /**
     * Return the total number of pages in a password-protected PDF.
     *
     * @param pdfPath path to the PDF file
     * @param password password used to open the document
     * @return number of pages
     * @throws RuntimeException if opening/parsing fails (e.g. wrong password or IO error)
     */
    public static int pageCount(String pdfPath, String password) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read page count (pwd) for " + pdfPath, e);
        }
    }

    /**
     * Quick heuristic to determine whether a file appears to be a PDF.
     *
     * <p>This does a lightweight header check (reads the first few bytes and looks for the
     * standard "%PDF-" signature). It is not a full validation and may yield false positives
     * or false negatives for truncated or specially-encoded files.</p>
     *
     * @param pdfPath path to the file to probe
     * @return true if the first bytes match the PDF header pattern, false otherwise
     */
    public static boolean isPdfFile(String pdfPath) {
        try {
            // Read raw bytes from disk and check ASCII header.
            byte[] first = Files.readAllBytes(new File(pdfPath).toPath());
            String head = new String(first, 0, Math.min(first.length, 5), StandardCharsets.US_ASCII);
            return head.startsWith("%PDF-");
        } catch (Exception e) {
            // On any IO error (missing file, permission, etc.) treat as not a PDF.
            return false;
        }
    }

    /* ==================== RENDER (PNG) ==================== */

    /**
     * Render a single page (1-based) of the given PDF to a PNG image file and return
     * the absolute path of the written file.
     *
     * <p>Suggested DPI:
     * <ul>
     *   <li>150–200 for readable on-screen text</li>
     *   <li>300 for OCR or high fidelity captures</li>
     * </ul>
     * </p>
     *
     * @param pdfPath path to the PDF file
     * @param pageNumber 1-based page number to render
     * @param dpi target resolution in dots-per-inch for the rendered image
     * @param outFilePath output path for the PNG (directories will be created as needed)
     * @return absolute path to the generated PNG image
     * @throws RuntimeException wrapping any IO or PDFBox exceptions encountered during rendering
     */
    public static String renderPageToPng(String pdfPath, int pageNumber, float dpi, String outFilePath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            return render(doc, pageNumber, dpi, outFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to render page " + pageNumber + " from " + pdfPath, e);
        }
    }

    /**
     * Version of renderPageToPng that supports password-protected PDFs.
     *
     * @param pdfPath path to the PDF file
     * @param password password to open the document
     * @param pageNumber 1-based page number to render
     * @param dpi rendering resolution in DPI
     * @param outFilePath output path for the PNG file
     * @return absolute path to the generated image
     */
    public static String renderPageToPng(String pdfPath, String password, int pageNumber, float dpi, String outFilePath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return render(doc, pageNumber, dpi, outFilePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to render page " + pageNumber + " (pwd) from " + pdfPath, e);
        }
    }

    /* ==================== INTERNAL ==================== */

    /**
     * Internal helper that renders a page from an already-open PDDocument to a PNG file.
     *
     * <p>This method performs:
     * <ol>
     *   <li>Bounds checking for the requested page number (1..pageCount)</li>
     *   <li>Rendering using PDFRenderer at the requested DPI</li>
     *   <li>Writing the BufferedImage to disk as PNG, creating parent directories if needed</li>
     * </ol>
     * </p>
     *
     * @param doc already opened PDDocument (caller is responsible for opening/closing)
     * @param pageNumber 1-based page to render
     * @param dpi rendering resolution
     * @param outFilePath path where the PNG will be written
     * @return absolute path to the created PNG file
     * @throws Exception any exception from PDFBox or IO operations bubbles up to the caller
     */
    private static String render(PDDocument doc, int pageNumber, float dpi, String outFilePath) throws Exception {
        int count = doc.getNumberOfPages();
        if (pageNumber < 1 || pageNumber > count) {
            // Defensive check: ensure callers request a valid page.
            throw new IllegalArgumentException("Page " + pageNumber + " out of range 1.." + count);
        }
        // PDFBox rendering: page index is 0-based internally.
        PDFRenderer renderer = new PDFRenderer(doc);
        BufferedImage img = renderer.renderImageWithDPI(pageNumber - 1, dpi);

        // Ensure the output directory exists before writing.
        File out = new File(outFilePath);
        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();

        // Write the image to disk as PNG.
        ImageIO.write(img, "png", out);
        return out.getAbsolutePath();
    }

    /**
     * Extract text from a PDF between startPageInclusive and endPageInclusive.
     *
     * <p>If password is null or empty the document is opened without a password; otherwise
     * the provided password is used. The end page can be Integer.MAX_VALUE to indicate
     * "until the document end" — in this case the actual document page count is used.</p>
     *
     * <p>Uses PDFTextStripper to perform plain-text extraction. PDFTextStripper attempts to
     * preserve reading order but results may vary depending on PDF structure.</p>
     *
     * @param pdfPath path to the PDF document
     * @param password optional password for encrypted PDFs (may be null/empty)
     * @param startPageInclusive 1-based start page (inclusive)
     * @param endPageInclusive 1-based end page (inclusive) or Integer.MAX_VALUE to indicate document end
     * @return raw extracted text (not normalized)
     * @throws RuntimeException wrapping any exceptions from loading or extraction
     */
    private static String extract(String pdfPath, String password, int startPageInclusive, int endPageInclusive) {
        try (PDDocument doc = (password == null || password.isEmpty())
                ? PDDocument.load(new File(pdfPath))
                : PDDocument.load(new File(pdfPath), password)) {
            PDFTextStripper stripper = new PDFTextStripper();

            // Configure the page range for extraction. PDFTextStripper uses 1-based indexes.
            stripper.setStartPage(startPageInclusive);
            stripper.setEndPage(endPageInclusive == Integer.MAX_VALUE ? doc.getNumberOfPages() : endPageInclusive);

            // Return the raw text; caller is responsible for normalization if desired.
            return stripper.getText(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text from " + pdfPath, e);
        }
    }

    /**
     * Normalize a string to make comparisons resilient across different PDF renderers and encodings.
     *
     * <p>Normalization steps:
     * <ul>
     *   <li>Convert non-breaking spaces (U+00A0) to regular spaces</li>
     *   <li>Collapse all whitespace sequences (spaces, tabs, newlines, zero-width chars) to a single space</li>
     *   <li>Trim leading/trailing whitespace</li>
     * </ul>
     * </p>
     *
     * <p>For test assertions this reduces spurious mismatches caused by invisible or unusual whitespace.</p>
     *
     * @param text raw text which may contain special whitespace/ligature characters
     * @return normalized, trimmed string; never null
     */
    public static String normalize(String text) {
        if (text == null) return "";
        // Replace non-breaking space with a normal space for consistent handling.
        String t = text.replace('\u00A0', ' ');
        // Collapse all whitespace sequences (including zero-width and BOM) into a single ASCII space.
        t = t.replaceAll("[\\s\\u200B\\u200C\\u200D\\uFEFF]+", " ").trim();
        return t;
    }
}
