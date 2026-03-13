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

/**
 * High-level performance step definitions.
 *
 * <p>Testers should use only these high-level steps.
 * Framework internals, JMeter DSL, and engine-level classes remain architect-controlled.</p>
 */
public class PerformanceSteps {

    private final PerformanceEngine performanceEngine = new PerformanceEngine();
    private PerformanceExecutionResult latestResult;

    // ============================================================
    // GET
    // ============================================================

    @When("^we run GET performance test for path \"(.*?)\" with name \"(.*?)\"$")
    public void weRunGetPerformanceTest(String path, String testName) {
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
                .withPath(path)
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    @When("^we run GET performance test for path \"(.*?)\" with name \"(.*?)\" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$")
    public void weRunGetPerformanceTestWithCustomProfile(String path,
                                                         String testName,
                                                         int users,
                                                         int rampUpSeconds,
                                                         int holdSeconds) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("GET")
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
    // POST INLINE JSON
    // ============================================================

    @When("^we run POST performance test for path \"(.*?)\" with name \"(.*?)\" and json body \"(.*?)\"$")
    public void weRunPostPerformanceTest(String path, String testName, String jsonBody) {
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
                .withPath(path)
                .withJsonBody(jsonBody)
                .withAcceptType("application/json")
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

    @When("^we run POST performance test for path \"(.*?)\" with name \"(.*?)\" and json body \"(.*?)\" using (\\d+) users ramp (\\d+) seconds hold (\\d+) seconds$")
    public void weRunPostPerformanceTestWithCustomProfile(String path,
                                                          String testName,
                                                          String jsonBody,
                                                          int users,
                                                          int rampUpSeconds,
                                                          int holdSeconds) {

        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("POST")
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
    // PUT INLINE JSON
    // ============================================================

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

    @When("^we run DELETE performance test for path \"(.*?)\" with name \"(.*?)\"$")
    public void weRunDeletePerformanceTest(String path, String testName) {
        PerformanceRequest request = new PerformanceRequestBuilder()
                .withRequestName(testName)
                .withMethod("DELETE")
                .withPath(path)
                .build();

        latestResult = performanceEngine.runHttpTest(request);
    }

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

    @When("^we store bearer token alias \"(.*?)\" with value \"(.*?)\"$")
    public void weStoreBearerTokenAlias(String alias, String tokenValue) {
        performanceEngine.getAuthTokenManager().saveToken(alias, tokenValue);
    }

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
    // POSITIVE VALIDATIONS
    // ============================================================

    @Then("^performance dashboard path should be generated$")
    public void performanceDashboardPathShouldBeGenerated() {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

        if (latestResult.getDashboardPath() == null || latestResult.getDashboardPath().isBlank()) {
            throw new AssertionError("Performance dashboard path was not generated.");
        }
    }

    @Then("^performance result should be available$")
    public void performanceResultShouldBeAvailable() {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }
    }

    @Then("^performance summary file path should be generated$")
    public void performanceSummaryFilePathShouldBeGenerated() {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

        if (latestResult.getSummaryFilePath() == null || latestResult.getSummaryFilePath().isBlank()) {
            throw new AssertionError("Performance summary file path was not generated.");
        }
    }

    @Then("^performance jtl file path should be generated$")
    public void performanceJtlFilePathShouldBeGenerated() {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

        if (latestResult.getJtlFilePath() == null || latestResult.getJtlFilePath().isBlank()) {
            throw new AssertionError("Performance JTL file path was not generated.");
        }
    }

    @Then("^performance average response time should be less than (\\d+) ms$")
    public void performanceAverageResponseTimeShouldBeLessThan(long maxAverageResponseTime) {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

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

    @Then("^performance error percentage should be less than (\\d+(?:\\.\\d+)?)$")
    public void performanceErrorPercentageShouldBeLessThan(double maxErrorPercentage) {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

        if (latestResult.getErrorPercent() > maxErrorPercentage) {
            throw new AssertionError(
                    "Error percentage validation failed. Actual: "
                            + latestResult.getErrorPercent()
                            + ", Expected less than: "
                            + maxErrorPercentage
            );
        }
    }

    // ============================================================
    // NEGATIVE / FAILURE VALIDATIONS
    // ============================================================

    @Then("^performance total errors should be greater than (\\d+)$")
    public void performanceTotalErrorsShouldBeGreaterThan(long expectedMinimumErrors) {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

        if (latestResult.getTotalErrors() <= expectedMinimumErrors) {
            throw new AssertionError(
                    "Expected total errors to be greater than "
                            + expectedMinimumErrors
                            + " but actual was "
                            + latestResult.getTotalErrors()
            );
        }
    }

    @Then("^performance error percentage should be greater than (\\d+(?:\\.\\d+)?)$")
    public void performanceErrorPercentageShouldBeGreaterThan(double minimumErrorPercentage) {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

        if (latestResult.getErrorPercent() <= minimumErrorPercentage) {
            throw new AssertionError(
                    "Expected error percentage to be greater than "
                            + minimumErrorPercentage
                            + " but actual was "
                            + latestResult.getErrorPercent()
            );
        }
    }

    @Then("^performance total samples should be greater than (\\d+)$")
    public void performanceTotalSamplesShouldBeGreaterThan(long expectedMinimumSamples) {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

        if (latestResult.getTotalSamples() <= expectedMinimumSamples) {
            throw new AssertionError(
                    "Expected total samples to be greater than "
                            + expectedMinimumSamples
                            + " but actual was "
                            + latestResult.getTotalSamples()
            );
        }
    }

    @Then("^performance p95 response time should be less than (\\d+) ms$")
    public void performanceP95ResponseTimeShouldBeLessThan(long maxP95ResponseTime) {
        if (latestResult == null) {
            throw new AssertionError("Performance result is null.");
        }

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
}