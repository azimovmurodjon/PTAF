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
 */
public class YamlReader {
    // Static map to hold all the merged YAML data
    private static final Map<String, Object> data = new HashMap<>();

    // Static block to load YAML files during class initialization
    static {
        // Define all resource folders you want to scan for .yml files.
        String[] folderPaths = {"elements", "queries", "api_requests", "config", "performance"};

        Yaml yaml = new Yaml();

        // Loop through each specified folder path.
        for (String folderPath : folderPaths) {
            try {
                URL resourceUrl = YamlReader.class.getClassLoader().getResource(folderPath);
                if (resourceUrl == null) {
                    // This is not an error, it just means a folder might not exist yet.
                    System.out.println("INFO: Configuration folder not found in resources, skipping: " + folderPath);
                    continue; // Skip to the next folder
                }

                try (Stream<Path> paths = Files.walk(Paths.get(resourceUrl.toURI()))) {
                    paths
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                            .forEach(path -> {
                                try (InputStream inputStream = Files.newInputStream(path)) {
                                    Map<String, Object> fileData = yaml.load(inputStream);
                                    if (fileData != null) {
                                        mergeData(data, fileData);
                                    }
                                } catch (Exception e) {
                                    // EXACT WHY: show file + exception type + message (still prints stacktrace)
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
     * @param key The dot-separated key representing the path to the desired value.
     * @return The value associated with the key, or null if not found.
     */
    public static Object get(String key) {
        try {
            String[] keys = key.split("\\.");
            Map<String, Object> currentMap = data;

            // Traverse the map using each part of the key
            for (int i = 0; i < keys.length - 1; i++) {
                Object value = currentMap.get(keys[i]);
                if (value instanceof Map) {
                    currentMap = (Map<String, Object>) value;
                } else {
                    // EXACT WHY: tell which segment is missing/wrong type
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

            // Return the final value from the last key segment
            return currentMap.get(keys[keys.length - 1]);

        } catch (Exception e) {
            // EXACT WHY: unexpected runtime issue
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