package com.ptaf.copilot;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Verifies explicit sync/verify behavior without touching the framework repository or test runtime configuration.
 */
public class CopilotInstructionExporterTest {

    @Test
    public void shouldSynchronizeAndVerifyOnlyAllowListedInstructionFiles() throws IOException {
        Path consumerRoot = Files.createTempDirectory("fnb-etaf-copilot-consumer-");
        try {
            CopilotProfile profile = safeTestProfile();
            CopilotInstructionExporter exporter = new CopilotInstructionExporter();

            exporter.sync(profile, consumerRoot);
            exporter.verify(profile, consumerRoot);

            Path rootInstruction = consumerRoot.resolve(".github/copilot-instructions.md");
            Path scopedInstruction = consumerRoot.resolve(".github/instructions/fnb-etaf-test.instructions.md");
            Assert.assertTrue(Files.isRegularFile(rootInstruction));
            Assert.assertTrue(Files.isRegularFile(scopedInstruction));
            Assert.assertTrue(Files.readString(rootInstruction, StandardCharsets.UTF_8)
                    .contains(CopilotProfileValidator.GENERATED_MARKER));
            Assert.assertTrue(Files.readString(scopedInstruction, StandardCharsets.UTF_8).startsWith("---\n"));
        } finally {
            deleteRecursively(consumerRoot);
        }
    }

    @Test
    public void shouldDetectDriftWithoutOverwritingConsumerFilesDuringVerify() throws IOException {
        Path consumerRoot = Files.createTempDirectory("fnb-etaf-copilot-drift-");
        try {
            CopilotProfile profile = safeTestProfile();
            CopilotInstructionExporter exporter = new CopilotInstructionExporter();
            exporter.sync(profile, consumerRoot);

            Path output = consumerRoot.resolve(".github/copilot-instructions.md");
            String changedContent = Files.readString(output, StandardCharsets.UTF_8) + "\nmanual drift\n";
            Files.writeString(output, changedContent, StandardCharsets.UTF_8);

            CopilotProfileException exception = Assert.expectThrows(
                    CopilotProfileException.class,
                    () -> exporter.verify(profile, consumerRoot));
            Assert.assertTrue(exception.getMessage().contains("differs from the central profile"));
            Assert.assertEquals(Files.readString(output, StandardCharsets.UTF_8), changedContent,
                    "Verify must not modify a consumer repository.");
        } finally {
            deleteRecursively(consumerRoot);
        }
    }

    @Test
    public void shouldRejectUnexpectedGeneratedInstructionFile() throws IOException {
        Path consumerRoot = Files.createTempDirectory("fnb-etaf-copilot-unexpected-");
        try {
            CopilotProfile profile = safeTestProfile();
            CopilotInstructionExporter exporter = new CopilotInstructionExporter();
            exporter.sync(profile, consumerRoot);

            Path unexpected = consumerRoot.resolve(".github/instructions/old.instructions.md");
            Files.writeString(unexpected,
                    "---\napplyTo: \"**/*.java\"\n---\n"
                            + CopilotProfileValidator.GENERATED_MARKER + " old -->\nold content\n",
                    StandardCharsets.UTF_8);

            CopilotProfileException exception = Assert.expectThrows(
                    CopilotProfileException.class,
                    () -> exporter.verify(profile, consumerRoot));
            Assert.assertTrue(exception.getMessage().contains("Unexpected FNB-ETAF generated instruction"));
        } finally {
            deleteRecursively(consumerRoot);
        }
    }

    private CopilotProfile safeTestProfile() {
        return new CopilotProfile(
                "test",
                "Test repository-wide guidance.",
                Map.of(
                        ".github/instructions/fnb-etaf-test.instructions.md",
                        "---\napplyTo: \"**/*.java\"\n---\nTest scoped guidance."));
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("Could not delete temporary path: " + path, exception);
                }
            });
        }
    }
}
