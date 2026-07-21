package com.ptaf.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * PropertiesReader is a utility class designed for loading and managing configuration
 * data stored in .properties files. It reads all .properties files from a list of specified folders,
 * merges the data into a single map, and provides methods for retrieving values
 * based on dot-separated keys.
 */
public class PropertiesReader {
    private static final Map<String, Object> data = new HashMap<>();

    static {
        String[] folderPaths = {"src/test/resources"};

        for (String folderPath : folderPaths) {
            try {
                URL resourceUrl = PropertiesReader.class.getClassLoader().getResource(folderPath);
                if (resourceUrl == null) {
                    System.out.println("INFO: Configuration folder not found in resources, skipping: " + folderPath);
                    continue;
                }

                try (Stream<Path> paths = Files.walk(Paths.get(resourceUrl.toURI()))) {
                    paths
                            .filter(Files::isRegularFile)
                            .filter(path -> path.toString().endsWith(".properties"))
                            .forEach(path -> {
                                try (InputStream inputStream = Files.newInputStream(path)) {
                                    Properties props = new Properties();
                                    props.load(inputStream);

                                    for (String key : props.stringPropertyNames()) {
                                        String value = props.getProperty(key);
                                        if (value != null) {
                                            data.put(key.trim(), value.trim()); // Ensure clean key-value pairs
                                        }
                                    }
                                } catch (IOException e) {
                                    System.err.println("Error reading properties file: " + path);
                                    e.printStackTrace();
                                }
                            });
                }
            } catch (IOException | URISyntaxException e) {
                System.err.println("Error accessing resource folder: " + folderPath);
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves a value from the loaded properties data based on a dot-separated key.
     *
     * @param key The dot-separated key representing the path to the desired value.
     * @return The value associated with the key, or null if not found.
     */
    public static Object get(String key) {
        return data.get(key);
    }
}
