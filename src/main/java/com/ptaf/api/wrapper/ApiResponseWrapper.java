package com.ptaf.api.wrapper;

import java.util.Map;

/**
 * A wrapper class to store the results of an API call in a standardized format.
 * This decouples the framework from Playwright's specific APIResponse object and provides
 * easy access to the most important parts of the response.
 *
 * <p>
 * Designed to be a simple, immutable container:
 * - All fields are final to prevent accidental modification after construction.
 * - The headers map is stored by reference (no defensive copy) to keep the class lightweight.
 *   Callers should not modify the supplied map after creating an instance, or should provide
 *   an unmodifiable copy if mutation safety is required.
 * </p>
 *
 * <p>
 * Typical usage for a tester:
 * ApiResponseWrapper result = new ApiResponseWrapper(200, "{\"id\":1}", headersMap);
 * assertEquals(200, result.getStatusCode());
 * assertTrue(result.getBody().contains("\"id\":1"));
 * </p>
 */
public class ApiResponseWrapper {

    /**
     * The HTTP status code returned by the API call (for example: 200, 201, 400, 404, 500).
     * Marked final to indicate it is immutable after construction.
     */
    private final int statusCode;

    /**
     * The raw response body as a String. This may contain JSON, XML, HTML, plain text, or
     * be an empty string depending on the response. Callers are responsible for parsing
     * (e.g., converting to a JSON object) if structured access to the data is required.
     *
     * Note: This value may be null if the response had no body or if a null was explicitly passed.
     */
    private final String body;

    /**
     * A Map containing the response headers. Keys and values are strings representing header
     * names and header values respectively (e.g., "Content-Type" -> "application/json").
     *
     * Important: This class does not create a defensive copy of the map. The map reference
     * passed into the constructor is stored directly. To avoid accidental modification,
     * callers should pass an immutable map (e.g., Collections.unmodifiableMap) or a copy.
     */
    private final Map<String, String> headers;

    /**
     * Construct a new ApiResponseWrapper.
     *
     * @param statusCode the HTTP status code returned by the API
     * @param body       the response body as a String (may be null or empty)
     * @param headers    a Map of response headers (may be null). The map reference is stored
     *                   directly; this class does not perform a defensive copy.
     */
    public ApiResponseWrapper(int statusCode, String body, Map<String, String> headers) {
        // Assign the immutable fields. Using final fields ensures values cannot be changed later.
        this.statusCode = statusCode;

        // Store the raw response body. Do not attempt to parse here; leave parsing to consumers.
        this.body = body;

        // Store the headers map reference. Note: no defensive copy to keep this object lightweight.
        this.headers = headers;
    }

    /**
     * Get the HTTP status code for this response.
     *
     * @return the HTTP status code (e.g., 200, 404, 500)
     */
    public int getStatusCode() {
        // Simple accessor that returns the immutable stored status code.
        return statusCode;
    }

    /**
     * Get the response body as a String.
     *
     * @return the response body (may be null or empty). For structured content (e.g., JSON),
     * callers should parse the string using their preferred library.
     */
    public String getBody() {
        // Return the raw body string; do not perform any transformation here.
        return body;
    }

    /**
     * Get the response headers map.
     *
     * @return the headers map as provided to the constructor (may be null). The returned
     * map is the same instance passed in; modifying it will affect this wrapper's view.
     */
    public Map<String, String> getHeaders() {
        // Return the stored headers reference directly. Callers must be cautious of mutation.
        return headers;
    }

    /**
     * Provide a concise, human-readable representation of the wrapper primarily useful
     * for logging and debugging. This intentionally includes only the status code and body
     * to avoid very large header dumps in logs. If header information is required for
     * diagnostics, callers should explicitly call getHeaders().
     *
     * @return a short string describing the response wrapper
     */
    @Override
    public String toString() {
        // Build a compact representation containing status and body for debugging output.
        return "ApiResponseWrapper{" +
                "statusCode=" + statusCode +
                ", body='" + body + '\'' +
                '}';
    }
}
