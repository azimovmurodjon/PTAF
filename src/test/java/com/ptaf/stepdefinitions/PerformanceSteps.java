package com.ptaf.stepdefinitions;

import com.ptaf.performance.builders.PerformanceProfileBuilder;
import com.ptaf.performance.builders.PerformanceRequestBuilder;
import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.core.PerformanceEngine;
import com.ptaf.performance.models.PerformanceExecutionResult;
import com.ptaf.performance.models.PerformanceProfile;
import com.ptaf.performance.models.PerformanceRequest;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * High-level Cucumber step definitions for running performance tests and validating results.
 *
 * <p>
 * This class exposes concise, tester-facing steps to:
 * - run simple HTTP performance tests (GET/POST/PUT/DELETE),
 * - run tests with custom load profiles (users, ramp-up, hold),
 * - drive request bodies from inline JSON, YAML keys, CSV rows, or Excel sheets,
 * - authenticate requests with bearer tokens or basic auth,
 * - run tests that are expected to fail,
 * - and perform a variety of result and metric assertions.
 * </p>
 *
 * <p>
 * Implementation details are intentionally kept out of test scenarios. Testers should only use these
 * steps. The underlying PerformanceEngine, request/profile builders, and configuration properties
 * are controlled by framework authors.
 * </p>
 */
public class PerformanceSteps {

    /**
     * Core engine responsible for executing performance tests.
     *
     * <p>
     * The engine encapsulates the test execution lifecycle, reporting, and assertion evaluation.
     * Instantiated here to be reused across steps in the same scenario.
     * </p>
     */
    private final PerformanceEngine performanceEngine = new PerformanceEngine();

    /**
     * Latest performance execution result produced by the engine.
     *
     * <p>
     * Many validation steps operate against this object. It will be null until a performance step
     * runs and sets it. Use assertResultAvailable() to validate presence before reading values.
     * </p>
     */
    private PerformanceExecutionResult latestResult;

    // ============================================================
    // GET
    // ============================================================

