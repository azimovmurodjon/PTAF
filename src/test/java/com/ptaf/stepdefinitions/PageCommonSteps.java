package com.ptaf.stepdefinitions;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.pages.PageCommonMethods;
import com.ptaf.utils.ConfigurationProperties;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Step definitions implementing common page interactions for Cucumber scenarios.
 *
 * <p>
 * This class delegates almost all work to PageCommonMethods which encapsulates Playwright
 * interactions. Steps are written in a generic way so they can be reused across feature files.
 * Each step accepts an 'element' and a 'locator' parameter which are intended to match keys
 * defined in the project's element/locator repository (for example a page object or locator map).
 * </p>
 *
 * <p>
 * Notes for testers:
 * - 'element' typically represents a logical name or group for the locator (e.g., "loginPage", "header", etc.).
 * - 'locator' typically represents the key within that group used to find the element (e.g., "submitButton").
 * - When steps accept a 'value' parameter, that value is passed to the action (for typing, selecting, asserting, etc.).
 * - Some steps perform blocking waits using Thread.sleep; prefer explicit waiting via PageCommonMethods where possible.
 * </p>
 *
 * Do not change this class's logic when reusing it — it acts as a thin layer between Cucumber and PageCommonMethods.
 */
public class PageCommonSteps {
    /**
     * Playwright Page instance obtained from the Hooks class.
     * This represents the active browser tab/page used for interactions.
     */
    private final Page page = Hooks.getPage();

    /**
     * Helper class that abstracts common Playwright interactions (click, fill, select, etc.).
     * All actions from step definitions are delegated to this object to keep steps concise.
     */
    private final PageCommonMethods pageCommonMethods = new PageCommonMethods(page);

    /**
     * SLF4J logger for logging messages and debugging information during step execution.
     */
    private static final Logger logger = LoggerFactory.getLogger(PageCommonSteps.class);

    /**
     * BrowserContext from Playwright. Available here for future use if tests need context-level actions.
     */
    private final BrowserContext browserContext = Hooks.getContext();

//    The following commented code is retained for reference. It demonstrates an example of interacting
//    with an iframe and navigating to a configured URL. It's intentionally left commented out to avoid
//    changing runtime behavior, but kept to help testers/developers who may want to re-enable or adapt it.
//
//    public void switchToIframe() {
//        Page iframePage = page.waitForPopup(() -> {
//            page.frameLocator("iframe").getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("Continue")).click();
//        });
//    }
//
//    @Given("^we navigate to (.*?) url$")
//    public void weNavigateToURL(String URL) {
//        page.navigate(ConfigurationProperties.getBaseUrl(URL));
////        page.setViewportSize(1920, 1080);
//        switchToIframe();
//    }

    /**
     * Clicks on a locator on the current page.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the element on the page
     */
    @Then("^we click on page (.*?) locator (.*?)$")
    public void weClickActionOnPage(String element, String locator) {
        // Delegate the click action to the pageCommonMethods helper.
        pageCommonMethods.click(page, element, locator);
    }

    /**
     * Performs a double-click on the specified locator.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the element on the page
     */
    @Then("^we double click on page (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPage(String element, String locator) {
        // Delegate the double-click action to the helper.
        pageCommonMethods.dblclick(page, element, locator);
    }

    /**
     * Fills the specified input with the provided value.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the element on the page
     * @param value   the text value to fill into the input field
     */
    @Then("^we enter value on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPage(String element, String locator, String value) {
        // Use helper to fill value into the located element.
        pageCommonMethods.fill(page, element, locator, value);
    }

    /**
     * Selects an option (by value, label, or index depending on implementation) from a select element.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the select element on the page
     * @param value   the option value/label/index to select
     */
    @Then("^we select on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPage(String element, String locator, String value) {
        pageCommonMethods.select(page, element, locator, value);
    }

    /**
     * Checks a checkbox or toggles a checkable element to the checked state.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the checkbox element
     */
    @Then("^we check on page (.*?) locator (.*?)$")
    public void weCheckActionOnPage(String element, String locator) {
        pageCommonMethods.check(page, element, locator);
    }

    /**
     * Intended to uncheck a checkbox or togglable element. Note: currently calls the same helper as check().
     * If the helper handles state idempotently, this will uncheck only if needed; otherwise test definitions
     * should ensure the correct state or a separate uncheck method should be implemented.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the checkbox element
     */
    @Then("^we uncheck on page (.*?) locator (.*?)$")
    public void weUncheckActionOnPage(String element, String locator) {
        // Delegate to the same check() helper for now; keep method separate for readability of steps.
        pageCommonMethods.check(page, element, locator);
    }

