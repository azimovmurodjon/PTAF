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
    private static final Map<String, Object> DATA = new HashMap<>();

    static {
        loadFolder("mobile/config");
        loadFolder("mobile/elements");
    }

    private MobileYamlReader() {
        throw new IllegalStateException("Utility class");
    }

    private static void loadFolder(String folderPath) {
        try {
            URL resourceUrl = MobileYamlReader.class.getClassLoader().getResource(folderPath);
            if (resourceUrl == null) {
                return;
            }

            Yaml yaml = new Yaml();
            try (Stream<Path> paths = Files.walk(Paths.get(resourceUrl.toURI()))) {
                paths.filter(Files::isRegularFile)
                        .filter(MobileYamlReader::isFrameworkYamlFile)
                        .forEach(path -> loadFile(yaml, path));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load mobile YAML folder: " + folderPath, e);
        }
    }

    private static boolean isFrameworkYamlFile(Path path) {
        String normalized = path.toString().replace('\\', '/');
        return (normalized.endsWith(".yml") || normalized.endsWith(".yaml"))
                && (normalized.contains("/mobile/config/") || normalized.contains("/mobile/elements/"));
    }

    @SuppressWarnings("unchecked")
    private static void loadFile(Yaml yaml, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            Object loaded = yaml.load(inputStream);
            if (loaded == null) {
                return;
            }
            if (!(loaded instanceof Map)) {
                throw new IllegalStateException("Mobile YAML file must contain a map/object at root: " + path);
            }
            merge(DATA, (Map<String, Object>) loaded);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load mobile YAML file: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void merge(Map<String, Object> base, Map<String, Object> incoming) {
        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            Object current = base.get(entry.getKey());
            Object next = entry.getValue();
            if (current instanceof Map && next instanceof Map) {
                merge((Map<String, Object>) current, (Map<String, Object>) next);
            } else {
                base.put(entry.getKey(), next);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static Object get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        String[] segments = key.split("\\.");
        Map<String, Object> current = DATA;
        for (int i = 0; i < segments.length - 1; i++) {
            Object next = current.get(segments[i]);
            if (!(next instanceof Map)) {
                return null;
            }
            current = (Map<String, Object>) next;
        }
        return current.get(segments[segments.length - 1]);
    }

    public static String getString(String key, String defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
    }

    public static int getInt(String key, int defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
