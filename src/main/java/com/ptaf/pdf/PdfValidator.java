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

    public static void assertContains(String pdfPath, String substring) {
        String all = PdfUtils.readAllText(pdfPath);
        assertTrue(all.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Document does not contain", substring, all));
        log.info("✅ PDF contains: {}", substring);
    }

    public static void assertContains(String pdfPath, String substring, String password) {
        String all = PdfUtils.readAllText(pdfPath, password);
        assertTrue(all.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Document does not contain (pwd)", substring, all));
        log.info("✅ PDF (pwd) contains: {}", substring);
    }

    public static void assertContainsAll(String pdfPath, List<String> substrings) {
        String all = PdfUtils.readAllText(pdfPath);
        for (String s : substrings) {
            assertTrue(all.contains(PdfUtils.normalize(s)),
                    () -> failMsg(pdfPath, "Document missing", s, all));
        }
        log.info("✅ PDF contains all {} substrings", substrings.size());
    }

    public static void assertNotContains(String pdfPath, String substring) {
        String all = PdfUtils.readAllText(pdfPath);
        assertFalse(all.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Document unexpectedly contains", substring, all));
        log.info("✅ PDF does not contain: {}", substring);
    }

    public static void assertMatchesRegex(String pdfPath, String regex) {
        String all = PdfUtils.readAllText(pdfPath);
        assertTrue(Pattern.compile(regex).matcher(all).find(),
                () -> failMsg(pdfPath, "Regex not found", regex, all));
        log.info("✅ PDF matches regex: {}", regex);
    }

    /* ==================== Page-level (text) ==================== */

    public static void assertPageContains(String pdfPath, int page, String substring) {
        String txt = PdfUtils.readPageText(pdfPath, page);
        assertTrue(txt.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "Page " + page + " does not contain", substring, txt));
        log.info("✅ PDF page {} contains: {}", page, substring);
    }

    public static void assertPageMatchesRegex(String pdfPath, int page, String regex) {
        String txt = PdfUtils.readPageText(pdfPath, page);
        assertTrue(Pattern.compile(regex).matcher(txt).find(),
                () -> failMsg(pdfPath, "Page " + page + " regex not found", regex, txt));
        log.info("✅ PDF page {} matches regex: {}", page, regex);
    }

    /* ==================== OPTIONAL OCR (page) ==================== */

    /**
     * Assert that OCR text on a page contains the expected substring.
     * This assertion will be SKIPPED automatically when:
     *  - OCR is disabled via -Dpdf.ocr.enabled=false, OR
     *  - Tesseract is not available on PATH (company policy, etc.).
     *
     * @param dpi  150–300 usually yields good results (higher = slower).
     * @param lang Tesseract language code (e.g., "eng").
     */
    public static void assertOcrPageContains(String pdfPath, int page, String substring, float dpi, String lang) {
        // Skip if OCR is disabled or Tesseract is not available.
        assumeTrue(isOcrEnabled(), "OCR disabled by system property: " + PROP_OCR_ENABLED + "=false");
        assumeTrue(isTesseractAvailable(), "Tesseract not available on PATH; OCR test skipped.");

        // If we get here, OCR is enabled and available.
        String txt = PdfOcr.ocrPage(pdfPath, page, dpi, lang);
        assertTrue(txt.contains(PdfUtils.normalize(substring)),
                () -> failMsg(pdfPath, "OCR page " + page + " does not contain", substring, txt));
        log.info("✅ OCR page {} contains: {}", page, substring);
    }

    /* ==================== Structure ==================== */

    public static void assertPageCountEquals(String pdfPath, int expected) {
        int count = PdfUtils.pageCount(pdfPath);
        assertEquals(expected, count, () -> "Expected pages=" + expected + " but was=" + count + " in " + pdfPath);
        log.info("✅ PDF page count = {}", expected);
    }

    public static void assertIsPdf(String pdfPath) {
        assertTrue(PdfUtils.isPdfFile(pdfPath), () -> "Not a PDF: " + pdfPath);
        log.info("✅ File is a PDF: {}", pdfPath);
    }

    /* ==================== Visual diff ==================== */

    public static void assertPageVisualEquals(String pdfPath, int page, float dpi,
                                              String expectedPngPath, int channelTolerance,
                                              double maxDiffRatio, String diffOutPath) {
        try {
            String actual = PdfUtils.renderPageToPng(pdfPath, page, dpi,
                    expectedPngPath.replace(".png", ".actual.png"));
            PdfRenderDiff.DiffResult r = PdfRenderDiff.compare(
                    actual, expectedPngPath, channelTolerance, maxDiffRatio, diffOutPath);
            assertTrue(r.match, () -> "Page visual mismatch. diffRatio=" + r.diffRatio +
                    (r.diffImagePath != null ? (" diff=" + r.diffImagePath) : ""));
            log.info("✅ Visual match page {} (ratio={})", page, r.diffRatio);
        } catch (Exception e) {
            throw new RuntimeException("Visual comparison failed", e);
        }
    }

    /* ==================== Metadata & forms ==================== */

    public static void assertMetadataContains(String pdfPath, String key, String expectedContains) {
        Map<String, String> meta = PdfMeta.documentInfo(pdfPath);
        String val = meta.getOrDefault(key, "");
        assertTrue(PdfUtils.normalize(val).contains(PdfUtils.normalize(expectedContains)),
                () -> "Metadata key '" + key + "' does not contain expected value. Actual='" + val + "'");
        log.info("✅ Metadata {} contains '{}'", key, expectedContains);
    }

    public static void assertFormFieldEquals(String pdfPath, String fieldName, String expected) {
        Map<String, String> fields = PdfMeta.formFields(pdfPath);
        String val = fields.getOrDefault(fieldName, "");
        assertEquals(PdfUtils.normalize(expected), PdfUtils.normalize(val),
                () -> "Field '" + fieldName + "' mismatch. Expected='" + expected + "', Actual='" + val + "'");
        log.info("✅ Field {} equals '{}'", fieldName, expected);
    }

    /* ==================== OCR helpers ==================== */

    /** Returns true if OCR is enabled (default true). Disable with -Dpdf.ocr.enabled=false */
    public static boolean isOcrEnabled() {
        String prop = System.getProperty(PROP_OCR_ENABLED);
        if (prop == null) return true;                 // default: enabled
        return !"false".equalsIgnoreCase(prop.trim()); // only explicit "false" disables
    }

    /**
     * Detects whether system Tesseract is available on PATH.
     * We keep this public so steps can assert/skip explicitly if desired.
     */
    public static boolean isTesseractAvailable() {
        try {
            Process p = new ProcessBuilder("tesseract", "-v")
                    .redirectErrorStream(true)
                    .start();
            int code = p.waitFor();
            return code == 0; // version printed successfully
        } catch (Exception e) {
            return false;
        }
    }

    /* ==================== Failure message helper ==================== */

    private static String failMsg(String path, String reason, String expected, String actualSnippet) {
        String preview = actualSnippet == null ? "<null>" :
                actualSnippet.substring(0, Math.min(actualSnippet.length(), 1200));
        return reason + ": \"" + expected + "\" in " + path + "\n--- Preview ---\n" + preview + "\n---------------";
    }
}