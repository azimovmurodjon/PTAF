package com.ptaf.mobile.hierarchy;

import com.ptaf.mobile.config.MobilePlatform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Creates conservative, platform-specific locator suggestions from an Appium hierarchy XML.
 *
 * <p>The generator is intentionally not a self-healing locator engine. It does not alter active
 * locator files, it does not create index-based XPath expressions, and it labels every result for
 * tester review. Android candidates prefer accessibility and resource identifiers; iOS candidates
 * prefer stable accessibility/name identifiers. User-facing text and label values are deliberately
 * excluded because they may contain sensitive or dynamic data.</p>
 */
public final class MobileLocatorCandidateGenerator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?i).+@.+\\..+");
    private static final Pattern LONG_DIGIT_PATTERN = Pattern.compile(".*\\d{5,}.*");
    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "password", "passwd", "token", "secret", "account", "email", "phone", "ssn", "card", "otp"
    );

    /** Generate distinct, ordered suggestions from the supplied native hierarchy. */
    public List<MobileLocatorCandidate> generate(String hierarchyXml, MobilePlatform platform) {
        if (platform == null) {
            throw new IllegalArgumentException("A resolved Android or iOS platform is required for locator generation.");
        }
        Document document = MobileHierarchyXmlSanitizer.parse(hierarchyXml);
        Map<String, MobileLocatorCandidate> candidates = new LinkedHashMap<>();
        inspect(document.getDocumentElement(), platform, candidates);
        return new ArrayList<>(candidates.values());
    }

    private void inspect(Node node, MobilePlatform platform, Map<String, MobileLocatorCandidate> candidates) {
        if (node instanceof Element element) {
            if (platform.isAndroid()) {
                addAndroidCandidates(element, candidates);
            } else {
                addIosCandidates(element, candidates);
            }
        }
        NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            inspect(children.item(index), platform, candidates);
        }
    }

    private void addAndroidCandidates(Element element, Map<String, MobileLocatorCandidate> candidates) {
        String contentDescription = attribute(element, "content-desc");
        if (isLikelyStableIdentifier(contentDescription)) {
            add(candidates, keyFor(contentDescription, "element"),
                    "ACCESSIBILITY_ID_" + contentDescription, "content-desc", "high");
        }

        String resourceId = attribute(element, "resource-id");
        if (!resourceId.isBlank()) {
            String keySeed = resourceId.substring(resourceId.lastIndexOf('/') + 1);
            if (isLikelyStableIdentifier(keySeed)) {
                add(candidates, keyFor(keySeed, "element"), "ID_" + resourceId, "resource-id", "high");
            }
        }
    }

    private void addIosCandidates(Element element, Map<String, MobileLocatorCandidate> candidates) {
        String name = attribute(element, "name");
        if (isLikelyStableIdentifier(name)) {
            add(candidates, keyFor(name, "element"), "ACCESSIBILITY_ID_" + name, "name", "high");
        }
    }

    private void add(Map<String, MobileLocatorCandidate> candidates, String baseKey, String locator, String source, String confidence) {
        if (locator.isBlank() || candidates.values().stream().anyMatch(candidate -> candidate.locator().equals(locator))) {
            return;
        }
        String key = baseKey;
        int suffix = 2;
        while (candidates.containsKey(key)) {
            key = baseKey + suffix++;
        }
        candidates.put(key, new MobileLocatorCandidate(key, locator, source, confidence));
    }

    private String attribute(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name).trim() : "";
    }

    private boolean isLikelyStableIdentifier(String value) {
        if (value == null || value.isBlank() || value.length() > 80 || value.contains(" ")
                || EMAIL_PATTERN.matcher(value).matches() || LONG_DIGIT_PATTERN.matcher(value).matches()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return SENSITIVE_WORDS.stream().noneMatch(normalized::contains);
    }

    private String keyFor(String source, String fallback) {
        String[] parts = source.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("[^A-Za-z0-9]+", " ")
                .trim()
                .split("\\s+");
        StringBuilder key = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (key.isEmpty()) {
                key.append(part.substring(0, 1).toLowerCase(Locale.ROOT));
                key.append(part.substring(1));
            } else {
                key.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
                key.append(part.substring(1));
            }
        }
        if (key.isEmpty() || !Character.isLetter(key.charAt(0))) {
            return fallback;
        }
        return key.toString();
    }

}