    /**
     * Run a simple GET performance test for the supplied path and store the result.
     *
     * Example Cucumber step:
     * When we run GET performance test for path "/api/foo" with name "GetFooTest"
     *
     * @param path     HTTP path to target (relative to configured base URL)
     * @param testName Human-friendly name for the performance request (used in reports)
     */
    @When("^we run GET performance test for path \"(.*?)\" with name \"(.*?)\"$")
    public void weRunGetPerformanceTest(String path, String testName) {
        // Build a PerformanceRequest for a GET call with the provided name and path.
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
                .withPath(path)
                .build();

        // Execute the test and keep the latest result for subsequent validations.
        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run a GET performance test with a custom load profile (users, ramp-up, hold).
     *
     * Example Cucumber step:
     * When we run GET performance test for path "/api/foo" with name "GetFooTest" using 50 users ramp 30 seconds hold 60 seconds
     *
     * @param path          HTTP path to target
     * @param testName      Request name used in reports
     * @param users         Number of virtual users to simulate
     * @param rampUpSeconds Ramp-up duration in seconds (time to ramp to full concurrency)
     * @param holdSeconds   Hold duration in seconds (sustained load period)
     */
    @When("^we run GET performance test for path \"(.*?)\" with name \"(.*?)\" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$")
    public void weRunGetPerformanceTestWithCustomProfile(String path,
                                                         String testName,
                                                         int users,
                                                         int rampUpSeconds,
                                                         int holdSeconds) {

        // Build the basic request object for a GET.
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
                .withPath(path)
                .build();

        // Create a custom profile starting from defaults and overriding key parameters.
        PerformanceProfile profile = PerformanceProfileBuilder.fromDefaults()
                .withUsers(users)
                .withRampUpSeconds(rampUpSeconds)
                .withHoldSeconds(holdSeconds)
                // iterations = 0 typically means "run according to time-based profile"
                .withIterations(0)
                .build();

        // Run the test using the custom profile and the default assertion profile from configuration.
        latestResult = performanceEngine.runHttpTest(
                request,
                profile,
                PerformanceConfigurationProperties.getDefaultAssertionProfile()
        );
    }

    // ============================================================
    // POST INLINE JSON
    // ============================================================

    /**
     * Run a POST performance test using inline JSON body.
     *
     * Example:
     * When we run POST performance test for path "/api/items" with name "CreateItem" and json body "{\"name\":\"x\"}"
     *
     * @param path     Target HTTP path
     * @param testName Report-friendly request name
     * @param jsonBody Inline JSON payload to send as the request body
     */
    @When("^we run POST performance test for path \"(.*?)\" with name \"(.*?)\" and json body \"(.*?)\"$")
    public void weRunPostPerformanceTest(String path, String testName, String jsonBody) {
        // Build request with JSON body and set Accept header to application/json
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withJsonBody(jsonBody)
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run a POST performance test using inline JSON body with a custom profile.
     *
     * Example:
     * When we run POST performance test for path "/api/items" with name "CreateItem" and json body "{\"name\":\"x\"}" using 20 users ramp 10 seconds hold 30 seconds
     *
     * @param path          Target HTTP path
     * @param testName      Request name
     * @param jsonBody      Inline JSON payload
     * @param users         Number of virtual users
     * @param rampUpSeconds Ramp-up seconds
     * @param holdSeconds   Hold seconds
     */
    @When("^we run POST performance test for path \"(.*?)\" with name \"(.*?)\" and json body \"(.*?)\" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$")
    public void weRunPostPerformanceTestWithCustomProfile(String path,
                                                          String testName,
                                                          String jsonBody,
                                                          int users,
                                                          int rampUpSeconds,
                                                          int holdSeconds) {

        // Assemble the request with JSON content
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withJsonBody(jsonBody)
                .withAcceptType("application/json")
                .build();

        // Build and run with a custom profile
        PerformanceProfile profile = PerformanceProfileBuilder.fromDefaults()
                .withUsers(users)
                .withRampUpSeconds(rampUpSeconds)
                .withHoldSeconds(holdSeconds)
                .withIterations(0)
                .build();

        latestResult = performanceEngine.runHttpTest(
                request,
                profile,
                PerformanceConfigurationProperties.getDefaultAssertionProfile()
        );
    }

    // ============================================================
    // PUT INLINE JSON
    // ============================================================

    /**
     * Run a PUT performance test using inline JSON body.
     *
     * @param path     Target path
     * @param testName Name used in reports
     * @param jsonBody Inline JSON payload
     */
    @When("^we run PUT performance test for path \"(.*?)\" with name \"(.*?)\" and json body \"(.*?)\"$")
    public void weRunPutPerformanceTest(String path, String testName, String jsonBody) {
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("PUT")
                .withPath(path)
                .withJsonBody(jsonBody)
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run a PUT performance test with inline JSON body and custom profile parameters.
     *
     * @param path          Target path
     * @param testName      Report name
     * @param jsonBody      Inline JSON payload
     * @param users         Virtual users count
     * @param rampUpSeconds Ramp-up seconds
     * @param holdSeconds   Hold seconds
     */
    @When("^we run PUT performance test for path \"(.*?)\" with name \"(.*?)\" and json body \"(.*?)\" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$")
    public void weRunPutPerformanceTestWithCustomProfile(String path,
                                                         String testName,
                                                         String jsonBody,
                                                         int users,
                                                         int rampUpSeconds,
                                                         int holdSeconds) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("PUT")
                .withPath(path)
                .withJsonBody(jsonBody)
                .withAcceptType("application/json")
                .build();

        PerformanceProfile profile = PerformanceProfileBuilder.fromDefaults()
                .withUsers(users)
                .withRampUpSeconds(rampUpSeconds)
                .withHoldSeconds(holdSeconds)
                .withIterations(0)
                .build();

        latestResult = performanceEngine.runHttpTest(
                request,
                profile,
                PerformanceConfigurationProperties.getDefaultAssertionProfile()
        );
    }

    // ============================================================
    // DELETE
    // ============================================================

    /**
     * Run a DELETE performance test for the given path.
     *
     * @param path     Target path to delete
     * @param testName Report name for this request
     */
    @When("^we run DELETE performance test for path \"(.*?)\" with name \"(.*?)\"$")
    public void weRunDeletePerformanceTest(String path, String testName) {
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("DELETE")
                .withPath(path)
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run DELETE performance test with custom load profile.
     *
     * @param path          Target path
     * @param testName      Request name
     * @param users         Virtual users
     * @param rampUpSeconds Ramp-up seconds
     * @param holdSeconds   Hold seconds
     */
    @When("^we run DELETE performance test for path \"(.*?)\" with name \"(.*?)\" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$")
    public void weRunDeletePerformanceTestWithCustomProfile(String path,
                                                            String testName,
                                                            int users,
                                                            int rampUpSeconds,
                                                            int holdSeconds) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("DELETE")
                .withPath(path)
                .build();

        PerformanceProfile profile = PerformanceProfileBuilder.fromDefaults()
                .withUsers(users)
                .withRampUpSeconds(rampUpSeconds)
                .withHoldSeconds(holdSeconds)
                .withIterations(0)
                .build();

        latestResult = performanceEngine.runHttpTest(
                request,
                profile,
                PerformanceConfigurationProperties.getDefaultAssertionProfile()
        );
    }

    // ============================================================
    // YAML-DRIVEN PAYLOADS
    // ============================================================

    /**
     * Run a POST where the request body is resolved from YAML using a provided key.
     *
     * <p>
     * YAML-driven payloads are useful for storing reusable example payloads in a central YAML file
     * and referencing them by key in tests.
     * </p>
     *
     * @param path     Target path
     * @param testName Report name
     * @param yamlKey  Key referencing the YAML payload to use as body
     */
    @When("^we run YAML-driven POST performance test for path \"(.*?)\" with name \"(.*?)\" using yaml key \"(.*?)\"$")
    public void weRunYamlDrivenPostPerformanceTest(String path,
                                                   String testName,
                                                   String yamlKey) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withYamlBodyKey(yamlKey)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run a PUT where the request body is resolved from YAML using the given key.
     *
     * @param path     Target path
     * @param testName Report name
     * @param yamlKey  YAML key to resolve the body
     */
    @When("^we run YAML-driven PUT performance test for path \"(.*?)\" with name \"(.*?)\" using yaml key \"(.*?)\"$")
    public void weRunYamlDrivenPutPerformanceTest(String path,
                                                  String testName,
                                                  String yamlKey) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("PUT")
                .withPath(path)
                .withYamlBodyKey(yamlKey)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    // ============================================================
    // CSV-DRIVEN PAYLOADS
    // ============================================================

    /**
     * Run a POST where the request body is sourced from a CSV file.
     *
     * <p>
     * The CSV file is referenced by name (framework resolves location). Row is identified by a
     * rowIdentifier (likely a lookup key), and a specific column value is used as the body.
     * </p>
     *
     * @param path          Target path
     * @param testName      Report name
     * @param csvFile       CSV file name or path recognized by framework
     * @param rowIdentifier Identifier for the desired row in the CSV
     * @param columnName    Column name whose value will be used as the request body
     */
    @When("^we run CSV-driven POST performance test for path \"(.*?)\" with name \"(.*?)\" using csv file \"(.*?)\" row \"(.*?)\" column \"(.*?)\"$")
    public void weRunCsvDrivenPostPerformanceTest(String path,
                                                  String testName,
                                                  String csvFile,
                                                  String rowIdentifier,
                                                  String columnName) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withCsvBody(csvFile, rowIdentifier, columnName)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run a PUT where the body is taken from a CSV file cell.
     *
     * @param path          Target path
     * @param testName      Name for reports
     * @param csvFile       CSV file
     * @param rowIdentifier Row identifier within CSV
     * @param columnName    Column name to use as body
     */
    @When("^we run CSV-driven PUT performance test for path \"(.*?)\" with name \"(.*?)\" using csv file \"(.*?)\" row \"(.*?)\" column \"(.*?)\"$")
    public void weRunCsvDrivenPutPerformanceTest(String path,
                                                 String testName,
                                                 String csvFile,
                                                 String rowIdentifier,
                                                 String columnName) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("PUT")
                .withPath(path)
                .withCsvBody(csvFile, rowIdentifier, columnName)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    // ============================================================
    // EXCEL-DRIVEN PAYLOADS
    // ============================================================

    /**
     * Run a POST where the body is sourced from an Excel file cell.
     *
     * @param path          Target path
     * @param testName      Report name
     * @param excelFile     Excel file name or path recognized by framework
     * @param rowIdentifier Row lookup identifier (could be a key value or row number)
     * @param columnName    Column header whose cell will be used as the body
     */
    @When("^we run Excel-driven POST performance test for path \"(.*?)\" with name \"(.*?)\" using excel file \"(.*?)\" row \"(.*?)\" column \"(.*?)\"$")
    public void weRunExcelDrivenPostPerformanceTest(String path,
                                                    String testName,
                                                    String excelFile,
                                                    String rowIdentifier,
                                                    String columnName) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withExcelBody(excelFile, rowIdentifier, columnName)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run a PUT where the body is sourced from an Excel file cell.
     *
     * @param path          Target path
     * @param testName      Request name
     * @param excelFile     Excel file
     * @param rowIdentifier Row identifier
     * @param columnName    Column name
     */
    @When("^we run Excel-driven PUT performance test for path \"(.*?)\" with name \"(.*?)\" using excel file \"(.*?)\" row \"(.*?)\" column \"(.*?)\"$")
    public void weRunExcelDrivenPutPerformanceTest(String path,
                                                   String testName,
                                                   String excelFile,
                                                   String rowIdentifier,
                                                   String columnName) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("PUT")
                .withPath(path)
                .withExcelBody(excelFile, rowIdentifier, columnName)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    // ============================================================
    // BEARER TOKEN AUTH
    // ============================================================

    /**
     * Store a bearer token in the engine's token store under an alias.
     *
     * <p>
     * This allows subsequent requests to refer to stored tokens by alias rather than embedding
     * tokens into scenarios or source files.
     * </p>
     *
     * @param alias      Alias to store the token under
     * @param tokenValue Raw token string (e.g. JWT)
     */
    @When("^we store bearer token alias \"(.*?)\" with value \"(.*?)\"$")
    public void weStoreBearerTokenAlias(String alias, String tokenValue) {
        performanceEngine.storeBearerToken(alias, tokenValue);
    }

    /**
     * Run an authenticated GET using a stored bearer token alias.
     *
     * @param path       Target path
     * @param testName   Report name
     * @param tokenAlias Alias previously stored with weStoreBearerTokenAlias
     */
    @When("^we run authenticated GET performance test for path \"(.*?)\" with name \"(.*?)\" using bearer token alias \"(.*?)\"$")
    public void weRunAuthenticatedGetPerformanceTest(String path,
                                                     String testName,
                                                     String tokenAlias) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
                .withPath(path)
                .withBearerTokenAlias(tokenAlias)
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    /**
     * Run an authenticated YAML-driven POST using a stored bearer token alias.
     *
     * @param path       Target path
     * @param testName   Report name
     * @param yamlKey    YAML key to resolve body
     * @param tokenAlias Bearer token alias stored previously
     */
    @When("^we run authenticated YAML-driven POST performance test for path \"(.*?)\" with name \"(.*?)\" using yaml key \"(.*?)\" and bearer token alias \"(.*?)\"$")
    public void weRunAuthenticatedYamlDrivenPostPerformanceTest(String path,
                                                                String testName,
                                                                String yamlKey,
                                                                String tokenAlias) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withYamlBodyKey(yamlKey)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .withBearerTokenAlias(tokenAlias)
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    // ============================================================
    // BASIC AUTH
    // ============================================================

    /**
     * Run a GET performance test using HTTP Basic Authentication.
     *
     * @param path     Target path
     * @param testName Report name
     * @param username Basic auth username
     * @param password Basic auth password
     */
    @When("^we run basic auth GET performance test for path \"(.*?)\" with name \"(.*?)\" username \"(.*?)\" password \"(.*?)\"$")
    public void weRunBasicAuthGetPerformanceTest(String path,
                                                 String testName,
                                                 String username,
                                                 String password) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
                .withPath(path)
                .withBasicAuth(username, password)
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    // ============================================================
    // EXPECTED FAILURE EXECUTION
    // ============================================================

    /**
     * Run a GET performance test that is expected to fail.
     *
     * <p>
     * Use this when the scenario is verifying error handling or negative cases. The engine will
     * treat failures as anticipated and populate the result accordingly.
     * </p>
     *
     * @param path     Target path
     * @param testName Request name
     */
    @When("^we run GET performance test expecting failure for path \"(.*?)\" with name \"(.*?)\"$")
    public void weRunGetPerformanceTestExpectingFailure(String path, String testName) {
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
                .withPath(path)
                .build();

        latestResult = performanceEngine.runHttpTestExpectingFailure(request);
    }

    /**
     * Run a GET with basic auth that is expected to fail.
     *
     * @param path     Target path
     * @param testName Request name
     * @param username Basic auth username
     * @param password Basic auth password
     */
    @When("^we run basic auth GET performance test expecting failure for path \"(.*?)\" with name \"(.*?)\" username \"(.*?)\" password \"(.*?)\"$")
    public void weRunBasicAuthGetPerformanceTestExpectingFailure(String path,
                                                                 String testName,
                                                                 String username,
                                                                 String password) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
                .withPath(path)
                .withBasicAuth(username, password)
                .build();

        latestResult = performanceEngine.runHttpTestExpectingFailure(request);
    }

    /**
     * Run a YAML-driven POST that is expected to fail.
     *
     * @param path     Target path
     * @param testName Request name
     * @param yamlKey  YAML key for the body
     */
    @When("^we run YAML-driven POST performance test expecting failure for path \"(.*?)\" with name \"(.*?)\" using yaml key \"(.*?)\"$")
    public void weRunYamlDrivenPostPerformanceTestExpectingFailure(String path,
                                                                   String testName,
                                                                   String yamlKey) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withYamlBodyKey(yamlKey)
                .withContentType("application/json")
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTestExpectingFailure(request);
    }

