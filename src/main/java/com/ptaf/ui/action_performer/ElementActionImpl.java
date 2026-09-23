package com.ptaf.ui.action_performer;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.ptaf.ui.handlers.LocatorHandler;
import com.ptaf.ui.helpers.ElementLocatorHelper;
import com.ptaf.ui.interfaces.ElementAction;
import com.ptaf.ui.page_helper.PageHelper;
import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ElementActionImpl is an implementation of the ElementAction interface that provides methods for
 * performing actions and assertions on web elements within an instance of a Playwright Page or FrameLocator.
 * It leverages helper classes (ActionPerformer, LocatorHandler, ElementLocatorHelper) to:
 * - Resolve locators defined in YAML (via ElementLocatorHelper)
 * - Convert locator types into Playwright Locator objects (via LocatorHandler)
 * - Execute actions and waits on Locators (via ActionPerformer)
 *
 * Notes for testers and integrators:
 * - Locator strings may represent chained selectors separated by ">" (e.g. "parent > child > inner").
 *   The getLocator method will resolve and chain these into a final Locator that can be used for actions.
 * - Methods are provided for both Page- and FrameLocator-based contexts. Use the appropriate API depending
 *   on where the element exists (main page vs nested iframes).
 * - This class does not change any runtime behavior; it only orchestrates locator resolution and action execution.
 */
public class ElementActionImpl extends PageHelper implements ElementAction {
    private static final Logger logger = LoggerFactory.getLogger(ElementActionImpl.class);

    // Responsible for executing Playwright actions (click, type, etc.) and waiting for elements.
    private final ActionPerformer actionPerformer = new ActionPerformer();
    // Helper that reads element definitions (like selectors and locator types) from configuration (YAML).
    private final ElementLocatorHelper elementLocatorHelper = new ElementLocatorHelper();
    // Converts locator type names (e.g., css, xpath) into actual Playwright Locator objects.
    private final LocatorHandler locatorHandler = new LocatorHandler();

    /**
     * Constructor.
     *
     * @param page The Playwright Page instance used by methods that require a Page context.
     *             PageHelper superclass is initialized with this page.
     */
    public ElementActionImpl(Page page) {
        super(page);
    }

    /**
     * Resolves a Locator based on a defined element key in the element YAML. This method supports
     * chained locators using ">" as a delimiter. For example, a YAML value like:
     *   "css:.parent > css:.child > xpath://button"
     * will produce a Locator that first finds ".parent" on the provided context (Page or FrameLocator)
     * and then chains to ".child" and then the button in sequence.
     *
     * This routine also supports providing a FrameLocator context directly or identifying nested iframe
     * frameLocators by the provided iFrame / iFrame_2 / iFrame_3 paths.
     *
     * @param iFrame       Optional first iframe selector (String) to scope the search. May be null or empty.
     * @param iFrame_2     Optional nested iframe selector for second level. May be null or empty.
     * @param iFrame_3     Optional nested iframe selector for third level. May be null or empty.
     * @param element      Top-level element name as defined in YAML. Used to look up the full locator string.
     * @param key          Key within the element definition to pick the specific locator (e.g., "selector", "label").
     * @param page         Optional Playwright Page object to use as starting context. May be null when using frameLocator.
     * @param frameLocator Optional FrameLocator to use as starting context (preferred over iFrame parameters when provided).
     * @return The final Playwright Locator after resolving the chain and locator types.
     * @throws RuntimeException If any part of the locator resolution fails. Exceptions are wrapped for clarity.
     */
    @Override
    public Locator getLocator(String iFrame, String iFrame_2, String iFrame_3, String element, String key, Page page, FrameLocator frameLocator) {
        // Retrieve the raw locator string from YAML via the helper (may contain chained parts separated by ">").
        String fullLocatorString = elementLocatorHelper.getElement(element, key);
        // Split the string by " > " into parts, trimming surrounding whitespace.
        String[] locatorParts = fullLocatorString.split("\\s*>\\s*");

        Locator currentLocator = null;

        try {
            // 1. Establish the initial context. This can be:
            //    - the provided FrameLocator (if not null),
            //    - a nested frameLocator built from iFrame / iFrame_2 / iFrame_3 strings,
            //    - or the Page instance as the default context.
            Object context = page;
            if (frameLocator != null) {
                // Explicit FrameLocator precedence: use it as starting context.
                context = frameLocator;
            } else if (iFrame != null && !iFrame.isEmpty()) {
                // Build nested FrameLocator if iFrame(s) provided.
                FrameLocator fl = page.frameLocator(iFrame);
                if (iFrame_2 != null && !iFrame_2.isEmpty()) fl = fl.frameLocator(iFrame_2);
                if (iFrame_3 != null && !iFrame_3.isEmpty()) fl = fl.frameLocator(iFrame_3);
                context = fl;
            }

            // 2. Iterate through locatorParts and convert each part to a Locator.
            //    The first part is created from the Page or FrameLocator context, subsequent parts are chained
            //    off the previous Locator (i.e., currentLocator.locator(nextSelector)).
            for (int i = 0; i < locatorParts.length; i++) {
                String part = locatorParts[i].trim();
                // Determine locator type (e.g., css, xpath, text) and the actual locator string portion.
                String locatorType = elementLocatorHelper.getLocatorType(part);
                String locator = elementLocatorHelper.getLocator(part);

                if (i == 0) {
                    // For the first part, create the Locator from the base context (Page or FrameLocator).
                    if (context instanceof Page) {
                        currentLocator = locatorHandler.getLocatorForType(locatorType, (Page) context, locator);
                    } else {
                        currentLocator = locatorHandler.getLocatorForType(locatorType, (FrameLocator) context, locator);
                    }
                } else {
                    // For subsequent parts, chain using the existing Locator as context.
                    currentLocator = locatorHandler.getLocatorForType(locatorType, currentLocator, locator);
                }
            }
            return currentLocator;

        } catch (Exception e) {
            // Wrap and rethrow to provide informative failure message with the original locator string.
            throw new RuntimeException("Failed to get locator for: '" + fullLocatorString + "'", e);
        }
    }