    /**
     * Performs a hover action over the specified element.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the element to hover on
     */
    @Then("^we hover on page (.*?) locator (.*?)$")
    public void weHoverActionOnPage(String element, String locator) {
        pageCommonMethods.hover(page, element, locator);
    }

    /**
     * Types the given value into the element (may differ from fill by simulating keystrokes).
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the element
     * @param value   the value to type
     */
    @Then("^we type on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPage(String element, String locator, String value) {
        pageCommonMethods.type(page, element, locator, value);
    }

    /**
     * Scrolls the page to bring the locator into view. Implementation depends on PageCommonMethods.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to scroll to
     */
    @Then("^we scroll on page (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPage(String element, String locator) {
        pageCommonMethods.scroll(page, element, locator);
    }

    /**
     * Clears the text value from an input element.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to find the input element
     */
    @Then("^we clear value on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnPage(String element, String locator) {
        pageCommonMethods.clear(page, element, locator);
    }

    /**
     * Verifies that the locator is visible on the page. The underlying helper may throw or assert.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to verify visibility for
     */
    @Then("^we verify on page (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPageLocatorIsVisible(String element, String locator) {
        pageCommonMethods.isvisible(page, element, locator);
    }

    /**
     * Verifies that the locator is checked (for checkboxes/radios).
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to verify checked state for
     */
    @Then("^we verify on page (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPageLocatorIsChecked(String element, String locator) {
        pageCommonMethods.ischecked(page, element, locator);
    }

    /**
     * Verifies that the locator is enabled (not disabled) and can be interacted with.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to verify enabled state for
     */
    @Then("^we verify on page (.*?) of locator (.*?) is enabled")
    public void weVerifyOnPageLocatorIsEnabled(String element, String locator) {
        pageCommonMethods.isenabled(page, element, locator);
    }

    /**
     * Verifies that the locator exists in the DOM (presence check).
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key whose existence should be verified
     */
    @Then("^we verify on page (.*?) of locator (.*?) is existed")
    public void weVerifyOnPageLocatorIsExisted(String element, String locator) {
        pageCommonMethods.exists(page, element, locator);
    }

    /**
     * Verifies that the element contains the expected value/text.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to evaluate
     * @param value   expected substring or content to assert
     */
    @Then("^we contain on page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPageLocatorValue(String element, String locator, String value) {
        pageCommonMethods.contain(page, element, locator, value);
    }

    /**
     * Retrieves the text content of the specified locator. The helper typically returns the text, but
     * the step does not return it to the caller — it is expected that the helper handles any logging/assertions.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key whose text to retrieve
     */
    @Then("^we get text on page (.*?) locator (.*?)$")
    public void weGetTextOnPage(String element, String locator) {
        pageCommonMethods.gettext(page, element, locator);
    }

    /**
     * Retrieves the 'value' attribute of the specified element (commonly for inputs).
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to retrieve the value from
     */
    @Then("^we get value on page (.*?) locator (.*?)$")
    public void weGetValueOnPage(String element, String locator) {
        pageCommonMethods.getvalue(page, element, locator);
    }

    /**
     * Asserts that the element has the specified value.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to check the value on
     * @param value   expected value of the element
     */
    @Then("^we has value on page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPageLocatorValue(String element, String locator, String value) {
        pageCommonMethods.hasvalue(page, element, locator, value);
    }

    /**
     * Retrieves a list of elements matching the locator and returns/handles them in the helper.
     *
     * @param element logical element/group name used to resolve the locator group
     * @param locator specific locator key that can match multiple elements
     */
    @Then("^we get list of elements on page (.*?) locator (.*?)$")
    public void weGetListOfElementsOnPage(String element, String locator) {
        pageCommonMethods.getListOfElements(page, element, locator);
    }

    /**
     * Retrieves and prints the text of an element (or the first matched element depending on implementation).
     * This step explicitly prints the returned value to stdout for quick debugging in test runs.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key whose text to retrieve
     */
    @Then("^we get text of elements on page (.*?) locator (.*?)$")
    public void weGetTextOfElementsOnPage(String element, String locator) {
        // Call helper to get text; print it for visibility in test output.
        String value = pageCommonMethods.gettext(page, element, locator);
        System.out.println("Value: " + value);
    }

