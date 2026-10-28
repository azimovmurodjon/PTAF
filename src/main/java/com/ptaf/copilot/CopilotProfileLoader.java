package com.ptaf.copilot;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads the FNB-ETAF Copilot profile as one explicit classpath resource.
 *
 * <p>This loader deliberately does not use the framework-wide {@code YamlReader}. The global
 * reader scans test runtime folders and is not suitable for a resource packaged in a dependency JAR.
 * This class performs no directory walking and never changes test configuration.</p>
 */
public final class CopilotProfileLoader {
    /** The one editable, dependency-packaged FNB-ETAF guidance and local-discovery policy source. */
    public static final String DEFAULT_PROFILE_RESOURCE = "copilot/fnb-etaf-copilot-profile.yml";

    private CopilotProfileLoader() {
        // Utility class.
    }

    /** Loads the canonical profile packaged in FNB-ETAF. */
    public static CopilotProfile loadEmbeddedProfile() {
        ClassLoader loader = CopilotProfileLoader.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(DEFAULT_PROFILE_RESOURCE)) {
            if (input == null) {
                throw new CopilotProfileException("FNB-ETAF Copilot profile was not found on the classpath: "
                        + DEFAULT_PROFILE_RESOURCE);
            }
            return parse(input, "classpath:" + DEFAULT_PROFILE_RESOURCE);
        } catch (IOException exception) {
            throw new CopilotProfileException("Could not close the FNB-ETAF Copilot profile resource.", exception);
        }
    }

    /**
     * Loads a supplied profile file for local contract tests and controlled tooling use.
     * Normal FNB-ETAF consumers should use {@link #loadEmbeddedProfile()}.
     */
    public static CopilotProfile load(Path profilePath) {
        if (profilePath == null || !Files.isRegularFile(profilePath)) {
            throw new CopilotProfileException("Copilot profile file does not exist: " + profilePath);
        }
        try (InputStream input = Files.newInputStream(profilePath)) {
            return parse(input, profilePath.toAbsolutePath().normalize().toString());
        } catch (IOException exception) {
            throw new CopilotProfileException("Could not read Copilot profile: " + profilePath, exception);
        }
    }

    private static CopilotProfile parse(InputStream input, String source) {
        try {
            Object loaded = safeYaml().load(input);
            if (!(loaded instanceof Map<?, ?> rawRoot)) {
                throw new CopilotProfileException("Copilot profile must contain a YAML mapping: " + source);
            }

            Map<String, Object> root = toStringKeyMap(rawRoot, "profile root", source);
            Set<String> supportedKeys = Set.of("version", "repository_instruction", "path_instructions", "discovery_policy");
            rejectUnknownKeys(root, supportedKeys, "profile root", source);
            String version = requiredString(root, "version", source);
            String repositoryInstruction = requiredString(root, "repository_instruction", source);
            Object rawInstructions = root.get("path_instructions");
            if (!(rawInstructions instanceof Map<?, ?> rawInstructionMap)) {
                throw new CopilotProfileException("Copilot profile key 'path_instructions' must be a mapping: " + source);
            }

            Map<String, Object> instructionMap = toStringKeyMap(rawInstructionMap, "path_instructions", source);
            Map<String, String> instructions = new LinkedHashMap<>();
            instructionMap.forEach((path, content) -> {
                if (!(content instanceof String text) || text.isBlank()) {
                    throw new CopilotProfileException("Instruction content must be a non-blank string for " + path
                            + " in " + source + ".");
                }
                instructions.put(path, normalizeLineEndings(text));
            });

            CopilotDiscoveryPolicy discoveryPolicy = null;
            if (root.containsKey("discovery_policy")) {
                discoveryPolicy = parseDiscoveryPolicy(root.get("discovery_policy"), source);
            }
            CopilotProfile profile = new CopilotProfile(version, repositoryInstruction, instructions, discoveryPolicy);
            CopilotProfileValidator.validate(profile);
            return profile;
        } catch (CopilotProfileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CopilotProfileException("Could not parse FNB-ETAF Copilot profile " + source + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static CopilotDiscoveryPolicy parseDiscoveryPolicy(Object rawPolicy, String source) {
        if (!(rawPolicy instanceof Map<?, ?> rawMap)) {
            throw new CopilotProfileException("Copilot profile key 'discovery_policy' must be a mapping: " + source);
        }
        Map<String, Object> policy = toStringKeyMap(rawMap, "discovery_policy", source);
        Set<String> requiredKeys = Set.of(
                "draft_output_directory", "mcp_template_output_path", "mcp_server_name", "playwright_mcp_package",
                "default_browser", "default_headless", "idle_timeout_millis", "console_level",
                "allowed_locator_types", "allowed_actions", "maximum_candidates");
        if (!policy.keySet().equals(requiredKeys)) {
            Set<String> missing = new LinkedHashSet<>(requiredKeys);
            missing.removeAll(policy.keySet());
            Set<String> unsupported = new LinkedHashSet<>(policy.keySet());
            unsupported.removeAll(requiredKeys);
            throw new CopilotProfileException("Invalid discovery_policy keys in " + source
                    + "; missing=" + missing + ", unsupported=" + unsupported + ".");
        }

        return new CopilotDiscoveryPolicy(
                requiredString(policy, "draft_output_directory", source),
                requiredString(policy, "mcp_template_output_path", source),
                requiredString(policy, "mcp_server_name", source),
                requiredString(policy, "playwright_mcp_package", source),
                requiredString(policy, "default_browser", source),
                requiredBoolean(policy, "default_headless", source),
                requiredLong(policy, "idle_timeout_millis", source),
                requiredString(policy, "console_level", source),
                requiredStringList(policy, "allowed_locator_types", source),
                requiredStringList(policy, "allowed_actions", source),
                requiredInt(policy, "maximum_candidates", source)
        );
    }

    private static Map<String, Object> toStringKeyMap(Map<?, ?> input, String label, String source) {
        Map<String, Object> result = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (!(key instanceof String stringKey) || stringKey.isBlank()) {
                throw new CopilotProfileException("All " + label + " keys must be non-blank strings in " + source + ".");
            }
            if (result.put(stringKey, value) != null) {
                throw new CopilotProfileException("Duplicate " + label + " key '" + stringKey + "' in " + source + ".");
            }
        });
        return result;
    }

    private static void rejectUnknownKeys(Map<String, Object> values, Set<String> supportedKeys, String label, String source) {
        values.keySet().stream()
                .filter(key -> !supportedKeys.contains(key))
                .findFirst()
                .ifPresent(key -> {
                    throw new CopilotProfileException("Unsupported Copilot profile key '" + key
                            + "' in " + label + " at " + source + ". Supported keys: " + supportedKeys + ".");
                });
    }

    private static String requiredString(Map<String, Object> values, String key, String source) {
        Object value = values.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new CopilotProfileException("Copilot profile key '" + key + "' must be a non-blank string: " + source);
        }
        return normalizeLineEndings(text);
    }

    private static boolean requiredBoolean(Map<String, Object> values, String key, String source) {
        Object value = values.get(key);
        if (!(value instanceof Boolean booleanValue)) {
            throw new CopilotProfileException("Copilot profile key '" + key + "' must be a boolean: " + source);
        }
        return booleanValue;
    }

    private static long requiredLong(Map<String, Object> values, String key, String source) {
        Object value = values.get(key);
        if (!(value instanceof Number number)) {
            throw new CopilotProfileException("Copilot profile key '" + key + "' must be a number: " + source);
        }
        return number.longValue();
    }

    private static int requiredInt(Map<String, Object> values, String key, String source) {
        long value = requiredLong(values, key, source);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new CopilotProfileException("Copilot profile key '" + key + "' is outside the supported integer range: " + source);
        }
        return (int) value;
    }

    private static List<String> requiredStringList(Map<String, Object> values, String key, String source) {
        Object value = values.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw new CopilotProfileException("Copilot profile key '" + key + "' must be a non-empty list: " + source);
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text) || text.isBlank()) {
                throw new CopilotProfileException("Copilot profile list '" + key + "' must contain only non-blank strings: " + source);
            }
            result.add(text.trim());
        }
        return List.copyOf(result);
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static Yaml safeYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(10);
        options.setCodePointLimit(500_000);
        return new Yaml(new SafeConstructor(options));
    }
}
