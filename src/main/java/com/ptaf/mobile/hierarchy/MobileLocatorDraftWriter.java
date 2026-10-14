package com.ptaf.mobile.hierarchy;

import com.ptaf.mobile.config.MobilePlatform;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes review-only, platform-aware locator draft YAML outside active framework locator resources.
 *
 * <p>A second capture for the same screen and the other platform merges its platform value into the
 * same draft when a generated logical key matches. Existing generated platform values remain intact
 * unless the caller explicitly enables overwrite. Testers remain responsible for reviewing, naming,
 * and copying approved entries into {@code src/test/resources/mobile/elements}.</p>
 */
public final class MobileLocatorDraftWriter {

    /** Upsert candidates into one review-only YAML draft and return its path. */
    public Path write(
            Path draftPath,
            String screenName,
            MobilePlatform platform,
            List<MobileLocatorCandidate> candidates,
            boolean overwriteExistingPlatformValues
    ) {
        Map<String, Object> root = loadExistingDraft(draftPath);
        Map<String, Object> metadata = map(root, "mobile_locator_draft_metadata");
        metadata.put("screen", screenName);
        metadata.put("last_generated_at_utc", Instant.now().toString());
        metadata.put("review_required", true);
        metadata.put("active_mobile_elements_modified", false);

        Map<String, Object> mobileElements = map(root, "mobile_elements");
        Map<String, Object> screen = map(mobileElements, screenName);
        String platformKey = platform.name().toLowerCase();

        for (MobileLocatorCandidate candidate : candidates) {
            Map<String, Object> entry = map(screen, candidate.key());
            if (overwriteExistingPlatformValues || !entry.containsKey(platformKey)) {
                entry.put(platformKey, candidate.locator());
                Map<String, Object> generatedFrom = map(entry, "generated_from");
                generatedFrom.put(platformKey, candidate.source());
                Map<String, Object> confidence = map(entry, "confidence");
                confidence.put(platformKey, candidate.confidence());
            }
            entry.put("review_status", "review_required");
        }

        try {
            Files.createDirectories(draftPath.getParent());
            Files.writeString(draftPath, createYaml().dump(root));
            return draftPath;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write mobile locator review draft: " + draftPath, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadExistingDraft(Path draftPath) {
        if (!Files.isRegularFile(draftPath)) {
            return new LinkedHashMap<>();
        }
        try (InputStream input = Files.newInputStream(draftPath)) {
            Object parsed = new Yaml().load(input);
            if (parsed == null) {
                return new LinkedHashMap<>();
            }
            if (!(parsed instanceof Map<?, ?> parsedMap)) {
                throw new IllegalStateException("Locator draft YAML must contain a mapping at its root: " + draftPath);
            }
            return deepCopy((Map<String, Object>) parsedMap);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read mobile locator review draft: " + draftPath, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value instanceof Map<?, ?> valueMap) {
            return (Map<String, Object>) valueMap;
        }
        Map<String, Object> nested = new LinkedHashMap<>();
        parent.put(key, nested);
        return nested;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) nested));
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy;
    }

    private Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        // SnakeYAML requires indicator indentation to be smaller than normal indentation.
        options.setIndicatorIndent(1);
        return new Yaml(options);
    }
}
