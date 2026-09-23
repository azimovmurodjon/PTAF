package com.ptaf.ui.mobilebrowser;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Isolated YAML reader for Playwright mobile-browser emulation profiles.
 *
 * <p>This utility class loads all YAML files found in the classpath folder "mobile_browser"
 * at class initialization time and merges them into a single in-memory map structure.
 * YAML contents are expected to be mappings (maps) which are merged recursively so that
 * nested maps from multiple files combine; non-map values from later files override earlier ones.
 *
 * <p>Access helpers are provided to fetch values via dot-separated keys (e.g. "iphone.viewport.width")
 * and to obtain nested maps safely.
 *
 * <p>Notes for testers:
 * - YAML files must be placed under the "mobile_browser" resource folder on the classpath.
 * - File names may end with either .yml or .yaml.
 * - If any error occurs while reading resources, an IllegalStateException is thrown during class initialization.
 */
public final class MobileBrowserYamlReader {
    /**
     * Central storage for all loaded YAML data. Uses LinkedHashMap to preserve insertion order,
     * which can be useful for deterministic behavior when merging multiple files.
     *
     * The map holds keys to either scalar values or nested Map<String, Object> structures for
     * YAML mappings. The structure mirrors the hierarchical keys accessible via the get(...) method.
     */
    private static final Map<String, Object> DATA = new LinkedHashMap<>();

    /*
     * Static initializer loads YAML files from the "mobile_browser" resource folder at class-load time.
     * Any failure here results in an IllegalStateException preventing normal use of the utility.
     */
    static { loadFolder("mobile_browser"); }

    /**
     * Private constructor to prevent instantiation.
     *
     * This class is a pure utility holder for static methods and state; instantiation is not meaningful.
     * Attempting to instantiate will result in IllegalStateException.
     */
    private MobileBrowserYamlReader() { throw new IllegalStateException("Utility class"); }

    /**
     * Load all YAML files from the given folder path on the classpath and merge them into {@link #DATA}.
     *
     * <p>Behavior:
     * - Locates the folder using the class loader.
     * - Walks the folder recursively and filters regular files that end with .yml or .yaml.
     * - Each matching file is loaded and merged into the shared DATA map.
     *
     * @param folderPath the folder path relative to the classpath to search for YAML files (e.g. "mobile_browser")
     * @throws IllegalStateException if the folder cannot be read or any IO/URI issue occurs during loading
     */
    private static void loadFolder(String folderPath) {
        try {
            // Resolve the resource URL for the requested folder on the classpath.
            URL url = MobileBrowserYamlReader.class.getClassLoader().getResource(folderPath);
            // If the resource does not exist on the classpath, there's nothing to load; return silently.
            if (url == null) return;

            // SnakeYAML instance used to parse YAML input streams into Java Map structures.
            Yaml yaml = new Yaml();

            // Walk the directory tree rooted at the resource URL and process each regular YAML file.
            try (Stream<Path> paths = Files.walk(Paths.get(url.toURI()))) {
                // Filter to regular files and those that end with .yml or .yaml, then load each file.
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                        .forEach(p -> loadFile(yaml, p));
            }
        } catch (Exception e) {
            // Wrap and rethrow to signal a configuration/resource loading problem early.
            throw new IllegalStateException("Unable to load mobile-browser YAML resources", e);
        }
    }

    /**
     * Load a single YAML file and merge its contents into the provided global DATA map.
     *
     * <p>Each file is opened as a stream and parsed by SnakeYAML. The resulting object is expected
     * to be a Map&lt;String, Object&gt; (YAML mapping). If the file yields null (empty file),
     * nothing is merged.
     *
     * @param yaml YAML parser instance to reuse for parsing the file
     * @param path filesystem path to the YAML file to load
     * @throws IllegalStateException if the file cannot be opened or parsed
     */
    private static void loadFile(Yaml yaml, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            // Parse the YAML file into a Map structure. SnakeYAML returns null for empty files.
            Map<String, Object> fileData = yaml.load(inputStream);
            // If parsed data is present, merge it into the global DATA map.
            if (fileData != null) merge(DATA, fileData);
        } catch (Exception e) {
            // Include the file path in the error message to aid debugging / tester diagnostics.
            throw new IllegalStateException("Unable to load mobile-browser YAML file: " + path, e);
        }
    }

    /**
     * Recursively merge entries from the incoming map into the base map.
     *
     * <p>Merge semantics:
     * - If both the existing value in base and the incoming value are maps, merge them recursively.
     * - Otherwise, the incoming value replaces the value in base.
     *
     * <p>This method mutates the base map in-place.
     *
     * @param base the map to merge into (will be modified)
     * @param incoming the map whose entries should be merged into base
     */
    @SuppressWarnings("unchecked") private static void merge(Map<String, Object> base, Map<String, Object> incoming) {
        for (Map.Entry<String, Object> e : incoming.entrySet()) {
            Object cur = base.get(e.getKey());
            Object next = e.getValue();
            // If both current and incoming values are maps, merge recursively to preserve nested keys.
            if (cur instanceof Map && next instanceof Map) merge((Map<String, Object>) cur, (Map<String, Object>) next);
            else base.put(e.getKey(), next); // Otherwise, replace or insert the value.
        }
    }

    /**
     * Retrieve an arbitrary object from the merged YAML data using a dot-separated key.
     *
     * <p>Example:
     * - Given YAML structure { "iphone": { "viewport": { "width": 375 } } }
     * - get("iphone.viewport.width") returns the Integer value 375.
     *
     * <p>Behavior:
     * - If the key argument is null or empty, returns null.
     * - If any intermediate path element does not exist or is not a map, returns null.
     *
     * @param key dot-separated path to the desired value (e.g. "device.name" or "iphone.viewport")
     * @return the object stored at the path, or null if not found or invalid
     */
    @SuppressWarnings("unchecked") public static Object get(String key) {
        // Guard against bad input.
        if (key == null || key.trim().isEmpty()) return null;

        // Split path into segments on dots; backslash-escaped dots are not handled.
        String[] s = key.split("\\.");
        Map<String, Object> current = DATA;

        // Traverse all but the last segment, ensuring each segment resolves to a nested map.
        for (int i = 0; i < s.length - 1; i++) {
            Object next = current.get(s[i]);
            // If the next element is not a map, the requested dotted path is invalid; return null.
            if (!(next instanceof Map)) return null;
            current = (Map<String, Object>) next;
        }

        // Return the final value (may be a scalar or a nested map).
        return current.get(s[s.length - 1]);
    }

    /**
     * Convenience helper to fetch a nested map from the YAML data.
     *
     * <p>Returns an empty LinkedHashMap if the key does not exist or does not point to a map,
     * which avoids null checks for callers (useful for tests).
     *
     * @param key dot-separated path expected to resolve to a map
     * @return the map located at the given key, or an empty map if missing / not a map
     */
    @SuppressWarnings("unchecked") public static Map<String, Object> getMap(String key) {
        Object v = get(key);
        return v instanceof Map ? (Map<String, Object>) v : new LinkedHashMap<>();
    }
}
