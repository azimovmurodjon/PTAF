package com.ptaf.stepdefinitions;

import com.ptaf.csv.CsvContext;
import com.ptaf.csv.CsvFileHandler;
import com.ptaf.utils.ConfigurationProperties;
import com.ptaf.xml.XmlContext;
import com.ptaf.xml.XmlFileHandler;
import com.ptaf.zip.ZipContext;
import com.ptaf.zip.ZipFileHandler;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * ZipSteps — Cucumber step definitions for ZIP file operations within FNB-ETAF.
 *
 * <p>This class provides steps for:</p>
 * <ul>
 *   <li>Unzipping a downloaded or local ZIP file to a configurable extraction directory.</li>
 *   <li>Discovering files inside the ZIP by name or extension.</li>
 *   <li>Loading CSV or XML files directly from the extracted ZIP contents.</li>
 *   <li>Converting pipe-delimited (or any delimiter) TXT files to proper CSV format.</li>
 *   <li>Cleaning up extracted files after the scenario.</li>
 * </ul>
 *
 * <h3>Configuration (config.yml)</h3>
 * <pre>
 * zip:
 *   extraction_dir: "test-output/extracted"   # where ZIP contents are extracted
 *   cleanup_after_scenario: true              # auto-delete extracted files after scenario
 *   recursive_unzip: true                     # extract nested ZIPs inside the archive
 * </pre>
 *
 * <h3>Complete example feature file</h3>
 * <pre>
 * Feature: Monthly Report Validation
 *
 *   Scenario: Validate downloaded monthly report ZIP
 *     # Step 1: Download the ZIP from the UI (uses existing download step)
 *     When we click on page ReportPage locator downloadButton
 *     # Step 2: Unzip the downloaded file using the path returned by the download step
 *     Given I unzip file "src/test/downloads/monthly_report.zip"
 *     # Step 3: Convert the pipe-delimited TXT inside the ZIP to CSV
 *     When I convert txt file "transactions.txt" to CSV using delimiter "|"
 *     # Step 4: Load the converted CSV for validation
 *     Given I load CSV from zip file "transactions.csv"
 *     Then CSV row count equals 5
 *     Then CSV row 1 column "Amount" equals "100.00"
 *     Then CSV row 1 column "Status" equals "PAID"
 *     # Step 5: Load an XML file from the same ZIP
 *     Given I load XML from zip file "summary.xml"
 *     Then XML node "totalAmount" equals "500.00"
 *     # Step 6: Clean up (optional — also auto-cleans if cleanup_after_scenario: true)
 *     Then I cleanup extracted zip files
 * </pre>
 */
public class ZipSteps {

    private static final Logger logger = LoggerFactory.getLogger(ZipSteps.class);

    /** Shared ZIP file handler instance — stateless, safe to reuse across steps. */
    private final ZipFileHandler zipHandler = new ZipFileHandler();

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Cucumber {@code @After} hook that cleans up extracted ZIP files at the end of
     * each scenario if {@code zip.cleanup_after_scenario: true} is set in config.yml.
     *
     * <p>This hook only runs if a ZIP was extracted during the scenario. It does not
     * affect scenarios that did not use any ZIP steps.</p>
     */
    @After
    public void cleanupZipAfterScenario() {
        if (ZipContext.hasExtractionResult() && ConfigurationProperties.isZipCleanupAfterScenario()) {
            String extractionDir = ZipContext.getExtractionDir();
            logger.info("FNB-ETAF ZIP | Auto-cleanup after scenario: deleting [{}]", extractionDir);
            zipHandler.cleanup(extractionDir);
        }
        ZipContext.clear();
    }

    // ─── Unzip Steps ──────────────────────────────────────────────────────────────

