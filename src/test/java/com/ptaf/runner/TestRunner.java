package com.ptaf.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/*
 * CucumberOptions:
 *
 * - features: location of the feature files for Cucumber to execute.
 *   Here it is set to "src/test/resources/features", which is the common Maven
 *   convention. Place feature files under that directory (or update the path).
 *
 * - glue: package(s) containing step definition and hook classes. Cucumber uses
 *   these packages to locate the step definitions that implement Gherkin steps.
 *   Keep step definitions in the package(s) specified here.
 *
 * - tags: logical expression that filters which scenarios are executed. The
 *   expression "@google_mobile_browser and @android" means only scenarios
 *   annotated with both @google_mobile_browser and @android will run.
 *   Adjust this expression to include/exclude tests (e.g., "not @wip" or
 *   "@smoke or @regression").
 *
 * - plugin: list of reporting and auxiliary plugins. The configured plugins are:
 *     "pretty" - prints readable output to the console,
 *     "html:target/cucumber-reports/cucumber-pretty" - generates an HTML report,
 *     "json:target/cucumber-reports/CucumberTestReport.json" - produces a JSON report,
 *     "rerun:target/cucumber-reports/rerun.txt" - writes failed scenario paths to a file
 *       which can be used to re-run only failed tests.
 *
 * Notes for testers:
 * - To run a different set of scenarios, change the tags expression here (for example,
 *   to run @smoke tests) or add/remove tags on feature/scenario level.
 * - If feature files are moved, adjust the 'features' path accordingly.
 * - The generated reports live under target/cucumber-reports by default; check the
 *   HTML/JSON output after a test run for details.
 * - The rerun file contains paths to failed scenarios. You can feed that file back
 *   into the runner to re-execute only failures (commonly used in CI pipelines).
 */

/**
 * TestRunner integrates Cucumber with TestNG.
 *
 * <p>This class delegates test execution to Cucumber via the TestNG runner by
 * extending AbstractTestNGCucumberTests. No methods are overridden here because
 * the default behavior provided by AbstractTestNGCucumberTests is sufficient:
 *
 * - It creates a TestNG data provider for Cucumber scenarios and executes them
 *   as TestNG tests, allowing parallelization when configured via TestNG.
 *
 * Usage notes for testers:
 * - Execute this test class via your IDE's TestNG runner, Maven (mvn test),
 *   or your CI system. The TestNG/Cucumber integration will discover and run
 *   scenarios according to the CucumberOptions declared above.
 * - To modify which scenarios run, update the tags expression in the
 *   @CucumberOptions annotation. To point to other feature files, update the
 *   'features' path. To add/modify reporting outputs, edit the 'plugin' list.
 *
 * Important: This class contains only configuration. Do not add test logic here;
 * implement step definitions, hooks, and page objects under the packages
 * referenced by 'glue'.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.ptaf.stepdefinitions"},
        tags = "@google_mobile_browser and @android",
        plugin = {"pretty", "html:target/cucumber-reports/cucumber-pretty", "json:target/cucumber-reports/CucumberTestReport.json", "rerun:target/cucumber-reports/rerun.txt"}
)
public class TestRunner extends AbstractTestNGCucumberTests {
    // Intentionally left empty: this class is a configuration holder only.
    // AbstractTestNGCucumberTests provides the necessary TestNG integration
    // so Cucumber feature scenarios are discovered and executed as TestNG tests.
}
