package com.ptaf.stepdefinitions;

import com.ptaf.api.methods.ApiCommonMethods;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * ApiSteps contains the Gherkin step definitions for API testing.
 * It provides a user-friendly, stateful way to build a request, send it,
 * and then validate the response.
 */
public class ApiSteps {

    private final ApiCommonMethods apiMethods;

    public ApiSteps() {
        this.apiMethods = new ApiCommonMethods();
    }

    // --- Steps for Building the Request ---

    @Given("I set the request header {string} to {string}")
    public void i_set_the_request_header(String key, String value) {
        apiMethods.setHeader(key, value);
    }

    @And("I set the path parameter {string} to {string}")
    public void i_set_the_path_parameter(String key, String value) {
        apiMethods.setPathParameter(key, value);
    }

    @And("I set the query parameter {string} to {string}")
    public void i_set_the_query_parameter(String key, String value) {
        apiMethods.setQueryParameter(key, value);
    }

    @And("I set the request body to")
    public void i_set_the_request_body_to(String docString) {
        // The docString from the feature file is passed directly as the request body.
        apiMethods.setRequestBody(docString);
    }

    // --- Step for Sending the Request ---

    @When("I send a {string} request to the {string} service")
    public void i_send_a_request_to_the_service(String requestKey, String serviceName) {
        // 'requestKey' is from api_requests.yml (e.g., "jsonplaceholder_requests.get_single_post")
        // 'serviceName' is from config.yml (e.g., "jsonplaceholder")
        apiMethods.sendRequest(serviceName, requestKey);
    }

    // --- Steps for Verifying the Response ---

    @Then("the response code should be {int}")
    public void the_response_code_should_be(int expectedStatusCode) {
        apiMethods.verifyResponseStatusCode(expectedStatusCode);
    }

    @And("the response body should contain the text {string}")
    public void the_response_body_should_contain_the_text(String expectedText) {
        apiMethods.verifyResponseBodyContains(expectedText);
    }

    @And("the response header {string} should be {string}")
    public void the_response_header_should_be(String headerName, String expectedValue) {
        apiMethods.verifyResponseHeader(headerName, expectedValue);
    }

    @And("the value of the JSON path {string} should be {string}")
    public void the_value_of_the_json_path_should_be(String jsonPath, String expectedValue) {
        apiMethods.verifyJsonPathValue(jsonPath, expectedValue);
    }
}