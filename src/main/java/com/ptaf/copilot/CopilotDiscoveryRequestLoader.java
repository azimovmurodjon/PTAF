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
 * Loads a local, structured discovery-candidate file supplied by a human or Copilot Chat.
 *
 * <p>Only the fixed schema documented in the central profile is accepted. Unknown fields,
 * embedded input values, URLs, browser state, and arbitrary YAML structures are rejected later by
 * {@link CopilotDiscoveryValidator}. Keeping the loader strict makes it impossible for the draft
 * tool to silently accept a new command or data field.</p>
 */
public final class CopilotDiscoveryRequestLoader {
    /** Root YAML key for all local discovery requests. */
    public static final String ROOT_KEY = "discovery_draft";

    private CopilotDiscoveryRequestLoader() {
        // Utility class.
    }

    /** Loads one request file without reading any browser, configuration, or target resource. */
    public static CopilotDiscoveryRequest load(Path requestPath) {
        if (requestPath == null || !Files.isRegularFile(requestPath)) {
            throw new CopilotProfileException("Discovery candidate file does not exist: " + requestPath);
        }
        try (InputStream input = Files.newInputStream(requestPath)) {
            Object parsed = safeYaml().load(input);
            if (!(parsed instanceof Map<?, ?> rawRoot)) {
                throw new CopilotProfileException("Discovery candidate YAML must contain a mapping: " + requestPath);
            }
            Map<String, Object> root = map(rawRoot, "discovery candidate root", requestPath);
            requireExactKeys(root, Set.of(ROOT_KEY), "discovery candidate root", requestPath);
            Object nested = root.get(ROOT_KEY);
            if (!(nested instanceof Map<?, ?> rawDraft)) {
                throw new CopilotProfileException("Discovery candidate key '" + ROOT_KEY + "' must be a mapping: " + requestPath);
            }
            Map<String, Object> draft = map(rawDraft, ROOT_KEY, requestPath);
            Set<String> required = Set.of(
                    "target_alias", "locator_file_name", "element_group", "feature_name", "scenario_name",
                    "page_context", "candidates");
            requireExactKeys(draft, required, ROOT_KEY, requestPath);

            Object rawCandidates = draft.get("candidates");
            if (!(rawCandidates instanceof List<?> candidateList)) {
                throw new CopilotProfileException("Discovery candidate key 'candidates' must be a list: " + requestPath);
            }
            List<CopilotDiscoveryCandidate> candidates = new ArrayList<>();
            for (int index = 0; index < candidateList.size(); index++) {
                Object rawCandidate = candidateList.get(index);
                if (!(rawCandidate instanceof Map<?, ?> rawCandidateMap)) {
                    throw new CopilotProfileException("Discovery candidate #" + (index + 1)
                            + " must be a mapping: " + requestPath);
                }
                Map<String, Object> candidate = map(rawCandidateMap, "candidate #" + (index + 1), requestPath);
                requireExactKeys(candidate, Set.of("key", "locator_type", "locator_value", "action"),
                        "candidate #" + (index + 1), requestPath);
                candidates.add(new CopilotDiscoveryCandidate(
                        requiredString(candidate, "key", "candidate #" + (index + 1), requestPath),
                        requiredString(candidate, "locator_type", "candidate #" + (index + 1), requestPath),
                        requiredString(candidate, "locator_value", "candidate #" + (index + 1), requestPath),
                        requiredString(candidate, "action", "candidate #" + (index + 1), requestPath)
                ));
            }

            return new CopilotDiscoveryRequest(
                    requiredString(draft, "target_alias", ROOT_KEY, requestPath),
                    requiredString(draft, "locator_file_name", ROOT_KEY, requestPath),
                    requiredString(draft, "element_group", ROOT_KEY, requestPath),
                    requiredString(draft, "feature_name", ROOT_KEY, requestPath),
                    requiredString(draft, "scenario_name", ROOT_KEY, requestPath),
                    requiredString(draft, "page_context", ROOT_KEY, requestPath),
                    candidates
            );
        } catch (CopilotProfileException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new CopilotProfileException("Could not read discovery candidate file: " + requestPath, exception);
        } catch (Exception exception) {
            throw new CopilotProfileException("Could not parse discovery candidate YAML " + requestPath + ": "
                    + exception.getMessage(), exception);
        }
    }

    private static Map<String, Object> map(Map<?, ?> raw, String label, Path source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                throw new CopilotProfileException("All keys in " + label + " must be non-blank strings: " + source);
            }
            if (result.put(key, entry.getValue()) != null) {
                throw new CopilotProfileException("Duplicate key '" + key + "' in " + label + ": " + source);
            }
        }
        return result;
    }

    private static void requireExactKeys(Map<String, Object> values, Set<String> expected, String label, Path source) {
        Set<String> actual = new LinkedHashSet<>(values.keySet());
        if (!actual.equals(expected)) {
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(actual);
            Set<String> unknown = new LinkedHashSet<>(actual);
            unknown.removeAll(expected);
            throw new CopilotProfileException("Invalid keys in " + label + " at " + source
                    + "; missing=" + missing + ", unsupported=" + unknown + ".");
        }
    }

    private static String requiredString(Map<String, Object> values, String key, String label, Path source) {
        Object value = values.get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new CopilotProfileException("Discovery candidate key '" + key + "' in " + label
                    + " must be a non-blank string: " + source);
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static Yaml safeYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(10);
        options.setCodePointLimit(100_000);
        return new Yaml(new SafeConstructor(options));
    }
}
