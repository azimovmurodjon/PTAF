package com.ptaf.api.performer;

import com.google.gson.Gson;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import com.ptaf.api.wrapper.ApiResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Utility class responsible for performing HTTP actions against an API using a Playwright
 * APIRequestContext. This class centralizes the construction of requests (headers, query
 * parameters, path parameters, and body serialization) and converts Playwright's APIResponse
 * into the project's ApiResponseWrapper for consistent test assertions and logging.
 *
 * <p>Key responsibilities:
 * - Replace path parameter placeholders in the endpoint (e.g. "/users/{userId}").
 * - Attach headers and query parameters to the request in a Playwright RequestOptions object.
 * - Serialize request bodies to JSON using Gson when present and set the Content-Type header
 *   if not provided by the caller.
 * - Execute the HTTP method on the provided APIRequestContext and time the request.
 * - Convert the APIResponse to ApiResponseWrapper containing status, body text, and headers.
 *
 * <p>Notes for testers:
 * - The method will throw IllegalArgumentException for unsupported HTTP verbs.
 * - Query parameter values are always converted to String via String.valueOf(...). Nulls will
 *   become the string "null" unless filtered by the caller.
 * - If a body is provided and no "Content-Type" header was set by the caller, this class will
 *   automatically set "Content-Type: application/json".
 * - Path parameter replacement is literal string replacement of placeholders of the form
 *   {paramName} and does not encode values; callers should ensure values are URL-safe if needed.
 *
 * <p>Dependencies:
 * - Requires Playwright (for APIRequestContext, APIResponse, RequestOptions).
 * - Requires Gson (for JSON serialization).
 */
public class ApiActionPerformer {

    // SLF4J logger for informational and debug output of request/response lifecycle.
    private static final Logger logger = LoggerFactory.getLogger(ApiActionPerformer.class);

    // Gson instance used to serialize request body objects into JSON strings.
    // Kept as an instance field to avoid re-creating the Gson object on every request.
    private final Gson gson = new Gson(); // For serializing request bodies to JSON

    /**
     * Default constructor.
     *
     * <p>Note: Gson library must be present on the classpath for body serialization to function.
     */
    public ApiActionPerformer() {
        // NOTE: For this to work, you will need the Google Gson library dependency.
    }

