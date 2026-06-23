package com.ptaf.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/** Dedicated Appium runner for native Android and iOS scenarios. */
@RunWith(Cucumber.class)
@CucumberOptions(
        plugin = {
                "pretty",
                "html:target/cucumber-reports/mobile-report.html",
                "json:target/cucumber-reports/mobile-report.json",
                "junit:target/cucumber-reports/mobile-report.xml",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
        },
        features = "src/test/resources/features/mobile",
        glue = {"com.ptaf.stepdefinitions", "com.ptaf.hooks"},
        tags = "@theapp_smoke",
        dryRun = false,
        monochrome = true
)
public class MobileTestRunner { }