    // ============================================================
    // CORE FILE / RESULT VALIDATIONS
    // ============================================================

    /**
     * Assert that a performance result object is available (i.e. a test has been run).
     *
     * Throws AssertionError if no result is present.
     */
    @Then("^performance result should be available$")
    public void performanceResultShouldBeAvailable() {
        assertResultAvailable();
    }

    /**
     * Assert that the engine generated a dashboard path for the last run.
     *
     * Throws AssertionError if result missing or dashboard path blank.
     */
    @Then("^performance dashboard path should be generated$")
    public void performanceDashboardPathShouldBeGenerated() {
        assertResultAvailable();
        assertNotBlank(latestResult.getDashboardPath(), "Performance dashboard path was not generated.");
    }

    /**
     * Assert that the engine generated a summary file path for the last run.
     */
    @Then("^performance summary file path should be generated$")
    public void performanceSummaryFilePathShouldBeGenerated() {
        assertResultAvailable();
        assertNotBlank(latestResult.getSummaryFilePath(), "Performance summary file path was not generated.");
    }

    /**
     * Assert that the engine generated a readable summary file path for the last run.
     */
    @Then("^performance readable summary file path should be generated$")
    public void performanceReadableSummaryFilePathShouldBeGenerated() {
        assertResultAvailable();
        assertNotBlank(latestResult.getReadableSummaryFilePath(), "Performance readable summary file path was not generated.");
    }

