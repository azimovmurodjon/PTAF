package com.ptaf.ui.pages;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.ptaf.ui.action_performer.ElementActionImpl;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.interfaces.ElementAction;
import com.ptaf.utils.ScreenshotHandler;
import com.microsoft.playwright.Page;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

/**
 * <h1>PageCommonMethods</h1>
 *
 * <p>
 * The <code>PageCommonMethods</code> class serves as a foundational
 * utility for executing common web element interactions in a
 * Playwright-powered test automation framework. This class encapsulates
 * operations such as clicking, filling inputs, verifying visibility,
 * and more, abstracting these actions to promote code reusability and
 * maintainability across multiple page objects.
 * </p>
 *
 * <p>
 * The class utilizes an <code>ElementAction</code> instance to perform
 * interactions with web elements specified by locators. Robust error
 * handling is incorporated to gracefully manage failures, ensuring that
 * the test execution proceeds smoothly while logging pertinent information.
 * The screenshot functionality captures the state of the application when
 * scenarios pass or fail, serving as a vital tool for debugging and
 * validation.
 * </p>
 *
 * <p>
 * This class is designed to be thread-safe and works in conjunction
 * with Cucumber scenarios to maintain context-specific information.
 * </p>
 */
public class PageCommonMethods {
    private final Page page; // Playwright Page instance to interact with the browser
    private final ElementAction elementAction; // Interface instance handling element actions
    private boolean isFailed = false; // Flag indicating if any action has failed
    private static final ThreadLocal<Scenario> currentScenario = new ThreadLocal<>(); // Thread-local variable to maintain the current Cucumber scenario
    private static final Logger logger = LoggerFactory.getLogger(PageCommonMethods.class); // Logger for logging events

    /**
     * Initializes an instance of <code>PageCommonMethods</code> with a given Playwright page.
     *
     * @param page The Playwright Page instance to interact with.
     */
    public PageCommonMethods(Page page) {
        this.page = page; // Assign the provided page instance to the class variable
        this.elementAction = new ElementActionImpl(page); // Initialize element action performer for executing actions on web elements
    }

    /**
     * Clicks on a web element specified by the locator.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being clicked.
     * @param locator The locator string used to identify the element.
     */
    public void click(Page page, String element, String locator) {
        performAction("click", page, element, locator, null); // Delegate the click action to performAction method
    }

    /**
     * Check on radio on a web element specified by the locator.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being clicked.
     * @param locator The locator string used to identify the element.
     */
    public void radio(Page page, String element, String locator) {
        performAction("radio", page, element, locator, null); // Delegate the click action to performAction method
    }

    /**
     * Fills an input field with the specified value.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being filled.
     * @param locator The locator string used to identify the input element.
     * @param value   The value to be filled in the field.
     */
    public void fill(Page page, String element, String locator, String value) {
        performAction("fill", page, element, locator, value); // Delegate the fill action to performAction method
    }

    /**
     * Selects an option from a dropdown based on the provided value.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the dropdown.
     * @param locator The locator string used to identify the dropdown.
     * @param value   The value of the option to be selected.
     */
    public void select(Page page, String element, String locator, String value) {
        performAction("select", page, element, locator, value); // Delegate the selection action to performAction method
    }

    /**
     * Checks a checkbox element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the checkbox.
     * @param locator The locator string used to identify the checkbox.
     */
    public void check(Page page, String element, String locator) {
        performAction("check", page, element, locator, null); // Delegate the check action to performAction method
    }

    /**
     * Unchecks a checkbox element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the checkbox.
     * @param locator The locator string used to identify the checkbox.
     */
    public void uncheck(Page page, String element, String locator) {
        performAction("uncheck", page, element, locator, null); // Delegate the uncheck action to performAction method
    }

    /**
     * Hovers over a specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to hover.
     * @param locator The locator string used to identify the element.
     */
    public void hover(Page page, String element, String locator) {
        performAction("hover", page, element, locator, null); // Delegate the hover action to performAction method
    }

    /**
     * Types a specified value into a designated input field.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being typed into.
     * @param locator The locator string used to identify the input field.
     * @param value   The value to be typed into the field.
     */
    public void type(Page page, String element, String locator, String value) {
        performAction("type", page, element, locator, value); // Delegate the type action to performAction method
    }