    /**
     * Unzips a ZIP file from the given path to the configured extraction directory.
     *
     * <p>The ZIP file path can be:</p>
     * <ul>
     *   <li>An absolute path (e.g., {@code /home/user/downloads/report.zip})</li>
     *   <li>A relative path from the project root (e.g., {@code src/test/downloads/report.zip})</li>
     *   <li>The path returned by the UI download step (e.g., stored in a variable)</li>
     * </ul>
     *
     * <p>After extraction, all files inside the ZIP are discoverable by name or extension
     * using the subsequent ZIP steps.</p>
     *
     * <p>Example: {@code Given I unzip file "src/test/downloads/monthly_report.zip"}</p>
     *
     * @param zipFilePath the path to the ZIP file to extract
     */
    @Given("I unzip file {string}")
    public void iUnzipFile(String zipFilePath) {
        try {
            String extractionDir = ConfigurationProperties.getZipExtractionDir();
            boolean recursive = ConfigurationProperties.isZipRecursiveUnzip();

            logger.info("FNB-ETAF ZIP | Unzipping [{}] → [{}] (recursive={})",
                zipFilePath, extractionDir, recursive);

            ZipFileHandler.ZipExtractionResult result = zipHandler.unzip(zipFilePath, extractionDir, recursive);
            ZipContext.setExtractionResult(result);

            logger.info("FNB-ETAF ZIP | Unzip complete: {}", result);
        } catch (Exception e) {
            throw new RuntimeException("FNB-ETAF ZIP | Failed to unzip file [" + zipFilePath + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Unzips a ZIP file from the given path to a specific target directory.
     *
     * <p>Use this step when you need to extract to a specific location rather than
     * the default configured extraction directory.</p>
     *
     * <p>Example: {@code Given I unzip file "report.zip" to directory "test-output/my-reports"}</p>
     *
     * @param zipFilePath the path to the ZIP file to extract
     * @param targetDir   the target directory to extract into
     */
    @Given("I unzip file {string} to directory {string}")
    public void iUnzipFileToDirectory(String zipFilePath, String targetDir) {
        try {
            boolean recursive = ConfigurationProperties.isZipRecursiveUnzip();

            logger.info("FNB-ETAF ZIP | Unzipping [{}] → [{}] (recursive={})",
                zipFilePath, targetDir, recursive);

            ZipFileHandler.ZipExtractionResult result = zipHandler.unzip(zipFilePath, targetDir, recursive);
            ZipContext.setExtractionResult(result);

            logger.info("FNB-ETAF ZIP | Unzip complete: {}", result);
        } catch (Exception e) {
            throw new RuntimeException("FNB-ETAF ZIP | Failed to unzip file [" + zipFilePath
                + "] to [" + targetDir + "]: " + e.getMessage(), e);
        }
    }

    // ─── TXT to CSV Conversion Steps ──────────────────────────────────────────────

    /**
     * Converts a TXT file from the extracted ZIP to a CSV file using the specified delimiter.
     *
     * <p>The TXT file must have been extracted by a previous "I unzip file" step.
     * The converted CSV file is saved in the same extraction directory and is immediately
     * available for use with the standard CSV step definitions.</p>
     *
     * <p>Common delimiters:</p>
     * <ul>
     *   <li>{@code |} — pipe (most common in FNB report exports)</li>
     *   <li>{@code \t} — tab</li>
     *   <li>{@code ;} — semicolon</li>
     * </ul>
     *
     * <p>Example: {@code When I convert txt file "transactions.txt" to CSV using delimiter "|"}</p>
     *
     * @param txtFileName the name of the TXT file within the extracted ZIP (e.g., "transactions.txt")
     * @param delimiter   the delimiter used in the TXT file (e.g., "|", "\t", ";")
     */
    @When("I convert txt file {string} to CSV using delimiter {string}")
    public void iConvertTxtFileToCsvUsingDelimiter(String txtFileName, String delimiter) {
        assertZipExtracted("convert TXT to CSV");

        Map<String, List<File>> filesByExtension = ZipContext.getFilesByExtension();
        File txtFile = zipHandler.findFileByName(filesByExtension, txtFileName);

        if (txtFile == null) {
            throw new RuntimeException("FNB-ETAF ZIP | TXT file [" + txtFileName
                + "] not found in extracted ZIP contents. Available files: "
                + summarizeFiles(filesByExtension));
        }

        try {
            // Handle escaped tab character from Gherkin
            String actualDelimiter = delimiter.replace("\\t", "\t");
            File csvFile = zipHandler.convertTxtToCsv(txtFile, actualDelimiter);

            // Register the new CSV file in the extraction result so it can be found by name
            ZipFileHandler.ZipExtractionResult current = ZipContext.getExtractionResult();
            current.filesByExtension.computeIfAbsent("csv", k -> new java.util.ArrayList<>()).add(csvFile);

            logger.info("FNB-ETAF ZIP | TXT [{}] converted to CSV [{}]",
                txtFileName, csvFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("FNB-ETAF ZIP | Failed to convert TXT [" + txtFileName
                + "] to CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Converts a TXT file from the extracted ZIP to CSV using the default pipe delimiter ({@code |}).
     *
     * <p>This is a convenience step for the most common FNB report format where
     * TXT files use pipe as the column separator.</p>
     *
     * <p>Example: {@code When I convert txt file "transactions.txt" to CSV}</p>
     *
     * @param txtFileName the name of the TXT file within the extracted ZIP
     */
    @When("I convert txt file {string} to CSV")
    public void iConvertTxtFileToCsv(String txtFileName) {
        iConvertTxtFileToCsvUsingDelimiter(txtFileName, "|");
    }

    // ─── CSV Loading from ZIP Steps ───────────────────────────────────────────────

    /**
     * Loads a specific CSV file from the extracted ZIP into the CSV context for validation.
     *
     * <p>After this step, all standard CSV step definitions (e.g., "CSV row 1 column X equals Y")
     * can be used to validate the loaded CSV data.</p>
     *
     * <p>Example: {@code Given I load CSV from zip file "transactions.csv"}</p>
     *
     * @param csvFileName the name of the CSV file within the extracted ZIP (e.g., "transactions.csv")
     */
    @Given("I load CSV from zip file {string}")
    public void iLoadCsvFromZipFile(String csvFileName) {
        assertZipExtracted("load CSV from zip");

        Map<String, List<File>> filesByExtension = ZipContext.getFilesByExtension();
        File csvFile = zipHandler.findFileByName(filesByExtension, csvFileName);

        if (csvFile == null) {
            throw new RuntimeException("FNB-ETAF ZIP | CSV file [" + csvFileName
                + "] not found in extracted ZIP. Available files: "
                + summarizeFiles(filesByExtension));
        }

        try {
            CsvFileHandler handler = new CsvFileHandler();
            handler.loadFromFile(csvFile.getAbsolutePath());
            CsvContext.set(handler);
            logger.info("FNB-ETAF ZIP | Loaded CSV [{}] from zip (rows: {})",
                csvFileName, handler.getRowCount());
        } catch (Exception e) {
            throw new RuntimeException("FNB-ETAF ZIP | Failed to load CSV [" + csvFileName
                + "] from zip: " + e.getMessage(), e);
        }
    }

    /**
     * Automatically finds and loads the first CSV file from the extracted ZIP.
     *
     * <p>Use this step when the ZIP contains exactly one CSV file and you do not
     * need to specify its name explicitly.</p>
     *
     * <p>Example: {@code Given I load the first CSV file from zip}</p>
     */
    @Given("I load the first CSV file from zip")
    public void iLoadFirstCsvFileFromZip() {
        assertZipExtracted("load first CSV from zip");

        Map<String, List<File>> filesByExtension = ZipContext.getFilesByExtension();
        File csvFile = zipHandler.findFirstFileByExtension(filesByExtension, "csv");

        if (csvFile == null) {
            throw new RuntimeException("FNB-ETAF ZIP | No CSV file found in extracted ZIP. Available files: "
                + summarizeFiles(filesByExtension));
        }

        try {
            CsvFileHandler handler = new CsvFileHandler();
            handler.loadFromFile(csvFile.getAbsolutePath());
            CsvContext.set(handler);
            logger.info("FNB-ETAF ZIP | Auto-loaded first CSV [{}] from zip (rows: {})",
                csvFile.getName(), handler.getRowCount());
        } catch (Exception e) {
            throw new RuntimeException("FNB-ETAF ZIP | Failed to load first CSV from zip: " + e.getMessage(), e);
        }
    }

    // ─── XML Loading from ZIP Steps ───────────────────────────────────────────────

    /**
     * Loads a specific XML file from the extracted ZIP into the XML context for validation.
     *
     * <p>After this step, all standard XML step definitions (e.g., "XML node X equals Y")
     * can be used to validate the loaded XML data.</p>
     *
     * <p>Example: {@code Given I load XML from zip file "summary.xml"}</p>
     *
     * @param xmlFileName the name of the XML file within the extracted ZIP (e.g., "summary.xml")
     */
    @Given("I load XML from zip file {string}")
    public void iLoadXmlFromZipFile(String xmlFileName) {
        assertZipExtracted("load XML from zip");

        Map<String, List<File>> filesByExtension = ZipContext.getFilesByExtension();
        File xmlFile = zipHandler.findFileByName(filesByExtension, xmlFileName);

        if (xmlFile == null) {
            throw new RuntimeException("FNB-ETAF ZIP | XML file [" + xmlFileName
                + "] not found in extracted ZIP. Available files: "
                + summarizeFiles(filesByExtension));
        }

        try {
            XmlFileHandler handler = new XmlFileHandler();
            handler.loadFromFile(xmlFile.getAbsolutePath());
            XmlContext.set(handler);
            logger.info("FNB-ETAF ZIP | Loaded XML [{}] from zip", xmlFileName);
        } catch (Exception e) {
            throw new RuntimeException("FNB-ETAF ZIP | Failed to load XML [" + xmlFileName
                + "] from zip: " + e.getMessage(), e);
        }
    }

    /**
     * Automatically finds and loads the first XML file from the extracted ZIP.
     *
     * <p>Use this step when the ZIP contains exactly one XML file and you do not
     * need to specify its name explicitly.</p>
     *
     * <p>Example: {@code Given I load the first XML file from zip}</p>
     */
    @Given("I load the first XML file from zip")
    public void iLoadFirstXmlFileFromZip() {
        assertZipExtracted("load first XML from zip");

        Map<String, List<File>> filesByExtension = ZipContext.getFilesByExtension();
        File xmlFile = zipHandler.findFirstFileByExtension(filesByExtension, "xml");

        if (xmlFile == null) {
            throw new RuntimeException("FNB-ETAF ZIP | No XML file found in extracted ZIP. Available files: "
                + summarizeFiles(filesByExtension));
        }

        try {
            XmlFileHandler handler = new XmlFileHandler();
            handler.loadFromFile(xmlFile.getAbsolutePath());
            XmlContext.set(handler);
            logger.info("FNB-ETAF ZIP | Auto-loaded first XML [{}] from zip", xmlFile.getName());
        } catch (Exception e) {
            throw new RuntimeException("FNB-ETAF ZIP | Failed to load first XML from zip: " + e.getMessage(), e);
        }
    }

    // ─── File Discovery Steps ─────────────────────────────────────────────────────

    /**
     * Verifies that a file with the given name exists within the extracted ZIP.
     *
     * <p>Use this step to assert that a specific file was present in the downloaded ZIP
     * before attempting to load it.</p>
     *
     * <p>Example: {@code Then zip contains file "transactions.csv"}</p>
     *
     * @param fileName the name of the file to check for (e.g., "transactions.csv")
     */
    @Then("zip contains file {string}")
    public void zipContainsFile(String fileName) {
        assertZipExtracted("verify file exists in zip");

        Map<String, List<File>> filesByExtension = ZipContext.getFilesByExtension();
        File found = zipHandler.findFileByName(filesByExtension, fileName);

        if (found == null) {
            throw new AssertionError("FNB-ETAF ZIP | Expected file [" + fileName
                + "] was not found in the extracted ZIP. Available files: "
                + summarizeFiles(filesByExtension));
        }

        logger.info("FNB-ETAF ZIP | Verified file [{}] exists in zip at [{}]",
            fileName, found.getAbsolutePath());
    }

    /**
     * Verifies that at least one file with the given extension exists within the extracted ZIP.
     *
     * <p>Example: {@code Then zip contains a "csv" file}</p>
     *
     * @param extension the file extension to check for (e.g., "csv", "xml", "txt")
     */
    @Then("zip contains a {string} file")
    public void zipContainsAFileWithExtension(String extension) {
        assertZipExtracted("verify file type exists in zip");

        Map<String, List<File>> filesByExtension = ZipContext.getFilesByExtension();
        List<File> files = filesByExtension.get(extension.toLowerCase().replace(".", ""));

        if (files == null || files.isEmpty()) {
            throw new AssertionError("FNB-ETAF ZIP | No [" + extension
                + "] file found in the extracted ZIP. Available files: "
                + summarizeFiles(filesByExtension));
        }

        logger.info("FNB-ETAF ZIP | Verified {} [{}] file(s) exist in zip.", files.size(), extension);
    }

    // ─── Cleanup Steps ────────────────────────────────────────────────────────────

    /**
     * Explicitly deletes all files extracted from the ZIP in the current scenario.
     *
     * <p>This step can be used to clean up extracted files at any point during the scenario,
     * regardless of the {@code zip.cleanup_after_scenario} config setting. It is also useful
     * when you want to clean up immediately after validation rather than waiting for the
     * scenario to end.</p>
     *
     * <p>Example: {@code Then I cleanup extracted zip files}</p>
     */
    @Then("I cleanup extracted zip files")
    public void iCleanupExtractedZipFiles() {
        if (ZipContext.hasExtractionResult()) {
            String extractionDir = ZipContext.getExtractionDir();
            zipHandler.cleanup(extractionDir);
            ZipContext.clear();
            logger.info("FNB-ETAF ZIP | Explicitly cleaned up extracted files from [{}]", extractionDir);
        } else {
            logger.debug("FNB-ETAF ZIP | No extracted ZIP files to clean up for this scenario.");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    /**
     * Asserts that a ZIP has been extracted in the current scenario.
     * Throws a descriptive {@link IllegalStateException} if not.
     *
     * @param action a description of the action being attempted (for the error message)
     */
    private void assertZipExtracted(String action) {
        if (!ZipContext.hasExtractionResult()) {
            throw new IllegalStateException(
                "FNB-ETAF ZIP | Cannot " + action + " — no ZIP file has been extracted in this scenario. "
                + "Use 'Given I unzip file \"path/to/file.zip\"' before this step.");
        }
    }

    /**
     * Builds a human-readable summary of the files discovered in the extracted ZIP.
     * Used in error messages to help testers identify what files are available.
     *
     * @param filesByExtension the map of discovered files
     * @return a summary string listing file names grouped by extension
     */
    private String summarizeFiles(Map<String, List<File>> filesByExtension) {
        if (filesByExtension == null || filesByExtension.isEmpty()) {
            return "(no files found)";
        }
        StringBuilder sb = new StringBuilder();
        filesByExtension.forEach((ext, files) -> {
            sb.append("[").append(ext).append(": ");
            files.forEach(f -> sb.append(f.getName()).append(", "));
            sb.append("] ");
        });
        return sb.toString().trim();
    }
}
