package com.ptaf.ui.pages;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.ptaf.ui.action_performer.ElementActionImpl;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.interfaces.ElementAction;
import com.ptaf.utils.ScreenshotHandler;
import io.cucumber.java.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

/**
 * <h1>FrameCommonMethods</h1>
 *
 * <p>
 * The <code>FrameCommonMethods</code> class provides utility methods for
 * interacting with elements within iframes in a Playwright-based test automation
 * framework. This class encapsulates common actions such as clicking,
 * filling inputs, selecting options, and more, specifically targeting elements within
 * one or more nested iframes to facilitate effective test automation.
 * </p>
 *
 * <p>
 * The class uses an <code>ElementAction</code> instance to perform interactions
 * with these web elements based on the provided locators. It incorporates
 * robust error management to log failures and continue executing subsequent tests
 * smoothly. It also provides functionality for taking screenshots and
 * capturing logs as needed.
 * </p>
 *
 * <p>
 * Notes for testers:
 * - Methods accept optional iframe identifiers (iFrame, iFrame_2, iFrame_3). If an element is not inside an iframe,
 *   pass null for those parameters.
 * - Many operations will throw a RuntimeException via handleFailure when they fail; tests should expect that
 *   to mark the step/scenario as failed and stop further steps in the same scenario instance.
 * - Screenshot and teardown are coordinated with the Scenario context managed by Hooks and ScreenshotHandler.
 * </p>
 */
public class FrameCommonMethods {
    /**
     * The Playwright Page instance used for all page-level interactions in this class.
     * This instance is provided at construction time and should represent the active browser page.
     */
    private final Page page; // The Playwright Page instance to interact with the browser

    /**
     * ElementAction is an abstraction that encapsulates low-level interactions (click, fill, evaluate, etc.).
     * We use an implementation of this interface (ElementActionImpl) to apply actions in the context of frames.
     */
    private final ElementAction elementAction; // Interface instance for handling element actions

    /**
     * Flag indicating whether any prior step in this FrameCommonMethods instance has failed.
     * Once set to true, subsequent executeStep calls will be skipped to avoid cascading errors.
     */
    private boolean isFailed = false; // Flag to indicate if any action has failed

    /**
     * Thread-local storage for the current Cucumber Scenario. This is used by screenshot/teardown helpers
     * to attach artifacts (screenshots/logs) to the correct scenario in multi-threaded test execution.
     */
    private static final ThreadLocal<Scenario> currentScenario = new ThreadLocal<>(); // Thread-local variable for the current Cucumber scenario

    /**
     * Logger instance for this class; used to record debug, info and error messages to aid troubleshooting.
     */
    private static final Logger logger = LoggerFactory.getLogger(FrameCommonMethods.class); // Logger for logging events

    /**
     * Initializes an instance of <code>FrameCommonMethods</code> with a specified Playwright page.
     *
     * @param page The Playwright Page instance to interact with.
     */
    public FrameCommonMethods(Page page) {
        this.page = page; // Assign the provided page instance
        // Initialize the ElementAction implementation which will execute operations in page/frame context.
        // ElementActionImpl encapsulates the actual Playwright interaction logic for all supported actions.
        this.elementAction = new ElementActionImpl(page); // Initialize the element action instance for performing actions
    }

