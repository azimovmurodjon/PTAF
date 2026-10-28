package com.ptaf.copilot;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Read-only verifier for the standard consumer URL-alias configuration.
 *
 * <p>The verifier confirms that the candidate refers to an existing standard configuration alias
 * (including the normal {@code environments.<env>.<alias>} override) with an HTTP or HTTPS value.
 * It intentionally never returns, logs, renders, hashes, or writes
 * the resolved URL. This lets the generated feature use the existing
 * {@code Given we navigate to <alias> url} contract without duplicating an endpoint.</p>
 */
public final class CopilotDiscoveryTargetConfigurationVerifier {
    /** Standard location expected by the normal FNB-ETAF navigation step. */
    public static final Path STANDARD_CONFIGURATION_PATH = Path.of("src/test/resources/config/config.yml");

    private CopilotDiscoveryTargetConfigurationVerifier() {
        // Utility class.
    }

    /** Confirms the target alias exists in the standard consumer configuration without disclosing it. */
    public static void verify(Path consumerRoot, String targetAlias) {
        if (consumerRoot == null || targetAlias == null || targetAlias.isBlank()) {
            throw new CopilotProfileException("Consumer root and target alias are required for configuration verification.");
        }
        Path root = consumerRoot.toAbsolutePath().normalize();
        Path configPath = root.resolve(STANDARD_CONFIGURATION_PATH).normalize();
        if (!configPath.startsWith(root) || !Files.isRegularFile(configPath)) {
            throw new CopilotProfileException("The standard FNB-ETAF configuration file was not found for target alias verification.");
        }
        try (InputStream input = Files.newInputStream(configPath)) {
            Object parsed = safeYaml().load(input);
            if (!(parsed instanceof Map<?, ?> values)) {
                throw new CopilotProfileException("The standard FNB-ETAF configuration is not a YAML mapping.");
            }
            String environment = System.getProperty("env", "QA");
            Object rawTarget = resolve(asMap(values.get("environments")), environment + "." + targetAlias);
            if (rawTarget == null) {
                rawTarget = resolve(asMap(values), targetAlias);
            }
            if (!(rawTarget instanceof String target) || target.isBlank()) {
                throw new CopilotProfileException("Target alias '" + targetAlias
                        + "' is not a non-blank entry in the standard FNB-ETAF configuration.");
            }
            URI uri = URI.create(target.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new CopilotProfileException("Target alias '" + targetAlias
                        + "' does not resolve to a valid HTTP or HTTPS address in standard configuration.");
            }
        } catch (CopilotProfileException exception) {
            throw exception;
        } catch (IllegalArgumentException exception) {
            throw new CopilotProfileException("Target alias '" + targetAlias
                    + "' does not resolve to a valid HTTP or HTTPS address in standard configuration.");
        } catch (IOException exception) {
            throw new CopilotProfileException("Could not read standard FNB-ETAF configuration for target alias verification.", exception);
        }
    }

    private static Yaml safeYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(10);
        options.setCodePointLimit(250_000);
        return new Yaml(new SafeConstructor(options));
    }

    private static Map<?, ?> asMap(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    /** Resolves a direct key first, then the same dot-separated nested key semantics used by YAML accessors. */
    private static Object resolve(Map<?, ?> values, String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }
        Object current = values;
        for (String segment : key.split("\\.")) {
            if (!(current instanceof Map<?, ?> map) || !map.containsKey(segment)) {
                return null;
            }
            current = map.get(segment);
        }
        return current;
    }
}
