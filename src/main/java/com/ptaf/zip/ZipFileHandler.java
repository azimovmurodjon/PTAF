package com.ptaf.zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZipFileHandler — Utility class for ZIP file operations within FNB-ETAF.
 *
 * <p>This class provides the following capabilities:</p>
 * <ul>
 *   <li><strong>Unzip:</strong> Extracts a ZIP file to a configurable target directory.
 *       Supports nested ZIPs (ZIPs inside ZIPs) with recursive extraction.</li>
 *   <li><strong>File discovery:</strong> After extraction, discovers all files by type
 *       (CSV, XML, TXT, or any extension) so testers can reference them by name in steps.</li>
 *   <li><strong>TXT to CSV conversion:</strong> Converts a pipe-delimited (or any delimiter)
 *       TXT file to a proper comma-separated CSV file, ready for use with the existing
 *       CSV step definitions.</li>
 *   <li><strong>Cleanup:</strong> Deletes the extracted directory after the scenario
 *       completes, controlled by the {@code zip_cleanup_after_scenario} config flag.</li>
 * </ul>
 *
 * <h3>Usage in feature files (via ZipSteps)</h3>
 * <pre>
 * Given I unzip file "src/test/downloads/report.zip"
 * When I convert txt file "report.txt" to CSV using delimiter "|"
 * Given I load CSV from zip file "report.csv"
 * Then CSV row 1 column "Amount" equals "100.00"
 * </pre>
 *
 * <h3>Thread safety</h3>
 * <p>This class is stateless. All state is held in {@link ZipContext} which uses
 * {@code ThreadLocal} storage, making it safe for parallel test execution.</p>
 */
public class ZipFileHandler {

    private static final Logger logger = LoggerFactory.getLogger(ZipFileHandler.class);

    /** Default extraction directory if not configured in config.yml. */
    private static final String DEFAULT_EXTRACTION_DIR = "test-output/extracted";

    /**
     * Extracts a ZIP file to the specified target directory.
     *
     * <p>If the target directory does not exist, it is created automatically.
     * If {@code recursiveUnzip} is true, any ZIP files found inside the extracted
     * contents are also extracted recursively into subdirectories.</p>
     *
     * @param zipFilePath    the absolute or relative path to the ZIP file to extract
     * @param targetDir      the directory where extracted files will be placed
     * @param recursiveUnzip if true, nested ZIP files inside the archive are also extracted
     * @return a {@link ZipExtractionResult} containing the extraction directory path
     *         and a map of all discovered files grouped by extension
     * @throws IOException if the ZIP file cannot be read or the target directory cannot be created
     */
    public ZipExtractionResult unzip(String zipFilePath, String targetDir, boolean recursiveUnzip)
            throws IOException {

        File zipFile = resolveFile(zipFilePath);
        if (!zipFile.exists()) {
            throw new IOException("ZIP file not found: " + zipFile.getAbsolutePath());
        }
        if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
            throw new IOException("File is not a ZIP archive: " + zipFile.getAbsolutePath());
        }

        // Create a unique subdirectory named after the ZIP file (without extension)
        String zipStem = zipFile.getName().replaceAll("\\.zip$", "");
        Path extractionPath = Paths.get(targetDir, zipStem);
        Files.createDirectories(extractionPath);

        logger.info("FNB-ETAF ZIP | Extracting [{}] → [{}]", zipFile.getAbsolutePath(), extractionPath);

        extractZip(zipFile, extractionPath.toFile(), recursiveUnzip);

        // Discover all files in the extraction directory grouped by extension
        Map<String, List<File>> filesByExtension = discoverFiles(extractionPath.toFile());

        int totalFiles = filesByExtension.values().stream().mapToInt(List::size).sum();
        logger.info("FNB-ETAF ZIP | Extraction complete. {} file(s) discovered in [{}]",
            totalFiles, extractionPath);

