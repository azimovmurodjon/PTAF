package com.ptaf.copilot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Explicitly synchronizes or verifies generated GitHub Copilot instructions in a consumer repository.
 *
 * <p>No method in this class is called during FNB-ETAF test execution. Consumers must invoke the
 * command-line tool intentionally, review the generated files, and commit them to their own repository.</p>
 */
public final class CopilotInstructionExporter {
    private final CopilotInstructionRenderer renderer;

    public CopilotInstructionExporter() {
        this(new CopilotInstructionRenderer());
    }

    CopilotInstructionExporter(CopilotInstructionRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Writes only allow-listed instruction files under the supplied consumer repository root.
     * Existing matching files are left untouched to preserve timestamps and avoid needless Git diffs.
     *
     * @param profile central profile
     * @param consumerRoot consumer repository root
     * @throws IOException if output cannot be written
     */
    public void sync(CopilotProfile profile, Path consumerRoot) throws IOException {
        Path root = validatedRoot(consumerRoot);
        List<CopilotInstructionTarget> targets = renderer.render(profile);
        for (CopilotInstructionTarget target : targets) {
            Path output = resolveOutput(root, target.relativePath());
            Files.createDirectories(output.getParent());
            String current = Files.isRegularFile(output) ? Files.readString(output, StandardCharsets.UTF_8) : null;
            if (!target.content().equals(current)) {
                writeAtomically(output, target.content());
            }
        }
        verify(profile, root);
    }

    /**
     * Verifies expected files byte-for-byte and detects unexpected FNB-ETAF-generated instruction files.
     * This operation never writes a file.
     *
     * @param profile central profile
     * @param consumerRoot consumer repository root
     * @throws IOException if files cannot be read
     */
    public void verify(CopilotProfile profile, Path consumerRoot) throws IOException {
        Path root = validatedRoot(consumerRoot);
        List<CopilotInstructionTarget> targets = renderer.render(profile);
        List<String> problems = new ArrayList<>();
        Set<Path> expected = new HashSet<>();

        for (CopilotInstructionTarget target : targets) {
            Path output = resolveOutput(root, target.relativePath());
            expected.add(output);
            if (!Files.isRegularFile(output)) {
                problems.add("Missing generated instruction: " + target.relativePath());
                continue;
            }
            String actual = Files.readString(output, StandardCharsets.UTF_8);
            if (!target.content().equals(actual)) {
                problems.add("Generated instruction differs from the central profile: " + target.relativePath());
            }
        }

        Path githubDirectory = root.resolve(".github");
        if (Files.isDirectory(githubDirectory)) {
            try (Stream<Path> files = Files.walk(githubDirectory)) {
                files.filter(Files::isRegularFile)
                        .filter(this::isGeneratedByFnbEtaf)
                        .filter(path -> !expected.contains(path.toAbsolutePath().normalize()))
                        .forEach(path -> problems.add("Unexpected FNB-ETAF generated instruction: "
                                + root.relativize(path.toAbsolutePath().normalize())));
            }
        }

        if (!problems.isEmpty()) {
            throw new CopilotProfileException("FNB-ETAF Copilot instruction verification failed:\n - "
                    + String.join("\n - ", problems)
                    + "\nRun sync explicitly after reviewing the central profile.");
        }
    }

    private boolean isGeneratedByFnbEtaf(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8)
                    .contains(CopilotProfileValidator.GENERATED_MARKER);
        } catch (IOException ignored) {
            return false;
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

    private Path resolveOutput(Path root, String relativePath) {
        CopilotProfileValidator.validateInstructionPath(relativePath);
        Path output = root.resolve(relativePath).normalize().toAbsolutePath();
        if (!output.startsWith(root)) {
            throw new CopilotProfileException("Generated instruction path escapes consumer root: " + relativePath);
        }
        return output;
    }

    private void writeAtomically(Path output, String content) throws IOException {
        Path temporary = Files.createTempFile(output.getParent(), ".fnb-etaf-copilot-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
