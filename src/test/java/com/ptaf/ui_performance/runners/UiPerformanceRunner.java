package com.ptaf.ui_performance.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Dedicated TestNG/Cucumber entry point for isolated UI performance scenarios.
 *
 * <p>The runner deliberately scans only {@code com.ptaf.ui_performance.stepdefinitions}. It does
 * not include {@code com.ptaf.hooks}, ordinary UI step definitions, mobile hooks, or Extent/PDF
 * listeners. As a result, a UI performance run cannot change normal UI browser lifecycle or reports.</p>
 *
 * <p>Run with: {@code mvn clean test -Pui_performance}. The packaged eStore journey is selected by
 * {@code @ui_performance}; its target, load profile, browser mode, data, thresholds, and reports are
 * controlled only from the separate UI performance resources.</p>
 */
@CucumberOptions(
        features = "src/test/resources/ui_performance/features",
        glue = "com.ptaf.ui_performance.stepdefinitions",
        tags = "@ui_performance and not @template",
        plugin = {
                "pretty",
                "html:test-output/ui_performance/cucumber/cucumber.html",
                "json:test-output/ui_performance/cucumber/cucumber.json",
                "junit:test-output/ui_performance/cucumber/cucumber.xml"
        },
        monochrome = true
)
public class UiPerformanceRunner extends AbstractTestNGCucumberTests {
    // The internal engine controls browser-user concurrency. Cucumber scenario-level parallelism is intentionally disabled.
}
