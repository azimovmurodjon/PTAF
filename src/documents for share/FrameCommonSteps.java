package com.ptaf.ui.stepdefinitions;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.pages.FrameCommonMethods;
import com.ptaf.utils.ConfigurationProperties;
import com.ptaf.utils.ExcelReader;
import com.ptaf.utils.ExcelWriter;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Random;

public class FrameCommonSteps {
    private final Page page;
    /**
     * Scenario-scoped active page used by all frame actions. It starts as the browser page
     * created by Hooks and becomes the popup page only after an explicit frame/screen switch.
     * It must not be static: a static Page can point to a browser deliberately closed by a
     * preceding @LastScenario scenario.
     */
    private Page iframePage;
    private final Random rand = new Random();
    private static final String filePath = ConfigurationProperties.getExcelDocumentLocation();
    private final String iFrame = "iframe[name='iframeApplicationContent']";
    private final String iFrame_2 = "iframe[name='iframeContent']";
    private final String iFrame_5 = "iframe[name='DueDiligenceForm']";
    private final String iFrame_3 = "iframe[name='iframeProductsContent']";
    private final String iFrame_4 = "iframe[name='DueDiligenceForm']";
    private final String Teller_Frame = "iframe[name='iframeTellerContent']";
    private final String warning_box = null;
    private final String header = null;
    private final String pop_up = "(//iframe[starts-with(@name,'iframeWindowModal') and not(@style='display:none')])[last()]";
    private final FrameCommonMethods frameCommonMethods;

    public FrameCommonSteps() {
        this.page = Hooks.getPage();
        if (this.page == null || this.page.isClosed()) {
            throw new IllegalStateException(
                "No active Playwright page is available for frame actions. " +
                "Ensure navigation occurs after the browser setup hook has created the scenario page."
            );
        }
        // Most Argo frame steps run inside iframes on the initial browser page. Initialise the
        // active frame page to that fresh page so all existing frame steps keep their behavior.
        this.iframePage = this.page;
        this.frameCommonMethods = new FrameCommonMethods(this.page);
    }

    public void switchToIframe() {
        Page sourcePage = iframePage;
        if (sourcePage == null || sourcePage.isClosed()) {
            sourcePage = Hooks.getPage();
        }
        if (sourcePage == null || sourcePage.isClosed()) {
            throw new IllegalStateException("Cannot switch frame/screen because the active Playwright page is closed.");
        }

        // A frame/screen transition popup must not be resized by the generic maximize listener;
        // resizing it during creation can invalidate its frame context before the next action.
        Hooks.suppressNextPopupAutoMaximize();
        Page currentPage = sourcePage;
        iframePage = currentPage.waitForPopup(() -> {
            currentPage.frameLocator("iframe")
                .getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions().setName("Continue"))
                .click();
        });
        iframePage.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        // Keep all later step definitions in the scenario aligned with the new popup page.
        Hooks.setPage(iframePage);
    }

    @Given("^we navigate to (.*?) url$")
    public void weNavigateToURL(String URL) {
        page.navigate(ConfigurationProperties.getBaseUrl(URL));
//        page.setViewportSize(1920, 1080);
//        switchToIframe();
    }

    @Then("^we click on main frame (.*?) locator (.*?)$")
    public void weClickActionOnMainFrame(String element, String locator) throws InterruptedException {
        frameCommonMethods.click(iframePage, iFrame, null, null, element, locator);
        Thread.sleep(1000);
    }

    @Then("^we report list of selection on main frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnMainFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we double click on main frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnMainFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we enter value on main frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnMainFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, iFrame, null, null, element, locator, value);
    }

    @Then("^we enter random value on main frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterRandomValueOnMainFrame(String element, String locator, String value) {
        int randomVal = rand.nextInt(999999);
        String new_value = value + randomVal;
        frameCommonMethods.fill(iframePage, iFrame, null, null, element, locator, new_value);
    }

    @Then("^we select on main frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnMainFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, iFrame, null, null, element, locator, value);
    }

    @Then("^we check on main frame (.*?) locator (.*?)$")
    public void weCheckActionOnMainFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we uncheck on main frame (.*?) locator (.*?)$")
    public void weUncheckActionOnMainFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we hover on main frame (.*?) locator (.*?)$")
    public void weHoverActionOnMainFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we type on main frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnMainFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, iFrame, null, null, element, locator, value);
    }

    @Then("^we scroll on main frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnMainFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we clear value on main frame (.*?) locator (.*?)$")
    public void weClearValueOnMainFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we verify on main frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnMainFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we verify on main frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnMainFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we verify on main frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnMainFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we verify on main frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnMainFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we contain on main frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnMainFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, iFrame, null, null, element, locator, value);
    }

    @Then("^we get text on main frame (.*?) locator (.*?)$")
    public void weGetTextOnMainFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we has value on main frame (.*?) of locator (.*?)$")
    public void weHasValueOnMainFrameLocatorValue(String element, String locator) {
        frameCommonMethods.hasvalue(iframePage, iFrame, null, null, element, locator, null);
    }

    @Then("^we get value of elements on main frame (.*?) locator (.*?)$")
    public void weGetValueOfElementsOnMainFrame(String element, String locator) {
        frameCommonMethods.getvalue(iframePage, iFrame, null, null, element, locator);
    }

    @Then("^we get list of elements on main frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnMainFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, null, null, element, locator);
    }

    @When("we click radio on main frame (.*?) list locator (.*?)$")
    public void clickRadioOnMainFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, iFrame, element, locator);
    }

    @And("^we capture screenshot on main frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnMainFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, iFrame, null, null, element, locator, filePath);
    }

    @And("^we press on main frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnMainFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, iFrame, null, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) main frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseMainFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        System.out.println(finalValue);
        frameCommonMethods.fill(iframePage, iFrame, null, null, element, locator, finalValue);
    }

    @Then("^we write to excel for testCase (.*?) column \"(.*?)\" main frame element (.*?) locator (.*?)$")
    public void weWriteToExcelForTestCaseOnMainFrame(String testCaseId, String columnName, String element, String locator) {
        String valueToWrite = frameCommonMethods.gettext(iframePage, iFrame, null, null, element, locator);
        ExcelWriter.writeData(filePath, testCaseId, columnName, valueToWrite);
    }

    @Then("^we get text and contain on main frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnMainFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, iFrame, null, null, element, locator);
    }

    @And("^we download on main frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnMainFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, iFrame, null, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for main frame (.*?) locator (.*?)$")
    public void weSelectFileForMainFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,iFrame, null,null, element, locator, filePath);
    }

