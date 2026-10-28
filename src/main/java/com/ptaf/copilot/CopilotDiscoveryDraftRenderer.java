package com.ptaf.copilot;

import java.util.Locale;

/**
 * Renders review-only FNB-ETAF locator and Gherkin text from approved discovery candidates.
 *
 * <p>The renderer has no filesystem, browser, MCP, configuration-reader, or network dependency.
 * It does not infer selectors or actions. It simply converts the narrow validated candidate schema
 * into copy-ready drafts that use existing FNB-ETAF YAML and Cucumber conventions.</p>
 */
public final class CopilotDiscoveryDraftRenderer {
    /** Renders a locator YAML draft that reviewers may copy into an approved active locator file. */
    public String renderLocatorYaml(CopilotDiscoveryRequest request) {
        StringBuilder result = new StringBuilder();
        result.append("# REVIEW-ONLY FNB-ETAF discovery draft.\n")
                .append("# Generated locally from an approved, non-sensitive candidate file.\n")
                .append("# Copy reviewed entries into src/test/resources/elements/")
                .append(request.locatorFileName()).append(".yml; do not use this draft directly at runtime.\n")
                .append("# Existing target alias (not a URL): ").append(request.targetAlias()).append("\n")
                .append("# Active framework files were not modified.\n\n")
                .append("elements:\n")
                .append("  ").append(request.elementGroup()).append(":\n");
        for (CopilotDiscoveryCandidate candidate : request.candidates()) {
            result.append("    ").append(candidate.key()).append(": ")
                    .append(quote(candidate.locatorType() + "_" + candidate.locatorValue())).append("\n");
        }
        return result.toString();
    }

    /** Renders one clean, normal-UI feature draft using only existing page-level Cucumber bindings. */
    public String renderFeature(CopilotDiscoveryRequest request) {
        StringBuilder result = new StringBuilder();
        result.append("# REVIEW-ONLY FNB-ETAF discovery draft.\n")
                .append("# Target is configuration alias '").append(request.targetAlias())
                .append("'; no URL, credentials, cookies, values, or evidence are included.\n")
                .append("# Review each action, validate it locally, then copy it to an approved feature file.\n")
                .append("# This draft uses only existing top-level page steps; frame/popup/auth/data actions remain manual.\n\n")
                .append("Feature: ").append(request.featureName()).append("\n\n")
                .append("  Scenario: ").append(request.scenarioName()).append("\n")
                .append("    Given we navigate to ").append(request.targetAlias()).append(" url\n");
        for (CopilotDiscoveryCandidate candidate : request.candidates()) {
            result.append("    ").append(toGherkin(request.elementGroup(), candidate)).append("\n");
        }
        return result.toString();
    }

    /** Renders a short review record that explicitly states what did and did not occur. */
    public String renderReadme(CopilotDiscoveryRequest request) {
        return "# FNB-ETAF Playwright MCP discovery draft\n\n"
                + "## Review status\n\n"
                + "**Manual review required.** This directory contains only local drafts. The FNB-ETAF "
                + "runtime, active locator YAML, active feature files, Hooks, BrowserFactory, runners, reports, "
                + "videos, and test execution were not changed.\n\n"
                + "## Approved configuration reference\n\n"
                + "- Target configuration alias: `" + request.targetAlias() + "`\n"
                + "- Proposed active locator file: `src/test/resources/elements/" + request.locatorFileName() + ".yml`\n"
                + "- Proposed element group: `" + request.elementGroup() + "`\n"
                + "- Page context: `" + request.pageContext().toLowerCase(Locale.ROOT) + "`\n\n"
                + "## Files\n\n"
                + "- `" + request.locatorFileName() + "-locator-draft.yml`: locator entries in existing FNB-ETAF YAML syntax.\n"
                + "- `" + request.locatorFileName() + "-feature-draft.feature`: clean page-level Gherkin using existing bindings.\n\n"
                + "## Required human actions\n\n"
                + "1. Confirm the target alias is already configured in `src/test/resources/config/config.yml`.\n"
                + "2. Confirm each locator with an approved non-sensitive test view.\n"
                + "3. Check the locator type and logical key against project naming conventions.\n"
                + "4. Copy only approved entries into active test resources and run the focused test manually.\n"
                + "5. Do not copy URLs, credentials, tokens, cookies, customer data, screenshots, or page source into this directory or source control.\n";
    }

    private String toGherkin(String elementGroup, CopilotDiscoveryCandidate candidate) {
        return switch (candidate.action()) {
            case "click" -> "Then we click on page " + elementGroup + " locator " + candidate.key();
            case "verify_visible" -> "Then we verify on page " + elementGroup + " of locator "
                    + candidate.key() + " is visible";
            case "verify_enabled" -> "Then we verify on page " + elementGroup + " of locator "
                    + candidate.key() + " is enabled";
            case "verify_exists" -> "Then we verify on page " + elementGroup + " of locator "
                    + candidate.key() + " is existed";
            default -> throw new CopilotProfileException("Unsupported validated discovery action: " + candidate.action());
        };
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
