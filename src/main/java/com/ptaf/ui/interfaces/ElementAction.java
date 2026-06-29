package com.ptaf.ui.interfaces;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.List;

/**
 * The ElementAction interface defines a set of methods for interacting with web elements
 * on a page or within frames using the Playwright framework.
 *
 * <p>This interface provides a thin abstraction layer over Playwright's Page/Locator/Frame
 * primitives to support common test automation activities such as clicking, typing,
 * retrieving text/value, uploading files and performing assertions. Implementations of
 * this interface are expected to centralize element locator resolution (e.g., mapping
 * logical element names to concrete selectors) and standardize the way actions and
 * validations are performed in your test framework.</p>
 *
 * <p>Notes for testers and implementers:
 * - The {@code element} parameter is intended to be a logical name or key as used by an
 *   element repository (for example "loginButton" or "userNameField"). The implementation
 *   should translate that logical name + {@code key} to an actual selector (CSS, XPath,
 *   data-test-id, etc.).
 * - The {@code key} parameter typically contains the raw selector or a secondary index
 *   used by the repository to resolve the final Locator. Its meaning depends on your
 *   project conventions.
 * - Methods that return boolean should return true when the action succeeded and false
 *   when it failed in a handled way (e.g., element missing, action timed out). Errors
 *   that cannot be recovered from may still be thrown by the underlying Playwright APIs;
 *   implementations should decide whether to catch and convert them to boolean false or
 *   rethrow them depending on project conventions.</p>
 *
 * <p>Keep implementations consistent so test code and testers know whether they need to
 * check boolean results or expect thrown exceptions.</p>
 */
public interface ElementAction {

    // Perform an action on an element in the main (top-level) page.
    // Common action values: "click", "type", "clear", "hover", "doubleClick", "select", etc.
    // The implementation should resolve 'element' + 'key' into a concrete selector and
    // apply the requested action using the provided Playwright Page instance.
    /**
     * Performs a specified action on a web element located on the main page.
     *
     * <p>Usage examples:
     * - performActionPage(page, "click", "submitButton", "css=>#submit", null)
     * - performActionPage(page, "type", "username", "css=>input[name='user']", "testUser")</p>
     *
     * @param page   The Playwright Page instance representing the current web page. Must not be null.
     * @param action The action to be performed on the element (e.g., "click", "type", "clear", "getText").
     *               Implementations should document supported action strings.
     * @param element The logical name or identifier of the element in your element repository.
     * @param key    The locator string or auxiliary key used to identify the specific element (CSS, XPath, test-id, etc.).
     * @param value  The value used with the action when required (e.g., text to type). May be null for actions that don't need a value.
     * @return true if the action was performed successfully; false otherwise. Implementations should avoid throwing for common failures
     *         unless a non-recoverable error occurs; prefer returning false and logging details for testers.
     */
    boolean performActionPage(Page page, String action, String element, String key, String value);

    // Perform an action that returns a String result (for example "getText" or "getValue").
    // If the requested action does not return a meaningful String, implementations should return null.
    /**
     * Performs an action and returns a String result when applicable (e.g., "getText", "getValue").
     *
     * <p>This method is intended for read-only operations where the result needs to be used
     * by the caller (for assertions or further processing). For example:
     * - performActionPageWithReturn(page, "getText", "headerTitle", "css=>h1", null)</p>
     *
     * @param page    The Playwright Page instance.
     * @param action  The action to perform that yields a String result (e.g., "getText", "getValue", "getAttribute").
     * @param element Logical name of the element to operate on.
     * @param key     Locator string or auxiliary key for selector resolution.
     * @param value   Optional value needed for some actions (e.g., attribute name for "getAttribute"). May be null.
     * @return The resulting String value when applicable, or null if the action does not produce a String or the element was not found.
     */
    String performActionPageWithReturn(Page page, String action, String element, String key, String value);

