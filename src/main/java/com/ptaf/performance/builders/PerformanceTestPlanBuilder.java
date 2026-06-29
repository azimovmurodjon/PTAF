package com.ptaf.performance.builders;

import com.ptaf.performance.models.PerformanceProfile;
import com.ptaf.performance.models.PerformanceRequest;
import org.apache.http.entity.ContentType;
import us.abstracta.jmeter.javadsl.core.DslTestPlan;
import us.abstracta.jmeter.javadsl.core.threadgroups.DslDefaultThreadGroup;
import us.abstracta.jmeter.javadsl.http.DslHttpSampler;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
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
 *
 * <p>Responsibilities:
 * - Convert a PerformanceRequest and PerformanceProfile into a DslTestPlan.
 * - Resolve and merge headers including Accept/Content-Type, bearer tokens and basic auth.
 * - Configure HTTP defaults and shared headers for the test plan.
 * - Create an appropriate thread group either in iteration mode or duration mode.</p>
 */
public class PerformanceTestPlanBuilder {

    /**
     * Overload used by PerformanceEngine.
     *
     * <p>This variant accepts token store and string paths which are converted
     * to java.nio.file.Path before delegating to the main builder method.</p>
     *
     * @param request      high-level request model describing endpoint, method, body and headers.
     * @param profile      performance profile describing users, ramp-up, hold or iterations.
     * @param tokenStore   optional map of bearer token aliases to actual token values. May be null
     *                     if no bearer tokens are required by the request (in which case the request
     *                     must not contain a bearer token alias).
     * @param jtlFilePath  path to the JMeter .jtl results file as a string.
     * @param dashboardPath path to write the HTML dashboard as a string.
     * @param summaryPath  (unused in current implementation) kept for compatibility with callers.
     * @return fully built DslTestPlan ready to be executed by the JMeter DSL runner.
     * @throws IllegalArgumentException if validation of inputs fails while building.
     */
    public DslTestPlan buildHttpTestPlan(PerformanceRequest request,
                                         PerformanceProfile profile,
                                         Map<String, String> tokenStore,
                                         String jtlFilePath,
                                         String dashboardPath,
                                         String summaryPath) {

        // Convert provided string paths to java.nio.file.Path for internal processing.
        Path jtlPath = Paths.get(jtlFilePath);
        Path dashboardOutputPath = Paths.get(dashboardPath);

        // Resolve headers by combining request headers, accept/content-type, bearer token and basic auth.
        Map<String, String> resolvedHeaders = resolveHeaders(request, tokenStore);

        // Delegate to the main builder method that accepts Paths and resolved headers.
        return buildHttpTestPlan(
                request,
                profile,
                resolvedHeaders,
                dashboardOutputPath,
                jtlPath
        );
    }

    /**
     * Builds a complete HTTP test plan using already-resolved headers.
     *
     * <p>Main entry point for constructing a JMeter DSL test plan. This method validates
     * inputs, constructs the HTTP sampler and thread group, and attaches JTL writer and
     * HTML reporter elements required for execution and reporting.</p>
     *
     * @param request         PerformanceRequest describing the HTTP call to make.
     * @param profile         PerformanceProfile with execution parameters.
     * @param resolvedHeaders Headers already resolved and ready to be applied to the test plan.
     * @param dashboardPath   Path to write the HTML dashboard into.
     * @param jtlFilePath     Path to write the JTL results into.
     * @return DslTestPlan ready to be executed.
     * @throws IllegalArgumentException when inputs are invalid or inconsistent.
     */
    public DslTestPlan buildHttpTestPlan(PerformanceRequest request,
                                         PerformanceProfile profile,
                                         Map<String, String> resolvedHeaders,
                                         Path dashboardPath,
                                         Path jtlFilePath) {

        // Validate all inputs early to fail fast and provide clear error messages to testers.
        validateInputs(request, profile, resolvedHeaders, dashboardPath, jtlFilePath);

        // Create the HTTP sampler representing the single request under test.
        DslHttpSampler sampler = buildSampler(request);
        // Create the thread group that will execute the sampler according to the profile.
        DslDefaultThreadGroup executionThreadGroup =
                buildThreadGroup(request, profile, sampler, resolvedHeaders);

        // Assemble and return the final test plan:
        // - thread group containing sampler, shared headers and HTTP defaults
        // - JTL writer that will output results into the specified directory/file
        // - HTML reporter that will generate the dashboard at the specified path
        return testPlan(
                executionThreadGroup,
                jtlWriter(
                        jtlFilePath.getParent().toString(),
                        jtlFilePath.getFileName().toString()
                ),
                htmlReporter(dashboardPath.toString())
        );
    }

