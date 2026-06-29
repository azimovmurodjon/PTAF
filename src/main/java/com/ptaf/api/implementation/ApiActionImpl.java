package com.ptaf.api.implementation;

import com.jayway.jsonpath.JsonPath;
import com.ptaf.api.handlers.ApiRequestHandler;
import com.ptaf.api.interfaces.ApiAction;
import com.ptaf.api.performer.ApiActionPerformer;
import com.ptaf.api.wrapper.ApiResponseWrapper;
import com.ptaf.utils.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements the ApiAction interface to provide concrete methods for building,
 * sending, and verifying API requests.
 *
 * <p>
 * This class is intended to be used in a test automation flow where steps prepare
 * parts of an HTTP request (headers, path/query parameters, body) and then send the
 * request. The implementation uses ThreadLocal storage for request state and the
 * last received response so that parallel test execution (multi-threaded scenarios)
 * does not share state between threads.
 * </p>
 *
 * <p>
 * The typical usage pattern:
 * <ol>
 *     <li>setHeader / setPathParameter / setQueryParameter / setRequestBody to build the request</li>
 *     <li>sendRequest(serviceName, requestKey) to read the request definition and execute it</li>
 *     <li>getLastResponse / getResponseStatusCode / getResponseBody / getValueFromResponse to validate the response</li>
 * </ol>
 * </p>
 *
 * Notes:
 * - Request definitions (method, endpoint) are read from a YAML file via YamlReader using the requestKey.
 * - After every sendRequest call, the request state (headers, params, body) is cleared for the current thread
 *   to avoid leaking state into subsequent requests.
 */
public class ApiActionImpl implements ApiAction {

    // Logger for diagnostic messages and errors
    private static final Logger logger = LoggerFactory.getLogger(ApiActionImpl.class);

    // Responsible for executing requests (encapsulates HTTP client logic)
    private final ApiActionPerformer apiPerformer;

    // ThreadLocal variables to hold the state for the NEXT request to be sent.
    // Using ThreadLocal ensures that concurrent tests running in different threads
    // do not interfere with each other's request data.

    /**
     * Request headers to be included in the next request for the current thread.
     * Keys and values are both strings.
     */
    private final ThreadLocal<Map<String, String>> headers = ThreadLocal.withInitial(HashMap::new);

    /**
     * Path parameters for the next request for the current thread.
     * Values are string representations, inserted into endpoint path templates.
     */
    private final ThreadLocal<Map<String, String>> pathParams = ThreadLocal.withInitial(HashMap::new);

    /**
     * Query parameters for the next request for the current thread.
     * Values are typed as Object to allow numbers, booleans, lists, etc.
     */
    private final ThreadLocal<Map<String, Object>> queryParams = ThreadLocal.withInitial(HashMap::new);

    /**
     * Request body for the next request for the current thread.
     * Stored as an Object to allow JSON maps, strings, or custom POJOs depending
     * on the ApiActionPerformer expectations.
     */
    private final ThreadLocal<Object> requestBody = new ThreadLocal<>();

    /**
     * Holds the last response received for the current thread. This allows subsequent
     * validation steps to access status code, body and parsed values from the response.
     */
    private final ThreadLocal<ApiResponseWrapper> lastResponse = new ThreadLocal<>();

    /**
     * Default constructor which instantiates the underlying performer responsible
     * for executing HTTP requests.
     *
     * Note:
     * For getValueFromResponse to work you must include the Jayway JsonPath library
     * in your project dependencies (see comments below). This method uses JsonPath
     * to evaluate JSONPath expressions against the response body.
     */
    public ApiActionImpl() {
        this.apiPerformer = new ApiActionPerformer();
        // NOTE: For getValueFromResponse to work, you will need the Jayway JsonPath library.
        // In your pom.xml, add:
        // <dependency>
        //     <groupId>com.jayway.jsonpath</groupId>
        //     <artifactId>json-path</artifactId>
        //     <version>2.9.0</version>
        // </dependency>
    }

    /**
     * Adds or replaces a header for the next request constructed in the current thread.
     *
     * @param key   header name (e.g., "Content-Type", "Authorization")
     * @param value header value
     */
    @Override
    public void setHeader(String key, String value) {
        // simply put into the ThreadLocal map for headers
        this.headers.get().put(key, value);
    }

    /**
     * Sets a path parameter to be applied when the endpoint contains placeholders.
     * Example: endpoint "/users/{userId}" with setPathParameter("userId", "123")
     * will substitute {userId} with "123" when the request is sent.
     *
     * @param key   the name of the path parameter
     * @param value the value to substitute into the endpoint
     */
    @Override
    public void setPathParameter(String key, String value) {
        this.pathParams.get().put(key, value);
    }

    /**
     * Adds or replaces a query parameter for the next request on the current thread.
     * The value is typed as Object to allow numeric and boolean values as well as strings.
     *
     * @param key   parameter name (e.g., "limit", "sort")
     * @param value parameter value; may be null, a primitive wrapper, collection, etc.
     */
    @Override
    public void setQueryParameter(String key, Object value) {
        this.queryParams.get().put(key, value);
    }

