package com.ptaf.csv;

/**
 * Thread-local context holder for the CSV automation module.
 *
 * <p>This class stores the current {@link CsvFileHandler} instance in a {@link ThreadLocal}
 * so that each test thread (scenario) has its own isolated CSV data. This follows the same
 * pattern used by {@link com.ptaf.xml.XmlContext} and the PTAF mobile module.</p>
 *
 * <p>The context is automatically cleared after each scenario by {@link CsvCommonMethods#clear()},
 * which is called from the {@code @After} hook in {@link com.ptaf.stepdefinitions.CsvSteps}.</p>
 *
 * <p>This class is a pure static utility and cannot be instantiated.</p>
 */
public final class CsvContext {

    private static final ThreadLocal<CsvFileHandler> CONTEXT = new ThreadLocal<>();

    private CsvContext() {
        throw new IllegalStateException("CsvContext is a static utility class and cannot be instantiated.");
    }

    /**
     * Store a {@link CsvFileHandler} instance for the current thread.
     *
     * @param handler the CsvFileHandler to store; must not be null
     */
    public static void set(CsvFileHandler handler) {
        if (handler == null) throw new IllegalArgumentException("CsvFileHandler cannot be null.");
        CONTEXT.set(handler);
    }

    /**
     * Retrieve the {@link CsvFileHandler} for the current thread.
     *
     * @return the current CsvFileHandler, or {@code null} if none is set
     */
    public static CsvFileHandler get() {
        return CONTEXT.get();
    }

    /**
     * Remove the {@link CsvFileHandler} for the current thread and release the parsed data.
     * Call this after each scenario to ensure test isolation.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Check whether CSV data is currently loaded for this thread.
     *
     * @return {@code true} if a handler has been set, {@code false} otherwise
     */
    public static boolean isLoaded() {
        return CONTEXT.get() != null;
    }
}
