package com.ptaf.stepdefinitions;

import com.microsoft.playwright.Page;
import com.ptaf.hooks.Hooks;
import com.ptaf.pdf.*;
import com.ptaf.ui.action_performer.ElementActionImpl;
import com.ptaf.ui.interfaces.ElementAction;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PdfSteps
 *
 * Purpose:
 *  - Minimal step glue to connect Gherkin to the reusable PDF library.
 *
 * Why:
 *  - Keeps steps thin and readable; heavy lifting stays in src/main classes.
 *  - Works with your existing Hooks and Playwright ActionPerformer.
 *
 * Notes for testers:
 *  - These step definitions assume a Playwright Page object is available via Hooks.getPage().
 *  - Download steps store the last used PDF path in PdfStore for subsequent validations.
 *  - Several assertions will call PdfStore.ensureExists() to guarantee there is a "last PDF" set.
 *  - OCR steps require a system Tesseract installation and the language data for the given lang code.
 */
public class PdfSteps {

    /* ==================== Download + store ==================== */

    /**
     * Downloads a PDF from a page element and saves it to the given directory.
     *
     * Gherkin example:
     *   When I download PDF from "myButton.download" saving to "target/downloads"
     *
     * The method:
     *  - Obtains the Playwright Page from Hooks.
     *  - Creates an ElementAction implementation that can perform a "download" action.
     *  - Calls the action and expects a path string back.
     *  - Stores the returned path as the "last PDF" in PdfStore for future assertions.
     *
     * @param element  the locator or identifier of the element to trigger the download (as used by your ElementAction)
     * @param key      optional key or attribute used by your ElementAction implementation (keeps signature compatible)
     * @param directory directory path to save the downloaded file to
     * @throws IllegalStateException if the download action did not return a file path
     */
    @When("I download PDF from {string}.{string} saving to {string}")
    public void downloadPdf(String element, String key, String directory) {
        // Retrieve the current Playwright Page instance from test Hooks.
        Page page = Hooks.getPage();
        // Create a concrete ElementAction to perform UI interactions (download in this case).
        ElementAction act = new ElementActionImpl(page);
        // Perform the action which is expected to return the saved file path.
        String path = act.performActionPageWithReturn(page, "download", element, key, directory);
        // Fail fast if the action did not return a valid path (prevents subsequent null-pointer use).
        if (path == null) throw new IllegalStateException("Download did not return a path.");
        // Store path as the last downloaded PDF so other steps can reference it.
        PdfStore.setLastPdfPath(path);
    }

    /**
     * Sets the "last PDF" to the most recent PDF file found in the given directory.
     *
     * Gherkin example:
     *   When I set last PDF from directory "target/downloads"
     *
     * This is a convenience for when downloads are performed outside of the ElementAction flow
     * or when you want to operate on an existing PDF file in a directory.
     *
     * @param dir the directory to search for the most recent PDF file
     */
    @When("I set last PDF from directory {string}")
    public void setLastPdfFromDirectory(String dir) {
        // Look up the most recent file with .pdf extension in the provided directory.
        PdfStore.setLastFromDirectory(dir, ".pdf");
    }

    /* ==================== Existence & smoke ==================== */

    /**
     * Asserts that the "last PDF" path exists and points to an existing file.
     *
     * Gherkin example:
     *   Then the last PDF should exist
     *
     * This step will throw a runtime exception if no last PDF is set or if the file is missing.
     */
    @Then("the last PDF should exist")
    public void lastPdfShouldExist() {
        PdfStore.ensureExists();
    }

    /**
     * Asserts that the "last PDF" exists and is a valid PDF file (basic format validation).
     *
     * Gherkin example:
     *   Then the last PDF should be a valid PDF
     *
     * This typically checks PDF file signatures and basic structure; implementation lives in PdfValidator.
     */
    @Then("the last PDF should be a valid PDF")
    public void lastPdfShouldBeValidPdf() {
        // Ensure we have a last PDF to check.
        PdfStore.ensureExists();
        // Delegate strong PDF validation to the PdfValidator utility.
        PdfValidator.assertIsPdf(PdfStore.getLastPdfPath());
    }

    /* ==================== Content checks (whole doc) ==================== */

