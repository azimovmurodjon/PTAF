package com.ptaf.runners;

import org.testng.annotations.DataProvider;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Test runner that configures and launches Cucumber feature execution using TestNG.
 *
 * <p>This class:
 * <ul>
 *     <li>Specifies Cucumber options such as feature locations, step definition glue, reporting plugins,
 *     and tags to filter scenarios.</li>
 *     <li>Integrates Cucumber with TestNG by extending AbstractTestNGCucumberTests.</li>
 *     <li>Overrides the scenarios data provider to enable parallel execution of scenarios by TestNG.</li>
 * </ul>
 *
 * Usage notes for testers:
 * <ul>
 *     <li>Change the {@code tags} value in the {@code @CucumberOptions} annotation to run different sets
 *     of scenarios (for example use {@code @smoke} or {@code @regression}).</li>
 *     <li>Feature files are expected under {@code src/test/resources/features}—update {@code features}
 *     if your project uses a different path.</li>
 *     <li>Report output locations are configured in {@code plugin}. For example, an HTML report will be
 *     written to {@code target/cucumber-reports.html} and an ExtentReports adapter is enabled.</li>
 *     <li>Enable or disable parallel execution by changing the {@code @DataProvider(parallel = true)}
 *     flag in the {@code scenarios()} method. When enabled, ensure step definitions and any shared
 *     test state are thread-safe.</li>
 * </ul>
 *
 * Important: This class contains no test logic itself. It delegates scenario discovery and execution
 * to the parent class (AbstractTestNGCucumberTests) and to the Cucumber framework.
 */
@CucumberOptions(
        plugin = {
                "pretty",  // Print a human-readable summary of Cucumber execution to the console
                "html:target/cucumber-reports.html",  // Generate an HTML report at the specified path
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",  // ExtentReports adapter for richer reporting
                "com.ptaf.reporting.PerFeatureReportListener",  // Per-feature individual reports (controlled by reporting.per_feature_reports_enabled in config.yml)
                "com.ptaf.reporting.SoftAssertionReportListener",  // marks soft-failed steps as FAILED in Extent/PDF reports
                "timeline:test-output-thread/"  // Produce a timeline view of execution per thread
        },
        monochrome = true,  // Produce cleaner console output without ANSI escape codes
        glue = {"com/ptaf/stepdefinitions", "com/ptaf/hooks"},  // Packages to search for step definitions and hooks
        tags = "@secondPageTest",  // CI/test-runner will execute only scenarios or features annotated with this tag
        features = {"src/test/resources/features"}  // Location of feature files to execute
)
public class ParallelRun extends AbstractTestNGCucumberTests {

    /**
     * Provides Cucumber scenarios to TestNG for execution.
     *
     * <p>This method overrides the parent {@code scenarios()} to attach a TestNG DataProvider
     * annotation with {@code parallel = true}. TestNG will use the returned two-dimensional array
     * to schedule and execute Cucumber scenarios. Each entry in the array represents a single
     * scenario invocation.</p>
     *
     * <p>Key points for testers:
     * <ul>
     *     <li>Setting {@code parallel = true} allows TestNG to run multiple scenarios concurrently.
     *     This can significantly reduce total execution time on machines with multiple cores.</li>
     *     <li>When running in parallel, ensure that any shared resources (drivers, files, test data,
     *     application state) are handled in a thread-safe manner (for example by using ThreadLocal
     *     WebDriver instances or synchronized access to shared fixtures).</li>
     *     <li>The method returns {@code super.scenarios()} which delegates building the scenarios
     *     matrix to the AbstractTestNGCucumberTests implementation—do not modify the return value.</li>
     * </ul>
     *
     * @return a two-dimensional Object array where each inner array contains parameters for a single
     * scenario execution (as provided by the Cucumber-TestNG integration).
     */
    @Override
    @DataProvider(parallel = true)  // Instruct TestNG to run provided scenarios in parallel
    public Object[][] scenarios() {
        // Delegate scenario discovery to the parent class. The parent constructs the Object[][]
        // required by TestNG DataProvider. We do not alter or filter the scenarios here.
        return super.scenarios();
    }
}