    /**
     * Combines and resolves headers for a request.
     *
     * <p>Resolution order:
     * - copy any explicit headers from the request (request.getHeaders())
     * - set Accept header if request provides an accept type and header not already present
     * - set Content-Type header if request provides content type and header not already present
     * - resolve bearer token alias into Authorization: Bearer &lt;token&gt; if provided
     * - resolve basic auth username/password into Authorization: Basic &lt;base64&gt; if provided</p>
     *
     * @param request    request model that may contain header values, aliases and credentials.
     * @param tokenStore map of bearer token aliases to actual tokens; required only if request supplies a bearer alias.
     * @return a new Map containing resolved headers. Never null.
     * @throws IllegalArgumentException if a bearer token alias was provided but tokenStore is null,
     *                                  or alias is not found / blank in the token store.
     */
    private Map<String, String> resolveHeaders(PerformanceRequest request,
                                               Map<String, String> tokenStore) {

        // Use a LinkedHashMap to preserve order which can aid debugging and deterministic outputs.
        Map<String, String> headers = new LinkedHashMap<>();

        // If the request already provides explicit headers, copy them first.
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            headers.putAll(request.getHeaders());
        }

        // Respect explicit Accept and Content-Type values on the request if not already set in headers.
        if (request.getAcceptType() != null && !request.getAcceptType().isBlank()) {
            headers.putIfAbsent("Accept", request.getAcceptType().trim());
        }

        if (request.getContentType() != null && !request.getContentType().isBlank()) {
            headers.putIfAbsent("Content-Type", request.getContentType().trim());
        }

        // Resolve bearer token alias into Authorization header.
        if (request.getBearerTokenAlias() != null && !request.getBearerTokenAlias().isBlank()) {
            // If the caller provided an alias but no token store is available, fail early with a clear message.
            if (tokenStore == null) {
                throw new IllegalArgumentException(
                        "Bearer token alias was provided but token store is null. Alias: "
                                + request.getBearerTokenAlias()
                );
            }

            // Lookup token by alias and validate presence.
            String resolvedToken = tokenStore.get(request.getBearerTokenAlias());
            if (resolvedToken == null || resolvedToken.isBlank()) {
                throw new IllegalArgumentException(
                        "No bearer token found for alias: " + request.getBearerTokenAlias()
                );
            }

            // Add Authorization header using Bearer scheme.
            headers.put("Authorization", "Bearer " + resolvedToken);
        }

        // Resolve Basic Authentication if username is provided.
        if (request.getBasicAuthUsername() != null && !request.getBasicAuthUsername().isBlank()) {
            String username = request.getBasicAuthUsername();
            // Password may be null; treat as empty string if absent.
            String password = request.getBasicAuthPassword() == null ? "" : request.getBasicAuthPassword();
            String rawValue = username + ":" + password;
            // Perform Base64 encoding using UTF-8 charset, as required by HTTP Basic Auth spec.
            String encoded = java.util.Base64.getEncoder()
                    .encodeToString(rawValue.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            headers.put("Authorization", "Basic " + encoded);
        }

        return headers;
    }

    /**
     * Build the HTTP sampler for the provided request.
     *
     * <p>Sampler creation rules:
     * - Determine sampler name from request or derive from method + path.
     * - Construct full URL including protocol, host, port and path.
     * - Normalize method to uppercase with default GET if null.
     * - If method is POST and a body is present, use httpSampler(...).post(body, contentType)
     *   which sets method+body in one fluent call.
     * - For other methods, set method explicitly then attach body/content-type only if the method allows a body.</p>
     *
     * @param request request model containing method, path, body and content-type info.
     * @return configured DslHttpSampler ready to be attached to a thread group.
     */
    private DslHttpSampler buildSampler(PerformanceRequest request) {
        String samplerName = resolveSamplerName(request);
        String fullUrl = buildFullUrl(request);
        String method = normalizeMethod(request.getMethod());
        String requestBody = request.getRequestBody();

        // If POST and body present, use the convenience .post(body, contentType) builder.
        if ("POST".equals(method) && hasBody(requestBody)) {
            return httpSampler(samplerName, fullUrl)
                    .post(requestBody, resolveContentType(request.getContentType()));
        }

        // For all other methods, start with a sampler and set method explicitly.
        DslHttpSampler sampler = httpSampler(samplerName, fullUrl)
                .method(method);

        // If a body is present and the method semantically allows a request body, set content-type and body.
        if (hasBody(requestBody) && allowsBody(method)) {
            sampler = sampler
                    .contentType(resolveContentType(request.getContentType()))
                    .body(requestBody);
        }

        return sampler;
    }

