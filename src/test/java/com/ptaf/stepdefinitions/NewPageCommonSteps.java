package com.ptaf.stepdefinitions;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.pages.FrameCommonMethods;
import com.ptaf.ui.pages.PageCommonMethods;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions used by Cucumber scenarios to interact with pages and frames using Playwright.
 *
 * <p>
 * This class groups together a set of generic "action" step definitions that operate:
 * - On the current active page (referred to as "new page" in step names)
 * - Inside specific iframe contexts (Plaid, Pop, Atomic)
 * - And to open/pop-up windows and switch context to them
 * </p>
 *
 * <p>
 * Each public method is bound to a Cucumber step (see annotations). The methods delegate the actual
 * element interaction implementation to PageCommonMethods or FrameCommonMethods, which encapsulate
 * low-level Playwright operations.
 * </p>
 *
 * Notes for testers:
 * - The "element" parameter commonly refers to a logical element name used by the framework.
 * - The "locator" parameter is the locator key or selector used to find the element under the given
 *   element mapping. Exact resolution logic is implemented in FrameCommonMethods/PageCommonMethods.
 * - For framed interactions, use the corresponding "plaid/pop/atomic frame" steps to ensure actions
 *   target the correct iframe context.
 */
public class NewPageCommonSteps {

    /**
     * CSS selector identifying the Plaid Link iframe. Used to scope operations to the Plaid frame.
     */
    private static final String PLAID_FRAME = "iframe[title='Plaid Link']";

    /**
     * XPath expression for a typical "Accept UI" popup iframe container. Used to scope pop-frame actions.
     */
    private static final String POP_FRAME = "//*[@id='AcceptUIContainer']/iframe";

    /**
     * CSS selector identifying the atomic transact iframe. Used to scope atomic-frame operations.
     */
    private static final String ATOMIC_FRAME = "#atomic-transact-iframe";

    /**
     * Shared variable to capture a consumer tracking ID (e.g., confirmation or transaction ID).
     * Can be referenced from other steps or printed to logs by test code.
     */
    public static String trackingID = null;

    /**
     * Default no-arg constructor.
     *
     * Included for clarity. Cucumber uses reflection to construct this step definition class.
     */
    public NewPageCommonSteps() {
    }

    /**
     * Retrieve the currently active Playwright Page from Hooks.
     *
     * The Hooks class manages the Playwright Browser/Context/Page lifecycle for the test run.
     *
     * @return the active Page instance used for subsequent actions
     */
    private Page getActivePage() {
        Page page = Hooks.getPage();
        if (page == null) {
            // The page is null — this happens in @LastScenario features when a previous scenario failed
            // and closed the shared browser. Throw a clear, descriptive error instead of a NullPointerException.
            throw new IllegalStateException(
                "The Playwright page is not available for this scenario. " +
                "This typically occurs in @LastScenario features when a previous scenario failed " +
                "and closed the shared browser. Check the failure in the previous scenario for the root cause."
            );
        }
        return page;
    }

    /**
     * Create a FrameCommonMethods helper tied to the active page.
     *
     * FrameCommonMethods contains methods that accept optional frame selectors and perform
     * actions (click, fill, select, etc.) inside the proper frame or on the main page.
     *
     * @return a new FrameCommonMethods instance initialized with the active Page
     */
    private FrameCommonMethods getFrameCommonMethods() {
        return new FrameCommonMethods(getActivePage());
    }

    /**
     * Create a PageCommonMethods helper tied to the active page.
     *
     * PageCommonMethods contains common page-level utilities (click wrappers, waits, etc.).
     *
     * @return a new PageCommonMethods instance initialized with the active Page
     */
    private PageCommonMethods getPageCommonMethods() {
        return new PageCommonMethods(getActivePage());
    }

