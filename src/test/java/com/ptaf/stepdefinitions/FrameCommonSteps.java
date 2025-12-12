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

public class FrameCommonSteps {
    private final Page page;
    private static Page iframePage;
    private final String iFrame = "#frame1";
    private final String iFrame_2 = "iframe";
    private final String iFrame_3 = null;
    private final FrameCommonMethods frameCommonMethods;

    public FrameCommonSteps() {
        this.page = Hooks.getPage(); // Retrieve the Page instance from Hooks
        this.frameCommonMethods = new FrameCommonMethods(iframePage); // Initialize with the Page instance
    }

    public void switchToIframe() {
        iframePage = page.waitForPopup(() -> {
            page.frameLocator("iframe").getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("Continue")).click();
        });
    }

    @Given("^we navigate to (.*?) url$")
    public void weNavigateToURL(String URL) {
        page.navigate(ConfigurationProperties.getBaseUrl(URL));
//        page.setViewportSize(1920, 1080);
//        switchToIframe();
    }

    @Then("^we click on frame (.*?) locator (.*?)$")
    public void weClickActionOnPage(String element, String locator) {
        frameCommonMethods.click(page, iFrame, null, null, element, locator);
    }

    @Then("^we double click on frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPage(String element, String locator) {
        frameCommonMethods.dblclick(page, iFrame, null, null, element, locator);
    }

    @Then("^we enter value on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPage(String element, String locator, String value) {
        frameCommonMethods.fill(page, iFrame, null, null,element, locator, value);
    }

    @Then("^we select on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPage(String element, String locator, String value) {
        frameCommonMethods.select(page, iFrame, null, null, element, locator, value);
    }

    @Then("^we check on frame (.*?) locator (.*?)$")
    public void weCheckActionOnPage(String element, String locator) {
        frameCommonMethods.check(page, iFrame, null, null, element, locator);
    }

    @Then("^we uncheck on frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPage(String element, String locator) {
        frameCommonMethods.check(page, iFrame, null, null, element, locator);
    }

    @Then("^we hover on frame (.*?) locator (.*?)$")
    public void weHoverActionOnPage(String element, String locator) {
        frameCommonMethods.hover(page, iFrame, null, null, element, locator);
    }

    @Then("^we type on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPage(String element, String locator, String value) {
        frameCommonMethods.type(page, iFrame, null, null, element, locator, value);
    }

    @Then("^we scroll on frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPage(String element, String locator) {
        frameCommonMethods.scroll(page, iFrame, null, null, element, locator);
    }

    @Then("^we clear value on frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnPage(String element, String locator) {
        frameCommonMethods.clear(page, iFrame, null, null, element, locator);
    }

    @Then("^we verify on frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPageLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(page, iFrame, null, null, element, locator);
    }

    @Then("^we verify on frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPageLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(page, iFrame, null, null, element, locator);
    }

    @Then("^we verify on frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnPageLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(page, iFrame, null, null, element, locator);
    }

    @Then("^we verify on frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnPageLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(page, iFrame, null, null, element, locator);
    }

    @Then("^we contain on frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPageLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(page, iFrame, null, null, element, locator, value);
    }

    @Then("^we get text on frame (.*?) locator (.*?)$")
    public void weGetTextOnPage(String element, String locator) {
        String value = frameCommonMethods.gettext(page, iFrame, null, null, element, locator);
        System.out.println("Value: " + value);
    }

    @Then("^we has value on frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnNewPageLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(page, iFrame, null, null, element, locator, value);
    }

    @Then("^we get list of elements on frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnNewPage(String element, String locator) {
        frameCommonMethods.gettext(page, iFrame, null, null, element, locator);
    }

    @When("we click radio on frame (.*?) list locator (.*?)$")
    public void clickRadioOnNewPage(String element, String locator) {
        frameCommonMethods.clickRadioButton(page, iFrame, element, locator);
    }

    @And("^we capture screenshot on frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPage(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(page, iFrame, null, null, element, locator, filePath);
    }

    @And("^we press on frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPageKey(String element, String locator, String value) {
        frameCommonMethods.press(page, iFrame, null, null, element, locator, value);
    }

    @Then("^we click on second frame (.*?) locator (.*?)$")
    public void weClickActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.click(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we double click on second frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.dblclick(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we enter value on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.fill(page, iFrame, iFrame_2, null,element, locator, value);
    }

    @Then("^we select on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.select(page, iFrame, iFrame_2, null, element, locator, value);
    }

    @Then("^we check on second frame (.*?) locator (.*?)$")
    public void weCheckActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.check(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we uncheck on second frame (.*?) locator (.*?)$")
    public void weUncheckActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.check(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we hover on second frame (.*?) locator (.*?)$")
    public void weHoverActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.hover(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we type on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.type(page, iFrame, iFrame_2, null, element, locator, value);
    }

    @Then("^we scroll on second frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnSecondFrame(String element, String locator) {
        frameCommonMethods.scroll(page, iFrame, null, null, element, locator);
    }

    @Then("^we clear value on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnSecondFrame(String element, String locator) {
        frameCommonMethods.clear(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnSecondFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnSecondFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnSecondFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnSecondFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(page, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we contain on second frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnSecondFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(page, iFrame, iFrame_2, null, element, locator, value);
    }

    @Then("^we get text on second frame (.*?) locator (.*?)$")
    public void weGetTextOnSecondFrame(String element, String locator) {
        String value = frameCommonMethods.gettext(page, iFrame, iFrame_2, null, element, locator);
        System.out.println("Value: " + value);
    }

    @And("^we capture screenshot on second frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnSecondFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(page, iFrame, iFrame_2, null, element, locator, filePath);
    }

    @And("^we press on second frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnSecondFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(page, iFrame, iFrame_2, null, element, locator, value);
    }
}