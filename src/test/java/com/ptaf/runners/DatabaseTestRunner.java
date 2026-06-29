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
 * This runner does not manage connection details directly; it only triggers the
 * execution of scenarios that rely on the shared database utility layer.
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
 *
 * <p>
 * How to run:
 * - From IDE: Run this class as a JUnit test (right click -> Run DatabaseTestRunner).
 * - From build tools: Configure your test goal to include this runner or provide
 *   it explicitly (for example using maven-surefire's -Dtest=DatabaseTestRunner).
 * </p>
 *
 * <p>
 * Notes for Testers:
 * - Keep DB feature files that are long-running or destructive separated from
 *   fast, non-destructive tests so you can execute them independently.
 * - Verify tags on feature files or scenarios to ensure they are picked up by
 *   this runner (use @db, @database, or @sql).
 * - Check generated reports under target/cucumber-reports/ for results:
 *     database-report.html, database-report.json, database-report.xml
 * </p>
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        /*
         * Feature file location for database scenarios.
         * Keep DB feature files separated from UI/API features for clean execution control.
         *
         * - Relative path used so that the runner works consistently across developer
         *   machines and CI environments where the project root is the working dir.
         */
        features = "src/test/resources/features/db",

        /*
         * Glue packages for DB steps and DB-specific lifecycle hooks.
         * Dot notation is preferred and more readable than slash notation.
         *
         * - com.ptaf.stepdefinitions should contain Cucumber step definition classes
         *   that implement the Gherkin steps present in the DB feature files.
         * - com.ptaf.hooks should contain hook classes (e.g., for setup/teardown
         *   of DB connections or test data cleanup).
         */
        glue = {
                "com.ptaf.stepdefinitions",
                "com.ptaf.hooks"
        },

        /*
         * Only execute database-related scenarios.
         * This prevents accidental execution of UI/API/PDF scenarios from this runner.
         *
         * - The expression "@db or @database or @sql" selects any scenario or feature
         *   annotated with at least one of these tags.
         * - Use tags at the feature or scenario level depending on the desired scope.
         */
        tags = "@db or @database or @sql",

        /*
         * Plugins generate readable execution output and reports.
         *
         * - "pretty" prints readable console output for steps and results.
         * - HTML, JSON, and JUnit XML reports are written to target/cucumber-reports/
         *   for review and for CI integration (report collection, trend analysis).
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
         *
         * - dryRun=true is useful when writing or refactoring step definitions to
         *   confirm that every step in the feature files has a matching method.
         */
        dryRun = false,

        /*
         * monochrome=true makes console output easier to read in local and CI logs.
         * It removes ANSI escape codes from the output so log viewers and build
         * servers render text cleanly.
         */
        monochrome = true
)
public class DatabaseTestRunner {
        /*
         * This class intentionally remains empty.
         *
         * Why empty?
         * - Cucumber and JUnit use the annotations on this class to discover where
         *   the feature files are, which glue (step/hook) packages to load, which
         *   tags to execute, and which reporting plugins to enable.
         *
         * What to change here (if needed):
         * - Typically you do not need to add code to this class. If you need to
         *   customize runtime behavior beyond CucumberOptions, prefer to modify
         *   hook classes or the project's configuration rather than changing this runner.
         *
         * Helpful tips:
         * - If a scenario annotated with @db is not executed, confirm the feature
         *   file path and tags are correct and that the step definition classes are
         *   in the listed glue packages.
         * - If you see leftover/active DB connections after scenarios, check the
         *   DatabaseHooks implementation to ensure it closes connections in @After hooks.
         */
}
