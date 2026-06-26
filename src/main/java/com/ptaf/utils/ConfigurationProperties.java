package com.ptaf.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigurationProperties is a centralized utility class responsible for retrieving
 * framework-level configuration values from the YAML configuration file.
 *
 * <p>
 * Enterprise Framework Responsibility:
 * This class provides one controlled access point for configuration values used
 * across UI, API, DB, PDF, Performance, and other framework modules.
 * Centralizing configuration access improves maintainability, consistency,
 * readability, and long-term framework scalability.
 * </p>
 *
 * <p>
 * Configuration Source:
 * Values are read from config.yml through the YamlReader utility.
 * </p>
 */
public class ConfigurationProperties {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationProperties.class);

    /**
     * Private constructor prevents object creation because this class is designed
     * as a static utility class.
     */
    private ConfigurationProperties() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves the base URL from the YAML configuration using the specified key.
     *
     * @param URL the YAML key for the target application URL.
     * @return the configured base URL as a String, or null if the key is not found.
     */
    public static String getBaseUrl(String URL) {
        return getValue(URL);
    }

    /**
     * Retrieves the configured browser type from config.yml.
     *
     * <p>
     * Example config.yml value:
     * browser: "chrome"
     * </p>
     *
     * @return the configured browser name, or null if not configured.
     */
    public static String getBrowser() {
        return getValue("browser");
    }

    /**
     * Retrieves the configured headless mode value from config.yml.
     *
     * <p>
     * Example config.yml value:
     * headless: "false"
     * </p>
     *
     * @return "true" or "false" as configured in config.yml.
     */
    public static String getHeadlessMode() {
        return getValue("headless");
    }

    /**
     * Retrieves the HTTPS / SSL certificate error handling configuration from config.yml.
     *
     * <p>
     * Example config.yml value:
     * ignoreHTTPSErrors: "true"
     * </p>
     *
     * <p>
     * Enterprise Usage:
     * When this value is set to true, the framework can bypass SSL certificate
     * validation errors for both UI and API automation. This is commonly required
     * for QA, DEV, SIT, UAT, and internal test environments where certificates
     * may be self-signed, expired, internally issued, or not trusted by the local machine.
     * </p>
     *
     * <p>
     * Security Note:
     * For production validation, this value can be set to false to enforce strict
     * certificate validation based on enterprise security requirements.
     * </p>
     *
     * @return "true" or "false" as configured in config.yml. Defaults to "false" if missing.
     */
    public static String getIgnoreHTTPSErrors() {
        String value = getValue("ignoreHTTPSErrors");

        if (value == null || value.trim().isEmpty()) {
            logger.warn("Configuration key 'ignoreHTTPSErrors' was not found. Defaulting to false.");
            return "false";
        }

        return value;
    }

    /**
     * Retrieves the YAML store location from config.yml.
     *
     * @return the configured YAML store location, or null if not configured.
     */
    public static String getYamlStoreLocation() {
        return getValue("yamlStoreLocation");
    }

    /**
     * Retrieves the Excel document location from config.yml.
     *
     * @return the configured Excel document location, or null if not configured.
     */
    public static String getExcelDocumentLocation() {
        return getValue("excelDocumentLocation");
    }

    /**
     * Retrieves the video capture configuration from config.yml.
     *
     * <p>
     * Example config.yml value:
     * videoCapture: "false"
     * </p>
     *
     * @return "true" or "false" as configured in config.yml.
     */
    public static String getVideoCapture() {
        return getValue("videoCapture");
    }

    /**
     * Retrieves a generic configuration value from config.yml.
     *
     * <p>
     * This method supports nested YAML paths when the YamlReader implementation
     * supports dot notation.
     * </p>
     *
     * <p>
     * Example:
     * api_services.jsonplaceholder.base_url
     * </p>
     *
     * @param value the YAML key or nested YAML path.
     * @return the configuration value as a String, or null if not found.
     */
    /**
     * Retrieves the runtime timeout in milliseconds from config.yml.
     *
     * <p>
     * Example config.yml value:
     * runtimeTimeoutMillis: 30000
     * </p>
     *
     * @return the configured runtime timeout as a long, or a default value if not configured.
     */
    public static long getRuntimeTimeoutMillis() {
        String value = getValue("runtimeTimeoutMillis");
        if (value == null || value.trim().isEmpty()) {
            logger.warn("Configuration key 'runtimeTimeoutMillis' was not found. Defaulting to 30000 ms.");
            return 30000L; // Default to 30 seconds
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.error("Invalid number format for runtimeTimeoutMillis: {}. Defaulting to 30000 ms.", value, e);
            return 30000L;
        }
    }

    public static String getValue(String value) {
        Object rawValue = YamlReader.get(value);
        return rawValue == null ? null : String.valueOf(rawValue);
    }
}