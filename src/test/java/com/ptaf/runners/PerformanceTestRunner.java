package com.ptaf.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Test runner for performance-related Cucumber scenarios.
 *
 * <p>
 * This class is intentionally empty and serves only as an entry point for JUnit to
 * invoke Cucumber. The behavior of the test execution is configured through the
 * {@link CucumberOptions} annotation below.
 * </p>
 *
 * <p>Who should read this:
 * - Testers who want to run only performance tests.
 * - Developers who need to understand where feature files and step definitions live.
 * </p>
 *
 * <p>How to use:
 * - Run this class as a JUnit test (from an IDE) or execute a Maven/Gradle test task.
 * - To change which scenarios are executed, update the {@code tags} value in the
 *   {@link CucumberOptions} annotation. For multiple tags, follow Cucumber tag expression syntax.
 * - To point to a different set of feature files, update {@code features}.
 * - To register different step definition/package locations, update {@code glue}.
 * </p>
 *
 * <p>Notes:
 * - The {@code plugin} entries control reporting. Keep the corresponding reporter
 *   dependencies on the classpath if you enable additional report adapters.
 * - No logic or fields are required here; all configuration is annotation-driven.
 * </p>
 */
@RunWith(Cucumber.class) // Use JUnit runner to execute Cucumber feature files
@CucumberOptions(
        plugin = {
                /*
                 * "pretty" - prints readable Gherkin source with additional colors and
                 * formatting to the console. Useful for quick local feedback.
                 */
                "pretty",

                /*
                 * "html:target/performance-cucumber-report.html" - generates an HTML report
                 * at the given path relative to the project root. This is a simple,
                 * readily viewable report for test results.
                 */
                "html:target/performance-cucumber-report.html",

                /*
                 * "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
                "com.ptaf.reporting.PerFeatureReportListener",
                "com.ptaf.reporting.SoftAssertionReportListener"  // marks soft-failed steps as FAILED in Extent/PDF reports  // Per-feature individual reports (controlled by reporting.per_feature_reports_enabled in config.yml)
                 * - integrates with the ExtentReports reporter adapter. When this adapter
                 * is present on the classpath, a rich HTML report (Extent) will be produced.
                 * Ensure the Extent Cucumber adapter dependency and its configuration file
                 * (if any) are available in the project when using this plugin.
                 */
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",
                "com.ptaf.reporting.PerFeatureReportListener",
                "com.ptaf.reporting.SoftAssertionReportListener"  // marks soft-failed steps as FAILED in Extent/PDF reports  // Per-feature individual reports (controlled by reporting.per_feature_reports_enabled in config.yml)
        },

        /*
         * tags - limits execution to scenarios/features that match the given tag
         * expression. Currently configured to run scenarios marked with @performance_testing.
         *
         * Examples:
         * - "@performance_testing"       => runs scenarios with that tag
         * - "@smoke or @regression"     => runs scenarios with either tag
         * - "@smoke and not @wip"       => runs smoke tests excluding work-in-progress
         */
        tags = "@performance_testing",

        /*
         * features - path to the directory (or a specific feature file) containing
         * the Gherkin feature files to be executed. This path is relative to the
         * project root. Here it targets the performance feature suite.
         */
        features = "src/test/resources/features/performance",

        /*
         * glue - packages to scan for step definitions, hooks, and any other
         * Cucumber-related glue code. The order matters if there are overlapping
         * step definitions; Cucumber will search these packages in the order listed.
         */
        glue = {"com.ptaf.stepdefinitions", "com.ptaf.hooks"}
)
public class PerformanceTestRunner {
    /**
     * Intentionally empty.
     *
     * <p>The presence of this class with the annotations above is sufficient for JUnit
     * and Cucumber to discover and run feature files. Do not add logic here.</p>
     */
}
