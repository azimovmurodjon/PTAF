package com.ptaf.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConfigurationProperties is a utility class that provides methods
 * for retrieving configuration properties from a YAML file.
 * It allows access to commonly used configurations, such as the base URL
 * and the browser type for the testing framework.
 */
public class ConfigurationProperties {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationProperties.class);

    /**
     * Retrieves the base URL from the YAML configuration using the specified key.
     *
     * @param URL The key for the base URL in the YAML file.
     * @return The base URL as a string. If the key is not found, it returns null.
     */
    public static String getBaseUrl(String URL) {
        // Retrieve and return the base URL from the YAML file using the provided key
        return (String) YamlReader.get(URL);
    }

    /**
     * Retrieves the browser type from the YAML configuration.
     *
     * @return The browser type as a string. If the key "browser" is not found, it returns null.
     */
    public static String getBrowser() {
        // Retrieve and return the browser type from the YAML file
        return (String) YamlReader.get("browser");
    }

    public static String getHeadlessMode() {
        // Retrieve and return the headless mode value from the YAML file
        return (String) YamlReader.get("headless");
    }

    public static String getYamlStoreLocation() {
        // Retrieve and return the headless mode value from the YAML file
        return (String) YamlReader.get("yamlStoreLocation");
    }

    public static String getExcelDocumentLocation() {
        // Retrieve and return the headless mode value from the YAML file
        return (String) YamlReader.get("excelDocumentLocation");
    }

    public static String getVideoCapture() {
        // Retrieve and return the headless mode value from the YAML file
        return (String) YamlReader.get("videoCapture");
    }

    public static String getValue(String value) {
        Object rawValue = YamlReader.get(value);
        // Retrieve and return the headless mode value from the YAML file
        return rawValue == null ? null : String.valueOf(rawValue);
    }

}