    /**
     * Assert that the engine generated a JTL file path for the last run.
     */
    @Then("^performance jtl file path should be generated$")
    public void performanceJtlFilePathShouldBeGenerated() {
        assertResultAvailable();
        assertNotBlank(latestResult.getJtlFilePath(), "Performance JTL file path was not generated.");
    }

    /**
     * Assert that the engine generated a root path for the run report.
     */
    @Then("^performance run report root path should be generated$")
    public void performanceRunReportRootPathShouldBeGenerated() {
        assertResultAvailable();
        assertNotBlank(latestResult.getRunReportRootPath(), "Performance run report root path was not generated.");
    }

    /**
     * Assert that the Excel report for the run was generated on disk.
     *
     * Throws AssertionError if the report file does not exist.
     */
    @Then("^performance excel report should be generated$")
    public void performanceExcelReportShouldBeGenerated() {
        assertResultAvailable();

        Path excelReportPath = Path.of(latestResult.getRunReportRootPath(), "performance-run-report.xlsx");
        if (!Files.exists(excelReportPath)) {
            throw new AssertionError("Performance Excel report was not generated at path: " + excelReportPath);
        }
    }

    // ============================================================
    // EXECUTION / STATUS VALIDATIONS
    // ============================================================

    /**
     * Assert that the last performance execution passed its configured assertions.
     *
     * Throws AssertionError with the failure message when execution didn't pass.
     */
    @Then("^performance execution should pass$")
    public void performanceExecutionShouldPass() {
        assertResultAvailable();

        if (!latestResult.isExecutionPassed()) {
            throw new AssertionError(
                    "Expected performance execution to pass, but it failed. Failure message: "
                            + latestResult.getFailureMessage()
            );
        }
    }

