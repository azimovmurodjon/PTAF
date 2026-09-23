package com.ptaf.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PdfValidator
 *
 * Purpose:
 *  - High-level, readable assertion methods for PDFs.
 *  - Delegates to PdfUtils/PdfRenderDiff/PdfMeta for the heavy lifting.
 *  - OCR assertions are OPTIONAL: they auto-skip when Tesseract is not available
 *    OR when OCR is disabled via system property.
 *
 * Why:
 *  - Keeps step definitions very thin and makes test intent obvious.
 *  - Centralizes assertion messages for consistent, debuggable failures.
 *
 * OCR behavior (no company install required):
 *  - By default, we TRY to use OCR if present on PATH.
 *  - If not present, we SKIP OCR tests instead of failing them.
 *  - You can explicitly disable OCR via JVM flag: -Dpdf.ocr.enabled=false
 */
public final class PdfValidator {
    private static final Logger log = LoggerFactory.getLogger(PdfValidator.class);

    /** System property toggle: -Dpdf.ocr.enabled=false to hard-disable OCR in all runs. */
    private static final String PROP_OCR_ENABLED = "pdf.ocr.enabled";

    private PdfValidator() {}

    /* ==================== Whole document (text) ==================== */

    /**
     * Assert that the full text of the given PDF contains the provided substring.
     *
     * This method:
     *  - Reads all text from the PDF via PdfUtils.readAllText(...)
     *  - Normalizes the expected substring before checking containment (PdfUtils.normalize)
     *  - Uses JUnit assertions so failures integrate cleanly with test runners.
     *
     * @param pdfPath   path to the PDF file to inspect
     * @param substring expected substring (will be normalized before comparison)
     */
    public static void assertContains(String pdfPath, String substring) {
        // Read and normalize full document text.
        String all = PdfUtils.readAllText(pdfPath);
        // Assert presence and produce helpful failure messages using lazy message supplier.
        assertTrue(all.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Document does not contain", substring, all));
        log.info("✅ PDF contains: {}", substring);
    }

    /**
     * Same as assertContains(pdfPath, substring) but for password-protected PDFs.
     *
     * @param pdfPath   path to the PDF file to inspect
     * @param substring expected substring (will be normalized before comparison)
     * @param password  password to open the PDF
     */
    public static void assertContains(String pdfPath, String substring, String password) {
        // Read with password and normalize expected substring.
        String all = PdfUtils.readAllText(pdfPath, password);
        assertTrue(all.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Document does not contain (pwd)", substring, all));
        log.info("✅ PDF (pwd) contains: {}", substring);
    }

    /**
     * Assert that the full text of the PDF contains ALL of the provided substrings.
     * Each substring is normalized before comparison.
     *
     * Useful for asserting multiple independent text expectations in one call.
     *
     * @param pdfPath    path to the PDF file
     * @param substrings list of expected substrings (each will be normalized)
     */
    public static void assertContainsAll(String pdfPath, List<String> substrings) {
        String all = PdfUtils.readAllText(pdfPath);
        // Check each expected substring individually so failures indicate which one is missing.
        for (String s : substrings) {
            assertTrue(all.contains(PdfUtils.normalize(s)),
                    () -> failMsg(pdfPath, "Document missing", s, all));
        }
        log.info("✅ PDF contains all {} substrings", substrings.size());
    }

    /**
     * Assert that the full text does NOT contain the provided substring.
     *
     * @param pdfPath   path to the PDF file
     * @param substring substring that must not appear in document text
     */
    public static void assertNotContains(String pdfPath, String substring) {
        String all = PdfUtils.readAllText(pdfPath);
        assertFalse(all.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Document unexpectedly contains", substring, all));
        log.info("✅ PDF does not contain: {}", substring);
    }

    /**
     * Assert that the provided regular expression finds a match in the full document text.
     *
     * @param pdfPath path to the PDF file
     * @param regex   regular expression to search for
     */
    public static void assertMatchesRegex(String pdfPath, String regex) {
        String all = PdfUtils.readAllText(pdfPath);
        // Use Pattern to search anywhere in the document text.
        assertTrue(Pattern.compile(regex).matcher(all).find(),
                () -> failMsg(pdfPath, "Regex not found", regex, all));
        log.info("✅ PDF matches regex: {}", regex);
    }

    /* ==================== Page-level (text) ==================== */

    /**
     * Assert that a specific page contains the expected substring.
     *
     * Page numbers are typically 1-based (depends on PdfUtils.readPageText implementation).
     *
     * @param pdfPath   path to the PDF file
     * @param page      page index/number to inspect
     * @param substring expected substring (normalized before check)
     */
    public static void assertPageContains(String pdfPath, int page, String substring) {
        // Read only the specific page's text (efficient for large PDFs).
        String txt = PdfUtils.readPageText(pdfPath, page);
        assertTrue(txt.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Page " + page + " does not contain", substring, txt));
        log.info("✅ PDF page {} contains: {}", page, substring);
    }

    /**
     * Assert that a regular expression matches somewhere on the given page.
     *
     * @param pdfPath path to the PDF file
     * @param page    page index/number to inspect
     * @param regex   regular expression to search for on the page
     */
    public static void assertPageMatchesRegex(String pdfPath, int page, String regex) {
        String txt = PdfUtils.readPageText(pdfPath, page);
        assertTrue(Pattern.compile(regex).matcher(txt).find(),
                () -> failMsg(pdfPath, "Page " + page + " regex not found", regex, txt));
        log.info("✅ PDF page {} matches regex: {}", page, regex);
    }

    /* ==================== OPTIONAL OCR (page) ==================== */

    /**
     * Assert that OCR text extracted from a page contains the expected substring.
     *
     * OCR is optional and will be skipped automatically when:
     *  - OCR is disabled via system property (-Dpdf.ocr.enabled=false), OR
     *  - Tesseract is not available on the executing machine's PATH.
     *
     * The method uses JUnit's assumeTrue to mark the test as skipped when OCR is not possible,
     * which is useful in environments without Tesseract (CI agents, developer machines, etc.).
     *
     * @param pdfPath   path to the PDF file
     * @param page      page index/number to OCR
     * @param substring expected substring that must appear in OCR output (normalized before check)
     * @param dpi       rasterization DPI used for OCR (150–300 commonly used; higher = slower)
     * @param lang      Tesseract language code (e.g., "eng")
     */
    public static void assertOcrPageContains(String pdfPath, int page, String substring, float dpi, String lang) {
        // Skip if OCR explicitly disabled via system property.
        assumeTrue(isOcrEnabled(), "OCR disabled by system property: " + PROP_OCR_ENABLED + "=false");
        // Skip if Tesseract binary is not detectable on PATH.
        assumeTrue(isTesseractAvailable(), "Tesseract not available on PATH; OCR test skipped.");

        // If we get here, OCR is enabled and tesseract is present: perform OCR.
        String txt = PdfOcr.ocrPage(pdfPath, page, dpi, lang);
        assertTrue(txt.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "OCR page " + page + " does not contain", substring, txt));
        log.info("✅ OCR page {} contains: {}", page, substring);
    }

    /* ==================== Structure ==================== */

    /**
     * Assert that the PDF has the expected number of pages.
     *
     * @param pdfPath  path to the PDF file
     * @param expected expected page count
     */
    public static void assertPageCountEquals(String pdfPath, int expected) {
        int count = PdfUtils.pageCount(pdfPath);
        assertEquals(expected, count, () -> "Expected pages=" + expected + " but was=" + count + " in " + pdfPath);
        log.info("✅ PDF page count = {}", expected);
    }

    /**
     * Assert that the given path references a PDF file.
     * Delegates to PdfUtils.isPdfFile for file content/structure detection.
     *
     * @param pdfPath path to the file to validate
     */
    public static void assertIsPdf(String pdfPath) {
        assertTrue(PdfUtils.isPdfFile(pdfPath), () -> "Not a PDF: " + pdfPath);
        log.info("✅ File is a PDF: {}", pdfPath);
    }

    /* ==================== Visual diff ==================== */

    /**
     * Render a page from the PDF and compare it visually against an expected PNG image.
     *
     * Steps:
     *  - Renders the PDF page to a temporary/actual PNG (PdfUtils.renderPageToPng).
     *  - Compares actual vs expected images using PdfRenderDiff.compare.
     *  - Asserts that the match meets the tolerance and ratio thresholds.
     *
     * On failure, the assertion message includes the diff ratio and, when available,
     * the path to the generated diff image to aid debugging.
     *
     * @param pdfPath          path to the PDF containing the page to render
     * @param page             page index/number to render
     * @param dpi              render DPI (controls rasterization quality)
     * @param expectedPngPath  path to the expected PNG to compare against
     * @param channelTolerance per-color-channel tolerance (0 = strict)
     * @param maxDiffRatio     maximum acceptable diff ratio (0.0..1.0)
     * @param diffOutPath      path to write visual diff image when a mismatch occurs (optional)
     */
    public static void assertPageVisualEquals(String pdfPath, int page, float dpi,
                                              String expectedPngPath, int channelTolerance,
                                              double maxDiffRatio, String diffOutPath) {
        try {
            // Render the specified page to a PNG. We create an ".actual.png" alongside the expected path.
            String actual = PdfUtils.renderPageToPng(pdfPath, page, dpi,
                    expectedPngPath.replace(".png", ".actual.png"));
            // Compare the actual render to the expected image.
            PdfRenderDiff.DiffResult r = PdfRenderDiff.compare(
                    actual, expectedPngPath, channelTolerance, maxDiffRatio, diffOutPath);
            // Assert that the comparison matched within the provided thresholds.
            assertTrue(r.match, () -> "Page visual mismatch. diffRatio=" + r.diffRatio +
                    (r.diffImagePath != null ? (" diff=" + r.diffImagePath) : ""));
            log.info("✅ Visual match page {} (ratio={})", page, r.diffRatio);
        } catch (Exception e) {
            // Wrap any unexpected exception to provide a clear failure in the test stack trace.
            throw new RuntimeException("Visual comparison failed", e);
        }
    }

    /* ==================== Metadata & forms ==================== */

    /**
     * Assert that the PDF document metadata (Info dictionary) contains an expected substring
     * for a given key. Both actual and expected values are normalized before comparison.
     *
     * @param pdfPath          path to the PDF file
     * @param key              metadata key to look up (e.g., "Title", "Author")
     * @param expectedContains substring expected to appear in the metadata value
     */
    public static void assertMetadataContains(String pdfPath, String key, String expectedContains) {
        Map<String, String> meta = PdfMeta.documentInfo(pdfPath);
        String val = meta.getOrDefault(key, "");
        assertTrue(PdfUtils.normalize(val).contains(PdfUtils.normalize(expectedContains)),
                () -> "Metadata key '" + key + "' does not contain expected value. Actual='" + val + "'");
        log.info("✅ Metadata {} contains '{}'", key, expectedContains);
    }

    /**
     * Assert that a form field in an interactive PDF equals the expected value.
     * Both expected and actual values are normalized prior to equality check.
     *
     * @param pdfPath   path to the PDF file
     * @param fieldName name of the form field to assert
     * @param expected  expected value for the field
     */
    public static void assertFormFieldEquals(String pdfPath, String fieldName, String expected) {
        Map<String, String> fields = PdfMeta.formFields(pdfPath);
        String val = fields.getOrDefault(fieldName, "");
        assertEquals(PdfUtils.normalize(expected), PdfUtils.normalize(val),
                () -> "Field '" + fieldName + "' mismatch. Expected='" + expected + "', Actual='" + val + "'");
        log.info("✅ Field {} equals '{}'", fieldName, expected);
    }

    /* ==================== OCR helpers ==================== */

    /**
     * Returns true if OCR is enabled. Default is true.
     *
     * To explicitly disable OCR across all runs set:
     *  -Dpdf.ocr.enabled=false
     *
     * Any value other than the case-insensitive literal "false" keeps OCR enabled.
     *
     * @return whether OCR is enabled by system property
     */
    public static boolean isOcrEnabled() {
        String prop = System.getProperty(PROP_OCR_ENABLED);
        if (prop == null) return true;                 // default: enabled
        return !"false".equalsIgnoreCase(prop.trim()); // only explicit "false" disables
    }

    /**
     * Detects whether system Tesseract is available on PATH.
     *
     * Implementation details:
     *  - Launches "tesseract -v" to request Tesseract version output.
     *  - redirectErrorStream(true) ensures version output is visible on the standard stream.
     *  - waitFor() blocks until the process completes; a zero exit code is treated as success.
     *
     * This helper is public so test steps can explicitly assert or skip based on Tesseract presence.
     *
     * @return true if tesseract command executes successfully, false otherwise
     */
    public static boolean isTesseractAvailable() {
        try {
            Process p = new ProcessBuilder("tesseract", "-v")
                    .redirectErrorStream(true)
                    .start();
            int code = p.waitFor();
            return code == 0; // version printed successfully
        } catch (Exception e) {
            // Any exception (IO, interrupted, security) indicates Tesseract is not available.
            return false;
        }
    }

    /* ==================== Failure message helper ==================== */

    /**
     * Helper to build a compact, informative failure message for assertions.
     *
     * The message contains:
     *  - A short reason (e.g., "Document does not contain")
     *  - The expected snippet
     *  - The file path under test
     *  - A preview of the actual content (limited to first 1200 chars) to avoid log spam
     *
     * @param path          the PDF path under test
     * @param reason        reason prefix for the failure
     * @param expected      expected value (or pattern) that failed to match
     * @param actualSnippet actual content retrieved (full document or page text)
     * @return formatted failure message
     */
    private static String failMsg(String path, String reason, String expected, String actualSnippet) {
        // Limit the preview length so messages remain readable in test output.
        String preview = actualSnippet == null ? "<null>" :
                actualSnippet.substring(0, Math.min(actualSnippet.length(), 1200));
        return reason + ": \"" + expected + "\" in " + path + "\n--- Preview ---\n" + preview + "\n---------------";
    }
}
