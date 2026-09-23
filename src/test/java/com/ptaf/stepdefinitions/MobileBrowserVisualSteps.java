package com.ptaf.stepdefinitions;

import com.microsoft.playwright.Page;
import com.ptaf.hooks.Hooks;
import com.ptaf.ui.mobilebrowser.MobileBrowserVisualValidator;
import com.ptaf.utils.ConfigurationProperties;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Then;
import io.cucumber.java.Before;

/**
 * Cucumber step definitions related to visual regression testing of mobile browser pages.
 *
 * <p>This class provides glue code between Cucumber scenarios and the visual validation
 * infrastructure. It relies on:
 * - Hooks.getPage() to provide the current Playwright Page instance for the scenario,
 * - ConfigurationProperties.getBrowser() to determine which browser/profile is under test,
 * - MobileBrowserVisualValidator.compareCurrentPage(...) to perform the actual visual comparison
 *   against a named baseline.
 *
 * <p>The lifecycle is:
 * 1. The {@link #captureScenario(Scenario)} method (annotated with {@code @Before}) captures the
 *    current Cucumber {@link Scenario} instance so it can be passed to the visual validator.
 * 2. The step method {@link #iCompareMobileBrowserPageWithVisualBaseline(String)} is invoked
 *    from feature files to trigger a visual comparison using the supplied baseline name.
 *
 * <p>Notes for testers:
 * - Provide descriptive baseline names in feature files to clearly indicate the expected target.
 * - The comparison implementation (in MobileBrowserVisualValidator) will typically log results,
 *   attach artifacts, and may mark the scenario as failed depending on the comparison outcome.
 */
public class MobileBrowserVisualSteps {
    /**
     * Reference to the currently executing Cucumber scenario.
     *
     * <p>Captured by the {@link #captureScenario(Scenario)} hook method so it can be supplied
     * to the visual validator. Keeping a reference enables the validator to attach comparison
     * results, screenshots, or any other metadata to the scenario in reports.
     */
    private Scenario scenario;

    /**
     * Cucumber hook that runs before each scenario to capture the current Scenario object.
     *
     * <p>The Scenario instance is stored for later use by step definitions that need to
     * annotate or attach artifacts to the scenario (for example, visual comparison diffs).
     *
     * @param scenario the current Cucumber scenario provided by the test framework
     */
    @Before
    public void captureScenario(Scenario scenario) { this.scenario = scenario; }

    /**
     * Step definition that triggers a visual comparison of the current mobile browser page
     * against a named visual baseline.
     *
     * <p>Typical usage in a feature file:
     * Then I compare mobile browser page with visual baseline "homepage-v1"
     *
     * <p>Operational steps performed by this method:
     * 1. Retrieve the Playwright {@link Page} instance for the currently executing test via {@link Hooks#getPage()}.
     * 2. Read the browser/profile identifier from {@link ConfigurationProperties#getBrowser()}.
     * 3. Invoke {@link MobileBrowserVisualValidator#compareCurrentPage(Page, Scenario, String, String)}
     *    to perform the visual diff between the live page and the stored baseline. The scenario and
     *    browser profile are provided so that the validator can produce context-aware artifacts
     *    and reporting (for example, environment-specific baselines or attachments).
     *
     * <p>Parameters:
     * @param baselineName the name of the visual baseline to compare against; this should match a
     *                     baseline stored in your visual test repository or artifact store.
     */
    @Then("I compare mobile browser page with visual baseline {string}")
    public void iCompareMobileBrowserPageWithVisualBaseline(String baselineName) {
        // Obtain the active Playwright Page instance for the scenario from the shared Hooks utility.
        Page page = Hooks.getPage();

        // Determine the browser/profile that is currently under test. This is commonly used
        // by the visual validator to select the appropriate baseline or to include in reports.
        String browserProfile = ConfigurationProperties.getBrowser();

        // Delegate the actual visual comparison to the MobileBrowserVisualValidator. The validator
        // will compare the currently rendered page to the named baseline and typically attach
        // results (screenshots, diffs, logs) to the provided Scenario.
        MobileBrowserVisualValidator.compareCurrentPage(page, scenario, baselineName, browserProfile);
    }
}