//      ________________________________________________________________________________________________________________

    @Then("^we click on second frame (.*?) locator (.*?)$")
    public void weClickActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we report list of selection on second frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnSecondFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we double click on second frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we enter value on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, iFrame, iFrame_2, null, element, locator, value);
    }

    @Then("^we select on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, iFrame, iFrame_2, null, element, locator, value);
    }

    @Then("^we check on second frame (.*?) locator (.*?)$")
    public void weCheckActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we uncheck on second frame (.*?) locator (.*?)$")
    public void weUncheckActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we hover on second frame (.*?) locator (.*?)$")
    public void weHoverActionOnSecondFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we type on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnSecondFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, iFrame, iFrame_2, null, element, locator, value);
    }

    @Then("^we scroll on second frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnSecondFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we clear value on second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnSecondFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnSecondFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnSecondFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnSecondFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we verify on second frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnSecondFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we contain on second frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnSecondFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, iFrame, null, null, element, locator, value);
    }

    @Then("^we get text on second frame (.*?) locator (.*?)$")
    public void weGetTextOnSecondFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we has value on second frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnSecondFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, iFrame, iFrame_2, null, element, locator, value);
    }

    @Then("^we get value of elements on second frame (.*?) locator (.*?)$")
    public void weGetValueOfElementsOnSecondFrame(String element, String locator) {
        frameCommonMethods.getvalue(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we get list of elements on second frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnSecondFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @When("we click radio on second frame (.*?) list locator (.*?)$")
    public void clickRadioOnSecondFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, iFrame, element, locator);
    }

    @And("^we capture screenshot on second frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnSecondFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, iFrame, iFrame_2, null, element, locator, filePath);
    }

    @And("^we press on second frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnSecondFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, iFrame, iFrame_2, null, element, locator, value);
    }

    @And("^we download on second frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnSecondFrame(String element, String locator, String fileType) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String finalFileType = "." + fileType;
        frameCommonMethods.download(iframePage, iFrame, iFrame_2, null, element, locator, filePath + finalFileType);
    }

    @Then("^we enter using excel data on testCase (.*?) second frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseSecondFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, iFrame, iFrame_2, null, element, locator, finalValue);
    }

    @Then("^we write to excel for testCase (.*?) column \"(.*?)\" second frame element (.*?) locator (.*?)$")
    public void weWriteToExcelForTestCaseOnSecondFrame(String testCaseId, String columnName, String element, String locator) {
        String valueToWrite = frameCommonMethods.gettext(iframePage, iFrame, iFrame_2, null, element, locator);
        System.out.println(valueToWrite);
        ExcelWriter.writeData(filePath, testCaseId, columnName, valueToWrite);
    }

    @Then("^we get text and contain on second frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnSecondFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, iFrame, iFrame_2, null, element, locator);
    }

    @Then("^we select file: (.*?) for second frame (.*?) locator (.*?)$")
    public void weSelectFileForSecondFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,iFrame, iFrame_2,null, element, locator, filePath);
    }
    //    __________________________________________________________________________________________________________________
    @Then("^we click on teller frame (.*?) locator (.*?)$")
    public void weClickActionOnTellerFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we report list of selection on teller frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnTellerLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we double click on teller frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnTellerFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we enter value on teller frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnTellerFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, iFrame, Teller_Frame, null, element, locator, value);
    }

    @Then("^we select on teller frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnTellerFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, iFrame, Teller_Frame, null, element, locator, value);
    }

    @Then("^we check on teller frame (.*?) locator (.*?)$")
    public void weCheckActionOnTellerFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we uncheck on teller frame (.*?) locator (.*?)$")
    public void weUncheckActionOnTellerFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we hover on teller frame (.*?) locator (.*?)$")
    public void weHoverActionOnTellerFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we type on teller frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnTellerFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, iFrame, Teller_Frame, null, element, locator, value);
    }

    @Then("^we scroll on teller frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnTellerFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we clear value on teller frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnTellerFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we verify on teller frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnTellerFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we verify on teller frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnTellerFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we verify on teller frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnTellerFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we verify on teller frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnTellerFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we contain on teller frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnTellerFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, iFrame, Teller_Frame, null, element, locator, value);
    }

    @Then("^we get text on teller frame (.*?) locator (.*?)$")
    public void weGetTextOnTellerFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @Then("^we has value on teller frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnTellerFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, iFrame, Teller_Frame, null, element, locator, value);
    }

    @Then("^we get list of elements on teller frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnTellerFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @When("we click radio on teller frame (.*?) list locator (.*?)$")
    public void clickRadioOnTellerFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, Teller_Frame, element, locator);
    }

    @And("^we capture screenshot on teller frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnTellerFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, iFrame, Teller_Frame, null, element, locator, filePath);
    }

    @And("^we press on teller frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnTellerFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, iFrame, Teller_Frame, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) teller frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseTellerFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, iFrame, Teller_Frame, null, element, locator, finalValue);
    }

    @Then("^we get text and contain on teller frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnWarningTellerFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, iFrame, Teller_Frame, null, element, locator);
    }

    @And("^we download on teller frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnTellerFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, iFrame, Teller_Frame, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for teller frame (.*?) locator (.*?)$")
    public void weSelectFileForTellerFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,iFrame, Teller_Frame,null, element, locator, filePath);
    }

    //    __________________________________________________________________________________________________________________
    @Then("^we click on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weClickActionOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we report list of selection on  DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnDueDiligenceFormFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we double click on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we enter value on DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnDueDiligenceFormFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, iFrame, iFrame_5, null, element, locator, value);
    }

    @Then("^we select on DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnDueDiligenceFormFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, iFrame, iFrame_5, null, element, locator, value);
    }

    @Then("^we check on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weCheckActionOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we uncheck on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weUncheckActionOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we hover on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weHoverActionOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we type on DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnDueDiligenceFormFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, iFrame, iFrame_5, null, element, locator, value);
    }

    @Then("^we scroll on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we clear value on DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnDueDiligenceFormFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnDueDiligenceFormFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnDueDiligenceFormFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is disabled")
    public void weVerifyOnDueDiligenceFormFrameLocatorIsDisabled(String element, String locator) {
        frameCommonMethods.isdisabled(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we verify on DueDiligenceForm frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnDueDiligenceFormFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we get text on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weGetTextOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @Then("^we has value on DueDiligenceForm frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnDueDiligenceFormFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, iFrame, iFrame_5, null, element, locator, value);
    }

    @Then("^we get list of elements on DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @And("^we capture screenshot on DueDiligenceForm frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnDueDiligenceFormFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, iFrame, iFrame_5, null, element, locator, filePath);
    }

    @And("^we press on DueDiligenceForm frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnDueDiligenceFormFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, iFrame, iFrame_5, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseDueDiligenceFormFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, iFrame, iFrame_5, null, element, locator, finalValue);
    }

    @Then("^we get text and contain on DueDiligenceForm frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnWarningDueDiligenceFormFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, iFrame, iFrame_5, null, element, locator);
    }

    @And("^we download on DueDiligenceForm frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnDueDiligenceFormFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, iFrame, iFrame_5, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weSelectFileForDueDiligenceFormFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,iFrame, iFrame_5,null, element, locator, filePath);

    }

    //   ___________________________________________________________________________________________________________________
    @Then("^we click on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weClickActionOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we report list of selection on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnPopUpDueDiligenceFormFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we double click on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we enter value on pop-up DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPopUpDueDiligenceFormFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, pop_up, iFrame_5, null, element, locator, value);
    }

    @Then("^we select on pop-up DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPopUpDueDiligenceFormFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, pop_up, iFrame_5, null, element, locator, value);
    }

    @Then("^we check on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weCheckActionOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we uncheck on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we hover on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weHoverActionOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we type on pop-up DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPopUpDueDiligenceFormFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, pop_up, iFrame_5, null, element, locator, value);
    }

    @Then("^we scroll on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we clear value on pop-up DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPopUpDueDiligenceFormFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPopUpDueDiligenceFormFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnPopUpDueDiligenceFormFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is disabled")
    public void weVerifyOnPopUpDueDiligenceFormFrameLocatorIsDisabled(String element, String locator) {
        frameCommonMethods.isdisabled(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we verify on pop-up DueDiligenceForm frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnPopUpDueDiligenceFormFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we get text on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weGetTextOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @Then("^we has value on pop-up DueDiligenceForm frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPopUpDueDiligenceFormFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, pop_up, iFrame_5, null, element, locator, value);
    }

    @Then("^we get list of elements on pop-up DueDiligenceForm frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnPopUpDueDiligenceFormFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @And("^we capture screenshot on pop-up DueDiligenceForm frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPopUpDueDiligenceFormFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, pop_up, iFrame_5, null, element, locator, filePath);
    }

    @And("^we press on pop-up DueDiligenceForm frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPopUpDueDiligenceFormFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, pop_up, iFrame_5, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) pop-up DueDiligenceForm frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCasePopUpDueDiligenceFormFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, pop_up, iFrame_5, null, element, locator, finalValue);
    }

    @Then("^we get text and contain on pop-up DueDiligenceForm frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnWarningPopUpDueDiligenceFormFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, pop_up, iFrame_5, null, element, locator);
    }

    @And("^we download on pop-up DueDiligenceForm frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnPopUpDueDiligenceFormFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, pop_up, iFrame_5, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for pop-up DueDiligenceForm (.*?) locator (.*?)$")
    public void weSelectFileForPopUpDueDiligenceFormFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,pop_up, iFrame_5,null, element, locator, filePath);
    }

    //______________________________________________________________________________________________________________________
    @Then("^we click on warning-box frame (.*?) locator (.*?)$")
    public void weClickActionOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, null, null, null, element, locator);
    }

    @Then("^we report list of selection on warning-box frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnWarningBoxFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, null, null, null, element, locator);
    }

    @Then("^we double click on warning-box frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, null, null, null, element, locator);
    }

    @Then("^we enter value on warning-box frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnWarning_BoxFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we select on warning-box frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnWarning_BoxFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we check on warning-box frame (.*?) locator (.*?)$")
    public void weCheckActionOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, null, null, null, element, locator);
    }

    @Then("^we uncheck on warning-box frame (.*?) locator (.*?)$")
    public void weUncheckActionOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, null, null, null, element, locator);
    }

    @Then("^we hover on warning-box frame (.*?) locator (.*?)$")
    public void weHoverActionOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, null, null, null, element, locator);
    }

    @Then("^we type on warning-box frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnWarning_BoxFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we scroll on warning-box frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, null, null, null, element, locator);
    }

    @Then("^we clear value on warning-box frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on warning-box frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnWarning_BoxFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on warning-box frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnWarning_BoxFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on warning-box frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnWarning_BoxFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on warning-box frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnWarning_BoxFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, null, null, null, element, locator);
    }

    @Then("^we contain on warning-box frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnWarning_BoxFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we get text on warning-box frame (.*?) locator (.*?)$")
    public void weGetTextOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, null, null, null, element, locator);
    }

    @Then("^we has value on warning-box frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnWarning_BoxFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we get list of elements on warning-box frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, null, null, null, element, locator);
    }

    @When("we click radio on warning-box frame (.*?) list locator (.*?)$")
    public void clickRadioOnWarning_BoxFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, null, element, locator);
    }

    @And("^we capture screenshot on warning-box frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnWarning_BoxFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, null, null, null, element, locator, filePath);
    }

    @And("^we press on warning-box frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnWarning_BoxFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) warning-box frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseWarningBoxFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, null, null, null, element, locator, finalValue);
    }

    @Then("^we get text and contain on warning-box frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnWarningBoxUpFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, null, null, null, element, locator);
    }

    @And("^we download on warning-box frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnWarningBox(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, null, null, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for warning-box frame (.*?) locator (.*?)$")
    public void weSelectFileForWarningBoxFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,null, null,null, element, locator, filePath);
    }

