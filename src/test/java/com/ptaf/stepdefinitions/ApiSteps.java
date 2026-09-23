package com.ptaf.stepdefinitions;

import com.ptaf.api.methods.ApiCommonMethods;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * ApiSteps contains the Gherkin step definitions used by Cucumber feature files
 * to perform API testing in a human-readable, stateful manner.
 *
 * <p>This class acts as a thin layer that translates Gherkin steps into calls
 * to ApiCommonMethods. It does not implement HTTP logic itself; instead it
 * delegates to the ApiCommonMethods helper which maintains request state
 * (headers, path/query parameters, body) and performs sending and verification
 * of responses.</p>
 *
 * <p>Typical usage in a feature file:
 * Given I set the request header "Content-Type" to "application/json"
 * And I set the path parameter "id" to "1"
 * And I set the request body to
 * """
 * { "title": "Hello" }
 * """
 * When I send a "jsonplaceholder_requests.get_single_post" request to the "jsonplaceholder" service
 * Then the response code should be 200
 * And the response body should contain the text "Hello"
 * </p>
 *
 * Notes for testers:
 * - The step parameters (like the request key and service name) are typically
 *   defined in external YAML/JSON configuration files (e.g., api_requests.yml,
 *   config.yml) and are passed straight through to ApiCommonMethods.
 * - Order matters: build the request first (headers/params/body), then send it,
 *   then verify the response.
 */
public class ApiSteps {

    /**
     * The ApiCommonMethods instance that holds request/response state and
     * performs the actual HTTP operations and verifications.
     *
     * This is final because the step definitions should share a single helper
     * instance throughout the lifetime of the ApiSteps object, preserving the
     * built-up request state between steps.
     */
    private final ApiCommonMethods apiMethods;

    /**
     * Default constructor initializes the ApiCommonMethods helper.
     *
     * ApiCommonMethods is responsible for:
     * - storing headers, path/query parameters, request body
     * - sending the HTTP request
     * - verifying response status code, headers, body content and JSON path values
     */
    public ApiSteps() {
        this.apiMethods = new ApiCommonMethods();
    }

    // --- Steps for Building the Request ---

    /**
     * Sets a request header to be used for subsequent requests.
     *
     * <p>Gherkin: Given I set the request header "Header-Name" to "HeaderValue"</p>
     *
     * @param key   the header name (case-sensitive depending on implementation)
     * @param value the header value to set
     *
     * This method delegates to ApiCommonMethods#setHeader which stores the header
     * into the request state. Call this before sending the request if the
     * header is required by the API under test.
     */
    @Given("I set the request header {string} to {string}")
    public void i_set_the_request_header(String key, String value) {
        // Delegate to the helper which records request headers for later use when sending.
        apiMethods.setHeader(key, value);
    }

    /**
     * Sets a path parameter that will be substituted into the request URL.
     *
     * <p>Gherkin: And I set the path parameter "id" to "1"</p>
     *
     * @param key   the path parameter key used in the request template (e.g., "id")
     * @param value the value to substitute for the path parameter
     *
     * The path parameter will be used when ApiCommonMethods constructs the final URL
     * for the request. Ensure the request template referenced when sending the
     * request contains a placeholder for this key.
     */
    @And("I set the path parameter {string} to {string}")
    public void i_set_the_path_parameter(String key, String value) {
        // Store the path parameter for later URL construction.
        apiMethods.setPathParameter(key, value);
    }

    /**
     * Sets a query parameter that will be appended to the request URL.
     *
     * <p>Gherkin: And I set the query parameter "sort" to "desc"</p>
     *
     * @param key   the query parameter name (e.g., "page", "sort")
     * @param value the query parameter value
     *
     * Multiple calls to this step can be used to add multiple query parameters.
     * The helper will append them appropriately when forming the request.
     */
    @And("I set the query parameter {string} to {string}")
    public void i_set_the_query_parameter(String key, String value) {
        // Store the query parameter to be appended when the request is sent.
        apiMethods.setQueryParameter(key, value);
    }