    /**
     * Assert that the last performance execution failed (an actual failure was detected).
     *
     * Useful for negative tests where a failure is expected.
     */
    @Then("^performance execution should fail$")
    public void performanceExecutionShouldFail() {
        assertResultAvailable();

        if (!latestResult.isActualFailureDetected()) {
            throw new AssertionError("Expected performance execution to fail, but no actual failure was detected.");
        }
    }

    /**
     * Assert that the engine was running in expected-failure mode for the last execution.
     *
     * Expected-failure mode means the run was executed with the intention to validate that a
     * failure occurs and be reported as such.
     */
    @Then("^performance execution should be in expected failure mode$")
    public void performanceExecutionShouldBeInExpectedFailureMode() {
        assertResultAvailable();

        if (!latestResult.isExpectedFailureMode()) {
            throw new AssertionError("Expected performance execution to be in expected failure mode.");
        }
    }

    /**
     * Assert that the failure message contains the provided text fragment.
     *
     * @param expectedText Text fragment expected to be present in the failure message
     */
    @Then("^performance failure message should contain \"(.*?)\"$")
    public void performanceFailureMessageShouldContain(String expectedText) {
        assertResultAvailable();

        String actualMessage = latestResult.getFailureMessage();
        if (actualMessage == null || !actualMessage.contains(expectedText)) {
            throw new AssertionError(
                    "Expected failure message to contain [" + expectedText + "] but actual was [" + actualMessage + "]"
            );
        }
    }