//______________________________________________________________________________________________________________________

    @Then("^we click on pop-up frame (.*?) locator (.*?)$")
    public void weClickActionOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we report list of selection on pop-up frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnPopUpFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we double click on pop-up frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we enter value on pop-up frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnPop_UpFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, pop_up, null, null, element, locator, value);
    }

    @Then("^we select on pop-up frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnPop_UpFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, pop_up, null, null, element, locator, value);
    }

    @Then("^we check on pop-up frame (.*?) locator (.*?)$")
    public void weCheckActionOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we uncheck on pop-up frame (.*?) locator (.*?)$")
    public void weUncheckActionOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we hover on pop-up frame (.*?) locator (.*?)$")
    public void weHoverActionOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we type on pop-up frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnPop_UpFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, pop_up, null, null, element, locator, value);
    }

    @Then("^we scroll on pop-up frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we clear value on pop-up frame (.*?) locator (.*?)$")
    public void weClearValueOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we verify on pop-up frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnPop_UpFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we verify on pop-up frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnPop_UpFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we verify on pop-up frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnPop_UpFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we verify on pop-up frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnPop_UpFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we contain on pop-up frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnPop_UpFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, pop_up, null, null, element, locator, value);
    }

    @Then("^we get text on pop-up frame (.*?) locator (.*?)$")
    public void weGetTextOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we has value on pop-up frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnPop_UpFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, pop_up, null, null, element, locator, value);
    }

    @Then("^we get list of elements on pop-up frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, pop_up, null, null, element, locator);
    }

    @When("we click radio on pop-up frame (.*?) list locator (.*?)$")
    public void clickRadioOnPop_UpFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, pop_up, element, locator);
    }

    @And("^we capture screenshot on pop-up frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnPop_UpFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, pop_up, null, null, element, locator, filePath);
    }

    @And("^we press on pop-up frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnPop_UpFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, pop_up, null, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) pop-up frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCasePopUpFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, pop_up, null, null, element, locator, finalValue);
    }

    @Then("^we get text and contain on pop-up frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnPopUpFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, pop_up, null, null, element, locator);
    }

    @And("^we download on pop-up frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnPopUpFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, pop_up, null, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for pop-up frame (.*?) locator (.*?)$")
    public void weSelectFileForPopUpFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage, pop_up, null,null, element, locator, filePath);
    }
