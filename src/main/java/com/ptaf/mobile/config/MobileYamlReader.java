package com.ptaf.mobile.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Enterprise-safe YAML reader for PTAF native mobile automation resources.
 *
 * <p>This reader intentionally loads only framework-owned YAML files from:</p>
 * <ul>
 *     <li>{@code src/test/resources/mobile/config}</li>
 *     <li>{@code src/test/resources/mobile/elements}</li>
 * </ul>
 *
 * <p>It must not recursively load all files under {@code mobile}, because real mobile
 * app artifacts such as {@code .app} bundles, SDK bundles, camera SDKs, Kofax bundles,
 * or vendor resources may also contain internal {@code .yml/.yaml} files that are not
 * PTAF configuration files. Parsing those files can break framework startup before
 * Appium even creates a session.</p>
 */
public final class MobileYamlReader {
    /**
     * In-memory representation of loaded YAML data.
     *
     * <p>All YAML files loaded by this class are merged into this single nested map.
     * Keys correspond to top-level YAML keys and values may be nested maps or simple
     * scalar values depending on the YAML content.</p>
     *
     * <p>This map is static and populated at class load time via the static initializer
     * so that callers can read configuration values without explicit initialization.</p>
     */
    private static final Map<String, Object> DATA = new HashMap<>();