    // Perform an action on an element that resides inside a specific frame. The frameLocator
    // parameter should point to the correct frame (by index, name, or selector) prior to resolving the inner element.
    /**
     * Performs a specified action on a web element located within a specified frame.
     *
     * <p>Frame-scoped actions are necessary when the target element exists inside an iframe.
     * The provided FrameLocator should be resolved by the caller/implementation to the frame
     * containing the element before performing the action.</p>
     *
     * @param frameLocator The Playwright FrameLocator instance used to identify the frame containing the element.
     *                     Must refer to the frame context where the element exists.
     * @param action The action to be performed on the element (e.g., "click", "type", "getText").
     * @param element The logical name or identifier of the element in your element repository.
     * @param key    The locator string used to identify the specific element within the frame.
     * @param value  The value to be used with the action (e.g., text to be entered during typing). May be null.
     * @return true if the action was performed successfully; false otherwise.
     */
    boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value);

    // Perform an action on an element that may be located within nested iframes.
    // iFrame, iFrame_2 and iFrame_3 should be provided as locator strings when nested frames are present.
    // If a frameLocator is provided it can be used by the implementation to scope searches more efficiently.
    /**
     * Performs a specified action on an element located in multiple nested frames if applicable.
     *
     * <p>This method supports up to three levels of iframe nesting through {@code iFrame}, {@code iFrame_2},
     * and {@code iFrame_3}. Implementations should choose the correct frame chain based on which values are
     * non-null/non-empty. If {@code frameLocator} is provided, it can be used to optimize frame resolution.</p>
     *
     * <p>Typical use-cases:
     * - Clicking a control inside an iframe inside a modal: pass the iframe selectors and the inner element logical name.
     * - Typing into a field within nested frames: pass "type" and the value to input.</p>
     *
     * @param page        The Playwright Page instance representing the current web page.
     * @param iFrame      Locator string for the first (outermost) iframe; may be null/empty if not used.
     * @param iFrame_2    Locator string for the second iframe (nested); may be null/empty if not used.
     * @param iFrame_3    Locator string for the third iframe (deeply nested); may be null/empty if not used.
     * @param action      The action to be performed on the element (e.g., "click", "type").
     * @param element     The logical name or identifier of the element to target.
     * @param key         The locator string used to identify the specific element (inside the innermost frame).
     * @param value       The value to be used with the action (e.g., text to be entered). May be null.
     * @param frameLocator Optional FrameLocator instance when pre-resolving frames is desired.
     * @return true if the action was performed successfully; false otherwise.
     */
    boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator);

    // Variation of performActionPageFrame which returns a String result (when applicable).
    // Use this for operations inside nested frames that produce textual output (getText/getValue).
    /**
     * Performs a specified action within nested frames and returns a String result when applicable.
     *
     * <p>Works like {@link #performActionPageFrame(Page, String, String, String, String, String, String, String, FrameLocator)}
     * but is intended for actions that return a textual result. If the requested action does not yield a string,
     * implementations should return null.</p>
     *
     * @param page        The Playwright Page instance.
     * @param iFrame      Locator string for the outermost iframe, if applicable.
     * @param iFrame_2    Locator string for the second-level iframe, if applicable.
     * @param iFrame_3    Locator string for the third-level iframe, if applicable.
     * @param action      The action to perform (for example "getText" or "getValue").
     * @param element     Logical name of the element to operate on.
     * @param key         Locator string for the element inside the relevant frame.
     * @param value       Optional value for actions that need it; may be null.
     * @param frameLocator Optional FrameLocator to help resolve frames faster.
     * @return The resulting String produced by the action, or null if not applicable or if the element was not found.
     */
    String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator);


    // Retrieve an ElementHandle for a given logical element on the top-level page.
    // Returning boolean implies the implementation may store the handle internally for later use;
    // it should return true if such retrieval succeeded.
    /**
     * Retrieves the ElementHandle for a specified element on the main page.
     *
     * <p>ElementHandle gives lower-level access to the DOM node and may be useful for
     * advanced operations not exposed by high-level Locator APIs (for example, JS evaluation
     * in the element's context). Implementations may cache the handle for subsequent operations.</p>
     *
     * @param page   The Playwright Page instance representing the current web page.
     * @param element The logical name or identifier of the element to retrieve.
     * @param key    The locator string used to identify the specific element.
     * @return true if the ElementHandle was obtained and (optionally) stored successfully; false otherwise.
     */
    boolean getElementHandlePage(Page page, String element, String key);

    // Retrieve an ElementHandle for an element located inside a frame.
    // Similar semantics to getElementHandlePage but scoped to the frame.
    /**
     * Retrieves the ElementHandle for a specified element within a frame.
     *
     * @param frameLocator The Playwright FrameLocator instance used to identify the frame.
     * @param element     The logical name or identifier of the element to retrieve.
     * @param key         The locator string used to identify the specific element.
     * @return true if the ElementHandle was obtained successfully; false otherwise.
     */
    boolean getElementHandleFrame(FrameLocator frameLocator, String element, String key);

    // Assert that the visible text of an element on the main page equals the expected text.
    // Implementations should trim/normalize whitespace as appropriate or document exact behavior.
    /**
     * Asserts that the text of a web element on the main page matches the expected text.
     *
     * <p>Implementations should clarify whether comparison is case-sensitive and whether
     * surrounding whitespace is trimmed prior to comparison. For reliability in tests, consider
     * normalizing whitespace and using explicit expectations.</p>
     *
     * @param page         The Playwright Page instance representing the current web page.
     * @param element      The logical name or identifier of the element whose text is being asserted.
     * @param key          The locator string used to identify the specific element.
     * @param expectedText The expected text that should match the element's actual text.
     * @return true if the element's text matches the expected text; false otherwise.
     */
    boolean assertElementTextPage(Page page, String element, String key, String expectedText);

    // Assert that the visible text of an element inside a frame equals the expected text.
    /**
     * Asserts that the text of a web element within a frame matches the expected text.
     *
     * @param frameLocator  The Playwright FrameLocator instance used to identify the frame containing the element.
     * @param element      The logical name or identifier of the element whose text is being asserted.
     * @param key          The locator string used to identify the specific element within the frame.
     * @param expectedText The expected text that should match the element's actual text.
     * @return true if the element's text matches the expected text; false otherwise.
     */
    boolean assertElementTextFrame(FrameLocator frameLocator, String element, String key, String expectedText);

    // Upload a file by resolving the element and invoking Playwright's file upload on the element.
    // The file_name parameter should be an accessible path from the test execution environment.
    /**
     * Uploads a file to a specified element on the main page.
     *
     * <p>The implementation should resolve {@code element} + {@code key} to a file input element
     * and set the file path specified by {@code file_name}. The file path should be accessible
     * from the machine or container running the tests (absolute paths are typically safest).</p>
     *
     * @param page      The Playwright Page instance representing the current web page.
     * @param file_name The name or path of the file to be uploaded. Prefer absolute paths or well-known test resource paths.
     * @param element   The logical name or identifier of the file upload element.
     * @param key       The locator string used to identify the specific element.
     */
    void uploadFile(Page page, String file_name, String element, String key);

    // Click a document link identified by name. Useful for lists of documents where link text,
    // title or a data attribute uniquely identifies the link.
    /**
     * Clicks on a document link identified by its name on the main page.
     *
     * <p>This helper is useful when documents/links are identified by displayed name rather than by a static selector.
     * Implementations may need to search a list or table for the given name and click the associated link.</p>
     *
     * @param page    The Playwright Page instance representing the current web page.
     * @param element The logical name or identifier of the document link to click (may correspond to display text).
     * @param key     The locator string used to identify the specific element or the container where the name is searched.
     */
    void clickOnDocumentLinkName(Page page, String element, String key);

    // Resolve and return the locator string for a logical element. This lets callers inspect
    // which selector will be used (handy for debugging and verifying repository mappings).
    /**
     * Retrieves the locator string for a specified element.
     *
     * <p>Returns the actual selector or locator expression that the implementation uses for
     * the logical {@code element} and {@code key}. This can be useful for debugging or reporting
     * to verify mappings in the element repository.</p>
     *
     * @param element The logical name of the element.
     * @param key     The locator string or auxiliary key used to resolve the specific element.
     * @return The resolved locator string corresponding to the specified element.
     */
    String getElement(String element, String key);

    // Return a list of ElementHandles matching the provided selector. If the element occurs multiple
    // times on the page (or inside a frame), this returns all matches. The frameLocator param is optional
    // and should be used when searching inside a frame.
    /**
     * Retrieves a list of ElementHandles for a specified element on the main page or within a frame.
     *
     * <p>When multiple matching DOM elements are present, this method returns handles for each match.
     * The returned list may be empty if no elements were found. Callers should check the list size
     * before using elements to avoid IndexOutOfBounds issues.</p>
     *
     * @param page        The Playwright Page instance representing the current web page.
     * @param element     The logical name or identifier of the element.
     * @param key         The locator string used to identify the specific element.
     * @param frameLocator The Playwright FrameLocator instance if working within a frame; may be null to search the top-level page.
     * @return A list of ElementHandles for the specified element. The list will be empty if no matches are found.
     */
    List<ElementHandle> getElementHandleList(Page page, String element, String key, FrameLocator frameLocator);

    // Return a Playwright Locator for an element, resolving nested iframe selectors if needed.
    // Locator provides higher-level, resilient operations (auto-waits) compared to ElementHandle.
    /**
     * Retrieves a Locator object for a specified element, considering the possible presence of nested frames.
     *
     * <p>Locator is a higher-level construct than ElementHandle and is preferred for most interactions
     * because it automatically waits for elements to be actionable. This method should resolve nested
     * frames using {@code iFrame}, {@code iFrame_2}, and {@code iFrame_3}, and then produce a Locator
     * pointing to the target element.</p>
     *
     * @param iFrame     The locator string for the first iframe (outermost), if applicable.
     * @param iFrame_2   The locator string for the second iframe (nested), if applicable.
     * @param iFrame_3   The locator string for the third iframe (deepest nested), if applicable.
     * @param element    The logical name or identifier of the element to locate.
     * @param key        The locator string used to identify the specific element.
     * @param page       The Playwright Page instance representing the current web page.
     * @param frameLocator The Playwright FrameLocator instance when needed for frame operations; may be null.
     * @return A Locator object for the specified element, taking frame nesting into account. Implementations should never return null;
     *         when an element cannot be resolved they may throw an informative exception or return a locator that will fail when used.
     */
    Locator getLocator(String iFrame, String iFrame_2, String iFrame_3, String element, String key, Page page, FrameLocator frameLocator);

    // Return the final, exact selector string that will be used for the element (no logical keys).
    // Useful for debug logs and verification.
    /**
     * Retrieves the exact locator string for a specified element.
     *
     * <p>This returns the final selector expression (for example a CSS or XPath string) that the
     * framework will use to identify the element. This is useful for troubleshooting locator mismatches.</p>
     *
     * @param element The logical name of the element.
     * @param key     The locator string or auxiliary key used to identify the specific element.
     * @return The exact locator string corresponding to the specified element.
     */
    String getExactLocator(String element, String key);
}
