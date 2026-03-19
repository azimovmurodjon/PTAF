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

    private static final String NOT_AVAILABLE = "N/A";

    private final String requestName;
    private final String method;
    private final String protocol;
    private final String host;
    private final int port;
    private final String path;
    private final String requestBody;
    private final String contentType;
    private final String acceptType;

    /**
     * Optional resolved/custom headers.
     */
    private final Map<String, String> headers;

    /**
     * Optional bearer token alias stored in engine token store.
     */
    private final String bearerTokenAlias;

    /**
     * Optional basic auth username.
     */
    private final String basicAuthUsername;

    /**
     * Optional basic auth password.
     */
    private final String basicAuthPassword;

    /**
     * Human-readable payload source type.
     * Examples: Inline JSON, YAML, CSV, Excel, No Payload
     */
    private final String payloadSourceType;

    /**
     * Human-readable payload source details.
     * Examples:
     * YAML key: performance.payloads.createCustomer
     * CSV file: data.csv, row: row1, column: requestBody
     */
    private final String payloadSourceDetails;

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
        this.requestName = normalizeText(requestName);
        this.method = normalizeMethod(method);
        this.protocol = normalizeProtocol(protocol);
        this.host = normalizeText(host);
        this.port = sanitizePort(port);
        this.path = normalizePath(path);
        this.requestBody = normalizeMultilineText(requestBody);
        this.contentType = normalizeText(contentType);
        this.acceptType = normalizeText(acceptType);
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.bearerTokenAlias = normalizeText(bearerTokenAlias);
        this.basicAuthUsername = normalizeText(basicAuthUsername);
        this.basicAuthPassword = basicAuthPassword == null ? "" : basicAuthPassword;
        this.payloadSourceType = normalizeText(payloadSourceType);
        this.payloadSourceDetails = normalizeText(payloadSourceDetails);
    }

    public String getRequestName() {
        return requestName;
    }

    public String getMethod() {
        return method;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getContentType() {
        return contentType;
    }

    public String getAcceptType() {
        return acceptType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBearerTokenAlias() {
        return bearerTokenAlias;
    }

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public String getPayloadSourceType() {
        return payloadSourceType;
    }

    public String getPayloadSourceDetails() {
        return payloadSourceDetails;
    }

    // ============================================================
    // REPORTING / HELPER METHODS
    // ============================================================

    public boolean hasBearerTokenAuth() {
        return !bearerTokenAlias.isBlank();
    }

    public boolean hasBasicAuth() {
        return !basicAuthUsername.isBlank();
    }

    public boolean hasHeaders() {
        return !headers.isEmpty();
    }

    public boolean hasRequestBody() {
        return !requestBody.isBlank();
    }

    public String getResolvedAuthType() {
        if (hasBearerTokenAuth()) {
            return "Bearer Token";
        }
        if (hasBasicAuth()) {
            return "Basic Authentication";
        }
        return "No Authentication";
    }

    public String getSafeRequestName() {
        return requestName.isBlank() ? NOT_AVAILABLE : requestName;
    }

    public String getSafeMethod() {
        return method.isBlank() ? NOT_AVAILABLE : method;
    }

    public String getSafeProtocol() {
        return protocol.isBlank() ? NOT_AVAILABLE : protocol;
    }

    public String getSafeHost() {
        return host.isBlank() ? NOT_AVAILABLE : host;
    }

    public String getSafePath() {
        return path.isBlank() ? "/" : path;
    }

    public String getSafeContentType() {
        return contentType.isBlank() ? NOT_AVAILABLE : contentType;
    }

    public String getSafeAcceptType() {
        return acceptType.isBlank() ? NOT_AVAILABLE : acceptType;
    }

    public String getSafePayloadSourceType() {
        return payloadSourceType.isBlank() ? "No Payload" : payloadSourceType;
    }

    public String getSafePayloadSourceDetails() {
        return payloadSourceDetails.isBlank() ? NOT_AVAILABLE : payloadSourceDetails;
    }

    public String buildDisplayUrl() {
        if (protocol.isBlank() || host.isBlank() || port <= 0) {
            return NOT_AVAILABLE;
        }
        return protocol + "://" + host + ":" + port + getSafePath();
    }

    // ============================================================
    // INTERNAL NORMALIZATION
    // ============================================================

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        String cleaned = value.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        return cleaned
                .replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s{2,}", " ");
    }

    private String normalizeMultilineText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String normalizeMethod(String value) {
        String cleaned = normalizeText(value);
        return cleaned.isEmpty() ? "" : cleaned.toUpperCase();
    }

    private String normalizeProtocol(String value) {
        String cleaned = normalizeText(value);
        return cleaned.isEmpty() ? "" : cleaned.toLowerCase();
    }

    private String normalizePath(String value) {
        String cleaned = normalizeText(value);

        if (cleaned.isEmpty()) {
            return "/";
        }

        return cleaned.startsWith("/") ? cleaned : "/" + cleaned;
    }

    private int sanitizePort(int value) {
        return Math.max(value, 0);
    }

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