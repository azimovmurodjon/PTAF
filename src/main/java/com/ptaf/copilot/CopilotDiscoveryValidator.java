package com.ptaf.copilot;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Enforces the review-only boundary for Copilot and Playwright MCP discovery drafts.
 *
 * <p>Validation happens before an output directory is created. The validator deliberately does not
 * inspect a target URL, open a browser, read existing locator YAML, infer test data, or contact a
 * network service. It validates only the local candidate structure against the centrally governed
 * policy.</p>
 */
public final class CopilotDiscoveryValidator {
    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,79}");
    private static final Pattern TARGET_ALIAS = Pattern.compile("[A-Za-z][A-Za-z0-9_.-]{0,119}");
    private static final Pattern URL = Pattern.compile("(?i)https?://");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[a-z0-9._~+\\-/]{16,}");
    private static final Pattern JWT = Pattern.compile("eyJ[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}\\.[a-zA-Z0-9_-]{10,}");
    private static final Pattern SECRET_ASSIGNMENT = Pattern.compile(
            "(?i)(password|passphrase|token|secret|authorization|cookie|session(?:id)?)\\s*[:=]\\s*[^\\s]+" );

    private CopilotDiscoveryValidator() {
        // Utility class.
    }

    /** Validates a complete candidate request against the embedded central policy. */
    public static void validate(CopilotDiscoveryRequest request, CopilotDiscoveryPolicy policy) {
        if (request == null) {
            throw new CopilotProfileException("Discovery request must not be null.");
        }
        if (policy == null) {
            throw new CopilotProfileException("Discovery policy must not be null.");
        }

        requirePattern(request.targetAlias(), TARGET_ALIAS, "Target alias");
        rejectSensitiveOrUrl(request.targetAlias(), "Target alias");
        requirePattern(request.locatorFileName(), SIMPLE_IDENTIFIER, "Locator file name");
        requirePattern(request.elementGroup(), SIMPLE_IDENTIFIER, "Element group");
        requireTitle(request.featureName(), "Feature name");
        requireTitle(request.scenarioName(), "Scenario name");

        if (!"page".equalsIgnoreCase(request.pageContext())) {
            throw new CopilotProfileException("Only page_context: page is supported for automatic drafts. "
                    + "Frames, nested frames, popups, authentication, uploads, downloads, and payment flows require manual review.");
        }
        if (request.candidates().isEmpty()) {
            throw new CopilotProfileException("Discovery request must contain at least one candidate.");
        }
        if (request.candidates().size() > policy.maximumCandidates()) {
            throw new CopilotProfileException("Discovery request contains " + request.candidates().size()
                    + " candidates; the central policy permits at most " + policy.maximumCandidates() + ".");
        }

        Set<String> keys = new HashSet<>();
        for (int index = 0; index < request.candidates().size(); index++) {
            CopilotDiscoveryCandidate candidate = request.candidates().get(index);
            int humanIndex = index + 1;
            if (candidate == null) {
                throw new CopilotProfileException("Discovery candidate #" + humanIndex + " must not be null.");
            }
            requirePattern(candidate.key(), SIMPLE_IDENTIFIER, "Candidate #" + humanIndex + " key");
            if (!keys.add(candidate.key().toLowerCase(Locale.ROOT))) {
                throw new CopilotProfileException("Duplicate candidate key: " + candidate.key());
            }
            if (!policy.allowsLocatorType(candidate.locatorType())) {
                throw new CopilotProfileException("Candidate '" + candidate.key() + "' uses locator type '"
                        + candidate.locatorType() + "', which is not allowed by the central discovery policy.");
            }
            if (!policy.allowsAction(candidate.action())) {
                throw new CopilotProfileException("Candidate '" + candidate.key() + "' uses action '"
                        + candidate.action() + "', which is not allowed by the central discovery policy.");
            }
            if (candidate.locatorValue().isBlank() || candidate.locatorValue().length() > 500
                    || containsLineBreak(candidate.locatorValue())) {
                throw new CopilotProfileException("Candidate '" + candidate.key()
                        + "' locator value must be a single non-blank line of at most 500 characters.");
            }
            rejectSensitiveOrUrl(candidate.locatorValue(), "Candidate '" + candidate.key() + "' locator value");
        }
    }

    private static void requirePattern(String value, Pattern pattern, String label) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new CopilotProfileException(label + " has an invalid format.");
        }
    }

    private static void requireTitle(String value, String label) {
        if (value == null || value.isBlank() || value.length() > 160 || containsLineBreak(value)) {
            throw new CopilotProfileException(label + " must be a single non-blank line of at most 160 characters.");
        }
        rejectSensitiveOrUrl(value, label);
    }

    private static boolean containsLineBreak(String value) {
        return value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
    }

    private static void rejectSensitiveOrUrl(String value, String label) {
        if (URL.matcher(value).find()) {
            throw new CopilotProfileException(label + " must not contain a URL. Use a pre-approved configuration alias instead.");
        }
        if (BEARER.matcher(value).find() || JWT.matcher(value).find() || SECRET_ASSIGNMENT.matcher(value).find()) {
            throw new CopilotProfileException(label + " contains a credential-like value and was rejected.");
        }
    }
}