    /**
     * Build a thread group with HTTP defaults and shared headers.
     *
     * <p>This method:
     * - creates shared headers element populated with resolved headers
     * - creates http defaults element using protocol, host and port from the request
     * - chooses execution mode based on the profile:
     *   - iteration mode when profile.iterations > 0: ramp to users and hold for the given number of iterations
     *   - duration mode otherwise: ramp to users and hold for specified hold seconds</p>
     *
     * @param request         PerformanceRequest containing host/protocol/port.
     * @param profile         PerformanceProfile controlling user load and timing.
     * @param sampler         HTTP sampler to be executed by the thread group.
     * @param resolvedHeaders headers to be applied as shared headers for all samplers.
     * @return configured DslDefaultThreadGroup instance.
     */
    private DslDefaultThreadGroup buildThreadGroup(PerformanceRequest request,
                                                   PerformanceProfile profile,
                                                   DslHttpSampler sampler,
                                                   Map<String, String> resolvedHeaders) {

        // Build httpHeaders element and populate with all resolved headers.
        var sharedHeaders = httpHeaders();
        for (Map.Entry<String, String> entry : resolvedHeaders.entrySet()) {
            sharedHeaders.header(entry.getKey(), entry.getValue());
        }

        // HTTP defaults to avoid repeating protocol/host/port on each sampler.
        var defaults = httpDefaults()
                .protocol(request.getProtocol())
                .host(request.getHost())
                .port(request.getPort());

        // If iterations > 0 then the profile is interpreted in iteration mode:
        // ramp to X users and perform N iterations (per user) then stop.
        if (profile.getIterations() > 0) {
            return threadGroup()
                    .rampTo(profile.getUsers(), ofSeconds(profile.getRampUpSeconds()))
                    .holdIterating(profile.getIterations())
                    .children(defaults, sharedHeaders, sampler);
        }

        // Otherwise interpret profile in duration mode: ramp to X users, hold for Y seconds, then stop.
        return threadGroup()
                .rampToAndHold(
                        profile.getUsers(),
                        ofSeconds(profile.getRampUpSeconds()),
                        ofSeconds(profile.getHoldSeconds())
                )
                .children(defaults, sharedHeaders, sampler);
    }

    /**
     * Resolve a string content type to an org.apache.http.entity.ContentType.
     *
     * <p>If contentType is null or blank, defaults to application/json.
     * Recognized values are matched case-insensitively and trimmed. Unrecognized
     * types return application/json by default.</p>
     *
     * @param contentType string representation of the content-type header value.
     * @return appropriate ContentType instance mapping.
     */
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

    /**
     * Construct the full URL from request components (protocol, host, port, path).
     *
     * <p>This method normalizes the path to ensure it begins with a '/' character.
     * It does not perform URL encoding and expects the provided components to be valid.</p>
     *
     * @param request request containing protocol, host, port and path.
     * @return fully concatenated URL string (e.g. "http://example.com:8080/api/foo").
     */
    private String buildFullUrl(PerformanceRequest request) {
        String protocol = request.getProtocol();
        String host = request.getHost();
        int port = request.getPort();
        String path = request.getPath();

        // Ensure path starts with '/'; if path is null, this will produce "null" and will be rejected by validation earlier.
        String normalizedPath = (path != null && path.startsWith("/")) ? path : "/" + path;
        return protocol + "://" + host + ":" + port + normalizedPath;
    }

    /**
     * Determine sampler name for reporting purposes.
     *
     * <p>If the request provided an explicit requestName, it will be used. Otherwise the name
     * is derived as "METHOD PATH" where METHOD is normalized to uppercase.</p>
     *
     * @param request request that may hold a readable name for the request.
     * @return sampler name to be displayed in JMeter results and reports.
     */
    private String resolveSamplerName(PerformanceRequest request) {
        if (request.getRequestName() != null && !request.getRequestName().isBlank()) {
            return request.getRequestName();
        }

        return normalizeMethod(request.getMethod()) + " " + request.getPath();
    }

    /**
     * Helper to determine if requestBody contains meaningful content (not null/blank).
     *
     * @param requestBody raw request body string from model.
     * @return true when the body is present and not blank.
     */
    private boolean hasBody(String requestBody) {
        return requestBody != null && !requestBody.isBlank();
    }

    /**
     * Determine whether the HTTP method allows a request body.
     *
     * <p>Typical methods that accept bodies in practice: POST, PUT, PATCH.</p>
     *
     * @param method uppercase HTTP method (caller often passes normalized value).
     * @return true when the method semantically allows a request body.
     */
    private boolean allowsBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    /**
     * Normalize an HTTP method string to a non-null uppercase value.
     *
     * @param method raw method string; may be null.
     * @return trimmed uppercase method, or "GET" when input is null.
     */
    private String normalizeMethod(String method) {
        return method == null ? "GET" : method.trim().toUpperCase();
    }

    /**
     * Validate high-level inputs and throw IllegalArgumentException with clear messages when invalid.
     *
     * <p>Checks cover null references as well as logically invalid numeric values (e.g. users <= 0)
     * and incompatible profile settings (both iterations and hold seconds must be >0 for respective modes).</p>
     *
     * @param request         PerformanceRequest to validate (must not be null and must contain required fields).
     * @param profile         PerformanceProfile to validate (must not be null and must contain sensible numeric fields).
     * @param resolvedHeaders resolved headers map (must not be null).
     * @param dashboardPath   path to HTML dashboard (must not be null).
     * @param jtlFilePath     path to JTL results file (must not be null).
     * @throws IllegalArgumentException when any validation rule fails. Messages are intended to help testers correct configuration.
     */
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

        // Ensure at least one mode is valid: either iterations > 0 (iteration mode)
        // or holdSeconds > 0 (duration mode). If both are non-positive, configuration is invalid.
        if (profile.getIterations() <= 0 && profile.getHoldSeconds() <= 0) {
            throw new IllegalArgumentException(
                    "Performance profile is invalid. For duration mode, holdSeconds must be greater than 0. " +
                            "For iteration mode, iterations must be greater than 0."
            );
        }
    }
}
