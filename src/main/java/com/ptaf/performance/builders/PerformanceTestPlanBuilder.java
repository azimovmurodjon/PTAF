package com.ptaf.performance.builders;

import com.ptaf.performance.models.PerformanceProfile;
import com.ptaf.performance.models.PerformanceRequest;
import org.apache.http.entity.ContentType;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

import java.nio.file.Path;
import java.util.Map;

import static java.time.Duration.ofSeconds;
import static us.abstracta.jmeter.javadsl.JmeterDsl.htmlReporter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpDefaults;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpHeaders;
import static us.abstracta.jmeter.javadsl.JmeterDsl.httpSampler;
import static us.abstracta.jmeter.javadsl.JmeterDsl.jtlWriter;
import static us.abstracta.jmeter.javadsl.JmeterDsl.testPlan;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

/**
 * Builds JMeter DSL test plans from framework-owned request and profile models.
 *
 * <p>This class is architect-controlled and hides JMeter DSL details
 * from testers and step definitions.</p>
 */
public class PerformanceTestPlanBuilder {

    /**
     * Builds a complete HTTP test plan using already-resolved headers.
     *
     * @param request request definition
     * @param profile execution profile
     * @param resolvedHeaders fully resolved headers including auth and defaults
     * @param dashboardPath dashboard output path
     * @param jtlFilePath jtl output path
     * @return prepared test plan
     */
    public DslTestPlan buildHttpTestPlan(PerformanceRequest request,
                                         PerformanceProfile profile,
                                         Map<String, String> resolvedHeaders,
                                         Path dashboardPath,
                                         Path jtlFilePath) {

        validateInputs(request, profile, resolvedHeaders, dashboardPath, jtlFilePath);

        DslHttpSampler sampler = buildSampler(request);
        DslDefaultThreadGroup executionThreadGroup =
                buildThreadGroup(request, profile, sampler, resolvedHeaders);

        return testPlan(
                executionThreadGroup,
                jtlWriter(
                        jtlFilePath.getParent().toString(),
                        jtlFilePath.getFileName().toString()
                ),
                htmlReporter(dashboardPath.toString())
        );
    }

    private DslHttpSampler buildSampler(PerformanceRequest request) {
        String samplerName = resolveSamplerName(request);
        String fullUrl = buildFullUrl(request);
        String method = normalizeMethod(request.getMethod());
        String requestBody = request.getRequestBody();

        if ("POST".equals(method) && hasBody(requestBody)) {
            return httpSampler(samplerName, fullUrl)
                    .post(requestBody, resolveContentType(request.getContentType()));
        }

        DslHttpSampler sampler = httpSampler(samplerName, fullUrl)
                .method(method);

        if (hasBody(requestBody)) {
            sampler = sampler
                    .contentType(resolveContentType(request.getContentType()))
                    .body(requestBody);
        }

        return sampler;
    }

    private DslDefaultThreadGroup buildThreadGroup(PerformanceRequest request,
                                                   PerformanceProfile profile,
                                                   DslHttpSampler sampler,
                                                   Map<String, String> resolvedHeaders) {

        var sharedHeaders = httpHeaders();
        for (Map.Entry<String, String> entry : resolvedHeaders.entrySet()) {
            sharedHeaders.header(entry.getKey(), entry.getValue());
        }

        var defaults = httpDefaults()
                .protocol(request.getProtocol())
                .host(request.getHost())
                .port(request.getPort());

        if (profile.getIterations() > 0) {
            return threadGroup()
                    .rampTo(profile.getUsers(), ofSeconds(profile.getRampUpSeconds()))
                    .holdIterating(profile.getIterations())
                    .children(defaults, sharedHeaders, sampler);
        }

        return threadGroup()
                .rampToAndHold(
                        profile.getUsers(),
                        ofSeconds(profile.getRampUpSeconds()),
                        ofSeconds(profile.getHoldSeconds())
                )
                .children(defaults, sharedHeaders, sampler);
    }

    private ContentType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return ContentType.APPLICATION_JSON;
        }

        String normalized = contentType.trim().toLowerCase();
        return switch (normalized) {
            case "application/json" -> ContentType.APPLICATION_JSON;
            case "application/xml", "text/xml" -> ContentType.APPLICATION_XML;
            case "text/plain" -> ContentType.TEXT_PLAIN;
            case "application/x-www-form-urlencoded" -> ContentType.APPLICATION_FORM_URLENCODED;
            default -> ContentType.APPLICATION_JSON;
        };
    }

    private String buildFullUrl(PerformanceRequest request) {
        String protocol = request.getProtocol();
        String host = request.getHost();
        int port = request.getPort();
        String path = request.getPath();

        String normalizedPath = (path != null && path.startsWith("/")) ? path : "/" + path;
        return protocol + "://" + host + ":" + port + normalizedPath;
    }

    private String resolveSamplerName(PerformanceRequest request) {
        if (request.getRequestName() != null && !request.getRequestName().isBlank()) {
            return request.getRequestName();
        }

        return normalizeMethod(request.getMethod()) + " " + request.getPath();
    }

    private boolean hasBody(String requestBody) {
        return requestBody != null && !requestBody.isBlank();
    }

    private String normalizeMethod(String method) {
        return method == null ? "GET" : method.trim().toUpperCase();
    }

    private void validateInputs(PerformanceRequest request,
                                PerformanceProfile profile,
                                Map<String, String> resolvedHeaders,
                                Path dashboardPath,
                                Path jtlFilePath) {

        if (request == null) {
            throw new IllegalArgumentException("Performance request cannot be null.");
        }

        if (profile == null) {
            throw new IllegalArgumentException("Performance profile cannot be null.");
        }

        if (resolvedHeaders == null) {
            throw new IllegalArgumentException("Resolved headers cannot be null.");
        }

        if (dashboardPath == null) {
            throw new IllegalArgumentException("Dashboard path cannot be null.");
        }

        if (jtlFilePath == null) {
            throw new IllegalArgumentException("JTL file path cannot be null.");
        }

        if (request.getPath() == null || request.getPath().isBlank()) {
            throw new IllegalArgumentException("Performance request path cannot be null or blank.");
        }

        if (request.getProtocol() == null || request.getProtocol().isBlank()) {
            throw new IllegalArgumentException("Performance request protocol cannot be null or blank.");
        }

        if (request.getHost() == null || request.getHost().isBlank()) {
            throw new IllegalArgumentException("Performance request host cannot be null or blank.");
        }

        if (request.getPort() <= 0) {
            throw new IllegalArgumentException("Performance request port must be greater than 0.");
        }

        if (profile.getUsers() <= 0) {
            throw new IllegalArgumentException("Performance profile users must be greater than 0.");
        }

        if (profile.getRampUpSeconds() < 0) {
            throw new IllegalArgumentException("Performance profile rampUpSeconds cannot be negative.");
        }

        if (profile.getHoldSeconds() < 0) {
            throw new IllegalArgumentException("Performance profile holdSeconds cannot be negative.");
        }

        if (profile.getIterations() < 0) {
            throw new IllegalArgumentException("Performance profile iterations cannot be negative.");
        }

        if (profile.getIterations() <= 0 && profile.getHoldSeconds() <= 0) {
            throw new IllegalArgumentException(
                    "Performance profile is invalid. For duration mode, holdSeconds must be greater than 0. " +
                            "For iteration mode, iterations must be greater than 0."
            );
        }
    }
}