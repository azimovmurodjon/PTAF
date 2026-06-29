package com.ptaf.performance.headers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Framework-owned header manager for performance execution.
 *
 * <p>This class centralizes HTTP header creation and merge behavior so that
 * testers never manually build low-level header maps in feature logic.</p>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Store default framework headers</li>
 *   <li>Store request-specific headers</li>
 *   <li>Apply bearer token authentication</li>
 *   <li>Apply basic authentication</li>
 *   <li>Return final merged header map</li>
 * </ul>
 * </p>
 *
 * <p>Merge priority:
 * request-specific headers override default headers.</p>
 *
 * <p>Thread-safety:
 * This implementation is not synchronized. If this manager is accessed from
 * multiple threads concurrently, external synchronization is required.</p>
 */
public class PerformanceHeaderManager {

    /**
     * Default headers that are owned by the framework and intended to be applied
     * to most requests. These headers represent global defaults and are only
     * overridden by request-level headers when the same header key appears
     * in both maps.
     *
     * LinkedHashMap is used to preserve insertion order which can be useful
     * for deterministic output during testing and debugging.
     */
    private final Map<String, String> defaultHeaders = new LinkedHashMap<>();

    /**
     * Headers specific to an individual request. These are merged on top of
     * defaultHeaders when build() is called; requestHeaders take precedence.
     *
     * LinkedHashMap is used to preserve insertion order so that the final
     * merged map has a predictable iteration order.
     */
    private final Map<String, String> requestHeaders = new LinkedHashMap<>();

    /**
     * Adds or replaces a default framework-level header.
     *
     * <p>If the key already exists in the default headers map, the value will
     * be replaced. Keys and values are validated - the key must be non-null
     * and non-blank, and the value must be non-null.</p>
     *
     * @param key header name
     * @param value header value
     * @return current manager for fluent usage
     * @throws IllegalArgumentException if key is null/blank or value is null
     */
    public PerformanceHeaderManager addDefaultHeader(String key, String value) {
        // Validate inputs to prevent invalid header entries.
        validateHeader(key, value);
        defaultHeaders.put(key, value);
        return this;
    }

    /**
     * Adds multiple default framework-level headers.
     *
     * <p>This method is null-safe and ignores empty maps. Individual header
     * entries are validated via addDefaultHeader(...) which will throw an
     * exception for invalid key/value pairs.</p>
     *
     * @param headers headers to add; may be null or empty
     * @return current manager for fluent usage
     */
    public PerformanceHeaderManager addDefaultHeaders(Map<String, String> headers) {
        // Fast-path: nothing to do for null or empty input.
        if (headers == null || headers.isEmpty()) {
            return this;
        }

        // Delegate to single-header addition so validation and behavior is consistent.
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            addDefaultHeader(entry.getKey(), entry.getValue());
        }

