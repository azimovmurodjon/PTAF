package com.ptaf.api.methods;

import com.ptaf.api.implementation.ApiActionImpl;
import com.ptaf.api.interfaces.ApiAction;
import org.junit.jupiter.api.Assertions; // Using JUnit 5 for assertions
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * ApiCommonMethods provides a high-level API for interacting with web services during tests.
 * This class translates simple, readable method calls into API actions, which are
 * then used in the step definition files.
 *
 * <p>
 * Purpose for testers:
 * - This class is a thin facade around an ApiAction implementation. It provides
 *   commonly used request building helpers, a method to send requests, and several
 *   verification helpers to assert on the last API response.
 * - All verification methods use JUnit 5 assertions and will fail the test upon mismatch.
 * - Logging is performed for visibility into what the test is doing; check logs when tests fail
 *   for helpful context.
 * </p>
 */
public class ApiCommonMethods {

    private static final Logger logger = LoggerFactory.getLogger(ApiCommonMethods.class);
    private final ApiAction apiAction;

    /**
     * Default constructor.
     *
     * <p>
     * Initializes the underlying ApiAction implementation used to perform HTTP operations
     * and hold the last response. Tests rely on this object to build requests, send them,
     * and query response details.
     * </p>
     */
    public ApiCommonMethods() {
        this.apiAction = new ApiActionImpl();
    }

    // --- Request Building Methods ---

    /**
     * Adds or updates a header in the outgoing request.
     *
     * @param key   Header name (e.g., "Content-Type", "Authorization").
     * @param value Header value. If authorization tokens or other dynamic values are used,
     *              they should be provided here.
     */
    public void setHeader(String key, String value) {
        // Log the header being added for visibility in test logs.
        logger.info("Setting header: {} = {}", key, value);
        // Delegate to the concrete ApiAction implementation which stores the header for the next request.
        apiAction.setHeader(key, value);
    }

    /**
     * Sets a path parameter to be substituted in the endpoint URL before sending the request.
     *
     * @param key   The name of the path parameter placeholder (e.g., "userId" for /users/{userId}).
     * @param value The value to substitute for the placeholder. Test should ensure the value
     *              is properly encoded if it contains special characters (this implementation
     *              will simply pass the value through to the ApiAction).
     */
    public void setPathParameter(String key, String value) {
        logger.info("Setting path parameter: {} = {}", key, value);
        // The ApiAction implementation is responsible for applying path parameters to the URL.
        apiAction.setPathParameter(key, value);
    }

    /**
     * Sets a query parameter for the outgoing request.
     *
     * @param key   Query parameter name (e.g., "limit", "sort").
     * @param value Query parameter value. This method accepts Object so callers can pass
     *              numbers, booleans, or other types; the ApiAction implementation will
     *              handle serialization to a string form.
     */
    public void setQueryParameter(String key, Object value) {
        logger.info("Setting query parameter: {} = {}", key, value);
        // Delegates to ApiAction which should handle converting the value to the appropriate string.
        apiAction.setQueryParameter(key, value);
    }

    /**
     * Assigns the request body that will be sent with the request.
     *
     * @param body The body object. It can be a plain string, a map, or a POJO depending on how
     *             the ApiAction implementation serializes the request (usually to JSON).
     */
    public void setRequestBody(Object body) {
        logger.info("Setting request body.");
        // Provide a debug-level dump of the body for troubleshooting without cluttering info logs.
        logger.debug("Request body content: {}", body);
        apiAction.setRequestBody(body);
    }

    // --- Request Sending Method ---

    /**
     * Sends a previously constructed API request.
     *
     * @param serviceName Logical name of the service or base URL configuration to use.
     *                    This typically maps to an entry in a configuration or properties file.
     * @param requestKey  Logical key identifying the request definition (e.g., endpoint path,
     *                    HTTP method) within the ApiAction implementation or configuration.
     *
     * Notes for testers:
     * - Ensure setHeader / setPathParameter / setQueryParameter / setRequestBody have been called
     *   as needed before calling sendRequest.
     * - The ApiAction implementation will store the last response which verification methods read.
     */
    public void sendRequest(String serviceName, String requestKey) {
        logger.info("Sending request for key '{}' to service '{}'", requestKey, serviceName);
        // The ApiAction implementation performs the actual HTTP call using the accumulated request data.
        apiAction.sendRequest(serviceName, requestKey);
    }

    // --- Response Verification Methods ---