    /**
     * Asserts that the entire text content of the last PDF contains the provided substring.
     *
     * Gherkin example:
     *   Then the last PDF should contain "Invoice #1234"
     *
     * @param text substring expected to appear somewhere in the PDF text.
     */
    @Then("the last PDF should contain {string}")
    public void lastPdfShouldContain(String text) {
        PdfStore.ensureExists();
        PdfValidator.assertContains(PdfStore.getLastPdfPath(), text);
    }

    /**
     * Asserts that the entire text content of the last PDF does NOT contain the provided substring.
     *
     * Gherkin example:
     *   Then the last PDF should not contain "DO NOT INCLUDE"
     *
     * @param text substring expected to be absent from the PDF text.
     */
    @Then("the last PDF should not contain {string}")
    public void lastPdfShouldNotContain(String text) {
        PdfStore.ensureExists();
        PdfValidator.assertNotContains(PdfStore.getLastPdfPath(), text);
    }

    /**
     * Asserts that the last PDF contains all strings provided in a Cucumber DataTable.
     *
     * Gherkin example:
     *   Then the last PDF should contain all:
     *     | Invoice #1234 |
     *     | Total: $45.67 |
     *
     * @param table Cucumber DataTable where each row is a string to look for in the PDF.
     */
    @Then("the last PDF should contain all:")
    public void lastPdfShouldContainAll(DataTable table) {
        PdfStore.ensureExists();
        // Convert Cucumber data table rows into a list of expected strings.
        List<String> rows = table.asList();
        PdfValidator.assertContainsAll(PdfStore.getLastPdfPath(), rows);
    }

    /**
     * Asserts that the last PDF matches the provided regular expression somewhere in its text.
     *
     * Gherkin example:
     *   Then the last PDF should match regex "\\d{4}-\\d{2}-\\d{2}"
     *
     * @param regex the regular expression to match against the PDF text content.
     */
    @Then("the last PDF should match regex {string}")
    public void lastPdfShouldMatchRegex(String regex) {
        PdfStore.ensureExists();
        PdfValidator.assertMatchesRegex(PdfStore.getLastPdfPath(), regex);
    }

    /* ==================== Page-level ==================== */

    /**
     * Asserts that a specific page of the last PDF contains the provided text.
     *
     * Gherkin example:
     *   Then page 2 of the last PDF should contain "Terms and Conditions"
     *
     * @param page one-based page index to check (match the PdfValidator's expected indexing)
     * @param text substring expected to appear on the specified page
     */
    @Then("page {int} of the last PDF should contain {string}")
    public void pageOfLastPdfShouldContain(int page, String text) {
        PdfStore.ensureExists();
        PdfValidator.assertPageContains(PdfStore.getLastPdfPath(), page, text);
    }

    /**
     * Asserts that a specific page of the last PDF matches a regular expression.
     *
     * Gherkin example:
     *   Then page 1 of the last PDF should match regex "Invoice\\s+#\\d+"
     *
     * @param page one-based page index to check
     * @param regex regular expression to match against the text of the given page
     */
    @Then("page {int} of the last PDF should match regex {string}")
    public void pageOfLastPdfShouldMatchRegex(int page, String regex) {
        PdfStore.ensureExists();
        PdfValidator.assertPageMatchesRegex(PdfStore.getLastPdfPath(), page, regex);
    }

    /* ==================== Structure ==================== */

    /**
     * Asserts that the last PDF has the expected number of pages.
     *
     * Gherkin example:
     *   Then the last PDF should have 3 pages
     *
     * @param pages expected page count
     */
    @Then("the last PDF should have {int} pages")
    public void lastPdfShouldHavePages(int pages) {
        PdfStore.ensureExists();
        PdfValidator.assertPageCountEquals(PdfStore.getLastPdfPath(), pages);
    }

    /* ==================== OCR (requires system Tesseract) ==================== */

    /**
     * Runs OCR on a specific page of the last PDF and asserts the extracted text contains the given substring.
     *
     * Notes:
     *  - This step requires a system Tesseract installation and the appropriate language data files.
     *  - DPI controls the rendering resolution used for OCR which can affect accuracy.
     *
     * Gherkin example:
     *   Then OCR on page 1 of the last PDF with dpi 300.0 and lang "eng" should contain "Authorized Signature"
     *
     * @param page one-based page index to OCR
     * @param dpi  rendering resolution used when converting page to image for OCR
     * @param lang Tesseract language code (e.g., "eng", "fra")
     * @param text substring expected to appear in OCR output
     */
    @Then("OCR on page {int} of the last PDF with dpi {float} and lang {string} should contain {string}")
    public void ocrShouldContain(int page, float dpi, String lang, String text) {
        PdfStore.ensureExists();
        PdfValidator.assertOcrPageContains(PdfStore.getLastPdfPath(), page, text, dpi, lang);
    }