    /**
     * Presses a specified key on the target element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element on which the key will be pressed.
     * @param locator The locator string used to identify the element.
     * @param value   The key to be pressed.
     */
    public void press(Page page, String element, String locator, String value) {
        performAction("press", page, element, locator, value); // Delegate the press action to performAction method
    }

    /**
     * Double-clicks on the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being double-clicked.
     * @param locator The locator string used to identify the element.
     */
    public void dblclick(Page page, String element, String locator) {
        performAction("dblclick", page, element, locator, null); // Delegate the double-click action to performAction method
    }

    /**
     * Takes a screenshot of the specified element on the current Playwright Page.
     * <p>
     * This method constructs the locator for the specified element using the provided
     * logical name and locator string. It then delegates the action of taking
     * a screenshot to a helper method and finalizes the scenario by taking
     * an additional screenshot if applicable.
     *
     * @param page    The current Playwright Page instance where the screenshot will be taken.
     * @param element The logical name of the element from which to capture the screenshot.
     * @param locator The locator string used to identify the specific element on the page.
     * @param value   Additional parameters for the screenshot action, if any (e.g., options for the screenshot).
     */
    public void screenshot(Page page, String element, String locator, String value) {
        // Get the precise locator for the specified element using the element action helper
        String targetLocator = elementAction.getExactLocator(element, locator);

        // Delegate the screenshot action to the performAction method,
        // which is responsible for interfacing with Playwright to capture the screenshot.
        performAction("screenshot", page, element, locator, value);

        // Finalize the scenario by taking a screenshot using the target locator,
        // this may attach a screenshot to the scenario if certain conditions are met.
        finalizeScenarioScreenshot(page, targetLocator);
    }

    /**
     * Initiates a file download from the given page by interacting with a specified element
     * and saves the downloaded file to the provided directory with a custom suffix.
     *
     * @param page    Playwright Page object representing the current browser page.
     * @param element The element name defined in the locator configuration (e.g., YAML key).
     * @param locator The locator type used to identify the element (e.g., XPATH, CSS).
     * @param value   The directory path where the downloaded file should be saved.
     */
    public void download(Page page, String element, String locator, String value) {
        performAction("download", page, element, locator, value);
//
//        // Wait for a download to begin after performing the download-triggering action
//        Download download = page.waitForDownload(() -> {
//            // Perform the click action on the specified element to initiate the download
//            click(page, element, locator);
//        });
//
//        // After the download is completed, save the file to the specified path
//        // The filename is composed of the browser-suggested name plus the provided suffix
//        download.saveAs(Paths.get(value, download.suggestedFilename() + name));
    }


    /**
     * Scrolls the page to the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to scroll to.
     * @param locator The locator string used to identify the element.
     */
    public void scroll(Page page, String element, String locator) {
        performAction("scroll", page, element, locator, null); // Delegate the scroll action to performAction method
    }

    /**
     * Focuses on the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to focus on.
     * @param locator The locator string used to identify the element.
     */
    public void focus(Page page, String element, String locator) {
        performAction("focus", page, element, locator, null); // Delegate the focus action to performAction method
    }

    /**
     * Removes focus from the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to blur.
     * @param locator The locator string used to identify the element.
     * @param value   Additional parameters for blurring, if any.
     */
    public void blur(Page page, String element, String locator, String value) {
        performAction("blur", page, element, locator, value); // Delegate the blur action to performAction method
    }

    /**
     * Clears the value of an input or element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to clear.
     * @param locator The locator string used to identify the element.
     */
    public void clear(Page page, String element, String locator) {
        performAction("clear", page, element, locator, null); // Delegate the clear action to performAction method
    }

    /**
     * Drags the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to drag.
     * @param locator The locator string used to identify the element.
     */
    public void drag(Page page, String element, String locator) {
        performAction("drag", page, element, locator, null); // Delegate the drag action to performAction method
    }

