package com.ptaf.ui_performance.stepdefinitions;

import com.ptaf.ui_performance.config.UiPerformanceConfiguration;
import com.ptaf.ui_performance.config.UiPerformanceLocatorRepository;
import com.ptaf.ui_performance.core.UiPerformanceEngine;
import com.ptaf.ui_performance.model.UiPerformanceJourney;
import com.ptaf.ui_performance.model.UiPerformanceRunResult;
import com.ptaf.ui_performance.model.UiPerformanceStep;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Cucumber glue for the isolated UI performance module.
 *
 * <p>These steps form a small, explicit journey DSL. Targets, routes, locators, users, and thresholds
 * remain in the dedicated UI performance YAML/CSV files. Feature files describe only the business flow
 * and do not contain URLs, selectors, credentials, tokens, or environment-specific values.</p>
 */
public class UiPerformanceSteps {
    private UiPerformanceJourney journey;
    private UiPerformanceRunResult result;

    /** Starts a journey using the protocol, host, port, and base path from isolated YAML configuration. */
    @Given("UI performance journey {string} uses configured target")
    public void createJourneyUsingConfiguredTarget(String journeyName) {
        this.journey = new UiPerformanceJourney(journeyName, UiPerformanceConfiguration.getConfiguredTargetUrl());
    }

    /**
     * Retained for backward compatibility with the first UI performance template. New journeys should
     * use the configuration-first target step so URLs do not appear in features.
     */
    @Given("UI performance journey {string} targets {string}")
    public void createJourney(String journeyName, String baseUrl) {
        this.journey = new UiPerformanceJourney(journeyName, baseUrl);
    }

    /** Adds navigation using a named, relative route from the isolated UI performance YAML. */
    @When("UI performance journey navigates to configured route {string}")
    public void addConfiguredNavigation(String routeName) {
        String route = UiPerformanceConfiguration.getConfiguredRoute(routeName);
        requireJourney().addStep(new UiPerformanceStep("Navigate " + routeName,
                UiPerformanceStep.Action.NAVIGATE, route, null));
    }

    /** Retained for backward compatibility; new features should navigate through a configured route name. */
    @When("UI performance journey navigates to {string}")
    public void addNavigation(String route) {
        requireJourney().addStep(new UiPerformanceStep("Navigate", UiPerformanceStep.Action.NAVIGATE, route, null));
    }

    /** Adds a field-fill step whose value is read only from the separate CSV data column at runtime. */
    @And("UI performance journey fills locator {string} {string} with data field {string}")
    public void addDataFill(String locatorGroup, String locatorKey, String dataField) {
        requireJourney().addStep(new UiPerformanceStep("Fill " + locatorGroup + "." + locatorKey,
                UiPerformanceStep.Action.FILL,
                UiPerformanceLocatorRepository.getLocatorDefinition(locatorGroup, locatorKey),
                "${" + dataField + "}"));
    }

    /** Adds a select-list step whose visible option label comes from the isolated CSV row. */
    @And("UI performance journey selects locator {string} {string} with data field {string}")
    public void addDataSelection(String locatorGroup, String locatorKey, String dataField) {
        requireJourney().addStep(new UiPerformanceStep("Select " + locatorGroup + "." + locatorKey,
                UiPerformanceStep.Action.SELECT_OPTION,
                UiPerformanceLocatorRepository.getLocatorDefinition(locatorGroup, locatorKey),
                "${" + dataField + "}"));
    }

    /** Adds a non-sensitive literal fill. Passwords, tokens, and credentials must use a CSV data field instead. */
    @And("UI performance journey fills locator {string} {string} with literal {string}")
    public void addLiteralFill(String locatorGroup, String locatorKey, String value) {
        String normalizedKey = locatorKey == null ? "" : locatorKey.toLowerCase(java.util.Locale.ROOT);
        if (normalizedKey.contains("password") || normalizedKey.contains("token")
                || normalizedKey.contains("secret") || normalizedKey.contains("credential")) {
            throw new IllegalArgumentException(
                    "Sensitive UI performance values must use the separate CSV data-field step, not a literal feature-file value.");
        }
        requireJourney().addStep(new UiPerformanceStep("Fill " + locatorGroup + "." + locatorKey,
                UiPerformanceStep.Action.FILL,
                UiPerformanceLocatorRepository.getLocatorDefinition(locatorGroup, locatorKey),
                value));
    }

    /** Adds a click step resolved only from the separate regular-style UI performance locator YAML. */
    @And("UI performance journey clicks locator {string} {string}")
    public void addClick(String locatorGroup, String locatorKey) {
        requireJourney().addStep(new UiPerformanceStep("Click " + locatorGroup + "." + locatorKey,
                UiPerformanceStep.Action.CLICK,
                UiPerformanceLocatorRepository.getLocatorDefinition(locatorGroup, locatorKey),
                null));
    }

    /** Adds an action that clicks a configured locator and continues the journey in its popup page. */
    @And("UI performance journey clicks locator {string} {string} and switches to popup")
    public void addPopupClick(String locatorGroup, String locatorKey) {
        requireJourney().addStep(new UiPerformanceStep("Open popup " + locatorGroup + "." + locatorKey,
                UiPerformanceStep.Action.CLICK_AND_SWITCH_TO_POPUP,
                UiPerformanceLocatorRepository.getLocatorDefinition(locatorGroup, locatorKey),
                null));
    }

    /** Adds a visible-state validation step resolved only from the separate regular-style locator YAML. */
    @Then("UI performance journey verifies locator {string} {string} is visible")
    public void addVisibilityCheck(String locatorGroup, String locatorKey) {
        requireJourney().addStep(new UiPerformanceStep("Verify " + locatorGroup + "." + locatorKey,
                UiPerformanceStep.Action.VERIFY_VISIBLE,
                UiPerformanceLocatorRepository.getLocatorDefinition(locatorGroup, locatorKey),
                null));
    }

    /** Executes all configured concurrent browser users and writes standalone performance reports. */
    @Then("the configured UI performance users execute the journey")
    public void executeJourney() {
        this.result = new UiPerformanceEngine().execute(requireJourney());
    }

    /** Verifies that a completed run was collected; configured performance thresholds are enforced during execution. */
    @Then("the UI performance run produces a standalone performance report")
    public void verifyPerformanceReport() {
        verifyReportDirectory();
    }

    /** Retained for the initial feature wording. */
    @Then("the UI performance run produces a standalone report")
    public void verifyReport() {
        verifyReportDirectory();
    }

    private void verifyReportDirectory() {
        if (result == null || result.getReportDirectory() == null) {
            throw new AssertionError("UI performance run did not produce a standalone report directory.");
        }
    }

    private UiPerformanceJourney requireJourney() {
        if (journey == null) {
            throw new IllegalStateException("Create a UI performance journey before adding steps.");
        }
        return journey;
    }
}
