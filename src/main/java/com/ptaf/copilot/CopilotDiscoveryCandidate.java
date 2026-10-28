package com.ptaf.copilot;

import java.util.Locale;

/**
 * One reviewed-before-promotion locator/action proposal produced from an accessibility inspection.
 *
 * <p>The class holds no target URL, credentials, cookies, test data, screenshots, page HTML, or
 * browser state. Its values describe only a logical FNB-ETAF locator and a supported business-level
 * Gherkin action that may be rendered into a local draft.</p>
 */
public record CopilotDiscoveryCandidate(
        String key,
        String locatorType,
        String locatorValue,
        String action
) {
    public CopilotDiscoveryCandidate {
        key = normalize(key);
        locatorType = normalize(locatorType).toUpperCase(Locale.ROOT);
        locatorValue = normalize(locatorValue);
        action = normalize(action).toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