    /**
     * Retrieves the visible text content of the specified element on the given page.
     * Logs a message if the text is null or empty.
     *
     * @param page    The Playwright {@link Page} instance used for locating the element.
     * @param element The logical name or identifier of the element (used in the YAML or config).
     * @param locator The specific locator key for identifying the element in the configuration.
     * @return The text content of the element, or null if not found or empty.
     */
    public String gettext(Page page, String element, String locator) {
        String value = getStringValue("gettext", page, element, locator, null);

        if (value == null || value.trim().isEmpty()) {
            System.out.println("There is no text value for element: " + element + ", locator: " + locator);
        }

        return value;
    }

    /**
     * Gets the text content of the specified element and contains content.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to extract text from.
     * @param locator The locator string used to identify the element.
     */
    public void get_and_contain_text(Page page, String element, String locator) {
        performAction("get_and_contain_text", page, element, locator, null); // Delegate the action to get text to performAction method
    }

    /**
     * Checks whether the given element is visible on the page.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check visibility.
     * @param locator The locator string used to identify the element.
     */
    public void isvisible(Page page, String element, String locator) {
        performAction("isvisible", page, element, locator, null); // Delegate visibility check to performAction method
    }

    /**
     * Checks if the given element is enabled on the current page.
     * This method uses the performAction method to perform the actual check.
     *
     * @param page    The current Playwright Page where the element resides.
     * @param element The logical name of the element to check (for logging or reporting purposes).
     * @param locator The locator string used to identify the element in the DOM.
     */
    public void isenabled(Page page, String element, String locator) {
        // Delegates the check of element's enabled state to the performAction method
        performAction("isenabled", page, element, locator, null);
    }

    /**
     * Checks if the given element is disabled on the current page.
     * This method uses the performAction method to perform the actual check.
     *
     * @param page    The current Playwright Page where the element resides.
     * @param element The logical name of the element to check (for logging or reporting purposes).
     * @param locator The locator string used to identify the element in the DOM.
     */
    public void isdisabled(Page page, String element, String locator) {
        // Delegates the check of element's disabled state to the performAction method
        performAction("isdisabled", page, element, locator, null);
    }

    /**
     * Checks if the given element is hidden on the current page.
     * This method uses the performAction method to perform the actual check.
     *
     * @param page    The current Playwright Page where the element resides.
     * @param element The logical name of the element to check (for logging or reporting purposes).
     * @param locator The locator string used to identify the element in the DOM.
     */
    public void ishidden(Page page, String element, String locator) {
        // Delegates the check of element's visibility state (hidden) to the performAction method
        performAction("ishidden", page, element, locator, null);
    }

    /**
     * Checks if the specified checkbox is currently checked.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the checkbox to check.
     * @param locator The locator string used to identify the checkbox.
     */
    public void ischecked(Page page, String element, String locator) {
        performAction("ischecked", page, element, locator, null); // Delegate checked status check to performAction method
    }

    /**
     * Retrieves the locator for the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to locate.
     * @param locator  The locator string used to identify the element.
     * @return The Locator object representing the specified element.
     */
    public Locator getElement_locator(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        return elementAction.getLocator(iFrame, iFrame_2, iFrame_3, element, locator, page, null); // Retrieve the locator for the specified element
    }

    /**
     * Checks if the specified element exists on the page.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check.
     * @param locator The locator string used to identify the element.
     */
    public void exists(Page page, String element, String locator) {
        performAction("exists", page, element, locator, null); // Delegate existence check to performAction method
    }

    /**
     * Checks if the specified element not_exists on the page.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check.
     * @param locator The locator string used to identify the element.
     */
    public void not_exists(Page page, String element, String locator) {
        Locator targetLocator = getElement_locator(page, null, null, null, element, locator);
        if (targetLocator.count() > 0) {
            performAction("not_exists", page, element, locator, null);
        }

    }

    /**
     * Right-clicks on the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being right-clicked.
     * @param locator The locator string used to identify the element.
     */
    public void rightclick(Page page, String element, String locator) {
        performAction("rightclick", page, element, locator, null); // Delegate right-click action to performAction method
    }

    /**
     * Taps on the specified element, primarily for mobile scenarios.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being tapped.
     * @param locator The locator string used to identify the element.
     */
    public void tap(Page page, String element, String locator) {
        performAction("tap", page, element, locator, null); // Delegate tap action to performAction method
    }