//    __________________________________________________________________________________________________________________

    @Then("^we click on third frame (.*?) locator (.*?)$")
    public void weClickActionOnThirdFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we report list of selection on third frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnThirdFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, pop_up, null, null, element, locator);
    }

    @Then("^we double click on third frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnThirdFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we enter value on third frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnThirdFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, iFrame, iFrame_2, iFrame_3, element, locator, value);
    }

    @Then("^we select on third frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnThirdFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, iFrame, iFrame_2, iFrame_3, element, locator, value);
    }

    @Then("^we check on third frame (.*?) locator (.*?)$")
    public void weCheckActionOnThirdFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we uncheck on third frame (.*?) locator (.*?)$")
    public void weUncheckActionOnThirdFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we hover on third frame (.*?) locator (.*?)$")
    public void weHoverActionOnThirdFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we type on third frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnThirdFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, iFrame, iFrame_2, iFrame_3, element, locator, value);
    }

    @Then("^we scroll on third frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnThirdFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we clear value on third frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnThirdFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we verify on third frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnThirdFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we verify on third frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnThirdFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we verify on third frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnThirdFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we verify on third frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnThirdFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we contain on third frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnThirdFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, iFrame, element, null, null, locator, value);
    }

    @Then("^we get text on third frame (.*?) locator (.*?)$")
    public void weGetTextOnThirdFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @Then("^we has value on third frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnThirdFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, iFrame, iFrame_2, iFrame_3, element, locator, value);
    }

    @Then("^we get list of elements on third frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnThirdFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @When("we click radio on third frame (.*?) list locator (.*?)$")
    public void clickRadioOnThirdFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, iFrame, element, locator);
    }

    @And("^we capture screenshot on third frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnThirdFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, iFrame, iFrame_2, iFrame_3, element, locator, filePath);
    }

    @And("^we press on third frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnThirdFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, iFrame, iFrame_2, iFrame_3, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) third frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseThirdFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, iFrame, iFrame_2, iFrame_3, element, locator, finalValue);
    }

    @Then("^we write to excel for testCase (.*?) column \"(.*?)\" third frame element (.*?) locator (.*?)$")
    public void weWriteToExcelForTestCaseOnThirdFrame(String testCaseId, String columnName, String element, String locator) {
        String valueToWrite = frameCommonMethods.get_frame_element_string_value(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
        System.out.println(valueToWrite);
        ExcelWriter.writeData(filePath, testCaseId, columnName, valueToWrite);
    }

    @Then("^we get text and contain on third frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnThirdFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, iFrame, iFrame_2, iFrame_3, element, locator);
    }

    @And("^we download on third frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnThirdFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, iFrame, iFrame_2, iFrame_5, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for third frame (.*?) locator (.*?)$")
    public void weSelectFileForThirdFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,iFrame, iFrame_2,iFrame_5, element, locator, filePath);
    }
