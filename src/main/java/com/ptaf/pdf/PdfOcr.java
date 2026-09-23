package com.ptaf.pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * PdfOcr
 *
 * Purpose:
 *  - Provide a lightweight way to OCR scanned PDF pages without embedding a Java OCR library.
 *  - Renders a specific PDF page to a PNG file and invokes the external "tesseract" binary to extract text.
 *
 * Important notes for testers and integrators:
 *  - This class relies on an external Tesseract installation being available on the system PATH.
 *    (mac: brew install tesseract, linux: apt-get install tesseract-ocr, or install via your package manager).
 *  - You may need to set the TESSDATA_PREFIX environment variable if custom language data is installed in a non-standard location.
 *  - The rendered PNG file is created as a temporary file. This class currently does not explicitly delete the temp file.
 *    Temp files are created using File.createTempFile and will be placed in the JVM/temp directory (usually /tmp on Unix).
 *  - Page numbers are 1-based (i.e., the first page is pageNumber = 1).
 *
 * When to use:
 *  - Use this when text extraction via PdfUtils.read*Text returns little or no text because the PDF is image-only (scanned).
 *
 * Thread-safety and performance:
 *  - This class only contains stateless static helpers and is safe to call from multiple threads, but each call spawns an external process.
 *  - OCR is IO and CPU intensive; keep DPI values moderate (150–300 recommended). Higher DPI improves accuracy but increases CPU and disk usage.
 */
public final class PdfOcr {
    // Prevent instantiation - utility class only.
    private PdfOcr() {}

    /**
     * OCR a single page of a PDF using the system-installed Tesseract OCR engine.
     *
     * Typical usage:
     *  String text = PdfOcr.ocrPage("/path/to/file.pdf", 1, 200f, "eng");
     *
     * @param pdfPath    Absolute or relative path to the PDF file to OCR.
     * @param pageNumber 1-based page index (1 = first page).
     * @param dpi        Rendering DPI used when creating the PNG. 150–300 is a common range. Higher DPI improves OCR fidelity but is slower.
     * @param lang       Tesseract language code (example: "eng"). If null or blank, defaults to "eng".
     * @return The extracted text from the requested page. Text is run through PdfUtils.normalize(...) before returning.
     * @throws RuntimeException If rendering or OCR fails; wraps underlying exceptions with a descriptive message.
     */
    public static String ocrPage(String pdfPath, int pageNumber, float dpi, String lang) {
        try {
            // Create a temporary PNG file to hold the rendered page image.
            // File.createTempFile will choose a unique name and place it in the system temp directory.
            File tmp = File.createTempFile("ocr_page_" + pageNumber + "_", ".png");

            // Render the requested page to the temporary PNG.
            // This delegates to PdfUtils, which is expected to write the image to the provided path.
            String png = PdfUtils.renderPageToPng(pdfPath, pageNumber, dpi, tmp.getAbsolutePath());

            // Run Tesseract on the generated PNG and return the OCR text.
            return runTesseract(png, lang);
        } catch (Exception e) {
            // Wrap any exception in a RuntimeException to provide context to callers/testers.
            // Tests should inspect the cause for the underlying error (rendering, process startup, etc.).
            throw new RuntimeException("OCR failed for " + pdfPath + " page " + pageNumber, e);
        }
    }

    /**
     * Password-protected PDF variant of ocrPage.
     *
     * Use when the PDF is encrypted and a password is required to render pages.
     *
     * @param pdfPath    Path to the password-protected PDF.
     * @param password   Password for the PDF; pass an empty string or null if not needed.
     * @param pageNumber 1-based page index.
     * @param dpi        Rendering DPI for the PNG.
     * @param lang       Tesseract language code; defaults to "eng" when null/blank.
     * @return OCR text for the requested page.
     * @throws RuntimeException If rendering or OCR fails; wraps the underlying exception.
     */
    public static String ocrPage(String pdfPath, String password, int pageNumber, float dpi, String lang) {
        try {
            // Create a temporary PNG file for the rendered page image.
            File tmp = File.createTempFile("ocr_page_" + pageNumber + "_", ".png");

            // Render the page using the provided password. PdfUtils should handle decryption and rendering.
            String png = PdfUtils.renderPageToPng(pdfPath, password, pageNumber, dpi, tmp.getAbsolutePath());

            // Run Tesseract on the generated PNG and return the normalized text.
            return runTesseract(png, lang);
        } catch (Exception e) {
            // Include "(pwd)" in the message to indicate this was the password-protected variant.
            throw new RuntimeException("OCR failed (pwd) for " + pdfPath + " page " + pageNumber, e);
        }
    }

    /**
     * Internal helper that invokes the external Tesseract binary to OCR a PNG image file.
     *
     * Behavior and implementation notes:
     *  - If the provided lang is null or blank, "eng" is used as the default language.
     *  - This method builds a Process to run: tesseract <pngPath> stdout -l <language>
     *    and captures stdout as the OCR result. stderr is merged into stdout to facilitate debugging.
     *  - The stdout stream is read using UTF-8 encoding.
     *  - If Tesseract exits with a non-zero exit code an IllegalStateException is thrown with a helpful message.
     *  - The returned text is normalized by calling PdfUtils.normalize(...) before being returned.
     *
     * Tester's checklist if OCR fails:
     *  - Verify that the 'tesseract' executable is installed and available on the PATH for the user running the JVM.
     *  - If using non-default language packs, ensure TESSDATA_PREFIX is set and the language data is installed.
     *  - Inspect the cause of exceptions for permission issues or errors writing/reading the temp PNG.
     *
     * @param pngPath Absolute path to the PNG image to OCR.
     * @param lang    Tesseract language code (e.g., "eng"). If null or blank, defaults to "eng".
     * @return The normalized OCR text output by Tesseract.
     * @throws Exception Propagates exceptions (IO errors, interrupted exceptions, etc.) to callers for handling.
     */
    private static String runTesseract(String pngPath, String lang) throws Exception {
        // Choose default language when none provided.
        String language = (lang == null || lang.isBlank()) ? "eng" : lang;

        // Build the command: tesseract <pngPath> stdout -l <language>
        // Using "stdout" as the output file argument tells tesseract to write results to STDOUT.
        ProcessBuilder pb = new ProcessBuilder("tesseract", pngPath, "stdout", "-l", language);

        // Merge stderr into stdout so we can capture any informational or error messages through the same stream.
        pb.redirectErrorStream(true);

        // Start the process.
        Process p = pb.start();

        // Read the combined output stream (stdout + stderr) using UTF-8 encoding.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;

            // Read all lines produced by Tesseract.
            while ((line = br.readLine()) != null) sb.append(line).append('\n');

            // Wait for the process to finish and inspect the exit code.
            int code = p.waitFor();
            if (code != 0) {
                // Non-zero exit code indicates Tesseract failed. Include guidance in the exception message.
                throw new IllegalStateException("Tesseract exited with code " + code + ". " +
                        "Ensure Tesseract is installed and available on PATH.");
            }

            // Normalize and return the extracted text (PdfUtils.normalize may trim, normalize whitespace, etc.).
            return PdfUtils.normalize(sb.toString());
        }
    }
}