    /**
     * Verifies that the status code of the last API response matches the expected value.
     *
     * @param expectedStatusCode The expected HTTP status code (e.g., 200, 201, 404).
     *
     * Notes:
     * - Fails the test via JUnit assertion if the codes do not match.
     * - The actual status code is obtained from the underlying ApiAction.getResponseStatusCode().
     */
    public void verifyResponseStatusCode(int expectedStatusCode) {
        int actualStatusCode = apiAction.getResponseStatusCode();
        logger.info("Verifying response status code. Expected: {}, Actual: {}", expectedStatusCode, actualStatusCode);
        // Use JUnit 5 assertion to fail the test in case of mismatch.
        Assertions.assertEquals(expectedStatusCode, actualStatusCode, "Response status code mismatch.");
    }

    /**
     * Verifies that the response body from the last API call contains a specific piece of text.
     *
     * @param expectedText The text to search for in the response body.
     *
     * Notes for testers:
     * - This performs a simple substring check. For structured JSON validation prefer getValueByJsonPath
     *   or more advanced JSON assertions.
     * - If the response body is large or binary, consider converting or extracting the relevant part
     *   before asserting.
     */
    public void verifyResponseBodyContains(String expectedText) {
        String responseBody = apiAction.getResponseBody();
        logger.info("Verifying response body contains text: '{}'", expectedText);
        // A simple contains check; the assertion message includes the expected text for easier debugging.
        Assertions.assertTrue(responseBody.contains(expectedText),
                "Response body did not contain the expected text. Expected: " + expectedText);
    }

    /**
     * Verifies the value of a specific header from the last API response.
     *
     * @param headerName     The name of the header to check (case-insensitive).
     * @param expectedValue  The expected value of the header.
     *
     * Notes:
     * - Header lookup is done case-insensitively to accommodate servers that vary header capitalization.
     * - If the header is not present an assertion failure will occur with a clear message.
     */
    public void verifyResponseHeader(String headerName, String expectedValue) {
        Map<String, String> headers = apiAction.getLastResponse().getHeaders();
        // Header names are often case-insensitive, so we check for the key's presence carefully
        String actualValue = headers.entrySet().stream()
                .filter(entry -> headerName.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        logger.info("Verifying header '{}'. Expected: '{}', Actual: '{}'", headerName, expectedValue, actualValue);
        // Ensure header exists
        Assertions.assertNotNull(actualValue, "Header '" + headerName + "' not found in response.");
        // Ensure header value matches expected
        Assertions.assertEquals(expectedValue, actualValue, "Response header value mismatch.");
    }

    /**
     * Verifies that a value extracted from the JSON response body via JSONPath matches an expected value.
     *
     * @param jsonPath       The JSONPath expression to find the value (e.g., "$.data.email").
     * @param expectedValue  The expected value (as a String, which will be compared).
     *
     * Notes for testers:
     * - The actual value retrieved from the response is converted to a String using Objects.toString(..., null).
     *   If the JSONPath points to a null node or does not exist, actualValue will be null and the assertion
     *   will likely fail (or succeed if expectedValue is also null).
     * - For non-String comparisons consider fetching the value via getValueByJsonPath and performing type-aware checks.
     */
    public void verifyJsonPathValue(String jsonPath, String expectedValue) {
        Object actualValueObj = getValueByJsonPath(jsonPath);
        // Convert actual value to a String representation for a deterministic comparison.
        String actualValue = Objects.toString(actualValueObj, null); // Convert actual value to string for comparison

        logger.info("Verifying JSONPath '{}'. Expected: '{}', Actual: '{}'", jsonPath, expectedValue, actualValue);
        // Use JUnit assertion to compare expected and actual string representations.
        Assertions.assertEquals(expectedValue, actualValue, "Value from JSONPath did not match expected value.");
    }

    // --- Response Data Getter Method ---

    /**
     * Retrieves a value from the last response using a JSONPath expression.
     *
     * @param jsonPath The JSONPath expression.
     * @return The extracted value as an Object. Return type depends on the JSON value:
     *         it may be a String, Number, Boolean, Map/List, or null if the path is missing.
     *
     * Notes:
     * - This method defers to the ApiAction implementation to evaluate JSONPath against the last response body.
     * - Callers should be prepared to handle null results or to cast the returned Object to the expected type.
     */
    public Object getValueByJsonPath(String jsonPath) {
        logger.info("Getting value from response using JSONPath: {}", jsonPath);
        // Delegate to the ApiAction which performs JSONPath evaluation and returns the matched object.
        return apiAction.getValueFromResponse(jsonPath);
    }
}
