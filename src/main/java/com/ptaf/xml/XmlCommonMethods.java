package com.ptaf.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * High-level XML assertion and extraction methods for use by Cucumber step definitions.
 *
 * <p>This class sits between the raw {@link XmlFileHandler} (which handles parsing and XPath
 * evaluation) and the {@link com.ptaf.stepdefinitions.XmlSteps} class (which maps Gherkin
 * sentences to Java methods). It provides:</p>
 * <ul>
 *   <li>Loading XML from a file path or a raw string (e.g., extracted from a UI element).</li>
 *   <li>Asserting that a node or XPath expression equals or contains an expected value.</li>
 *   <li>Asserting node existence and absence.</li>
 *   <li>Asserting node count.</li>
 *   <li>Extracting values into a named variable store for use in later steps.</li>
 *   <li>Clearing the XML context after a scenario.</li>
 * </ul>
 *
 * <p>All assertion failures throw an {@link AssertionError} with a clear, descriptive message
 * that includes the query, the expected value, and the actual value found — making failures
 * easy to diagnose without reading Java stack traces.</p>
 *
 * <p>This class is not thread-safe by itself but is used via {@link XmlContext} which provides
 * per-thread isolation.</p>
 */
public class XmlCommonMethods {

    private static final Logger logger = LoggerFactory.getLogger(XmlCommonMethods.class);

    /**
     * In-scenario variable store. Values extracted with "store as" steps are kept here
     * and can be referenced in later steps within the same scenario.
     * Key: variable name (e.g., "ORDER_ID"). Value: extracted string value.
     */
    private final Map<String, String> variableStore = new HashMap<>();

    // ─── Loading ─────────────────────────────────────────────────────────────────

    /**
     * Load and parse an XML file from the filesystem into the current scenario's XML context.
     *
     * <p>The path is resolved relative to the project root. Both absolute and relative paths
     * are supported. This must be called before any assertion or extraction steps.</p>
     *
     * @param filePath path to the XML file (e.g., "src/test/resources/data/response.xml")
     */
    public void loadFromFile(String filePath) {
        XmlFileHandler handler = new XmlFileHandler();
        handler.loadFromFile(filePath);
        XmlContext.set(handler);
        logger.info("PTAF XML | Loaded XML file into context: {}", filePath);
    }

    /**
     * Load and parse XML content from a raw string into the current scenario's XML context.
     *
     * <p>This is used when XML has been extracted from a UI element (e.g., a textarea showing
     * an API response). The string must be well-formed XML.</p>
     *
     * @param xmlContent raw XML string to parse
     */
    public void loadFromString(String xmlContent) {
        XmlFileHandler handler = new XmlFileHandler();
        handler.loadFromString(xmlContent);
        XmlContext.set(handler);
        logger.info("PTAF XML | Loaded XML from string content into context.");
    }

    // ─── Assertions ──────────────────────────────────────────────────────────────

