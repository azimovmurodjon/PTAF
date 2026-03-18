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

public class NewPageCommonSteps {

    private static final String PLAID_FRAME = "iframe[title='Plaid Link']";
    private static final String POP_FRAME = "//*[@id='AcceptUIContainer']/iframe";
    private static final String ATOMIC_FRAME = "#atomic-transact-iframe";

    public static String trackingID = null;

    public NewPageCommonSteps() {
    }

    private Page getActivePage() {
        return Hooks.getPage();
    }

    private FrameCommonMethods getFrameCommonMethods() {
        return new FrameCommonMethods(getActivePage());
    }

    private PageCommonMethods getPageCommonMethods() {
        return new PageCommonMethods(getActivePage());
    }

    @Then("^we click (.*?) locator (.*?) and switch to popup$")
    public void weSwitchToPopup(String element, String locator) {
        Page currentPage = getActivePage();
        PageCommonMethods pageCommonMethods = getPageCommonMethods();

        Page popupPage = currentPage.waitForPopup(() ->
                pageCommonMethods.click(currentPage,  element, locator)
        );

        popupPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
        Hooks.setPage(popupPage);
    }

    // ============================================================================================================
    // NEW PAGE ACTIONS
    // ============================================================================================================

    @Then("^we click on new page (.*?) locator (.*?)$")
    public void weClickActionNewOnPage(String element, String locator) {
        getFrameCommonMethods().click(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we double click on new page (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPage(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we enter value on new page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPage(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), null, null, null, element, locator, value);
    }

    @Then("^we select on new page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPage(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), null, null, null, element, locator, value);
    }

    @Then("^we check on new page (.*?) locator (.*?)$")
    public void weCheckActionOnPage(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we uncheck on new page (.*?) locator (.*?)$")
    public void weUncheckActionOnPage(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we hover on new page (.*?) locator (.*?)$")
    public void weHoverActionOnPage(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we type on new page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPage(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), null, null, null, element, locator, value);
    }

    @Then("^we scroll on new page (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPage(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we clear value on new page (.*?) locator (.*?)$")
    public void weClearValueOnPage(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we verify on new page (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPageLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we verify on new page (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPageLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we verify on new page (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnPageLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we get value on new page (.*?) locator (.*?)$")
    public void weGetValueOnPage(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we verify element has value on new page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPageLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), null, null, null, element, locator, value);
    }

    @Then("^we verify on new page (.*?) of locator (.*?) is existed$")
    public void weVerifyOnPageLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we verify on new page (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnPageLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), null, null, null, element, locator);
    }

    @Then("^we contain on new page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPageLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().contain(getActivePage(), null, null, null, element, locator, value);
    }

    @Then("^we get text on new page (.*?) locator (.*?)$")
    public void weGetTextOnPage(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), null, null, null, element, locator);
    }

    @And("^we capture screenshot on new page (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPage(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), null, null, null, element, locator, filePath);
    }

    @And("^we press on new page (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPageKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), null, null, null, element, locator, value);
    }

    @When("^we click radio on new page (.*?) list locator (.*?)$")
    public void clickRadioOnPage(String element, String locator) {
        getFrameCommonMethods().clickRadioButton(getActivePage(), null, element, locator);
    }

    @Then("^we get text and contain on new page (.*?) locator (.*?)$")
    public void weGetTextAndContainOnPage(String element, String locator) {
        getFrameCommonMethods().get_and_contain_text(getActivePage(), null, null, null, element, locator);
    }

    @Then("^get consumer tracking ID and contain on new page$")
    public void weGetConsumerTrackingIDAndContainOnPage() {
        Locator trCode = getActivePage().locator("//div[contains(text(),'Confirmation ID:')]");
        trackingID = trCode.innerText();
        System.out.println("Tracking Code is: " + trackingID);
    }

    // ============================================================================================================
    // PLAID FRAME ACTIONS
    // ============================================================================================================

    @Then("^we click on plaid frame (.*?) locator (.*?)$")
    public void weClickActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().click(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we double click on plaid frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we enter value on plaid frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPlaidFrame(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    @Then("^we select on plaid frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPlaidFrame(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    @Then("^we check on plaid frame (.*?) locator (.*?)$")
    public void weCheckActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we uncheck on plaid frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we hover on plaid frame (.*?) locator (.*?)$")
    public void weHoverActionOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we type on plaid frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPlaidFrame(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    @Then("^we scroll on plaid frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we clear value on plaid frame (.*?) locator (.*?)$")
    public void weClearValueOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we verify on plaid frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPlaidFrameLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we verify on plaid frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPlaidFrameLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we verify on plaid frame (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnPlaidFrameLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we get value on plaid frame (.*?) locator (.*?)$")
    public void weGetValueOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we verify element has value on plaid frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPlaidFrameLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    @Then("^we verify on plaid frame (.*?) of locator (.*?) is existed$")
    public void weVerifyOnPlaidFrameLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we verify on plaid frame (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnPlaidFrameLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @Then("^we contain on plaid frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPlaidFrameLocatorValue(String element, String locator, String expectedText) {
        getFrameCommonMethods().contain(getActivePage(), PLAID_FRAME, null, null, element, locator, expectedText);
    }

    @Then("^we get text on plaid frame (.*?) locator (.*?)$")
    public void weGetTextOnPlaidFrame(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), PLAID_FRAME, null, null, element, locator);
    }

    @And("^we capture screenshot on plaid frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPlaidFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), PLAID_FRAME, null, null, element, locator, filePath);
    }

    @And("^we press on plaid frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPlaidFrameKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), PLAID_FRAME, null, null, element, locator, value);
    }

    // ============================================================================================================
    // POP FRAME ACTIONS
    // ============================================================================================================

    @Then("^we click on pop frame (.*?) locator (.*?)$")
    public void weClickActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().click(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we double click on pop frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we enter value on pop frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPopFrame(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    @Then("^we select on pop frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPopFrame(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    @Then("^we check on pop frame (.*?) locator (.*?)$")
    public void weCheckActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we uncheck on pop frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we hover on pop frame (.*?) locator (.*?)$")
    public void weHoverActionOnPopFrame(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we type on pop frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPopFrame(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    @Then("^we scroll on pop frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPopFrame(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we clear value on pop frame (.*?) locator (.*?)$")
    public void weClearValueOnPopFrame(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we verify on pop frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPopFrameLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we verify on pop frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPopFrameLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we verify on pop frame (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnPopFrameLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we get value on pop frame (.*?) locator (.*?)$")
    public void weGetValueOnPopFrame(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we verify element has value on pop frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPopFrameLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    @Then("^we verify on pop frame (.*?) of locator (.*?) is existed$")
    public void weVerifyOnPopFrameLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we verify on pop frame (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnPopFrameLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @Then("^we contain on pop frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPopFrameLocatorValue(String element, String locator, String expectedText) {
        getFrameCommonMethods().contain(getActivePage(), POP_FRAME, null, null, element, locator, expectedText);
    }

    @Then("^we get text on pop frame (.*?) locator (.*?)$")
    public void weGetTextOnPopFrame(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), POP_FRAME, null, null, element, locator);
    }

    @And("^we capture screenshot on pop frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPopFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), POP_FRAME, null, null, element, locator, filePath);
    }

    @And("^we press on pop frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPopFrameKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), POP_FRAME, null, null, element, locator, value);
    }

    // ============================================================================================================
    // ATOMIC FRAME ACTIONS
    // ============================================================================================================

    @Then("^we click on atomic frame (.*?) locator (.*?)$")
    public void weClickActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().click(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we double click on atomic frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().dblclick(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we enter value on atomic frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnAtomicFrame(String element, String locator, String value) {
        getFrameCommonMethods().fill(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    @Then("^we select on atomic frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnAtomicFrame(String element, String locator, String value) {
        getFrameCommonMethods().select(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    @Then("^we check on atomic frame (.*?) locator (.*?)$")
    public void weCheckActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().check(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we uncheck on atomic frame (.*?) locator (.*?)$")
    public void weUncheckActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().uncheck(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we hover on atomic frame (.*?) locator (.*?)$")
    public void weHoverActionOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().hover(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we type on atomic frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnAtomicFrame(String element, String locator, String value) {
        getFrameCommonMethods().type(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    @Then("^we scroll on atomic frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().scroll(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we clear value on atomic frame (.*?) locator (.*?)$")
    public void weClearValueOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().clear(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we verify on atomic frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnAtomicFrameLocatorIsVisible(String element, String locator) {
        getFrameCommonMethods().isvisible(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we verify on atomic frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnAtomicFrameLocatorIsChecked(String element, String locator) {
        getFrameCommonMethods().ischecked(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we verify on atomic frame (.*?) of locator (.*?) is enabled$")
    public void weVerifyOnAtomicFrameLocatorIsEnabled(String element, String locator) {
        getFrameCommonMethods().isenabled(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we get value on atomic frame (.*?) locator (.*?)$")
    public void weGetValueOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().getvalue(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we verify element has value on atomic frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnAtomicFrameLocatorValue(String element, String locator, String value) {
        getFrameCommonMethods().hasvalue(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }

    @Then("^we verify on atomic frame (.*?) of locator (.*?) is existed$")
    public void weVerifyOnAtomicFrameLocatorIsExisted(String element, String locator) {
        getFrameCommonMethods().exists(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we verify on atomic frame (.*?) of locator (.*?) is not existed$")
    public void weVerifyOnAtomicFrameLocatorIsNotExisted(String element, String locator) {
        getFrameCommonMethods().not_exists(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @Then("^we contain on atomic frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnAtomicFrameLocatorValue(String element, String locator, String expectedText) {
        getFrameCommonMethods().contain(getActivePage(), ATOMIC_FRAME, null, null, element, locator, expectedText);
    }

    @Then("^we get text on atomic frame (.*?) locator (.*?)$")
    public void weGetTextOnAtomicFrame(String element, String locator) {
        getFrameCommonMethods().gettext(getActivePage(), ATOMIC_FRAME, null, null, element, locator);
    }

    @And("^we capture screenshot on atomic frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnAtomicFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        getFrameCommonMethods().screenshot(getActivePage(), ATOMIC_FRAME, null, null, element, locator, filePath);
    }

    @And("^we press on atomic frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnAtomicFrameKey(String element, String locator, String value) {
        getFrameCommonMethods().press(getActivePage(), ATOMIC_FRAME, null, null, element, locator, value);
    }
}