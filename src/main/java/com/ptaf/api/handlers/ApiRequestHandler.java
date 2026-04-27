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

    private static final Logger logger = LoggerFactory.getLogger(ApiRequestHandler.class);

    /**
     * ThreadLocal APIRequestContext ensures each parallel test thread has its own isolated API context.
     */
    private static final ThreadLocal<APIRequestContext> apiContextThreadLocal = new ThreadLocal<>();

    /**
     * ThreadLocal Playwright instance ensures each API context can be cleaned up safely per thread.
     */
    private static final ThreadLocal<Playwright> playwrightThreadLocal = new ThreadLocal<>();

    /**
     * Private constructor prevents object creation because this is a utility/lifecycle handler class.
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
     * @param serviceName The service key from config.yml under api_services.
     *                    Example: api_services.jsonplaceholder.base_url
     * @return Thread-safe APIRequestContext for the requested service.
     */
    public static APIRequestContext getContext(String serviceName) {
        if (apiContextThreadLocal.get() == null) {
            logger.info("Creating new APIRequestContext for service: {}", serviceName);

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
             */
            String baseUrl = ConfigurationProperties.getValue("api_services." + serviceName + ".base_url");
            String tokenEnvVar = ConfigurationProperties.getValue("api_services." + serviceName + ".auth_token_env");

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
             */
            boolean ignoreHTTPSErrors = Boolean.parseBoolean(ConfigurationProperties.getIgnoreHTTPSErrors());

            APIRequest.NewContextOptions options = new APIRequest.NewContextOptions()
                    .setBaseURL(baseUrl)
                    .setIgnoreHTTPSErrors(ignoreHTTPSErrors);

            logger.info("API ignoreHTTPSErrors value from config.yml: {}", ignoreHTTPSErrors);

            /*
             * Add Authorization header only when auth_token_env is configured.
             * The actual token value is read from an environment variable to avoid storing
             * sensitive credentials directly in the framework source code or config.yml.
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
                logger.info("No API auth token environment variable configured for service: {}", serviceName);
            }

            APIRequestContext apiContext = playwright.request().newContext(options);
            apiContextThreadLocal.set(apiContext);

            logger.info("APIRequestContext created successfully for service: {}", serviceName);
        }

        return apiContextThreadLocal.get();
    }

    /**
     * Disposes the APIRequestContext and closes the Playwright instance for the current thread.
     *
     * <p>
     * This method should be called from an @After hook or teardown process to prevent
     * memory leaks and ensure clean execution during parallel test runs.
     * </p>
     */
    public static void disposeContext() {
        APIRequestContext context = apiContextThreadLocal.get();

        if (context != null) {
            try {
                context.dispose();
                logger.info("APIRequestContext disposed successfully for the current thread.");
            } catch (Exception e) {
                logger.error("Failed to dispose APIRequestContext for the current thread.", e);
            } finally {
                apiContextThreadLocal.remove();
            }
        }

        Playwright playwright = playwrightThreadLocal.get();

        if (playwright != null) {
            try {
                playwright.close();
                logger.info("Playwright instance closed successfully for the current API thread.");
            } catch (Exception e) {
                logger.error("Failed to close Playwright instance for the current API thread.", e);
            } finally {
                playwrightThreadLocal.remove();
            }
        }
    }
}