    // ============================================================
    // METRIC VALIDATIONS
    // ============================================================

    /**
     * Validate that the average response time observed is less than the provided threshold.
     *
     * @param maxAverageResponseTime Maximum allowed average response time in milliseconds
     */
    @Then("^performance average response time should be less than (\\d+) ms$")
    public void performanceAverageResponseTimeShouldBeLessThan(long maxAverageResponseTime) {
        assertResultAvailable();

        if (latestResult.getAverageResponseTimeMs() > maxAverageResponseTime) {
            throw new AssertionError(
                    "Average response time validation failed. Actual: "
                            + latestResult.getAverageResponseTimeMs()
                            + " ms, Expected less than: "
                            + maxAverageResponseTime
                            + " ms"
            );
        }
    }

    /**
     * Validate that the 95th percentile response time (P95) is below the provided value.
     *
     * @param maxP95ResponseTime Maximum allowed P95 in milliseconds
     */
    @Then("^performance p95 response time should be less than (\\d+) ms$")
    public void performanceP95ResponseTimeShouldBeLessThan(long maxP95ResponseTime) {
        assertResultAvailable();

        if (latestResult.getP95ResponseTimeMs() > maxP95ResponseTime) {
            throw new AssertionError(
                    "P95 response time validation failed. Actual: "
                            + latestResult.getP95ResponseTimeMs()
                            + " ms, Expected less than: "
                            + maxP95ResponseTime
                            + " ms"
            );
        }
    }