        return this;
    }

    /**
     * Adds or replaces a request-level header.
     *
     * <p>Request-level headers will override defaults with the same header
     * name when build() is called. Validation ensures keys are non-blank and
     * values are non-null.</p>
     *
     * @param key header name
     * @param value header value
     * @return current manager for fluent usage
     * @throws IllegalArgumentException if key is null/blank or value is null
     */
    public PerformanceHeaderManager addRequestHeader(String key, String value) {
        // Validate inputs to enforce consistent header rules.
        validateHeader(key, value);
        requestHeaders.put(key, value);
        return this;
    }

    /**
     * Adds multiple request-level headers.
     *
     * <p>This method is null-safe and ignores empty maps. Each header entry
     * is validated via addRequestHeader(...).</p>
     *
     * @param headers headers to add; may be null or empty
     * @return current manager for fluent usage
     */
    public PerformanceHeaderManager addRequestHeaders(Map<String, String> headers) {
        // Fast-path: nothing to do for null or empty input.
        if (headers == null || headers.isEmpty()) {
            return this;
        }

        // Delegate to single-header addition for consistent validation.
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            addRequestHeader(entry.getKey(), entry.getValue());
        }

        return this;
    }

    /**
     * Adds Authorization header using Bearer token strategy.
     *
     * <p>This places the Authorization header into requestHeaders with the
     * value "Bearer &lt;token&gt;". The provided token is trimmed and must not
     * be null or blank. This call will overwrite any existing Authorization
     * header at the request level.</p>
     *
     * @param token bearer token; must be non-null and non-blank
     * @return current manager for fluent usage
     * @throws IllegalArgumentException if token is null or blank
     */
    public PerformanceHeaderManager addBearerToken(String token) {
        // Ensure token is present and meaningful.
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Bearer token cannot be null or blank.");
        }

        // Store as a request-level header so it wins over default Authorization if present.
        requestHeaders.put("Authorization", "Bearer " + token.trim());
        return this;
    }

    /**
     * Adds Authorization header using Basic authentication strategy.
     *
     * <p>Constructs the header by concatenating username and password with a
     * colon, encoding the result with Base64 (UTF-8), and prefixing with
     * "Basic ". Username must be non-null and non-blank; password must be
     * non-null (blank password is allowed and encoded as empty string).</p>
     *
     * @param username username; must be non-null and non-blank
     * @param password password; must be non-null (may be blank)
     * @return current manager for fluent usage
     * @throws IllegalArgumentException if username is null/blank or password is null
     */
    public PerformanceHeaderManager addBasicAuth(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Basic auth username cannot be null or blank.");
        }

        if (password == null) {
            // Allow empty password but not null; null indicates a programming error.
            throw new IllegalArgumentException("Basic auth password cannot be null.");
        }

        // Prepare "username:password" and Base64 encode using UTF-8 to meet HTTP Basic spec.
        String rawCredentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));

        // Store as a request-level header to ensure it overrides any default Authorization header.
        requestHeaders.put("Authorization", "Basic " + encodedCredentials);
        return this;
    }

    /**
     * Adds Content-Type header.
     *
     * <p>The content type value is trimmed and validated; blank or null is
     * rejected. This is stored as a request-level header and will override
     * any default Content-Type header when merged.</p>
     *
     * @param contentType content type value; must be non-null and non-blank
     * @return current manager for fluent usage
     * @throws IllegalArgumentException if contentType is null or blank
     */
    public PerformanceHeaderManager addContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content-Type cannot be null or blank.");
        }

        requestHeaders.put("Content-Type", contentType.trim());
        return this;
    }

    /**
     * Adds Accept header.
     *
     * <p>The accept value is trimmed and validated; blank or null is rejected.
     * Stored as a request-level header and overrides any default Accept header
     * when merged.</p>
     *
     * @param accept accept value; must be non-null and non-blank
     * @return current manager for fluent usage
     * @throws IllegalArgumentException if accept is null or blank
     */
    public PerformanceHeaderManager addAccept(String accept) {
        if (accept == null || accept.isBlank()) {
            throw new IllegalArgumentException("Accept header cannot be null or blank.");
        }

        requestHeaders.put("Accept", accept.trim());
        return this;
    }

    /**
     * Returns final merged headers.
     *
     * <p>Merge behavior:
     * <ol>
     *   <li>All default headers are copied first.</li>
     *   <li>All request-level headers are copied next, overwriting any keys
     *       that were present in the defaults.</li>
     * </ol>
     * The resulting map is returned as an unmodifiable map to prevent callers
     * from changing the internal state of this manager.</p>
     *
     * @return immutable merged header map; insertion order is preserved
     */
    public Map<String, String> build() {
        Map<String, String> mergedHeaders = new LinkedHashMap<>();
        // Copy defaults first so request headers can override them when copied.
        mergedHeaders.putAll(defaultHeaders);
        // Copy request headers on top of defaults to implement override semantics.
        mergedHeaders.putAll(requestHeaders);
        // Return an immutable view to avoid accidental external mutation.
        return Collections.unmodifiableMap(mergedHeaders);
    }

    /**
     * Clears request-specific headers only.
     *
     * <p>Default framework headers remain unchanged. Useful between request
     * executions when you want to reset per-request modifications but keep
     * the global defaults.</p>
     *
     * @return current manager for fluent usage
     */
    public PerformanceHeaderManager clearRequestHeaders() {
        requestHeaders.clear();
        return this;
    }

    /**
     * Clears all headers (both default and request-level).
     *
     * <p>After calling this method the manager will be empty and behave as if
     * it was newly constructed.</p>
     *
     * @return current manager for fluent usage
     */
    public PerformanceHeaderManager clearAll() {
        defaultHeaders.clear();
        requestHeaders.clear();
        return this;
    }

    /**
     * Validates header name and value.
     *
     * <p>This helper enforces that header keys are non-null and non-blank and
     * that header values are non-null. It throws IllegalArgumentException with
     * helpful messages to make failures clear in tests.</p>
     *
     * @param key header name
     * @param value header value
     * @throws IllegalArgumentException if key is null/blank or value is null
     */
    private void validateHeader(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Header name cannot be null or blank.");
        }

        if (value == null) {
            throw new IllegalArgumentException("Header value cannot be null for header: " + key);
        }
    }
}