    /*
     * Static initializer: eagerly load YAML files from the two allowed framework
     * resource folders. This ensures configuration is available when the class is
     * first referenced and prevents lazy loading race conditions in tests.
     */
    static {
        loadFolder("mobile/config");
        loadFolder("mobile/elements");
    }

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class only exposes static helper methods and is intended to be used
     * as a utility. Instantiation would be incorrect usage so we throw an exception
     * if the constructor is invoked reflectively.</p>
     */
    private MobileYamlReader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Load all framework YAML files from the given resource folder path.
     *
     * <p>The method locates the folder on the classpath, walks the files under the
     * directory (non-recursive filtering is applied later by {@link #isFrameworkYamlFile}),
     * parses each YAML file and merges its contents into the global {@link #DATA} map.</p>
     *
     * @param folderPath the resource folder relative to the classpath (for example "mobile/config")
     * @throws IllegalStateException if any IO or parsing error occurs while loading the folder
     */
    private static void loadFolder(String folderPath) {
        try {
            // Find the folder in the classpath using the class loader.
            URL resourceUrl = MobileYamlReader.class.getClassLoader().getResource(folderPath);
            if (resourceUrl == null) {
                // If the resource folder does not exist on the classpath, nothing to load.
                return;
            }

            Yaml yaml = new Yaml();
            // Walk the directory tree starting at the resource path. Using try-with-resources
            // to ensure the stream of paths is closed.
            try (Stream<Path> paths = Files.walk(Paths.get(resourceUrl.toURI()))) {
                paths.filter(Files::isRegularFile) // only regular files, skip directories
                        .filter(MobileYamlReader::isFrameworkYamlFile) // only allowed YAML files
                        .forEach(path -> loadFile(yaml, path)); // parse and merge each file
            }
        } catch (Exception e) {
            // Wrap any exception with a descriptive message to help debugging initialization failures.
            throw new IllegalStateException("Unable to load mobile YAML folder: " + folderPath, e);
        }
    }

    /**
     * Determine whether a given file path is a framework-owned YAML file that should be loaded.
     *
     * <p>This method enforces two checks:
     * <ol>
     *     <li>The filename ends with {@code .yml} or {@code .yaml}.</li>
     *     <li>The file path contains either {@code /mobile/config/} or {@code /mobile/elements/}.</li>
     * </ol>
     *
     * <p>The normalization step replaces Windows backslashes with forward slashes so the checks
     * work consistently across platforms.</p>
     *
     * @param path file system path to check
     * @return true if this path represents a YAML file that belongs to the framework locations
     */
    private static boolean isFrameworkYamlFile(Path path) {
        // Normalize separators to '/' for consistent matching on all OSes.
        String normalized = path.toString().replace('\\', '/');
        return (normalized.endsWith(".yml") || normalized.endsWith(".yaml"))
                && (normalized.contains("/mobile/config/") || normalized.contains("/mobile/elements/"));
    }

    /**
     * Load a single YAML file and merge its contents into the provided YAML data map.
     *
     * <p>The method expects the YAML file to contain a mapping (YAML object) at the root.
     * If the parsed content is null (empty file) the method returns silently. If the root
     * value is not a map an exception is thrown because the framework expects object-mapped
     * YAML documents.</p>
     *
     * @param yaml YAML parser instance to use
     * @param path file system path to the YAML file to load
     * @throws IllegalStateException if the file cannot be read or parsed, or if the root is not a map
     */
    @SuppressWarnings("unchecked")
    private static void loadFile(Yaml yaml, Path path) {
        // Use try-with-resources to ensure the InputStream is closed after parsing.
        try (InputStream inputStream = Files.newInputStream(path)) {
            Object loaded = yaml.load(inputStream);
            if (loaded == null) {
                // Empty YAML file - nothing to merge.
                return;
            }
            if (!(loaded instanceof Map)) {
                // Enforce that top-level YAML documents are mappings.
                throw new IllegalStateException("Mobile YAML file must contain a map/object at root: " + path);
            }
            // Merge parsed map into the global DATA map. Suppress unchecked cast warning
            // because SnakeYAML returns raw types.
            merge(DATA, (Map<String, Object>) loaded);
        } catch (Exception e) {
            // Wrap exceptions with contextual file path information to aid debugging.
            throw new IllegalStateException("Unable to load mobile YAML file: " + path, e);
        }
    }

    /**
     * Deep merge the incoming map into the base map.
     *
     * <p>For keys that exist in both maps:
     * <ul>
     *     <li>If both values are maps they are merged recursively.</li>
     *     <li>Otherwise the incoming value replaces the existing one.</li>
     * </ul>
     *
     * <p>This merge behaviour allows multiple YAML files to contribute partial objects
     * under the same top-level key without losing nested entries.</p>
     *
     * @param base the destination map that will be modified
     * @param incoming the map with new values to merge into base
     */
    @SuppressWarnings("unchecked")
    private static void merge(Map<String, Object> base, Map<String, Object> incoming) {
        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            Object current = base.get(entry.getKey());
            Object next = entry.getValue();
            if (current instanceof Map && next instanceof Map) {
                // Both sides are maps - recurse to merge nested structures.
                merge((Map<String, Object>) current, (Map<String, Object>) next);
            } else {
                // Otherwise replace or insert the key with the incoming value.
                base.put(entry.getKey(), next);
            }
        }
    }

    /**
     * Retrieve a value from the merged YAML configuration using dot-separated keys.
     *
     * <p>Example: get("app.settings.timeout") will navigate DATA.get("app") -> get("settings") -> get("timeout").</p>
     *
     * <p>Returns {@code null} if:
     * <ul>
     *     <li>The provided key is null or empty.</li>
     *     <li>Any segment along the path is missing or not a mapping when a mapping is required.</li>
     *     <li>The final value does not exist.</li>
     * </ul>
     *
     * @param key dot-separated path to the desired configuration value
     * @return the value object found at the specified path or null if not present
     */
    @SuppressWarnings("unchecked")
    public static Object get(String key) {
        if (key == null || key.trim().isEmpty()) {
            // Defensive: invalid keys return null rather than throwing.
            return null;
        }
        String[] segments = key.split("\\.");
        Map<String, Object> current = DATA;
        // Traverse all but the last segment; the loop navigates nested maps.
        for (int i = 0; i < segments.length - 1; i++) {
            Object next = current.get(segments[i]);
            if (!(next instanceof Map)) {
                // If the expected nested map does not exist, return null.
                return null;
            }
            current = (Map<String, Object>) next;
        }
        // Return the final segment's value (might be a Map, List, String, Number, etc.)
        return current.get(segments[segments.length - 1]);
    }

    /**
     * Convenience accessor that returns a String representation of the value at the key.
     *
     * <p>If the key is missing this method returns the provided defaultValue. Non-null
     * values are converted using {@link String#valueOf(Object)}.</p>
     *
     * @param key the dot-separated configuration key
     * @param defaultValue value to return when the configuration value is missing
     * @return the string value found at key or defaultValue when not present
     */
    public static String getString(String key, String defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    /**
     * Convenience accessor that returns a boolean value for the given key.
     *
     * <p>The method converts the underlying value to a string and uses {@link Boolean#parseBoolean}
     * to interpret it. If the key is not present, the provided defaultValue is returned.</p>
     *
     * @param key the dot-separated configuration key
     * @param defaultValue default boolean to return when the configuration value is missing
     * @return parsed boolean value or defaultValue when not present
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Convenience accessor that returns an int value for the given key.
     *
     * <p>The method attempts to parse the underlying value as an integer. If parsing fails
     * or the key is not present the provided defaultValue is returned. This prevents the
     * method from throwing NumberFormatException to callers.</p>
     *
     * @param key the dot-separated configuration key
     * @param defaultValue default int to return when the configuration value is missing or invalid
     * @return parsed int value or defaultValue when not present or unparsable
     */
    public static int getInt(String key, int defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            // Return default on parse failure to keep consumer code simple and robust.
            return defaultValue;
        }
    }
}
