package com.ptaf.stepdefinitions;

import com.ptaf.mobile.assertions.MobileAssert;
import com.ptaf.mobile.config.MobilePlatform;
import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
import com.ptaf.mobile.implementation.MobileActionImpl;
import com.ptaf.mobile.permissions.MobilePermissionHandler;
import com.ptaf.mobile.interfaces.MobileAction;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for PTAF native mobile Appium automation.
 *
 * <p>
 * This class exposes a set of high-level step definitions that map Gherkin steps
 * to mobile interactions implemented by the MobileAction implementation and
 * helper classes such as MobileDriverManager, MobilePermissionHandler and
 * MobileEvidenceManager.
 * </p>
 *
 * <p>
 * Testers can use these steps in feature files. Each method is annotated with
 * Cucumber's {@code @Given}, {@code @When} or {@code @Then} and delegates the
 * actual work to underlying components. No test logic is contained here beyond
 * argument passing and simple assertions where appropriate.
 * </p>
 *
 * <p>
 * Note: This class does not manage driver lifecycle beyond delegating to the
 * MobileDriverManager. Make sure the appropriate driver configuration is
 * available when starting the driver or browser driver.
 * </p>
 */
public class MobileSteps {
    /**
     * Primary interface used to interact with mobile elements and actions.
     *
     * <p>
     * This field is final and initialized with the default implementation
     * MobileActionImpl. It encapsulates cross-platform mobile operations such
     * as tap, type, swipe, etc.
     * </p>
     */
    private final MobileAction mobileAction = new MobileActionImpl();

    /**
     * Helper factory method that creates a MobilePermissionHandler bound to the
     * current driver instance.
     *
     * <p>
     * Using this helper avoids repeating the driver lookup call and keeps
     * permission handling centralized.
     * </p>
     *
     * @return a MobilePermissionHandler tied to the current MobileDriver
     */
    private MobilePermissionHandler permissions() {
        // Create a new permission handler for the currently active driver.
        return new MobilePermissionHandler(MobileDriverManager.getDriver());
    }

    /**
     * Start native mobile application driver for the specified platform.
     *
     * <p>
     * Example Gherkin: Given I start mobile application using platform "ANDROID"
     * or "IOS". The provided platform string is converted into the MobilePlatform
     * enum and the MobileDriverManager handles driver startup.
     * </p>
     *
     * @param platform platform name (case-insensitive) expected by MobilePlatform.from()
     */
    @Given("I start mobile application using platform {string}")
    public void iStartMobileApplicationUsingPlatform(String platform) {
        // Convert incoming platform string to enum and start an Appium driver for the mobile app.
        MobileDriverManager.startDriver(MobilePlatform.from(platform));
    }

    /**
     * Start a mobile browser session (web context) for the specified platform.
     *
     * <p>
     * Use this when testing web pages in a mobile browser rather than a native
     * application.
     * </p>
     *
     * @param platform platform name (converted by MobilePlatform.from())
     */
    @Given("I start mobile browser using platform {string}")
    public void iStartMobileBrowserUsingPlatform(String platform) {
        // Start a browser-capable mobile driver for the given platform.
        MobileDriverManager.startBrowserDriver(MobilePlatform.from(platform));
    }

    /**
     * Open the given URL in the mobile browser context.
     *
     * @param url the URL to navigate to
     */
    @When("I open mobile browser url {string}")
    public void iOpenMobileBrowserUrl(String url) {
        // Delegate URL navigation to the MobileAction implementation.
        mobileAction.openUrl(url);
    }

    /**
     * Press the Enter key on an element found via page and locator.
     *
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @When("I press Enter on mobile page {word} locator {word}")
    public void iPressEnterOnMobilePageLocator(String page, String locator) {
        // Send an Enter/Return action to the element.
        mobileAction.pressEnter(page, locator);
    }

    /**
     * Save the current mobile browser page source to the provided path.
     *
     * @param outputPath path on the local machine where page source should be saved
     */
    @When("I save mobile browser page source to {string}")
    public void iSaveMobileBrowserPageSourceTo(String outputPath) {
        // Capture the page source and write it to the given output path.
        mobileAction.savePageSource(outputPath);
    }