    /**
     * Clicks a radio button from a list of radios resolved by the locator.
     *
     * @param element logical element/group name used to resolve the locator list
     * @param locator specific locator key representing the radio list
     */
    @When("we click radio on page (.*?) list locator (.*?)$")
    public void clickRadioOnPage(String element, String locator) {
        pageCommonMethods.clickRadioButton(page, element, locator);
    }

    /**
     * Captures a screenshot of the given locator and stores it under test-output/screenshots with the provided name.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to capture
     * @param name    filename (without extension) to save the screenshot as
     */
    @And("^we capture screenshot on page (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPage(String element, String locator, String name) {
        // Compose the file path for screenshots. Helpers expect a full path.
        String filePath = "test-output/screenshots/" + name + ".png";
        pageCommonMethods.screenshot(page, element, locator, filePath);
    }

    /**
     * Presses a keyboard key against the specified element. 'value' represents the key (e.g., Enter, Tab).
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key to press the key on
     * @param value   the keyboard key to press
     */
    @And("^we press on page (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPageKey(String element, String locator, String value) {
        pageCommonMethods.press(page, element, locator, value);
    }

    /**
     * Initiates a download by clicking the locator and places the file in the configured download directory.
     * The final filename used here appends ".jpeg" to the configured download path.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key that triggers the download when clicked
     */
    @And("^we click download on page (.*?) locator (.*?)$")
    public void weDownloadOnPageKey(String element, String locator) {
        // Get configured download path and delegate the download handling to the helper.
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        pageCommonMethods.download(page, element, locator, filePath + ".jpeg");
    }

    /**
     * Selects a file to upload for the given locator. The filename is hard-coded here; adapt as needed.
     *
     * @param element logical element/group name used to resolve the locator
     * @param locator specific locator key representing the file input control
     */
    @And("^we select document to upload on page (.*?) locator (.*?)$")
    public void weSelectDocument(String element, String locator) {
        // Example: retrieve a base path if needed; helper is passed a filename for the upload.
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        // NOTE: The file name is currently hard-coded. Update the filename or implement a parameterized step if needed.
        pageCommonMethods.selectFile(page, element, locator, "Mobile Automation Platforms.docx");
    }

    /**
     * Logs the current page title to the configured logger.
     * Useful for quick verification and debugging during scenario execution.
     */
    @Given("^get title of page$")
    public void getTitleOfPage() {
        // Retrieve title via Playwright Page API
        String title = page.title();
        logger.info("Page title: {}", title);
    }

    /**
     * A simple utility step that waits for a short, fixed amount of time (3 seconds).
     * Caution: This uses Thread.sleep and will block the current thread — prefer explicit waits.
     *
     * @throws InterruptedException if the sleep is interrupted
     */
    @And("^we wait for some time$")
    public void weWaitForSomeTime() throws InterruptedException {
        // Simple hard sleep; intended for short pauses between steps.
        Thread.sleep(3000);
    }

    /**
     * Waits (blocks) for the specified number of seconds.
     * This is a convenience step for scenarios but uses Thread.sleep; consider replacing with smarter waiting.
     *
     * @param time_to_wait number of seconds to wait (provided as string in Gherkin)
     * @throws InterruptedException if the sleep is interrupted
     */
    @And("^time out for (.*?) seconds$")
    public void timeOutFoSeconds(String time_to_wait) throws InterruptedException {
        // Convert the string to an integer representing seconds
        int seconds = Integer.parseInt(time_to_wait);

        // Wait for the specified number of seconds (converted to milliseconds)
        Thread.sleep(seconds * 1000L);
    }

    /**
     * Stops execution by blocking for 30 seconds.
     * This step is primarily useful for debugging or manual intervention during test runs.
     *
     * @throws Exception if the thread sleep is interrupted
     */
    @Then("^Stop Execution")
    public void stopExecution() throws Exception
    {
        // Intentionally long sleep to halt execution for debugging purposes.
        Thread.sleep(30000);

    }

    /**
     * Closes all browser resources by delegating to Hooks. This should be used to ensure Playwright
     * contexts, pages, and browsers are cleaned up at the end of scenarios.
     *
     * @throws Exception if closing resources throws
     */
    @Then("^we close all browsers$")
    public void weCloseAllBrowsers() throws Exception{
        // Delegate to the Hooks utility to free browser resources.
        Hooks.closeBrowserResources();
    }
}