    /**
     * Clicks an element (using element mapping + locator) which triggers a new popup window,
     * waits for that popup to be created, and switches the active context to the popup page.
     *
     * Step pattern: we click {element} locator {locator} and switch to popup
     *
     * @param element logical element name used by the framework mapping
     * @param locator locator key or selector for the element
     */
    @Then("^we click (.*?) locator (.*?) and switch to popup$")
    public void weSwitchToPopup(String element, String locator) {
        // Grab the current active page before clicking; it's needed to detect the popup created by the click.
        Page currentPage = getActivePage();
        PageCommonMethods pageCommonMethods = getPageCommonMethods();

        // Use Playwright's waitForPopup to capture the popup Page resulting from the click action.
        // The click is performed via the pageCommonMethods implementation.
        Page popupPage = currentPage.waitForPopup(() ->
                pageCommonMethods.click(currentPage,  element, locator)
        );

        // Wait for the popup's DOMContentLoaded event to ensure basic document structure is ready.
        popupPage.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Update the global Hooks active Page reference so subsequent steps operate on the popup page.
        Hooks.setPage(popupPage);
    }

    // ============================================================================================================
    // NEW PAGE ACTIONS
    // ============================================================================================================

    /**
     * Click an element on the active page (no frame). Delegates to FrameCommonMethods.click with no frame selector.
     *
     * Step pattern: we click on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we click on new page (.*?) locator (.*?)$")
    public void weClickActionNewOnPage(String element, String locator) {
        // Delegate to FrameCommonMethods; pass null for frame to indicate the main page context.
        getFrameCommonMethods().click(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Double click an element on the active page.
     *
     * Step pattern: we double click on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we double click on new page (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPage(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Fill (enter) a value into a form field on the active page.
     *
     * Step pattern: we enter value on new page {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text value to enter into the field
     */
    @Then("^we enter value on new page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPage(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), null, null, null, element, locator, value);
    }

    /**
     * Select an option from a select control on the active page.
     *
     * Step pattern: we select on new page {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector for the select element
     * @param value   option value to choose
     */
    @Then("^we select on new page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPage(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), null, null, null, element, locator, value);
    }

    /**
     * Check a checkbox or radio on the active page.
     *
     * Step pattern: we check on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we check on new page (.*?) locator (.*?)$")
    public void weCheckActionOnPage(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Uncheck a checkbox on the active page.
     *
     * Step pattern: we uncheck on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we uncheck on new page (.*?) locator (.*?)$")
    public void weUncheckActionOnPage(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Hover over an element on the active page.
     *
     * Step pattern: we hover on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we hover on new page (.*?) locator (.*?)$")
    public void weHoverActionOnPage(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Simulate typing into an element on the active page (character by character).
     *
     * Step pattern: we type on new page {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text to type
     */
    @Then("^we type on new page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPage(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), null, null, null, element, locator, value);
    }

    /**
     * Scroll the page (or element) into view on the active page.
     *
     * Step pattern: we scroll on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we scroll on new page (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPage(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Clear the value of an input or textarea on the active page.
     *
     * Step pattern: we clear value on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we clear value on new page (.*?) locator (.*?)$")
    public void weClearValueOnPage(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Verify that an element is visible on the active page.
     *
     * Step pattern: we verify on new page {element} of locator {locator} is visible
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on new page (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPageLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Verify that a checkbox/radio is checked on the active page.
     *
     * Step pattern: we verify on new page {element} of locator {locator} is checked
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on new page (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPageLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Verify that an element is enabled on the active page.
     *
     * Step pattern: we verify on new page {element} of locator {locator} is enabled
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on new page (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnPageLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Retrieve the value (e.g., input value) of an element on the active page.
     *
     * Step pattern: we get value on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get value on new page (.*?) locator (.*?)$")
    public void weGetValueOnPage(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Assert that an element has a specific value on the active page.
     *
     * Step pattern: we verify element has value on new page {element} of locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   expected value to compare against
     */
    @Then("^we verify element has value on new page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPageLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), null, null, null, element, locator, value);
    }

