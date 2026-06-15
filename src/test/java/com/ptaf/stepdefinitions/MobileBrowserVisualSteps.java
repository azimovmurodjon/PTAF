package com.ptaf.stepdefinitions;

import com.microsoft.playwright.Page;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.mobilebrowser.MobileBrowserVisualValidator;
import com.ptaf.utils.ConfigurationProperties;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.Before;

/** Cucumber steps for Playwright mobile-browser visual regression testing. */
public class MobileBrowserVisualSteps {
    private Scenario scenario;

    @Before
    public void captureScenario(Scenario scenario) { this.scenario = scenario; }

    @Then("I compare mobile browser page with visual baseline {string}")
    public void iCompareMobileBrowserPageWithVisualBaseline(String baselineName) {
        Page page = Hooks.getPage();
        String browserProfile = ConfigurationProperties.getBrowser();
        MobileBrowserVisualValidator.compareCurrentPage(page, scenario, baselineName, browserProfile);
    }
}
