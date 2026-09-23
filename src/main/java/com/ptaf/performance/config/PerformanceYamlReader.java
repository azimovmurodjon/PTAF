package com.ptaf.performance.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * Utility for reading application performance configuration from a YAML file on the classpath.
 *
 * <p>Behavior summary:
 * - Loads a single YAML file located at {@code performance/config/performance-config.yml} from the classpath
 *   when this class is first referenced (static initialization).
 * - Keeps the parsed YAML content as a nested {@link Map} in memory for subsequent lookups.
 * - Provides simple retrieval methods that accept dot-separated keys (e.g. "database.pool.size") to walk the
 *   nested Map and return values.
 *
 * <p>Notes for testers:
 * - Ensure the YAML file exists on the test classpath at the exact path:
 *   {@code performance/config/performance-config.yml}. If the file is missing or empty, the application will
 *   fail early with an exception during static initialization.
 * - Keys are resolved using dot notation. If a path component does not resolve to a map (or the key is missing),
 *   {@code get(...)} will return {@code null}.
 * - Numeric helper methods ({@link #getInt}, {@link #getLong}, {@link #getDouble}) convert the resolved value to
 *   a string and then parse it. If the value cannot be parsed, a {@link NumberFormatException} will be thrown.
 *
 * <p>Thread-safety:
 * - The YAML is loaded once during static initialization and the underlying {@code data} reference is not modified
 *   afterwards. Concurrent read access via the provided getters is safe.
 */
public class PerformanceYamlReader {

    /**
     * Path to the performance configuration YAML file inside the classpath resources.
     * This is a relative path resolved by the context ClassLoader.
     */
    private static final String CONFIG_PATH = "performance/config/performance-config.yml";

    /**
     * Parsed representation of the YAML configuration. The top-level type is expected to be a {@link Map}.
     * The content is loaded once during static initialization.
     */
    private static Map<String, Object> data;

    /*
     * Static initializer to load the YAML configuration as soon as this class is referenced.
     * Any failure during loading will throw a runtime exception and fail fast.
     */
    static {
        load();
    }

    /**
     * Load and parse the YAML configuration from the classpath into the {@link #data} map.
     *
     * <p>Implementation details:
     * - Uses the current thread context ClassLoader to find the resource at {@link #CONFIG_PATH}.
     * - Uses SnakeYAML {@link Yaml} to parse the InputStream into a nested Map structure.
     * - Validates that the file exists and that the parsed content is not empty.
     *
     * @throws RuntimeException if any I/O or parsing error occurs, or if the file is missing/empty.
     */
    @SuppressWarnings("unchecked")
    private static void load() {
        try {
            // Obtain the class loader that should be able to locate resources on the application's classpath.
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // Open the configuration file as a resource stream. Returns null if not found.
            InputStream inputStream = classLoader.getResourceAsStream(CONFIG_PATH);

            // If the resource was not found, fail fast with a clear message to help testers diagnose classpath issues.
            if (inputStream == null) {
                throw new IllegalStateException(
                        "Performance config file not found on classpath: " + CONFIG_PATH
                );
            }

            // Parse the YAML file into a nested map structure. SnakeYAML produces raw maps/lists.
            Yaml yaml = new Yaml();
            data = yaml.load(inputStream);

            // If parsing produced no data (empty file or only comments), fail fast so callers don't operate on null data.
            if (data == null || data.isEmpty()) {
                throw new IllegalStateException(
                        "Performance config file is empty: " + CONFIG_PATH
                );
            }

        } catch (Exception e) {
            // Wrap any exception into a RuntimeException so initialization failures are explicit.
            throw new RuntimeException(
                    "Unable to load performance config from classpath: " + CONFIG_PATH,
                    e
            );
        }
    }

    /**
     * Retrieve a value from the loaded YAML configuration using a dot-separated key path.
     *
     * <p>Examples:
     * - "server.port" will try to get the "server" map and then its "port" entry.
     * - "database.connections.max" will walk three levels deep in the nested map structure.
     *
     * <p>Return semantics:
     * - Returns the raw {@link Object} stored at the key path, or {@code null} when:
     *   - any path segment is missing, or
     *   - a path segment expected to be a Map is not a Map.
     *
     * <p>Important:
     * - This method does not perform type conversions. Callers (or helper methods below) must cast or convert
     *   the returned object to the desired type.
     *
     * @param key dot-separated key path to the value (must not be null).
     * @return the value found at the key path, or {@code null} if not found or path cannot be traversed.
     */
    @SuppressWarnings("unchecked")
    public static Object get(String key) {
        // Split the requested key into path segments.
        String[] keys = key.split("\\.");

        // Start traversal from the top-level parsed data map.
        Object current = data;

        // Walk the nested maps according to the key segments.
        for (String part : keys) {
            // If current is not a Map at any point, the path cannot be continued -> return null.
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            // Look up the next segment in the current map.
            current = map.get(part);
            // If the value is missing, return null to indicate absence.
            if (current == null) {
                return null;
            }
        }
        // Return the final resolved object (could be Map, List, String, Number, etc.).
        return current;
    }

    /**
     * Convenience accessor that returns the configuration value as a {@link String}.
     *
     * @param key dot-separated key path to the value.
     * @return the String representation of the value, or {@code null} if the key is not present.
     */
    public static String getString(String key) {
        Object value = get(key);
        // If null, return null. Otherwise use String.valueOf to handle non-string values safely.
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Convenience accessor that returns the configuration value as an {@code int}.
     *
     * <p>Behavior:
     * - If the key is missing or resolves to {@code null}, {@code defaultValue} is returned.
     * - Otherwise the value is converted via {@code Integer.parseInt(String.valueOf(value))}.
     * - A {@link NumberFormatException} will propagate if the value cannot be parsed as an integer.
     *
     * @param key          dot-separated key path to the value.
     * @param defaultValue value to return when the key is absent.
     * @return the parsed int value or {@code defaultValue} if absent.
     */
    public static int getInt(String key, int defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
    }

    /**
     * Convenience accessor that returns the configuration value as a {@code long}.
     *
     * <p>Behavior:
     * - If the key is missing or resolves to {@code null}, {@code defaultValue} is returned.
     * - Otherwise the value is converted via {@code Long.parseLong(String.valueOf(value))}.
     * - A {@link NumberFormatException} will propagate if the value cannot be parsed as a long.
     *
     * @param key          dot-separated key path to the value.
     * @param defaultValue value to return when the key is absent.
     * @return the parsed long value or {@code defaultValue} if absent.
     */
    public static long getLong(String key, long defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Long.parseLong(String.valueOf(value));
    }

    /**
     * Convenience accessor that returns the configuration value as a {@code double}.
     *
     * <p>Behavior:
     * - If the key is missing or resolves to {@code null}, {@code defaultValue} is returned.
     * - Otherwise the value is converted via {@code Double.parseDouble(String.valueOf(value))}.
     * - A {@link NumberFormatException} will propagate if the value cannot be parsed as a double.
     *
     * @param key          dot-separated key path to the value.
     * @param defaultValue value to return when the key is absent.
     * @return the parsed double value or {@code defaultValue} if absent.
     */
    public static double getDouble(String key, double defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Double.parseDouble(String.valueOf(value));
    }
}
