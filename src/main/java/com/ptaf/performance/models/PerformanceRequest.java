package com.ptaf.performance.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Framework-owned immutable request model for performance execution.
 *
 * <p>This object is constructed only through builder layers and consumed
 * by architect-controlled engine/test-plan classes.</p>
 *
 * <p>Reporting-safe goals:
 * <ul>
 *   <li>keep constructor compatibility</li>
 *   <li>normalize request fields for cleaner report output</li>
 *   <li>preserve immutability</li>
 *   <li>add helper methods only</li>
 * </ul>
 * </p>
 */
public class PerformanceRequest {

    /**
     * Default placeholder used when a value is not available for display.
     */
    private static final String NOT_AVAILABLE = "N/A";

    /**
     * Human-friendly name for the request. Normalized using {@link #normalizeText(String)}.
     * Never null; may be an empty string when not provided.
     */
    private final String requestName;

    /**
     * HTTP method (GET, POST, etc.). Normalized to uppercase via {@link #normalizeMethod(String)}.
     * Never null; may be an empty string when not provided.
     */
    private final String method;

    /**
     * Network protocol (http, https). Normalized to lowercase via {@link #normalizeProtocol(String)}.
     * Never null; may be an empty string when not provided.
     */
    private final String protocol;

    /**
     * Target host (hostname or IP). Normalized via {@link #normalizeText(String)}.
     * Never null; may be an empty string when not provided.
     */
    private final String host;

    /**
     * Target port number. Sanitized via {@link #sanitizePort(int)} to be non-negative.
     */
    private final int port;

    /**
     * Request path (resource URI). Normalized via {@link #normalizePath(String)} and guaranteed to
     * start with '/' (or be "/"). Never null.
     */
    private final String path;

    /**
     * Raw request payload/body. Normalized with {@link #normalizeMultilineText(String)} to preserve
     * multiline content but trim leading/trailing whitespace. Never null; may be empty.
     */
    private final String requestBody;

    /**
     * Content-Type header value. Normalized via {@link #normalizeText(String)}.
     * Never null; may be empty.
     */
    private final String contentType;

    /**
     * Accept header value. Normalized via {@link #normalizeText(String)}.
     * Never null; may be empty.
     */
    private final String acceptType;

    /**
     * Optional resolved/custom headers.
     *
     * <p>Immutable map instance (unmodifiable). Never null; empty map when no headers provided.
     * LinkedHashMap semantics are preserved during defensive copy to retain insertion order for
     * predictable reporting.</p>
     */
    private final Map<String, String> headers;

    /**
     * Optional bearer token alias stored in engine token store. Normalized via {@link #normalizeText(String)}.
     * Never null; may be empty.
     */
    private final String bearerTokenAlias;

    /**
     * Optional basic auth username. Normalized via {@link #normalizeText(String)}.
     * Never null; may be empty.
     */
    private final String basicAuthUsername;

    /**
     * Optional basic auth password. Note: not normalized (kept as-is except for null -> empty).
     * Stored as raw string for engine consumption; redacted in {@link #toString()}.
     */
    private final String basicAuthPassword;

    /**
     * Human-readable payload source type.
     * Examples: Inline JSON, YAML, CSV, Excel, No Payload
     * Normalized via {@link #normalizeText(String)}.
     */
    private final String payloadSourceType;

    /**
     * Human-readable payload source details.
     * Examples:
     * YAML key: performance.payloads.createCustomer
     * CSV file: data.csv, row: row1, column: requestBody
     * Normalized via {@link #normalizeText(String)}.
     */
    private final String payloadSourceDetails;

