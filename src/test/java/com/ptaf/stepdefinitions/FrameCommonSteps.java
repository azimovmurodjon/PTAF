package com.ptaf.stepdefinitions;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.pages.FrameCommonMethods;
import com.ptaf.utils.ConfigurationProperties;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for interacting with elements inside iframes using Playwright.
 *
 * <p>
 * This class maps Cucumber/Gherkin steps to actions implemented in FrameCommonMethods.
 * It centralizes common iframe identifiers and delegates most interactions to the
 * FrameCommonMethods helper. These steps are intended to be used by testers writing
 * feature files and allow common operations such as click, fill, select, hover,
 * verify visibility/existence/state, take screenshots, and keyboard actions on elements
 * that live inside one or nested iframes.
 * </p>
 *
 * Notes for testers:
 * - iFrame is set to the selector "#frame1" by default (most single-frame scenarios).
 * - iFrame_2 is set to "iframe" to represent a nested/secondary frame when needed.
 * - iFrame_3 is declared but intentionally left null (reserved for future use).
 * - Many methods accept "element" and "locator" strings which are passed through to
 *   FrameCommonMethods. The exact meaning/format of those depends on the project's
 *   locator conventions (e.g., CSS, XPath, test ids).
 *
 * Important:
 * - Do NOT change method signatures or logic in this class. This file only contains
 *   step-to-action wiring and comments for clarity.
 */
public class FrameCommonSteps {
    /**
     * Playwright Page instance used for top-level navigation and interactions.
     * This instance is obtained from Hooks.getPage(), which should be initialized in
     * the test setup lifecycle.
     */
    private Page page;

    /**
     * A scenario-scoped Page reference used when switching context to a popup/iframe page.
     * It is intentionally not static, so a Page from a deliberately closed browser cannot
     * leak into the next @LastScenario scenario after a fresh browser is created.
     */
    private Page iframePage;

    /**
     * Primary iframe selector used in most steps. Points to the first-level frame.
     * Default value: "#frame1"
     */
    private final String iFrame = "#frame1";

    /**
     * Secondary/nested iframe selector. Used to indicate a nested frame when invoking
     * FrameCommonMethods for elements inside a child frame.
     *
     * Default value: "iframe"
     */
    private final String iFrame_2 = "iframe";

    /**
     * Reserved tertiary iframe selector for potential future use. Currently null.
     */
    private final String iFrame_3 = null;

    /**
     * Helper instance that contains common methods to interact with elements inside frames.
     * The helper resolves Hooks' current page at action time, so its public step APIs remain
     * valid after navigation or a popup/frame switch.
     */
    private final FrameCommonMethods frameCommonMethods;

    /**
     * Default constructor that initializes the Playwright Page from Hooks and the
     * FrameCommonMethods helper using the fresh page created for this scenario.
     *
     * Hooks.getPage() must return a valid Playwright Page instance created in the test
     * setup. frameCommonMethods receives the iframePage reference which can be updated
     * later (for example by switchToIframe()).
     */
    public FrameCommonSteps() {
        this.page = Hooks.getPage();
        this.iframePage = null;
        this.frameCommonMethods = new FrameCommonMethods(this.page);
    }