    /**
     * Uploads a file to a designated input element (type=file).
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the file input.
     * @param locator The locator string used to identify the input.
     * @param value   The path to the file to be uploaded.
     */
    public void uploadFile(Page page, String element, String locator, String value) {
        performAction("uploadfile", page, element, locator, value); // Delegate file upload action to performAction method
    }

    /**
     * Selects multiple options from a dropdown element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the dropdown.
     * @param locator The locator string used to identify the dropdown.
     */
    public void selectMultiple(Page page, String element, String locator) {
        performAction("selectmultiple", page, element, locator, null); // Delegate multiple selection to performAction method
    }

    /**
     * Retrieves the value of a specified attribute from an element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element for which to get the attribute.
     * @param locator The locator string used to identify the element.
     * @param value   The name of the attribute to be retrieved.
     */
    public void getAttribute(Page page, String element, String locator, String value) {
        performAction("getattribute", page, element, locator, value); // Delegate get attribute action to performAction method
    }

    /**
     * Sets a specified attribute on an element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element for which to set the attribute.
     * @param locator The locator string used to identify the element.
     * @param value   The value to set for the attribute.
     */
    public void setAttribute(Page page, String element, String locator, String value) {
        performAction("setattribute", page, element, locator, value); // Delegate set attribute action to performAction method
    }

    /**
     * Removes a specified attribute from an element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element from which to remove the attribute.
     * @param locator The locator string used to identify the element.
     * @param value   The name of the attribute to be removed.
     */
    public void removeAttribute(Page page, String element, String locator, String value) {
        performAction("removeattribute", page, element, locator, value); // Delegate remove attribute action to performAction method
    }

    /**
     * Evaluates JavaScript on the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element on which to evaluate the script.
     * @param locator The locator string used to identify the element.
     * @param value   The JavaScript code to be evaluated.
     */
    public void evaluate(Page page, String element, String locator, String value) {
        performAction("evaluate", page, element, locator, value); // Delegate evaluation action to performAction method
    }

    /**
     * Waits for a specified element to be present on the page.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to wait for.
     * @param locator The locator string used to identify the element.
     */
    public void waitForElement(Page page, String element, String locator) {
        performAction("waitForelement", page, element, locator, null); // Delegate waiting for element to performAction method
    }

    /**
     * Waits until a specified element reaches a certain state.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to wait for.
     * @param locator The locator string used to identify the element.
     */
    public void waitForState(Page page, String element, String locator) {
        performAction("waitforstate", page, element, locator, null); // Delegate waiting for state to performAction method
    }

    /**
     * Waits for specified text to appear in a particular element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check for text.
     * @param locator The locator string used to identify the element.
     * @param value   The text to wait for.
     */
    public void waitForText(Page page, String element, String locator, String value) {
        performAction("waitfortext", page, element, locator, value); // Delegate waiting for text appearance to performAction method
    }

    /**
     * Waits for a specified value in an element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check for the value.
     * @param locator The locator string used to identify the element.
     * @param value   The value to wait for.
     */
    public void waitForValue(Page page, String element, String locator, String value) {
        performAction("waitforvalue", page, element, locator, value); // Delegate waiting for value to performAction method
    }

    /**
     * Initiates a drag action on the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to drag.
     * @param locator The locator string used to identify the element.
     */
    public void dragStart(Page page, String element, String locator) {
        performAction("dragstart", page, element, locator, null); // Delegate drag start action to performAction method
    }

    /**
     * Ends a drag action on the specified element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to end dragging.
     * @param locator The locator string used to identify the element.
     */
    public void dragEnd(Page page, String element, String locator) {
        performAction("dragend", page, element, locator, null); // Delegate drag end action to performAction method
    }

    /**
     * Inputs a specified value into a designated element.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element for which the value is to be inputted.
     * @param locator The locator string used to identify the element.
     * @param value   The value to be inputted.
     */
    public void input(Page page, String element, String locator, String value) {
        performAction("input", page, element, locator, value); // Delegate input action to performAction method
    }

    /**
     * Selects a file for an input element of type=file.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the input element for file selection.
     * @param locator The locator string used to identify the input element.
     * @param value   The path to the file to be selected.
     */
    public void selectFile(Page page, String element, String locator, String value) {
        performAction("selectfile", page, element, locator, value); // Delegate file selection action to performAction method
    }

