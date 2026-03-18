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
 */
public class PerformanceHeaderManager {

    private final Map<String, String> defaultHeaders = new LinkedHashMap<>();
    private final Map<String, String> requestHeaders = new LinkedHashMap<>();

    /**
     * Adds or replaces a default framework-level header.
     *
     * @param key header name
     * @param value header value
     * @return current manager
     */
    public PerformanceHeaderManager addDefaultHeader(String key, String value) {
        validateHeader(key, value);
        defaultHeaders.put(key, value);
        return this;
    }

    /**
     * Adds multiple default framework-level headers.
     *
     * @param headers headers to add
     * @return current manager
     */
    public PerformanceHeaderManager addDefaultHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return this;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            addDefaultHeader(entry.getKey(), entry.getValue());
        }

        return this;
    }

    /**
     * Adds or replaces a request-level header.
     *
     * @param key header name
     * @param value header value
     * @return current manager
     */
    public PerformanceHeaderManager addRequestHeader(String key, String value) {
        validateHeader(key, value);
        requestHeaders.put(key, value);
        return this;
    }

    /**
     * Adds multiple request-level headers.
     *
     * @param headers headers to add
     * @return current manager
     */
    public PerformanceHeaderManager addRequestHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return this;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            addRequestHeader(entry.getKey(), entry.getValue());
        }

        return this;
    }

    /**
     * Adds Authorization header using Bearer token strategy.
     *
     * @param token bearer token
     * @return current manager
     */
    public PerformanceHeaderManager addBearerToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Bearer token cannot be null or blank.");
        }

        requestHeaders.put("Authorization", "Bearer " + token.trim());
        return this;
    }

    /**
     * Adds Authorization header using Basic authentication strategy.
     *
     * @param username username
     * @param password password
     * @return current manager
     */
    public PerformanceHeaderManager addBasicAuth(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Basic auth username cannot be null or blank.");
        }

        if (password == null) {
            throw new IllegalArgumentException("Basic auth password cannot be null.");
        }

        String rawCredentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(rawCredentials.getBytes(StandardCharsets.UTF_8));

        requestHeaders.put("Authorization", "Basic " + encodedCredentials);
        return this;
    }

    /**
     * Adds Content-Type header.
     *
     * @param contentType content type value
     * @return current manager
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
     * @param accept accept value
     * @return current manager
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
     * <p>Request-level headers override default headers when keys match.</p>
     *
     * @return immutable merged header map
     */
    public Map<String, String> build() {
        Map<String, String> mergedHeaders = new LinkedHashMap<>();
        mergedHeaders.putAll(defaultHeaders);
        mergedHeaders.putAll(requestHeaders);
        return Collections.unmodifiableMap(mergedHeaders);
    }

    /**
     * Clears request-specific headers only.
     *
     * <p>Default framework headers remain unchanged.</p>
     *
     * @return current manager
     */
    public PerformanceHeaderManager clearRequestHeaders() {
        requestHeaders.clear();
        return this;
    }

    /**
     * Clears all headers.
     *
     * @return current manager
     */
    public PerformanceHeaderManager clearAll() {
        defaultHeaders.clear();
        requestHeaders.clear();
        return this;
    }

    /**
     * Validates header name and value.
     *
     * @param key header name
     * @param value header value
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