    /**
     * Verify that an element exists on the active page.
     *
     * Step pattern: we verify on new page {element} of locator {locator} is existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on new page (.*?) of locator (.*?) is existed$")
    public void weVerifyOnPageLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Verify that an element does NOT exist on the active page.
     *
     * Step pattern: we verify on new page {element} of locator {locator} is not existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on new page (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnPageLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Assert that an element's text contains the expected substring on the active page.
     *
     * Step pattern: we contain on new page {element} of locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   expected substring to be contained in the element's text
     */
    @Then("^we contain on new page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPageLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().contain(getActivePage(), null, null, null, element, locator, value);
    }

    /**
     * Retrieve and log the visible text for a given element on the active page.
     *
     * Step pattern: we get text on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get text on new page (.*?) locator (.*?)$")
    public void weGetTextOnPage(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Capture a screenshot for an element (or full page depending on underlying implementation).
     *
     * The screenshot is saved under test-output/screenshots/{name}.png.
     *
     * Step pattern: we capture screenshot on new page {element} locator {locator} name "{name}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param name    short name used to build the screenshot file name
     */
    @And("^we capture screenshot on new page (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPage(String element, String locator, String name) {
        // Build a consistent file path for screenshots used by test reporting.
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), null, null, null, element, locator, filePath);
    }

    /**
     * Press a specific keyboard key while focused on an element on the active page.
     *
     * Step pattern: we press on new page {element} locator {locator} key "{value}" keyboard
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   key name recognized by Playwright (e.g., Enter, ArrowDown)
     */
    @And("^we press on new page (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPageKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), null, null, null, element, locator, value);
    }

    /**
     * Click a radio button from a list on the active page.
     *
     * Step pattern: we click radio on new page {element} list locator {locator}
     *
     * @param element logical element name representing the radio group
     * @param locator locator key or selector for the radio list
     */
    @When("^we click radio on new page (.*?) list locator (.*?)$")
    public void clickRadioOnPage(String element, String locator) {
        getFrameCommonMethods().clickRadioButton(getActivePage(), null, element, locator);
    }

    /**
     * Get the text of an element and verify it contains the expected value.
     *
     * Step pattern: we get text and contain on new page {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get text and contain on new page (.*?) locator (.*?)$")
    public void weGetTextAndContainOnPage(String element, String locator) {
        getFrameCommonMethods().get_and_contain_text(getActivePage(), null, null, null, element, locator);
    }

    /**
     * Capture the consumer tracking ID from a confirmation area on the active page and store it in a shared variable.
     *
     * Step pattern: get consumer tracking ID and contain on new page
     *
     * This method locates a div containing the text 'Confirmation ID:' and reads its inner text.
     * The result is stored in the public static trackingID field for reuse across steps.
     */
    @Then("^get consumer tracking ID and contain on new page$")
    public void weGetConsumerTrackingIDAndContainOnPage() {
        // Locate the confirmation text that contains 'Confirmation ID:' and extract its inner text.
        Locator trCode = getActivePage().locator("//div[contains(text(),'Confirmation ID:')]");
        trackingID = trCode.innerText();
        // Log to console to make it easy to see during test execution and troubleshooting.
        System.out.println("Tracking Code is: " + trackingID);
    }

    // ============================================================================================================
    // PLAID FRAME ACTIONS
    // ============================================================================================================

    /**
     * Click an element inside the Plaid iframe context.
     *
     * Step pattern: we click on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we click on plaid frame (.*?) locator (.*?)$")
    public void weClickActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().click(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Double click an element inside the Plaid iframe context.
     *
     * Step pattern: we double click on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we double click on plaid frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Fill a field inside the Plaid iframe context.
     *
     * Step pattern: we enter value on plaid frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text to enter
     */
    @Then("^we enter value on plaid frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPlaidFrame(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    /**
     * Select an option inside the Plaid iframe context.
     *
     * Step pattern: we select on plaid frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   option value to choose
     */
    @Then("^we select on plaid frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPlaidFrame(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    /**
     * Check a checkbox inside the Plaid iframe.
     *
     * Step pattern: we check on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we check on plaid frame (.*?) locator (.*?)$")
    public void weCheckActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Uncheck a checkbox inside the Plaid iframe.
     *
     * Step pattern: we uncheck on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we uncheck on plaid frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Hover inside the Plaid iframe.
     *
     * Step pattern: we hover on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we hover on plaid frame (.*?) locator (.*?)$")
    public void weHoverActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Type into an element inside the Plaid iframe (character by character).
     *
     * Step pattern: we type on plaid frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text to type
     */
    @Then("^we type on plaid frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPlaidFrame(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    /**
     * Scroll to element inside the Plaid iframe.
     *
     * Step pattern: we scroll on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we scroll on plaid frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Clear a field inside the Plaid iframe.
     *
     * Step pattern: we clear value on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we clear value on plaid frame (.*?) locator (.*?)$")
    public void weClearValueOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Verify visibility of an element inside the Plaid iframe.
     *
     * Step pattern: we verify on plaid frame {element} of locator {locator} is visible
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on plaid frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPlaidFrameLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Verify a checkbox/radio is checked inside the Plaid iframe.
     *
     * Step pattern: we verify on plaid frame {element} of locator {locator} is checked
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on plaid frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPlaidFrameLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Verify an element is enabled inside the Plaid iframe.
     *
     * Step pattern: we verify on plaid frame {element} of locator {locator} is enabled
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on plaid frame (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnPlaidFrameLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Get the value of an element inside the Plaid iframe.
     *
     * Step pattern: we get value on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get value on plaid frame (.*?) locator (.*?)$")
    public void weGetValueOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Assert an element has a specific value inside the Plaid iframe.
     *
     * Step pattern: we verify element has value on plaid frame {element} of locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   expected value
     */
    @Then("^we verify element has value on plaid frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPlaidFrameLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    /**
     * Verify that an element exists inside the Plaid iframe.
     *
     * Step pattern: we verify on plaid frame {element} of locator {locator} is existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on plaid frame (.*?) of locator (.*?) is existed$")
    public void weVerifyOnPlaidFrameLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Verify that an element does NOT exist inside the Plaid iframe.
     *
     * Step pattern: we verify on plaid frame {element} of locator {locator} is not existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on plaid frame (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnPlaidFrameLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Assert that an element's text contains expected text inside the Plaid iframe.
     *
     * Step pattern: we contain on plaid frame {element} of locator {locator} value "{expectedText}"
     *
     * @param element      logical element name
     * @param locator      locator key or selector
     * @param expectedText expected substring to be found
     */
    @Then("^we contain on plaid frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPlaidFrameLocatorValue(String element, String locator, String expectedText) {
        getFrameCommonMethods().contain(getActivePage(), PLAID_FRAME, null, null, element, locator, expectedText);
    }

    /**
     * Retrieve text from an element inside the Plaid iframe.
     *
     * Step pattern: we get text on plaid frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get text on plaid frame (.*?) locator (.*?)$")
    public void weGetTextOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    /**
     * Capture a screenshot inside the Plaid iframe context.
     *
     * Step pattern: we capture screenshot on plaid frame {element} locator {locator} name "{name}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param name    short file name to save screenshot as
     */
    @And("^we capture screenshot on plaid frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPlaidFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), PLAID_FRAME, null, null, element, locator, filePath);
    }

    /**
     * Press a keyboard key while focused on an element within the Plaid iframe.
     *
     * Step pattern: we press on plaid frame {element} locator {locator} key "{value}" keyboard
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   key name (e.g., Enter)
     */
    @And("^we press on plaid frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPlaidFrameKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    // ============================================================================================================
    // POP FRAME ACTIONS
    // ============================================================================================================

    /**
     * Click inside the Pop iframe context.
     *
     * Step pattern: we click on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we click on pop frame (.*?) locator (.*?)$")
    public void weClickActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().click(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Double click inside the Pop iframe context.
     *
     * Step pattern: we double click on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we double click on pop frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Fill a field inside the Pop iframe.
     *
     * Step pattern: we enter value on pop frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text to enter
     */
    @Then("^we enter value on pop frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPopFrame(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    /**
     * Select an option inside the Pop iframe.
     *
     * Step pattern: we select on pop frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   option value to select
     */
    @Then("^we select on pop frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPopFrame(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    /**
     * Check a checkbox inside the Pop iframe.
     *
     * Step pattern: we check on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we check on pop frame (.*?) locator (.*?)$")
    public void weCheckActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Uncheck a checkbox inside the Pop iframe.
     *
     * Step pattern: we uncheck on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we uncheck on pop frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Hover inside the Pop iframe.
     *
     * Step pattern: we hover on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we hover on pop frame (.*?) locator (.*?)$")
    public void weHoverActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Type inside the Pop iframe.
     *
     * Step pattern: we type on pop frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text to type
     */
    @Then("^we type on pop frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPopFrame(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    /**
     * Scroll inside the Pop iframe.
     *
     * Step pattern: we scroll on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we scroll on pop frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPopFrame(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Clear a field inside the Pop iframe.
     *
     * Step pattern: we clear value on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we clear value on pop frame (.*?) locator (.*?)$")
    public void weClearValueOnPopFrame(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Verify element visibility inside the Pop iframe.
     *
     * Step pattern: we verify on pop frame {element} of locator {locator} is visible
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on pop frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPopFrameLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Verify checked state inside the Pop iframe.
     *
     * Step pattern: we verify on pop frame {element} of locator {locator} is checked
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on pop frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPopFrameLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Verify enabled state inside the Pop iframe.
     *
     * Step pattern: we verify on pop frame {element} of locator {locator} is enabled
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on pop frame (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnPopFrameLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Get an element's value inside the Pop iframe.
     *
     * Step pattern: we get value on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get value on pop frame (.*?) locator (.*?)$")
    public void weGetValueOnPopFrame(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Assert an element value inside the Pop iframe.
     *
     * Step pattern: we verify element has value on pop frame {element} of locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   expected value
     */
    @Then("^we verify element has value on pop frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPopFrameLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    /**
     * Verify existence inside the Pop iframe.
     *
     * Step pattern: we verify on pop frame {element} of locator {locator} is existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on pop frame (.*?) of locator (.*?) is existed$")
    public void weVerifyOnPopFrameLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Verify non-existence inside the Pop iframe.
     *
     * Step pattern: we verify on pop frame {element} of locator {locator} is not existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on pop frame (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnPopFrameLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Assert text containment inside the Pop iframe.
     *
     * Step pattern: we contain on pop frame {element} of locator {locator} value "{expectedText}"
     *
     * @param element      logical element name
     * @param locator      locator key or selector
     * @param expectedText expected substring in the element text
     */
    @Then("^we contain on pop frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPopFrameLocatorValue(String element, String locator, String expectedText) {
        getFrameCommonMethods().contain(getActivePage(), POP_FRAME, null, null, element, locator, expectedText);
    }

    /**
     * Get text from an element inside the Pop iframe.
     *
     * Step pattern: we get text on pop frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get text on pop frame (.*?) locator (.*?)$")
    public void weGetTextOnPopFrame(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    /**
     * Capture screenshot inside the Pop iframe.
     *
     * Step pattern: we capture screenshot on pop frame {element} locator {locator} name "{name}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param name    file name base for the screenshot
     */
    @And("^we capture screenshot on pop frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPopFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), POP_FRAME, null, null, element, locator, filePath);
    }

    /**
     * Press a key inside the Pop iframe.
     *
     * Step pattern: we press on pop frame {element} locator {locator} key "{value}" keyboard
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   key name to press
     */
    @And("^we press on pop frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPopFrameKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    // ============================================================================================================
    // ATOMIC FRAME ACTIONS
    // ============================================================================================================

    /**
     * Click inside the Atomic iframe context.
     *
     * Step pattern: we click on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we click on atomic frame (.*?) locator (.*?)$")
    public void weClickActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().click(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Double click inside the Atomic iframe.
     *
     * Step pattern: we double click on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we double click on atomic frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Fill a field inside the Atomic iframe.
     *
     * Step pattern: we enter value on atomic frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text to enter
     */
    @Then("^we enter value on atomic frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnAtomicFrame(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    /**
     * Select an option inside the Atomic iframe.
     *
     * Step pattern: we select on atomic frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   option value
     */
    @Then("^we select on atomic frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnAtomicFrame(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    /**
     * Check a checkbox inside the Atomic iframe.
     *
     * Step pattern: we check on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we check on atomic frame (.*?) locator (.*?)$")
    public void weCheckActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Uncheck a checkbox inside the Atomic iframe.
     *
     * Step pattern: we uncheck on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we uncheck on atomic frame (.*?) locator (.*?)$")
    public void weUncheckActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Hover inside the Atomic iframe.
     *
     * Step pattern: we hover on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we hover on atomic frame (.*?) locator (.*?)$")
    public void weHoverActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Type inside the Atomic iframe.
     *
     * Step pattern: we type on atomic frame {element} locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   text to type
     */
    @Then("^we type on atomic frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnAtomicFrame(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    /**
     * Scroll inside the Atomic iframe.
     *
     * Step pattern: we scroll on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we scroll on atomic frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Clear a field inside the Atomic iframe.
     *
     * Step pattern: we clear value on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we clear value on atomic frame (.*?) locator (.*?)$")
    public void weClearValueOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Verify visibility inside the Atomic iframe.
     *
     * Step pattern: we verify on atomic frame {element} of locator {locator} is visible
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on atomic frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnAtomicFrameLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Verify checked state inside the Atomic iframe.
     *
     * Step pattern: we verify on atomic frame {element} of locator {locator} is checked
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on atomic frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnAtomicFrameLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Verify enabled state inside the Atomic iframe.
     *
     * Step pattern: we verify on atomic frame {element} of locator {locator} is enabled
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on atomic frame (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnAtomicFrameLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Get an element value inside the Atomic iframe.
     *
     * Step pattern: we get value on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get value on atomic frame (.*?) locator (.*?)$")
    public void weGetValueOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Assert element has a given value inside the Atomic iframe.
     *
     * Step pattern: we verify element has value on atomic frame {element} of locator {locator} value "{value}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   expected value
     */
    @Then("^we verify element has value on atomic frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnAtomicFrameLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    /**
     * Verify element existence inside the Atomic iframe.
     *
     * Step pattern: we verify on atomic frame {element} of locator {locator} is existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on atomic frame (.*?) of locator (.*?) is existed$")
    public void weVerifyOnAtomicFrameLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Verify element non-existence inside the Atomic iframe.
     *
     * Step pattern: we verify on atomic frame {element} of locator {locator} is not existed
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we verify on atomic frame (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnAtomicFrameLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Assert text containment inside the Atomic iframe.
     *
     * Step pattern: we contain on atomic frame {element} of locator {locator} value "{expectedText}"
     *
     * @param element      logical element name
     * @param locator      locator key or selector
     * @param expectedText expected text substring
     */
    @Then("^we contain on atomic frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnAtomicFrameLocatorValue(String element, String locator, String expectedText) {
        getFrameCommonMethods().contain(getActivePage(), ATOMIC_FRAME, null, null, element, locator, expectedText);
    }

    /**
     * Get text from an element inside the Atomic iframe.
     *
     * Step pattern: we get text on atomic frame {element} locator {locator}
     *
     * @param element logical element name
     * @param locator locator key or selector
     */
    @Then("^we get text on atomic frame (.*?) locator (.*?)$")
    public void weGetTextOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    /**
     * Capture screenshot inside the Atomic iframe.
     *
     * Step pattern: we capture screenshot on atomic frame {element} locator {locator} name "{name}"
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param name    screenshot file name base
     */
    @And("^we capture screenshot on atomic frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnAtomicFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), ATOMIC_FRAME, null, null, element, locator, filePath);
    }

    /**
     * Press a key inside the Atomic iframe.
     *
     * Step pattern: we press on atomic frame {element} locator {locator} key "{value}" keyboard
     *
     * @param element logical element name
     * @param locator locator key or selector
     * @param value   key name
     */
    @And("^we press on atomic frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnAtomicFrameKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }
}