    /**
     * Sets the request body using a doc string from the feature file.
     *
     * <p>Gherkin:
     * And I set the request body to
     * """
     * { "title": "example", "body": "text" }
     * """
     * </p>
     *
     * @param docString the multi-line string provided in the feature file that
     *                  represents the full request body (JSON, XML, etc.)
     *
     * Note: The raw docString is passed directly to ApiCommonMethods#setRequestBody.
     * If you need to parameterize the body, do it in the feature file or via
     * pre-processing in your step definitions (not done here).
     */
    @And("I set the request body to")
    public void i_set_the_request_body_to(String docString) {
        // The docString from the feature file is passed directly as the request body.
        apiMethods.setRequestBody(docString);
    }

    // --- Step for Sending the Request ---

    /**
     * Sends an HTTP request based on a named request template and a service identifier.
     *
     * <p>Gherkin: When I send a "requestKey" request to the "serviceName" service</p>
     *
     * @param requestKey  a logical request identifier (typically defined in api_requests.yml).
     *                    Example: "jsonplaceholder_requests.get_single_post"
     * @param serviceName the configured service environment name (typically from config.yml).
     *                    Example: "jsonplaceholder"
     *
     * The method instructs ApiCommonMethods to build the final request using any
     * previously set headers, path/query parameters, and body, then execute it.
     * The response is stored by ApiCommonMethods for subsequent verification steps.
     */
    @When("I send a {string} request to the {string} service")
    public void i_send_a_request_to_the_service(String requestKey, String serviceName) {
        // 'requestKey' is from api_requests.yml (e.g., "jsonplaceholder_requests.get_single_post")
        // 'serviceName' is from config.yml (e.g., "jsonplaceholder")
        // Delegate to the helper which performs URL/template resolution and sends the HTTP request.
        apiMethods.sendRequest(serviceName, requestKey);
    }

    // --- Steps for Verifying the Response ---

    /**
     * Verifies the HTTP response status code matches the expected value.
     *
     * <p>Gherkin: Then the response code should be 200</p>
     *
     * @param expectedStatusCode the expected HTTP status code
     *
     * This delegates to ApiCommonMethods which should perform an assertion or
     * throw an exception when the value does not match. Test execution will fail
     * in that case, providing feedback to the tester.
     */
    @Then("the response code should be {int}")
    public void the_response_code_should_be(int expectedStatusCode) {
        // Verify response code saved by ApiCommonMethods when the request was sent.
        apiMethods.verifyResponseStatusCode(expectedStatusCode);
    }

    /**
     * Verifies that the response body contains the provided substring.
     *
     * <p>Gherkin: And the response body should contain the text "expectedText"</p>
     *
     * @param expectedText the substring expected to be present somewhere in the response body
     *
     * This is useful for simple content checks. For structured checks prefer
     * using the JSON path verification step.
     */
    @And("the response body should contain the text {string}")
    public void the_response_body_should_contain_the_text(String expectedText) {
        // Delegate to helper to check if the response body includes expected text.
        apiMethods.verifyResponseBodyContains(expectedText);
    }

    /**
     * Verifies a specific response header has the expected value.
     *
     * <p>Gherkin: And the response header "Content-Type" should be "application/json"</p>
     *
     * @param headerName    the name of the header to verify (case sensitivity depends on implementation)
     * @param expectedValue the expected value of the header
     *
     * Use this to confirm headers such as Content-Type, Cache-Control, etc.
     */
    @And("the response header {string} should be {string}")
    public void the_response_header_should_be(String headerName, String expectedValue) {
        // Delegate header verification to the helper which will assert or raise an error.
        apiMethods.verifyResponseHeader(headerName, expectedValue);
    }

    /**
     * Verifies the value at the given JSON path matches the expected string.
     *
     * <p>Gherkin: And the value of the JSON path "$.data.id" should be "123"</p>
     *
     * @param jsonPath      a JSONPath expression targeting the value to verify
     * @param expectedValue the expected value (provided as a string). The helper may
     *                      perform conversions/normalization if necessary.
     *
     * This step is intended for precise checks inside JSON responses. For complex
     * assertions or non-string types consult the ApiCommonMethods implementation.
     */
    @And("the value of the JSON path {string} should be {string}")
    public void the_value_of_the_json_path_should_be(String jsonPath, String expectedValue) {
        // Leverage the helper's JSON path evaluation and comparison routines.
        apiMethods.verifyJsonPathValue(jsonPath, expectedValue);
    }
}
