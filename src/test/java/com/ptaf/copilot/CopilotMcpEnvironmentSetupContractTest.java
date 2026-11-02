package com.ptaf.copilot;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Browserless contracts using only temporary files and example.invalid target fixtures. */
public class CopilotMcpEnvironmentSetupContractTest {
    private static final String APPROVED_SELECTION = """
            environment: 'QA'
            selected_target_alias: 'application.baseUrl'
            approved_target_aliases: ['application.baseUrl']
            non_sensitive_target_approved: true
            """;
    private static final String VALID_CONFIG = "application:\n  baseUrl: 'https://example.invalid/default'\n";

    @Test
    public void shouldInitializeSafeEmptyDefaultsOnceWithoutWritingMcpOrTouchingConfig() throws IOException {
        Path root = consumerRoot();
        try {
            CopilotMcpEnvironmentSetup setup = new CopilotMcpEnvironmentSetup();
            Path selection = setup.initializeSelection(root);
            String defaults = Files.readString(selection);
            Object parsed = new Yaml(new SafeConstructor(new LoaderOptions())).load(defaults);
            Assert.assertEquals(parsed, Map.of("environment", "QA", "selected_target_alias", "",
                    "approved_target_aliases", List.of(), "non_sensitive_target_approved", false));
            Assert.assertFalse(Files.exists(root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH)));
            Assert.assertEquals(Files.readString(configPath(root)), VALID_CONFIG);
            Files.writeString(selection, APPROVED_SELECTION);
            Assert.assertEquals(setup.initializeSelection(root), selection);
            Assert.assertEquals(Files.readString(selection), APPROVED_SELECTION);
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldInitializeMissingSelectionThenRequireManualApprovalWithoutReplacingMcp() throws IOException {
        Path root = consumerRoot();
        try {
            Path mcp = root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH);
            Files.createDirectories(mcp.getParent());
            Files.writeString(mcp, "existing personal MCP configuration\n");
            CopilotProfileException missing = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertTrue(missing.getMessage().contains("edit the approved fields"));
            Assert.assertTrue(Files.exists(root.resolve(CopilotMcpEnvironmentSetup.SELECTION_PATH)));
            Assert.assertEquals(Files.readString(mcp), "existing personal MCP configuration\n");
            CopilotProfileException unapproved = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertTrue(unapproved.getMessage().contains("Owner approval"));
            Assert.assertFalse(Files.exists(mcp.resolveSibling("mcp.json.bak")));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldPreserveExistingTemplateAndSelectionAndCreateOnlyMissingSelection() throws IOException {
        Path root = consumerRoot();
        try {
            CopilotDiscoveryDraftExporter exporter = new CopilotDiscoveryDraftExporter();
            Path mcp = exporter.writeMcpTemplate(policy(), root);
            Assert.assertTrue(Files.readString(mcp).contains("https://<APPROVED_TARGET_ORIGIN>"));
            Path selection = root.resolve(CopilotMcpEnvironmentSetup.SELECTION_PATH);
            Files.writeString(mcp, "existing local MCP configuration\n");
            Files.writeString(selection, APPROVED_SELECTION);
            Assert.assertEquals(exporter.writeMcpTemplate(policy(), root), mcp);
            Assert.assertEquals(Files.readString(mcp), "existing local MCP configuration\n");
            Assert.assertEquals(Files.readString(selection), APPROVED_SELECTION);
            Files.delete(selection);
            Assert.assertEquals(exporter.writeMcpTemplate(policy(), root), mcp);
            Assert.assertTrue(Files.readString(selection).contains("non_sensitive_target_approved: false"));
            Assert.assertEquals(Files.readString(mcp), "existing local MCP configuration\n");
        } finally {
            deleteRecursively(root);
        }
    }

    @DataProvider(name = "invalidSelections")
    public Object[][] invalidSelections() {
        return new Object[][] {
                {APPROVED_SELECTION.replace("true", "false")},
                {APPROVED_SELECTION.replace("true", "'true'")},
                {APPROVED_SELECTION.replace("['application.baseUrl']", "[]")},
                {APPROVED_SELECTION.replace("['application.baseUrl']", "['different_alias']")},
                {APPROVED_SELECTION.replace("'QA'", "'pRoD'")},
                {APPROVED_SELECTION.replace("'QA'", "'production'")},
                {APPROVED_SELECTION.replace("'QA'", "'LIVE'")},
                {APPROVED_SELECTION.replace("'QA'", "'QA unsafe'")},
                {APPROVED_SELECTION.replace("'QA'", "'1QA'")},
                {APPROVED_SELECTION.replace("'application.baseUrl'", "'https://example.invalid'")},
                {APPROVED_SELECTION.replace("'application.baseUrl'", "'1alias'")},
                {APPROVED_SELECTION.replace("'application.baseUrl'", "'" + "a".repeat(101) + "'")},
                {APPROVED_SELECTION + "unapproved_extra: 'https://example.invalid/private'\n"},
                {APPROVED_SELECTION.replace("approved_target_aliases: ['application.baseUrl']",
                        "approved_target_aliases: 'application.baseUrl'")}
        };
    }

    @Test(dataProvider = "invalidSelections")
    public void shouldRejectMissingOrInvalidApprovalBeforeWrites(String selection) throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, selection);
            Assert.expectThrows(CopilotProfileException.class, () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertFalse(Files.exists(root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH)));
            Assert.assertEquals(Files.readString(configPath(root)), VALID_CONFIG);
            Assert.assertEquals(Files.readString(root.resolve(CopilotMcpEnvironmentSetup.SELECTION_PATH)), selection);
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldResolveSelectionEnvironmentOverrideAndStripUrlToLowercaseOrigin() throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, APPROVED_SELECTION);
            String config = """
                    application:
                      baseUrl: 'https://example.invalid/default'
                    environments:
                      QA:
                        application:
                          baseUrl: 'HTTPS://EXAMPLE.INVALID:8443/private/path?secret=discarded#discarded'
                    """;
            Files.writeString(configPath(root), config);
            CopilotMcpEnvironmentSetup.SetupOutput result = new CopilotMcpEnvironmentSetup().setup(policy(), root);
            Assert.assertEquals(result.selectedTargetAlias(), "application.baseUrl");
            Assert.assertTrue(result.changed());
            String json = Files.readString(result.mcpPath());
            Assert.assertEquals(json, new CopilotPlaywrightMcpTemplateRenderer().render(policy())
                    .replace("https://<APPROVED_TARGET_ORIGIN>", "https://example.invalid:8443"));
            Assert.assertFalse(json.contains("secret"));
            Assert.assertFalse(json.contains("private/path"));
            Assert.assertEquals(Files.readString(configPath(root)), config);
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldUseTopLevelNestedAliasFallbackNormalize443AndRemainIdempotent() throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, APPROVED_SELECTION.replace("'QA'", "'SIT'"));
            String config = """
                    application:
                      baseUrl: 'https://EXAMPLE.INVALID:443/default?discarded=secret#fragment'
                    environments:
                      QA:
                        application:
                          baseUrl: 'http://example.invalid/not-selected'
                    """;
            Files.writeString(configPath(root), config);
            CopilotMcpEnvironmentSetup setup = new CopilotMcpEnvironmentSetup();
            CopilotMcpEnvironmentSetup.SetupOutput first = setup.setup(policy(), root);
            String rendered = Files.readString(first.mcpPath());
            var modified = Files.getLastModifiedTime(first.mcpPath());
            CopilotMcpEnvironmentSetup.SetupOutput second = setup.setup(policy(), root);
            Assert.assertFalse(second.changed());
            Assert.assertEquals(Files.getLastModifiedTime(second.mcpPath()), modified);
            Assert.assertEquals(Files.readString(second.mcpPath()), rendered);
            Assert.assertTrue(rendered.contains("--allowed-origins=https://example.invalid\""));
            Assert.assertFalse(rendered.contains("example.invalid:443"));
            Assert.assertFalse(Files.exists(first.mcpPath().resolveSibling("mcp.json.bak")));
            Assert.assertEquals(Files.readString(configPath(root)), config);
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldBackUpPreviousMcpAtomicallyAndNotChangeBackupOnIdenticalRerun() throws IOException {
        Path root = consumerRoot();
        try {
            CopilotDiscoveryDraftExporter exporter = new CopilotDiscoveryDraftExporter();
            Path mcp = exporter.writeMcpTemplate(policy(), root);
            String template = Files.readString(mcp);
            writeSelection(root, APPROVED_SELECTION);
            CopilotMcpEnvironmentSetup setup = new CopilotMcpEnvironmentSetup();
            setup.setup(policy(), root);
            Path backup = mcp.resolveSibling("mcp.json.bak");
            Assert.assertEquals(Files.readString(backup), template);
            String firstRendered = Files.readString(mcp);
            Files.writeString(configPath(root), "application:\n  baseUrl: 'https://example.invalid:9443/new'\n");
            setup.setup(policy(), root);
            Assert.assertEquals(Files.readString(backup), firstRendered);
            Assert.assertTrue(Files.readString(mcp).contains("--allowed-origins=https://example.invalid:9443"));
            var backupModified = Files.getLastModifiedTime(backup);
            setup.setup(policy(), root);
            Assert.assertEquals(Files.getLastModifiedTime(backup), backupModified);
            try (var files = Files.list(mcp.getParent())) {
                Assert.assertEquals(files.count(), 3L, "Only selection, MCP and its immediate backup should remain.");
            }
        } finally {
            deleteRecursively(root);
        }
    }

    @DataProvider(name = "unsafeTargets")
    public Object[][] unsafeTargets() {
        return new Object[][] {
                {"http://example.invalid/path"},
                {"https://user:secret@example.invalid/path"},
                {"https://example.invalid:65536/path"},
                {"https://example.invalid:0/path"},
                {"https://example.invalid:/path"},
                {"https://example.invalid:-1/path"},
                {"https://example.invalid:abc/path"},
                {"https://*.example.invalid/path"},
                {"https://localhost/path"},
                {"https://LOCALHOST./path"},
                {"https://sub.localhost/path"},
                {"https://127.0.0.1/path"},
                {"https://127.2.3.4/path"},
                {"https://2130706433/path"},
                {"https://0177.0.0.1/path"},
                {"https://[::1]/path"},
                {"https://[::ffff:127.0.0.1]/path"},
                {"https://0.0.0.0/path"},
                {"https://[::]/path"},
                {"https://169.254.169.254/path"},
                {"https://100.100.100.200/path"},
                {"https://192.0.0.192/path"},
                {"https://[fd00:ec2::254]/path"},
                {"https://metadata.google.internal/path"},
                {"https://example.invalid/private path"},
                {"https://example.invalid/path?secret=%malformed"},
                {"file:///private/path"},
                {"https:///missing-host"}
        };
    }

    @Test(dataProvider = "unsafeTargets")
    public void shouldRejectUnsafeTargetsWithoutLeakingResolvedValueOrChangingExistingMcp(String target) throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, APPROVED_SELECTION);
            Path mcp = root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH);
            Files.writeString(mcp, "existing local MCP\n");
            String config = "application:\n  baseUrl: '" + target + "'\n";
            Files.writeString(configPath(root), config);
            CopilotProfileException exception = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertFalse(exception.getMessage().contains(target));
            Assert.assertNull(exception.getCause(), "Unsafe URL causes must not retain sensitive URI input.");
            Assert.assertEquals(Files.readString(mcp), "existing local MCP\n");
            Assert.assertEquals(Files.readString(configPath(root)), config);
            Assert.assertFalse(Files.exists(mcp.resolveSibling("mcp.json.bak")));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldAllowMaximumPortAndSafeLiteralIpv6WithoutRetainingPath() throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, APPROVED_SELECTION);
            Files.writeString(configPath(root), "application:\n  baseUrl: 'https://example.invalid:65535/path'\n");
            Path output = new CopilotMcpEnvironmentSetup().setup(policy(), root).mcpPath();
            Assert.assertTrue(Files.readString(output).contains("--allowed-origins=https://example.invalid:65535"));
            Files.writeString(configPath(root), "application:\n  baseUrl: 'https://[2001:DB8::1]:443/path'\n");
            new CopilotMcpEnvironmentSetup().setup(policy(), root);
            Assert.assertTrue(Files.readString(output).contains("--allowed-origins=https://[2001:db8::1]\""));
        } finally {
            deleteRecursively(root);
        }
    }

    @DataProvider(name = "invalidConfigurations")
    public Object[][] invalidConfigurations() {
        return new Object[][] {
                {"unrelated: 'https://example.invalid/private'\n"},
                {"application:\n  baseUrl: ''\n"},
                {"application:\n  baseUrl: ['https://example.invalid/private']\n"},
                {"application.baseUrl: 'https://example.invalid/not-a-nested-key'\n"},
                {"application:\n  baseUrl: 'https://example.invalid/default'\nenvironments:\n  QA:\n    application:\n      baseUrl: 'http://example.invalid/override-is-invalid'\n"}
        };
    }

    @Test(dataProvider = "invalidConfigurations")
    public void shouldFailUnknownOrInvalidConfigBeforeWritingMcp(String configuration) throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, APPROVED_SELECTION);
            Files.writeString(configPath(root), configuration);
            Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertFalse(Files.exists(root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH)));
            Assert.assertEquals(Files.readString(configPath(root)), configuration);
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldRejectMalformedYamlWithoutRawDiagnosticsInExceptionOrCli() throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, APPROVED_SELECTION);
            String malformed = "application:\n  baseUrl: [https://example.invalid/private?secret=hidden\n";
            Files.writeString(configPath(root), malformed);
            CopilotProfileException exception = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertFalse(exception.getMessage().contains("example.invalid"));
            Assert.assertFalse(exception.getMessage().contains("hidden"));
            Assert.assertNull(exception.getCause());
            synchronized (CopilotDiscoveryTool.class) {
                PrintStream original = System.err;
                ByteArrayOutputStream errors = new ByteArrayOutputStream();
                try (PrintStream capture = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
                    System.setErr(capture);
                    Assert.assertEquals(CopilotDiscoveryTool.run("mcp-setup", "--root", root.toString()), 1);
                } finally {
                    System.setErr(original);
                }
                Assert.assertFalse(errors.toString(StandardCharsets.UTF_8).contains("example.invalid"));
                Assert.assertFalse(errors.toString(StandardCharsets.UTF_8).contains("hidden"));
            }
            Files.writeString(configPath(root), VALID_CONFIG);
            writeSelection(root, "environment: [https://example.invalid/private?secret=hidden\n");
            CopilotProfileException selectionError = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertFalse(selectionError.getMessage().contains("example.invalid"));
            Assert.assertNull(selectionError.getCause());
            Assert.assertFalse(Files.exists(root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH)));
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldRequireRootIgnoreRuleBeforeInitializingAnyLocalFile() throws IOException {
        Path root = consumerRoot();
        try {
            Files.delete(root.resolve(".gitignore"));
            CopilotMcpEnvironmentSetup setup = new CopilotMcpEnvironmentSetup();
            Assert.expectThrows(CopilotProfileException.class, () -> setup.initializeSelection(root));
            Assert.expectThrows(CopilotProfileException.class, () -> setup.setup(policy(), root));
            Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotDiscoveryDraftExporter().writeMcpTemplate(policy(), root));
            Assert.assertFalse(Files.exists(root.resolve(".fnb-etaf")));
            Files.writeString(root.resolve(".gitignore"), ".fnb-etaf/\n");
            Assert.expectThrows(CopilotProfileException.class, () -> setup.initializeSelection(root));
            Assert.assertFalse(Files.exists(root.resolve(".fnb-etaf")));
        } finally {
            deleteRecursively(root);
        }
    }

    @DataProvider(name = "symlinkPaths")
    public Object[][] symlinkPaths() {
        return new Object[][] {
                {".fnb-etaf", true}, {".fnb-etaf/copilot", true},
                {".fnb-etaf/copilot/selection.yml", false}, {".fnb-etaf/copilot/mcp.json", false},
                {".fnb-etaf/copilot/mcp.json.bak", false}, {"src", true},
                {"src/test/resources/config", true}, {"src/test/resources/config/config.yml", false},
                {".gitignore", false}
        };
    }

    @Test(dataProvider = "symlinkPaths")
    public void shouldRejectSymlinkedLocalArtifactsAndConfigurationAncestors(String relativePath, boolean directory) throws IOException {
        Path root = consumerRoot();
        Path outside = Files.createTempDirectory("fnb-etaf-mcp-outside-");
        try {
            writeSelection(root, APPROVED_SELECTION);
            Path mcp = root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH);
            Files.writeString(mcp, "previous MCP\n");
            Path link = root.resolve(relativePath);
            Path target = directory ? outside.resolve("directory") : outside.resolve("file");
            if (directory) {
                Files.createDirectory(target);
            } else {
                Files.writeString(target, "outside must remain unchanged\n");
            }
            deleteRecursively(link);
            Files.createDirectories(link.getParent());
            Files.createSymbolicLink(link, target);
            CopilotProfileException exception = Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().setup(policy(), root));
            Assert.assertTrue(exception.getMessage().contains("Symbolic links"));
            if (directory) {
                try (var files = Files.list(target)) {
                    Assert.assertEquals(files.count(), 0L);
                }
            } else {
                Assert.assertEquals(Files.readString(target), "outside must remain unchanged\n");
            }
        } finally {
            deleteRecursively(root);
            deleteRecursively(outside);
        }
    }

    @Test
    public void shouldRejectSymlinkRootAndTemplateOutputWithoutCreatingSelection() throws IOException {
        Path root = consumerRoot();
        Path links = Files.createTempDirectory("fnb-etaf-mcp-root-link-");
        try {
            Path rootLink = links.resolve("consumer");
            Files.createSymbolicLink(rootLink, root);
            Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotMcpEnvironmentSetup().initializeSelection(rootLink));
            Path mcp = root.resolve(CopilotMcpEnvironmentSetup.MCP_PATH);
            Files.createDirectories(mcp.getParent());
            Files.createSymbolicLink(mcp, configPath(root));
            Assert.expectThrows(CopilotProfileException.class,
                    () -> new CopilotDiscoveryDraftExporter().writeMcpTemplate(policy(), root));
            Assert.assertFalse(Files.exists(root.resolve(CopilotMcpEnvironmentSetup.SELECTION_PATH)));
            Assert.assertEquals(Files.readString(configPath(root)), VALID_CONFIG);
        } finally {
            deleteRecursively(links);
            deleteRecursively(root);
        }
    }

    @Test
    public void shouldKeepExactCliSwitchesAndPrintOnlyAliasAndRegistrationReminder() throws IOException {
        Path root = consumerRoot();
        try {
            writeSelection(root, APPROVED_SELECTION);
            synchronized (CopilotDiscoveryTool.class) {
                PrintStream original = System.out;
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
                    System.setOut(capture);
                    Assert.assertEquals(CopilotDiscoveryTool.run("mcp-setup", "--root", root.toString()), 0);
                } finally {
                    System.setOut(original);
                }
                String message = output.toString(StandardCharsets.UTF_8);
                Assert.assertTrue(message.contains("application.baseUrl"));
                Assert.assertTrue(message.contains("No MCP server or browser was started"));
                Assert.assertTrue(message.contains("Personal Copilot/IntelliJ MCP registration or reload"));
                Assert.assertFalse(message.contains("https://"));
                Assert.assertFalse(message.contains("example.invalid"));
            }
            Assert.assertEquals(CopilotDiscoveryTool.run("mcp-setup", "--input", root.toString()), 2);
            Assert.assertEquals(CopilotDiscoveryTool.run("mcp-setup", "--root", root.toString(), "--extra"), 2);
        } finally {
            deleteRecursively(root);
        }
    }

    private static CopilotDiscoveryPolicy policy() {
        return CopilotProfileLoader.loadEmbeddedProfile().discoveryPolicy();
    }

    private static Path consumerRoot() throws IOException {
        Path root = Files.createTempDirectory("fnb-etaf-mcp-consumer-");
        Files.writeString(root.resolve(".gitignore"), "/.fnb-etaf/\n");
        Files.createDirectories(configPath(root).getParent());
        Files.writeString(configPath(root), VALID_CONFIG);
        return root;
    }

    private static Path configPath(Path root) {
        return root.resolve(CopilotDiscoveryTargetConfigurationVerifier.STANDARD_CONFIGURATION_PATH);
    }

    private static void writeSelection(Path root, String selection) throws IOException {
        Path path = root.resolve(CopilotMcpEnvironmentSetup.SELECTION_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, selection);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("Could not delete temporary test path.", exception);
                }
            });
        }
    }
}