    /**
     * Sets the request body to be used for the next request in the current thread.
     * The performer will decide how to serialize this object (e.g., to JSON) before sending.
     *
     * @param body the request payload; may be a Map, POJO, String or any supported type by the performer
     */
    @Override
    public void setRequestBody(Object body) {
        this.requestBody.set(body);
    }

    /**
     * Sends an HTTP request based on a YAML definition key and the state previously
     * configured via setHeader, setPathParameter, setQueryParameter, and setRequestBody.
     *
     * The YAML is expected to contain at least a 'method' and an 'endpoint' for the given requestKey.
     * Example YAML structure:
     * <pre>
     * someRequest:
     *   method: GET
     *   endpoint: /users/{userId}
     * </pre>
     *
     * After sending the request, the response is stored in ThreadLocal lastResponse
     * so that subsequent validation steps can introspect it. The request state is
     * cleared for the current thread to ensure no leakage between requests.
     *
     * @param serviceName a logical service identifier used by ApiRequestHandler to provide context
     * @param requestKey  a key used to look up request definition (method, endpoint) from YAML
     * @return the ApiResponseWrapper containing status, headers and body
     * @throws IllegalArgumentException if the YAML request definition is missing or incomplete
     */
    @Override
    public ApiResponseWrapper sendRequest(String serviceName, String requestKey) {
        // Read request definition from YAML via the provided requestKey
        String method = (String) YamlReader.get(requestKey + ".method");
        String endpoint = (String) YamlReader.get(requestKey + ".endpoint");

        // Validate that the required parts of the request definition are present
        if (method == null || endpoint == null) {
            throw new IllegalArgumentException("Request definition for key '" + requestKey + "' not found or is incomplete in api_requests.yml.");
        }

        // Delegate actual HTTP sending to the performer, providing all pieces of state
        ApiResponseWrapper response = apiPerformer.sendRequest(
                ApiRequestHandler.getContext(serviceName),
                method,
                endpoint,
                headers.get(),
                queryParams.get(),
                pathParams.get(),
                requestBody.get()
        );

        // Save the response for later verification steps within the same thread
        this.lastResponse.set(response);

        // IMPORTANT: Clear the request-specific state so it doesn't leak into the next API call.
        // This keeps each request isolated and forces explicit setting of headers/params/body per request.
        clearRequestState();

        return response;
    }

    /**
     * Returns the last ApiResponseWrapper stored for the current thread.
     * Test steps should call this after a sendRequest; otherwise an IllegalStateException will be thrown.
     *
     * @return the last ApiResponseWrapper for the current thread
     * @throws IllegalStateException if no request has been sent yet in this thread
     */
    @Override
    public ApiResponseWrapper getLastResponse() {
        ApiResponseWrapper response = this.lastResponse.get();
        if (response == null) {
            throw new IllegalStateException("No API request has been sent yet in this scenario. Cannot get a response.");
        }
        return response;
    }

    /**
     * Convenience method to obtain the HTTP status code of the last response.
     *
     * @return HTTP status code
     * @throws IllegalStateException if no request has been sent yet in this thread
     */
    @Override
    public int getResponseStatusCode() {
        return getLastResponse().getStatusCode();
    }

    /**
     * Convenience method to obtain the raw response body (as a String) of the last response.
     *
     * @return response body string (may be null or empty)
     * @throws IllegalStateException if no request has been sent yet in this thread
     */
    @Override
    public String getResponseBody() {
        return getLastResponse().getBody();
    }

    /**
     * Extracts a value from the last response body using a JSONPath expression.
     *
     * <p>
     * This method uses the Jayway JsonPath library to evaluate the supplied expression
     * against the response body. Example usage:
     * getValueFromResponse("$.data[0].id");
     * </p>
     *
     * @param jsonPath a JSONPath expression to evaluate against the response body
     * @return the value matched by the JSONPath; the return type depends on the JSON content and expression
     * @throws RuntimeException       if the JSONPath expression is invalid or the body cannot be parsed
     * @throws IllegalStateException  if no request has been sent yet in this thread
     */
    @Override
    public Object getValueFromResponse(String jsonPath) {
        String body = getResponseBody();
        if (body == null || body.isEmpty()) {
            // Warn testers that the body is empty and therefore no extraction can be performed.
            logger.warn("Cannot get value from JSONPath because response body is empty.");
            return null;
        }
        try {
            // JsonPath.read will throw an exception for invalid expressions or unparsable JSON.
            return JsonPath.read(body, jsonPath);
        } catch (Exception e) {
            // Log the detailed cause to aid debugging in tests, then rethrow a runtime exception
            // so test frameworks can mark the step as failed.
            logger.error("Failed to read JSONPath '{}' from response body.", jsonPath, e);
            throw new RuntimeException("Invalid JSONPath expression or body format.", e);
        }
    }

    /**
     * Clears all request-specific state (headers, params, body) for the current thread.
     * This method is intentionally private and invoked after each successful call to sendRequest.
     *
     * Clearing strategy:
     * - For maps (headers, pathParams, queryParams) we clear the existing map instance.
     * - For requestBody we remove the ThreadLocal entry.
     *
     * The method logs at debug level to help troubleshoot state-related issues during test execution.
     */
    private void clearRequestState() {
        this.headers.get().clear();
        this.pathParams.get().clear();
        this.queryParams.get().clear();
        this.requestBody.remove();
        logger.debug("Request state cleared for the current thread.");
    }
}