    /**
     * Convenience wrapper to perform an action on a Page context.
     *
     * @param page   The Playwright Page to use.
     * @param action The action name to perform (interpreted by ActionPerformer).
     * @param element Element name defined in YAML.
     * @param key    Specific key within the element definition.
     * @param value  Optional value (e.g., text to type).
     * @return true if action succeeded, false otherwise.
     */
    @Override
    public boolean performActionPage(Page page, String action, String element, String key, String value) {
        return performAction(page, null, null, null, action, element, key, value, null);
    }

    /**
     * Convenience wrapper to perform an action on a FrameLocator context.
     *
     * @param frameLocator FrameLocator that scopes the action.
     * @param action       The action to perform.
     * @param element      Element name in YAML.
     * @param key          Key under the element definition.
     * @param value        Optional action value.
     * @return true if action performed successfully, false otherwise.
     */
    @Override
    public boolean performActionFrame(FrameLocator frameLocator, String action, String element, String key, String value) {
        return performAction(null, null, null, null, action, element, key, value, frameLocator);
    }

    /**
     * Convenience wrapper for performing an action on a Page within one or more nested iframes.
     *
     * @param page     Main Page context.
     * @param iFrame   First level iframe selector (may be null).
     * @param iFrame_2 Second level iframe selector (may be null).
     * @param iFrame_3 Third level iframe selector (may be null).
     * @param action   Action to perform.
     * @param element  Element name in YAML.
     * @param key      Key inside the element definition.
     * @param value    Optional value for the action.
     * @param frameLocator Ignored in this wrapper; provided for signature parity with other methods.
     * @return true if action succeeded, false otherwise.
     */
    @Override
    public boolean performActionPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        return performAction(page, iFrame, iFrame_2, iFrame_3, action, element, key, value, null);
    }

    /**
     * Attempts to retrieve ElementHandles for an element defined in YAML on a Page.
     *
     * @param page    Page context.
     * @param element Element name.
     * @param key     Key within the element definition.
     * @return true if at least one ElementHandle was found, false otherwise.
     */
    @Override
    public boolean getElementHandlePage(Page page, String element, String key) {
        List<ElementHandle> elementHandles = getElementHandleList(page, element, key, null);
        return !elementHandles.isEmpty();
    }

    /**
     * Attempts to retrieve ElementHandles for an element defined in YAML on a FrameLocator.
     *
     * @param frameLocator FrameLocator context.
     * @param element      Element name.
     * @param key          Key within the element definition.
     * @return true if at least one ElementHandle was found, false otherwise.
     */
    @Override
    public boolean getElementHandleFrame(FrameLocator frameLocator, String element, String key) {
        List<ElementHandle> elementHandles = getElementHandleList(null, element, key, frameLocator);
        return !elementHandles.isEmpty();
    }

    /**
     * Assert that the text content of an element on the Page matches the expectedText.
     *
     * @param page         Playwright Page context.
     * @param element      Element name in YAML.
     * @param key          Key within element definition.
     * @param expectedText Expected text content for assertion.
     * @return true if text matches exactly, false otherwise.
     */
    @Override
    public boolean assertElementTextPage(Page page, String element, String key, String expectedText) {
        return assertElementText(page, element, key, expectedText, null);
    }

    /**
     * Assert that the text content of an element inside a FrameLocator matches the expectedText.
     *
     * @param frameLocator FrameLocator context.
     * @param element      Element name.
     * @param key          Key within element definition.
     * @param expectedText Expected text content.
     * @return true if text matches exactly, false otherwise.
     */
    @Override
    public boolean assertElementTextFrame(FrameLocator frameLocator, String element, String key, String expectedText) {
        return assertElementText(null, element, key, expectedText, frameLocator);
    }

    /**
     * Perform an action that returns a String result (for actions that produce values).
     * Example: retrieving text or attribute values after some action.
     *
     * @param page    Page context used by the ActionPerformer for some operations.
     * @param action  Action name.
     * @param element Element in YAML.
     * @param key     Key inside element definition.
     * @param value   Optional value for action.
     * @return String result returned by the action performer, or null on failure.
     */
    @Override
    public String performActionPageWithReturn(Page page, String action, String element, String key, String value) {
        try {
            // Resolve the target locator using the Page context.
            Locator targetLocator = getLocatorBasedOnPage(page, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for element: {} with key: {}", element, key);
                return null;
            }
            // Ensure the element is present/visible as required by the performer.
            actionPerformer.waitForLocator(targetLocator);
            // Delegate the actual action that returns a value to the ActionPerformer.
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageWithReturn for element '{}' and action '{}':", element, action, e);
            return null;
        }
    }

    /**
     * Perform an action that returns a String result within nested iframe context on a Page.
     *
     * @param page      Page context.
     * @param iFrame    First level iframe selector (may be null).
     * @param iFrame_2  Second level iframe selector (may be null).
     * @param iFrame_3  Third level iframe selector (may be null).
     * @param action    Action to perform.
     * @param element   Element name in YAML.
     * @param key       Key in element definition.
     * @param value     Optional action value.
     * @param frameLocator Ignored in this wrapper; included for parity with other signatures.
     * @return String result or null on failure.
     */
    @Override
    public String performActionPageFrameWithReturn(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        try {
            // Get locator using nested iframe selectors
            Locator targetLocator = getLocatorBasedOnPageFrame(page, iFrame, iFrame_2, iFrame_3, element, key);
            if (targetLocator == null) {
                logger.error("Locator not found for nested frame element: {} with key: {}", element, key);
                return null;
            }
            actionPerformer.waitForLocator(targetLocator);
            return actionPerformer.performActionWithReturn(page, action, targetLocator, value);
        } catch (Exception e) {
            logger.error("Exception in performActionPageFrameWithReturn for element '{}' and action '{}':", element, action, e);
            return null;
        }
    }

    /**
     * Core internal method that resolves the appropriate Locator (Page or FrameLocator based),
     * waits for the locator and then asks ActionPerformer to execute the action.
     *
     * This is the single implementation point used by the public performAction wrappers.
     *
     * @param page        Page context (may be null if frameLocator is provided).
     * @param iFrame      Optional iframe path component 1 (used when page != null).
     * @param iFrame_2    Optional iframe path component 2 (used when page != null).
     * @param iFrame_3    Optional iframe path component 3 (used when page != null).
     * @param action      Action to perform.
     * @param element     YAML element name.
     * @param key         Key under element.
     * @param value       Optional action value.
     * @param frameLocator Optional FrameLocator context (takes precedence if provided).
     * @return true if action executed successfully, false on any error.
     */
    private boolean performAction(Page page, String iFrame, String iFrame_2, String iFrame_3, String action, String element, String key, String value, FrameLocator frameLocator) {
        Locator targetLocator = null;
        try {
            // Determine context and resolve locator appropriately.
            if (frameLocator != null) {
                // FrameLocator context directly provided.
                targetLocator = getLocator(null, null, null, element, key, null, frameLocator);
            } else if (page != null) {
                // Page context with optional nested iframe selectors.
                targetLocator = getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
            } else {
                // No valid context provided, throw to indicate misuse.
                throw new IllegalArgumentException("A Page or FrameLocator context is required.");
            }

            if (targetLocator == null) {
                // Defensive check: ensure we have a locator before attempting action.
                throw new IllegalStateException("Failed to resolve a target Locator for element: " + element + " with key: " + key);
            }

            // Wait for the resolved locator to be ready (visibility/attached as per ActionPerformer).
            actionPerformer.waitForLocator(targetLocator);
            // Delegate to the performer to execute the specified action.
            actionPerformer.performAction(page, action, targetLocator, value);
            return true;
        } catch (Exception e) {
            // Log the failure with context for easier debugging during tests.
            logger.error("Error while performing action '{}' on element '{}' with key '{}'", action, element, key, e);
        }

        return false;
    }

    /**
     * Uploads a file by invoking a file chooser on the page and setting the file.
     *
     * Important: The element definition for file input/button is expected to be present in YAML.
     *
     * @param page      Page context.
     * @param file_name Key under the element definition that contains the actual file path to upload.
     * @param element   Element name which triggers the file chooser (e.g., "uploadButton").
     * @param key       Key for the element's selector in YAML.
     */
    @Override
    public void uploadFile(Page page, String file_name, String element, String key) {
        // Wait for the file chooser event that will be triggered by the click on the target element.
        FileChooser fileChooser = page.waitForFileChooser(() ->
                page.click(getElement(element, key)));
        // Convert the file path from YAML into a Path and set it on the chooser.
        fileChooser.setFiles(Paths.get(getElement(element, file_name)));
    }

    /**
     * Clicks a link identified by its accessible name (role=link, name=<fileName>).
     * This method extracts the last segment of the configured element path and uses it as the link name.
     *
     * @param page    Page instance.
     * @param element Element name in YAML that holds the document link path.
     * @param key     Key within the element definition that returns the link path.
     */
    @Override
    public void clickOnDocumentLinkName(Page page, String element, String key) {
        String documentLinkName = getElement(element, key);
        // Extract the filename from a potential path (e.g., "dir/myfile.pdf" -> "myfile.pdf")
        String fileName = extractFileName(documentLinkName);
        System.out.println(fileName);
        try {
            // Use Playwright's accessibility-based role selection to click the link by name.
            page.getByRole(AriaRole.LINK,
                    new Page.GetByRoleOptions().setName(fileName)).click();
        } catch (Exception e) {
            // Log failure for diagnostics. Keep method resilient for test flows.
            logger.error("Failed to click on element by Role '{}'", element + key, e);
        }
    }

    /**
     * Extract the file name from a path string by splitting on "/" and returning the last element.
     *
     * @param filePath Path or URL-like string.
     * @return The final segment after the last "/".
     */
    public static String extractFileName(String filePath) {
        String[] parts = filePath.split("/");
        return parts[parts.length - 1];
    }

    /**
     * Helper to fetch raw element selector strings from YAML using YamlReader.
     *
     * @param element Element name in YAML.
     * @param key     Key under the element (e.g., "selector" or "label").
     * @return The raw string value stored in YAML for the element key.
     * @throws RuntimeException Rethrows any failure to retrieve the selector, after logging.
     */
    public String getElement(String element, String key) {
        try {
            return (String) YamlReader.get("elements." + element + "." + key);
        } catch (Exception e) {
            // Log with context to help testers locate missing configuration values.
            logger.error("Failed to retrieve selector for element '{}'", element + key, e);
            throw e;
        }
    }

    /**
     * Internal assertion routine used by public assertion wrappers. Resolves the locator and compares
     * the first matched element's textContent to the expected text.
     *
     * @param page         Page context, may be null if frameLocator is provided.
     * @param element      Element name in YAML.
     * @param key          Key under element.
     * @param expectedText The expected text to compare against.
     * @param frameLocator Optional FrameLocator to scope the assertion.
     * @return true if text matches exactly, false otherwise (including on any error).
     */
    private boolean assertElementText(Page page, String element, String key, String expectedText, FrameLocator frameLocator) {
        try {
            // Resolve the target locator based on provided context.
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            // Read the text content from the first matched node.
            String actualText = targetLocator.first().textContent();
            boolean isTextMatching = expectedText.equals(actualText);
            logger.info("Asserting text on element '{}': expected '{}', actual '{}'", element, expectedText, actualText);
            if (!isTextMatching) {
                // Log mismatch details to aid test debugging.
                logger.error("Text mismatch: expected '{}' but found '{}'", expectedText, actualText);
            }
            return isTextMatching;
        } catch (Exception e) {
            logger.error("Error while asserting text on element '{}'", element, e);
            return false;
        }
    }

    /**
     * Retrieve a list of ElementHandle objects for an element defined in YAML. This can be useful
     * for low-level interactions and validations not covered by Locator-based actions.
     *
     * @param page         Page context (nullable if frameLocator provided).
     * @param element      Element name.
     * @param key          Key in element definition pointing to selector.
     * @param frameLocator Optional FrameLocator to use as context.
     * @return List of ElementHandle instances (empty list on failure or if none found).
     */
    @Override
    public List<ElementHandle> getElementHandleList(Page page, String element, String key, FrameLocator frameLocator) {
        List<ElementHandle> elementHandles = new ArrayList<>();
        try {
            // Resolve a Locator for the element, then obtain its element handles.
            Locator targetLocator = getLocator(null, null, null, element, key, page, frameLocator);
            if (targetLocator != null) {
                elementHandles = targetLocator.elementHandles();
            } else {
                logger.error("Target locator for element '{}' could not be determined.", element);
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve element handles for '{}'", element, e);
        }
        return elementHandles;
    }

    /**
     * Convenience helper to get a Locator for a top-level Page element.
     *
     * @param page    Page context.
     * @param element Element name.
     * @param key     Key within element definition.
     * @return Resolved Locator or null if resolution fails.
     */
    private Locator getLocatorBasedOnPage(Page page, String element, String key) {
        return getLocator(null, null, null, element, key, page, null);
    }

    /**
     * Convenience helper to get a Locator for an element inside nested iframes on a Page.
     *
     * @param page     Page context.
     * @param iFrame   First-level iframe selector.
     * @param iFrame_2 Second-level iframe selector.
     * @param iFrame_3 Third-level iframe selector.
     * @param element  Element name.
     * @param key      Key within element definition.
     * @return Resolved Locator or null if resolution fails.
     */
    private Locator getLocatorBasedOnPageFrame(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String key) {
        return getLocator(iFrame, iFrame_2, iFrame_3, element, key, page, null);
    }

    /**
     * Convenience helper to get a Locator when a FrameLocator is already available.
     *
     * @param frameLocator FrameLocator context.
     * @param element      Element name.
     * @param key          Key within element definition.
     * @return Resolved Locator or null if resolution fails.
     */
    private Locator getLocatorBasedOnFrame(FrameLocator frameLocator, String element, String key) {
        return getLocator(null, null, null, element, key, null, frameLocator);
    }

    /**
     * Returns the raw locator string for a given element and key (the actual selector portion),
     * not including any locator type prefix. This is useful when callers need the literal selector value.
     *
     * @param element Element name from YAML.
     * @param key     Key within the element definition.
     * @return The specific locator string extracted from the YAML entry.
     */
    @Override
    public String getExactLocator(String element, String key) {
        String locatorValue = elementLocatorHelper.getElement(element, key);
        return elementLocatorHelper.getLocator(locatorValue);
    }
}
