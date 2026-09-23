package com.ptaf.pdf;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * PdfStore
 *
 * Purpose:
 *  - Thread-scoped storage for the "current" PDF path under test.
 *  - Provides utility to set that path from the newest file in a directory
 *    using pure Java NIO (no external libs).
 *
 * Why:
 *  - Step definitions often need to validate the same file across many steps.
 *  - A ThreadLocal stash avoids passing paths around and supports parallel execution.
 *
 * Typical usage:
 *  - After a download: PdfStore.setLastPdfPath(downloadedPath);
 *  - Or: PdfStore.setLastFromDirectory("target/downloads", ".pdf");
 *  - Then validate with PdfValidator, reading PdfStore.getLastPdfPath().
 *
 * Notes for testers:
 *  - This class does not perform any PDF parsing. It only tracks the file path.
 *  - When running tests in parallel, each test thread has its own independent store.
 *  - Remember to call clear() if you reuse threads and want to avoid state leakage
 *    between tests (most test frameworks spawn fresh threads or use a fresh context,
 *    but long-lived thread pools may require explicit clearing).
 */
public final class PdfStore {

    /**
     * Thread-local storage for arbitrary key/value pairs scoped to the current thread.
     *
     * We use a ConcurrentHashMap as the backing container to allow fast concurrent
     * access patterns if tests store multiple values (even though in practice
     * we only use the "lastPdfPath" key here). ThreadLocal ensures each thread
     * gets its own map instance.
     */
    private static final ThreadLocal<ConcurrentHashMap<String, Object>> TL =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * Private constructor: utility class only. Prevent instantiation.
     */
    private PdfStore() {}

    /**
     * Save the absolute path of the current PDF under test (thread-local).
     *
     * This stores the provided path string under the fixed key "lastPdfPath" in
     * the thread-local map. Callers should pass an absolute path when possible to
     * avoid confusion about the working directory.
     *
     * @param path absolute or relative path to the PDF file to remember for this thread
     */
    public static void setLastPdfPath(String path) {
        // Store the path under the well-known key so other utilities can retrieve it.
        TL.get().put("lastPdfPath", path);
    }

    /**
     * Retrieve the current PDF path for this thread (or null if not set).
     *
     * Tests and validators can call this method to obtain the path previously set
     * by setLastPdfPath(...) or setLastFromDirectory(...).
     *
     * @return the stored path as a String, or null if no path has been set for the current thread
     */
    public static String getLastPdfPath() {
        // Read the object and defensively convert to String (handles nulls).
        Object v = TL.get().get("lastPdfPath");
        return v == null ? null : String.valueOf(v);
    }

    /**
     * Clear the thread-local storage (usually not required unless reusing threads).
     *
     * This removes all keys stored in the thread-local map for the current thread.
     * Useful when tests execute on long-lived threads (for example, custom thread pools)
     * and you want to make sure previous state does not leak into subsequent tests.
     */
    public static void clear() {
        TL.get().clear();
    }

    /**
     * Pick the newest file in a directory and set it as the current PDF.
     *
     * The method:
     *  - Validates that the provided directory exists and is a directory.
     *  - Lists the directory using Files.list(...) (stream based; the stream is closed
     *    automatically by the try-with-resources block).
     *  - Filters entries to regular files (ignores sub-directories and other non-regular entries).
     *  - Optionally filters files by name suffix if endsWith is non-null (e.g., ".pdf").
     *  - Chooses the file with the most recent lastModified time via a comparator.
     *  - Stores the resulting file's absolute path in the thread-local store and returns it.
     *
     * Exceptions:
     *  - IllegalArgumentException if the provided path is not a directory.
     *  - IllegalStateException if no matching files are found in the directory.
     *  - RuntimeException wrapping IOException if listing the directory fails.
     *
     * Note for testers:
     *  - If multiple files share the same lastModified timestamp, the comparator will
     *    pick one of them arbitrarily (as determined by stream ordering).
     *
     * @param dir      directory to scan
     * @param endsWith optional suffix filter (e.g., ".pdf"); pass null to allow any file
     * @return absolute path of the chosen file (also stored in the thread-local map)
     * @throws IllegalArgumentException if dir is not a directory
     * @throws IllegalStateException    if no matching files are found
     * @throws RuntimeException         if an I/O error occurs when listing the directory
     */
    public static String setLastFromDirectory(String dir, String endsWith) {
        Path folder = Paths.get(dir);
        // Validate upfront that the path points to a directory.
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Not a directory: " + dir);
        }

        // Use try-with-resources to ensure the stream returned by Files.list is closed.
        try (Stream<Path> s = Files.list(folder)) {
            Path newest = s
                    // Keep only regular files (excludes directories, symbolic links to dirs, etc.).
                    .filter(Files::isRegularFile)
                    // If endsWith is provided, filter by file name suffix (case-sensitive).
                    .filter(p -> endsWith == null || p.getFileName().toString().endsWith(endsWith))
                    // Choose the file with the largest lastModified timestamp.
                    .max(Comparator.comparingLong(PdfStore::lastModified))
                    // Fail fast if no files matched the criteria.
                    .orElseThrow(() -> new IllegalStateException("No files found in: " + dir));
            // Convert to absolute path string for clarity and store it for later retrieval.
            String abs = newest.toAbsolutePath().toString();
            setLastPdfPath(abs);
            return abs;
        } catch (IOException e) {
            // Wrap IOExceptions to avoid forcing callers to handle checked exceptions.
            throw new RuntimeException("Failed listing directory: " + dir, e);
        }
    }

    /**
     * Guard helper: fail early if the current PDF path is missing or the file does not exist.
     *
     * Use this in tests before attempting to open or validate the stored PDF to provide
     * a clear and early diagnostic message rather than a lower-level I/O exception.
     *
     * Throws IllegalStateException if:
     *  - No lastPdfPath has been set for the current thread.
     *  - The file referenced by lastPdfPath does not exist on the filesystem.
     */
    public static void ensureExists() {
        String p = getLastPdfPath();
        if (p == null) throw new IllegalStateException("No lastPdfPath set. Download or set from directory first.");
        if (!Files.exists(Paths.get(p))) throw new IllegalStateException("PDF not found: " + p);
    }

    /**
     * Efficient last-modified fetch via NIO attributes.
     *
     * This helper reads BasicFileAttributes and returns the lastModifiedTime in milliseconds.
     * If an I/O error occurs while reading attributes the method returns 0L (treated as very old).
     *
     * Returning 0L on error is intentional: files that cannot have attributes read will not
     * be chosen as "newest" when compared against valid files. Callers should be aware that
     * silently returning 0L can hide attribute-read failures; this design favors robustness
     * in the presence of transient filesystem issues.
     *
     * @param p path to inspect
     * @return last modified time in milliseconds, or 0L if attributes cannot be read
     */
    private static long lastModified(Path p) {
        try {
            BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
            return a.lastModifiedTime().toMillis();
        } catch (IOException e) {
            // On failure, return epoch-ish 0 to deprioritize this path in comparisons.
            return 0L;
        }
    }
}
