package com.ptaf.copilot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Explicit filesystem boundary for the optional discovery workflow.
 *
 * <p>The exporter can write only beneath a consumer-root-local {@code .fnb-etaf} folder. It rejects
 * existing files rather than overwriting them. It has no access to active framework configuration,
 * test resources, reports, browser state, or network services.</p>
 */
public final class CopilotDiscoveryDraftExporter {
    private final CopilotPlaywrightMcpTemplateRenderer mcpTemplateRenderer;
    private final CopilotDiscoveryDraftRenderer draftRenderer;

    public CopilotDiscoveryDraftExporter() {
        this(new CopilotPlaywrightMcpTemplateRenderer(), new CopilotDiscoveryDraftRenderer());
    }

    CopilotDiscoveryDraftExporter(
            CopilotPlaywrightMcpTemplateRenderer mcpTemplateRenderer,
            CopilotDiscoveryDraftRenderer draftRenderer
    ) {
        this.mcpTemplateRenderer = mcpTemplateRenderer;
        this.draftRenderer = draftRenderer;
    }

    /** Writes the local-only IntelliJ MCP configuration template once, without launching an MCP server. */
    public Path writeMcpTemplate(CopilotDiscoveryPolicy policy, Path consumerRoot) throws IOException {
        Path root = validatedRoot(consumerRoot);
        Path output = resolveBelow(root, policy.mcpTemplateOutputPath());
        writeNew(output, mcpTemplateRenderer.render(policy));
        return output;
    }

    /** Writes an isolated locator/feature draft directory only after strict validation. */
    public CopilotDiscoveryDraftOutput writeDraft(
            CopilotDiscoveryRequest request,
            CopilotDiscoveryPolicy policy,
            Path consumerRoot
    ) throws IOException {
        CopilotDiscoveryValidator.validate(request, policy);
        Path root = validatedRoot(consumerRoot);
        CopilotDiscoveryTargetConfigurationVerifier.verify(root, request.targetAlias());
        Path directory = resolveBelow(root, policy.draftOutputDirectory()).resolve(request.locatorFileName()).normalize();
        if (!directory.startsWith(root) || Files.exists(directory)) {
            if (Files.exists(directory)) {
                throw new CopilotProfileException("Discovery draft directory already exists: "
                        + root.relativize(directory) + ". Review it or remove it explicitly before rendering again.");
            }
            throw new CopilotProfileException("Discovery draft directory escapes the consumer repository.");
        }

        Path locatorDraft = directory.resolve(request.locatorFileName() + "-locator-draft.yml");
        Path featureDraft = directory.resolve(request.locatorFileName() + "-feature-draft.feature");
        Path readme = directory.resolve("README.md");
        Files.createDirectories(directory);
        try {
            writeNew(locatorDraft, draftRenderer.renderLocatorYaml(request));
            writeNew(featureDraft, draftRenderer.renderFeature(request));
            writeNew(readme, draftRenderer.renderReadme(request));
            return new CopilotDiscoveryDraftOutput(directory, locatorDraft, featureDraft, readme);
        } catch (Exception exception) {
            deleteRecursively(directory);
            if (exception instanceof IOException ioException) {
                throw ioException;
            }
            throw exception;
        }
    }

    private Path validatedRoot(Path consumerRoot) {
        if (consumerRoot == null) {
            throw new CopilotProfileException("Consumer repository root must not be null.");
        }
        Path root = consumerRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new CopilotProfileException("Consumer repository root is not a directory: " + root);
        }
        return root;
    }

    private Path resolveBelow(Path root, String relativePath) {
        Path output = root.resolve(relativePath).normalize().toAbsolutePath();
        if (!output.startsWith(root.resolve(".fnb-etaf").toAbsolutePath().normalize())) {
            throw new CopilotProfileException("Copilot discovery may write only under .fnb-etaf.");
        }
        return output;
    }

    private void writeNew(Path output, String content) throws IOException {
        if (Files.exists(output)) {
            throw new CopilotProfileException("Refusing to overwrite existing local discovery artifact: " + output);
        }
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(output.getParent(), ".fnb-etaf-discovery-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, output);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Preserve the original write failure; the directory remains local and ignored.
                }
            });
        } catch (IOException ignored) {
            // Preserve the original write failure.
        }
    }

    /** Paths created by one successful review-only discovery-draft export. */
    public record CopilotDiscoveryDraftOutput(Path directory, Path locatorDraft, Path featureDraft, Path readme) {
        public List<Path> files() {
            return List.of(locatorDraft, featureDraft, readme);
        }
    }
}