//    __________________________________________________________________________________________________________________

    @Then("^we click on header frame (.*?) locator (.*?)$")
    public void weClickActionOnHeaderFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, null, null, null, element, locator);
    }

    @Then("^we report list of selection on header frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnHeaderFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, null, null, null, element, locator);
    }

    @Then("^we double click on header frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnHeaderFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, null, null, null, element, locator);
    }

    @Then("^we enter value on header frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnHeaderFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we select on header frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnHeaderFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we check on header frame (.*?) locator (.*?)$")
    public void weCheckActionOnHeaderFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, null, null, null, element, locator);
    }

    @Then("^we uncheck on header frame (.*?) locator (.*?)$")
    public void weUncheckActionOnHeaderFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, null, null, null, element, locator);
    }

    @Then("^we hover on header frame (.*?) locator (.*?)$")
    public void weHoverActionOnHeaderFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, null, null, null, element, locator);
    }

    @Then("^we type on header frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnHeaderFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we scroll on header frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnHeaderFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, null, null, null, element, locator);
    }

    @Then("^we clear value on header frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnHeaderFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on header frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnHeaderFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on header frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnHeaderFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on header frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnHeaderFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, null, null, null, element, locator);
    }

    @Then("^we verify on header frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnHeaderFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, null, null, null, element, locator);
    }

    @Then("^we contain on header frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnHeaderFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, null, element, null, null, locator, value);
    }

    @Then("^we get text on header frame (.*?) locator (.*?)$")
    public void weGetTextOnHeaderFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, null, null, null, element, locator);
    }

    @Then("^we has value on header frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnHeaderFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we get list of elements on header frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnHeaderFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, null, null, null, element, locator);
    }

    @When("we click radio on header frame (.*?) list locator (.*?)$")
    public void clickRadioOnHeaderFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, null, element, locator);
    }

    @And("^we capture screenshot on header frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnHeaderFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, null, null, null, element, locator, filePath);
    }

    @And("^we press on header frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnHeaderFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, null, null, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) header frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseHeaderFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, null, null, null, element, locator, finalValue);
    }

    @Then("^we get text and contain on header frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnHeaderFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, null, null, null, element, locator);
    }

    @And("^we download on header frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnHeaderFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, null, null, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for header frame (.*?) locator (.*?)$")
    public void weSelectFileForHeaderFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,null, null,null, element, locator, filePath);
    }
