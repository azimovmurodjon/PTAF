package com.ptaf.copilot;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Browserless contracts for the optional local discovery workflow.
 *
 * <p>These tests deliberately use only temporary local files. They do not create Playwright,
 * contact an MCP server, navigate to an application, read the framework's actual configuration,
 * or invoke Hooks, BrowserFactory, Cucumber, reporting, or a test runner.</p>
 */
public class CopilotDiscoveryDraftContractTest {

    @Test
    public void shouldRenderReviewOnlyDraftsWithoutChangingActiveFrameworkResources() throws IOException {
        Path root = createConsumerRoot();
        try {
            Path elements = root.resolve("src/test/resources/elements/checkout.yml");
            Path feature = root.resolve("src/test/resources/features/checkout.feature");
            Path config = root.resolve("src/test/resources/config/config.yml");
            String beforeElements = Files.readString(elements, StandardCharsets.UTF_8);
            String beforeFeature = Files.readString(feature, StandardCharsets.UTF_8);
            String beforeConfig = Files.readString(config, StandardCharsets.UTF_8);

            CopilotDiscoveryPolicy policy = embeddedPolicy();
            CopilotDiscoveryRequest request = validRequest();
            CopilotDiscoveryDraftExporter.CopilotDiscoveryDraftOutput output =
                    new CopilotDiscoveryDraftExporter().writeDraft(request, policy, root);

            Assert.assertTrue(Files.isRegularFile(output.locatorDraft()));
            Assert.assertTrue(Files.isRegularFile(output.featureDraft()));
            Assert.assertTrue(Files.isRegularFile(output.readme()));
            Assert.assertTrue(output.directory().startsWith(root.resolve(".fnb-etaf/discovery-drafts")));
            Assert.assertFalse(Files.exists(root.resolve("src/test/resources/elements/checkout_discovery.yml")));
            Assert.assertFalse(Files.exists(root.resolve("src/test/resources/features/checkout_discovery.feature")));
            Assert.assertEquals(Files.readString(elements, StandardCharsets.UTF_8), beforeElements);
            Assert.assertEquals(Files.readString(feature, StandardCharsets.UTF_8), beforeFeature);
            Assert.assertEquals(Files.readString(config, StandardCharsets.UTF_8), beforeConfig);

            String locatorDraft = Files.readString(output.locatorDraft(), StandardCharsets.UTF_8);
            String featureDraft = Files.readString(output.featureDraft(), StandardCharsets.UTF_8);
            Assert.assertTrue(locatorDraft.contains("continue_button: \"TESTID_continue\""));
            Assert.assertTrue(featureDraft.contains("Given we navigate to approved_target_alias url"));
            Assert.assertTrue(featureDraft.contains("Then we click on page checkout locator continue_button"));
            Assert.assertTrue(featureDraft.contains("Then we verify on page checkout of locator confirmation_heading is visible"));
            Assert.assertFalse(locatorDraft.contains("example.invalid"), "Resolved target URLs must not be rendered.");
            Assert.assertFalse(featureDraft.contains("example.invalid"), "Resolved target URLs must not be rendered.");
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldValidateWithoutCreatingFilesAndRejectSensitiveOrUnsafeCandidates() throws IOException {
        Path root = createConsumerRoot();
        try {
            CopilotDiscoveryPolicy policy = embeddedPolicy();
            CopilotDiscoveryValidator.validate(validRequest(), policy);
            Assert.assertFalse(Files.exists(root.resolve(".fnb-etaf")), "Validation must not write a draft directory.");

            CopilotDiscoveryRequest unsafeUrl = new CopilotDiscoveryRequest(
                    "https://not-allowed.example", "checkout_discovery", "checkout",
                    "Checkout", "User continues", "page", validRequest().candidates());
            CopilotProfileException urlException = Assert.expectThrows(CopilotProfileException.class,
                    () -> CopilotDiscoveryValidator.validate(unsafeUrl, policy));
            Assert.assertFalse(urlException.getMessage().contains("not-allowed.example"));

            CopilotDiscoveryRequest unsafeSecret = new CopilotDiscoveryRequest(
                    "approved_target_alias", "checkout_discovery", "checkout",
                    "Checkout", "User continues", "page", List.of(new CopilotDiscoveryCandidate(
                            "continue_button", "TESTID", "Authorization: Bearer secret-value-that-is-long", "click")));
            CopilotProfileException secretException = Assert.expectThrows(CopilotProfileException.class,
                    () -> CopilotDiscoveryValidator.validate(unsafeSecret, policy));
            Assert.assertFalse(secretException.getMessage().contains("secret-value-that-is-long"));

            CopilotDiscoveryRequest unsupportedAction = new CopilotDiscoveryRequest(
                    "approved_target_alias", "checkout_discovery", "checkout",
                    "Checkout", "User continues", "page", List.of(new CopilotDiscoveryCandidate(
                            "continue_button", "TESTID", "continue", "fill")));
            Assert.expectThrows(CopilotProfileException.class,
                    () -> CopilotDiscoveryValidator.validate(unsupportedAction, policy));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldRenderDisabledLocalMcpTemplateOnlyUnderLocalMetadataDirectory() throws IOException {
        Path root = createConsumerRoot();
        try {
            CopilotDiscoveryPolicy policy = embeddedPolicy();
            Path template = new CopilotDiscoveryDraftExporter().writeMcpTemplate(policy, root);
            String content = Files.readString(template, StandardCharsets.UTF_8);

            Assert.assertEquals(template, root.resolve(".fnb-etaf/copilot/mcp.json"));
            Assert.assertTrue(content.contains("\"servers\""));
            Assert.assertTrue(content.contains("@playwright/mcp@0.0.83"));
            Assert.assertTrue(content.contains("--isolated"));
            Assert.assertTrue(content.contains("--codegen=java"));
            Assert.assertTrue(content.contains("--sandbox"));
            Assert.assertTrue(content.contains("--no-webmcp"));
            Assert.assertTrue(content.contains("--image-responses=omit"));
            Assert.assertTrue(content.contains("<APPROVED_TARGET_ORIGIN>"));
            Assert.assertFalse(content.contains("@latest"));
            Assert.assertFalse(content.contains("--caps="));
            Assert.assertFalse(content.contains("--ignore-https-errors"));
            Assert.assertFalse(content.contains("--allow-unrestricted-file-access"));
            Assert.assertFalse(content.contains("--no-sandbox"));

            Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotDiscoveryDraftExporter().writeMcpTemplate(policy, root));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldRejectUnknownOrNonHttpTargetAliasesWithoutDisclosingTheirValue() throws IOException {
        Path root = createConsumerRoot();
        try {
            CopilotDiscoveryRequest unknown = new CopilotDiscoveryRequest(
                    "does_not_exist", "checkout_discovery", "checkout", "Checkout", "User continues", "page",
                    validRequest().candidates());
            CopilotProfileException unknownException = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotDiscoveryDraftExporter().writeDraft(unknown, embeddedPolicy(), root));
            Assert.assertTrue(unknownException.getMessage().contains("does_not_exist"));

            Files.writeString(root.resolve("src/test/resources/config/config.yml"),
                    "approved_target_alias: \"file:///unsafe-local-path\"\n", StandardCharsets.UTF_8);
            CopilotProfileException schemeException = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotDiscoveryDraftExporter().writeDraft(validRequest(), embeddedPolicy(), root));
            Assert.assertFalse(schemeException.getMessage().contains("unsafe-local-path"));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldAcceptTheSameEnvironmentOverrideAliasPrecedenceAsNormalFrameworkConfiguration() throws IOException {
        Path root = createConsumerRoot();
        try {
            Files.writeString(root.resolve("src/test/resources/config/config.yml"), """
                    approved_target_alias: "https://example.invalid/default"
                    environments:
                      QA:
                        approved_target_alias: "https://example.invalid/qa"
                    """, StandardCharsets.UTF_8);
            CopilotDiscoveryDraftExporter.CopilotDiscoveryDraftOutput output =
                    new CopilotDiscoveryDraftExporter().writeDraft(validRequest(), embeddedPolicy(), root);
            Assert.assertTrue(Files.isRegularFile(output.featureDraft()));
            Assert.assertFalse(Files.readString(output.featureDraft(), StandardCharsets.UTF_8).contains("example.invalid"));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldLoadOnlyTheStrictCandidateSchemaAndKeepValidationBrowserless() throws IOException {
        Path input = Files.createTempFile("fnb-etaf-discovery-candidate-", ".yml");
        try {
            Files.writeString(input, """
                    discovery_draft:
                      target_alias: "approved_target_alias"
                      locator_file_name: "checkout_discovery"
                      element_group: "checkout"
                      feature_name: "Checkout navigation"
                      scenario_name: "User continues to the confirmation view"
                      page_context: "page"
                      candidates:
                        - key: "continue_button"
                          locator_type: "TESTID"
                          locator_value: "continue"
                          action: "click"
                    """, StandardCharsets.UTF_8);
            CopilotDiscoveryRequest loaded = CopilotDiscoveryRequestLoader.load(input);
            CopilotDiscoveryValidator.validate(loaded, embeddedPolicy());
            Assert.assertEquals(loaded.candidates().size(), 1);

            Files.writeString(input, """
                    discovery_draft:
                      target_alias: "approved_target_alias"
                      locator_file_name: "checkout_discovery"
                      element_group: "checkout"
                      feature_name: "Checkout navigation"
                      scenario_name: "User continues to the confirmation view"
                      page_context: "page"
                      candidates: []
                      output_path: "src/test/resources/elements/overwrite.yml"
                    """, StandardCharsets.UTF_8);
            CopilotProfileException unknownFieldException = Assert.expectThrows(CopilotProfileException.class,
                    () -> CopilotDiscoveryRequestLoader.load(input));
            Assert.assertTrue(unknownFieldException.getMessage().contains("unsupported"));

            Files.writeString(input, """
                    discovery_draft: !!java/object:java.lang.String "unsafe"
                    """, StandardCharsets.UTF_8);
            Assert.expectThrows(CopilotProfileException.class, () -> CopilotDiscoveryRequestLoader.load(input));
        } finally {
            Files.deleteIfExists(input);
        }
    }

    @Test
    public void shouldRejectUnsupportedCliArgumentsWithoutWritingFiles() throws IOException {
        Path root = createConsumerRoot();
        try {
            int exitCode = CopilotDiscoveryTool.run("draft", "--output", "outside.yml", "--root", root.toString());
            Assert.assertEquals(exitCode, 2);
            Assert.assertFalse(Files.exists(root.resolve(".fnb-etaf")));
        } finally {
            deleteRecursively(root);
        }
    }

    private CopilotDiscoveryPolicy embeddedPolicy() {
        CopilotDiscoveryPolicy policy = CopilotProfileLoader.loadEmbeddedProfile().discoveryPolicy();
        Assert.assertNotNull(policy, "The bundled central profile must include the discovery policy.");
        return policy;
    }

    private CopilotDiscoveryRequest validRequest() {
        return new CopilotDiscoveryRequest(
                "approved_target_alias",
                "checkout_discovery",
                "checkout",
                "Checkout navigation",
                "User continues to the confirmation view",
                "page",
                List.of(
                        new CopilotDiscoveryCandidate("continue_button", "TESTID", "continue", "click"),
                        new CopilotDiscoveryCandidate("confirmation_heading", "HEADING", "Confirmation", "verify_visible")
                ));
    }

    private Path createConsumerRoot() throws IOException {
        Path root = Files.createTempDirectory("fnb-etaf-copilot-discovery-");
        Files.createDirectories(root.resolve("src/test/resources/config"));
        Files.createDirectories(root.resolve("src/test/resources/elements"));
        Files.createDirectories(root.resolve("src/test/resources/features"));
        Files.writeString(root.resolve("src/test/resources/config/config.yml"),
                "approved_target_alias: \"https://example.invalid/approved-test-view\"\n", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("src/test/resources/elements/checkout.yml"),
                "elements:\n  checkout:\n    existing_button: \"BUTTON_Existing\"\n", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("src/test/resources/features/checkout.feature"),
                "Feature: Existing checkout\n\n  Scenario: Existing navigation\n    Given existing approved coverage\n", StandardCharsets.UTF_8);
        return root;
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
                    throw new IllegalStateException("Could not delete temporary test path: " + path, exception);
                }
            });
        }
    }
}
