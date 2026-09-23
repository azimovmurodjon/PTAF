package com.ptaf.performance.builders;

import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.models.PerformanceRequest;
import com.ptaf.performance.payloads.PerformancePayloadResolver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Architect-controlled builder for performance requests.
 *
 * <p>This builder hides request construction complexity from testers and step definitions.
 * It provides a fluent API to assemble all parts of a performance request (method, URL,
 * headers, payload, authentication, etc.), resolves payloads from multiple sources
 * (inline JSON, YAML keys, CSV, Excel), and validates required fields before creating
 * a {@link PerformanceRequest} instance.</p>
 *
 * <p>Usage example:
 * PerformanceRequest request = new PerformanceRequestBuilder()
 *      .withRequestName("CreateUser")
 *      .withMethod("POST")
 *      .withPath("/users")
 *      .withJsonBody("{\"name\":\"Alice\"}")
 *      .withHeader("X-Correlation-Id", "abc-123")
 *      .build();
 * </p>
 *
 * <p>Defaults:
 * - protocol and host and port default to values provided by {@link PerformanceConfigurationProperties}
 * - contentType defaults to "application/json"
 * - acceptType defaults to "application/json"</p>
 *
 * <p>Note: This builder is not thread-safe and is intended to be used as a single-use object
 * to construct a single {@link PerformanceRequest}.</p>
 */
public class PerformanceRequestBuilder {

    /**
     * Optional descriptive name for the request used in reporting/logging.
     */
    private String requestName;

    /**
     * HTTP method (GET, POST, PUT, DELETE, etc.). Required.
     */
    private String method;

    /**
     * Protocol to use (http/https). Defaults are read from configuration properties.
     */
    private String protocol;

    /**
     * Hostname or IP address for the request. Defaults are read from configuration.
     */
    private String host;

    /**
     * Port to connect to. Defaults are read from configuration.
     */
    private int port;

    /**
     * Path part of the URL (e.g. "/api/v1/resource"). Required.
     */
    private String path;

    /**
     * Resolved request body (typically JSON). This may be set directly (inline),
     * or populated by resolving YAML/CSV/Excel payload sources.
     */
    private String requestBody;

    /**
     * Content-Type header value. Defaults to "application/json".
     */
    private String contentType;

    /**
     * Accept header value. Defaults to "application/json".
     */
    private String acceptType;

    /**
     * Additional request headers. LinkedHashMap preserves insertion order,
     * which can be useful for deterministic logs or tests.
     */
    private final Map<String, String> headers = new LinkedHashMap<>();

    /**
     * Alias used to resolve a bearer token from some token store. Mutually exclusive with basic auth.
     */
    private String bearerTokenAlias;

    /**
     * Basic auth username (if using basic authentication).
     */
    private String basicAuthUsername;

    /**
     * Basic auth password (if using basic authentication).
     */
    private String basicAuthPassword;

    // Payload-source support

    /**
     * If set, the builder will attempt to resolve a request body from a YAML payload registry
     * using this key when an inline body is not provided.
     */
    private String yamlBodyKey;

    /**
     * File path to a CSV file that can provide payload values.
     */
    private String csvFilePath;

    /**
     * Row identifier (for CSV) to locate the desired record within the CSV file.
     */
    private String csvRowIdentifier;

    /**
     * Column name (for CSV) that contains the payload content.
     */
    private String csvColumnName;

    /**
     * File path to an Excel file that can provide payload values.
     */
    private String excelFilePath;

    /**
     * Row identifier (for Excel) to locate the desired record within the Excel file.
     */
    private String excelRowIdentifier;

    /**
     * Column name (for Excel) that contains the payload content.
     */
    private String excelColumnName;

    /**
     * Create a new builder instance initializing defaults.
     *
     * <p>Defaults are pulled from {@link PerformanceConfigurationProperties} for protocol,
     * host and port. Content-type and accept-type default to "application/json".</p>
     */
    public PerformanceRequestBuilder() {
        this.protocol = PerformanceConfigurationProperties.getProtocol();
        this.host = PerformanceConfigurationProperties.getHost();
        this.port = PerformanceConfigurationProperties.getPort();
        this.contentType = "application/json";
        this.acceptType = "application/json";
    }