    /**
     * Builds and sends an HTTP request using the provided APIRequestContext and returns a wrapped
     * response.
     *
     * <p>Behavior details:
     * - Path parameters in the endpoint (e.g. "/users/{id}") are replaced by values from pathParams.
     * - Headers from the headers map are applied to the request. If a body is provided and the
     *   caller did not include a Content-Type header, this method sets "Content-Type: application/json".
     * - Query parameters are added to the request; each value is converted to a String via
     *   String.valueOf(...).
     * - The body object (for POST/PUT/PATCH etc.) is serialized to JSON using Gson and sent as the
     *   request data payload.
     * - Supported methods: GET, POST, PUT, DELETE, PATCH. Any other method will result in
     *   IllegalArgumentException.
     *
     * @param context     Playwright APIRequestContext that performs the low-level HTTP call using
     *                    configured base URL, authentication/interceptors, etc.
     * @param method      HTTP method name (case-insensitive). Supported: GET, POST, PUT, DELETE, PATCH.
     * @param endpoint    Endpoint path. May include placeholders of form {paramName} which will
     *                    be replaced by corresponding entries in pathParams.
     * @param headers     Map of request headers to set. May be null or empty.
     * @param queryParams Map of query parameter key -> value. Values are converted to String.
     *                    May be null or empty.
     * @param pathParams  Map of path parameter name -> replacement value used to replace placeholders
     *                    in the endpoint. May be null or empty.
     * @param body        Request body object. If non-null, it will be serialized to JSON and sent
     *                    as the request payload. May be null for methods without bodies (e.g. GET).
     * @return ApiResponseWrapper containing numeric HTTP status, response body text, and response headers.
     * @throws IllegalArgumentException if an unsupported HTTP method is provided.
     */
    public ApiResponseWrapper sendRequest(APIRequestContext context, String method, String endpoint,
                                          Map<String, String> headers, Map<String, Object> queryParams,
                                          Map<String, String> pathParams, Object body) {

        // Replace any placeholders in the endpoint with provided path parameter values.
        String processedEndpoint = replacePathParameters(endpoint, pathParams);
        logger.info("Sending {} request to endpoint: {}", method.toUpperCase(), processedEndpoint);

        // Create Playwright RequestOptions to accumulate headers, query params and body.
        RequestOptions options = RequestOptions.create();

        // Copy provided headers into the request options (if any).
        if (headers != null && !headers.isEmpty()) {
            // RequestOptions.setHeader handles setting a header name/value pair.
            headers.forEach(options::setHeader);
        }

        // Process and attach query parameters.
        // Playwright expects each query param value to be represented as a String.
        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                // Use String.valueOf to safely handle nulls and different object types.
                options.setQueryParam(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }

        // If a body is provided, serialize it to JSON and attach as request data.
        if (body != null) {
            // Convert the body object into a JSON string representation.
            String jsonBody = gson.toJson(body);
            options.setData(jsonBody);

            // Ensure Content-Type header is set to application/json unless caller already set it.
            if (headers == null || !headers.containsKey("Content-Type")) {
                options.setHeader("Content-Type", "application/json");
            }

            // Log the serialized body at debug level to avoid cluttering normal logs.
            logger.debug("Request Body: {}", jsonBody);
        }

        APIResponse response;
        // Record start time to measure request duration for logging and performance troubleshooting.
        long startTime = System.currentTimeMillis();

        // Dispatch the request using the provided HTTP method. The Playwright APIRequestContext
        // provides strongly-typed methods for each common HTTP verb.
        switch (method.toUpperCase()) {
            case "GET":
                response = context.get(processedEndpoint, options);
                break;
            case "POST":
                response = context.post(processedEndpoint, options);
                break;
            case "PUT":
                response = context.put(processedEndpoint, options);
                break;
            case "DELETE":
                response = context.delete(processedEndpoint, options);
                break;
            case "PATCH":
                response = context.patch(processedEndpoint, options);
                break;
            default:
                // Explicitly fail fast for unsupported HTTP methods to make test failures obvious.
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        // Compute how long the remote call took.
        long duration = System.currentTimeMillis() - startTime;

        // Read the response body as plain text. This will load the response payload into memory.
        // Testers should be mindful of extremely large responses.
        String responseBody = response.text();

        // Log summary information and debug-level full body text.
        logger.info("Received response with Status: {} in {}ms", response.status(), duration);
        logger.debug("Response Body: {}", responseBody);

        // Wrap Playwright's APIResponse into the project's ApiResponseWrapper and return it.
        // ApiResponseWrapper typically contains status code, body text and headers map for easier assertions.
        return new ApiResponseWrapper(response.status(), responseBody, response.headers());
    }

    /**
     * Replace placeholders in the endpoint string with values from the provided pathParams map.
     *
     * <p>Placeholders are expected to be of the form {key}. Replacement is a simple literal
     * string replacement, not URL-encoding. If a parameter is absent in pathParams, the placeholder
     * will remain in the returned endpoint string.
     *
     * @param endpoint   The endpoint template potentially containing placeholders like {id}.
     * @param pathParams Map of placeholder names to replacement values.
     * @return The endpoint with placeholders replaced when matching entries exist in pathParams.
     */
    private String replacePathParameters(String endpoint, Map<String, String> pathParams) {
        if (pathParams == null || pathParams.isEmpty()) {
            // No path parameters to replace; return endpoint unchanged.
            return endpoint;
        }
        String processedEndpoint = endpoint;
        for (Map.Entry<String, String> param : pathParams.entrySet()) {
            // Construct the expected placeholder syntax and perform a literal replacement.
            String placeholder = "{" + param.getKey() + "}";
            processedEndpoint = processedEndpoint.replace(placeholder, param.getValue());
        }
        return processedEndpoint;
    }
}