    /**
     * Assert that the mobile browser's current URL contains the expected substring.
     *
     * @param expected substring expected to be present in the current URL
     * @throws AssertionError if the current URL is null or does not contain expected
     */
    @Then("mobile browser current url should contain {string}")
    public void mobileBrowserCurrentUrlShouldContain(String expected) {
        // Retrieve the current URL via MobileAction and perform a contains check.
        String actual = mobileAction.getCurrentUrl();
        if (actual == null || !actual.contains(expected)) {
            // Throw an assertion error so the Cucumber scenario fails with a clear message.
            throw new AssertionError("Expected mobile browser URL to contain [" + expected + "] but was [" + actual + "]");
        }
    }

    /**
     * Assert that the mobile browser's title contains the expected substring,
     * case-insensitive.
     *
     * @param expected substring expected to be present in the page title
     * @throws AssertionError if the title is null or does not contain expected (case-insensitive)
     */
    @Then("mobile browser title should contain {string}")
    public void mobileBrowserTitleShouldContain(String expected) {
        // Compare titles case-insensitively for a more robust check.
        String actual = mobileAction.getTitle();
        if (actual == null || !actual.toLowerCase().contains(expected.toLowerCase())) {
            throw new AssertionError("Expected mobile browser title to contain [" + expected + "] but was [" + actual + "]");
        }
    }

    /**
     * Tap (single press) on a mobile element identified by page and locator.
     *
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @When("I tap on mobile page {word} locator {word}")
    public void iTapOnMobilePageLocator(String page, String locator) {
        mobileAction.tap(page, locator);
    }

    /**
     * Long press (press and hold) on a mobile element for a specified duration.
     *
     * @param page          page identifier used by the locator strategy
     * @param locator       locator identifier within the page
     * @param durationMillis duration to hold in milliseconds
     */
    @When("I long press mobile page {word} locator {word} for {int} milliseconds")
    public void iLongPressMobileElement(String page, String locator, int durationMillis) {
        mobileAction.longPress(page, locator, durationMillis);
    }

    /**
     * Perform a double tap on a mobile element.
     *
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @When("I double tap mobile page {word} locator {word}")
    public void iDoubleTapMobileElement(String page, String locator) {
        mobileAction.doubleTap(page, locator);
    }

    /**
     * Tap a specific coordinate on the mobile screen.
     *
     * @param x x-coordinate (pixels or screen units depending on driver)
     * @param y y-coordinate (pixels or screen units depending on driver)
     */
    @When("I tap mobile screen at x {int} y {int}")
    public void iTapMobileScreenAt(int x, int y) {
        mobileAction.tapAt(x, y);
    }

    /**
     * Drag an element from one locator to another.
     *
     * @param fromPage    page identifier for source element
     * @param fromLocator locator identifier for source element
     * @param toPage      page identifier for target element
     * @param toLocator   locator identifier for target element
     */
    @When("I drag mobile page {word} locator {word} to page {word} locator {word}")
    public void iDragMobileElement(String fromPage, String fromLocator, String toPage, String toLocator) {
        mobileAction.drag(fromPage, fromLocator, toPage, toLocator);
    }

    /**
     * Scroll until the element becomes visible or the maximum number of swipes is reached.
     *
     * @param page      page identifier used by the locator strategy
     * @param locator   locator identifier within the page
     * @param maxSwipes maximum number of swipe attempts to bring the element into view
     */
    @When("I scroll mobile page {word} locator {word} into view with max {int} swipes")
    public void iScrollMobileElementIntoView(String page, String locator, int maxSwipes) {
        mobileAction.scrollUntilVisible(page, locator, maxSwipes);
    }

