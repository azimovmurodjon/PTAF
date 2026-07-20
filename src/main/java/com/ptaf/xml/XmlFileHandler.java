package com.ptaf.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Core XML parsing and querying engine for the PTAF XML automation module.
 *
 * <p>This class is responsible for:</p>
 * <ul>
 *   <li>Loading XML content from a filesystem path or a raw string (e.g., extracted from a UI element).</li>
 *   <li>Parsing the XML into a W3C {@link Document} object using the standard Java XML API.</li>
 *   <li>Querying the parsed document using either XPath expressions or simple node name lookups.</li>
 *   <li>Counting nodes that match an XPath expression.</li>
 *   <li>Checking whether a node or XPath result exists in the document.</li>
 * </ul>
 *
 * <p><strong>No external libraries are required.</strong> This class uses only the standard Java SE
 * XML APIs ({@code javax.xml.parsers}, {@code javax.xml.xpath}, {@code org.w3c.dom}) which are
 * bundled with every JDK 11+ installation.</p>
 *
 * <p>This class is not meant to be instantiated directly by test code. Use
 * {@link XmlCommonMethods} or {@link com.ptaf.stepdefinitions.XmlSteps} instead.</p>
 *
 * <h3>Querying strategy:</h3>
 * <ul>
 *   <li>If the query string starts with {@code /} or {@code //}, it is treated as a full XPath expression.</li>
 *   <li>Otherwise, it is treated as a simple node name and the framework searches the entire document
 *       for the first element with that name.</li>
 * </ul>
 *
 * <h3>Thread safety:</h3>
 * <p>Each instance holds its own parsed {@link Document}. Instances are created per-scenario via
 * {@link XmlContext} and are not shared across threads.</p>
 */
public class XmlFileHandler {

    private static final Logger logger = LoggerFactory.getLogger(XmlFileHandler.class);

    /** The parsed XML document. Set by {@link #loadFromFile(String)} or {@link #loadFromString(String)}. */
    private Document document;

    /** The XPath engine used for all XPath evaluations. Created once per instance. */
    private final XPath xpath;

    /**
     * Creates a new XmlFileHandler with a fresh XPath engine.
     * No XML is loaded at construction time — call {@link #loadFromFile(String)} or
     * {@link #loadFromString(String)} to load content before querying.
     */
    public XmlFileHandler() {
        this.xpath = XPathFactory.newInstance().newXPath();
    }

    // ─── Loading ─────────────────────────────────────────────────────────────────

    /**
     * Load and parse an XML file from the filesystem.
     *
     * <p>The path is resolved relative to the project root (the directory from which Maven runs).
     * Both absolute paths and relative paths are supported. The file must be a well-formed XML
     * document; malformed XML will throw a {@link RuntimeException} with a descriptive message.</p>
     *
     * <p>Example usage in a feature file:</p>
     * <pre>Given I load XML file "src/test/resources/data/response.xml"</pre>
     *
     * @param filePath path to the XML file (absolute or relative to project root)
     * @throws RuntimeException if the file does not exist, cannot be read, or is not valid XML
     */
    public void loadFromFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("XML file path cannot be null or blank.");
        }
        Path path = Path.of(filePath.trim());
        if (!Files.exists(path)) {
            throw new RuntimeException(
                "PTAF XML | File not found: [" + filePath + "]. " +
                "Check the path is correct and the file exists. " +
                "Paths are relative to the project root (where pom.xml is located)."
            );
        }
        try {
            logger.info("PTAF XML | Loading XML file: {}", filePath);
            document = buildDocument(new File(filePath));
            logger.info("PTAF XML | XML file loaded and parsed successfully: {}", filePath);
        } catch (Exception e) {
            throw new RuntimeException(
                "PTAF XML | Failed to parse XML file [" + filePath + "]: " + e.getMessage(), e
            );
        }
    }

    /**
     * Load and parse XML content from a raw string.
     *
     * <p>This is used when XML content has been extracted from a UI element (e.g., a textarea
     * showing an API response, a code block, or a pre-formatted text area). The string must be
     * well-formed XML.</p>
     *
     * <p>Example usage in a feature file:</p>
     * <pre>Given I load XML from UI element on page "ResponsePage" locator "xmlTextArea"</pre>
     *
     * @param xmlContent raw XML string to parse
     * @throws RuntimeException if the string is blank or is not valid XML
     */
    public void loadFromString(String xmlContent) {
        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("XML content string cannot be null or blank.");
        }
        try {
            logger.info("PTAF XML | Parsing XML from string content ({} characters).", xmlContent.length());
            InputStream stream = new ByteArrayInputStream(xmlContent.trim().getBytes(StandardCharsets.UTF_8));
            document = buildDocument(stream);
            logger.info("PTAF XML | XML string parsed successfully.");
        } catch (Exception e) {
            throw new RuntimeException(
                "PTAF XML | Failed to parse XML string content. " +
                "Ensure the content is well-formed XML. Root cause: " + e.getMessage(), e
            );
        }
    }

    // ─── Querying ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve the text value of an XML node or XPath expression.
     *
     * <p>The {@code query} parameter is interpreted as follows:</p>
     * <ul>
     *   <li>If it starts with {@code /} or {@code //} — treated as a full XPath expression.
     *       Example: {@code //order/status} or {@code /root/items/item[@id='1']/price}</li>
     *   <li>Otherwise — treated as a simple node name. The framework finds the first element
     *       in the document with that name. Example: {@code status} or {@code orderId}</li>
     * </ul>
     *
     * @param query XPath expression or simple node name
     * @return the text content of the matched node, or an empty string if the node has no text
     * @throws RuntimeException if the document has not been loaded, or the query finds no matching node
     */
    public String getValue(String query) {
        ensureDocumentLoaded("getValue");
        String xpathExpr = toXPath(query);
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            String result = (String) expr.evaluate(document, XPathConstants.STRING);
            logger.debug("PTAF XML | getValue({}) = [{}]", query, result);
            return result != null ? result : "";
        } catch (Exception e) {
            throw new RuntimeException(
                "PTAF XML | Failed to evaluate query [" + query + "] (resolved XPath: [" + xpathExpr + "]). " +
                "Root cause: " + e.getMessage(), e
            );
        }
    }

    /**
     * Count the number of nodes matching an XPath expression.
     *
     * <p>Useful for asserting that a collection contains a specific number of items.
     * Example: assert that an order contains exactly 3 line items.</p>
     *
     * <p>Example usage in a feature file:</p>
     * <pre>Then XML XPath "//order/items/item" count equals 3</pre>
     *
     * @param xpathExpression full XPath expression (must start with {@code /} or {@code //})
     * @return the number of nodes matching the expression
     * @throws RuntimeException if the document has not been loaded or the XPath is invalid
     */
    public int countNodes(String xpathExpression) {
        ensureDocumentLoaded("countNodes");
        try {
            XPathExpression expr = xpath.compile(xpathExpression);
            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            int count = nodes.getLength();
            logger.debug("PTAF XML | countNodes({}) = {}", xpathExpression, count);
            return count;
        } catch (Exception e) {
            throw new RuntimeException(
                "PTAF XML | Failed to count nodes for XPath [" + xpathExpression + "]. " +
                "Root cause: " + e.getMessage(), e
            );
        }
    }

    /**
     * Check whether a node or XPath expression matches at least one node in the document.
     *
     * <p>Uses the same query interpretation as {@link #getValue(String)}: if the query starts
     * with {@code /} or {@code //} it is treated as XPath, otherwise as a simple node name.</p>
     *
     * @param query XPath expression or simple node name
     * @return {@code true} if at least one matching node exists, {@code false} otherwise
     */
    public boolean nodeExists(String query) {
        ensureDocumentLoaded("nodeExists");
        String xpathExpr = toXPath(query);
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
            boolean exists = nodes.getLength() > 0;
            logger.debug("PTAF XML | nodeExists({}) = {}", query, exists);
            return exists;
        } catch (Exception e) {
            // If the XPath itself is invalid, treat as not found rather than throwing
            logger.warn("PTAF XML | nodeExists({}) evaluation error (treating as not found): {}", query, e.getMessage());
            return false;
        }
    }

    /**
     * Retrieve the value of an attribute on the first node matching the given XPath or node name.
     *
     * <p>Example: get the {@code id} attribute of the first {@code item} element.</p>
     * <pre>When I extract XML XPath "//order/items/item" attribute "id" and store as "FIRST_ITEM_ID"</pre>
     *
     * @param query         XPath expression or simple node name to locate the element
     * @param attributeName name of the attribute to retrieve
     * @return the attribute value, or an empty string if the attribute does not exist
     * @throws RuntimeException if the document has not been loaded or the query is invalid
     */
    public String getAttributeValue(String query, String attributeName) {
        ensureDocumentLoaded("getAttributeValue");
        String xpathExpr = toXPath(query);
        try {
            XPathExpression expr = xpath.compile(xpathExpr);
            Node node = (Node) expr.evaluate(document, XPathConstants.NODE);
            if (node == null) {
                throw new RuntimeException(
                    "PTAF XML | No node found for query [" + query + "] when reading attribute [" + attributeName + "]."
                );
            }
            Node attr = node.getAttributes() != null ? node.getAttributes().getNamedItem(attributeName) : null;
            String value = attr != null ? attr.getNodeValue() : "";
            logger.debug("PTAF XML | getAttributeValue({}, {}) = [{}]", query, attributeName, value);
            return value;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                "PTAF XML | Failed to get attribute [" + attributeName + "] for query [" + query + "]. " +
                "Root cause: " + e.getMessage(), e
            );
        }
    }

    /**
     * Returns the raw XML string of the currently loaded document.
     * Useful for debugging or logging the full XML content.
     *
     * @return raw XML string, or a message indicating no document is loaded
     */
    public String getRawContent() {
        if (document == null) return "(no XML document loaded)";
        try {
            javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            java.io.StringWriter sw = new java.io.StringWriter();
            transformer.transform(
                new javax.xml.transform.dom.DOMSource(document),
                new javax.xml.transform.stream.StreamResult(sw)
            );
            return sw.toString();
        } catch (Exception e) {
            return "(unable to serialize XML: " + e.getMessage() + ")";
        }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────────

    /**
     * Convert a user-supplied query string to an XPath expression.
     * If the query already starts with / or // it is returned unchanged.
     * Otherwise it is wrapped in //* to search for any element with that tag name.
     */
    private String toXPath(String query) {
        if (query == null) throw new IllegalArgumentException("XML query cannot be null.");
        String trimmed = query.trim();
        if (trimmed.startsWith("/")) return trimmed;
        // Simple node name — search anywhere in the document for the first element with this name
        return "//" + trimmed;
    }

    /** Build a Document from a File. */
    private Document buildDocument(File file) throws Exception {
        DocumentBuilderFactory factory = newSecureFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    /** Build a Document from an InputStream. */
    private Document buildDocument(InputStream stream) throws Exception {
        DocumentBuilderFactory factory = newSecureFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(stream);
    }

    /**
     * Create a DocumentBuilderFactory configured with XXE protection.
     * This prevents XML External Entity (XXE) injection attacks when parsing
     * untrusted XML content extracted from UI elements.
     */
    private DocumentBuilderFactory newSecureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // XXE protection — disable external entity resolution
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    /** Throw a clear error if the document has not been loaded yet. */
    private void ensureDocumentLoaded(String operation) {
        if (document == null) {
            throw new IllegalStateException(
                "PTAF XML | Cannot perform [" + operation + "] — no XML document is loaded. " +
                "Add a step like: Given I load XML file \"path/to/file.xml\" " +
                "or: Given I load XML from UI element on page \"X\" locator \"Y\" " +
                "before any XML assertion or extraction steps."
            );
        }
    }
}
