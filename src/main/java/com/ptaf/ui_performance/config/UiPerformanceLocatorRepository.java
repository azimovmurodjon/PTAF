package com.ptaf.ui_performance.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves regular-style locator definitions from the UI performance-only locator repository.
 *
 * <p>Values use the same {@code TYPE_value} convention as regular UI element YAML files, such as
 * {@code CSS_#username}, {@code Button_Sign in}, or {@code xpath_//button[@type='submit']}. The
 * repository remains separate, so UI performance journeys never change existing UI locator files.</p>
 */
public final class UiPerformanceLocatorRepository {
    private static final String RESOURCE = "ui_performance/locators/ui_performance-locators.yml";
    private static final Map<String, Map<String, String>> LOCATORS = load();

    private UiPerformanceLocatorRepository() {
        throw new IllegalStateException("Utility class");
    }

    /** Returns the configured TYPE_value locator definition for a separate group/key pair. */
    public static String getLocatorDefinition(String group, String key) {
        if (group == null || group.isBlank() || key == null || key.isBlank()) {
            throw new IllegalArgumentException("UI performance locator group and key cannot be blank.");
        }
        Map<String, String> groupValues = LOCATORS.get(group.trim());
        if (groupValues == null) {
            throw new IllegalArgumentException("UI performance locator group was not found: " + group);
        }
        String definition = groupValues.get(key.trim());
        if (definition == null || definition.isBlank()) {
            throw new IllegalArgumentException("UI performance locator was not found: " + group + "." + key);
        }
        return definition.trim();
    }

    /**
     * Legacy alias retained for the initial UI performance template. New code should use
     * {@link #getLocatorDefinition(String, String)} because locators are not limited to CSS.
     */
    public static String getCssSelector(String group, String key) {
        return getLocatorDefinition(group, key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, String>> load() {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("UI performance locator file is not available on the classpath: " + RESOURCE);
            }
            Object parsed = new Yaml().load(stream);
            if (!(parsed instanceof Map<?, ?> rawRoot)) {
                throw new IllegalStateException("UI performance locator file must contain a YAML map: " + RESOURCE);
            }
            Object rawGroups = rawRoot.get("ui_performance_locators");
            if (!(rawGroups instanceof Map<?, ?> groups)) {
                throw new IllegalStateException("UI performance locator file must contain ui_performance_locators: " + RESOURCE);
            }
            Map<String, Map<String, String>> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> group : groups.entrySet()) {
                if (!(group.getValue() instanceof Map<?, ?> rawValues)) {
                    throw new IllegalArgumentException("UI performance locator group must be a map: " + group.getKey());
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawValues.entrySet()) {
                    values.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
                result.put(String.valueOf(group.getKey()), Map.copyOf(values));
            }
            return Map.copyOf(result);
        } catch (Exception exception) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Unable to load UI performance locators: " + RESOURCE, exception);
        }
    }
}