    /**
     * Checks if the specified element contains the given text.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check for text.
     * @param locator The locator string used to identify the element.
     * @param value   The text to check for.
     */
    public void hasText(Page page, String element, String locator, String value) {
        performAction("hastext", page, element, locator, value); // Delegate text presence check to performAction method
    }

    /**
     * Checks if the specified element has the given CSS class.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check.
     * @param locator The locator string used to identify the element.
     */
    public void hasclass(Page page, String element, String locator) {
        performAction("hasclass", page, element, locator, null); // Delegate class check to performAction method
    }

    /**
     * Compares the value of the specified element against an expected value.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element being compared.
     * @param locator The locator string used to identify the element.
     * @param value   The value to compare against.
     */
    public void hasEqualValue(Page page, String element, String locator, String value) {
        performAction("hasqualvalue", page, element, locator, value); // Delegate equality check to performAction method
    }

    /**
     * Checks if the specified element is empty.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the element to check.
     * @param locator The locator string used to identify the element.
     */
    public void isempty(Page page, String element, String locator) {
        performAction("isempty", page, element, locator, null); // Delegate emptiness check to performAction method
    }

    /**
     * Asserts that the specified element contains the expected text.
     *
     * @param page         The current Playwright Page.
     * @param element      The logical name of the element being checked.
     * @param locator      The locator string used to identify the element.
     * @param expectedText The expected text that should be contained within the element.
     */
    public void contain(Page page, String element, String locator, String expectedText) {
        performAction("hastext", page, element, locator, expectedText);
    }

    /**
     * Initiates a file chooser for the upload feature.
     *
     * @param page     The current Playwright Page.
     * @param fileName The name of the file to upload.
     * @param element  The logical name of the file input element.
     * @param locator  The locator string used to identify the file input.
     */
    public void file_chooser_for_upload(Page page, String fileName, String element, String locator) {
        elementAction.uploadFile(page, fileName, element, locator); // Utilize the element action to upload the specified file
    }

    /**
     * Clicks on a document link.
     *
     * @param page    The current Playwright Page.
     * @param element The logical name of the document link.
     * @param locator The locator string used to identify the document link.
     */
    public void click_document_link(Page page, String element, String locator) {
        elementAction.clickOnDocumentLinkName(page, element, locator); // Utilize the element action to click on the document link
    }

    /**
     * Centralized method to perform actions on elements,
     * managing success and failure scenarios.
     *
     * @param action  The action to perform (e.g., "click", "fill").
     * @param page    The current Playwright Page.
     * @param element The logical name of the element involved in the action.
     * @param locator The locator string used to identify the element.
     * @param value   Any additional value needed for the action, if required.
     */
    private void performAction(String action, Page page, String element, String locator, String value) {
        executeStep(() -> {
            // Execute the action specified on the given element and check for success
            boolean actionStatus = elementAction.performActionPage(page, action, element, locator, value);
            if (!actionStatus) {
                handleFailure(page, action, element); // Handle the failure if the action was unsuccessful
            }
        });
    }

    /**
     * Executes an action and returns its string result if applicable.
     * Does not affect the existing performAction method logic.
     *
     * @param action  The action to perform (e.g., "gettext", "getvalue")
     * @param page    The current Playwright Page
     * @param element The logical name of the element involved
     * @param locator The locator string used to identify the element
     * @param value   Any additional value needed for the action
     * @return The string result of the action, or null if not applicable
     */
    private String getStringValue(String action, Page page, String element, String locator, String value) {
        final String[] result = {null};
        executeStep(() -> {
            // Perform the action and capture the result
            result[0] = elementAction.performActionPageWithReturn(page, action, element, locator, value);

            // Handle failure if result is expected but null
            if (result[0] == null && actionRequiresResult(action)) {
                handleFailure(page, action, element);
            } else if (result[0] != null && !result[0].isEmpty()) {
                logger.info("Action '{}' returned result: {}", action, result[0]);
            }
        });
        return result[0];
    }

