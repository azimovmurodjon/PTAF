package com.ptaf.runners;


import org.testng.annotations.DataProvider;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Configures and runs Cucumber tests in parallel using TestNG.
 * Extends AbstractTestNGCucumberTests to integrate Cucumber with TestNG.
 */
@CucumberOptions(
        plugin = {
                "pretty",  // Print a readable format of the test execution results to the console
                "html:target/cucumber-reports.html",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",  // ExtentReports plugin for detailed reporting
                "timeline:test-output-thread/"  // Generate a timeline of test execution
        },
        monochrome = true,  // Improve readability of console output
        glue = {"com/ptaf/stepdefinitions", "com/ptaf/hooks"},  // Path to the step definitions
        tags = "@secondPageTest",
        features = {"src/test/resources/features"}  // Path to the feature files
)
public class ParallelRun extends AbstractTestNGCucumberTests {

    /**
     * Provides test scenarios in parallel to TestNG.
     *
     * @return An array of scenarios to be run in parallel.
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();  // Retrieve scenarios from the parent class
    }
}