    /**
     * Scroll the mobile screen until the provided text is visible.
     *
     * @param text the visible text to scroll to
     */
    @When("I scroll mobile screen to text {string}")
    public void iScrollMobileScreenToText(String text) {
        mobileAction.scrollToText(text);
    }

    /**
     * Enter text into a mobile input element found by page and locator.
     *
     * @param value   text value to type into the element
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @When("I enter mobile value {string} on page {word} locator {word}")
    public void iEnterMobileValueOnPageLocator(String value, String page, String locator) {
        mobileAction.type(page, locator, value);
    }

    /**
     * Clear the content of a mobile element (e.g., input field).
     *
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @When("I clear mobile page {word} locator {word}")
    public void iClearMobilePageLocator(String page, String locator) {
        mobileAction.clear(page, locator);
    }

    /**
     * Attempt to hide the on-screen mobile keyboard if it is displayed.
     */
    @When("I hide mobile keyboard")
    public void iHideMobileKeyboard() {
        mobileAction.hideKeyboard();
    }

    /**
     * Send the app to background for the specified number of seconds.
     *
     * @param seconds time in seconds to keep the app in the background
     */
    @When("I background mobile app for {int} seconds")
    public void iBackgroundMobileAppForSeconds(int seconds) {
        mobileAction.backgroundApp(seconds);
    }

    /**
     * Swipe up on the mobile screen (useful for scrolling).
     */
    @When("I swipe mobile screen up")
    public void iSwipeMobileScreenUp() {
        mobileAction.swipeUp();
    }

    /**
     * Swipe down on the mobile screen (useful for scrolling).
     */
    @When("I swipe mobile screen down")
    public void iSwipeMobileScreenDown() {
        mobileAction.swipeDown();
    }

    /**
     * Swipe left on the mobile screen (often used for carousel navigation).
     */
    @When("I swipe mobile screen left")
    public void iSwipeMobileScreenLeft() {
        mobileAction.swipeLeft();
    }

    /**
     * Swipe right on the mobile screen (often used for carousel navigation).
     */
    @When("I swipe mobile screen right")
    public void iSwipeMobileScreenRight() {
        mobileAction.swipeRight();
    }

    /**
     * Perform a pinch-in gesture on the mobile screen (zoom out).
     */
    @When("I pinch in mobile screen")
    public void iPinchInMobileScreen() {
        mobileAction.pinchIn();
    }

    /**
     * Perform a zoom-out gesture on the mobile screen (zoom in).
     */
    @When("I zoom out mobile screen")
    public void iZoomOutMobileScreen() {
        mobileAction.zoomOut();
    }

    /**
     * Set the device orientation using the provided orientation string.
     *
     * @param orientation requested orientation (e.g., "LANDSCAPE", "PORTRAIT")
     */
    @When("I rotate mobile screen to {string}")
    public void iRotateMobileScreenTo(String orientation) {
        mobileAction.setOrientation(orientation);
    }

    /**
     * Rotate the device using a pre-configured orientation from test configuration.
     */
    @When("I rotate mobile screen using configured orientation")
    public void iRotateMobileScreenUsingConfiguredOrientation() {
        mobileAction.setConfiguredOrientation();
    }

    /**
     * Activate a different mobile application by its application identifier.
     *
     * @param appId application package or bundle identifier to activate
     */
    @When("I activate mobile app {string}")
    public void iActivateMobileApp(String appId) {
        mobileAction.activateApp(appId);
    }

    /**
     * Terminate a mobile application identified by its application identifier.
     *
     * @param appId application package or bundle identifier to terminate
     */
    @When("I terminate mobile app {string}")
    public void iTerminateMobileApp(String appId) {
        mobileAction.terminateApp(appId);
    }

    /**
     * Open a deep link URL associated with a specific application.
     *
     * @param url   deep link URL to open
     * @param appId application package or bundle identifier to handle the deep link
     */
    @When("I open mobile deep link {string} for app {string}")
    public void iOpenMobileDeepLink(String url, String appId) {
        mobileAction.openDeepLink(url, appId);
    }

