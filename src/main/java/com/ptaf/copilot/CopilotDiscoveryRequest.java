package com.ptaf.copilot;

import java.util.List;

/**
 * Complete review-only request for rendering FNB-ETAF locator and feature drafts.
 *
 * <p>{@code targetAlias} is a key already maintained in the consumer's existing configuration.
 * It is intentionally an alias, not a URL, so generated drafts do not duplicate environment
 * endpoints. Only top-level page actions are generated automatically; frame, popup, login, file,
 * payment, and other higher-risk workflows remain a human review decision.</p>
 */
public record CopilotDiscoveryRequest(
        String targetAlias,
        String locatorFileName,
        String elementGroup,
        String featureName,
        String scenarioName,
        String pageContext,
        List<CopilotDiscoveryCandidate> candidates
) {
    public CopilotDiscoveryRequest {
        targetAlias = clean(targetAlias);
        locatorFileName = clean(locatorFileName);
        elementGroup = clean(elementGroup);
        featureName = clean(featureName);
        scenarioName = clean(scenarioName);
        pageContext = clean(pageContext);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
