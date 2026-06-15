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

/** Isolated YAML reader for Playwright mobile-browser emulation profiles. */
public final class MobileBrowserYamlReader {
    private static final Map<String, Object> DATA = new LinkedHashMap<>();
    static { loadFolder("mobile_browser"); }
    private MobileBrowserYamlReader() { throw new IllegalStateException("Utility class"); }
    private static void loadFolder(String folderPath) {
        try {
            URL url = MobileBrowserYamlReader.class.getClassLoader().getResource(folderPath);
            if (url == null) return;
            Yaml yaml = new Yaml();
            try (Stream<Path> paths = Files.walk(Paths.get(url.toURI()))) {
                paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml")).forEach(p -> loadFile(yaml, p));
            }
        } catch (Exception e) { throw new IllegalStateException("Unable to load mobile-browser YAML resources", e); }
    }
    private static void loadFile(Yaml yaml, Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) { Map<String, Object> fileData = yaml.load(inputStream); if (fileData != null) merge(DATA, fileData); }
        catch (Exception e) { throw new IllegalStateException("Unable to load mobile-browser YAML file: " + path, e); }
    }
    @SuppressWarnings("unchecked") private static void merge(Map<String, Object> base, Map<String, Object> incoming) {
        for (Map.Entry<String, Object> e : incoming.entrySet()) { Object cur = base.get(e.getKey()); Object next = e.getValue(); if (cur instanceof Map && next instanceof Map) merge((Map<String, Object>) cur, (Map<String, Object>) next); else base.put(e.getKey(), next); }
    }
    @SuppressWarnings("unchecked") public static Object get(String key) {
        if (key == null || key.trim().isEmpty()) return null; String[] s = key.split("\\."); Map<String, Object> current = DATA;
        for (int i = 0; i < s.length - 1; i++) { Object next = current.get(s[i]); if (!(next instanceof Map)) return null; current = (Map<String, Object>) next; }
        return current.get(s[s.length - 1]);
    }
    @SuppressWarnings("unchecked") public static Map<String, Object> getMap(String key) { Object v = get(key); return v instanceof Map ? (Map<String, Object>) v : new LinkedHashMap<>(); }
}
