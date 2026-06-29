package com.ptaf.api.handlers;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import com.ptaf.utils.ConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * ApiRequestHandler manages the lifecycle of Playwright APIRequestContext instances.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class centralizes API context creation for all API automation tests.
 * It ensures that each execution thread receives an isolated APIRequestContext,
 * which supports parallel execution and prevents test data/session conflicts.
 * </p>
 *
 * <p>
 * HTTPS / SSL Handling:
 * The APIRequestContext is configured using the framework-level
 * "ignoreHTTPSErrors" value from config.yml.
 * </p>
 *
 * <p>
 * When ignoreHTTPSErrors=true:
 * API tests can execute against lower environments where certificates may be
 * self-signed, expired, internally issued, or not trusted by the local machine.
 * </p>
 *
 * <p>
 * When ignoreHTTPSErrors=false:
 * The framework enforces strict SSL certificate validation.
 * </p>
 */
public class ApiRequestHandler {

    /**
     * Logger for diagnostic and lifecycle messages.
     *
     * <p>
     * Examples of messages logged:
     * - When a new APIRequestContext is created
     * - When Authorization header configuration is detected or missing
     * - When resources are disposed
     * </p>
     */
    private static final Logger logger = LoggerFactory.getLogger(ApiRequestHandler.class);

    /**
     * ThreadLocal APIRequestContext ensures each parallel test thread has its own isolated API context.
     *
     * <p>
     * Reasoning:
     * - Playwright APIRequestContext is not thread-safe to share across test threads.
     * - Using ThreadLocal provides per-thread storage so parallel tests do not interfere.
     * </p>
     *
     * <p>
     * Lifecycle:
     * - Initialized lazily on first call to getContext(String).
     * - Cleared by disposeContext() to avoid memory leaks.
     * </p>
     */
    private static final ThreadLocal<APIRequestContext> apiContextThreadLocal = new ThreadLocal<>();

    /**
     * ThreadLocal Playwright instance ensures each API context can be cleaned up safely per thread.
     *
     * <p>
     * Playwright.create() must be matched with playwright.close() to free native resources.
     * Storing Playwright per-thread allows disposeContext() to close the correct Playwright instance.
     * </p>
     */
    private static final ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();

    /**
     * Private constructor prevents object creation because this is a utility/lifecycle handler class.
     *
     * <p>
     * This class exposes only static methods and maintains static ThreadLocal state. Constructing
     * an instance would be meaningless and could encourage incorrect use.
     * </p>
     *
     * @throws IllegalStateException always when called to indicate misuse
     */
    private ApiRequestHandler() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates and returns an APIRequestContext for the current execution thread.
     *
     * <p>
     * If the context already exists for the current thread, the same context is reused.
     * If it does not exist, a new Playwright APIRequestContext is created using the
     * configured service base URL and optional authorization token.
     * </p>
     *
     * <p>
     * Typical usage:
     * - Tests call ApiRequestHandler.getContext("jsonplaceholder") to obtain a ready-to-use context.
     * - The returned context has the base URL and optional Authorization header pre-configured.
     * </p>
     *
     * <p>
     * Thread-safety:
     * - This method is thread-safe in the sense that each calling thread receives its own context.
     * - It is idempotent per thread: subsequent calls from the same thread return the already-created context.
     * </p>
     *
     * @param serviceName The service key from config.yml under api_services.
     *                    Example: api_services.jsonplaceholder.base_url
     * @return Thread-safe APIRequestContext for the requested service.
     * @throws IllegalArgumentException if the configured base URL is missing or if an expected
     *                                  environment variable for the auth token is not set or empty.
     */
    public static APIRequestContext getContext(String serviceName) {
        // If the current thread does not yet have an API context, create and store one.
        if (apiContextThreadLocal.get() == null) {
            logger.info("Creating new APIRequestContext for service: {}", serviceName);

            // Create a Playwright instance specific to this thread and store it for later cleanup.
            Playwright playwright = Playwright.create();
            playwrightThreadLocal.set(playwright);

            /*
             * Read API service configuration from config.yml.
             *
             * Expected config.yml structure:
             *
             * api_services:
             *   jsonplaceholder:
             *     base_url: "https://jsonplaceholder.typicode.com"
             *     auth_token_env: "API_AUTH_TOKEN"
             *
             * Notes:
             * - base_url must be defined; otherwise we cannot create a context.
             * - auth_token_env is optional; when present, it should contain the name of an environment variable
             *   that stores the token value (to avoid committing secrets into config files).
             */
            String baseUrl = ConfigurationProperties.getValue("api_services." + serviceName + ".base_url");
            String tokenEnvVar = ConfigurationProperties.getValue("api_services." + serviceName + ".auth_token_env");

            // Validate that a base URL exists and is not blank.
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Base URL for API service '" + serviceName + "' was not found in config.yml."
                );
            }

