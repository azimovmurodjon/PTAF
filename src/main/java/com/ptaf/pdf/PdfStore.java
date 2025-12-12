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
 */
public final class PdfStore {

    private static final ThreadLocal<ConcurrentHashMap<String, Object>> TL =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    private PdfStore() {}

    /** Save the absolute path of the current PDF under test (thread-local). */
    public static void setLastPdfPath(String path) {
        TL.get().put("lastPdfPath", path);
    }

    /** Retrieve the current PDF path for this thread (or null if not set). */
    public static String getLastPdfPath() {
        Object v = TL.get().get("lastPdfPath");
        return v == null ? null : String.valueOf(v);
    }

    /** Clear the thread-local storage (usually not required unless reusing threads). */
    public static void clear() {
        TL.get().clear();
    }

    /**
     * Pick the newest file in a directory and set it as the current PDF.
     *
     * @param dir      directory to scan
     * @param endsWith optional suffix filter (e.g., ".pdf"); pass null to allow any file
     * @return absolute path of the chosen file
     */
    public static String setLastFromDirectory(String dir, String endsWith) {
        Path folder = Paths.get(dir);
        if (!Files.isDirectory(folder)) {
            throw new IllegalArgumentException("Not a directory: " + dir);
        }

        try (Stream<Path> s = Files.list(folder)) {
            Path newest = s
                    .filter(Files::isRegularFile)
                    .filter(p -> endsWith == null || p.getFileName().toString().endsWith(endsWith))
                    .max(Comparator.comparingLong(PdfStore::lastModified))
                    .orElseThrow(() -> new IllegalStateException("No files found in: " + dir));
            String abs = newest.toAbsolutePath().toString();
            setLastPdfPath(abs);
            return abs;
        } catch (IOException e) {
            throw new RuntimeException("Failed listing directory: " + dir, e);
        }
    }

    /** Guard helper: fail early if the current PDF path is missing or the file does not exist. */
    public static void ensureExists() {
        String p = getLastPdfPath();
        if (p == null) throw new IllegalStateException("No lastPdfPath set. Download or set from directory first.");
        if (!Files.exists(Paths.get(p))) throw new IllegalStateException("PDF not found: " + p);
    }

    /** Efficient last-modified fetch via NIO attributes. */
    private static long lastModified(Path p) {
        try {
            BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
            return a.lastModifiedTime().toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}