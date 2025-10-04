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
 */
public class PdfSteps {

    /* ==================== Download + store ==================== */

    @When("I download PDF from {string}.{string} saving to {string}")
    public void downloadPdf(String element, String key, String directory) {
        Page page = Hooks.getPage();
        ElementAction act = new ElementActionImpl(page);
        String path = act.performActionPageWithReturn(page, "download", element, key, directory);
        if (path == null) throw new IllegalStateException("Download did not return a path.");
        PdfStore.setLastPdfPath(path);
    }

    @When("I set last PDF from directory {string}")
    public void setLastPdfFromDirectory(String dir) {
        PdfStore.setLastFromDirectory(dir, ".pdf");
    }

    /* ==================== Existence & smoke ==================== */

    @Then("the last PDF should exist")
    public void lastPdfShouldExist() {
        PdfStore.ensureExists();
    }

    @Then("the last PDF should be a valid PDF")
    public void lastPdfShouldBeValidPdf() {
        PdfStore.ensureExists();
        PdfValidator.assertIsPdf(PdfStore.getLastPdfPath());
    }

    /* ==================== Content checks (whole doc) ==================== */

    @Then("the last PDF should contain {string}")
    public void lastPdfShouldContain(String text) {
        PdfStore.ensureExists();
        PdfValidator.assertContains(PdfStore.getLastPdfPath(), text);
    }

    @Then("the last PDF should not contain {string}")
    public void lastPdfShouldNotContain(String text) {
        PdfStore.ensureExists();
        PdfValidator.assertNotContains(PdfStore.getLastPdfPath(), text);
    }

    @Then("the last PDF should contain all:")
    public void lastPdfShouldContainAll(DataTable table) {
        PdfStore.ensureExists();
        List<String> rows = table.asList();
        PdfValidator.assertContainsAll(PdfStore.getLastPdfPath(), rows);
    }

    @Then("the last PDF should match regex {string}")
    public void lastPdfShouldMatchRegex(String regex) {
        PdfStore.ensureExists();
        PdfValidator.assertMatchesRegex(PdfStore.getLastPdfPath(), regex);
    }

    /* ==================== Page-level ==================== */

    @Then("page {int} of the last PDF should contain {string}")
    public void pageOfLastPdfShouldContain(int page, String text) {
        PdfStore.ensureExists();
        PdfValidator.assertPageContains(PdfStore.getLastPdfPath(), page, text);
    }

    @Then("page {int} of the last PDF should match regex {string}")
    public void pageOfLastPdfShouldMatchRegex(int page, String regex) {
        PdfStore.ensureExists();
        PdfValidator.assertPageMatchesRegex(PdfStore.getLastPdfPath(), page, regex);
    }

    /* ==================== Structure ==================== */

    @Then("the last PDF should have {int} pages")
    public void lastPdfShouldHavePages(int pages) {
        PdfStore.ensureExists();
        PdfValidator.assertPageCountEquals(PdfStore.getLastPdfPath(), pages);
    }

    /* ==================== OCR (requires system Tesseract) ==================== */

    @Then("OCR on page {int} of the last PDF with dpi {float} and lang {string} should contain {string}")
    public void ocrShouldContain(int page, float dpi, String lang, String text) {
        PdfStore.ensureExists();
        PdfValidator.assertOcrPageContains(PdfStore.getLastPdfPath(), page, text, dpi, lang);
    }

    /* ==================== Visual diff ==================== */

    @Then("page {int} of the last PDF rendered at {float} dpi should visually equal {string} with tolerance {int} and max diff {double}, diff out {string}")
    public void visualEquals(int page, float dpi, String expectedPng,
                             int channelTolerance, double maxDiffRatio, String diffOutPath) {
        PdfStore.ensureExists();
        PdfValidator.assertPageVisualEquals(PdfStore.getLastPdfPath(), page, dpi, expectedPng, channelTolerance, maxDiffRatio, diffOutPath);
    }

    /* ==================== Metadata & form fields ==================== */

    @Then("the last PDF metadata {string} should contain {string}")
    public void metadataContains(String key, String expectedContains) {
        PdfStore.ensureExists();
        PdfValidator.assertMetadataContains(PdfStore.getLastPdfPath(), key, expectedContains);
    }

    @Then("the last PDF form field {string} should equal {string}")
    public void formFieldEquals(String field, String expected) {
        PdfStore.ensureExists();
        PdfValidator.assertFormFieldEquals(PdfStore.getLastPdfPath(), field, expected);
    }

    /* ==================== Utility ==================== */

    @Then("I print first {int} chars of last PDF")
    public void printFirstChars(int limit) {
        PdfStore.ensureExists();
        String txt = PdfUtils.readAllText(PdfStore.getLastPdfPath());
        System.out.println(txt.substring(0, Math.min(limit, txt.length())));
        assertTrue(txt.length() > 0, "PDF text is empty.");
    }
}