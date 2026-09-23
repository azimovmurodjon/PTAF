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
 * Utility class to extract metadata and AcroForm field values from PDF documents using PDFBox.
 *
 * Purpose:
 *  - Read PDF document metadata (Title, Author, Subject, etc.) and form fields (AcroForm).
 *
 * Why:
 *  - Tests frequently need to assert that a generated PDF contains specific metadata
 *    or that particular form fields are populated with expected values.
 *
 * Notes:
 *  - Public methods return simple Map<String, String> structures (LinkedHashMap) for easy assertions
 *    and deterministic ordering when logging or comparing results.
 *  - This class does not modify PDFs; it only reads information.
 *  - All IO is wrapped in try-with-resources to ensure PDDocument is closed.
 *
 * Thread-safety:
 *  - Methods are stateless and safe to call from multiple threads concurrently.
 */
public final class PdfMeta {
    // Private constructor to prevent instantiation - utility class only.
    private PdfMeta() {}

    /**
     * Read standard document information (metadata) from a non-password-protected PDF.
     *
     * The returned map contains entries such as Title, Author, Subject, Keywords, Creator,
     * Producer, CreationDate, ModificationDate and Trapped when present. Dates are converted to
     * their Date#getTime().toString() representation for a simple string value.
     *
     * @param pdfPath filesystem path to the PDF file
     * @return LinkedHashMap of metadata keys to values (empty string for null values, key omitted if null)
     * @throws RuntimeException wrapped if loading or reading the PDF fails
     */
    public static Map<String, String> documentInfo(String pdfPath) {
        // Use try-with-resources to ensure the PDDocument is closed even if an exception occurs.
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            // Delegate to shared helper that extracts information from an opened PDDocument.
            return info(doc);
        } catch (Exception e) {
            // Wrap low-level exceptions to provide context to callers/tests.
            throw new RuntimeException("Failed to read metadata " + pdfPath, e);
        }
    }

    /**
     * Read standard document information (metadata) from a password-protected PDF.
     *
     * This is the same as documentInfo(String) but supplies the owner/user password during load.
     *
     * @param pdfPath filesystem path to the PDF file
     * @param password password required to open the PDF
     * @return LinkedHashMap of metadata keys to values
     * @throws RuntimeException wrapped if loading or reading the PDF fails (including wrong password)
     */
    public static Map<String, String> documentInfo(String pdfPath, String password) {
        // Pass the password to PDDocument.load to open encrypted PDFs.
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return info(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read metadata (pwd) " + pdfPath, e);
        }
    }

    /**
     * Read AcroForm form field names and their current string values from a non-password PDF.
     *
     * Returns a map where each entry key is the fully-qualified field name and the value is the
     * field's value as returned by PDField#getValueAsString(). If a field's value is null it is
     * returned as an empty string to simplify assertions.
     *
     * @param pdfPath filesystem path to the PDF file
     * @return LinkedHashMap of field names to their string values
     * @throws RuntimeException wrapped if loading or reading the PDF fails
     */
    public static Map<String, String> formFields(String pdfPath) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
            return fields(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read form fields " + pdfPath, e);
        }
    }

    /**
     * Read AcroForm form field names and their current string values from a password-protected PDF.
     *
     * @param pdfPath filesystem path to the PDF file
     * @param password password required to open the PDF
     * @return LinkedHashMap of field names to their string values
     * @throws RuntimeException wrapped if loading or reading the PDF fails (including wrong password)
     */
    public static Map<String, String> formFields(String pdfPath, String password) {
        try (PDDocument doc = PDDocument.load(new File(pdfPath), password)) {
            return fields(doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read form fields (pwd) " + pdfPath, e);
        }
    }

    /* ---------------- internal helpers ---------------- */

    /**
     * Extracts document information fields from an already-open PDDocument into a LinkedHashMap.
     *
     * LinkedHashMap is used to preserve insertion order which is helpful for predictable logging
     * and testing output comparisons.
     *
     * Keys are standard PDF metadata keys (Title, Author, Subject, etc.). Date values are converted
     * to a string via Date#getTime().toString() for a simple textual representation. If the
     * PDDocumentInformation object is null, an empty map is returned.
     *
     * @param doc already opened PDDocument (caller is responsible for closing)
     * @return LinkedHashMap of metadata keys to values (empty string for null values)
     */
    private static Map<String, String> info(PDDocument doc) {
        PDDocumentInformation i = doc.getDocumentInformation();
        Map<String, String> map = new LinkedHashMap<>();
        if (i != null) {
            // For each metadata field, add it to the map using the put helper which
            // normalizes null values to empty strings and ignores null keys.
            put(map, "Title", i.getTitle());
            put(map, "Author", i.getAuthor());
            put(map, "Subject", i.getSubject());
            put(map, "Keywords", i.getKeywords());
            put(map, "Creator", i.getCreator());
            put(map, "Producer", i.getProducer());
            // CreationDate and ModificationDate are optional; convert to string if present.
            put(map, "CreationDate", i.getCreationDate() != null ? i.getCreationDate().getTime().toString() : null);
            put(map, "ModificationDate", i.getModificationDate() != null ? i.getModificationDate().getTime().toString() : null);
            put(map, "Trapped", i.getTrapped());
        }
        // Return possibly-empty map; callers can assert map contents as needed.
        return map;
    }

    /**
     * Extracts AcroForm fields and their values from an already-open PDDocument.
     *
     * Iterates over top-level fields returned by PDAcroForm#getFields(). For each PDField the
     * fully-qualified name is used as the key and PDField#getValueAsString() as the value.
     *
     * @param doc already opened PDDocument (caller is responsible for closing)
     * @return LinkedHashMap of fully-qualified field names to their string values (empty string for null values)
     * @throws Exception if accessing the form or fields triggers PDFBox exceptions
     */
    private static Map<String, String> fields(PDDocument doc) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        // Acquire the AcroForm from the document catalogue; may be null if no form exists.
        PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
        if (form != null) {
            // Iterate top-level fields. Note: nested fields may be represented via fully-qualified names.
            for (PDField f : form.getFields()) {
                put(map, f.getFullyQualifiedName(), f.getValueAsString());
            }
        }
        return map;
    }

    /**
     * Helper to put a key/value into the map while normalizing null values and ignoring null keys.
     *
     * Behavior:
     *  - If key (k) is null -> do nothing (no entry inserted).
     *  - If value (v) is null -> store an empty string ("") for simpler equality/assertions in tests.
     *
     * @param m target map
     * @param k key (may be null)
     * @param v value (may be null)
     */
    private static void put(Map<String, String> m, String k, String v) {
        if (k != null) m.put(k, v == null ? "" : v);
    }
}
