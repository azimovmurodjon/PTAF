package com.ptaf.utils;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * YamlReader is a utility class designed for loading and managing configuration
 * data stored in YAML files. It now reads all YAML files from a list of specified folders,
 * merges the data into a single map, and provides methods for retrieving values
 * based on dot-separated keys.
 *
 * ONLY change from your version:
 * - Print "exact why it failed" when reading folders/files/YAML parsing fails
 * - NO new features, no behavior changes, same logic
 *
 * Usage notes for testers:
 * - All YAML resources are expected to be on the classpath under one of the configured
 *   folder names (elements, queries, api_requests, config, performance).
 * - Keys are retrieved using dot-separated paths, for example:
 *     Object value = YamlReader.get("api_requests.users.get_user_by_id");
 * - If a folder is missing from resources, the loader will skip it and print an INFO message.
 * - If reading a specific file or parsing YAML fails, detailed diagnostic information
 *   is printed to stderr so testers can quickly identify the problematic file or YAML content.
 */
public class YamlReader {
    // Static map to hold all the merged YAML data. This acts as a global, in-memory
    // configuration store loaded at class initialization time.
    private static final Map<String, Object> data = new HashMap<>();

    // Static initializer block executes once when the class is first accessed.
    // It scans configured resource folders for .yml/.yaml files and merges them into 'data'.
    static {
        // Define all resource folders you want to scan for .yml files.
        // These correspond to directories on the classpath (e.g. src/main/resources/elements)
        String[] folderPaths = {"elements", "queries", "api_requests", "config", "performance"};

        // SnakeYAML instance used to parse YAML content from input streams.
        Yaml yaml = new Yaml();

        // Loop through each specified folder path on the classpath.
        for (String folderPath : folderPaths) {
            try {
                // Attempt to locate the folder as a resource on the classpath.
                URL resourceUrl = YamlReader.class.getClassLoader().getResource(folderPath);
                if (resourceUrl == null) {
                    // If the folder resource is not present, this is not a hard error -
                    // it just means no files to load from this folder. Informational log.
                    System.out.println("INFO: Configuration folder not found in resources, skipping: " + folderPath);
                    continue; // Skip to the next folder
                }

                // Walk the file tree starting from the resource URL path. This will enumerate
                // all files within the given folder and subfolders (if any).
                try (Stream<Path> paths = Files.walk(Paths.get(resourceUrl.toURI()))) {
                    paths
                            // Only consider regular files (ignore directories)
                            .filter(Files::isRegularFile)
                            // Only process files that end with .yml or .yaml (case-sensitive per current logic)
                            .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                            // For each YAML file, attempt to open and parse it
                            .forEach(path -> {
                                try (InputStream inputStream = Files.newInputStream(path)) {
                                    // Parse the YAML content into a Map structure.
                                    // SnakeYAML returns java.util.Map for YAML mappings; null if file is empty.
                                    Map<String, Object> fileData = yaml.load(inputStream);
                                    if (fileData != null) {
                                        // Merge parsed data into the global 'data' map.
                                        mergeData(data, fileData);
                                    }
                                } catch (Exception e) {
                                    // EXACT WHY: show file + exception type + message (still prints stacktrace)
                                    // This block prints detailed diagnostics to stderr so testers and developers
                                    // can quickly identify the exact file and the exception that occurred
                                    // while reading or parsing it.
                                    System.err.println("\n========== YAML LOAD FAILURE (EXACT WHY) ==========");
                                    System.err.println("Folder   : " + folderPath);
                                    System.err.println("File     : " + path);
                                    System.err.println("Exception: " + e.getClass().getName());
                                    System.err.println("Message  : " + e.getMessage());
                                    System.err.println("==================================================\n");
                                    e.printStackTrace();
                                }
                            });
                }
            } catch (Exception e) {
                // EXACT WHY: show folder + resource url + exception details
                // This catch handles issues when attempting to access the folder itself,
                // for example URI conversion problems or IO errors walking the directory.
                System.err.println("\n========== YAML FOLDER ACCESS FAILURE (EXACT WHY) ==========");
                System.err.println("Folder   : " + folderPath);
                System.err.println("URL      : " + (YamlReader.class.getClassLoader().getResource(folderPath)));
                System.err.println("Exception: " + e.getClass().getName());
                System.err.println("Message  : " + e.getMessage());
                System.err.println("===========================================================\n");
                e.printStackTrace();
            }
        }
    }

