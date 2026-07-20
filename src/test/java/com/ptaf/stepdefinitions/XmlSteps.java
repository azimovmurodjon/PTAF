package com.ptaf.stepdefinitions;

import com.ptaf.hooks.Hooks;
import com.ptaf.ui.pages.PageCommonMethods;
import com.ptaf.xml.XmlCommonMethods;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber step definitions for XML automation in the PTAF framework.
 *
 * <p>This class provides two categories of XML steps:</p>
 *
 * <h3>1. File-based XML steps</h3>
 * <p>Load an XML file from the filesystem and assert or extract values from it.</p>
 * <pre>
 * Given I load XML file "src/test/resources/data/response.xml"
 * Then XML node "status" equals "SUCCESS"
 * Then XML XPath "//order/items/item" count equals 3
 * </pre>
 *
 * <h3>2. UI-embedded XML steps</h3>
 * <p>Extract XML content from a visible UI element (e.g., a textarea, code block, or
 * pre-formatted text area) and then assert or extract values from it.</p>
 * <pre>
 * Given I load XML from UI element on page "ResponsePage" locator "xmlTextArea"
 * Then XML node "status" equals "SUCCESS"
 * Then XML XPath "//response/orderId" equals "12345"
 * </pre>
 *
 * <h3>Querying strategy</h3>
 * <p>All query steps accept either:</p>
 * <ul>
 *   <li>A <strong>simple node name</strong> (e.g., {@code "status"}) — finds the first element
 *       with that tag name anywhere in the document.</li>
 *   <li>A <strong>full XPath expression</strong> (e.g., {@code "//order/status"}) — evaluates
 *       the XPath against the document root. Any valid XPath 1.0 expression is supported.</li>
 * </ul>
 *
 * <h3>Thread safety and lifecycle</h3>
 * <p>The loaded XML document is stored in a {@link com.ptaf.xml.XmlContext} ThreadLocal and is
 * automatically cleared after each scenario via the {@code @After} hook in this class.</p>
 */
public class XmlSteps {

    private static final Logger logger = LoggerFactory.getLogger(XmlSteps.class);

    /** Shared XML methods instance — holds the variable store for this scenario. */
    private final XmlCommonMethods xml = new XmlCommonMethods();

    // ─── Loading Steps ────────────────────────────────────────────────────────────

    /**
     * Load and parse an XML file from the filesystem into the current scenario's XML context.
     *
     * <p>The path is resolved relative to the project root (where {@code pom.xml} is located).
     * Both absolute and relative paths are supported.</p>
     *
     * <p>Example:</p>
     * <pre>Given I load XML file "src/test/resources/data/order_response.xml"</pre>
     *
     * @param filePath path to the XML file
     */
    @Given("I load XML file {string}")
    public void iLoadXmlFile(String filePath) {
        xml.loadFromFile(filePath);
    }

