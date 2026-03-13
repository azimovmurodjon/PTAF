package com.ptaf.performance.builders;

import com.ptaf.performance.config.PerformanceConfigurationProperties;
import com.ptaf.performance.models.PerformanceRequest;
import com.ptaf.performance.models.PerformanceRequest.AuthStrategy;
import com.ptaf.performance.payloads.PayloadSourceType;
import com.ptaf.performance.payloads.PerformancePayloadDefinition;
import com.ptaf.performance.payloads.PerformancePayloadResolver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Architect-controlled builder for framework-owned performance requests.
 */
public class PerformanceRequestBuilder {

    private String requestName;
    private String method;
    private String protocol;
    private String host;
    private int port;
    private String path;
    private String requestBody;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private String contentType;
    private String acceptType;
    private PerformancePayloadDefinition payloadDefinition;
    private AuthStrategy authStrategy;
    private String tokenAlias;
    private String basicAuthUsername;
    private String basicAuthPassword;

    public PerformanceRequestBuilder() {
        this.protocol = PerformanceConfigurationProperties.getProtocol();
        this.host = PerformanceConfigurationProperties.getHost();
        this.port = PerformanceConfigurationProperties.getPort();
        this.method = "GET";
        this.authStrategy = AuthStrategy.NONE;
    }

    public PerformanceRequestBuilder withRequestName(String requestName) {
        this.requestName = requestName;
        return this;
    }