            /*
             * Read enterprise SSL bypass setting from config.yml.
             *
             * Example:
             * ignoreHTTPSErrors: "true"
             *
             * This allows teams to enable or disable SSL certificate validation
             * without changing source code.
             *
             * The ConfigurationProperties returns a String; Boolean.parseBoolean handles null and invalid values
             * by returning false in those cases.
             */
            boolean ignoreHTTPSErrors = Boolean.parseBoolean(ConfigurationProperties.getIgnoreHTTPSErrors());

            // Prepare Playwright API request context options with base URL and SSL behavior.
            APIRequest.NewContextOptions options = new APIRequest.NewContextOptions()
                    .setBaseURL(baseUrl)
                    .setIgnoreHTTPSErrors(ignoreHTTPSErrors);

            logger.info("API ignoreHTTPSErrors value from config.yml: {}", ignoreHTTPSErrors);

            /*
             * Add Authorization header only when auth_token_env is configured.
             * The actual token value is read from an environment variable to avoid storing
             * sensitive credentials directly in the framework source code or config.yml.
             *
             * Behavior:
             * - If auth_token_env property is set: read the environment variable with that name.
             *   - If the environment variable is missing or empty, throw IllegalArgumentException to fail fast.
             *   - If present, attach the header "Authorization: Bearer <token>" via extra HTTP headers.
             * - If auth_token_env property is absent or blank: do not add Authorization header.
             */
            if (tokenEnvVar != null && !tokenEnvVar.trim().isEmpty()) {
                String token = System.getenv(tokenEnvVar);

                if (token == null || token.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "API auth token environment variable '" + tokenEnvVar + "' is not set or is empty."
                    );
                }

                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);

                options.setExtraHTTPHeaders(headers);
                logger.info("APIRequestContext configured with Authorization header from environment variable: {}", tokenEnvVar);
            } else {
                // No auth token configured for this service; log that requests will be unauthenticated unless handled elsewhere.
                logger.info("No API auth token environment variable configured for service: {}", serviceName);
            }

            // Create the APIRequestContext using the Playwright instance and the prepared options.
            APIRequestContext apiContext = playwright.request().newContext(options);
            // Store the created context in ThreadLocal so subsequent calls from this thread reuse it.
            apiContextThreadLocal.set(apiContext);

            logger.info("APIRequestContext created successfully for service: {}", serviceName);
        }

        // Return the existing or newly created APIRequestContext for the current thread.
        return apiContextThreadLocal.get();
    }

    /**
     * Disposes the APIRequestContext and closes the Playwright instance for the current thread.
     *
     * <p>
     * This method should be called from an @After hook or teardown process to prevent
     * memory leaks and ensure clean execution during parallel test runs.
     * Steps performed:
     * - Dispose the APIRequestContext (releases HTTP connections, sockets, etc.)
     * - Close the Playwright instance (releases native resources)
     * - Remove ThreadLocal references so the garbage collector can reclaim memory
     * </p>
     *
     * <p>
     * Error handling:
     * - Exceptions thrown during disposal are caught and logged but not rethrown. This is deliberate to allow
     *   test suites to continue cleanup attempts for other threads or resources.
     * </p>
     *
     * <p>
     * Best practice:
     * - Always call disposeContext() in test teardown to keep system resources healthy,
     *   especially when running large test suites in parallel.
     * </p>
     */
    public static void disposeContext() {
        // Retrieve and dispose APIRequestContext for this thread if present.
        APIRequestContext context = apiContextThreadLocal.get();

        if (context != null) {
            try {
                // Dispose releases network resources associated with the context.
                context.dispose();
                logger.info("APIRequestContext disposed successfully for the current thread.");
            } catch (Exception e) {
                // Log the exception to help debugging cleanup failures.
                logger.error("Failed to dispose APIRequestContext for the current thread.", e);
            } finally {
                // Remove the ThreadLocal reference regardless of disposal success to avoid leaks.
                apiContextThreadLocal.remove();
            }
        }

        // Retrieve and close Playwright instance for this thread if present.
        Playwright playwright = playwrightThreadLocal.get();

        if (playwright != null) {
            try {
                // Close Playwright to free native resources; mandatory to avoid resource leaks.
                playwright.close();
                logger.info("Playwright instance closed successfully for the current API thread.");
            } catch (Exception e) {
                // Log the exception but do not propagate to ensure teardown continues.
                logger.error("Failed to close Playwright instance for the current API thread.", e);
            } finally {
                // Remove the ThreadLocal reference regardless of close success to avoid leaks.
                playwrightThreadLocal.remove();
            }
        }
    }
}