    /**
     * Push a local file from the test machine to a path on the mobile device.
     *
     * @param localPath  path on the test machine to push from
     * @param remotePath path on the mobile device to push to
     */
    @When("I push local file {string} to mobile path {string}")
    public void iPushLocalFileToMobilePath(String localPath, String remotePath) {
        // Note: underlying API expects remotePath first then localPath
        mobileAction.pushFile(remotePath, localPath);
    }

    /**
     * Pull a file from the mobile device to the local test machine.
     *
     * @param remotePath path on the mobile device to retrieve
     * @param localPath  destination path on the local machine
     */
    @When("I pull mobile file {string} to local path {string}")
    public void iPullMobileFileToLocalPath(String remotePath, String localPath) {
        mobileAction.pullFile(remotePath, localPath);
    }

    /**
     * Set the mobile device clipboard content to the provided text.
     *
     * @param text text to place into the clipboard
     */
    @When("I set mobile clipboard text {string}")
    public void iSetMobileClipboardText(String text) {
        mobileAction.setClipboard(text);
    }

    /**
     * Switch the mobile driver's context to the provided context name (for example WEBVIEW_x).
     *
     * @param contextName context name to switch to
     */
    @When("I switch mobile context to {string}")
    public void iSwitchMobileContextTo(String contextName) {
        mobileAction.switchContext(contextName);
    }

    /**
     * Switch the mobile driver's context back to the native application.
     */
    @When("I switch mobile context to native app")
    public void iSwitchMobileContextToNativeApp() {
        mobileAction.switchToNativeContext();
    }

    /**
     * Grant a specific runtime permission for the app under test.
     *
     * @param permission permission to grant (platform dependent format)
     * @param appId      application package or bundle identifier
     */
    @When("I grant mobile permission {string} for app {string}")
    public void iGrantMobilePermissionForApp(String permission, String appId) {
        mobileAction.grantPermission(appId, permission);
    }

    /**
     * Revoke a specific runtime permission for the app under test.
     *
     * @param permission permission to revoke (platform dependent format)
     * @param appId      application package or bundle identifier
     */
    @When("I revoke mobile permission {string} for app {string}")
    public void iRevokeMobilePermissionForApp(String permission, String appId) {
        mobileAction.revokePermission(appId, permission);
    }

    /**
     * Wait up to the specified number of seconds for an element to become visible.
     *
     * @param seconds maximum wait time in seconds
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @When("I wait up to {int} seconds for mobile page {word} locator {word} to be visible")
    public void iWaitUpToSecondsForMobilePageLocatorToBeVisible(int seconds, String page, String locator) {
        mobileAction.waitForVisible(page, locator, seconds);
    }

    /**
     * Wait up to the specified number of seconds for an element to disappear (not visible).
     *
     * @param seconds maximum wait time in seconds
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @When("I wait up to {int} seconds for mobile page {word} locator {word} to disappear")
    public void iWaitUpToSecondsForMobilePageLocatorToDisappear(int seconds, String page, String locator) {
        mobileAction.waitForNotVisible(page, locator, seconds);
    }

    /**
     * Pause execution for the specified number of seconds.
     *
     * <p>
     * Useful for temporary debugging or when explicit waits are required by the test.
     * </p>
     *
     * @param seconds number of seconds to pause
     */
    @When("I pause mobile execution for {int} seconds")
    public void iPauseMobileExecutionForSeconds(int seconds) {
        mobileAction.pause(seconds);
    }

    /**
     * If a runtime permission popup is displayed, click the "Allow" action.
     *
     * <p>
     * This uses the MobilePermissionHandler bound to the current driver.
     * </p>
     */
    @When("I allow mobile permission popup if displayed")
    public void iAllowMobilePermissionPopupIfDisplayed() {
        permissions().allowIfDisplayed();
    }

