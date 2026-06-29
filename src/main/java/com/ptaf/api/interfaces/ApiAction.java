package com.ptaf.api.interfaces;

import com.ptaf.api.wrapper.ApiResponseWrapper;

import java.util.Map;

/**
 * ApiAction defines the contract for performing high-level, reusable API operations.
 * This interface abstracts away the complexities of building and sending HTTP requests,
 * allowing tests to be written in a clean, readable, and stateful manner.
 *
 * <p>Typical workflow for tests using an implementation of this interface:
 * <ol>
 *     <li>Call one or more of the "set*" methods to prepare the next request (headers, path/query params, body).</li>
 *     <li>Invoke sendRequest(serviceName, requestKey) to execute the request defined in your request configuration (e.g. api_requests.yml).</li>
 *     <li>Use the provided getters and convenience methods to inspect or assert the response (status code, body, extracted values).</li>
 * </ol>
 *
 * <p>Notes for implementers and testers:
 * <ul>
 *     <li>Implementations may be stateful: the "set*" methods typically prepare data for the next single request.
 *         Callers should be aware whether the implementation reuses state across calls or resets after sendRequest.</li>
 *     <li>Thread-safety is implementation dependent. If your test suite runs requests in parallel,
 *         ensure the concrete implementation is safe for concurrent use or scope instances per-thread.</li>
 * </ul>
 *
 * @see com.ptaf.api.wrapper.ApiResponseWrapper
 * @since 1.0
 */
public interface ApiAction {

    // --- Methods for building the next request ---
    // The following methods are used to configure the next HTTP request that will be sent
    // by calling sendRequest(...). Implementations typically store these values internally
    // until sendRequest is invoked.

    /**
     * Adds a custom header to the next request.
     *
     * <p>Headers added by this method are expected to be included in the HTTP request
     * when sendRequest(...) is called. Calling this multiple times with the same key
     * may either overwrite the previous value or add another header depending on the
     * implementation — tests should rely on documented behavior of the concrete class.
     *
     * @param key   The header name (e.g., "x-request-id" or "Content-Type").
     * @param value The header value (e.g., "application/json").
     */
    void setHeader(String key, String value);

    /**
     * Sets a path parameter to be replaced in the endpoint URL.
     *
     * <p>For example, if the request template defined in api_requests.yml is
     * "/users/{userId}/posts/{postId}", calling setPathParameter("userId", "123")
     * and setPathParameter("postId", "456") should result in "/users/123/posts/456"
     * when the request is executed.
     *
     * @param key   The name of the placeholder in the endpoint string (without curly braces).
     * @param value The value to substitute for the placeholder.
     */
    void setPathParameter(String key, String value);

    /**
     * Adds a query parameter to the next request's URL.
     *
     * <p>Query parameters are appended to the request URL when sendRequest(...) is executed.
     * Implementations should handle proper encoding of parameter names and values. The
     * value type is {@code Object} to allow numbers, booleans, lists, or string types;
     * concrete implementations are responsible for converting these to a URL-friendly form.
     *
     * @param key   The query parameter name (e.g., "page", "limit", "search").
     * @param value The query parameter value. If null, implementations may omit the parameter.
     */
    void setQueryParameter(String key, Object value);

    /**
     * Sets the request body for the next POST, PUT, or PATCH request.
     *
     * <p>The provided object will typically be serialized to JSON by the implementation
     * (e.g., using Jackson or Gson). Acceptable types include:
     * <ul>
     *     <li>POJOs that can be serialized to JSON</li>
     *     <li>{@code Map<String, Object>} representing JSON objects</li>
     *     <li>Raw JSON strings</li>
     * </ul>
     *
     * <p>Callers should confirm whether the implementation overwrites a previously set body
     * or merges with it when invoked multiple times.
     *
     * @param body A POJO, Map, or String representing the request body.
     */
    void setRequestBody(Object body);

    // --- Method for executing the request ---
    // Use sendRequest to execute the prepared request against the service/request key defined
    // in your YAML (or other) configuration. The returned ApiResponseWrapper contains the
    // response payload, headers, status code, and other metadata useful for assertions.

    /**
     * Sends the configured API request.
     *
     * <p>The method looks up the request definition identified by {@code requestKey}
     * under the given {@code serviceName} (typically defined in api_requests.yml and config.yml)
     * and executes it using the headers, path parameters, query parameters and body previously set.
     *
     * <p>Implementations are expected to populate and return an {@link ApiResponseWrapper}
     * that contains at least the HTTP status code, response body as a string, and any
     * relevant headers or parsed data useful to the caller.
     *
     * @param serviceName The key for the API service in config.yml (e.g., "jsonplaceholder").
     * @param requestKey  The key for the request definition in api_requests.yml
     *                    (e.g., "jsonplaceholder_requests.get_all_posts").
     * @return An ApiResponseWrapper containing the response details. Implementations may
     * return null or throw an exception if execution fails — callers should handle such cases.
     */
    ApiResponseWrapper sendRequest(String serviceName, String requestKey);

    // --- Methods for retrieving data from the last response ---
    // After a request is executed, these methods allow tests to access the response
    // information. The "last response" semantics refer to the most recently completed
    // call to sendRequest(...) on this instance.

    /**
     * Retrieves the wrapper object for the most recent API response.
     *
     * <p>The returned wrapper typically contains raw response text, parsed JSON (if applicable),
     * headers, and status code. If no request has been executed yet, implementations may return
     * {@code null} or an empty wrapper object — callers should check for {@code null}.
     *
     * @return The last ApiResponseWrapper, or null if no request has been made.
     */
    ApiResponseWrapper getLastResponse();

    /**
     * A convenience method to get the status code from the last response.
     *
     * <p>This is a shorthand for {@code getLastResponse().getStatusCode()} (semantics vary by implementation).
     * If there is no last response, implementations may return 0 or another sentinel value — callers should
     * verify behavior for their concrete implementation.
     *
     * @return The HTTP status code of the last response.
     */
    int getResponseStatusCode();

    /**
     * A convenience method to get the body as a String from the last response.
     *
     * <p>This provides a quick way to retrieve the response payload for assertions or logging.
     * If no response exists, implementations may return {@code null} or an empty string.
     *
     * @return The response body as a String, or null if none is available.
     */
    String getResponseBody();

    /**
     * Extracts a single value from the last JSON response body using a JSONPath expression.
     *
     * <p>JSONPath expressions allow tests to target nested fields without manual parsing,
     * for example:
     * <ul>
     *     <li>"$.data.id" — extracts the 'id' field inside 'data' object</li>
     *     <li>"$[0].name" — extracts the 'name' field of the first element in a JSON array</li>
     * </ul>
     *
     * <p>Behavior notes:
     * <ul>
     *     <li>If the response is not valid JSON, implementations may throw an exception or return null.</li>
     *     <li>If the path does not match anything, implementations may return null or an empty result.</li>
     *     <li>Return type is {@link Object} to allow strings, numbers, booleans, lists, or maps to be returned
     *         depending on what the JSONPath selects.</li>
     * </ul>
     *
     * @param jsonPath The JSONPath expression (e.g., "$.data.id", "$[0].name").
     * @return The extracted value as an Object, or null if not found or not applicable.
     */
    Object getValueFromResponse(String jsonPath);

}
