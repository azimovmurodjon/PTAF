package com.ptaf.xml;

/**
 * Thread-local context holder for the XML automation module.
 *
 * <p>This class stores the current {@link XmlFileHandler} instance in a {@link ThreadLocal}
 * so that each test thread (scenario) has its own isolated XML document. This follows the
 * same pattern used by the PTAF UI module ({@code Hooks.java}) and mobile module
 * ({@code MobileDriverManager.java}).</p>
 *
 * <p>The context is automatically cleared after each scenario by {@link XmlCommonMethods#clear()},
 * which should be called from a Cucumber {@code @After} hook or at the end of each scenario.
 * If not explicitly cleared, the document persists for the duration of the JVM thread.</p>
 *
 * <p>This class is a pure static utility and cannot be instantiated.</p>
 *
 * <h3>Usage pattern:</h3>
 * <pre>
 * // Load an XML document (done via XmlCommonMethods or XmlSteps)
 * XmlContext.set(new XmlFileHandler());
 * XmlContext.get().loadFromFile("path/to/file.xml");
 *
 * // Query the document
 * String value = XmlContext.get().getValue("status");
 *
 * // Clear after the scenario
 * XmlContext.clear();
 * </pre>
 */
public final class XmlContext {

    /** ThreadLocal storage for the per-scenario XmlFileHandler instance. */
    private static final ThreadLocal<XmlFileHandler> CONTEXT = new ThreadLocal<>();

    /** Private constructor — this class is a static utility and must not be instantiated. */
    private XmlContext() {
        throw new IllegalStateException("XmlContext is a static utility class and cannot be instantiated.");
    }

    /**
     * Store an {@link XmlFileHandler} instance for the current thread.
     *
     * <p>This replaces any previously stored handler for this thread. Call this at the
     * start of each scenario or whenever a new XML document needs to be loaded.</p>
     *
     * @param handler the XmlFileHandler to store; must not be null
     * @throws IllegalArgumentException if handler is null
     */
    public static void set(XmlFileHandler handler) {
        if (handler == null) throw new IllegalArgumentException("XmlFileHandler cannot be null.");
        CONTEXT.set(handler);
    }

    /**
     * Retrieve the {@link XmlFileHandler} for the current thread.
     *
     * <p>Returns {@code null} if no handler has been set for this thread. Callers should
     * check for null or rely on {@link XmlFileHandler#getValue(String)} throwing a clear
     * error when no document is loaded.</p>
     *
     * @return the current XmlFileHandler, or {@code null} if none is set
     */
    public static XmlFileHandler get() {
        return CONTEXT.get();
    }

    /**
     * Remove the {@link XmlFileHandler} for the current thread and release the parsed document.
     *
     * <p>This should be called after each scenario to prevent memory leaks and ensure
     * test isolation. If not called, the document persists until the thread is reused
     * for a new scenario (which may cause unexpected state bleed-through).</p>
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Check whether an XML document is currently loaded for this thread.
     *
     * @return {@code true} if a handler has been set and is available, {@code false} otherwise
     */
    public static boolean isLoaded() {
        return CONTEXT.get() != null;
    }
}
