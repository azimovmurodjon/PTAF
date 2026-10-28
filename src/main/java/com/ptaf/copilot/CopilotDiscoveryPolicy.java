package com.ptaf.copilot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable, centrally governed policy for the optional Playwright MCP discovery workflow.
 *
 * <p>This policy is read from the same embedded FNB-ETAF Copilot profile that generates the
 * repository instructions. It controls only local, review-only discovery drafts. It never opens
 * a browser, contacts an MCP server, reads a target URL, changes normal framework configuration,
 * or writes to active feature and locator directories.</p>
 */
public final class CopilotDiscoveryPolicy {
    static final String REQUIRED_DRAFT_DIRECTORY_PREFIX = ".fnb-etaf/discovery-drafts";
    static final String REQUIRED_MCP_TEMPLATE_PREFIX = ".fnb-etaf/copilot";

    private final String draftOutputDirectory;
    private final String mcpTemplateOutputPath;
    private final String mcpServerName;
    private final String playwrightMcpPackage;
    private final String defaultBrowser;
    private final boolean defaultHeadless;
    private final long idleTimeoutMillis;
    private final String consoleLevel;
    private final List<String> allowedLocatorTypes;
    private final List<String> allowedActions;
    private final int maximumCandidates;

    /**
     * Creates a discovery policy after the loader has read the central configuration resource.
     */
    public CopilotDiscoveryPolicy(
            String draftOutputDirectory,
            String mcpTemplateOutputPath,
            String mcpServerName,
            String playwrightMcpPackage,
            String defaultBrowser,
            boolean defaultHeadless,
            long idleTimeoutMillis,
            String consoleLevel,
            List<String> allowedLocatorTypes,
            List<String> allowedActions,
            int maximumCandidates
    ) {
        this.draftOutputDirectory = requireSafeRelativePath(
                draftOutputDirectory, REQUIRED_DRAFT_DIRECTORY_PREFIX, "Discovery draft output directory");
        this.mcpTemplateOutputPath = requireSafeRelativePath(
                mcpTemplateOutputPath, REQUIRED_MCP_TEMPLATE_PREFIX, "MCP template output path");
        this.mcpServerName = requireIdentifier(mcpServerName, "MCP server name");
        this.playwrightMcpPackage = requirePackage(playwrightMcpPackage);
        this.defaultBrowser = requireBrowser(defaultBrowser);
        this.defaultHeadless = defaultHeadless;
        this.idleTimeoutMillis = requireRange(idleTimeoutMillis, 30_000L, 3_600_000L,
                "MCP idle timeout in milliseconds");
        this.consoleLevel = requireConsoleLevel(consoleLevel);
        this.allowedLocatorTypes = normalizeAllowList(allowedLocatorTypes, "Allowed locator types", true);
        this.allowedActions = normalizeAllowList(allowedActions, "Allowed discovery actions", false);
        if (maximumCandidates < 1 || maximumCandidates > 100) {
            throw new CopilotProfileException("Maximum discovery candidates must be between 1 and 100.");
        }
        this.maximumCandidates = maximumCandidates;
    }

    public String draftOutputDirectory() {
        return draftOutputDirectory;
    }

    public String mcpTemplateOutputPath() {
        return mcpTemplateOutputPath;
    }

    public String mcpServerName() {
        return mcpServerName;
    }

    public String playwrightMcpPackage() {
        return playwrightMcpPackage;
    }

    public String defaultBrowser() {
        return defaultBrowser;
    }

    public boolean defaultHeadless() {
        return defaultHeadless;
    }

    public long idleTimeoutMillis() {
        return idleTimeoutMillis;
    }

    public String consoleLevel() {
        return consoleLevel;
    }

    public List<String> allowedLocatorTypes() {
        return allowedLocatorTypes;
    }

    public List<String> allowedActions() {
        return allowedActions;
    }

    public int maximumCandidates() {
        return maximumCandidates;
    }

    /** Returns whether the central policy allows the supplied FNB-ETAF locator type. */
    public boolean allowsLocatorType(String locatorType) {
        return locatorType != null && allowedLocatorTypes.contains(locatorType.trim().toUpperCase(Locale.ROOT));
    }

    /** Returns whether the central policy allows the supplied high-level discovery action. */
    public boolean allowsAction(String action) {
        return action != null && allowedActions.contains(action.trim().toLowerCase(Locale.ROOT));
    }

    private static String requireSafeRelativePath(String value, String requiredPrefix, String label) {
        if (value == null || value.isBlank()) {
            throw new CopilotProfileException(label + " must not be blank.");
        }
        Path normalized = Path.of(value).normalize();
        String text = normalized.toString().replace('\\', '/');
        if (normalized.isAbsolute() || text.equals("..") || text.startsWith("../")
                || !(text.equals(requiredPrefix) || text.startsWith(requiredPrefix + "/"))) {
            throw new CopilotProfileException(label + " must remain under " + requiredPrefix + ".");
        }
        return text;
    }

    private static String requireIdentifier(String value, String label) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_-]{0,79}")) {
            throw new CopilotProfileException(label + " must start with a letter and contain only letters, numbers, hyphens, or underscores.");
        }
        return value;
    }

    private static String requirePackage(String value) {
        if (value == null || !value.matches("@playwright/mcp@[0-9]+\\.[0-9]+\\.[0-9]+")) {
            throw new CopilotProfileException("Playwright MCP package must be pinned as @playwright/mcp@x.y.z.");
        }
        return value;
    }

    private static String requireBrowser(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("chrome", "msedge", "firefox", "webkit").contains(normalized)) {
            throw new CopilotProfileException("Default MCP browser must be chrome, msedge, firefox, or webkit.");
        }
        return normalized;
    }

    private static long requireRange(long value, long minimum, long maximum, String label) {
        if (value < minimum || value > maximum) {
            throw new CopilotProfileException(label + " must be between " + minimum + " and " + maximum + ".");
        }
        return value;
    }

    private static String requireConsoleLevel(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("error", "warning", "info").contains(normalized)) {
            throw new CopilotProfileException("MCP console level must be error, warning, or info.");
        }
        return normalized;
    }

    private static List<String> normalizeAllowList(List<String> values, String label, boolean uppercase) {
        Objects.requireNonNull(values, label + " must not be null.");
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new CopilotProfileException(label + " must not contain blank values.");
            }
            normalized.add(uppercase ? value.trim().toUpperCase(Locale.ROOT) : value.trim().toLowerCase(Locale.ROOT));
        }
        if (normalized.isEmpty()) {
            throw new CopilotProfileException(label + " must contain at least one value.");
        }
        return List.copyOf(new ArrayList<>(normalized));
    }
}
