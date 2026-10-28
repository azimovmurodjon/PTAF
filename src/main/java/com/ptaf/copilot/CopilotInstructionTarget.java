package com.ptaf.copilot;

import java.util.Objects;

/**
 * A single allow-listed file generated for GitHub Copilot in a consuming repository.
 *
 * @param relativePath repository-relative target path
 * @param content deterministic UTF-8 Markdown content
 */
public record CopilotInstructionTarget(String relativePath, String content) {

    public CopilotInstructionTarget {
        Objects.requireNonNull(relativePath, "Relative path must not be null.");
        Objects.requireNonNull(content, "Instruction content must not be null.");
        if (relativePath.isBlank()) {
            throw new IllegalArgumentException("Relative path must not be blank.");
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("Instruction content must not be blank for " + relativePath + ".");
        }
    }
}