    /**
     * Merges new data into the base map recursively. This allows for structured YAML files.
     *
     * Behavior details:
     * - When a key is present in both maps and both values are themselves maps, this method
     *   will merge their contents recursively so nested structures are preserved.
     * - When a key is present in the new data and is not a map (or base value is not a map),
     *   the new value overwrites the value in the base map.
     *
     * Notes for testers:
     * - This merge strategy means later-loaded files can override values from earlier-loaded files.
     * - There is an unchecked cast when values are assumed to be Map<String,Object> based on runtime
     *   YAML structure; this matches how SnakeYAML represents mappings.
     *
     * @param base    the map into which data will be merged (modified in place)
     * @param newData the map containing new values to merge
     */
    private static void mergeData(Map<String, Object> base, Map<String, Object> newData) {
        for (Map.Entry<String, Object> entry : newData.entrySet()) {
            if (base.containsKey(entry.getKey()) && base.get(entry.getKey()) instanceof Map && entry.getValue() instanceof Map) {
                // If both the base and new value are maps, merge them recursively
                mergeData((Map<String, Object>) base.get(entry.getKey()), (Map<String, Object>) entry.getValue());
            } else {
                // Otherwise, the new value overwrites the old one
                base.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Retrieves a value from the loaded YAML data based on a dot-separated key.
     * Example: "api_requests.users.get_user_by_id"
     *
     * ONLY change from your version:
     * - If a key path fails (wrong structure), print EXACT WHY (which segment failed)
     *
     * Retrieval semantics:
     * - The key is split on '.' and each segment is used to traverse nested Map<String,Object>
     *   structures that mirror YAML mappings.
     * - All segments except the last must resolve to a Map; the final segment's value is returned
     *   as-is (could be a scalar, list, or map).
     *
     * Error reporting:
     * - If at any point a segment is missing or the structure differs (e.g. a scalar encountered
     *   where a Map was expected), a detailed error is printed to stderr and null is returned.
     *
     * @param key The dot-separated key representing the path to the desired value.
     * @return The value associated with the key, or null if not found or if traversal fails.
     */
    public static Object get(String key) {
        try {
            // Split the incoming key into path components. Note: empty segments are possible if
            // the input contains consecutive dots; the current logic will treat an empty string
            // as a map key.
            String[] keys = key.split("\\.");
            Map<String, Object> currentMap = data;

            // Traverse the map using each part of the key except the final segment.
            // Each intermediate segment must resolve to a Map<String,Object>.
            for (int i = 0; i < keys.length - 1; i++) {
                Object value = currentMap.get(keys[i]);
                if (value instanceof Map) {
                    // Continue traversal into the nested map.
                    currentMap = (Map<String, Object>) value;
                } else {
                    // EXACT WHY: tell which segment is missing/wrong type
                    // Provide a detailed diagnostic so testers can see exactly where traversal failed.
                    System.err.println("\n========== YAML GET FAILURE (EXACT WHY) ==========");
                    System.err.println("Key       : " + key);
                    System.err.println("FailedAt  : " + keys[i]);
                    System.err.println("Reason    : " + (value == null
                            ? "Key segment not found in map"
                            : ("Expected Map but found: " + value.getClass().getName())));
                    System.err.println("=================================================\n");
                    return null;
                }
            }

            // Return the final value from the last key segment. This may be null if the key doesn't exist.
            return currentMap.get(keys[keys.length - 1]);

        } catch (Exception e) {
            // EXACT WHY: unexpected runtime issue
            // Catch-all to ensure that any unforeseen runtime exception during traversal is reported
            // with full diagnostic information to help debugging and testing.
            System.err.println("\n========== YAML GET FAILURE (EXACT WHY) ==========");
            System.err.println("Key       : " + key);
            System.err.println("Exception : " + e.getClass().getName());
            System.err.println("Message   : " + e.getMessage());
            System.err.println("=================================================\n");
            e.printStackTrace();
            return null;
        }
    }
}