    /**
     * Set a user-friendly request name for reporting.
     *
     * @param requestName descriptive name
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withRequestName(String requestName) {
        this.requestName = requestName;
        return this;
    }

    /**
     * Set the HTTP method for the request.
     *
     * @param method HTTP verb (GET, POST, PUT, DELETE, etc.)
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withMethod(String method) {
        this.method = method;
        return this;
    }

    /**
     * Override the protocol (http/https).
     *
     * @param protocol protocol as a string
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * Override the host for the request.
     *
     * @param host hostname or IP
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Override the destination port.
     *
     * @param port port number; must be > 0 (validated during build)
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * Set the request path (resource path).
     *
     * @param path path portion of the URL
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Provide an inline JSON request body.
     *
     * @param requestBody full JSON string
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withJsonBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    /**
     * Provide an inline request body (generic).
     *
     * @param requestBody full body string (JSON or other)
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    /**
     * Set Content-Type header value for the request.
     *
     * @param contentType e.g. "application/json"
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Set Accept header value for the request.
     *
     * @param acceptType e.g. "application/json"
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withAcceptType(String acceptType) {
        this.acceptType = acceptType;
        return this;
    }

    /**
     * Add a single header. Headers with null or blank names are ignored.
     *
     * @param name  header name (must not be null/blank)
     * @param value header value (may be empty but not null)
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withHeader(String name, String value) {
        if (name != null && !name.isBlank() && value != null) {
            this.headers.put(name, value);
        }
        return this;
    }

    /**
     * Add multiple headers at once by merging the provided map into the builder's
     * header map. If the given map is null or empty, it is ignored.
     *
     * @param headers map of header name -> value
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withHeaders(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            this.headers.putAll(headers);
        }
        return this;
    }

    /**
     * Configure bearer token alias. The actual token resolution is performed elsewhere
     * using the alias when the request is executed.
     *
     * @param bearerTokenAlias alias/key used to look up a bearer token
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withBearerTokenAlias(String bearerTokenAlias) {
        this.bearerTokenAlias = bearerTokenAlias;
        return this;
    }

    /**
     * Configure basic authentication credentials.
     *
     * @param username basic auth username
     * @param password basic auth password
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withBasicAuth(String username, String password) {
        this.basicAuthUsername = username;
        this.basicAuthPassword = password;
        return this;
    }

    /**
     * Specify a YAML key whose value will be loaded and used as the request body if no
     * inline body is provided.
     *
     * @param yamlBodyKey key in the YAML payload registry
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withYamlBodyKey(String yamlBodyKey) {
        this.yamlBodyKey = yamlBodyKey;
        return this;
    }

    /**
     * Specify CSV-based payload source details. If no inline body is provided, the builder
     * will attempt to load the payload from the specified CSV file, row identifier and column.
     *
     * @param csvFilePath     path to the CSV file
     * @param csvRowIdentifier row id used to find the desired record
     * @param csvColumnName   column name that contains payload content
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withCsvBody(String csvFilePath,
                                                 String csvRowIdentifier,
                                                 String csvColumnName) {
        this.csvFilePath = csvFilePath;
        this.csvRowIdentifier = csvRowIdentifier;
        this.csvColumnName = csvColumnName;
        return this;
    }

    /**
     * Specify Excel-based payload source details. If no inline body is provided, the builder
     * will attempt to load the payload from the specified Excel file, row identifier and column.
     *
     * @param excelFilePath     path to the Excel file
     * @param excelRowIdentifier row id used to find the desired record
     * @param excelColumnName   column name that contains payload content
     * @return this builder for chaining
     */
    public PerformanceRequestBuilder withExcelBody(String excelFilePath,
                                                   String excelRowIdentifier,
                                                   String excelColumnName) {
        this.excelFilePath = excelFilePath;
        this.excelRowIdentifier = excelRowIdentifier;
        this.excelColumnName = excelColumnName;
        return this;
    }

    /**
     * Finalize the configuration, resolve payloads if needed, validate required fields,
     * and construct an immutable {@link PerformanceRequest} instance.
     *
     * <p>Order of payload resolution:
     * 1) Inline requestBody (explicit withJsonBody/withRequestBody)
     * 2) YAML (if yamlBodyKey provided)
     * 3) CSV (if csvFilePath provided)
     * 4) Excel (if excelFilePath provided)</p>
     *
     * @return constructed {@link PerformanceRequest}
     * @throws IllegalArgumentException if required fields are missing or incompatible auth is configured
     */
    public PerformanceRequest build() {
        // Attempt to populate requestBody from supported external payload sources only if an inline body was not provided.
        resolvePayloadIfNeeded();

        // Validate required fields and combinations before building the request object.
        validate();

        // Create and return the immutable PerformanceRequest. Also supply metadata about the payload source.
        return new PerformanceRequest(
                requestName,
                method,
                protocol,
                host,
                port,
                path,
                requestBody,
                contentType,
                acceptType,
                headers,
                bearerTokenAlias,
                basicAuthUsername,
                basicAuthPassword,
                resolvePayloadSourceType(),
                resolvePayloadSourceDetails()
        );
    }

