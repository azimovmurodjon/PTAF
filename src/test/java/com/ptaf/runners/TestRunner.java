package com.ptaf.runners;

import org.junit.runner.RunWith;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

// Specify the runner to use for executing the tests
@RunWith(Cucumber.class)
@CucumberOptions(
        // Define the output formats for the test reports
        plugin = {"pretty",                      // Print the test results in a readable format
                "html:target/cucumber-reports.html",  // Generate an HTML report in the specified location
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:",  // Generate an HTML report in the specified location
                "timeline:test-output-thread/"
        },
        tags = "@secondPageTest",
        // Specify the location of the feature files
        features = "src/test/resources/features",
        // Specify the packages containing the step definitions and hooks
        glue = {"com/ptaf/stepdefinitions", "com/ptaf/hooks"}
)
public class TestRunner {
        // This class is intentionally empty. It serves as an entry point for the Cucumber tests.
}
