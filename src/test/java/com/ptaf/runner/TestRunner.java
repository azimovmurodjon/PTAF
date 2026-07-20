package com.ptaf.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * TestNG Cucumber Runner — used by testng.xml and mvn clean test.
 *
 * <p>This class integrates Cucumber with TestNG by extending
 * {@link AbstractTestNGCucumberTests}. It is referenced by
 * {@code src/test/resources/testng.xml} and is the entry point
 * when running tests via Maven ({@code mvn clean test}) or via
 * right-clicking {@code testng.xml} in IntelliJ.</p>
 *
 * <h3>How to change which scenarios run</h3>
 * <p>Update the {@code tags} value in the {@code @CucumberOptions} annotation below.
 * Examples:</p>
 * <ul>
 *   <li>{@code "@eStore"}                   — run all eStore scenarios</li>
 *   <li>{@code "@smoke"}                    — run all smoke scenarios</li>
 *   <li>{@code "@eStore and @Regression"}   — run scenarios with both tags</li>
 *   <li>{@code "not @wip"}                  — run everything except work-in-progress</li>
 * </ul>
 *
 * <h3>Important: glue must include both packages</h3>
 * <p>The {@code glue} must include {@code "com.ptaf.stepdefinitions"} AND
 * {@code "com.ptaf.hooks"}. Without the hooks package, Cucumber cannot find
 * the {@code @Before} and {@code @After} lifecycle hooks, so the browser
 * will never be initialized and all tests will fail immediately.</p>
 *
 * <h3>This class is intentionally empty</h3>
 * <p>All configuration is in the {@code @CucumberOptions} annotation.
 * Do not add test logic here — implement step definitions and hooks in
 * the packages referenced by {@code glue}.</p>
 */
@CucumberOptions(
        // Location of feature files relative to the project root.
        features = "src/test/resources/features",

        // Packages to scan for step definitions AND hooks.
        // IMPORTANT: com.ptaf.hooks MUST be included here.
        // Without it, @Before and @After hooks are not found and the browser never starts.
        glue = {"com.ptaf.stepdefinitions", "com.ptaf.hooks"},

        // Tag expression controlling which scenarios are executed.
        // Change this to run a different set of scenarios.
        // This value is also used when running via testng.xml or mvn clean test.
        tags = "@eStore",

        // Reporting plugins.
        plugin = {
                "pretty",                                                                    // Human-readable console output
                "html:target/cucumber-reports/cucumber-pretty",                             // HTML report
                "json:target/cucumber-reports/CucumberTestReport.json",                     // JSON report for CI integration
                "rerun:target/cucumber-reports/rerun.txt",                                  // Failed scenario paths for re-run
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",     // Extent HTML report
                "com.ptaf.reporting.PerFeatureReportListener",
                "com.ptaf.reporting.SoftAssertionReportListener"  // marks soft-failed steps as FAILED in Extent/PDF reports                               // Per-feature reports (config: reporting.per_feature_reports_enabled)
        }
)
public class TestRunner extends AbstractTestNGCucumberTests {
    // Intentionally left empty.
    // AbstractTestNGCucumberTests provides the TestNG data provider that
    // discovers and executes Cucumber scenarios as TestNG tests.
}
