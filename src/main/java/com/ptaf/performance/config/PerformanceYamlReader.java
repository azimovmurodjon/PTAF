package com.ptaf.performance.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class PerformanceYamlReader {

    private static final String CONFIG_PATH = "performance/config/performance-config.yml";
    private static Map<String, Object> data;

    static {
        load();
    }

    @SuppressWarnings("unchecked")
    private static void load() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(CONFIG_PATH);

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Performance config file not found on classpath: " + CONFIG_PATH
                );
            }

            Yaml yaml = new Yaml();
            data = yaml.load(inputStream);

            if (data == null || data.isEmpty()) {
                throw new IllegalStateException(
                        "Performance config file is empty: " + CONFIG_PATH
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load performance config from classpath: " + CONFIG_PATH,
                    e
            );
        }
    }

    @SuppressWarnings("unchecked")
    public static Object get(String key) {
        String[] keys = key.split("\\.");
        Object current = data;

        for (String part : keys) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public static String getString(String key) {
        Object value = get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static int getInt(String key, int defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
    }

    public static long getLong(String key, long defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Long.parseLong(String.valueOf(value));
    }

    public static double getDouble(String key, double defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Double.parseDouble(String.valueOf(value));
    }
}