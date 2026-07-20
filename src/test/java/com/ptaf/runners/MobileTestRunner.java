package com.ptaf.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Dedicated Appium test runner for executing native Android and iOS Cucumber scenarios.
 *
 * <p>This class is intentionally empty and acts only as an entry point for JUnit to invoke
 * Cucumber with a specific set of options. Tests are discovered and executed by the
 * Cucumber JUnit runner configured via the {@link CucumberOptions} annotation below.</p>
 *
 * <p>Key responsibilities and notes for testers and engineers:
 * <ul>
 *   <li>features: Points to the folder that contains Gherkin feature files for mobile tests.</li>
 *   <li>glue: Contains the package(s) that hold step definitions and Cucumber hooks.</li>
 *   <li>tags: Controls which scenarios to include/exclude during execution. Current default is
 *       {@code @theapp_smoke}. Update this to run different sets of tests (e.g. {@code @regression}).</li>
 *   <li>plugin: Produces multiple report formats (pretty console output, HTML, JSON, JUnit XML and an
 *       ExtentReports adapter). Generated reports are written to the <code>target/cucumber-reports</code>
 *       directory by default.</li>
 *   <li>dryRun: When true, Cucumber checks that every step in the feature files has a matching
 *       step definition, without actually executing the steps. It is set to false here to execute tests.</li>
 *   <li>monochrome: When true, the console output is presented without ANSI colors or control characters,
 *       making logs cleaner for CI systems or plain-text consoles.</li>
 * </ul>
 *
 * Example CLI (Maven) to run this runner:
 * mvn -Dtest=com.ptaf.runners.MobileTestRunner test
 *
 * Example usage notes:
 * - To run a different tag, change the {@code tags} property in the {@link CucumberOptions}.
 * - To add or remove report types, modify the {@code plugin} array.
 * - Keep this class minimal and do not add test logic here; step definitions and hooks belong in the
 *   packages referenced by {@code glue}.
 * </p>
 */
@RunWith(Cucumber.class) // Use the Cucumber JUnit runner to execute scenarios with JUnit test lifecycle.
@CucumberOptions(
        plugin = {
                // Human-readable console output
                "pretty",
                // HTML report: open target/cucumber-reports/mobile-report.html after test run
                "html:target/cucumber-reports/mobile-report.html",
                // JSON report for integrations or further processing
                "json:target/cucumber-reports/mobile-report.json",
                // JUnit XML report to be consumed by CI servers
                "junit:target/cucumber-reports/mobile-report.xml",
                // ExtentReports adapter for rich interactive reports (requires adapter on classpath)
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
                "com.ptaf.reporting.PerFeatureReportListener",
                "com.ptaf.reporting.SoftAssertionReportListener"  // marks soft-failed steps as FAILED in Extent/PDF reports  // Per-feature individual reports (controlled by reporting.per_feature_reports_enabled in config.yml)
        },
        // Path to feature files that describe mobile test scenarios using Gherkin syntax.
        features = "src/test/resources/features/mobile",
        // Packages that contain step definitions and Cucumber hooks (before/after, etc.).
        glue = {"com.ptaf.stepdefinitions", "com.ptaf.hooks"},
        // Tag expression controlling which scenarios will be executed. Modify this to run other suites.
        tags = "@theapp_smoke",
        // When true, Cucumber will check for undefined steps without running tests. We execute tests here.
        dryRun = false,
        // When true, disables ANSI colors in console output for prettier CI logs and plain terminals.
        monochrome = true
)
public class MobileTestRunner {
    // Intentionally empty: the class serves only to configure and launch Cucumber via JUnit.
}
