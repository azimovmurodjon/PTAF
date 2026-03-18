package com.ptaf.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "pretty",
                "html:target/performance-cucumber-report.html",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
        },
        tags = "@performance_testing",
        features = "src/test/resources/features/performance",
        glue = {"com.ptaf.stepdefinitions", "com.ptaf.hooks"}
)
public class PerformanceTestRunner {
}