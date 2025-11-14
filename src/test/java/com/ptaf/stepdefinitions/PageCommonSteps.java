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

public class PageCommonSteps {
    private final Page page = Hooks.getPage();
    private final PageCommonMethods pageCommonMethods = new PageCommonMethods(page);
    private static final Logger logger = LoggerFactory.getLogger(PageCommonSteps.class);
    private final BrowserContext browserContext = Hooks.getContext();

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

    @Then("^we click on page (.*?) locator (.*?)$")
    public void weClickActionOnPage(String element, String locator) {
        pageCommonMethods.click(page, element, locator);
    }

    @Then("^we double click on page (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPage(String element, String locator) {
        pageCommonMethods.dblclick(page, element, locator);
    }

    @Then("^we enter value on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPage(String element, String locator, String value) {
        pageCommonMethods.fill(page, element, locator, value);
    }

    @Then("^we select on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPage(String element, String locator, String value) {
        pageCommonMethods.select(page, element, locator, value);
    }

    @Then("^we check on page (.*?) locator (.*?)$")
    public void weCheckActionOnPage(String element, String locator) {
        pageCommonMethods.check(page, element, locator);
    }

    @Then("^we uncheck on page (.*?) locator (.*?)$")
    public void weUncheckActionOnPage(String element, String locator) {
        pageCommonMethods.check(page, element, locator);
    }

    @Then("^we hover on page (.*?) locator (.*?)$")
    public void weHoverActionOnPage(String element, String locator) {
        pageCommonMethods.hover(page, element, locator);
    }

    @Then("^we type on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPage(String element, String locator, String value) {
        pageCommonMethods.type(page, element, locator, value);
    }

    @Then("^we scroll on page (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPage(String element, String locator) {
        pageCommonMethods.scroll(page, element, locator);
    }

    @Then("^we clear value on page (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnPage(String element, String locator) {
        pageCommonMethods.clear(page, element, locator);
    }

    @Then("^we verify on page (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPageLocatorIsVisible(String element, String locator) {
        pageCommonMethods.isvisible(page, element, locator);
    }

    @Then("^we verify on page (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPageLocatorIsChecked(String element, String locator) {
        pageCommonMethods.ischecked(page, element, locator);
    }

    @Then("^we verify on page (.*?) of locator (.*?) is enabled")
    public void weVerifyOnPageLocatorIsEnabled(String element, String locator) {
        pageCommonMethods.isenabled(page, element, locator);
    }

    @Then("^we verify on page (.*?) of locator (.*?) is existed")
    public void weVerifyOnPageLocatorIsExisted(String element, String locator) {
        pageCommonMethods.exists(page, element, locator);
    }

    @Then("^we contain on page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPageLocatorValue(String element, String locator, String value) {
        pageCommonMethods.contain(page, element, locator, value);
    }

    @Then("^we get text on page (.*?) locator (.*?)$")
    public void weGetTextOnPage(String element, String locator) {
        pageCommonMethods.gettext(page, element, locator);
    }

    @Then("^we get value on page (.*?) locator (.*?)$")
    public void weGetValueOnPage(String element, String locator) {
        pageCommonMethods.getvalue(page, element, locator);
    }

    @Then("^we has value on page (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPageLocatorValue(String element, String locator, String value) {
        pageCommonMethods.hasvalue(page, element, locator, value);
    }

    @Then("^we get list of elements on page (.*?) locator (.*?)$")
    public void weGetListOfElementsOnPage(String element, String locator) {
        pageCommonMethods.getListOfElements(page, element, locator);
    }

    @Then("^we get text of elements on page (.*?) locator (.*?)$")
    public void weGetTextOfElementsOnPage(String element, String locator) {
        String value = pageCommonMethods.gettext(page, element, locator);
        System.out.println("Value: " + value);
    }

    @When("we click radio on page (.*?) list locator (.*?)$")
    public void clickRadioOnPage(String element, String locator) {
        pageCommonMethods.clickRadioButton(page, element, locator);
    }

    @And("^we capture screenshot on page (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPage(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        pageCommonMethods.screenshot(page, element, locator, filePath);
    }

    @And("^we press on page (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPageKey(String element, String locator, String value) {
        pageCommonMethods.press(page, element, locator, value);
    }

    @And("^we click download on page (.*?) locator (.*?)$")
    public void weDownloadOnPageKey(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        pageCommonMethods.download(page, element, locator, filePath + ".jpeg");
    }

    @And("^we select document to upload on page (.*?) locator (.*?)$")
    public void weSelectDocument(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        pageCommonMethods.selectFile(page, element, locator, "Mobile Automation Platforms.docx");
    }

    @Given("^get title of page$")
    public void getTitleOfPage() {
        String title = page.title();
        logger.info("Page title: {}", title);
    }

    @And("^we wait for some time$")
    public void weWaitForSomeTime() throws InterruptedException {
        Thread.sleep(3000);
    }

    @Then("^Stop Execution")
    public void stopExecution() throws Exception
    {
        Thread.sleep(30000);

    }

    @Then("^we close all browsers$")
    public void weCloseAllBrowsers() throws Exception{
        Hooks.closeBrowserResources();
    }
}