package com.ptaf.runners;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * Test runner for API test scenarios.
 *
 * <p>
 * This class is a JUnit entry point that instructs JUnit to run Cucumber feature
 * files according to the options defined in the {@code @CucumberOptions} annotation.
 * The class is intentionally empty — its purpose is solely to hold configuration
 * metadata consumed by the test framework at runtime.
 * </p>
 *
 * <p>Key behaviors and conventions:
 * <ul>
 *   <li>JUnit's {@code RunWith(Cucumber.class)} integrates Cucumber with the JUnit runner.</li>
 *   <li>Only scenarios annotated with the {@code @api} tag will be executed (see {@code tags}).</li>
 *   <li>Feature files are expected under {@code src/test/resources/features}.</li>
 *   <li>Step definitions and hooks are discovered in the packages specified by {@code glue}.</li>
 *   <li>Multiple reporting plugins are configured to produce console, HTML, ExtentReports, and timeline outputs.</li>
 * </ul>
 * </p>
 *
 * <p>Typical usage for testers:
 * <ol>
 *   <li>Mark API scenarios in your feature files with the {@code @api} tag.</li>
 *   <li>Place feature files under {@code src/test/resources/features}.</li>
 *   <li>Implement step definitions under {@code com.ptaf.api.stepdefinitions} and hooks under {@code com.ptaf.hooks}.</li>
 *   <li>Execute this runner through your IDE or CI build to generate the configured reports.</li>
 * </ol>
 * </p>
 *
 * <p>Important notes:
 * <ul>
 *   <li>Do not rename this class or modify the annotation values unless you understand
 *       how the Cucumber runner discovers features, glue code, tags, and generates reports.</li>
 *   <li>This file only configures test execution; it does not contain test logic.</li>
 * </ul>
 * </p>
 */
 // Instruct JUnit to use the Cucumber runner for this test suite.
@RunWith(Cucumber.class)
@CucumberOptions(
        // Plugins control what reporting outputs are generated after the tests run.
        // The list below provides human-readable console output ("pretty"), an HTML report,
        // an ExtentReports adapter, and a timeline-style output for thread-aware reporting.
        plugin = {"pretty",
                "html:target/api-cucumber-reports.html", // HTML report file (keeps API runner reports separate)
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:", // ExtentReports integration
                "timeline:test-output-thread/", // Timeline output for multi-threaded execution visualization
                "com.ptaf.reporting.PerFeatureReportListener",
                "com.ptaf.reporting.SoftAssertionReportListener"  // marks soft-failed steps as FAILED in Extent/PDF reports  // Per-feature individual reports (controlled by reporting.per_feature_reports_enabled in config.yml)
        },
        // Only run scenarios/features marked with the @api tag. This keeps API tests isolated
        // from UI or other test types that may exist in the same project.
        tags = "@api",
        // Path to the directory containing Gherkin feature files for the API tests.
        // Testers should add feature files here (or subfolders) and tag them with @api to be executed.
        features = "src/test/resources/features",
        // Glue defines the packages Cucumber uses to search for step definition and hook classes.
        // Note: Use package names (dot-separated) or file path style as supported; this configuration
        // points to the API step definitions and shared hooks for setup/teardown logic.
        glue = {"com/ptaf/api/stepdefinitions", "com/ptaf/hooks"}
)
public class ApiTestRunner {
    /**
     * This class is intentionally left blank.
     *
     * It serves purely as a configuration holder for the Cucumber + JUnit integration
     * via annotations above. Do not add runtime logic or state here — place step
     * definitions, hooks, and other test code in the designated packages instead.
     */
}