    /* ==================== Visual diff ==================== */

    /**
     * Renders a PDF page to an image at the specified DPI and compares it visually against an expected PNG.
     *
     * Gherkin example:
     *   Then page 1 of the last PDF rendered at 300.0 dpi should visually equal "expected.png" with tolerance 3 and max diff 0.02, diff out "diff.png"
     *
     * Parameters:
     *  - page: one-based page index to render and compare
     *  - dpi: rendering resolution in dots-per-inch
     *  - expectedPng: path to a baseline PNG image to compare against
     *  - channelTolerance: per-channel pixel tolerance (e.g., 0..255) to ignore minor differences
     *  - maxDiffRatio: maximum allowed ratio of differing pixels (0.0 - 1.0)
     *  - diffOutPath: path to write the generated diff image if differences are found (useful for debugging)
     *
     * This delegates to PdfValidator which performs the image-based comparison.
     *
     * @param page one-based page index
     * @param dpi rendering resolution for page rasterization
     * @param expectedPng baseline image path to compare
     * @param channelTolerance allowed per-channel difference threshold
     * @param maxDiffRatio maximum allowed ratio of differing pixels
     * @param diffOutPath output path to write visual diff image if differences exceed tolerance
     */
    @Then("page {int} of the last PDF rendered at {float} dpi should visually equal {string} with tolerance {int} and max diff {double}, diff out {string}")
    public void visualEquals(int page, float dpi, String expectedPng,
                             int channelTolerance, double maxDiffRatio, String diffOutPath) {
        PdfStore.ensureExists();
        PdfValidator.assertPageVisualEquals(PdfStore.getLastPdfPath(), page, dpi, expectedPng, channelTolerance, maxDiffRatio, diffOutPath);
    }

    /* ==================== Metadata & form fields ==================== */

    /**
     * Asserts that a metadata entry in the last PDF contains the expected substring.
     *
     * Gherkin example:
     *   Then the last PDF metadata "Author" should contain "Acme Inc"
     *
     * @param key metadata key to inspect (e.g., "Author", "Title", "Subject")
     * @param expectedContains substring expected to be present in the metadata value
     */
    @Then("the last PDF metadata {string} should contain {string}")
    public void metadataContains(String key, String expectedContains) {
        PdfStore.ensureExists();
        PdfValidator.assertMetadataContains(PdfStore.getLastPdfPath(), key, expectedContains);
    }

    /**
     * Asserts that a form field value in the last PDF equals the expected string.
     *
     * Gherkin example:
     *   Then the last PDF form field "customer_name" should equal "Jane Doe"
     *
     * @param field form field name/identifier
     * @param expected expected value of the form field
     */
    @Then("the last PDF form field {string} should equal {string}")
    public void formFieldEquals(String field, String expected) {
        PdfStore.ensureExists();
        PdfValidator.assertFormFieldEquals(PdfStore.getLastPdfPath(), field, expected);
    }

    /* ==================== Utility ==================== */

    /**
     * Prints the first N characters of the last PDF's extracted text to standard output and asserts PDF text is non-empty.
     *
     * Useful for quick debugging during test runs to see a PDF's text content without opening files.
     *
     * Gherkin example:
     *   Then I print first 200 chars of last PDF
     *
     * @param limit maximum number of characters to print (will print less if PDF text is shorter)
     */
    @Then("I print first {int} chars of last PDF")
    public void printFirstChars(int limit) {
        PdfStore.ensureExists();
        // Read all available text extracted from the PDF.
        String txt = PdfUtils.readAllText(PdfStore.getLastPdfPath());
        // Print only up to the requested limit to avoid overwhelming test logs.
        System.out.println(txt.substring(0, Math.min(limit, txt.length())));
        // Ensure the PDF actually contains text; this will fail the test if empty.
        assertTrue(txt.length() > 0, "PDF text is empty.");
    }
}