    /**
     * Validate that the error percentage is less than the provided threshold.
     *
     * @param maxErrorPercentage Maximum allowed error percentage (e.g. 1.5)
     */
    @Then("^performance error percentage should be less than (\\d+(?:\\.\\d+)?)$")
    public void performanceErrorPercentageShouldBeLessThan(double maxErrorPercentage) {
        assertResultAvailable();

        if (latestResult.getErrorPercent() > maxErrorPercentage) {
            throw new AssertionError(
                    "Error percentage validation failed. Actual: "
                            + latestResult.getErrorPercent()
                            + ", Expected less than: "
                            + maxErrorPercentage
            );
        }
    }

    /**
     * Validate that the error percentage is greater than the provided minimum.
     *
     * Useful for tests that expect a certain level of failures.
     *
     * @param minimumErrorPercentage Minimum expected error percentage
     */
    @Then("^performance error percentage should be greater than (\\d+(?:\\.\\d+)?)$")
    public void performanceErrorPercentageShouldBeGreaterThan(double minimumErrorPercentage) {
        assertResultAvailable();

        if (latestResult.getErrorPercent() <= minimumErrorPercentage) {
            throw new AssertionError(
                    "Expected error percentage to be greater than "
                            + minimumErrorPercentage
                            + " but actual was "
                            + latestResult.getErrorPercent()
            );
        }
    }

    /**
     * Validate the total number of errors observed exceeds a threshold.
     *
     * @param expectedMinimumErrors Minimum expected error count
     */
    @Then("^performance total errors should be greater than (\\d+)$")
    public void performanceTotalErrorsShouldBeGreaterThan(long expectedMinimumErrors) {
        assertResultAvailable();

        if (latestResult.getTotalErrors() <= expectedMinimumErrors) {
            throw new AssertionError(
                    "Expected total errors to be greater than "
                            + expectedMinimumErrors
                            + " but actual was "
                            + latestResult.getTotalErrors()
            );
        }
    }

    /**
     * Validate that the total number of samples (requests) exceeds the provided threshold.
     *
     * @param expectedMinimumSamples Minimum expected samples count
     */
    @Then("^performance total samples should be greater than (\\d+)$")
    public void performanceTotalSamplesShouldBeGreaterThan(long expectedMinimumSamples) {
        assertResultAvailable();

        if (latestResult.getTotalSamples() <= expectedMinimumSamples) {
            throw new AssertionError(
                    "Expected total samples to be greater than "
                            + expectedMinimumSamples
                            + " but actual was "
                            + latestResult.getTotalSamples()
            );
        }
    }

    /**
     * Validate that the total scenario duration is greater than the given millisecond value.
     *
     * @param minimumDurationMs Minimum expected total scenario duration in milliseconds
     */
    @Then("^performance total scenario duration should be greater than (\\d+) ms$")
    public void performanceTotalScenarioDurationShouldBeGreaterThan(long minimumDurationMs) {
        assertResultAvailable();

        if (latestResult.getTotalScenarioDurationMs() <= minimumDurationMs) {
            throw new AssertionError(
                    "Expected total scenario duration to be greater than "
                            + minimumDurationMs
                            + " ms but actual was "
                            + latestResult.getTotalScenarioDurationMs()
                            + " ms"
            );
        }
    }

