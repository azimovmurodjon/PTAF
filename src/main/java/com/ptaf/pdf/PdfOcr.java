package com.ptaf.pdf;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * PdfOcr
 *
 * Purpose:
 *  - Provide OCR extraction for scanned PDFs without adding a Java OCR library.
 *  - Renders the desired page to PNG and invokes system "tesseract" to read text.
 *
 * Requirements:
 *  - Tesseract must be installed and available on PATH (mac: brew install tesseract, linux: apt-get install tesseract-ocr).
 *  - You may set TESSDATA_PREFIX in your environment for custom language packs.
 *
 * When to use:
 *  - When PdfUtils.read*Text returns little/no text because the PDF contains only images.
 */
public final class PdfOcr {
    private PdfOcr() {}

    /**
     * OCR a single page using system tesseract (1-based page index).
     * @param dpi 150–300 usually yields good results (higher = slower).
     * @param lang Tesseract language code, e.g., "eng".
     */
    public static String ocrPage(String pdfPath, int pageNumber, float dpi, String lang) {
        try {
            File tmp = File.createTempFile("ocr_page_" + pageNumber + "_", ".png");
            String png = PdfUtils.renderPageToPng(pdfPath, pageNumber, dpi, tmp.getAbsolutePath());
            return runTesseract(png, lang);
        } catch (Exception e) {
            throw new RuntimeException("OCR failed for " + pdfPath + " page " + pageNumber, e);
        }
    }

    /** Password-protected variant. */
    public static String ocrPage(String pdfPath, String password, int pageNumber, float dpi, String lang) {
        try {
            File tmp = File.createTempFile("ocr_page_" + pageNumber + "_", ".png");
            String png = PdfUtils.renderPageToPng(pdfPath, password, pageNumber, dpi, tmp.getAbsolutePath());
            return runTesseract(png, lang);
        } catch (Exception e) {
            throw new RuntimeException("OCR failed (pwd) for " + pdfPath + " page " + pageNumber, e);
        }
    }

    /** Invoke system tesseract and return normalized text. */
    private static String runTesseract(String pngPath, String lang) throws Exception {
        String language = (lang == null || lang.isBlank()) ? "eng" : lang;
        ProcessBuilder pb = new ProcessBuilder("tesseract", pngPath, "stdout", "-l", language);
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            int code = p.waitFor();
            if (code != 0) {
                throw new IllegalStateException("Tesseract exited with code " + code + ". " +
                        "Ensure Tesseract is installed and available on PATH.");
            }
            return PdfUtils.normalize(sb.toString());
        }
    }
}