    /**
     * If a runtime permission popup is displayed, click the "Deny" action.
     */
    @When("I deny mobile permission popup if displayed")
    public void iDenyMobilePermissionPopupIfDisplayed() {
        permissions().denyIfDisplayed();
    }

    /**
     * If a runtime permission popup is displayed, click a button matching the provided text.
     *
     * @param buttonText exact button text to match and click (platform/localization sensitive)
     */
    @When("I allow mobile permission popup with text {string} if displayed")
    public void iAllowMobilePermissionPopupWithTextIfDisplayed(String buttonText) {
        permissions().allowWithTextIfDisplayed(buttonText);
    }

    /**
     * If any permission popups appear, accept them all (common for granting multiple permissions).
     */
    @When("I allow all mobile permission popups if displayed")
    public void iAllowAllMobilePermissionPopupsIfDisplayed() {
        permissions().allowAllIfDisplayed();
    }

    /**
     * If any permission popups appear, deny them all.
     */
    @When("I deny all mobile permission popups if displayed")
    public void iDenyAllMobilePermissionPopupsIfDisplayed() {
        permissions().denyAllIfDisplayed();
    }

    /**
     * Handle a permission popup using a custom action string if displayed.
     *
     * <p>
     * The action parameter is interpreted by MobilePermissionHandler (for example
     * "allow", "deny", or a specific button label). This method will use the
     * configured permission popup timeout from MobileConfigurationProperties.
     * </p>
     *
     * @param action action to perform (platform-specific interpretation)
     */
    @When("I handle mobile permission popup using action {string} if displayed")
    public void iHandleMobilePermissionPopupUsingActionIfDisplayed(String action) {
        // Use configured timeout for permission popups to avoid hard-coded values here.
        permissions().handleIfDisplayed(action, null, com.ptaf.mobile.config.MobileConfigurationProperties.getPermissionPopupTimeoutSeconds());
    }

    /**
     * Verify that a mobile element is visible. Uses the MobileAssert helper to
     * provide consistent assertion behavior and reporting.
     *
     * @param page    page identifier used by the locator strategy
     * @param locator locator identifier within the page
     */
    @Then("I verify mobile page {word} locator {word} is visible")
    public void iVerifyMobilePageLocatorIsVisible(String page, String locator) {
        MobileAssert.assertVisible(page, locator);
    }

    /**
     * Verify that a mobile element's text contains the expected substring.
     *
     * @param page     page identifier used by the locator strategy
     * @param locator  locator identifier within the page
     * @param expected substring expected to be contained within the element text
     */
    @Then("I verify mobile page {word} locator {word} text contains {string}")
    public void iVerifyMobilePageLocatorTextContains(String page, String locator, String expected) {
        MobileAssert.assertTextContains(page, locator, expected);
    }

    /**
     * Capture a screenshot on the mobile device and save it with a friendly name.
     *
     * <p>
     * The MobileEvidenceManager will associate the screenshot with the current driver
     * and persist it according to the project's evidence storage strategy.
     * </p>
     *
     * @param screenshotName friendly name used for the saved screenshot file
     */
    @When("I capture mobile screenshot named {string}")
    public void iCaptureMobileScreenshotNamed(String screenshotName) {
        MobileEvidenceManager.captureNamedScreenshot(MobileDriverManager.getDriver(), screenshotName);
    }

    /**
     * Verify that the device clipboard contains the expected substring.
     *
     * @param expected substring expected to be present in the clipboard content
     * @throws AssertionError if the clipboard is null or does not contain expected
     */
    @Then("I verify mobile clipboard text contains {string}")
    public void iVerifyMobileClipboardTextContains(String expected) {
        // Retrieve clipboard content via MobileAction and assert the expected substring.
        String actual = mobileAction.getClipboard();
        if (actual == null || !actual.contains(expected))
            throw new AssertionError("Expected mobile clipboard to contain [" + expected + "] but was [" + actual + "]");
    }
}