    /**
     * Assert that the value of an XML node or XPath expression equals the expected value exactly.
     *
     * <p>The {@code query} parameter supports both XPath expressions (starting with {@code /} or
     * {@code //}) and simple node names. See {@link XmlFileHandler#getValue(String)} for details.</p>
     *
     * @param query    XPath expression or simple node name (e.g., "status" or "//order/status")
     * @param expected the exact expected value
     * @throws AssertionError if the actual value does not equal the expected value
     */
    public void assertValueEquals(String query, String expected) {
        String actual = handler().getValue(query);
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "PTAF XML | Assertion failed — value does not match.\n" +
                "  Query    : " + query + "\n" +
                "  Expected : [" + expected + "]\n" +
                "  Actual   : [" + actual + "]"
            );
        }
        logger.info("PTAF XML | assertValueEquals PASSED — query=[{}] value=[{}]", query, actual);
    }

    /**
     * Assert that the value of an XML node or XPath expression contains the expected substring.
     *
     * @param query    XPath expression or simple node name
     * @param expected the substring that must be present in the actual value
     * @throws AssertionError if the actual value does not contain the expected substring
     */
    public void assertValueContains(String query, String expected) {
        String actual = handler().getValue(query);
        if (!actual.contains(expected)) {
            throw new AssertionError(
                "PTAF XML | Assertion failed — value does not contain expected substring.\n" +
                "  Query    : " + query + "\n" +
                "  Expected to contain : [" + expected + "]\n" +
                "  Actual              : [" + actual + "]"
            );
        }
        logger.info("PTAF XML | assertValueContains PASSED — query=[{}] contains=[{}]", query, expected);
    }

    /**
     * Assert that the value of an XML node or XPath expression does NOT equal the given value.
     *
     * @param query    XPath expression or simple node name
     * @param expected the value that must NOT be present
     * @throws AssertionError if the actual value equals the expected value
     */
    public void assertValueNotEquals(String query, String expected) {
        String actual = handler().getValue(query);
        if (expected.equals(actual)) {
            throw new AssertionError(
                "PTAF XML | Assertion failed — value should not equal [" + expected + "] but it does.\n" +
                "  Query  : " + query
            );
        }
        logger.info("PTAF XML | assertValueNotEquals PASSED — query=[{}] value=[{}] (not equal to [{}])", query, actual, expected);
    }

    /**
     * Assert that at least one node matching the query exists in the document.
     *
     * @param query XPath expression or simple node name
     * @throws AssertionError if no matching node is found
     */
    public void assertNodeExists(String query) {
        if (!handler().nodeExists(query)) {
            throw new AssertionError(
                "PTAF XML | Assertion failed — expected node/XPath to exist but it was not found.\n" +
                "  Query : " + query
            );
        }
        logger.info("PTAF XML | assertNodeExists PASSED — query=[{}]", query);
    }

    /**
     * Assert that no node matching the query exists in the document.
     *
     * @param query XPath expression or simple node name
     * @throws AssertionError if a matching node is found
     */
    public void assertNodeNotExists(String query) {
        if (handler().nodeExists(query)) {
            throw new AssertionError(
                "PTAF XML | Assertion failed — expected node/XPath to NOT exist but it was found.\n" +
                "  Query : " + query
            );
        }
        logger.info("PTAF XML | assertNodeNotExists PASSED — query=[{}]", query);
    }

    /**
     * Assert that the number of nodes matching an XPath expression equals the expected count.
     *
     * @param xpathExpression full XPath expression (must start with {@code /} or {@code //})
     * @param expectedCount   the exact number of nodes expected
     * @throws AssertionError if the actual count does not match the expected count
     */
    public void assertNodeCount(String xpathExpression, int expectedCount) {
        int actual = handler().countNodes(xpathExpression);
        if (actual != expectedCount) {
            throw new AssertionError(
                "PTAF XML | Assertion failed — node count does not match.\n" +
                "  XPath    : " + xpathExpression + "\n" +
                "  Expected : " + expectedCount + "\n" +
                "  Actual   : " + actual
            );
        }
        logger.info("PTAF XML | assertNodeCount PASSED — xpath=[{}] count=[{}]", xpathExpression, actual);
    }

    /**
     * Assert that the value of an attribute on the first matching node equals the expected value.
     *
     * @param query         XPath expression or simple node name to locate the element
     * @param attributeName name of the attribute to check
     * @param expected      the expected attribute value
     * @throws AssertionError if the actual attribute value does not equal the expected value
     */
    public void assertAttributeEquals(String query, String attributeName, String expected) {
        String actual = handler().getAttributeValue(query, attributeName);
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "PTAF XML | Assertion failed — attribute value does not match.\n" +
                "  Query     : " + query + "\n" +
                "  Attribute : " + attributeName + "\n" +
                "  Expected  : [" + expected + "]\n" +
                "  Actual    : [" + actual + "]"
            );
        }
        logger.info("PTAF XML | assertAttributeEquals PASSED — query=[{}] attr=[{}] value=[{}]", query, attributeName, actual);
    }

    // ─── Extraction ──────────────────────────────────────────────────────────────

    /**
     * Extract the value of an XML node or XPath expression and store it in the variable store
     * under the given name for use in later steps within the same scenario.
     *
     * <p>Example usage in a feature file:</p>
     * <pre>When I extract XML node "orderId" and store as "ORDER_ID"
     * Then XML node "confirmationId" equals stored value "ORDER_ID"</pre>
     *
     * @param query        XPath expression or simple node name
     * @param variableName name to store the extracted value under
     */
    public void extractAndStore(String query, String variableName) {
        String value = handler().getValue(query);
        variableStore.put(variableName, value);
        logger.info("PTAF XML | Extracted value [{}] from query [{}] and stored as [{}]", value, query, variableName);
    }

    /**
     * Retrieve a previously stored variable value by name.
     *
     * @param variableName name of the variable to retrieve
     * @return the stored value
     * @throws RuntimeException if no variable with the given name has been stored
     */
    public String getStoredValue(String variableName) {
        if (!variableStore.containsKey(variableName)) {
            throw new RuntimeException(
                "PTAF XML | Variable [" + variableName + "] has not been stored. " +
                "Use a step like: When I extract XML node \"...\" and store as \"" + variableName + "\" first."
            );
        }
        return variableStore.get(variableName);
    }

    /**
     * Get the raw XML content of the currently loaded document as a string.
     * Useful for logging or debugging in test output.
     *
     * @return formatted XML string
     */
    public String getRawContent() {
        return handler().getRawContent();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Clear the XML context and variable store for the current scenario.
     *
     * <p>This should be called in a Cucumber {@code @After} hook to ensure test isolation.
     * Failing to call this may cause XML state from one scenario to bleed into the next.</p>
     */
    public void clear() {
        XmlContext.clear();
        variableStore.clear();
        logger.debug("PTAF XML | Context and variable store cleared.");
    }

    // ─── Internal ─────────────────────────────────────────────────────────────────

    /**
     * Get the current XmlFileHandler from the context, throwing a clear error if none is loaded.
     */
    private XmlFileHandler handler() {
        XmlFileHandler h = XmlContext.get();
        if (h == null) {
            throw new IllegalStateException(
                "PTAF XML | No XML document is loaded. " +
                "Add a step like: Given I load XML file \"path/to/file.xml\" " +
                "or: Given I load XML from UI element on page \"X\" locator \"Y\" " +
                "before any XML assertion or extraction steps."
            );
        }
        return h;
    }
}