    /**
     * Resolve requestBody from YAML/CSV/Excel if no inline request body was provided.
     *
     * Resolution priority:
     * - If requestBody is already set (non-blank), do nothing.
     * - Else if yamlBodyKey is set, resolve YAML payload.
     * - Else if csvFilePath is set, resolve CSV payload.
     * - Else if excelFilePath is set, resolve Excel payload.
     *
     * The actual resolution logic is delegated to {@link PerformancePayloadResolver}.
     */
    private void resolvePayloadIfNeeded() {
        // If an inline body has been provided, prefer that and do not attempt external resolution.
        if (requestBody != null && !requestBody.isBlank()) {
            return;
        }

        // Attempt to resolve a YAML payload if a YAML key is present.
        if (yamlBodyKey != null && !yamlBodyKey.isBlank()) {
            requestBody = PerformancePayloadResolver.resolveYaml(yamlBodyKey);
            return;
        }

        // Attempt to resolve from CSV: file path + row identifier + column name.
        if (csvFilePath != null && !csvFilePath.isBlank()) {
            requestBody = PerformancePayloadResolver.resolveCsv(
                    csvFilePath,
                    csvRowIdentifier,
                    csvColumnName
            );
            return;
        }

        // Attempt to resolve from Excel if provided.
        if (excelFilePath != null && !excelFilePath.isBlank()) {
            requestBody = PerformancePayloadResolver.resolveExcel(
                    excelFilePath,
                    excelRowIdentifier,
                    excelColumnName
            );
        }
    }

    /**
     * Determine a short, user-friendly label describing which payload source was used.
     *
     * Possibilities:
     * - "YAML"
     * - "CSV"
     * - "Excel"
     * - "Inline JSON"
     * - "No Payload" (for GET/DELETE)
     * - "Unknown" (none matched / ambiguous)
     *
     * @return short label of payload source
     */
    private String resolvePayloadSourceType() {
        // Normalize HTTP method for comparisons; treat null safely.
        String normalizedMethod = method == null ? "" : method.trim().toUpperCase();

        if (yamlBodyKey != null && !yamlBodyKey.isBlank()) {
            return "YAML";
        }

        if (csvFilePath != null && !csvFilePath.isBlank()) {
            return "CSV";
        }

        if (excelFilePath != null && !excelFilePath.isBlank()) {
            return "Excel";
        }

        if (requestBody != null && !requestBody.isBlank()) {
            // Inline body provided by the tester or resolved from a file.
            return "Inline JSON";
        }

        // Some HTTP methods typically do not require a body.
        if ("GET".equals(normalizedMethod) || "DELETE".equals(normalizedMethod)) {
            return "No Payload";
        }

        // Fallback when we cannot determine the payload source.
        return "Unknown";
    }

    /**
     * Provide human-readable details about the payload source. This may include:
     * - YAML key
     * - CSV/Excel file path, row and column (with N/A for missing row/column)
     * - A short preview of inline request body (truncated to 120 chars)
     * - A note for GET/DELETE indicating no body required
     *
     * @return descriptive payload details suitable for logs or reports
     */
    private String resolvePayloadSourceDetails() {
        String normalizedMethod = method == null ? "" : method.trim().toUpperCase();

        if (yamlBodyKey != null && !yamlBodyKey.isBlank()) {
            return "YAML key: " + yamlBodyKey;
        }

        if (csvFilePath != null && !csvFilePath.isBlank()) {
            // Use safe() to avoid exposing null/blank row or column values; keep output friendly.
            return "CSV file: " + csvFilePath
                    + ", row: " + safe(csvRowIdentifier)
                    + ", column: " + safe(csvColumnName);
        }

        if (excelFilePath != null && !excelFilePath.isBlank()) {
            return "Excel file: " + excelFilePath
                    + ", row: " + safe(excelRowIdentifier)
                    + ", column: " + safe(excelColumnName);
        }

        if (requestBody != null && !requestBody.isBlank()) {
            // Provide a preview of the request body for reporting. Trim and truncate long bodies.
            String trimmed = requestBody.trim();
            return trimmed.length() > 120 ? trimmed.substring(0, 120) + "..." : trimmed;
        }

        if ("GET".equals(normalizedMethod) || "DELETE".equals(normalizedMethod)) {
            return "Request type does not require a body payload.";
        }

        // If we still don't have details, indicate absence.
        return "Payload details not available.";
    }

    /**
     * Validate required fields and incompatible configurations.
     *
     * Checks performed:
     * - method, protocol, host, path must be non-null and non-blank
     * - port must be > 0
     * - bearer token and basic auth cannot be configured at the same time
     *
     * Throws IllegalArgumentException when validation fails to make failures explicit
     * and easy to diagnose in tests.
     */
    private void validate() {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Performance request method cannot be null or blank.");
        }

        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("Performance request protocol cannot be null or blank.");
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Performance request host cannot be null or blank.");
        }

        if (port <= 0) {
            throw new IllegalArgumentException("Performance request port must be greater than 0.");
        }

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Performance request path cannot be null or blank.");
        }

        // Ensure we do not configure both bearer token and basic auth simultaneously.
        if (bearerTokenAlias != null && !bearerTokenAlias.isBlank()
                && basicAuthUsername != null && !basicAuthUsername.isBlank()) {
            throw new IllegalArgumentException("Performance request cannot use bearer token auth and basic auth at the same time.");
        }
    }

    /**
     * Helper to safely present potentially null or blank values.
     *
     * @param value input string
     * @return original value if not blank; otherwise "N/A"
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}