    public PerformanceRequestBuilder withMethod(String method) {
        this.method = normalizeMethod(method);
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

    public PerformanceRequestBuilder withBody(String requestBody) {
        this.requestBody = requestBody;
        this.payloadDefinition = new PerformancePayloadDefinition(
                PayloadSourceType.INLINE,
                requestBody,
                null,
                null,
                null,
                null,
                null
        );
        return this;
    }

    public PerformanceRequestBuilder withJsonBody(String jsonBody) {
        this.requestBody = jsonBody;
        this.contentType = "application/json";
        this.payloadDefinition = new PerformancePayloadDefinition(
                PayloadSourceType.INLINE,
                jsonBody,
                null,
                null,
                null,
                null,
                null
        );
        return this;
    }

    public PerformanceRequestBuilder withYamlBodyKey(String yamlKey) {
        this.payloadDefinition = new PerformancePayloadDefinition(
                PayloadSourceType.YAML,
                null,
                yamlKey,
                null,
                null,
                null,
                null
        );
        return this;
    }

    public PerformanceRequestBuilder withCsvBody(String classpathFilePath,
                                                 String rowIdentifier,
                                                 String columnName) {
        this.payloadDefinition = new PerformancePayloadDefinition(
                PayloadSourceType.CSV,
                null,
                null,
                classpathFilePath,
                null,
                rowIdentifier,
                columnName
        );
        return this;
    }

    public PerformanceRequestBuilder withExcelBody(String filePath,
                                                   String rowIdentifier,
                                                   String columnName) {
        this.payloadDefinition = new PerformancePayloadDefinition(
                PayloadSourceType.EXCEL,
                null,
                null,
                filePath,
                null,
                rowIdentifier,
                columnName
        );
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

    public PerformanceRequestBuilder addHeader(String key, String value) {
        validateHeader(key, value);
        headers.put(key, value);
        return this;
    }

    public PerformanceRequestBuilder addHeaders(Map<String, String> inputHeaders) {
        if (inputHeaders == null || inputHeaders.isEmpty()) {
            return this;
        }

        for (Map.Entry<String, String> entry : inputHeaders.entrySet()) {
            addHeader(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public PerformanceRequestBuilder withBearerTokenAlias(String tokenAlias) {
        if (tokenAlias == null || tokenAlias.isBlank()) {
            throw new IllegalArgumentException("Token alias cannot be null or blank.");
        }

        this.authStrategy = AuthStrategy.BEARER_TOKEN;
        this.tokenAlias = tokenAlias.trim();
        this.basicAuthUsername = null;
        this.basicAuthPassword = null;
        return this;
    }

    public PerformanceRequestBuilder withBasicAuth(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Basic auth username cannot be null or blank.");
        }

        if (password == null) {
            throw new IllegalArgumentException("Basic auth password cannot be null.");
        }

        this.authStrategy = AuthStrategy.BASIC_AUTH;
        this.basicAuthUsername = username.trim();
        this.basicAuthPassword = password;
        this.tokenAlias = null;
        return this;
    }

    public PerformanceRequestBuilder withNoAuth() {
        this.authStrategy = AuthStrategy.NONE;
        this.tokenAlias = null;
        this.basicAuthUsername = null;
        this.basicAuthPassword = null;
        return this;
    }

    public PerformanceRequest build() {
        String finalRequestBody = requestBody;

        if (payloadDefinition != null) {
            finalRequestBody = PerformancePayloadResolver.resolve(payloadDefinition);
        }

        validate(finalRequestBody);

        Map<String, String> finalHeaders = new LinkedHashMap<>(headers);

        if (contentType != null && !contentType.isBlank() && !finalHeaders.containsKey("Content-Type")) {
            finalHeaders.put("Content-Type", contentType.trim());
        }

        if (acceptType != null && !acceptType.isBlank() && !finalHeaders.containsKey("Accept")) {
            finalHeaders.put("Accept", acceptType.trim());
        }

        return new PerformanceRequest(
                requestName,
                method,
                protocol,
                host,
                port,
                normalizePath(path),
                finalRequestBody,
                finalHeaders,
                contentType,
                acceptType,
                payloadDefinition,
                authStrategy,
                tokenAlias,
                basicAuthUsername,
                basicAuthPassword
        );
    }

    private void validate(String finalRequestBody) {
        if (requestName == null || requestName.isBlank()) {
            throw new IllegalArgumentException("Performance request validation failed: requestName cannot be null or blank.");
        }

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Performance request validation failed: method cannot be null or blank.");
        }

        if (protocol == null || protocol.isBlank()) {
            throw new IllegalArgumentException("Performance request validation failed: protocol cannot be null or blank.");
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Performance request validation failed: host cannot be null or blank.");
        }

        if (port <= 0) {
            throw new IllegalArgumentException("Performance request validation failed: port must be greater than 0.");
        }

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Performance request validation failed: path cannot be null or blank.");
        }

        boolean bodyAllowed = isBodyAllowed(method);
        if (!bodyAllowed && finalRequestBody != null && !finalRequestBody.isBlank()) {
            throw new IllegalArgumentException(
                    "Performance request validation failed: method " + method + " should not contain request body."
            );
        }

        if (authStrategy == AuthStrategy.BEARER_TOKEN) {
            if (tokenAlias == null || tokenAlias.isBlank()) {
                throw new IllegalArgumentException(
                        "Performance request validation failed: tokenAlias is required for BEARER_TOKEN strategy."
                );
            }
        }

        if (authStrategy == AuthStrategy.BASIC_AUTH) {
            if (basicAuthUsername == null || basicAuthUsername.isBlank()) {
                throw new IllegalArgumentException(
                        "Performance request validation failed: basicAuthUsername is required for BASIC_AUTH strategy."
                );
            }

            if (basicAuthPassword == null) {
                throw new IllegalArgumentException(
                        "Performance request validation failed: basicAuthPassword is required for BASIC_AUTH strategy."
                );
            }
        }
    }

    private boolean isBodyAllowed(String method) {
        String normalizedMethod = normalizeMethod(method);
        return "POST".equals(normalizedMethod)
                || "PUT".equals(normalizedMethod)
                || "PATCH".equals(normalizedMethod);
    }

    private String normalizeMethod(String method) {
        return method == null ? null : method.trim().toUpperCase();
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        return path.startsWith("/") ? path : "/" + path.trim();
    }

    private void validateHeader(String key, String value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Header name cannot be null or blank.");
        }

        if (value == null) {
            throw new IllegalArgumentException("Header value cannot be null for header: " + key);
        }
    }
}