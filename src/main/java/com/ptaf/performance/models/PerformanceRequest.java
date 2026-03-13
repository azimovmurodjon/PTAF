package com.ptaf.performance.models;

import com.ptaf.performance.payloads.PerformancePayloadDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable framework-owned request model for performance execution.
 *
 * <p>This model is intentionally independent from JMeter-specific classes.
 * It allows the framework to remain clean, reusable, and architect-controlled.</p>
 */
public final class PerformanceRequest {

    private final String requestName;
    private final String method;
    private final String protocol;
    private final String host;
    private final int port;
    private final String path;
    private final String requestBody;
    private final Map<String, String> headers;
    private final String contentType;
    private final String acceptType;
    private final PerformancePayloadDefinition payloadDefinition;
    private final AuthStrategy authStrategy;
    private final String tokenAlias;
    private final String basicAuthUsername;
    private final String basicAuthPassword;

    public PerformanceRequest(String requestName,
                              String method,
                              String protocol,
                              String host,
                              int port,
                              String path,
                              String requestBody,
                              Map<String, String> headers,
                              String contentType,
                              String acceptType,
                              PerformancePayloadDefinition payloadDefinition,
                              AuthStrategy authStrategy,
                              String tokenAlias,
                              String basicAuthUsername,
                              String basicAuthPassword) {
        this.requestName = requestName;
        this.method = method;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.requestBody = requestBody;
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.contentType = contentType;
        this.acceptType = acceptType;
        this.payloadDefinition = payloadDefinition;
        this.authStrategy = authStrategy == null ? AuthStrategy.NONE : authStrategy;
        this.tokenAlias = tokenAlias;
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getContentType() {
        return contentType;
    }

    public String getAcceptType() {
        return acceptType;
    }

    public PerformancePayloadDefinition getPayloadDefinition() {
        return payloadDefinition;
    }

    public AuthStrategy getAuthStrategy() {
        return authStrategy;
    }

    public String getTokenAlias() {
        return tokenAlias;
    }

    public String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    public String getBasicAuthPassword() {
        return basicAuthPassword;
    }

    public enum AuthStrategy {
        NONE,
        BEARER_TOKEN,
        BASIC_AUTH
    }
}