    /**
     * Primary constructor. All inputs are normalized inside the constructor to ensure the instance is
     * safe for reporting and consumption by engine/test-plan code.
     *
     * Important normalization / safety details:
     * - String fields are normalized (trimmed, whitespace collapsed, newlines handled) via helper methods.
     * - HTTP method is upper-cased and protocol lower-cased to make comparisons consistent.
     * - Path is guaranteed to start with '/' and defaults to "/" when empty.
     * - Port is sanitized to be non-negative (0 indicates unspecified).
     * - Headers are defensively copied into an unmodifiable map; a null headers reference becomes an empty map.
     * - Basic auth password: null is converted to empty string; it is intentionally not logged by toString().
     *
     * Note: Constructor preserves immutability by using final fields and creating unmodifiable collections.
     *
     * @param requestName human-friendly request name (may be null/empty)
     * @param method HTTP method (GET, POST, etc.) (may be null/empty)
     * @param protocol network protocol (http/https) (may be null/empty)
     * @param host target host (may be null/empty)
     * @param port target port (may be negative; sanitized to 0)
     * @param path request path/URI (may be null/empty)
     * @param requestBody raw request payload (may be null/empty)
     * @param contentType content-type of the request (may be null/empty)
     * @param acceptType accept header value (may be null/empty)
     * @param headers optional headers map (may be null); a defensive, unmodifiable copy is created
     * @param bearerTokenAlias optional alias for a bearer token in engine token store (may be null/empty)
     * @param basicAuthUsername optional basic auth username (may be null/empty)
     * @param basicAuthPassword optional basic auth password (may be null); null -> empty string
     * @param payloadSourceType human-friendly payload source type (may be null/empty)
     * @param payloadSourceDetails human-friendly payload source details (may be null/empty)
     */
    public PerformanceRequest(String requestName,
                              String method,
                              String protocol,
                              String host,
                              int port,
                              String path,
                              String requestBody,
                              String contentType,
                              String acceptType,
                              Map<String, String> headers,
                              String bearerTokenAlias,
                              String basicAuthUsername,
                              String basicAuthPassword,
                              String payloadSourceType,
                              String payloadSourceDetails) {
        // Normalize and assign simple text fields to maintain reporting consistency.
        this.requestName = normalizeText(requestName);
        this.method = normalizeMethod(method);
        this.protocol = normalizeProtocol(protocol);
        this.host = normalizeText(host);

        // Ensure port is non-negative (0 indicates unspecified).
        this.port = sanitizePort(port);

        // Ensure path always starts with a leading slash and is never empty.
        this.path = normalizePath(path);

        // Preserve multiline payloads but trim leading/trailing whitespace.
        this.requestBody = normalizeMultilineText(requestBody);

        this.contentType = normalizeText(contentType);
        this.acceptType = normalizeText(acceptType);

        // Defensive copy of headers: preserve insertion order and make unmodifiable for immutability guarantees.
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));

        this.bearerTokenAlias = normalizeText(bearerTokenAlias);
        this.basicAuthUsername = normalizeText(basicAuthUsername);

        // Do not normalize password beyond null -> empty. Engines use the raw value.
        this.basicAuthPassword = basicAuthPassword == null ? "" : basicAuthPassword;

        this.payloadSourceType = normalizeText(payloadSourceType);
        this.payloadSourceDetails = normalizeText(payloadSourceDetails);
    }

    /**
     * @return normalized request name (may be empty but never null)
     */
    public String getRequestName() {
        return requestName;
    }

    /**
     * @return normalized HTTP method (uppercase; may be empty but never null)
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return normalized protocol (lowercase; may be empty but never null)
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return normalized host (may be empty but never null)
     */
    public String getHost() {
        return host;
    }

    /**
     * @return sanitized port number (0 indicates unspecified)
     */
    public int getPort() {
        return port;
    }

    /**
     * @return normalized request path (always non-null and starts with '/')
     */
    public String getPath() {
        return path;
    }

    /**
     * @return request body/payload (trimmed; may contain internal newlines; never null)
     */
    public String getRequestBody() {
        return requestBody;
    }

    /**
     * @return content-type value (may be empty but never null)
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return accept-type value (may be empty but never null)
     */
    public String getAcceptType() {
        return acceptType;
    }

    /**
     * @return an unmodifiable map of headers; never null. Use {@link #hasHeaders()} to check emptiness.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @return normalized bearer token alias (may be empty but never null)
     */
    public String getBearerTokenAlias() {
        return bearerTokenAlias;
    }

    /**
     * @return normalized basic auth username (may be empty but never null)
     */
    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    /**
     * @return basic auth password (raw, may be empty but never null). For safety the password is redacted in toString().
     */
    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    /**
     * @return human-friendly payload source type (may be empty but never null)
     */
    public String getPayloadSourceType() {
        return payloadSourceType;
    }

    /**
     * @return human-friendly payload source details (may be empty but never null)
     */
    public String getPayloadSourceDetails() {
        return payloadSourceDetails;
    }

    // ============================================================
    // REPORTING / HELPER METHODS
    // ============================================================

    /**
     * Convenience check whether bearer-token style authentication is configured.
     *
     * @return true when a bearer token alias is present (non-blank)
     */
    public boolean hasBearerTokenAuth() {
        return !bearerTokenAlias.isBlank();
    }

    /**
     * Convenience check whether basic authentication is configured.
     *
     * @return true when a basic auth username is present (non-blank)
     */
    public boolean hasBasicAuth() {
        return !basicAuthUsername.isBlank();
    }

    /**
     * @return true when the request contains one or more headers
     */
    public boolean hasHeaders() {
        return !headers.isEmpty();
    }

    /**
     * @return true when the request body is non-blank (useful for determining payload presence)
     */
    public boolean hasRequestBody() {
        return !requestBody.isBlank();
    }

    /**
     * Resolve a human-readable authentication type for reporting.
     *
     * @return "Bearer Token", "Basic Authentication", or "No Authentication"
     */
    public String getResolvedAuthType() {
        if (hasBearerTokenAuth()) {
            return "Bearer Token";
        }
        if (hasBasicAuth()) {
            return "Basic Authentication";
        }
        return "No Authentication";
    }

    /**
     * Return a display-safe request name for reports. Converts blank values to a standard N/A placeholder.
     *
     * @return requestName or {@value NOT_AVAILABLE} when blank
     */
    public String getSafeRequestName() {
        return requestName.isBlank() ? NOT_AVAILABLE : requestName;
    }

    /**
     * Return a display-safe HTTP method for reports.
     *
     * @return method or {@value NOT_AVAILABLE} when blank
     */
    public String getSafeMethod() {
        return method.isBlank() ? NOT_AVAILABLE : method;
    }

    /**
     * Return a display-safe protocol for reports.
     *
     * @return protocol or {@value NOT_AVAILABLE} when blank
     */
    public String getSafeProtocol() {
        return protocol.isBlank() ? NOT_AVAILABLE : protocol;
    }

    /**
     * Return a display-safe host for reports.
     *
     * @return host or {@value NOT_AVAILABLE} when blank
     */
    public String getSafeHost() {
        return host.isBlank() ? NOT_AVAILABLE : host;
    }

    /**
     * Return a display-safe path for reports. Guarantee at least "/" when blank.
     *
     * @return path or "/" when blank
     */
    public String getSafePath() {
        return path.isBlank() ? "/" : path;
    }

    /**
     * Return a display-safe content type for reports.
     *
     * @return contentType or {@value NOT_AVAILABLE} when blank
     */
    public String getSafeContentType() {
        return contentType.isBlank() ? NOT_AVAILABLE : contentType;
    }

    /**
     * Return a display-safe accept type for reports.
     *
     * @return acceptType or {@value NOT_AVAILABLE} when blank
     */
    public String getSafeAcceptType() {
        return acceptType.isBlank() ? NOT_AVAILABLE : acceptType;
    }

    /**
     * Return a display-safe payload source type. Defaults to "No Payload" when blank.
     *
     * @return payloadSourceType or "No Payload" when blank
     */
    public String getSafePayloadSourceType() {
        return payloadSourceType.isBlank() ? "No Payload" : payloadSourceType;
    }

    /**
     * Return a display-safe payload source details for reports.
     *
     * @return payloadSourceDetails or {@value NOT_AVAILABLE} when blank
     */
    public String getSafePayloadSourceDetails() {
        return payloadSourceDetails.isBlank() ? NOT_AVAILABLE : payloadSourceDetails;
    }

    /**
     * Build a simple display URL composed of protocol, host, port and path for reporting purposes.
     *
     * Notes:
     * - Returns a placeholder ({@value NOT_AVAILABLE}) if protocol or host are blank or port is unspecified (<= 0).
     * - Uses {@link #getSafePath()} to ensure a valid path component.
     *
     * @return a printable URL string or {@value NOT_AVAILABLE} when essential components are missing
     */
    public String buildDisplayUrl() {
        if (protocol.isBlank() || host.isBlank() || port <= 0) {
            return NOT_AVAILABLE;
        }
        return protocol + "://" + host + ":" + port + getSafePath();
    }

    // ============================================================
    // INTERNAL NORMALIZATION
    // ============================================================

    /**
     * Normalize a single-line text value for reporting/storage:
     * - Null -> empty string
     * - Trim leading/trailing whitespace
     * - Replace CR/LF/newline characters with spaces
     * - Collapse multiple internal whitespace characters into a single space
     *
     * This produces compact, single-line values suitable for report tables and keys.
     *
     * @param value raw input (may be null)
     * @return normalized single-line string (never null)
     */
    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        // Trim leading/trailing whitespace first.
        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        // Replace various newline sequences with spaces and collapse repeated whitespace.
        return cleaned
                .replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s{2,}", " ");
    }

    /**
     * Normalize multiline text while preserving internal newlines:
     * - Null -> empty string
     * - Trim leading/trailing whitespace only
     *
     * This is intentionally lighter-weight than {@link #normalizeText(String)} because request bodies
     * may rely on formatting/line breaks.
     *
     * @param value raw input (may be null)
     * @return trimmed multiline string (never null)
     */
    private String normalizeMultilineText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    /**
     * Normalize HTTP method: apply {@link #normalizeText(String)} then upper-case.
     *
     * @param value raw method (may be null)
     * @return upper-cased method or empty string when not provided
     */
    private String normalizeMethod(String value) {
        String cleaned = normalizeText(value);
        return cleaned.isEmpty() ? "" : cleaned.toUpperCase();
    }

    /**
     * Normalize protocol: apply {@link #normalizeText(String)} then lower-case.
     *
     * @param value raw protocol (may be null)
     * @return lower-cased protocol or empty string when not provided
     */
    private String normalizeProtocol(String value) {
        String cleaned = normalizeText(value);
        return cleaned.isEmpty() ? "" : cleaned.toLowerCase();
    }

    /**
     * Normalize path ensuring there is a leading slash:
     * - Null/empty -> "/"
     * - If provided, ensure it starts with "/" so consumers can safely concatenate it with host/port.
     *
     * @param value raw path (may be null)
     * @return normalized path starting with '/' (never null)
     */
    private String normalizePath(String value) {
        String cleaned = normalizeText(value);

        if (cleaned.isEmpty()) {
            return "/";
        }

        return cleaned.startsWith("/") ? cleaned : "/" + cleaned;
    }

    /**
     * Ensure port is non-negative. Negative values are sanitized to 0 (unspecified).
     *
     * @param value raw port value
     * @return sanitized port (>= 0)
     */
    private int sanitizePort(int value) {
        return Math.max(value, 0);
    }

    /**
     * Debug-friendly string representation. Note that basicAuthPassword is redacted for safety.
     *
     * @return a string representation of this PerformanceRequest
     */
    @Override
    public String toString() {
        return "PerformanceRequest{" +
                "requestName='" + requestName + '\'' +
                ", method='" + method + '\'' +
                ", protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", path='" + path + '\'' +
                ", requestBody='" + requestBody + '\'' +
                ", contentType='" + contentType + '\'' +
                ", acceptType='" + acceptType + '\'' +
                ", headers=" + headers +
                ", bearerTokenAlias='" + bearerTokenAlias + '\'' +
                ", basicAuthUsername='" + basicAuthUsername + '\'' +
                ", basicAuthPassword='***'" +
                ", payloadSourceType='" + payloadSourceType + '\'' +
                ", payloadSourceDetails='" + payloadSourceDetails + '\'' +
                '}';
    }
}
