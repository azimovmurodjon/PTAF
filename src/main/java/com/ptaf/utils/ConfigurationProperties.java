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

    /**
     * SLF4J logger used to record warnings, errors, and informational messages
     * while resolving configuration values. Useful for troubleshooting missing
     * or malformed configuration entries during test execution.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationProperties.class);

    /**
     * Private constructor prevents object creation because this class is designed
     * as a static utility class.
     *
     * <p>
     * Throwing an IllegalStateException makes the intent explicit and avoids
     * accidental instantiation in tests or other code.
     * </p>
     */
    private ConfigurationProperties() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves the base URL from the YAML configuration using the specified key.
     *
     * <p>
     * Typical usage: getBaseUrl("application.baseUrl") or getBaseUrl("staging.url")
     * depending on how keys are organized in config.yml.
     * </p>
     *
     * @param URL the YAML key for the target application URL.
     * @return the configured base URL as a String, or null if the key is not found.
     */
    public static String getBaseUrl(String URL) {
        // Delegate to generic getter so callers don't need to know underlying retrieval details.
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
        // Standard key used across framework to identify browser for UI automation.
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
        // Returns string representation; caller should interpret as boolean if necessary.
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
        // Retrieve raw value as string
        String value = getValue("ignoreHTTPSErrors");

        // If the configuration is absent or empty, log a warning and return a safe default.
        if (value == null || value.trim().isEmpty()) {
            logger.warn("Configuration key 'ignoreHTTPSErrors' was not found. Defaulting to false.");
            return "false";
        }

        // Return the configured value (expected to be "true" or "false").
        return value;
    }

    /**
     * Retrieves the YAML store location from config.yml.
     *
     * <p>
     * This path may point to a directory or file that the framework uses to read/write
     * intermediate YAML artifacts during test execution.
     * </p>
     *
     * @return the configured YAML store location, or null if not configured.
     */
    public static String getYamlStoreLocation() {
        return getValue("yamlStoreLocation");
    }

    /**
     * Retrieves the Excel document location from config.yml.
     *
     * <p>
     * This property is commonly used by data-driven test components which rely on
     * spreadsheets for test case definitions, parameters, or expected results.
     * </p>
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
     * <p>
     * When enabled, the framework may record video of UI test runs for debugging
     * and reporting purposes. The returned value is a string and should be
     * interpreted by the caller as a boolean flag.
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
        // First try runtimeTimeoutMillis (milliseconds) for backward compatibility.
        // Fall back to runtimeWait (seconds) which is the actual key in config.yml.
        String value = getValue("runtimeTimeoutMillis");
        if (value != null && !value.trim().isEmpty()) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Try runtimeWait (value in seconds, convert to milliseconds)
        String waitSeconds = getValue("runtimeWait");
        if (waitSeconds != null && !waitSeconds.trim().isEmpty()) {
            try {
                long seconds = Long.parseLong(waitSeconds.trim());
                return seconds * 1000L;
            } catch (NumberFormatException ignored) {}
        }

        // Default to 30 seconds if neither key is found.
        logger.warn("Configuration keys 'runtimeTimeoutMillis' and 'runtimeWait' were not found. Defaulting to 30000 ms.");
        return 30000L;
    }

    /**
     * Generic accessor that delegates to YamlReader to fetch a value by key.
     *
     * <p>
     * Implementation notes:
     * - YamlReader.get(String) is expected to return an Object or null if key is not found.
     * - This method converts the returned Object to its String representation
     *   using String.valueOf. If the raw value is null, this method returns null.
     * - Testers can call this method directly for any ad-hoc configuration keys
     *   not covered by the typed helper methods in this class.
     * </p>
     *
     * @param value the YAML key or path to retrieve.
     * @return the String representation of the configured value, or null if not present.
     */
    public static String getValue(String value) {
        // First try environment-specific key: environments.{env}.{key}
        // This allows per-environment overrides in config.yml under an 'environments' section.
        // The 'env' system property defaults to 'QA' if not set (e.g. -Denv=PROD).
        String env = System.getProperty("env", "QA");
        String envValueKey = "environments." + env + "." + value;

        Object rawValue;

        try {
            YamlReader.setSuppressLogs(true);
            rawValue = YamlReader.get(envValueKey);
        } finally {
            YamlReader.setSuppressLogs(false);
        }

        // Fall back to the plain key if no environment-specific value was found.
        if (rawValue == null) {
            rawValue = YamlReader.get(value);
        }

        // If rawValue is null, return null so callers can handle absence explicitly.
        return rawValue == null ? null : String.valueOf(rawValue);
    }

    // ─── Reporting configuration ──────────────────────────────────────────────────

    /**
     * Whether per-feature-file Extent Reports are enabled.
     *
     * <p>When {@code true}, PTAF generates one individual Extent HTML report per feature file
     * in addition to the combined report. Each report is titled with the Feature Name
     * from the feature file's {@code Feature:} declaration.</p>
     *
     * <p>Config key: {@code reporting.per_feature_reports_enabled}</p>
     * <p>Default: {@code false} (only the combined report is generated).</p>
     *
     * @return {@code true} if per-feature reports are enabled, {@code false} otherwise
     */
    public static boolean isPerFeatureReportsEnabled() {
        String value = getValue("reporting.per_feature_reports_enabled");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * The output directory for per-feature Extent HTML reports.
     *
     * <p>Each report is saved as: {@code {output_dir}/{feature_file_name}_{timestamp}.html}.
     * The directory is created automatically if it does not exist.</p>
     *
     * <p>Config key: {@code reporting.per_feature_reports_output_dir}</p>
     * <p>Default: {@code "test-output/per-feature-reports"}</p>
     *
     * @return the configured output directory path
     */
    public static String getPerFeatureReportsOutputDir() {
        String value = getValue("reporting.per_feature_reports_output_dir");
        return (value != null && !value.trim().isEmpty()) ? value.trim() : "test-output/per-feature-reports";
    }

    /**
     * Whether a PDF version of each per-feature report should also be generated.
     *
     * <p>Only has effect when {@link #isPerFeatureReportsEnabled()} returns {@code true}.</p>
     *
     * <p>Config key: {@code reporting.per_feature_pdf_enabled}</p>
     * <p>Default: {@code false}</p>
     *
     * @return {@code true} if per-feature PDF reports are enabled, {@code false} otherwise
     */
    public static boolean isPerFeaturePdfEnabled() {
        String value = getValue("reporting.per_feature_pdf_enabled");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * Whether a Glass-style PDF (via cucumber-pdf-report subprocess) should be generated
     * for each per-feature report, written to a separate directory.
     *
     * <p>Only has effect when {@link #isPerFeatureReportsEnabled()} returns {@code true}.</p>
     *
     * <p>Config key: {@code reporting.per_feature_glass_pdf_enabled}</p>
     * <p>Default: {@code false}</p>
     *
     * @return {@code true} if per-feature Glass PDF reports are enabled, {@code false} otherwise
     */
    public static boolean isPerFeatureGlassPdfEnabled() {
        String value = getValue("reporting.per_feature_glass_pdf_enabled");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * The output directory for per-feature Glass-style PDF reports.
     *
     * <p>Config key: {@code reporting.per_feature_glass_pdf_output_dir}</p>
     * <p>Default: {@code "test-output/per-feature-reports-glass"}</p>
     *
     * @return the configured Glass PDF output directory path
     */
    public static String getPerFeatureGlassPdfOutputDir() {
        String value = getValue("reporting.per_feature_glass_pdf_output_dir");
        return (value != null && !value.trim().isEmpty()) ? value.trim() : "test-output/per-feature-reports-glass";
    }

    // ─── ZIP file configuration ──────────────────────────────────────────────────

    /**
     * The directory where ZIP file contents are extracted during test execution.
     *
     * <p>This directory is created automatically if it does not exist.
     * Each ZIP is extracted into a subdirectory named after the ZIP file (without extension).</p>
     *
     * <p>Config key: {@code zip.extraction_dir}</p>
     * <p>Default: {@code "test-output/extracted"}</p>
     *
     * @return the configured ZIP extraction directory path
     */
    public static String getZipExtractionDir() {
        String value = getValue("zip.extraction_dir");
        return (value != null && !value.trim().isEmpty()) ? value.trim() : "test-output/extracted";
    }

    /**
     * Whether extracted ZIP files should be automatically deleted at the end of each scenario.
     *
     * <p>When {@code true}, the extraction directory for the current scenario is deleted
     * in the {@code @After} hook of {@code ZipSteps} after each scenario completes.
     * When {@code false}, extracted files are kept for debugging and manual inspection.</p>
     *
     * <p>Config key: {@code zip.cleanup_after_scenario}</p>
     * <p>Default: {@code true}</p>
     *
     * @return {@code true} if auto-cleanup is enabled, {@code false} otherwise
     */
    public static boolean isZipCleanupAfterScenario() {
        String value = getValue("zip.cleanup_after_scenario");
        if (value == null || value.trim().isEmpty()) return true; // default: cleanup enabled
        return "true".equalsIgnoreCase(value.trim());
    }

    /**
     * Whether nested ZIP files found inside an extracted ZIP should also be extracted recursively.
     *
     * <p>When {@code true}, any {@code .zip} file found within the extracted contents
     * is automatically extracted into a subdirectory of the same name.
     * When {@code false}, nested ZIPs are left as-is.</p>
     *
     * <p>Config key: {@code zip.recursive_unzip}</p>
     * <p>Default: {@code true}</p>
     *
     * @return {@code true} if recursive unzip is enabled, {@code false} otherwise
     */
    public static boolean isZipRecursiveUnzip() {
        String value = getValue("zip.recursive_unzip");
        if (value == null || value.trim().isEmpty()) return true; // default: recursive enabled
        return "true".equalsIgnoreCase(value.trim());
    }

    // ─── Soft assertions configuration ────────────────────────────────────────────

    /**
     * Whether soft assertion mode is enabled.
     *
     * <p>When {@code true}, a failing step is retried for {@link #getSoftAssertionRetrySeconds()}
     * seconds before being recorded as a soft failure. Execution continues to the next step.
     * The scenario fails at the end if any soft failures were recorded.</p>
     *
     * <p>When {@code false} (default), normal fail-fast behavior applies: the first failure
     * stops the scenario immediately and closes the browser. This is the current behavior
     * and is unchanged unless this setting is explicitly set to {@code true}.</p>
     *
     * <p>Config key: {@code soft_assertions.enabled}</p>
     * <p>Default: {@code false}</p>
     *
     * @return {@code true} if soft assertion mode is enabled, {@code false} otherwise
     */
    public static boolean isSoftAssertionsEnabled() {
        String value = getValue("soft_assertions.enabled");
        return "true".equalsIgnoreCase(value);
    }

    /**
     * The number of seconds to retry a failed step before recording it as a soft failure.
     *
     * <p>Only used when {@link #isSoftAssertionsEnabled()} returns {@code true}.</p>
     *
     * <p>Keep this value low (1–10 seconds). If an element has not appeared within 3 seconds
     * it is almost certainly a real failure, not a timing issue.</p>
     *
     * <p>Config key: {@code soft_assertions.retry_seconds}</p>
     * <p>Default: {@code 3} seconds</p>
     *
     * @return the configured retry duration in seconds
     */
    public static int getSoftAssertionRetrySeconds() {
        String value = getValue("soft_assertions.retry_seconds");
        if (value == null || value.trim().isEmpty()) return 3;
        try {
            int seconds = Integer.parseInt(value.trim());
            return Math.max(1, Math.min(seconds, 60)); // clamp between 1 and 60 seconds
        } catch (NumberFormatException e) {
            logger.warn("Invalid value for soft_assertions.retry_seconds: [{}]. Defaulting to 3 seconds.", value);
            return 3;
        }
    }

    public static String getPropertyValue(String value) {
        return (String)PropertiesReader.get(value);
    }
}
