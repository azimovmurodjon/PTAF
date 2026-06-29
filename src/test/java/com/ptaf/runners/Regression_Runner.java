package com.ptaf.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * JUnit runner class for executing Cucumber feature files that are marked for regression testing.
 *
 * <p>This class is intentionally minimal and acts solely as a configuration holder for Cucumber
 * execution through JUnit. All behavior is defined via annotations; there are no methods or fields.
 *
 * <h3>Purpose</h3>
 * - Centralizes the execution configuration for the regression test suite.
 * - Selects which feature files and scenarios to run (using tags).
 * - Configures reporting output and the glue code packages used by Cucumber.
 *
 * <h3>How to run</h3>
 * - From an IDE: right-click this class and choose "Run" or "Debug".
 * - From Maven: use your test goal, e.g., mvn test -Dtest=Regression_Runner (depending on your build setup).
 *
 * <h3>Important details for testers</h3>
 * - Tag selection: Only scenarios or feature files annotated with "@regression" will be executed.
 * - Features directory: Feature files should be located under src/test/resources/features.
 * - Glue code: Step definitions and hooks must be present in the packages:
 *      com.ptaf.stepdefinitions and com.ptaf.hooks (the annotation uses slash-separated package names).
 * - Reporting:
 *      - "pretty": human-readable console output for quick feedback.
 *      - ExtentCucumberAdapter: generates an HTML report (requires the Extent Cucumber adapter dependency
 *        and its configuration to be present in the project).
 *      - "timeline:test-output-thread/": outputs timeline artifacts useful for visualizing parallel execution.
 *
 * <p>Keep this runner class focused on configuration. To modify execution behavior (tags, reports,
 * features, or glue), update the annotation values here rather than adding logic to the class.
 */
@RunWith(Cucumber.class) // Use the Cucumber JUnit runner to execute Cucumber feature files via JUnit
@CucumberOptions(
        // Define the output formats and locations for the test reports and console output.
        plugin = {"pretty",                       // Print the test results in a readable format to the console
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",  // Produce an Extent HTML report (adapter controls output location/config)
                "timeline:test-output-thread/"   // Generate timeline artifacts under test-output-thread/ for parallel run visualization
        },
        tags = "@regression", // Execute only scenarios and features annotated with this tag
        // Specify the location of the feature files relative to the project root.
        features = "src/test/resources/features",
        // Specify the packages that contain step definitions and Cucumber hooks.
        // Note: the glue paths are provided in a slash-separated format here; the runtime resolves them to package names.
        glue = {"com/ptaf/stepdefinitions", "com/ptaf/hooks"}
)
public class Regression_Runner {
    // This class intentionally contains no methods or fields. Its sole responsibility is to hold the
    // Cucumber/JUnit configuration via the annotations above. Testers can modify the annotations
    // (for example, change tags or feature paths) to control which tests are executed and how reports are generated.
}