    /**
     * Extract XML content from a visible UI element and load it into the XML context.
     *
     * <p>This step finds the UI element identified by the given page and locator keys,
     * extracts its text content (using {@code .getText()} and {@code .getAttribute("value")}
     * as fallback), and parses it as XML. The element must contain well-formed XML.</p>
     *
     * <p>Typical use cases:</p>
     * <ul>
     *   <li>A {@code <textarea>} showing an API response body</li>
     *   <li>A {@code <pre>} or {@code <code>} block displaying formatted XML</li>
     *   <li>A read-only input field containing an XML snippet</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>Given I load XML from UI element on page "ResponsePage" locator "xmlTextArea"</pre>
     *
     * @param page    the logical page name matching the YAML element file (e.g., "ResponsePage")
     * @param locator the element key matching the YAML element file (e.g., "xmlTextArea")
     */
    @Given("I load XML from UI element on page {string} locator {string}")
    public void iLoadXmlFromUiElement(String page, String locator) {
        com.microsoft.playwright.Page playwrightPage = Hooks.getPage();
        PageCommonMethods pcm = new PageCommonMethods(playwrightPage);
        // Use gettext() which resolves the locator via YAML and returns the element's inner text.
        // This works for div, pre, span, code, and textarea elements.
        String rawContent = pcm.gettext(playwrightPage, page, locator);
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new RuntimeException(
                "PTAF XML | UI element on page [" + page + "] locator [" + locator + "] " +
                "contains no text content. Ensure the element is visible and contains XML text. " +
                "For <input> or <textarea> elements, use a step that reads the 'value' attribute instead."
            );
        }
        xml.loadFromString(rawContent);
        logger.info("PTAF XML | Loaded XML from UI element [{}.{}]", page, locator);
    }

    // ─── Assertion Steps — Value Equality ────────────────────────────────────────

    /**
     * Assert that the value of an XML node equals the expected value exactly.
     *
     * <p>Accepts both simple node names and full XPath expressions.</p>
     *
     * <p>Examples:</p>
     * <pre>
     * Then XML node "status" equals "SUCCESS"
     * Then XML node "//order/status" equals "APPROVED"
     * </pre>
     *
     * @param query    simple node name or XPath expression
     * @param expected the exact expected value
     */
    @Then("XML node {string} equals {string}")
    public void xmlNodeEquals(String query, String expected) {
        xml.assertValueEquals(query, expected);
    }

    /**
     * Assert that the value of an XPath expression equals the expected value exactly.
     *
     * <p>This step is identical to {@link #xmlNodeEquals(String, String)} but uses the
     * keyword "XPath" for clarity when writing full XPath expressions.</p>
     *
     * <p>Example:</p>
     * <pre>Then XML XPath "//order/items/item[@id='1']/price" equals "29.99"</pre>
     *
     * @param xpathExpression full XPath expression
     * @param expected        the exact expected value
     */
    @Then("XML XPath {string} equals {string}")
    public void xmlXPathEquals(String xpathExpression, String expected) {
        xml.assertValueEquals(xpathExpression, expected);
    }

    // ─── Assertion Steps — Value Contains ────────────────────────────────────────

    /**
     * Assert that the value of an XML node contains the expected substring.
     *
     * <p>Example:</p>
     * <pre>Then XML node "message" contains "successfully processed"</pre>
     *
     * @param query    simple node name or XPath expression
     * @param expected the substring that must be present in the actual value
     */
    @Then("XML node {string} contains {string}")
    public void xmlNodeContains(String query, String expected) {
        xml.assertValueContains(query, expected);
    }

    /**
     * Assert that the value of an XPath expression contains the expected substring.
     *
     * <p>Example:</p>
     * <pre>Then XML XPath "//response/message" contains "processed"</pre>
     *
     * @param xpathExpression full XPath expression
     * @param expected        the substring that must be present
     */
    @Then("XML XPath {string} contains {string}")
    public void xmlXPathContains(String xpathExpression, String expected) {
        xml.assertValueContains(xpathExpression, expected);
    }

    // ─── Assertion Steps — Value Not Equals ──────────────────────────────────────

    /**
     * Assert that the value of an XML node does NOT equal the given value.
     *
     * <p>Example:</p>
     * <pre>Then XML node "errorCode" does not equal "0"</pre>
     *
     * @param query    simple node name or XPath expression
     * @param expected the value that must NOT be present
     */
    @Then("XML node {string} does not equal {string}")
    public void xmlNodeNotEquals(String query, String expected) {
        xml.assertValueNotEquals(query, expected);
    }

    // ─── Assertion Steps — Node Existence ────────────────────────────────────────

    /**
     * Assert that at least one node matching the query exists in the XML document.
     *
     * <p>Examples:</p>
     * <pre>
     * Then XML node "errorCode" exists
     * Then XML node "//order/items/item" exists
     * </pre>
     *
     * @param query simple node name or XPath expression
     */
    @Then("XML node {string} exists")
    public void xmlNodeExists(String query) {
        xml.assertNodeExists(query);
    }

    /**
     * Assert that no node matching the query exists in the XML document.
     *
     * <p>Example:</p>
     * <pre>Then XML node "errorCode" does not exist</pre>
     *
     * @param query simple node name or XPath expression
     */
    @Then("XML node {string} does not exist")
    public void xmlNodeNotExists(String query) {
        xml.assertNodeNotExists(query);
    }

    // ─── Assertion Steps — Node Count ────────────────────────────────────────────

    /**
     * Assert that the number of nodes matching an XPath expression equals the expected count.
     *
     * <p>Example:</p>
     * <pre>Then XML XPath "//order/items/item" count equals 3</pre>
     *
     * @param xpathExpression full XPath expression
     * @param expectedCount   the exact number of nodes expected
     */
    @Then("XML XPath {string} count equals {int}")
    public void xmlXPathCountEquals(String xpathExpression, int expectedCount) {
        xml.assertNodeCount(xpathExpression, expectedCount);
    }

    // ─── Assertion Steps — Attributes ────────────────────────────────────────────

    /**
     * Assert that the value of an attribute on the first matching node equals the expected value.
     *
     * <p>Example:</p>
     * <pre>Then XML node "//order/items/item" attribute "id" equals "1"</pre>
     *
     * @param query         XPath expression or simple node name
     * @param attributeName name of the XML attribute to check
     * @param expected      the expected attribute value
     */
    @Then("XML node {string} attribute {string} equals {string}")
    public void xmlNodeAttributeEquals(String query, String attributeName, String expected) {
        xml.assertAttributeEquals(query, attributeName, expected);
    }

    // ─── Assertion Steps — Stored Values ─────────────────────────────────────────

    /**
     * Assert that the value of an XML node equals a previously stored variable value.
     *
     * <p>Example:</p>
     * <pre>
     * When I extract XML node "orderId" and store as "ORDER_ID"
     * Then XML node "confirmationId" equals stored value "ORDER_ID"
     * </pre>
     *
     * @param query        simple node name or XPath expression
     * @param variableName name of the previously stored variable
     */
    @Then("XML node {string} equals stored value {string}")
    public void xmlNodeEqualsStoredValue(String query, String variableName) {
        String expected = xml.getStoredValue(variableName);
        xml.assertValueEquals(query, expected);
    }

    // ─── Extraction Steps ─────────────────────────────────────────────────────────

    /**
     * Extract the value of an XML node and store it under a named variable for use in later steps.
     *
     * <p>Example:</p>
     * <pre>When I extract XML node "orderId" and store as "ORDER_ID"</pre>
     *
     * @param query        simple node name or XPath expression
     * @param variableName name to store the extracted value under
     */
    @When("I extract XML node {string} and store as {string}")
    public void iExtractXmlNodeAndStoreAs(String query, String variableName) {
        xml.extractAndStore(query, variableName);
    }

    /**
     * Extract the value of an XPath expression and store it under a named variable.
     *
     * <p>Example:</p>
     * <pre>When I extract XML XPath "//order/items/item[@id='1']/price" and store as "ITEM_PRICE"</pre>
     *
     * @param xpathExpression full XPath expression
     * @param variableName    name to store the extracted value under
     */
    @When("I extract XML XPath {string} and store as {string}")
    public void iExtractXmlXPathAndStoreAs(String xpathExpression, String variableName) {
        xml.extractAndStore(xpathExpression, variableName);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────────

    /**
     * Cucumber {@code @After} hook that clears the XML context and variable store after each scenario.
     *
     * <p>This ensures test isolation — XML loaded in one scenario does not bleed into the next.
     * This hook runs automatically after every scenario; no action is required from testers.</p>
     */
    @After
    public void clearXmlContext() {
        xml.clear();
    }
}
