package com.ptaf.runners;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

/**
 * TestRunner is the JUnit entry point for executing Cucumber feature files in this project.
 *
 * <p>
 * This class is intentionally empty and only serves as a configuration holder for Cucumber
 * via annotations. JUnit discovers this runner and delegates to the Cucumber JUnit runner
 * specified by the {@link RunWith} annotation.
 * </p>
 *
 * <p>Key responsibilities (via annotations):
 * <ul>
 *   <li>Point Cucumber to the feature files to execute.</li>
 *   <li>Set the glue packages where step definitions and hooks are located.</li>
 *   <li>Configure reporting plugins and specify test selection via tags.</li>
 * </ul>
 * </p>
 *
 * <p>Notes for testers:
 * <ul>
 *   <li>To run a different subset of scenarios, modify the {@code tags} attribute below
 *       (for example: {@code "@smoke"} or {@code "not @wip"}). This class must be recompiled
 *       if the annotation value is changed.</li>
 *   <li>Report outputs are generated under the project target/output folders as specified
 *       in the {@code plugin} configuration. Ensure the corresponding report dependencies
 *       (for example, the Extent Cucumber adapter) are present in your build configuration
 *       if you rely on those reports.</li>
 *   <li>If you need to parameterize execution from the command line (without editing this
 *       file), configure your build tool to pass Cucumber options or consider a custom
 *       runner that reads system properties.</li>
 * </ul>
 * </p>
 */
@RunWith(Cucumber.class) // Use the Cucumber JUnit runner so JUnit can execute Cucumber feature files.
@CucumberOptions(
        // Configure output formats and destinations for test results
        plugin = {"pretty",                      // Print test results in a readable form to the console
                "html:target/cucumber-reports.html",  // Generate an HTML report at target/cucumber-reports.html
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",  // Adapter for ExtentReports (requires dependency)
                "timeline:test-output-thread/",       // Produce a timeline report in the specified folder
                "com.ptaf.reporting.PerFeatureReportListener",
                "com.ptaf.reporting.SoftAssertionReportListener"  // marks soft-failed steps as FAILED in Extent/PDF reports  // Per-feature individual reports (controlled by reporting.per_feature_reports_enabled in config.yml)
        },
        // Tag expression selecting which scenarios to run. Change this to run different tagged scenarios.
        // Examples:
        //  - "@smoke"        -> Run scenarios tagged with @smoke
        //  - "not @wip"      -> Run all scenarios except those tagged @wip
        //  - "@eStore and @Regression" -> Run scenarios tagged with both @eStore and @Regression
        tags = "@eStore",
        // Path to the feature files. This is relative to the project root.
        // Ensure feature files are located under src/test/resources/features.
        features = "src/test/resources/features",
        // Packages to scan for step definitions and hooks.
        // These should match the package names where your @Given/@When/@Then and hook classes reside.
        glue = {"com/ptaf/stepdefinitions", "com/ptaf/hooks"}
)
public class TestRunner {
        // Intentionally left blank.
        //
        // This class acts purely as a configuration holder for Cucumber via annotations.
        // JUnit instantiates and runs the Cucumber runner referenced by @RunWith(Cucumber.class).
        //
        // Typical usage for testers:
        //  - Execute tests using your build tool (for example, "mvn test" or a configured Gradle task).
        //  - Inspect generated reports in "target/cucumber-reports.html" and "test-output-thread/".
        //  - Update the @CucumberOptions tags or features path above if you need to change which scenarios run.
}
