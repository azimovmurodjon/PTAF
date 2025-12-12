package com.ptaf.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PdfMeta
 *
 * Purpose:
 *  - Read PDF document metadata and form fields via PDFBox.
 *
 * Why:
 *  - Some tests must verify author/title/subject or that form fields are populated correctly.
 *
 * Notes:
 *  - Methods return simple maps for easy asserts or logging.
 */
public final class PdfMeta {
    private PdfMeta() {}

    /** Metadata of a non-password PDF. */
    public static Map<String, String> documentInfo(String pdfPath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            return info(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read metadata " + pdfPath, e);
        }
    }

    /** Metadata of a password-protected PDF. */
    public static Map<String, String> documentInfo(String pdfPath, String password) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return info(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read metadata (pwd) " + pdfPath, e);
        }
    }

    /** Form fields of a non-password PDF (AcroForm). */
    public static Map<String, String> formFields(String pdfPath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            return fields(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read form fields " + pdfPath, e);
        }
    }

    /** Form fields of a password-protected PDF (AcroForm). */
    public static Map<String, String> formFields(String pdfPath, String password) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return fields(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read form fields (pwd) " + pdfPath, e);
        }
    }

    /* ---------------- internal helpers ---------------- */

    private static Map<String, String> info(PDDocument doc) {
        PDDocumentInformation i = doc.getDocumentInformation();
        Map<String, String> map = new LinkedHashMap<>();
        if (i != null) {
            put(map, "Title", i.getTitle());
            put(map, "Author", i.getAuthor());
            put(map, "Subject", i.getSubject());
            put(map, "Keywords", i.getKeywords());
            put(map, "Creator", i.getCreator());
            put(map, "Producer", i.getProducer());
            put(map, "CreationDate", i.getCreationDate() != null ? i.getCreationDate().getTime().toString() : null);
            put(map, "ModificationDate", i.getModificationDate() != null ? i.getModificationDate().getTime().toString() : null);
            put(map, "Trapped", i.getTrapped());
        }
        return map;
    }

    private static Map<String, String> fields(PDDocument doc) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        if (form != null) {
            for (PDField f : form.getFields()) {
                put(map, f.getFullyQualifiedName(), f.getValueAsString());
            }
        }
        return map;
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (k != null) m.put(k, v == null ? "" : v);
    }
}