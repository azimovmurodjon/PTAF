package com.ptaf.copilot;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contract coverage for the embedded FNB-ETAF Copilot profile and deterministic instruction rendering.
 */
public class CopilotProfileContractTest {

    @Test
    public void shouldLoadEmbeddedProfileAndRenderExpectedIntelliJInstructionTargets() {
        CopilotProfile profile = CopilotProfileLoader.loadEmbeddedProfile();
        List<CopilotInstructionTarget> targets = new CopilotInstructionRenderer().render(profile);
        Set<String> paths = targets.stream().map(CopilotInstructionTarget::relativePath).collect(Collectors.toSet());

        Assert.assertEquals(profile.version(), "1.1.0");
        Assert.assertNotNull(profile.discoveryPolicy(), "The bundled profile must govern local discovery drafts.");
        Assert.assertTrue(profile.discoveryPolicy().defaultHeadless());
        Assert.assertTrue(profile.discoveryPolicy().allowsLocatorType("TESTID"));
        Assert.assertTrue(profile.discoveryPolicy().allowsAction("verify_visible"));
        Assert.assertEquals(targets.size(), 9, "One root and eight scoped instruction files are required.");
        Assert.assertTrue(paths.contains(".github/copilot-instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-java.instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-feature.instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-ui.instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-api-db-data.instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-mobile.instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-performance.instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-reporting.instructions.md"));
        Assert.assertTrue(paths.contains(".github/instructions/fnb-etaf-maven.instructions.md"));

        targets.stream()
                .filter(target -> target.relativePath().endsWith(".instructions.md"))
                .forEach(target -> {
                    Assert.assertTrue(target.content().startsWith("---\n"));
                    Assert.assertTrue(target.content().contains("applyTo:"));
                    Assert.assertTrue(target.content().contains(CopilotProfileValidator.GENERATED_MARKER));
                });
    }

    @Test
    public void shouldRejectUnsafeInstructionTargetsAndSensitiveLiteralContent() {
        CopilotProfile invalidPathProfile = new CopilotProfile(
                "test",
                "Safe repository guidance.",
                java.util.Map.of("../outside.instructions.md", "---\napplyTo: \"**/*.java\"\n---\nSafe content."));
        CopilotProfileException pathException = Assert.expectThrows(
                CopilotProfileException.class,
                () -> CopilotProfileValidator.validate(invalidPathProfile));
        Assert.assertTrue(pathException.getMessage().contains("escapes"));

        CopilotProfile sensitiveProfile = new CopilotProfile(
                "test",
                "Bearer abcdefghijklmnopqrstuvwxyz",
                java.util.Map.of(".github/instructions/safe.instructions.md",
                        "---\napplyTo: \"**/*.java\"\n---\nSafe content."));
        CopilotProfileException contentException = Assert.expectThrows(
                CopilotProfileException.class,
                () -> CopilotProfileValidator.validate(sensitiveProfile));
        Assert.assertTrue(contentException.getMessage().contains("token-like"));
    }

    @Test
    public void shouldRejectUnknownTopLevelProfileKeys() throws Exception {
        Path profile = Files.createTempFile("fnb-etaf-copilot-profile-", ".yml");
        try {
            Files.writeString(profile, """
                    version: "test"
                    repository_instruction: "Safe repository guidance."
                    path_instructions:
                      .github/instructions/safe.instructions.md: |
                        ---
                        applyTo: "**/*.java"
                        ---
                        Safe content.
                    unsupported_setting: true
                    """, StandardCharsets.UTF_8);

            CopilotProfileException exception = Assert.expectThrows(
                    CopilotProfileException.class,
                    () -> CopilotProfileLoader.load(profile));
            Assert.assertTrue(exception.getMessage().contains("Unsupported Copilot profile key"));
        } finally {
            Files.deleteIfExists(profile);
        }
    }
}