        return new ZipExtractionResult(extractionPath.toString(), filesByExtension);
    }

    /**
     * Extracts all entries from a ZIP file into the target directory.
     * If {@code recursiveUnzip} is true, any nested ZIP files are also extracted.
     *
     * @param zipFile        the ZIP file to extract
     * @param targetDir      the directory to extract into
     * @param recursiveUnzip whether to recursively extract nested ZIP files
     * @throws IOException if extraction fails
     */
    private void extractZip(File zipFile, File targetDir, boolean recursiveUnzip) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());

                // Security check: prevent ZIP slip attacks (path traversal)
                if (!outFile.getCanonicalPath().startsWith(targetDir.getCanonicalPath() + File.separator)) {
                    logger.warn("FNB-ETAF ZIP | Skipping potentially unsafe ZIP entry: {}", entry.getName());
                    zis.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    // Ensure parent directories exist
                    outFile.getParentFile().mkdirs();

                    // Write the file content
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }

                    logger.debug("FNB-ETAF ZIP | Extracted: {}", outFile.getAbsolutePath());

                    // Recursively extract nested ZIPs if enabled
                    if (recursiveUnzip && outFile.getName().toLowerCase().endsWith(".zip")) {
                        logger.info("FNB-ETAF ZIP | Found nested ZIP [{}] — extracting recursively.", outFile.getName());
                        File nestedDir = new File(outFile.getParentFile(),
                            outFile.getName().replaceAll("\\.zip$", ""));
                        nestedDir.mkdirs();
                        extractZip(outFile, nestedDir, true);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Discovers all files within the given root directory, grouped by their file extension
     * (lowercase, without the leading dot). Files with no extension are grouped under
     * the key {@code "noext"}.
     *
     * <p>This map allows step definitions to quickly find files of a specific type
     * (e.g., all CSV files, all XML files) without knowing their exact names.</p>
     *
     * @param rootDir the root directory to search recursively
     * @return a map from lowercase extension (e.g., "csv", "xml", "txt") to list of matching files
     */
    public Map<String, List<File>> discoverFiles(File rootDir) {
        Map<String, List<File>> result = new HashMap<>();
        if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) {
            return result;
        }
        collectFiles(rootDir, result);
        return result;
    }

    /**
     * Recursively collects all files under the given directory into the result map.
     *
     * @param dir    the directory to search
     * @param result the map to populate
     */
    private void collectFiles(File dir, Map<String, List<File>> result) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collectFiles(child, result);
            } else {
                String ext = getExtension(child.getName());
                result.computeIfAbsent(ext, k -> new ArrayList<>()).add(child);
            }
        }
    }

    /**
     * Finds a specific file by name within the extracted files map.
     *
     * <p>The search is case-insensitive on the file name. If multiple files with the
     * same name exist in different subdirectories, the first match is returned.</p>
     *
     * @param filesByExtension the map of discovered files from {@link #discoverFiles}
     * @param fileName         the name of the file to find (e.g., "transactions.csv")
     * @return the matching {@link File}, or {@code null} if not found
     */
    public File findFileByName(Map<String, List<File>> filesByExtension, String fileName) {
        for (List<File> files : filesByExtension.values()) {
            for (File file : files) {
                if (file.getName().equalsIgnoreCase(fileName)) {
                    return file;
                }
            }
        }
        logger.warn("FNB-ETAF ZIP | File [{}] not found in extracted contents.", fileName);
        return null;
    }

    /**
     * Finds the first file with the given extension within the extracted files map.
     *
     * <p>Useful when a ZIP contains exactly one CSV or XML file and the tester does not
     * need to specify the exact file name.</p>
     *
     * @param filesByExtension the map of discovered files from {@link #discoverFiles}
     * @param extension        the file extension to search for (e.g., "csv", "xml", "txt")
     * @return the first matching {@link File}, or {@code null} if none found
     */
    public File findFirstFileByExtension(Map<String, List<File>> filesByExtension, String extension) {
        List<File> files = filesByExtension.get(extension.toLowerCase().replace(".", ""));
        if (files == null || files.isEmpty()) {
            logger.warn("FNB-ETAF ZIP | No [{}] file found in extracted contents.", extension);
            return null;
        }
        return files.get(0);
    }

    /**
     * Converts a delimited TXT file to a proper comma-separated CSV file.
     *
     * <p>The TXT file is expected to use a consistent delimiter on every line
     * (e.g., pipe {@code |}, tab {@code \t}, or semicolon {@code ;}).
     * The output CSV file is written to the same directory as the input TXT file
     * with the same base name and a {@code .csv} extension.</p>
     *
     * <p>Example: A TXT file containing:</p>
     * <pre>
     * Name|Amount|Status
     * John|100.00|PAID
     * </pre>
     * <p>is converted to a CSV file containing:</p>
     * <pre>
     * Name,Amount,Status
     * John,100.00,PAID
     * </pre>
     *
     * @param txtFile   the source TXT file to convert
     * @param delimiter the delimiter used in the TXT file (e.g., "|", "\t", ";")
     * @return the output CSV {@link File} that was created
     * @throws IOException if the TXT file cannot be read or the CSV file cannot be written
     */
    public File convertTxtToCsv(File txtFile, String delimiter) throws IOException {
        if (!txtFile.exists()) {
            throw new IOException("TXT file not found: " + txtFile.getAbsolutePath());
        }

        // Output CSV file in the same directory, same name, .csv extension
        String csvName = txtFile.getName().replaceAll("\\.[^.]+$", "") + ".csv";
        File csvFile = new File(txtFile.getParentFile(), csvName);

        logger.info("FNB-ETAF ZIP | Converting TXT [{}] → CSV [{}] using delimiter [{}]",
            txtFile.getName(), csvFile.getName(), delimiter);

        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(txtFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Split the line by the specified delimiter and re-join with comma
                // Handle values that may contain commas by wrapping in quotes
                String[] fields = line.split(escapeDelimiter(delimiter), -1);
                StringBuilder csvLine = new StringBuilder();
                for (int i = 0; i < fields.length; i++) {
                    if (i > 0) csvLine.append(",");
                    String field = fields[i].trim();
                    // Wrap in double quotes if the field contains a comma, quote, or newline
                    if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
                        field = "\"" + field.replace("\"", "\"\"") + "\"";
                    }
                    csvLine.append(field);
                }
                writer.write(csvLine.toString());
                writer.newLine();
                lineCount++;
            }
        }

        logger.info("FNB-ETAF ZIP | TXT to CSV conversion complete. {} line(s) written to [{}]",
            lineCount, csvFile.getAbsolutePath());
        return csvFile;
    }

    /**
     * Deletes the entire extraction directory and all its contents recursively.
     *
     * <p>Called at the end of a scenario when {@code zip_cleanup_after_scenario: true}
     * is set in config.yml, or when the tester explicitly calls the cleanup step.</p>
     *
     * @param extractionDir the path to the extraction directory to delete
     */
    public void cleanup(String extractionDir) {
        if (extractionDir == null || extractionDir.trim().isEmpty()) return;
        try {
            Path path = Paths.get(extractionDir);
            if (Files.exists(path)) {
                Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
                logger.info("FNB-ETAF ZIP | Cleaned up extraction directory: {}", extractionDir);
            }
        } catch (IOException e) {
            logger.warn("FNB-ETAF ZIP | Could not clean up extraction directory [{}]: {}",
                extractionDir, e.getMessage());
        }
    }

    /**
     * Returns the default extraction directory path.
     * Used when no extraction directory is specified in config.yml.
     *
     * @return the default extraction directory path string
     */
    public static String getDefaultExtractionDir() {
        return DEFAULT_EXTRACTION_DIR;
    }

    /**
     * Resolves a file path that may be absolute or relative to the project working directory.
     *
     * @param filePath the file path string to resolve
     * @return the resolved {@link File}
     */
    private File resolveFile(String filePath) {
        File file = new File(filePath);
        if (file.isAbsolute()) return file;
        // Resolve relative paths from the current working directory (project root)
        return new File(System.getProperty("user.dir"), filePath);
    }

    /**
     * Extracts the lowercase file extension from a file name, without the leading dot.
     * Returns "noext" if the file has no extension.
     *
     * @param fileName the file name to extract the extension from
     * @return the lowercase extension (e.g., "csv", "xml", "txt") or "noext"
     */
    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) return "noext";
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Escapes a delimiter string for use as a Java regex pattern in {@link String#split}.
     * Special regex characters (e.g., {@code |}, {@code .}, {@code *}) are escaped.
     *
     * @param delimiter the raw delimiter string
     * @return the regex-escaped delimiter
     */
    private String escapeDelimiter(String delimiter) {
        // Escape special regex metacharacters
        return delimiter.replace("\\", "\\\\")
                        .replace("|", "\\|")
                        .replace(".", "\\.")
                        .replace("*", "\\*")
                        .replace("+", "\\+")
                        .replace("?", "\\?")
                        .replace("(", "\\(")
                        .replace(")", "\\)")
                        .replace("[", "\\[")
                        .replace("]", "\\]")
                        .replace("{", "\\{")
                        .replace("}", "\\}")
                        .replace("^", "\\^")
                        .replace("$", "\\$");
    }

    // ─── Inner class ──────────────────────────────────────────────────────────────

    /**
     * Immutable result object returned by {@link ZipFileHandler#unzip}.
     *
     * <p>Contains the path to the extraction directory and a map of all discovered
     * files grouped by their file extension. Testers and step definitions use this
     * object to locate specific files within the extracted ZIP contents.</p>
     */
    public static final class ZipExtractionResult {

        /** The absolute path to the directory where the ZIP was extracted. */
        public final String extractionDir;

        /**
         * Map of discovered files grouped by lowercase extension (e.g., "csv", "xml", "txt").
         * Files with no extension are grouped under the key "noext".
         */
        public final Map<String, List<File>> filesByExtension;

        /**
         * Constructs a new ZipExtractionResult.
         *
         * @param extractionDir    the extraction directory path
         * @param filesByExtension the discovered files grouped by extension
         */
        public ZipExtractionResult(String extractionDir, Map<String, List<File>> filesByExtension) {
            this.extractionDir = extractionDir;
            this.filesByExtension = filesByExtension;
        }

        /**
         * Returns a human-readable summary of the extracted files for logging.
         *
         * @return a summary string listing file counts per extension
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ZipExtractionResult{dir=")
                .append(extractionDir).append(", files={");
            filesByExtension.forEach((ext, files) ->
                sb.append(ext).append(":").append(files.size()).append(" "));
            return sb.append("}}").toString();
        }
    }
}
