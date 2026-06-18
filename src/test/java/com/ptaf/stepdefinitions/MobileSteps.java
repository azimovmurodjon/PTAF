package com.ptaf.stepdefinitions;

import com.ptaf.mobile.assertions.MobileAssert;
import com.ptaf.mobile.config.MobilePlatform;
import com.ptaf.mobile.drivers.MobileDriverManager;
import com.ptaf.mobile.evidence.MobileEvidenceManager;
import com.ptaf.mobile.implementation.MobileActionImpl;
import com.ptaf.mobile.interfaces.MobileAction;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Cucumber step definitions for PTAF native mobile Appium automation.
 */
public class MobileSteps {
    private final MobileAction mobileAction = new MobileActionImpl();

    @Given("I start mobile application using platform {string}")
    public void iStartMobileApplicationUsingPlatform(String platform) {
        MobileDriverManager.startDriver(MobilePlatform.from(platform));
    }

    @When("I tap on mobile page {word} locator {word}")
    public void iTapOnMobilePageLocator(String page, String locator) {
        mobileAction.tap(page, locator);
    }

    @When("I long press mobile page {word} locator {word} for {int} milliseconds")
    public void iLongPressMobileElement(String page, String locator, int durationMillis) {
        mobileAction.longPress(page, locator, durationMillis);
    }

    @When("I double tap mobile page {word} locator {word}")
    public void iDoubleTapMobileElement(String page, String locator) {
        mobileAction.doubleTap(page, locator);
    }

    @When("I tap mobile screen at x {int} y {int}")
    public void iTapMobileScreenAt(int x, int y) {
        mobileAction.tapAt(x, y);
    }

    @When("I drag mobile page {word} locator {word} to page {word} locator {word}")
    public void iDragMobileElement(String fromPage, String fromLocator, String toPage, String toLocator) {
        mobileAction.drag(fromPage, fromLocator, toPage, toLocator);
    }

    @When("I scroll mobile page {word} locator {word} into view with max {int} swipes")
    public void iScrollMobileElementIntoView(String page, String locator, int maxSwipes) {
        mobileAction.scrollUntilVisible(page, locator, maxSwipes);
    }

    @When("I scroll mobile screen to text {string}")
    public void iScrollMobileScreenToText(String text) {
        mobileAction.scrollToText(text);
    }

    @When("I enter mobile value {string} on page {word} locator {word}")
    public void iEnterMobileValueOnPageLocator(String value, String page, String locator) {
        mobileAction.type(page, locator, value);
    }

    @When("I clear mobile page {word} locator {word}")
    public void iClearMobilePageLocator(String page, String locator) {
        mobileAction.clear(page, locator);
    }

    @When("I hide mobile keyboard")
    public void iHideMobileKeyboard() {
        mobileAction.hideKeyboard();
    }

    @When("I background mobile app for {int} seconds")
    public void iBackgroundMobileAppForSeconds(int seconds) {
        mobileAction.backgroundApp(seconds);
    }

    @When("I swipe mobile screen up")
    public void iSwipeMobileScreenUp() {
        mobileAction.swipeUp();
    }

    @When("I swipe mobile screen down")
    public void iSwipeMobileScreenDown() {
        mobileAction.swipeDown();
    }

    @When("I swipe mobile screen left")
    public void iSwipeMobileScreenLeft() {
        mobileAction.swipeLeft();
    }

    @When("I swipe mobile screen right")
    public void iSwipeMobileScreenRight() {
        mobileAction.swipeRight();
    }

    @When("I pinch in mobile screen")
    public void iPinchInMobileScreen() {
        mobileAction.pinchIn();
    }

    @When("I zoom out mobile screen")
    public void iZoomOutMobileScreen() {
        mobileAction.zoomOut();
    }

    @When("I rotate mobile screen to {string}")
    public void iRotateMobileScreenTo(String orientation) {
        mobileAction.setOrientation(orientation);
    }

    @When("I rotate mobile screen using configured orientation")
    public void iRotateMobileScreenUsingConfiguredOrientation() {
        mobileAction.setConfiguredOrientation();
    }

    @When("I activate mobile app {string}")
    public void iActivateMobileApp(String appId) {
        mobileAction.activateApp(appId);
    }

    @When("I terminate mobile app {string}")
    public void iTerminateMobileApp(String appId) {
        mobileAction.terminateApp(appId);
    }

    @When("I open mobile deep link {string} for app {string}")
    public void iOpenMobileDeepLink(String url, String appId) {
        mobileAction.openDeepLink(url, appId);
    }

    @When("I push local file {string} to mobile path {string}")
    public void iPushLocalFileToMobilePath(String localPath, String remotePath) {
        mobileAction.pushFile(remotePath, localPath);
    }

    @When("I pull mobile file {string} to local path {string}")
    public void iPullMobileFileToLocalPath(String remotePath, String localPath) {
        mobileAction.pullFile(remotePath, localPath);
    }

    @When("I set mobile clipboard text {string}")
    public void iSetMobileClipboardText(String text) {
        mobileAction.setClipboard(text);
    }

    @When("I switch mobile context to {string}")
    public void iSwitchMobileContextTo(String contextName) {
        mobileAction.switchContext(contextName);
    }

    @When("I switch mobile context to native app")
    public void iSwitchMobileContextToNativeApp() {
        mobileAction.switchToNativeContext();
    }

    @When("I grant mobile permission {string} for app {string}")
    public void iGrantMobilePermissionForApp(String permission, String appId) {
        mobileAction.grantPermission(appId, permission);
    }

    @When("I revoke mobile permission {string} for app {string}")
    public void iRevokeMobilePermissionForApp(String permission, String appId) {
        mobileAction.revokePermission(appId, permission);
    }

    @Then("I verify mobile page {word} locator {word} is visible")
    public void iVerifyMobilePageLocatorIsVisible(String page, String locator) {
        MobileAssert.assertVisible(page, locator);
    }

    @Then("I verify mobile page {word} locator {word} text contains {string}")
    public void iVerifyMobilePageLocatorTextContains(String page, String locator, String expected) {
        MobileAssert.assertTextContains(page, locator, expected);
    }

    @When("I capture mobile screenshot named {string}")
    public void iCaptureMobileScreenshotNamed(String screenshotName) {
        MobileEvidenceManager.captureNamedScreenshot(MobileDriverManager.getDriver(), screenshotName);
    }

    @Then("I verify mobile clipboard text contains {string}")
    public void iVerifyMobileClipboardTextContains(String expected) {
        String actual = mobileAction.getClipboard();
        if (actual == null || !actual.contains(expected))
            throw new AssertionError("Expected mobile clipboard to contain [" + expected + "] but was [" + actual + "]");
    }
}