    /**
     * Clicks on an element within specified iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to be clicked.
     * @param locator  The locator string used to identify the element.
     */
    public void click(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("click", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform click action
    }

    /**
     * Fills an input field within specified iframes with the provided value.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the input element.
     * @param locator  The locator string used to identify the input element.
     * @param value    The value to be filled in the input field.
     */
    public void fill(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("fill", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform fill action
    }

    /**
     * Selects an option from a dropdown within specified iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the dropdown element.
     * @param locator  The locator string used to identify the dropdown.
     * @param value    The value of the option to be selected.
     */
    public void select(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("select", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform select action
    }

    /**
     * Checks a checkbox element within specified iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the checkbox element.
     * @param locator  The locator string used to identify the checkbox.
     */
    public void check(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("check", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform check action
    }

    /**
     * Unchecks a checkbox element within specified iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the checkbox element.
     * @param locator  The locator string used to identify the checkbox.
     */
    public void uncheck(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("uncheck", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform uncheck action
    }

    /**
     * Hovers over a specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to hover.
     * @param locator  The locator string used to identify the element.
     */
    public void hover(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("hover", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform hover action
    }

    /**
     * Types a specified value into a designated input field within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the input element.
     * @param locator  The locator string used to identify the input field.
     * @param value    The value to be typed into the input field.
     */
    public void type(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("type", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform type action
    }

    /**
     * Presses a specified key on the targeted element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element.
     * @param locator  The locator string used to identify the element.
     * @param value    The key to be pressed.
     */
    public void press(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("press", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform press action
    }

    /**
     * Double-clicks on the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to double-click.
     * @param locator  The locator string used to identify the element.
     */
    public void dblclick(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("dblclick", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform double-click action
    }

    /**
     * Takes a screenshot of the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to screenshot.
     * @param locator  The locator string used to identify the element.
     * @param value    Additional parameters for the screenshot, if any.
     */
    public void screenshot(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        // Compute the exact locator string so that finalizeScenario can use it for targeted screenshots.
        String targetLocator = elementAction.getExactLocator(element, locator);
        performAction("screenshot", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform screenshot action
        // After performing the screenshot step, run finalize to attach screenshot and finalize scenario if passed.
        finalizeScenario(page, iFrame, iFrame_2, iFrame_3, targetLocator); // Finalize the scenario by handling teardown
    }

    /**
     * Initiates a file download from a web page and saves it to a specified location.
     *
     * @param page     Playwright Page object representing the browser page.
     * @param iFrame   First-level iFrame identifier (can be null if not applicable).
     * @param iFrame_2 Second-level iFrame identifier (nested frame, if applicable).
     * @param iFrame_3 Third-level iFrame identifier (deepest nested frame, if applicable).
     * @param element  The element name as defined in locator configuration (e.g., YAML).
     * @param locator  The type of locator to be used (e.g., XPATH, CSS, etc.).
     * @param value    The directory path where the downloaded file should be saved.
     * @param name     A custom suffix or name to append to the downloaded file.
     */
    public void download(Page page, String iFrame, String iFrame_2, String iFrame_3,
                         String element, String locator, String value, String name) {

        // Start waiting for a download event after the download-triggering action is performed.
        // page.waitForDownload registers a listener and executes the provided callback (click) in the same context,
        // returning the resulting Download object once the file starts or completes downloading (Playwright controls lifecycle).
        Download download = page.waitForDownload(() -> {
            // Perform the click action to initiate the download from the specified element.
            // This will use the same frame resolution mechanism as other actions.
            click(page, iFrame, iFrame_2, iFrame_3, element, locator);
        });

        // Once the download is complete or available, save the file to the specified path.
        // The filename chosen here is the suggested filename from the response appended with the provided custom name.
        download.saveAs(Paths.get(value, download.suggestedFilename() + name));
    }


    /**
     * Scrolls the page to the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to scroll to.
     * @param locator  The locator string used to identify the element.
     */
    public void scroll(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("scroll", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform scroll action
    }

    /**
     * Focuses on the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to focus on.
     * @param locator  The locator string used to identify the element.
     */
    public void focus(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("focus", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform focus action
    }

    /**
     * Removes focus from the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to blur.
     * @param locator  The locator string used to identify the element.
     * @param value    Additional parameters for blurring, if any.
     */
    public void blur(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("blur", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform blur action
    }

    /**
     * Clears the value of an input or element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to clear.
     * @param locator  The locator string used to identify the element.
     */
    public void clear(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("clear", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform clear action
    }

    /**
     * Drags the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to drag.
     * @param locator  The locator string used to identify the element.
     */
    public void drag(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("drag", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform drag action
    }

    /**
     * Gets the text content of the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to extract text from.
     * @param locator  The locator string used to identify the element.
     * @return The retrieved text value or null if not available.
     */
    public String gettext(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        String value = getStringValue("gettext", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform action to get text
        if (value == null || value.trim().isEmpty()) {
            // Helpful console output for quick debugging when text is expected but not found.
            System.out.println("There is no text value for element: " + element + ", locator: " + locator);
        }

        return value;
    }


    /**
     * Gets the text content of the specified element within nested iframes and contains with found text content with locator value.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to extract text from.
     * @param locator  The locator string used to identify the element.
     */
    public void get_and_contain_text(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("get_and_contain_text", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform action to get text
    }

    /**
     * Checks whether the specified element is visible within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check for visibility.
     * @param locator  The locator string used to identify the element.
     */
    public void isvisible(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("isvisible", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Check visibility of the element
    }

    /**
     * Checks if the specified element is enabled within nested iframes.
     * This method delegates the actual check to the performAction method.
     *
     * @param page     The current Playwright Page where the element resides.
     * @param iFrame   The identifier for the outer iframe containing the target element.
     * @param iFrame_2 The identifier for the second (nested) iframe containing the target element.
     * @param iFrame_3 The identifier for the third (further nested) iframe containing the target element.
     * @param element  The logical name of the element to check (for reporting or logging purposes).
     * @param locator  The locator string used to uniquely identify the element in the DOM.
     */
    public void isenabled(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        // Delegates the check of element's enabled state within nested iframes to the performAction method
        performAction("isenabled", page, iFrame, iFrame_2, iFrame_3, element, locator, null);
    }

    /**
     * Checks if the specified element is disabled within nested iframes.
     * This method delegates the actual check to the performAction method.
     *
     * @param page     The current Playwright Page where the element resides.
     * @param iFrame   The identifier for the outer iframe containing the target element.
     * @param iFrame_2 The identifier for the second (nested) iframe containing the target element.
     * @param iFrame_3 The identifier for the third (further nested) iframe containing the target element.
     * @param element  The logical name of the element to check (for reporting or logging purposes).
     * @param locator  The locator string used to uniquely identify the element in the DOM.
     */
    public void isdisabled(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        // Delegates the check of element's disabled state within nested iframes to the performAction method
        performAction("isdisabled", page, iFrame, iFrame_2, iFrame_3, element, locator, null);
    }

    /**
     * Checks if the specified element is hidden within nested iframes.
     * This method delegates the actual check to the performAction method.
     *
     * @param page     The current Playwright Page where the element resides.
     * @param iFrame   The identifier for the outer iframe containing the target element.
     * @param iFrame_2 The identifier for the second (nested) iframe containing the target element.
     * @param iFrame_3 The identifier for the third (further nested) iframe containing the target element.
     * @param element  The logical name of the element to check (for reporting or logging purposes).
     * @param locator  The locator string used to uniquely identify the element in the DOM.
     */
    public void ishidden(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        // Delegates the check of element's visibility state (hidden) within nested iframes to the performAction method
        performAction("ishidden", page, iFrame, iFrame_2, iFrame_3, element, locator, null);
    }

    /**
     * Checks if the specified checkbox is currently checked within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the checkbox to check.
     * @param locator  The locator string used to identify the checkbox.
     */
    public void ischecked(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("ischecked", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Check if the checkbox is checked
    }

    /**
     * Checks if the specified element exists within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check for existence.
     * @param locator  The locator string used to identify the element.
     */
    public void exists(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("exists", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Check for existence of the element
    }

    /**
     * Checks if the specified element not_exists within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check for existence.
     * @param locator  The locator string used to identify the element.
     */
    public void not_exists(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        // First retrieve the locator using the standard frame resolution so we know how many matched elements exist.
        Locator targetLocator = getElement_locator(page, iFrame, iFrame_2, iFrame_3, element, locator);
        // If there are any matches, invoke the not_exists action to validate non-existence (or to log/handle as per implementation).
        if (targetLocator.count() > 0) {
            performAction("not_exists", page, iFrame, iFrame_2, iFrame_3, element, locator, null);
        }
    }


    /**
     * Right-clicks on a specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to right-click.
     * @param locator  The locator string used to identify the element.
     */
    public void rightclick(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("rightclick", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform right-click action
    }

    /**
     * Taps on a specified element, primarily for mobile scenarios, within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to tap.
     * @param locator  The locator string used to identify the element.
     */
    public void tap(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("tap", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform tap action
    }

    /**
     * Uploads a file to a designated input element (type=file) within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the file input.
     * @param locator  The locator string used to identify the input.
     * @param value    The path to the file to be uploaded.
     */
    public void uploadFile(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("uploadfile", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform file upload action
    }

    /**
     * Selects multiple options from a dropdown element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element for multiple selection.
     * @param locator  The locator string used to identify the dropdown.
     */
    public void selectMultiple(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("selectmultiple", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform multiple select action
    }

    /**
     * Retrieves the value of a specified attribute from an element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element for which to get the attribute.
     * @param locator  The locator string used to identify the element.
     * @param value    The name of the attribute to be retrieved.
     */
    public void getAttribute(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("getattribute", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform get attribute action
    }

    /**
     * Sets a specified attribute on an element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element for which to set the attribute.
     * @param locator  The locator string used to identify the element.
     * @param value    The value to set for the attribute.
     */
    public void setAttribute(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("setattribute", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform set attribute action
    }

    /**
     * Removes a specified attribute from an element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element from which to remove the attribute.
     * @param locator  The locator string used to identify the element.
     * @param value    The name of the attribute to be removed.
     */
    public void removeAttribute(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("removeattribute", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform remove attribute action
    }

    /**
     * Evaluates JavaScript on the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element on which to evaluate the script.
     * @param locator  The locator string used to identify the element.
     * @param value    The JavaScript code to be executed.
     */
    public void evaluate(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("evaluate", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform evaluate action
    }

    /**
     * Waits for a specified element to be present within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to wait for.
     * @param locator  The locator string used to identify the element.
     */
    public void waitForElement(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("waitForelement", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform wait for element action
    }

    /**
     * Waits for the specified element to reach a certain state within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to wait for.
     * @param locator  The locator string used to identify the element.
     */
    public void waitForState(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("waitforstate", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform wait for state action
    }

    /**
     * Waits for specified text to appear in a particular element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check for text.
     * @param locator  The locator string used to identify the element.
     * @param value    The text to wait for.
     */
    public void waitForText(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("waitfortext", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform wait for text action
    }

    /**
     * Waits for a specified value in an element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check for the value.
     * @param locator  The locator string used to identify the element.
     * @param value    The value to wait for.
     */
    public void waitForValue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("waitforvalue", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform wait for value action
    }

    /**
     * Initiates a drag action on the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to drag.
     * @param locator  The locator string used to identify the element.
     */
    public void dragStart(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("dragstart", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform drag start action
    }

    /**
     * Ends a drag action on the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to end dragging.
     * @param locator  The locator string used to identify the element.
     */
    public void dragEnd(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("dragend", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform drag end action
    }

    /**
     * Inputs a specified value into a designated element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to input value.
     * @param locator  The locator string used to identify the element.
     * @param value    The value to be inputted.
     */
    public void input(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("input", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform input action
    }

    /**
     * Selects a file for an input element of type=file within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the input element for file selection.
     * @param locator  The locator string used to identify the input element.
     * @param value    The path to the file to be selected.
     */
    public void selectFile(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("selectfile", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform file selection action
    }

    /**
     * Checks if the specified element contains the given text within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check for text.
     * @param locator  The locator string used to identify the element.
     * @param value    The expected text to check against.
     */
    public void hasText(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("hastext", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform action to check for text
    }

    /**
     * Checks if the specified element has the given CSS class within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check.
     * @param locator  The locator string used to identify the element.
     */
    public void hasclass(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("hasclass", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform action to check for class presence
    }

    /**
     * Compares the value of the specified element against an expected value within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element being compared.
     * @param locator  The locator string used to identify the element.
     * @param value    The expected value to compare against.
     */
    public void hasEqualValue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("hasqualvalue", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform value equality check
    }

    /**
     * Checks if the specified element is empty within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element to check.
     * @param locator  The locator string used to identify the element.
     */
    public void isempty(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("isempty", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Check if the element is empty
    }

    /**
     * Checks if the specified element has the expected value within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element being checked.
     * @param locator  The locator string used to identify the element.
     * @param value    The expected value that the element should hold.
     */
    public void hasvalue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        performAction("hasvalue", page, iFrame, iFrame_2, iFrame_3, element, locator, value); // Perform action to check for value
    }

    /**
     * Retrieves the current value of the specified element within nested iframes.
     *
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element whose value is being retrieved.
     * @param locator  The locator string used to identify the element.
     */
    public void getvalue(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        performAction("getvalue", page, iFrame, iFrame_2, iFrame_3, element, locator, null); // Perform action to get the value
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
        // elementAction.getLocator resolves the element using provided iFrame identifiers and returns a Playwright Locator.
        return elementAction.getLocator(iFrame, iFrame_2, iFrame_3, element, locator, page, null); // Retrieve the locator for the specified element
    }

    /**
     * Returns the input value (value attribute) of a given element resolved within the specified frames.
     *
     * @param page     The current Playwright page instance.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the nested iframe.
     * @param element  The logical name of the element to read.
     * @param locator  The locator key used to find the element.
     * @return The string value from the input element (equivalent to element.inputValue()).
     */
    public String get_frame_element_string_value(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator) {
        // Resolve the locator in the context of frames and read its inputValue (suitable for <input> elements).
        Locator final_locator = getElement_locator(page, iFrame, iFrame_2, iFrame_3, element, locator);
        return final_locator.inputValue();
    }

    /**
     * Asserts that the specified element contains the expected text.
     *
     * @param page         The current Playwright Page.
     * @param iFrame       The identifier for the outer iframe.
     * @param element      The logical name of the element being checked.
     * @param locator      The locator string used to identify the element.
     * @param expectedText The expected text that should be contained within the element.
     */
    public void contain(Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String expectedText) {
        performAction("hastext", page, iFrame, iFrame_2, iFrame_3, element, locator, expectedText);
    }

    /**
     * Centralized method to perform actions on elements within nested iframes,
     * managing success and failure scenarios.
     *
     * <p>
     * This method:
     * - Wraps the action invocation in executeStep which prevents subsequent steps if a failure occurred.
     * - Calls elementAction.performActionPageFrame(...) which executes the actual Playwright operations.
     * - Interprets the boolean return to determine success/failure and invokes handleFailure on failure.
     * </p>
     *
     * @param action   The action to be performed (e.g., "click", "fill").
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element involved in the action.
     * @param locator  The locator string used to identify the element.
     * @param value    Any additional value needed for the action, if required.
     */
    private void performAction(String action, Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        executeStep(() -> {
            // Execute the action via the ElementAction abstraction. This returns true/false to indicate success.
            boolean actionStatus = elementAction.performActionPageFrame(page, iFrame, iFrame_2, iFrame_3, action, element, locator, value, null); // Execute action and check status
            if (!actionStatus) {
                // If the action failed, perform standard failure handling (logging, screenshot, teardown).
                handleFailure(page, action, element); // Handle failure if action is unsuccessful
            }
        });
    }

    /**
     * Executes an action and returns its string result if applicable.
     * Does not affect the existing performAction method logic.
     *
     * <p>
     * This method is used for actions that are expected to return a String (for example, gettext).
     * It stores the result in a one-element array (to allow assignment from inside a lambda).
     * If the result is null for actions that should return a value, the failure handler is invoked.
     * </p>
     *
     * @param action   The action to be performed (e.g., "click", "fill").
     * @param page     The current Playwright Page.
     * @param iFrame   The identifier for the outer iframe.
     * @param iFrame_2 The identifier for the nested iframe.
     * @param iFrame_3 The identifier for the further nested iframe.
     * @param element  The logical name of the element involved in the action.
     * @param locator  The locator string used to identify the element.
     * @param value    Any additional value needed for the action, if required.
     * @return The string result returned by the action or null if none.
     */
    private String getStringValue(String action, Page page, String iFrame, String iFrame_2, String iFrame_3, String element, String locator, String value) {
        final String[] result = {null};
        executeStep(() -> {
            // Perform the action and capture the result
            result[0] = elementAction.performActionPageFrameWithReturn(page, iFrame, iFrame_2, iFrame_3, action, element, locator, value, null);

            // Handle failure if result is expected but null
            if (result[0] == null && actionRequiresResult(action)) {
                handleFailure(page, action, element);
            } else if (result[0] != null && !result[0].isEmpty()) {
                // Log retrieved non-empty result for traceability.
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
     * Retrieves and prints a list of elements on the specified page within nested iframes.
     * This method checks the specified locator and prints each located element's details for debugging purposes.
     *
     * @param page    The Playwright Page object representing the current browser page.
     * @param iFrame  The identifier for the outer iframe.
     * @param element The name of the element to locate.
     * @param locator The locator key used to retrieve the element.
     */
    public void getListOfElements(Page page, String iFrame, String element, String locator) {
        // Retrieve a list of element handles matching the provided element and locator.
        List<ElementHandle> elements = elementAction.getElementHandleList(page, element, locator, null);

        // Check if the elements list is empty and log appropriate messages.
        if (elements.isEmpty()) {
            logger.info("No elements found for the specified locator."); // Log information for no elements found
        } else {
            // Iterate through each element in the list and print details.
            for (int i = 0; i < elements.size(); i++) {
                ElementHandle handle = elements.get(i);
                // Log details of each found element; ElementHandle.toString() is a lightweight representation useful for debugging.
                logger.info("Element " + (i + 1) + ": " + handle.toString()); // Log details of each found element
            }
        }
    }

    /**
     * Unchecks a radio button from a list of elements within nested iframes.
     * This method retrieves the elements matching the provided name and locator,
     * and unchecked the first enabled radio button found.
     *
     * @param page    The current Playwright Page.
     * @param iFrame  The identifier for the outer iframe.
     * @param element The logical name of the radio button to uncheck.
     * @param locator The locator string used to identify the radio button.
     */
    public void uncheckRadioButton(Page page, String iFrame, String element, String locator) {
        // Retrieve the list of radio button elements within the specified iframes.
        List<ElementHandle> elements = elementAction.getElementHandleList(page, element, locator, null);

        // Iterate through the elements to find and uncheck an enabled radio button.
        for (ElementHandle radioButton : elements) {
            if (radioButton.isEnabled()) {
                radioButton.uncheck(); // Uncheck the radio button
                logger.info("Radio button unchecked!"); // Log the successful unchecking
                break; // Exit loop after the first unchecked radio button
            } else {
                logger.info("Radio button is not enabled, moving to the next one."); // Log information for non-enabled radio button
            }
        }
    }

    /**
     * Clicks a radio button from a list of elements within nested iframes.
     * This method retrieves the elements matching the provided name and locator,
     * and checks the first enabled radio button found.
     *
     * @param page    The current Playwright Page.
     * @param iFrame  The identifier for the outer iframe.
     * @param element The logical name of the radio button to click.
     * @param locator The locator string used to identify the radio button.
     */
    public void clickRadioButton(Page page, String iFrame, String element, String locator) {
        // Retrieve the list of radio button elements within the specified iframes.
        List<ElementHandle> elements = elementAction.getElementHandleList(page, element, locator, null);

        // Iterate through the elements to find and check an enabled radio button.
        for (ElementHandle radioButton : elements) {
            if (radioButton.isEnabled()) {
                radioButton.check(); // Check the radio button
                logger.info("Radio button clicked!"); // Log the successful click
                break; // Exit loop after the first checked radio button
            } else {
                logger.info("Radio button is not enabled, moving to the next one."); // Log information for non-enabled radio button
            }
        }
    }

    /**
     * Executes a step while monitoring for exceptions and
     * logging failures appropriately.
     *
     * <p>
     * This wrapper ensures that:
     * - If a previous step has failed (isFailed == true), subsequent steps are skipped.
     * - Runtime exceptions are converted into a handled failure flow: mark failure, log, perform teardown.
     * </p>
     *
     * @param step A Runnable representing the action to be executed.
     */
    private void executeStep(Runnable step) {
        if (isFailed) {
            // Skip execution of further steps if a previous one has failed
            return; // Exit if a failure has already occurred to prevent cascading failures
        }
        try {
            step.run(); // Execute the provided step action
        } catch (Exception e) {
            // Any unexpected exception triggers the common failure handling pipeline.
            isFailed = true; // Mark the test as failed
            logger.error("Step execution failed: {}", e.getMessage(), e); // Log exception details
            handleFailure(page, "Step execution failed", null); // Handle failure cleanly by logging and performing cleanup
        }
    }

    /**
     * Handles a failure during execution by logging errors and
     * triggering cleanup measures.
     *
     * <p>
     * Failure handling includes:
     * - Setting internal failure flag
     * - Logging a clear error message
     * - Capturing a screenshot and attaching it to the current scenario via ScreenshotHandler
     * - Closing page and browser resources to avoid leaking sessions
     * - Throwing a RuntimeException to stop further processing in the current test flow
     * </p>
     *
     * @param page    The current Playwright Page.
     * @param action  The action that failed.
     * @param element The logical name of the element involved.
     */
    private void handleFailure(Page page, String action, String element) {
        isFailed = true; // Update internal state to indicate failure
        logger.error("Action '{}' failed on element '{}'", action, element); // Log details of the failure
        // Attach failure artifacts (screenshot) to the current scenario and perform any scenario-level teardown.
        ScreenshotHandler.handleScenarioTeardown(getCurrentScenario(), page, "Failure Step"); // Cleanup the scenario context
        closeBrowserOnFailure(); // Attempt to close any resources on failure
        // Throw a runtime exception so upper layers (Cucumber step runner) can mark the step as failed and stop execution.
        throw new RuntimeException(String.format("Action '%s' failed on element '%s', skipping further steps", action, element)); // Throw exception to indicate failure
    }

    /**
     * Closes the page and the browser if a failure occurs.
     *
     * <p>
     * This method attempts to gracefully close page and browser resources; exceptions during closure
     * are logged but do not interrupt the failure flow.
     * </p>
     */
    private void closeBrowserOnFailure() {
        try {
            if (page != null && !page.isClosed()) {
                page.close(); // Attempt to close the page
                logger.info("Page closed due to failure."); // Log that the page has been closed due to failure
            }
        } catch (Exception e) {
            // Log but suppress exceptions during resource cleanup.
            logger.error("Error closing the page: {}", e.getMessage(), e); // Log any errors encountered during closure
        }

        try {
            if (Hooks.getBrowser() != null) {
                Hooks.getBrowser().close(); // Attempt to close the browser instance
                logger.info("Browser closed due to failure."); // Log closure of browser
            }
        } catch (Exception e) {
            // Log but suppress exceptions during resource cleanup.
            logger.error("Error closing the browser: {}", e.getMessage(), e); // Log any errors encountered during closure
        }
    }

    /**
     * Stores the current Cucumber scenario in a thread-local variable.
     *
     * <p>
     * Test harness (Hooks) should set the active scenario for the current thread so that
     * screenshot and reporting utilities can attach artifacts to the correct scenario.
     * </p>
     *
     * @param scenario The current Cucumber scenario.
     */
    public static void setCurrentScenario(Scenario scenario) {
        currentScenario.set(scenario); // Save the scenario context
    }

    /**
     * Retrieves the current Cucumber scenario.
     *
     * <p>
     * Implementation uses Hooks.getCurrentScenario() as the canonical source for scenario state.
     * Keeping this method private prevents external modification while allowing internal utilities to read it.
     * </p>
     *
     * @return The current Cucumber scenario.
     */
    private Scenario getCurrentScenario() {
        return Hooks.getCurrentScenario(); // Access the saved scenario context
    }

    /**
     * Finalizes the scenario by performing necessary cleanup actions and capturing
     * screenshots if no failures occurred during the scenario execution.
     * <p>
     * This method checks if the scenario has completed successfully and, if so,
     * it invokes a utility method to handle the teardown process, which may include
     * taking screenshots of the specified element(s) and performing any necessary cleanup.
     *
     * @param page          The current Playwright Page instance for which the scenario is being finalized.
     * @param iFrame        Locator for the first iframe, if applicable (can be null if not used).
     * @param iFrame_2      Locator for the second iframe, if applicable (can be null if not used).
     * @param iFrame_3      Locator for the third iframe, if applicable (can be null if not used).
     * @param targetLocator A string representing the locator of the element
     *                      from which to capture the screenshot post-finalization.
     */
    public void finalizeScenario(Page page, String iFrame, String iFrame_2, String iFrame_3, String targetLocator) {
        // Check if the scenario has not encountered any failures during execution
        if (!isFailed) {
            // Perform the cleanup process for the scenario which includes:
            // 1. Handling the teardown process
            // 2. Capturing screenshots if applicable
            // 3. Updating the scenario context as "Passed Step"
            ScreenshotHandler.handleScenarioTeardownLocator(
                    getCurrentScenario(), // Retrieve the current scenario context
                    page,                 // The Playwright Page instance used for taking the screenshot
                    iFrame,              // First iframe locator, if relevant to the scenario
                    iFrame_2,            // Second iframe locator, if relevant to the scenario
                    iFrame_3,            // Third iframe locator, if relevant to the scenario
                    targetLocator,       // The locator of the target element for the screenshot
                    "Passed Step"        // Status message indicating successful completion of the step
            );
        }
    }
}
