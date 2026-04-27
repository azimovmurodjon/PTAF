package com.ptaf.runners;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * DatabaseTestRunner is the dedicated Cucumber runner for database automation scenarios.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This runner is responsible for executing database validation scenarios only.
 * Keeping DB execution separated from UI, API, PDF, and Performance runners helps
 * teams run targeted test suites based on the testing scope.
 * </p>
 *
 * <p>
 * SQL Server Support:
 * The database framework layer is configured to support Microsoft SQL Server.
 * Connection details are controlled from config.yml through DatabaseHandler.
 * This runner does not manage connection details directly.
 * </p>
 *
 * <p>
 * Glue Configuration:
 * The glue paths below load:
 * </p>
 *
 * <ul>
 *     <li>Database step definitions from com.ptaf.stepdefinitions</li>
 *     <li>Database cleanup hooks from com.ptaf.hooks</li>
 * </ul>
 *
 * <p>
 * Tag Usage:
 * Database scenarios should use one of the following tags:
 * </p>
 *
 * <ul>
 *     <li>@db</li>
 *     <li>@database</li>
 *     <li>@sql</li>
 * </ul>
 *
 * <p>
 * The DatabaseHooks class uses the same tags to automatically close database
 * connections after each database scenario.
 * </p>
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        /*
         * Feature file location for database scenarios.
         * Keep DB feature files separated from UI/API features for clean execution control.
         */
        features = "src/test/resources/features/db",

        /*
         * Glue packages for DB steps and DB-specific lifecycle hooks.
         * Dot notation is preferred and more readable than slash notation.
         */
        glue = {
                "com.ptaf.stepdefinitions",
                "com.ptaf.hooks"
        },

        /*
         * Only execute database-related scenarios.
         * This prevents accidental execution of UI/API/PDF scenarios from this runner.
         */
        tags = "@db or @database or @sql",

        /*
         * Plugins generate readable execution output and reports.
         */
        plugin = {
                "pretty",
                "html:target/cucumber-reports/database-report.html",
                "json:target/cucumber-reports/database-report.json",
                "junit:target/cucumber-reports/database-report.xml"
        },

        /*
         * Set to true to validate feature-step mapping without executing tests.
         * Keep false for normal execution.
         */
        dryRun = false,

        /*
         * monochrome=true makes console output easier to read in local and CI logs.
         */
        monochrome = true
)
public class DatabaseTestRunner {
        /*
         * This class intentionally remains empty.
         *
         * Cucumber uses annotations above to locate feature files, step definitions,
         * hooks, tags, and reporting plugins.
         */
}