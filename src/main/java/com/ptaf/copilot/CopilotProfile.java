package com.ptaf.copilot;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable representation of the centrally managed FNB-ETAF Copilot instruction profile.
 *
 * <p>The profile is guidance and local-tool metadata only. It does not participate in browser setup,
 * Hooks, runners, report generation, or test execution. The optional discovery policy controls an
 * explicit review-only generator; it does not create or call an MCP server.</p>
 */
public final class CopilotProfile {
    private final String version;
    private final String repositoryInstruction;
    private final Map<String, String> pathInstructions;
    private final CopilotDiscoveryPolicy discoveryPolicy;

    /**
     * Creates a legacy-compatible profile without a local browser-discovery policy.
     * This overload preserves the public constructor contract used by existing consumers and tests.
     */
    public CopilotProfile(String version, String repositoryInstruction, Map<String, String> pathInstructions) {
        this(version, repositoryInstruction, pathInstructions, null);
    }

    /** Creates a profile with optional centrally governed local discovery settings. */
    public CopilotProfile(
            String version,
            String repositoryInstruction,
            Map<String, String> pathInstructions,
            CopilotDiscoveryPolicy discoveryPolicy
    ) {
        this.version = requireNonBlank(version, "Profile version");
        this.repositoryInstruction = requireNonBlank(repositoryInstruction, "Repository instruction");
        Objects.requireNonNull(pathInstructions, "Path instructions must not be null.");
        if (pathInstructions.isEmpty()) {
            throw new IllegalArgumentException("At least one path-specific Copilot instruction is required.");
        }

        Map<String, String> copy = new LinkedHashMap<>();
        pathInstructions.forEach((path, content) -> copy.put(
                requireNonBlank(path, "Instruction output path"),
                requireNonBlank(content, "Instruction content for " + path)));
        // LinkedHashMap order is part of the generated-file contract; avoid Map.copyOf because
        // callers must receive the same instruction ordering on every render.
        this.pathInstructions = Collections.unmodifiableMap(new LinkedHashMap<>(copy));
        this.discoveryPolicy = discoveryPolicy;
    }

    public String version() {
        return version;
    }

    public String repositoryInstruction() {
        return repositoryInstruction;
    }

    public Map<String, String> pathInstructions() {
        return pathInstructions;
    }

    /**
     * Returns the optional local discovery policy, or {@code null} for a legacy profile.
     * The bundled FNB-ETAF profile always supplies this policy.
     */
    public CopilotDiscoveryPolicy discoveryPolicy() {
        return discoveryPolicy;
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
        return value;
    }
}
