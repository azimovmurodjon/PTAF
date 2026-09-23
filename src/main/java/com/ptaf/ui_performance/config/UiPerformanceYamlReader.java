package com.ptaf.ui_performance.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads only the dedicated UI performance YAML file.
 *
 * <p>This reader deliberately does not use the framework-wide {@code YamlReader}. Keeping a
 * private configuration store prevents UI performance settings from being merged with normal UI,
 * API, mobile, database, or JMeter performance configuration.</p>
 *
 * <p>The classpath resource can be overridden for controlled CI/CD execution with
 * {@code -Dui.performance.config=ui_performance/config/ui_performance-config.yml}. The override
 * must still resolve to a classpath resource so no secret-bearing local path is printed in reports.</p>
 */
public final class UiPerformanceYamlReader {
    public static final String DEFAULT_CONFIG_PATH = "ui_performance/config/ui_performance-config.yml";
    private static final Map<String, Object> DATA = load();

    private UiPerformanceYamlReader() {
        throw new IllegalStateException("Utility class");
    }

    /** Returns a configuration value addressed by a dot-separated key path, or null when absent. */
    @SuppressWarnings("unchecked")
    public static Object get(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        Object current = DATA;
        for (String segment : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /** Returns the configured value as a string, or the supplied default. */
    public static String getString(String key, String defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : String.valueOf(value).trim();
    }

    /** Returns the configured value as an integer, or the supplied default. */
    public static int getInt(String key, int defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("UI performance configuration value must be an integer: " + key, exception);
        }
    }

    /** Returns the configured value as a long, or the supplied default. */
    public static long getLong(String key, long defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("UI performance configuration value must be a whole number: " + key, exception);
        }
    }

    /** Returns the configured value as a double, or the supplied default. */
    public static double getDouble(String key, double defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("UI performance configuration value must be numeric: " + key, exception);
        }
    }

    /** Returns the configured boolean value, accepting only true or false. */
    public static boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        String normalized = String.valueOf(value).trim();
        if (!"true".equalsIgnoreCase(normalized) && !"false".equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("UI performance configuration value must be true or false: " + key);
        }
        return Boolean.parseBoolean(normalized);
    }

    /** Returns a configured YAML map or an immutable empty map when the key is absent. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(String key) {
        Object value = get(key);
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("UI performance configuration value must be a YAML map: " + key);
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>((Map<String, Object>) map));
    }

    /** Returns a configured YAML list or an immutable empty list when the key is absent. */
    public static List<?> getList(String key) {
        Object value = get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("UI performance configuration value must be a YAML list: " + key);
        }
        return List.copyOf(list);
    }

    /** Exposes an immutable copy for configuration diagnostics that never contains data-file values. */
    public static Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(DATA));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load() {
        String configPath = System.getProperty("ui.performance.config", DEFAULT_CONFIG_PATH).trim();
        if (configPath.isEmpty()) {
            configPath = DEFAULT_CONFIG_PATH;
        }
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configPath)) {
            if (stream == null) {
                throw new IllegalStateException("UI performance config was not found on the classpath: " + configPath);
            }
            Object parsed = new Yaml().load(stream);
            if (!(parsed instanceof Map<?, ?> parsedMap) || parsedMap.isEmpty()) {
                throw new IllegalStateException("UI performance config is empty or not a YAML map: " + configPath);
            }
            return new LinkedHashMap<>((Map<String, Object>) parsedMap);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load isolated UI performance configuration: " + configPath, exception);
        }
    }
}