//    __________________________________________________________________________________________________________________

    @Then("^we click on form frame (.*?) locator (.*?)$")
    public void weClickActionOnFormFrame(String element, String locator) {
        frameCommonMethods.click(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we report list of selection on form frame (.*?) locator (.*?)$")
    public void weReportListOfSelectionOnFormFrameLocator(String element, String locator) {
        frameCommonMethods.reportListOfDropdown(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we double click on form frame (.*?) locator (.*?)$")
    public void weDoubleClickActionOnFormFrame(String element, String locator) {
        frameCommonMethods.dblclick(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we enter value on form frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterValueOnFormFrame(String element, String locator, String value) {
        frameCommonMethods.fill(iframePage, iFrame, iFrame_4, null, element, locator, value);
    }

    @Then("^we select on form frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weSelectValueOnFormFrame(String element, String locator, String value) {
        frameCommonMethods.select(iframePage, iFrame, iFrame_4, null, element, locator, value);
    }

    @Then("^we check on form frame (.*?) locator (.*?)$")
    public void weCheckActionOnFormFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we uncheck on form frame (.*?) locator (.*?)$")
    public void weUncheckActionOnFormFrame(String element, String locator) {
        frameCommonMethods.check(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we hover on form frame (.*?) locator (.*?)$")
    public void weHoverActionOnFormFrame(String element, String locator) {
        frameCommonMethods.hover(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we type on form frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weTypeValueOnFormFrame(String element, String locator, String value) {
        frameCommonMethods.type(iframePage, iFrame, iFrame_4, null, element, locator, value);
    }

    @Then("^we scroll on form frame (.*?) locator (.*?)$")
    public void weScrollToLocatorOnFormFrame(String element, String locator) {
        frameCommonMethods.scroll(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we clear value on form frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weClearValueOnFormFrame(String element, String locator) {
        frameCommonMethods.clear(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we verify on form frame (.*?) of locator (.*?) is visible$")
    public void weVerifyOnFormFrameLocatorIsVisible(String element, String locator) {
        frameCommonMethods.isvisible(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we verify on form frame (.*?) of locator (.*?) is checked$")
    public void weVerifyOnFormFrameLocatorIsChecked(String element, String locator) {
        frameCommonMethods.ischecked(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we verify on form frame (.*?) of locator (.*?) is enabled")
    public void weVerifyOnFormFrameLocatorIsEnabled(String element, String locator) {
        frameCommonMethods.isenabled(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we verify on form frame (.*?) of locator (.*?) is existed")
    public void weVerifyOnFormFrameLocatorIsExisted(String element, String locator) {
        frameCommonMethods.exists(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we contain on form frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weContainOnFormFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.contain(iframePage, iFrame, element, null, null, locator, value);
    }

    @Then("^we get text on form frame (.*?) locator (.*?)$")
    public void weGetTextOnFormFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @Then("^we has value on form frame (.*?) of locator (.*?) value \"(.*?)\"$")
    public void weHasValueOnFormFrameLocatorValue(String element, String locator, String value) {
        frameCommonMethods.hasvalue(iframePage, iFrame, iFrame_4, null, element, locator, value);
    }

    @Then("^we get list of elements on form frame (.*?) locator (.*?)$")
    public void weGetListOfElementsOnFormFrame(String element, String locator) {
        frameCommonMethods.gettext(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @When("we click radio on form frame (.*?) list locator (.*?)$")
    public void clickRadioOnFormFrame(String element, String locator) {
        frameCommonMethods.clickRadioButton(iframePage, iFrame, element, locator);
    }

    @And("^we capture screenshot on form frame (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnFormFrame(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        frameCommonMethods.screenshot(iframePage, iFrame, iFrame_4, null, element, locator, filePath);
    }

    @And("^we press on form frame (.*?) locator (.*?) key \"(.*?)\" keyboard$")
    public void wePressOnFormFrameKey(String element, String locator, String value) {
        frameCommonMethods.press(iframePage, iFrame, iFrame_4, null, element, locator, value);
    }

    @Then("^we enter using excel data on testCase (.*?) form frame (.*?) locator (.*?) value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseFormFrameLocatorValue(String testCaseId, String element, String locator, String value) {
        String finalValue = ExcelReader.getData(filePath, testCaseId, value);
        frameCommonMethods.fill(iframePage, iFrame, iFrame_4, null, element, locator, finalValue);
    }

    @Then("^we get text and contain on Form frame (.*?) of locator (.*?)$")
    public void weGetTextAndContainOnFormFrameLocatorValue(String element, String locator) {
        // Calls the contain method from frameCommonMethods to check if the specified element
        // contains the expected value in its text or attributes, as identified by the locator in the second iframe.
        frameCommonMethods.get_and_contain_text(iframePage, iFrame, iFrame_4, null, element, locator);
    }

    @And("^we download on form frame (.*?) locator (.*?) and file type is \"(.*?)\"$")
    public void weDownloadOnFormFrame(String element, String locator) {
        String filePath = ConfigurationProperties.getValue("downloadDocument");
        String fileType = ".pdf";
        frameCommonMethods.download(iframePage, iFrame, iFrame_4, null, element, locator, filePath + fileType);
    }

    @Then("^we select file: (.*?) for Form frame (.*?) locator (.*?)$")
    public void weSelectFileForFormFrameLocator(String fileName, String element, String locator) {
        String filePath = "documents/" + fileName;
        frameCommonMethods.selectFile(iframePage,iFrame, iFrame_4,null, element, locator, filePath);
    }

    //    __________________________________________________________________________________________________________________

    @And("^we capture screenshot on FNB-Online page (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnFNBOnline(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        Page page1 = iframePage.waitForPopup(() -> {
            iframePage.frameLocator(pop_up).getByRole(AriaRole.LINK, new FrameLocator.GetByRoleOptions().setName("fnb-online.com")).click();
        });
        frameCommonMethods.screenshot(page1, null, null, null, element, locator, filePath);
        page1.close();
    }

    @And("^we capture screenshot on Harland Clarke page (.*?) locator (.*?) name \"(.*?)\"$")
    public void weCaptureScreenshotOnHarlandClarke(String element, String locator, String name) {
        String filePath = "test-output/screenshots/" + name + ".png";
        Page page1 = iframePage.waitForPopup(() -> {
            iframePage.frameLocator(pop_up).getByRole(AriaRole.LINK, new FrameLocator.GetByRoleOptions().setName("Harland Clarke"));
        });
        frameCommonMethods.screenshot(page1, null, null, null, element, locator, filePath);
        page1.close();
    }

    //    __________________________________________________________________________________________________________________

    @Then("we navigate to Vault page locator (.*?) and capture screenshot$")
    public void weNavigateToVaultPageAndCaptureScreenshot(String locator) throws InterruptedException {
        Page vault_page = iframePage.waitForPopup(() -> {
            iframePage.frameLocator(iFrame).locator(locator).click();
        });
        frameCommonMethods.fill(vault_page, null, null, null, "vault_page", "user_name_flt", "azimovm@fnb-corp.com" );
        frameCommonMethods.click(vault_page, null, null, null, "vault_page", "next_btn");
        frameCommonMethods.fill(vault_page, null, null, null, "vault_page", "password_flt", "Damir2020_" );
        frameCommonMethods.press(vault_page, null,null,null, "vault_page", "password_flt", "Enter");
        timeOutFoSeconds("3");
        frameCommonMethods.screenshot(vault_page, null, null, null, "vault_page", "body", "body");

        vault_page.close();
    }

    @Then("we navigate to Vault page from pop-up locator and capture screenshot$")
    public void weNavigateToVaultPagePopUpAndCaptureScreenshot() throws InterruptedException {
        Page vault_page = iframePage.waitForPopup(() -> {
            iframePage.locator(pop_up).contentFrame().getByText("?", new FrameLocator.GetByTextOptions().setExact(true)).click();
        });
        frameCommonMethods.fill(vault_page, null, null, null, "vault_page", "user_name_flt", "azimovm@fnb-corp.com" );
        frameCommonMethods.click(vault_page, null, null, null, "vault_page", "next_btn");
        frameCommonMethods.fill(vault_page, null, null, null, "vault_page", "password_flt", "Damir2020(" );
        frameCommonMethods.press(vault_page, null,null,null, "vault_page", "password_flt", "Enter");
        timeOutFoSeconds("8");
        frameCommonMethods.screenshot(vault_page, null, null, null, "vault_page", "body", "body");

        vault_page.close();
    }

    //    __________________________________________________________________________________________________________________
    @Given("^get title of page$")
    public void getTitleOfPage() {
        String title = page.title();
        System.out.println("Page title: " + title);
    }

    @And("^time out for (.*?) seconds$")
    public void timeOutFoSeconds(String time_to_wait) throws InterruptedException {
        // Convert the string to an integer representing seconds
        int seconds = Integer.parseInt(time_to_wait);

        // Wait for the specified number of seconds (converted to milliseconds)
        Thread.sleep(seconds * 1000L);
    }

    @And("^time out for (.*?) minutes$")
    public void timeOutForMinutes(String time_to_wait) throws InterruptedException {
        // Convert the string to an integer representing minutes
        int minutes = Integer.parseInt(time_to_wait);

        // Convert minutes to seconds and then to milliseconds
        long milliseconds = minutes * 60 * 1000L;

        // Wait for the specified number of minutes (converted to milliseconds)
        Thread.sleep(milliseconds);
    }

    @And("^we wait for some time$")
    public void weWaitForSomeTime() throws InterruptedException {
        Thread.sleep(3000);
    }

    @Then("^we enter using excel data on testCase \"(.*?)\" column name \"(.*?)\" value \"(.*?)\"$")
    public void weEnterUsingExcelDataOnTestCaseMainFrameLocatorValue(String testCaseId, String columnName, String value) {
        ExcelWriter.writeData(filePath, testCaseId, columnName, value);
    }
}
