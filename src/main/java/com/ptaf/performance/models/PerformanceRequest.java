package com.ptaf.performance.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Framework-owned immutable request model for performance execution.
 *
 * <p>This object is constructed only through builder layers and consumed
 * by architect-controlled engine/test-plan classes.</p>
 */
public class PerformanceRequest {

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
                              String basicAuthPassword) {
        this.requestName = requestName;
        this.method = method;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.path = path;
        this.requestBody = requestBody;
        this.contentType = contentType;
        this.acceptType = acceptType;
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.bearerTokenAlias = bearerTokenAlias;
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
                '}';
    }
}