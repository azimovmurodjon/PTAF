package com.ptaf.zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * ZipContext — ThreadLocal context holder for ZIP extraction state within FNB-ETAF.
 *
 * <p>This class stores the result of the most recent ZIP extraction for the current
 * test scenario thread. It follows the same pattern as {@code CsvContext} and
 * {@code XmlContext} to ensure thread safety during parallel test execution.</p>
 *
 * <h3>Lifecycle</h3>
 * <p>The context is populated when a tester calls the "I unzip file" step and is
 * cleared at the end of each scenario. Step definitions use this context to access
 * extracted files without needing to pass file paths between steps.</p>
 *
 * <h3>Thread safety</h3>
 * <p>All state is stored in {@code ThreadLocal} variables, ensuring that concurrent
 * test scenarios running in parallel do not interfere with each other's ZIP state.</p>
 *
 * <h3>Usage example in feature files</h3>
 * <pre>
 * Given I unzip file "src/test/downloads/monthly_report.zip"
 * # Context now holds the extraction result for this scenario
 * When I convert txt file "transactions.txt" to CSV using delimiter "|"
 * Given I load CSV from zip file "transactions.csv"
 * Then CSV row 1 column "Amount" equals "100.00"
 * Then I cleanup extracted zip files
 * </pre>
 */
public class ZipContext {

    private static final Logger logger = LoggerFactory.getLogger(ZipContext.class);

    /**
     * ThreadLocal holding the current scenario's ZIP extraction result.
     * Contains the extraction directory path and the map of discovered files.
     */
    private static final ThreadLocal<ZipFileHandler.ZipExtractionResult> EXTRACTION_RESULT =
        new ThreadLocal<>();

    /** Private constructor — static utility class, not instantiable. */
    private ZipContext() {
        throw new IllegalStateException("ZipContext is a static utility class.");
    }

    // ─── State management ─────────────────────────────────────────────────────────

    /**
     * Stores the ZIP extraction result for the current scenario thread.
     *
     * <p>Called by the "I unzip file" step definition after a successful extraction.</p>
     *
     * @param result the {@link ZipFileHandler.ZipExtractionResult} from the extraction
     */
    public static void setExtractionResult(ZipFileHandler.ZipExtractionResult result) {
        EXTRACTION_RESULT.set(result);
        logger.debug("FNB-ETAF ZIP Context | Extraction result stored: {}", result);
    }

    /**
     * Returns the current scenario's ZIP extraction result.
     *
     * @return the {@link ZipFileHandler.ZipExtractionResult}, or {@code null} if no ZIP
     *         has been extracted in the current scenario
     */
    public static ZipFileHandler.ZipExtractionResult getExtractionResult() {
        return EXTRACTION_RESULT.get();
    }

    /**
     * Returns the extraction directory path for the current scenario.
     *
     * @return the extraction directory path string, or {@code null} if no ZIP has been extracted
     */
    public static String getExtractionDir() {
        ZipFileHandler.ZipExtractionResult result = EXTRACTION_RESULT.get();
        return result != null ? result.extractionDir : null;
    }

    /**
     * Returns the map of files discovered in the current scenario's extracted ZIP,
     * grouped by lowercase file extension (e.g., "csv", "xml", "txt").
     *
     * @return the files-by-extension map, or {@code null} if no ZIP has been extracted
     */
    public static Map<String, List<File>> getFilesByExtension() {
        ZipFileHandler.ZipExtractionResult result = EXTRACTION_RESULT.get();
        return result != null ? result.filesByExtension : null;
    }

    /**
     * Returns whether a ZIP has been extracted in the current scenario.
     *
     * @return {@code true} if a ZIP extraction result is present, {@code false} otherwise
     */
    public static boolean hasExtractionResult() {
        return EXTRACTION_RESULT.get() != null;
    }

    /**
     * Clears the ZIP extraction state for the current scenario thread.
     *
     * <p>Must be called at the end of each scenario (in the {@code @After} hook or
     * via the explicit cleanup step) to ensure test isolation between scenarios.</p>
     */
    public static void clear() {
        if (EXTRACTION_RESULT.get() != null) {
            logger.debug("FNB-ETAF ZIP Context | Clearing extraction result for current thread.");
        }
        EXTRACTION_RESULT.remove();
    }
}