    /**
     * Determines whether the specified action is expected to return a result.
     * Used to differentiate between validation/assertion actions and void-type actions.
     *
     * @param action The name of the action.
     * @return true if the action typically returns a value and should not be null.
     */
    private boolean actionRequiresResult(String action) {
        switch (action.toLowerCase()) {
            case "gettext":
            case "getvalue":
            case "getattribute":
            case "hastext":
            case "hasclass":
            case "hasequalvalue":
            case "isempty":
            case "isvisible":
            case "isenabled":
            case "ischecked":
            case "isdisabled":
            case "exists":
            case "not_exists":
            case "waitfortext":
            case "waitforvalue":
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the specified element has the expected value.
     *
     * <p>
     * This method interacts with the specified web element, identified by the
     * provided locator, to verify if its current value matches the expected value.
     * This is useful for assertions in test scenarios where the correctness of
     * input fields or other value-bearing elements needs to be validated.
     * </p>
     *
     * @param page    The current Playwright Page instance used to interact with the browser.
     * @param element The logical name of the element being checked, for logging purposes.
     * @param locator The locator string used to identify the element on the page.
     * @param value   The expected value that the element should have.
     */
    public void hasvalue(Page page, String element, String locator, String value) {
        performAction("hasvalue", page, element, locator, value); // Execute the action checking for the expected value
    }

    /**
     * Retrieves the current value of the specified element.
     *
     * <p>
     * This method interacts with the element identified by the locator and
     * retrieves its current value. This can be useful in scenarios where the
     * current state of a field (for example, after a user action) needs to be
     * checked or stored for further verification.
     * </p>
     *
     * @param page    The current Playwright Page instance used to interact with the browser.
     * @param element The logical name of the element whose value is being retrieved, for logging purposes.
     * @param locator The locator string used to identify the element on the page.
     * @return
     */
    public String getvalue(Page page, String element, String locator) {
        performAction("getvalue", page, element, locator, null); // Delegate the action to retrieve the value to performAction method
        return element;
    }

    /**
     * Retrieves and prints a list of elements on the specified page.
     * This method locates elements based on the provided element name and locator key,
     * and prints each located element's details for debugging or verification purposes.
     *
     * @param page    The Playwright Page object representing the current browser page.
     * @param element The name of the element to locate, typically corresponding to a descriptive identifier.
     * @param locator The locator key, as specified in the YAML configuration, used to retrieve the element.
     */
    public void getListOfElements(Page page, String element, String locator) {
        // Retrieve a list of element handles matching the provided element and locator key on the page.
        List<ElementHandle> elements = elementAction.getElementHandleList(page, element, locator, null);

        // Check if elements list is empty and log appropriate information
        if (elements.isEmpty()) {
            logger.info("No elements found for the specified locator."); // Log that no elements were found
        } else {
            // Iterate through each element in the list and print its details on a new line.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < elements.size(); i++) {
                ElementHandle handle = elements.get(i);
                sb.append("Element ").append(i + 1).append(": ").append(handle.toString()).append("\n");
            }
            logger.info(sb.toString().trim()); // Trim to remove the last extra newline
        }
    }


    /**
     * Attempts to click a radio button within a list of elements on a specified page.
     * This method retrieves a list of element handles matching the provided element name and locator key,
     * then iterates through each radio button in the list until it finds an enabled one to click.
     *
     * @param page    The Playwright Page object representing the current browser page.
     * @param element The name of the element to locate, typically corresponding to a descriptive identifier.
     * @param locator The locator key, as specified in the YAML configuration, used to retrieve the element.
     */
    public void clickRadioButton(Page page, String element, String locator) {
        // Retrieve a list of element handles matching the provided element and locator key on the page.
        List<ElementHandle> elements = elementAction.getElementHandleList(page, element, locator, null);

        // Iterate through the list of element handles.
        for (ElementHandle radioButton : elements) {
            // Check if the current radio button is enabled and, if so, select it.
            if (radioButton.isEnabled()) {
                radioButton.check();  // Selects (clicks) the radio button
                logger.info("Radio button clicked!");  // Logs the successful selection
                break;  // Exit the loop once a radio button is successfully selected
            } else {
                // If the radio button is not enabled, log the information and continue to the next element.
                logger.info("Radio button is not enabled, moving to the next one.");
            }
        }
    }

    /**
     * Executes a step while monitoring for exceptions and
     * logging failures appropriately.
     *
     * @param step A Runnable representing the action to be executed.
     */
    private void executeStep(Runnable step) {
        if (isFailed) {
            // Skip execution of further steps if a previous one has failed
            return; // Exit if a failure has already occurred to prevent cascading failures
        }
        try {
            step.run(); // Execute the provided step
        } catch (Exception e) {
            isFailed = true; // Mark the failure
            logger.error("Step execution failed: {}", e.getMessage(), e); // Log the error with details
            handleFailure(page, "Step execution failed", null); // Handle failure cleanly by performing cleanup and logging
        }
    }

    /**
     * Handles a failure during execution by logging errors and
     * triggering cleanup measures.
     *
     * @param page    The current Playwright Page.
     * @param action  The action that failed.
     * @param element The logical name of the element involved.
     */
    private void handleFailure(Page page, String action, String element) {
        isFailed = true; // Update internal state to indicate failure
        logger.error("Action '{}' failed on element '{}'", action, element); // Log detailed failure
        ScreenshotHandler.handleScenarioTeardown(getCurrentScenario(), page, "Failure Step"); // Clean up for the scenario
        closeBrowserOnFailure(); // Attempt to close resources on failure
        throw new RuntimeException(String.format("Action '%s' failed on element '%s', skipping further steps", action, element)); // Provide feedback on failure
    }

    /**
     * Closes the page and the browser if a failure occurs.
     */
    private void closeBrowserOnFailure() {
        try {
            if (page != null && !page.isClosed()) {
                page.close(); // Attempt to close the page
                logger.info("Page closed due to failure."); // Log closure of page
            }
        } catch (Exception e) {
            logger.error("Error closing the page: {}", e.getMessage(), e); // Log any errors during closure
        }

        try {
            if (Hooks.getBrowser() != null) {
                Hooks.getBrowser().close(); // Attempt to close the browser instance
                logger.info("Browser closed due to failure."); // Log closure of browser
            }
        } catch (Exception e) {
            logger.error("Error closing the browser: {}", e.getMessage(), e); // Log any errors during closure
        }
    }

    /**
     * Stores the current Cucumber scenario in a thread-local variable.
     *
     * @param scenario The current Cucumber scenario.
     */
    public static void setCurrentScenario(Scenario scenario) {
        currentScenario.set(scenario); // Save the scenario context for later retrieval
    }

    /**
     * Retrieves the current Cucumber scenario.
     *
     * @return The current Cucumber scenario.
     */
    private Scenario getCurrentScenario() {
        return Hooks.getCurrentScenario(); // Access the saved scenario for context management
    }

    /**
     * Finalizes the scenario. Captures a screenshot if all steps have passed.
     * This method should be called at the completion of a scenario.
     */
    public void finalizeScenario() {
        if (isFailed) {
            // Handle the scenario teardown if a step has failed
            ScreenshotHandler.handleScenarioTeardown(getCurrentScenario(), page, "Passed Step");
        }
    }

    /**
     * Finalizes the current scenario and captures a screenshot if no failures occurred.
     * <p>
     * This method checks the status of the scenario being executed.
     * If the scenario has not failed, it will invoke the utility method
     * to capture a screenshot of the specified element or area represented
     * by the given target locator.
     *
     * @param page          The current Playwright Page for which to finalize the scenario.
     * @param targetLocator A string representing the locator of the target element
     *                      from which to capture the screenshot.
     */
    public void finalizeScenarioScreenshot(Page page, String targetLocator) {
        // Check if the scenario did not encounter any failures during execution
        if (!isFailed) {
            // Handle the scenario teardown process for a passed step by invoking
            // the utility method to capture a screenshot and attach it to the report.
            ScreenshotHandler.handleScenarioTeardownLocator(
                    getCurrentScenario(),  // Retrieve the current scenario context
                    page,                  // The Playwright Page instance providing context for the screenshot capture
                    null,                  // No iframe specified, as we are capturing from the main page context
                    null,                  // No second iframe specified
                    null,                  // No third iframe specified
                    targetLocator,         // The locator of the target element for the screenshot
                    "Passed Step"          // Status string indicating the scenario has passed successfully
            );
        }
    }
}