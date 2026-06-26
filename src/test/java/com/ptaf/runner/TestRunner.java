package com.ptaf.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.ptaf.stepdefinitions"},
        tags = "@google_mobile_browser and @android",
        plugin = {"pretty", "html:target/cucumber-reports/cucumber-pretty", "json:target/cucumber-reports/CucumberTestReport.json", "rerun:target/cucumber-reports/rerun.txt"}
)
public class TestRunner extends AbstractTestNGCucumberTests {
}
