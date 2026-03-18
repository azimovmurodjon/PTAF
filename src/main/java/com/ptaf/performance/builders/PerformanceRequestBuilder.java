package com.ptaf.performance.builders;

import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.models.PerformanceRequest;
import com.ptaf.performance.payloads.PerformancePayloadResolver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Architect-controlled builder for performance requests.
 *
 * <p>This builder hides request construction complexity from testers and step definitions.</p>
 */
public class PerformanceRequestBuilder {

    private String requestName;
    private String method;
    private String protocol;
    private String host;
    private int port;
    private String path;
    private String requestBody;
    private String contentType;
    private String acceptType;

    private final Map<String, String> headers = new LinkedHashMap<>();

    private String bearerTokenAlias;
    private String basicAuthUsername;
    private String basicAuthPassword;

    // Payload-source support
    private String yamlBodyKey;
    private String csvFilePath;
    private String csvRowIdentifier;
    private String csvColumnName;
    private String excelFilePath;
    private String excelRowIdentifier;
    private String excelColumnName;

    public PerformanceRequestBuilder() {
        this.protocol = PerformanceConfigurationProperties.getProtocol();
        this.host = PerformanceConfigurationProperties.getHost();
        this.port = PerformanceConfigurationProperties.getPort();
        this.contentType = "application/json";
        this.acceptType = "application/json";
    }

    public PerformanceRequestBuilder withRequestName(String requestName) {
        this.requestName = requestName;
        return this;
    }

    public PerformanceRequestBuilder withMethod(String method) {
        this.method = method;
        return this;
    }

    public PerformanceRequestBuilder withProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public PerformanceRequestBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    public PerformanceRequestBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public PerformanceRequestBuilder withPath(String path) {
        this.path = path;
        return this;
    }

    public PerformanceRequestBuilder withJsonBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public PerformanceRequestBuilder withRequestBody(String requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public PerformanceRequestBuilder withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public PerformanceRequestBuilder withAcceptType(String acceptType) {
        this.acceptType = acceptType;
        return this;
    }

    public PerformanceRequestBuilder withHeader(String name, String value) {
        if (name != null && !name.isBlank() && value != null) {
            this.headers.put(name, value);
        }
        return this;
    }

    public PerformanceRequestBuilder withHeaders(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            this.headers.putAll(headers);
        }
        return this;
    }

    public PerformanceRequestBuilder withBearerTokenAlias(String bearerTokenAlias) {
        this.bearerTokenAlias = bearerTokenAlias;
        return this;
    }

    public PerformanceRequestBuilder withBasicAuth(String username, String password) {
        this.basicAuthUsername = username;
        this.basicAuthPassword = password;
        return this;
    }

    public PerformanceRequestBuilder withYamlBodyKey(String yamlBodyKey) {
        this.yamlBodyKey = yamlBodyKey;
        return this;
    }

    public PerformanceRequestBuilder withCsvBody(String csvFilePath,
                                                 String csvRowIdentifier,
                                                 String csvColumnName) {
        this.csvFilePath = csvFilePath;
        this.csvRowIdentifier = csvRowIdentifier;
        this.csvColumnName = csvColumnName;
        return this;
    }

    public PerformanceRequestBuilder withExcelBody(String excelFilePath,
                                                   String excelRowIdentifier,
                                                   String excelColumnName) {
        this.excelFilePath = excelFilePath;
        this.excelRowIdentifier = excelRowIdentifier;
        this.excelColumnName = excelColumnName;
        return this;
    }

    public PerformanceRequest build() {
        resolvePayloadIfNeeded();
        validate();

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

    private void resolvePayloadIfNeeded() {
        if (requestBody != null && !requestBody.isBlank()) {
            return;
        }

        if (yamlBodyKey != null && !yamlBodyKey.isBlank()) {
            requestBody = PerformancePayloadResolver.resolveYaml(yamlBodyKey);
            return;
        }

        if (csvFilePath != null && !csvFilePath.isBlank()) {
            requestBody = PerformancePayloadResolver.resolveCsv(
                    csvFilePath,
                    csvRowIdentifier,
                    csvColumnName
            );
            return;
        }

        if (excelFilePath != null && !excelFilePath.isBlank()) {
            requestBody = PerformancePayloadResolver.resolveExcel(
                    excelFilePath,
                    excelRowIdentifier,
                    excelColumnName
            );
        }
    }

    private String resolvePayloadSourceType() {
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
            return "Inline JSON";
        }

        if ("GET".equals(normalizedMethod) || "DELETE".equals(normalizedMethod)) {
            return "No Payload";
        }

        return "Unknown";
    }

    private String resolvePayloadSourceDetails() {
        String normalizedMethod = method == null ? "" : method.trim().toUpperCase();

        if (yamlBodyKey != null && !yamlBodyKey.isBlank()) {
            return "YAML key: " + yamlBodyKey;
        }

        if (csvFilePath != null && !csvFilePath.isBlank()) {
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
            String trimmed = requestBody.trim();
            return trimmed.length() > 120 ? trimmed.substring(0, 120) + "..." : trimmed;
        }

        if ("GET".equals(normalizedMethod) || "DELETE".equals(normalizedMethod)) {
            return "Request type does not require a body payload.";
        }

        return "Payload details not available.";
    }

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

        if (bearerTokenAlias != null && !bearerTokenAlias.isBlank()
                && basicAuthUsername != null && !basicAuthUsername.isBlank()) {
            throw new IllegalArgumentException("Performance request cannot use bearer token auth and basic auth at the same time.");
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }
}