    /**
     * Example helper to switch context to an iframe popup.
     *
     * <p>
     * This method waits for a popup to open as a result of clicking a button that is
     * inside an iframe. It uses Playwright's waitForPopup to capture the new Page.
     * The implementation clicks a button with accessible name "Continue" inside any
     * "iframe" frame. The resulting popup Page reference is stored in the static
     * iframePage field for use by FrameCommonMethods (if those methods make use of it).
     * </p>
     *
     * WARNING:
     * - This method performs a waitForPopup and is therefore blocking until the popup
     *   appears or the default Playwright timeout is reached.
     * - The selector and role used in this helper are specific to a particular flow:
     *   page.frameLocator("iframe").getByRole(AriaRole.BUTTON, ...).click();
     */
    public void switchToIframe() {
        Page currentPage = requireActivePage();
        Hooks.suppressNextPopupAutoMaximize();
        try {
            // Wait for the navigator/frame transition popup and retain it as this scenario's
            // active page. The suppression applies only to this popup, not normal new windows.
            Page switchedPage = currentPage.waitForPopup(() ->
                currentPage.frameLocator("iframe")
                    .getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("Continue"))
                    .click()
            );
            switchedPage.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
            this.iframePage = switchedPage;
            this.page = switchedPage;
            Hooks.setPage(switchedPage);
        } catch (RuntimeException exception) {
            Hooks.cancelNextPopupAutoMaximizeSuppression();
            throw exception;
        }
    }

    /**
     * Navigate the top-level page to a URL key defined in ConfigurationProperties.
     *
     * Example Gherkin: Given we navigate to "home" url
     *
     * @param URL key or partial URL passed from the feature file which is resolved via ConfigurationProperties.getBaseUrl(URL)
     */
    @Given("^we navigate to (.*?) url$")
    public void weNavigateToURL(String URL) {
        this.page = requireActivePage();
        this.iframePage = this.page;
        // Use the project's configuration helper to get the full base URL.
        page.navigate(ConfigurationProperties.getBaseUrl(URL));
        // Optional: viewport configuration and iframe switching are commented out to avoid
        // altering test flow by default. Uncomment if needed for particular tests.
        // page.setViewportSize(1920, 1080);
        // switchToIframe();
    }

    /**
     * Retrieves the page currently owned by Hooks and rejects a missing or closed page before
     * a frame operation starts. This avoids reusing a Page from a deliberately closed browser.
     */
    private Page requireActivePage() {
        Page activePage = Hooks.getPage();
        if (activePage == null || activePage.isClosed()) {
            throw new IllegalStateException(
                "No active Playwright page is available for this frame action. " +
                "Navigate or complete the popup switch before continuing."
            );
        }
        return activePage;
    }

    /**
     * Click an element located inside the primary iframe.
     *
     * @param element logical element name used by FrameCommonMethods
     * @param locator locator string passed to FrameCommonMethods
     */
    @Then("^we click on frame (.*?) locator (.*?)$")
    public void weClickActionOnPage(String element, String locator) {
        frameCommonMethods.click(page, iFrame, null, null, element, locator);
    }

    /**
     * Double-click an element inside the primary iframe.
     *
     * @param element logical element name
     * @param locator selector/locator for the element
     */
    @Then("^we double click on frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPage(String element, String locator) {
        frameCommonMethods.dblclick(page, iFrame, null, null, element, locator);
    }

    /**
     * Fill (enter) a value into an input located inside the primary iframe.
     *
     * @param element logical element name
     * @param locator selector for the element
     * @param value value to input
     */
    @Then("^we enter value on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPage(String element, String locator, String value) {
        frameCommonMethods.fill(page, iFrame, null, null,element, locator, value);
    }

    /**
     * Select an option or choose a value inside a select element in the primary iframe.
     *
     * @param element logical element name
     * @param locator selector for the select element
     * @param value option or value to select
     */
    @Then("^we select on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPage(String element, String locator, String value) {
        frameCommonMethods.select(page, iFrame, null, null, element, locator, value);
    }

    /**
     * Check a checkbox/radio inside the primary iframe.
     *
     * @param element logical element name
     * @param locator selector for the checkbox/radio
     */
    @Then("^we check on frame (.*?) locator (.*?)$")
    public void weCheckActionOnPage(String element, String locator) {
        frameCommonMethods.check(page, iFrame, null, null, element, locator);
    }

    /**
     * Uncheck action for a checkbox inside the primary iframe.
     *
     * Note: This method currently delegates to frameCommonMethods.check(...) which might
     * represent a bug or an implementation where 'check' toggles state. Verify FrameCommonMethods
     * behavior if unchecking is not occurring as expected.
     *
     * @param element logical name
     * @param locator selector for the checkbox
     */
    @Then("^we uncheck on frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPage(String element, String locator) {
        // Intentionally calls check to preserve current logic.
        frameCommonMethods.check(page, iFrame, null, null, element, locator);
    }

    /**
     * Hover the mouse over an element inside the primary iframe.
     *
     * @param element logical element name
     * @param locator selector for the element to hover
     */
    @Then("^we hover on frame (.*?) locator (.*?)$")
    public void weHoverActionOnPage(String element, String locator) {
        frameCommonMethods.hover(page, iFrame, null, null, element, locator);
    }

    /**
     * Send low-level typing (character by character) to an element inside the primary iframe.
     *
     * @param element logical name
     * @param locator selector for target element
     * @param value string to type
     */
    @Then("^we type on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPage(String element, String locator, String value) {
        frameCommonMethods.type(page, iFrame, null, null, element, locator, value);
    }

    /**
     * Scroll to the element inside the primary iframe.
     *
     * @param element logical name
     * @param locator selector for the element to scroll into view
     */
    @Then("^we scroll on frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPage(String element, String locator) {
        frameCommonMethods.scroll(page, iFrame, null, null, element, locator);
    }

    /**
     * Clear the value of an input inside the primary iframe.
     *
     * @param element logical element name
     * @param locator selector for the input to clear
     */
    @Then("^we clear value on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnPage(String element, String locator) {
        frameCommonMethods.clear(page, iFrame, null, null, element, locator);
    }

    /**
     * Verify that an element in the primary iframe is visible.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPageLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(page, iFrame, null, null, element, locator);
    }

    /**
     * Verify that a checkbox/radio inside the primary iframe is checked.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPageLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(page, iFrame, null, null, element, locator);
    }

    /**
     * Verify that an element inside the primary iframe is enabled (not disabled).
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnPageLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(page, iFrame, null, null, element, locator);
    }

    /**
     * Verify that an element exists in the DOM inside the primary iframe.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnPageLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(page, iFrame, null, null, element, locator);
    }

    /**
     * Verify that an element contains the expected text/value inside the primary iframe.
     *
     * @param element logical name
     * @param locator selector for the element
     * @param value expected substring or value to assert contains
     */
    @Then("^we contain on frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPageLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(page, iFrame, null, null, element, locator, value);
    }

    /**
     * Get text from an element inside the primary iframe and print it to the console.
     *
     * Useful for quick debugging or logging values during test runs.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we get text on frame (.*?) locator (.*?)$")
    public void weGetTextOnPage(String element, String locator) {
        String value = frameCommonMethods.gettext(page, iFrame, null, null, element, locator);
        // Print returned text to standard output to make it visible in test logs.
        System.out.println("Value: " + value);
    }

    /**
     * Assert that an element has the exact value attribute (or underlying value) inside the primary iframe.
     *
     * @param element logical name
     * @param locator selector for the element
     * @param value expected value to assert
     */
    @Then("^we has value on frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnNewPageLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(page, iFrame, null, null, element, locator, value);
    }

    /**
     * Retrieve text for list of elements inside the primary iframe. Delegates to gettext which
     * may return concatenated or first-match text depending on implementation.
     *
     * @param element logical name representing the list
     * @param locator selector that matches multiple elements
     */
    @Then("^we get list of elements on frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnNewPage(String element, String locator) {
        frameCommonMethods.gettext(page, iFrame, null, null, element, locator);
    }

    /**
     * Click a radio button inside the primary iframe using a helper specialized for radio lists.
     *
     * @param element logical name for the radio list
     * @param locator selector to identify the specific radio items
     */
    @When("we click radio on frame (.*?) list locator (.*?)$")
    public void clickRadioOnNewPage(String element, String locator) {
        frameCommonMethods.clickRadioButton(page, iFrame, element, locator);
    }

    /**
     * Capture a screenshot of an element inside the primary iframe and save it to test-output/screenshots.
     *
     * @param element logical element name
     * @param locator selector for the element to capture
     * @param name desired name of screenshot file (without extension)
     */
    @And("^we capture screenshot on frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPage(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(page, iFrame, null, null, element, locator, filePath);
    }

    /**
     * Press a keyboard key on a target element inside the primary iframe.
     *
     * Example of key values: "Enter", "Tab", "ArrowDown".
     *
     * @param element logical element name
     * @param locator selector for the element
     * @param value key name to press
     */
    @And("^we press on frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPageKey(String element, String locator, String value) {
        frameCommonMethods.press(page, iFrame, null, null, element, locator, value);
    }

    /**
     * Click an element inside a nested/secondary frame (iFrame -> iFrame_2).
     *
     * @param element logical name
     * @param locator selector inside the nested frame
     */
    @Then("^we click on second frame (.*?) locator (.*?)$")
    public void weClickActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.click(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Double-click an element inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element inside the nested frame
     */
    @Then("^we double click on second frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.dblclick(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Fill a value into an input inside the nested secondary frame.
     *
     * @param element logical element name
     * @param locator selector inside nested frame
     * @param value text to enter
     */
    @Then("^we enter value on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.fill(page, iFrame, iFrame_2, null,element, locator, value);
    }

    /**
     * Select a value inside a select element located in the nested secondary frame.
     *
     * @param element logical name for the select
     * @param locator selector for the select element
     * @param value option/value to select
     */
    @Then("^we select on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.select(page, iFrame, iFrame_2, null, element, locator, value);
    }

    /**
     * Check a checkbox inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the checkbox
     */
    @Then("^we check on second frame (.*?) locator (.*?)$")
    public void weCheckActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.check(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Uncheck a checkbox inside the nested secondary frame.
     *
     * Note: Similar to the single-frame uncheck, this method calls check(...). Verify
     * FrameCommonMethods behavior if unchecking is required and not occurring.
     *
     * @param element logical name
     * @param locator selector for the checkbox
     */
    @Then("^we uncheck on second frame (.*?) locator (.*?)$")
    public void weUncheckActionOnSecondFrame(String element, String locator) {
        // Intentionally calls check to preserve existing behavior.
        frameCommonMethods.check(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Hover an element inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we hover on second frame (.*?) locator (.*?)$")
    public void weHoverActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.hover(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Type characters into an element inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element
     * @param value text to type
     */
    @Then("^we type on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.type(page, iFrame, iFrame_2, null, element, locator, value);
    }

    /**
     * Scroll to an element inside the nested secondary frame.
     *
     * NOTE: This call delegates to frameCommonMethods.scroll with the second frame combination
     * intentionally using iFrame for the frame path. Confirm the correct frame path if scrolling
     * does not reach the intended element.
     *
     * @param element logical name
     * @param locator selector for the element to scroll to
     */
    @Then("^we scroll on second frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnSecondFrame(String element, String locator) {
        // Intentionally calls scroll with iFrame and null for nested frame (matches original code).
        frameCommonMethods.scroll(page, iFrame, null, null, element, locator);
    }

    /**
     * Clear an input inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the input to clear
     */
    @Then("^we clear value on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnSecondFrame(String element, String locator) {
        frameCommonMethods.clear(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Verify visibility of an element inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on second frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnSecondFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Verify checked state of a checkbox/radio inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on second frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnSecondFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Verify that an element inside the nested secondary frame is enabled.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on second frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnSecondFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Verify existence of an element inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we verify on second frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnSecondFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(page, iFrame, iFrame_2, null, element, locator);
    }

    /**
     * Verify that an element contains the expected text/value inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element
     * @param value substring/value expected to be present
     */
    @Then("^we contain on second frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnSecondFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(page, iFrame, iFrame_2, null, element, locator, value);
    }

    /**
     * Get text from an element inside the nested secondary frame and print it to the console.
     *
     * @param element logical name
     * @param locator selector for the element
     */
    @Then("^we get text on second frame (.*?) locator (.*?)$")
    public void weGetTextOnSecondFrame(String element, String locator) {
        String value = frameCommonMethods.gettext(page, iFrame, iFrame_2, null, element, locator);
        // Print the retrieved value for diagnostic purposes.
        System.out.println("Value: " + value);
    }

    /**
     * Capture a screenshot of an element inside the nested secondary frame and save it to disk.
     *
     * @param element logical name
     * @param locator selector for the element
     * @param name desired filename for the screenshot (no extension)
     */
    @And("^we capture screenshot on second frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnSecondFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(page, iFrame, iFrame_2, null, element, locator, filePath);
    }

    /**
     * Press a key on a target element located inside the nested secondary frame.
     *
     * @param element logical name
     * @param locator selector for the element
     * @param value key name to press (e.g., "Enter", "Tab")
     */
    @And("^we press on second frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnSecondFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(page, iFrame, iFrame_2, null, element, locator, value);
    }
}