    // ============================================================
    // SMART REPORTING VALIDATIONS
    // ============================================================

    /**
     * Validate the risk score computed in the smart report is greater than the provided minimum.
     *
     * @param minimumRiskScore Minimum expected risk score (inclusive)
     */
    @Then("^performance risk score should be greater than (\\d+)$")
    public void performanceRiskScoreShouldBeGreaterThan(int minimumRiskScore) {
        assertResultAvailable();

        if (latestResult.getRiskScore() <= minimumRiskScore) {
            throw new AssertionError(
                    "Expected risk score to be greater than "
                            + minimumRiskScore
                            + " but actual was "
                            + latestResult.getRiskScore()
            );
        }
    }

    /**
     * Validate the risk score is less than the provided maximum.
     *
     * @param maximumRiskScore Maximum allowed risk score
     */
    @Then("^performance risk score should be less than (\\d+)$")
    public void performanceRiskScoreShouldBeLessThan(int maximumRiskScore) {
        assertResultAvailable();

        if (latestResult.getRiskScore() >= maximumRiskScore) {
            throw new AssertionError(
                    "Expected risk score to be less than "
                            + maximumRiskScore
                            + " but actual was "
                            + latestResult.getRiskScore()
            );
        }
    }

    /**
     * Validate the textual risk level equals the expected value (case-insensitive).
     *
     * @param expectedRiskLevel Expected risk level string (e.g. "LOW", "MEDIUM", "HIGH")
     */
    @Then("^performance risk level should be \"(.*?)\"$")
    public void performanceRiskLevelShouldBe(String expectedRiskLevel) {
        assertResultAvailable();

        String actualRiskLevel = latestResult.getRiskLevel();
        if (actualRiskLevel == null || !actualRiskLevel.equalsIgnoreCase(expectedRiskLevel)) {
            throw new AssertionError(
                    "Expected risk level to be [" + expectedRiskLevel + "] but actual was [" + actualRiskLevel + "]"
            );
        }
    }

    /**
     * Validate that the threshold breach summary contains the provided text.
     *
     * @param expectedText Fragment expected in threshold breach summary
     */
    @Then("^performance threshold breach summary should contain \"(.*?)\"$")
    public void performanceThresholdBreachSummaryShouldContain(String expectedText) {
        assertResultAvailable();

        String actualSummary = latestResult.getThresholdBreachSummary();
        if (actualSummary == null || !actualSummary.contains(expectedText)) {
            throw new AssertionError(
                    "Expected threshold breach summary to contain [" + expectedText + "] but actual was [" + actualSummary + "]"
            );
        }
    }

    /**
     * Validate that the recommended action text contains the provided fragment.
     *
     * @param expectedText Fragment expected in recommended action
     */
    @Then("^performance recommended action should contain \"(.*?)\"$")
    public void performanceRecommendedActionShouldContain(String expectedText) {
        assertResultAvailable();

        String actualAction = latestResult.getRecommendedAction();
        if (actualAction == null || !actualAction.contains(expectedText)) {
            throw new AssertionError(
                    "Expected recommended action to contain [" + expectedText + "] but actual was [" + actualAction + "]"
            );
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Helper that ensures a performance result exists before attempting validations.
     *
     * Throws AssertionError if no test has been executed (latestResult is null).
     */
    private void assertResultAvailable() {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }
    }

    /**
     * Helper that validates a String is neither null nor blank.
     *
     * Throws AssertionError with the supplied message when the validation fails.
     *
     * @param value   String to validate
     * @param message Assertion message for failures
     */
    private void assertNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AssertionError(message);
        }
    }
}
